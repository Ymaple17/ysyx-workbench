# 乱序单发 CPU（OoO）实现计划 v3 —— 含乘除法器前置

项目：`npc/ioid_chisel_vsrc`（Chisel，顺序五级流水线 IFU→IDU→EXU→LSU→WBU）

> 前置条件：已完成 `MulDiv_实现计划.md`（硬件 M 扩展，DivUnit 独立模块），BPU 已含 GShare+RAS，
> difftest 已修复（PC 检查 + MMIO 提交时检测），仿真有 [HANG]/[STUCK] 检测。
> 概念参考：香山是 6 发射超标量（非单发），但其后端部件（rename/rob/rs）可逐个对照；
> 姚永斌《超标量处理器设计》第 3 章精读；NutShell 文档 ISU/WBU 两页。

## 路线图

```
阶段0 乘除前置   阶段1 四件套    阶段2 顺序后端    阶段3 异常/CSR    阶段4 乱序发射    阶段5 内存序    阶段6 前端增强   阶段7 调优
DivUnit模块 ──→ PRF/BusyTable ──→ REN→ROB→提交 ──→ 全部提交化 ──→ RS+广播 ──→ load/store ──→ FQ+BPU升级 ──→ 参数/perf
(已完成)      RAT/FreeList     分支冲刷+回滚      ecall/ebreak    真正乱序       冲突检测       取指缓冲       综合/文档
              单元测试          cpu-tests过       coremark过      IPC↑          序敏感测试      预测率↑
```

**原则**：每阶段结束可编译可回归；先功能后乱序；DivUnit 的握手接口原样迁移为长延迟执行单元。

---

## 阶段 0：乘除法器前置（已另列 `MulDiv_实现计划.md`）

要点回顾：
- 乘法进组合 ALU（1 拍）；除法做成**独立 DivUnit**（req/valid/ready + result_valid 接口）
- EXU 集成只加 3 行门控：`in.ready && !div_busy`、`out.valid && div_done`、`alu_result := Mux(is_div, div.result, ...)`
- **乱序改造时 DivUnit 原样保留**，只是"冻结流水线"改成"发射后独立跑 + 结果广播"

---

## 阶段 1：四个核心模块（先独立写、独立测）

> 不接线，模块级仿真验证。此时对"端口数、时序、x0 特判"纠错成本最低。

### 1.1 PRF 物理寄存器堆 —— `core/prf.scala`

```scala
class PRF_IO(xlen: Int) extends Bundle {
  val raddr1 = Input(UInt(PHYS_W.W));  val rdata1 = Output(UInt(xlen.W))
  val raddr2 = Input(UInt(PHYS_W.W));  val rdata2 = Output(UInt(xlen.W))
  val waddr1 = Input(UInt(PHYS_W.W));  val wdata1 = Input(UInt(xlen.W));  val wen1 = Input(Bool())
  val waddr2 = Input(UInt(PHYS_W.W));  val wdata2 = Input(UInt(xlen.W));  val wen2 = Input(Bool())
}
```
- `N_PHYS = 32 + ROB_SIZE`（单发最少配比），`PHYS_W = log2Ceil(N_PHYS)`
- **物理号 0 恒为 x0**：读特判 0，写口排除 0
- 口 1 = 执行回写（乱序），口 2 = 提交回写（CSR 结果）
- 风格照抄现有 `refile.scala`（Mem + 读 mux）

### 1.2 BusyTable 就绪表 —— `core/busytable.scala`

参考香山 `backend/rename/BusyTable.scala`。**每个物理寄存器 1 个忙位**：
- 重命名分配时置 1；执行写回（写 PRF）时清 0
- 源就绪判定 = 1 次查表（比扫描 ROB 简单一个量级）
- 读口 2、写口 2（rename 置位 + 写回清位），同周期读写注意端口仲裁

### 1.3 RAT + FreeList —— `core/rename.scala`

参考香山 `backend/rename/Rename.scala`。

```scala
val rat = RegInit(VecInit((0 until 32).map(i => i.U(PHYS_W.W))))
// 读：src_phys = rat(rs1/rs2)
// 写（重命名）：dest_phys = free_list 弹出一个；old_phys = rat(rd) 随指令进 ROB（提交时回收）
// ★ x0：rd==0 不分配、不写 RAT、不回收
```
FreeList：环形队列，初始装 32..47；rename pop、commit push。

### 1.4 ROB —— `core/rob.scala`（16 项循环队列）

```scala
class ROBEntry extends Bundle {
  val valid = Bool();  val done = Bool()     // 执行完成
  val pc, inst = UInt(32.W)
  val signals = new Signals                  // 提交时决定做什么
  val imm_ext = UInt(32.W)
  val arch_rd = UInt(5.W)
  val old_phys, new_phys = UInt(PHYS_W.W)
  val dest_val = UInt(32.W)                  // 执行结果
  val is_ebreak = Bool()
  val state = new State                      // 异常（iaf 等）
  val bp_taken = Bool()                      // 预测信息，分支对账用
  val rs1_val = UInt(32.W)                   // 发射时算好的源值（CSR/store 用）
  val store_addr, store_data = UInt(32.W)
}
```
- 分配写 tail、提交读 head；满 → 停重命名（唯一的背压源之一）

**单元测试**：PRF/BusyTable/RAT+FreeList/ROB 各写小 testbench（`src/test/scala` 或波形）：
x0 三处特判、双写口、环形分配/提交/满、free list 循环。

**验收**：模块级仿真全部正确。

---

## 阶段 2：顺序后端接线（最难、最重要的阶段）

> 目标：改成 **REN(译码+重命名) → 顺序发射 → 执行 → PRF/ROB → 顺序提交**。
> 不做乱序，但重命名、ROB、提交、分支回滚全对。结束时 = "带精确异常的顺序核"。

### 2.1 流水线结构

```
IFU(+BPU) ──> REN: Control+IMM 译码 → RAT 重命名 → 写 ROB(tail) → 顺序发射
                │                                                │
                ▼                                                ▼
          PRF 读操作数(src_phys)                           EXE: ALU/BRANCH/LSU/DivUnit
                                                              │
                                       结果 → 写 PRF(口1) → 清 BusyTable → ROB.done
                                                              │
          COMMIT: ROB(head) 顺序提交 → 回收 old_phys → store 提交写内存 → ebreak
```

### 2.2 部件改动

| 现有文件 | 改动 |
|---|---|
| `idu.scala` | 删流水寄存器与 Refile/CSR 读取；保留 Control/IMM，输出改成译码结果 + RAT 物理号 |
| `exu.scala` | 改为执行单元：输入 `(src1, src2, signals, pc, imm_ext, pdest, rob_idx)`，1 拍出结果；**DivUnit 原样接入** |
| `lsu.scala` | load 走原 AXI 状态机；store 只算地址（交 ALU），写内存移到提交 |
| `wbu.scala` / `refile.scala` | **删除** |
| `core.scala` | 全部重连 + 回滚逻辑 + COMMIT |

### 2.3 发射与执行（顺序版）

- REN 完成 → 写 ROB(tail)，源物理号送 BusyTable 查就绪、PRF 读值
- **顺序发射**：最新未发射指令 `!busy(src1) && !busy(src2)` 才发射；不满足等下周期
- 执行完成：`PRF(口1) := result`；`BusyTable 清位`；`ROB.done := 1`
- DivUnit：发射后 32 拍完成——期间顺序发射停（同现在的 EXU 冻结语义）

### 2.4 分支：对账 + 冲刷 + RAT 回滚（本阶段核心难点）

分支执行完成（顺序核里 = EXU 解析，沿用现有对账）：

```scala
val mispred = is_jump && ((is_ch =/= exu.io.in.bits.bp_taken) || (is_ch && exu.io.in.bits.bp_taken && exu.io.in.bits.bp_target =/= correct_pc))
```

**mispred 处理（单发逐项回滚）**：
```
1. 重定向 IFU（correct_pc）+ BPU update（照旧）
2. 找分支在 ROB 的位置（priority encoder）
3. 对 [分支+1, tail) 的每项：RAT(arch_rd) := old_phys；FreeList.push(new_phys)；invalid
4. 分支项自身保留（valid, done），等提交
```
- **坑**：回滚周期禁止重命名；分支项之前的未提交项必须保留；回滚与提交互斥。

### 2.5 提交 COMMIT

```scala
// 1) FreeList.push(old_phys)（arch_rd != 0）
// 2) store：提交时写内存（LSU 的 AXI 写口复用）
// 3) ebreak：提交时给出信号（C++ 路径适配见 2.6）
// 4) head++
```

### 2.6 difftest / C++ 适配（必做）

`cpu.cpp` 引用的 `wbu_io_in_*` 信号全失效：
- commit 有效/PC → 改读新 COMMIT 单元的信号
- 架构寄存器读取（difftest 用）：`arch_rdata(i) = Mux(i==0, 0, PRF(RAT(i)))` 组合读，从 core 引出
- 两个 csrc（NPC + SoC）都要改

**验收**：cpu-tests 全过（difftest 开）；IPC 与顺序核相当（略低正常）。

---

## 阶段 3：CSR / 异常 / 中断提交化

| 提交项 | 行为 |
|---|---|
| CSRRW/CSRRS | 组合读 CSR（确认读口是组合的）→ 算 wdata（复用 csr_sel mux）→ 写 CSR → 结果写 PRF(口 2) |
| ECALL | 停止提交 → 冲刷 → `csr.exception(valid, pc, cause)` → 重定向 mtvec |
| 外部中断 | 提交间隙采样 `csr.io.irq` → 异常流程 |
| EBREAK | 提交时给出信号（同 2.5） |
| MRET | 冲刷 → 重定向 mepc |
| FENCE.I | ICache 无效化 + 冲刷 |

`csr.scala` 新增异常端口：`exception { valid, pc→mepc, cause→mcause, mtvec }`。

**验收**：cpu-tests + coremark（含 CSR/异常路径）+ difftest。

---

## 阶段 4：乱序发射（真正的 OoO）

### 4.1 RS 保留站（8 项）—— `core/rs.scala`

```scala
class RSEntry extends Bundle {
  val busy = Bool()
  val fu   = UInt(2.W)          // ALU / BRANCH / LSU / DIV
  val signals = new Signals
  val pc, imm_ext = UInt(32.W)
  val pdest   = UInt(PHYS_W.W)
  val arch_rd = UInt(5.W)
  val rob_idx = UInt(ROB_PTR_W.W)
  val src1_ready = Bool(); val src1_phys = UInt(PHYS_W.W); val src1_val = UInt(32.W)
  val src2_ready = Bool(); val src2_phys = UInt(PHYS_W.W); val src2_val = UInt(32.W)
}
```
- 入站：BusyTable 查就绪度初始化、PRF 读值
- **广播（CDB）**：执行完成 `(pdest, value)` 组合匹配全部 8 项，唤醒等待者
- 发射：每周期挑一条就绪项（优先最老；DIV/load 完成后才出站）
- **DivUnit 在这里原样接入**：发射进 DIV 单元 → 32 拍后 `result_valid` → 广播 + 写 PRF + ROB.done
- 背压链：RS 满 → 停重命名 → 停 IFU

### 4.2 乱序的效果

```
addi x1, x0, 1      // 发射 → ALU，1 拍
lw   x2, 0(x1)      // 发射 → LSU，等内存 N 拍
addi x3, x0, 2      // ★ 不等 load，直接执行
add  x4, x3, x1     // ★ 依赖 x3（已就绪）
div  x5, x4, x2     // ★ 发射进 DivUnit，32 拍内别的指令照跑
```

**验收**：cpu-tests + difftest 全过；microbench IPC 明显提升。

---

## 阶段 5：load / store 内存序

- store 提交写内存；load 乱序执行前检查 ROB 中未决 store 的地址，相等则等待（LSU 加 WAIT 状态）
- store 的 `store_addr/store_data` 在发射（ALU 算地址）时存入 ROB
- 死锁检查：load WAIT 不占总线；store 提交每拍 1 条；无环

**验收**：cpu-tests + difftest；写一个"store 后紧跟同地址 load"的微测试。

---

## 阶段 6：前端增强（FQ + 分支预测器升级）

### 6.1 FQ 取指缓冲（4 项）—— `core/fq.scala`
- IFU 输出 → FQ，FQ → REN；冲刷时整队清空
- 参考 NutShell IFU 的指令队列

### 6.2 分支预测器升级
- 当前 GShare（128 项 BHT，train 命中率 78.6%——瓶颈是表太小）：
  - 先把 `BHT_SIZE` 提到 1024（`PC[10:2] ^ ghr`，ghr 10 位）——预计 85%+
- 乱序核里冲刷代价更高，预测器收益放大：
  - **BTB**：配合 FQ/深流水时做（取指阶段就要出 target）
  - **TAGE+FTB+RAS**：性能冲刺。**必须在乱序里做**——TAGE 的推测更新恢复与 ROB 的冲刷/回滚语义耦合，顺序核里写会返工

**验收**：全部回归 + 命中率数据 + IPC。

---

## 阶段 7：性能调优与收尾

- 参数扫描：ROB 16→32、RS 8→16、PRF 48→80、FQ 4→8，每档全套回归
- perf 事件：`EVENT_ROB_STALL`、`EVENT_RS_EMPTY`、`EVENT_COMMIT`、`EVENT_LOAD_WAIT`、`EVENT_MISPREDICT`（沿用现有 PM 机制 + perf.cpp 打印）
- 综合检查；更新文档

---

## 文件改动总览

| 文件 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|---|
| `MulDiv`（已完成） | ★ | | | | | | | |
| `config.scala` | muldiv 开关 | | | | | | | 调参 |
| `core/prf.scala` | | ★ | | | | | | |
| `core/busytable.scala` | | ★ | | | | | | |
| `core/rename.scala` | | ★ | 接线 | | | | | |
| `core/rob.scala` | | ★ | 接线 | | | | | |
| `core/rs.scala` | | | | | ★ | | | |
| `core/fq.scala` | | | | | | | ★ | |
| `core/idu.scala` | | | 改造 | | | | | |
| `core/exu.scala` | | | 改造 | | 改造 | | | |
| `core/lsu.scala` | | | 改造 | | 改造 | 改造 | | |
| `core/wbu.scala`/`refile.scala` | | | 删除 | | | | | |
| `core/csr.scala` | | | | ★ | | | | |
| `core/core.scala` | | | 重连 | 重连 | 重连 | 重连 | 重连 | |
| `cpu/*.cpp`（两个 csrc） | | | ★路径适配 | | | | | |
| `unit/PerfMonitor.scala` | | | | | | | | ★ |

## 风险清单（每阶段开始前过一遍）

1. **RAT 回滚与重命名互斥**：回滚周期禁止重命名。
2. **x0 三处特判**：RAT 不分配、PRF 读恒 0、提交不回收。
3. **PRF 双写口**：执行写 + 提交写，别省。
4. **BusyTable 同周期读写**：端口时序想清楚（香山有现成答案）。
5. **CSR 组合读**：CSRRS 要"读旧写新"。
6. **C++ 信号路径**：`wbu_io_*` 全失效，`cpu.cpp` 不更新 = difftest 全红。
7. **死锁三连查**：ROB满→停REN→停IFU；RS满→停REN；load等store、store等commit、commit等总线——确认无环。
8. **每阶段单独验收**：阶段 2 结束必须 cpu-tests+difftest 全绿再动阶段 3。

## 预期收益

| 指标 | 当前（顺序核） | 乱序单发（预期） |
|---|---|---|
| microbench IPC | 0.67（train） | 0.8+（隐藏 load 延迟 + DivUnit 并行） |
| 分支命中率 | 78.6%（128 项 BHT） | 先提 BHT→85%+；乱序稳定后 TAGE→90%+ |
| coremark | - | 乱序 + BTB 后显著 |
