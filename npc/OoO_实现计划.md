# 乱序单发 CPU（OoO）实现计划 v4 —— 含 FU 拆分与乱序隐坑排查

项目：`npc/ioid_chisel_vsrc`（Chisel，当前顺序五级流水线 IFU→IDU→EXU→LSU→WBU）

> 前置条件：`MulDiv` 已完成（独立 DivUnit）；BPU 含 GShare+RAS；difftest 已修复；仿真有 [HANG]/[STUCK] 检测。
> 参考：香山 rename/rob/rs（6 发射超标量，部件逐个对照）；姚永斌《超标量处理器设计》第 3 章；NutShell 文档 ISU/WBU。
> **v4 相对 v3 的审查修正**（v3 的坑，v4 已标注 ★）：
> - ★1 x0 特判判据是 `reg_write && rd≠0`，不是 `rd≠0`——store 指令 `inst(11,7)` 是 funct3 的一部分**不是 0**，按 rd≠0 会误分配/误回收
> - ★2 ROB 需补 `issued` 标志（区分"已发射未完成"与"未发射"）
> - ★3 长延迟单元（DIV/load）在飞期间被冲刷：结果写回必须核对 ROB 项有效性，否则污染已回收的物理寄存器
> - ★4 乱序后 BPU 的 GShare(ghr)/RAS(push/pop) 必须移到**提交时**更新（执行序≠程序序），否则表内容错乱
> - ★5 ROB 需存 load 的访存地址 `load_addr`（difftest MMIO 检测 + 阶段 5 冲突检测都要用）
> - ★6 store 提交写内存与 load 执行读内存**共用一条 AXI 口**，需要仲裁
> - ★7 C++ 侧不只 cpu.cpp，`regs.cpp`（读 refile 内存数组）也要改
> - ★8 FENCE.I 现在直连 IDU 译码即触发，提交化后必须改由 COMMIT 触发
> - ★9 PRF/BusyTable 必须**组合读**（Reg 数组而非同步 Mem），否则 REN 级要多插一拍
> - ★10 阶段 4 的 EXU 必须**拆分为独立 FU**（ALU/BRANCH/LSU/DIV 各自带输入锁存），否则 DIV 在算的 32 拍内 ALU 指令无处可去

## 路线图

```
阶段0 乘除前置   阶段1 四件套    阶段2 顺序后端    阶段3 异常/CSR    阶段4 乱序发射    阶段5 内存序    阶段6 前端增强   阶段7 调优
DivUnit模块 ──→ PRF/BusyTable ──→ REN→ROB→提交 ──→ 全部提交化 ──→ RS+CDB+FU拆分 ──→ load/store ──→ FQ+BPU升级 ──→ 参数/perf
(已完成)      RAT/FreeList     分支冲刷+回滚      ecall/ebreak     真正乱序       冲突检测+仲裁    取指缓冲       综合/文档
              单元测试          cpu-tests过       coremark过       IPC↑            序敏感测试      预测率↑
```

**原则**：每阶段结束可编译可回归；先功能后乱序；DivUnit 握手接口原样迁移为长延迟执行单元。

---

## 阶段 0：乘除法器前置（回顾，已另列 MulDiv 计划）

- 乘法进组合 ALU（1 拍）；除法做成独立 `DIV`（`req_valid/req_ready` + `result_valid`，结果拍 `cnt==0`）
- 当前 EXU 集成 3 行门控：`in.ready` 含 `!div_busy`；`out.valid` 含 `div_done`；`alu_result := Mux(is_div, div.result, ...)`
- 乱序改造时 **DIV 内部一字不改**，只有"冻结流水线"变成"发射后独立跑 + 结果广播"

---

## 阶段 1：四个核心模块（先独立写、独立测）

> 不接线，模块级仿真验证。此时纠错成本最低。
> **为什么是这四个**：乱序核心 = 用"物理寄存器 + 重命名映射 + 就绪表 + 顺序提交锚点"替换"架构寄存器 + 旁路网络"。

### 1.1 PRF 物理寄存器堆 —— `core/prf.scala`

**原理**：乱序执行需要同时存在多个版本的同一个架构寄存器（WAR/WAW 消除）。每次写目标分配一个新物理寄存器，旧版本留在 PRF 里供已发射的读操作使用；架构最新版本由 RAT 指出。

```scala
class PRF_IO(xlen: Int) extends Bundle {
  val raddr1 = Input(UInt(PHYS_W.W));  val rdata1 = Output(UInt(xlen.W))
  val raddr2 = Input(UInt(PHYS_W.W));  val rdata2 = Output(UInt(xlen.W))
  val waddr1 = Input(UInt(PHYS_W.W));  val wdata1 = Input(UInt(xlen.W));  val wen1 = Input(Bool())
  val waddr2 = Input(UInt(PHYS_W.W));  val wdata2 = Input(UInt(xlen.W));  val wen2 = Input(Bool())
}
```
- `N_PHYS = 32 + ROB_SIZE`（单发最少配比，阶段 7 再扫描增大），`PHYS_W = log2Ceil(N_PHYS)`
- **物理号 0 恒为 x0**：读特判 0；写口排除 0（`when(wen && waddr =/= 0.U)`）
- 口 1 = 执行写回（乱序，来自 FU 结果）；口 2 = 提交写回（CSR 指令的结果在提交时才算得出来）
- ★9 存储用 `Reg(Vec(N_PHYS, UInt(xlen.W)))` + **组合读**（不要 Mem 同步读）：
  REN 级要在同一拍拿到源值做发射判定，同步读会逼你多插一拍。
- 复位值随意（初始版本由 RAT 指向的 0..31 决定，RAT 初值即 arch 值）

### 1.2 BusyTable 就绪表 —— `core/busytable.scala`

**原理**：物理寄存器"已分配但结果还没写回"= 忙。指令的源是否就绪 = 查源物理号 1 次（比扫描 ROB 快一个量级，香山即此方案）。

```scala
class BusyTable_IO extends Bundle {
  val raddr1 = Input(UInt(PHYS_W.W));  val rready1 = Output(Bool())
  val raddr2 = Input(UInt(PHYS_W.W));  val rready2 = Output(Bool())
  val waddr1 = Input(UInt(PHYS_W.W));  val wvalid1 = Input(Bool())   // 写回：清 0
  val waddr2 = Input(UInt(PHYS_W.W));  val wvalid2 = Input(Bool())   // rename：置 1
}
```
- 每个物理寄存器 1 bit；**组合读**（Reg 数组）
- 端口分配：读 2 + 写 2。写回清位优先级高于 rename 置位：
  同一物理寄存器被"前一条指令写回 + 后一条指令重命名"引用时，Reg 语义下后写者胜——
  **把清位写在置位之后**（`when` 顺序），因为写回发生在本拍、rename 置位服务于下一条指令
- 复位：全部 0（0..31 是 RAT 初值指向的 arch 版本，本就就绪；32..47 在 FreeList 里从未被引用）

### 1.3 RAT + FreeList —— `core/rename.scala`

**原理**：RAT 是"架构寄存器 → 物理寄存器"的映射，重命名在它上面改写目标；FreeList 是空闲物理寄存器池，分配/回收它。

```scala
val rat = RegInit(VecInit((0 until 32).map(i => i.U(PHYS_W.W))))
// 读：src_phys = rat(rs1/rs2)（组合）
// 写（重命名）：dest_phys = free_list 弹出一个；old_phys = rat(rd) 随指令进 ROB（提交/回滚时回收）
```
- FreeList：环形队列（Reg 数组 + head/tail 指针），初始装 32..47；rename pop、commit push
- ★1 **分配/回滚/回收的判据统一是 `reg_write && rd =/= 0.U`，绝不是 `rd =/= 0`**：
  - store 指令 `inst(11,7)` 是 funct3（如 `000`/`001`/`010`）≠ 0，但 reg_write=0，**不得分配**
  - branch 同理
  - 当前代码里 decode 已经给出 `wbu.reg_write`，直接用
- **x0 三处特判**（全部基于 reg_write）：RAT 不分配（x0 永远映射物理号 0）；PRF 读恒 0（物理号 0 特判）；提交/回滚不回收（没有分配过）

### 1.4 ROB —— `core/rob.scala`（16 项循环队列）

**原理**：乱序执行、顺序提交的唯一锚点。ROB 里每项 = 指令的所有"提交时需要的信息"（映射关系用于回收/回滚、结果值用于写 PRF、异常状态用于精确异常、store 数据用于提交写内存）。

```scala
class ROBEntry extends Bundle {
  val valid = Bool();  val done = Bool()      // 执行完成（可提交）
  val issued = Bool()                          // ★2 已发射（区分"未发射"与"在飞"）
  val pc, inst = UInt(32.W)
  val signals = new Signals                  // 提交时决定做什么
  val imm_ext = UInt(32.W)
  val arch_rd = UInt(5.W)
  val old_phys, new_phys = UInt(PHYS_W.W)
  val dest_val = UInt(32.W)                  // 执行结果（FU 写回时存入）
  val is_ebreak = Bool()
  val state = new State                      // 异常（iaf 等，IFU/LSU 执行时写入）
  val bp_taken = Bool()                      // 预测信息，分支对账/提交更新用
  val bp_index = UInt(log2Ceil(BHT_SIZE).W)  // 随指令流传递的预测索引
  val rs1_val = UInt(32.W)                   // 发射时算好的源值（CSR/store 用）
  val store_addr, store_data = UInt(32.W)    // store 提交写内存用（发射时算好存入）
  val load_addr = UInt(32.W)                 // ★5 load 访存地址（difftest MMIO 检测 + 阶段5冲突检测）
}
```
- 分配写 tail、提交读 head；**满 → 停 REN**（唯一背压源）
- 回滚：对 `[分支+1, tail)` 逐项 invalid + 恢复 RAT + 回收 FreeList（见阶段 2）
- 阶段 5 需要**组合扫描输出**未决 store 的地址列表（`valid && issued && is_store && !done` 的 16 项组合比较）

**单元测试**：PRF/BusyTable/RAT+FreeList/ROB 各写小 testbench（`src/test/scala`，需在 `build.sbt` 配 chiseltest，或临时主函数+波形）：
x0 三处特判、store 不分配（★1 重点）、双写口、环形分配/提交/满、free list 循环、回滚恢复。

**验收**：模块级仿真全部正确。

---

## 阶段 2：顺序后端接线（最难、最重要的阶段）

> 目标：改成 **REN(译码+重命名) → 顺序发射 → 执行 → PRF/ROB → 顺序提交**。
> 不做乱序，但重命名、ROB、提交、分支回滚全对。结束时 = "带精确异常的顺序核"。

### 2.1 流水线结构与流水级划分

```
IFU(+BPU) ──> REN: Control+IMM 译码 → RAT 重命名 → 写 ROB(tail) → BusyTable 置位
                 │                （同拍组合读 PRF/BusyTable → 发射判定）
                 ▼
           顺序发射（最新未发射项：!busy(src1) && !busy(src2) 才放行，否则整级停）
                 ▼
          EXE: ALU/BRANCH/LSU/DIV（DivUnit 原样接入，发射后 32 拍）
                 │   结果 → 写 PRF(口1) → 清 BusyTable → ROB.done
                 ▼
          COMMIT: ROB(head) 顺序提交 → 回收 old_phys → store 提交写内存 → ebreak
```

关键时序决策（★9）：PRF/BusyTable 用 Reg 数组**组合读**，因此 REN 是**单拍组合级**——
同一拍内完成"译码→重命名→写 ROB→置 BusyTable→读源值→判就绪→发射握手"，
EXE 下一拍执行。若用同步读，REN 要拆成"重命名拍 + 读操作数拍"，两倍复杂度，不要。

### 2.2 部件改动

| 现有文件 | 改动 |
|---|---|
| `idu.scala` | 删流水寄存器与 Refile/CSR 读取；保留 Control/IMM/BPU 透传；输出改为译码结果（signals/pc/imm/inst）+ RAT 物理号 + 源值 |
| `exu.scala` | 改为执行单元：输入 `(src1, src2, signals, pc, imm_ext, pdest, rob_idx)`，1 拍出结果 `(result, pdest, rob_idx, done)`；DivUnit 原样接入（忙则停发射） |
| `lsu.scala` | load 走原 AXI 读状态机；**store 不占用 LSU**（地址交给 ALU 算，写内存移到提交） |
| `wbu.scala` / `refile.scala` | **删除**（提交逻辑并入 core 的 COMMIT 部分；Refile 被 PRF 替代） |
| `core.scala` | 全部重连 + 回滚逻辑 + COMMIT |

### 2.3 REN 组合逻辑细节（核心改动点）

新 `core/ren.scala`（或并入 core.scala）：
```
译码（现有 Control+IMM 原样保留）→ rs1_phys=rat(rs1), rs2_phys=rat(rs2)
→ 若 reg_write：dest_phys=fl.pop()，old_phys=rat(rd)，rat(rd):=dest_phys
→ ROB 写 tail：整包信号 + new_phys/old_phys + issued=0
→ BusyTable：waddr2=dest_phys 置 1
→ src1_ready = !bt(rs1_phys)；src2_ready = !bt(rs2_phys)
→ 读 PRF：src1_val = prf(rs1_phys)（组合）
```
**x0 的隐藏点**：`rat(0)` 恒为 0（复位 0，且 reg_write 门控保证永不改写），PRF 物理号 0 恒 0，
两处都自然满足。

### 2.4 发射与执行（顺序版）

- **发射窗口 = ROB 中最新一条 `valid && !issued` 的指令**（只可能有一条）
- 发射条件：`src1_ready && src2_ready && EXE 可接收`
  - 非 div 指令：EXE 空闲即接收；div 指令：还需 `!div_busy`（DIV 处于 IDLE 才能进）
- 发射拍：`ROB(entry).issued := 1`，源值 + 物理号 + signals 送 EXE 输入锁存
- 执行完成（EXE 输出 valid）：`PRF(口1) := result`；`BusyTable 清位`；`ROB.done := 1`；`dest_val := result`
- **DivUnit 冻结语义等价物**：div 发射后、`result_valid` 前，发射窗口停在 div 之后的指令上
  （div 自己 done 前，后面的指令永远不是"最新未发射"？不对——顺序发射下后一条指令就是
  `valid && !issued` 的项，若它的源没依赖 div 也**不能发射**，因为必须按程序序执行。
  这正是顺序核的代价，阶段 4 用 RS 解除）

### 2.5 分支：对账 + 冲刷 + RAT 回滚（本阶段核心难点）

分支执行完成（顺序核里 = EXU 解析，对账逻辑照搬现有 core.scala）：

```scala
val mispred = is_jump && ((is_ch =/= exu.io.in.bits.bp_taken) || (is_ch && exu.io.in.bits.bp_taken && exu.io.in.bits.bp_target =/= correct_pc))
```

**mispred 处理（单发逐项回滚）**：
```
1. 重定向 IFU（correct_pc）+ BPU update（照旧——顺序核里执行序=程序序，这里更新是对的）
2. 找分支在 ROB 的位置（priority encoder）
3. 对 [分支+1, tail) 的每项：
     if (reg_write) { RAT(arch_rd) := old_phys; FreeList.push(new_phys) }
     valid := 0
4. 分支项自身保留（valid, done），等提交
```
- **坑**：回滚周期禁止重命名（REN 停一拍）；分支项之前的未提交项必须保留；回滚与提交互斥。
- 若分支尚未发射（issued=0，比如前一条是 div 卡住）→ 无需回滚 RAT（还没改），仅 invalid 其后项。
  判断依据：分支项 `issued` 标志。

### 2.6 提交 COMMIT

```
1) head 项条件：valid && done && !state.state && !is_ebreak → 可提交
2) FreeList.push(old_phys)（if reg_write）
3) store：提交时写内存（AXI 写口，见 ★6 仲裁）
4) CSR 指令：提交时组合读 CSR → 算 wdata（复用 WBU 的 csr_sel mux）→ 写 CSR → 结果写 PRF(口 2)
5) ebreak：提交时给出信号（DPIC → Ebreak 模块；非 DPIC → io.ebreak）
6) head++
```
- **提交每周期 1 条**（单发）；store 提交期间 AXI 未完成则停提交（head 不动）
- ★5 difftest 需要：COMMIT 单元输出 `commit_valid / commit_pc / commit_mem_addr（load_addr 或 store_addr）/ commit_is_load`

### 2.7 difftest / C++ 适配（必做，两个 csrc 都要改）

`wbu_io_in_*` 信号全失效。逐文件：

| C++ 文件 | 现在 | 改成 |
|---|---|---|
| `cpu/cpu.cpp` | `wbu_io_in_valid`（提交判定）、`wbu_io_in_bits_r_pc`（commit_pc）、`wbu_io_in_bits_r_alu_result`（MMIO 地址）、`wbu_io_in_bits_r_signals_wbu_reg_write_sel`（is_load_wb） | 读 COMMIT 单元信号：`commit_valid/commit_pc/commit_mem_addr/commit_is_load`；MMIO 判定 = `(is_store || commit_is_load) && is_mmio_addr(commit_mem_addr)` |
| `cpu/regs.cpp` | `read_gpr_from_top` 直读 `refile__DOT__rf_ext__DOT__Memory[idx]` | ★7 架构寄存器 = `Mux(i==0, 0, PRF(RAT(i)))`——**从 core 导出组合端口 `arch_rdata(32 个)`**（不导内部 RAT 数组，路径脆弱） |
| `difftest/difftest.cpp` | — | 不动 |
| 其余（perf/monitor/device） | — | 不动 |

- 两份 csrc：`npc/ioid_chisel_csrc`（NPC 版）、`npc/mul_chisel_soc_csrc`（SoC 版）都要改
- 注意 commit 每周期最多 1 条，C++ 侧"每 commit 执行一次 difftest_one_exec"的节奏不变

**验收**：cpu-tests 全过（difftest 开）；IPC 与顺序核相当（略低正常）。

---

## 阶段 3：CSR / 异常 / 中断提交化

> 原理：乱序核里唯一能改变"外部可见状态"（CSR、内存、退出）的地方是 COMMIT。
> 异常必须"精确"：异常指令之前的一切已提交，之后的一切被冲刷。

| 提交项 | 行为 |
|---|---|
| CSRRW/CSRRS | 组合读 CSR（现有读口已是组合的）→ 算 wdata（复用 csr_sel mux）→ 写 CSR → 结果写 PRF(口 2) |
| ECALL | head 项 `wbu.irq` 标志 → 停止提交 → 写 `mepc=pc, mcause=irq_num` → **冲刷 + 回滚 `[head+1, tail)` 全部项** → 重定向 mtvec |
| 外部中断 | 提交间隙采样 `csr.io.irq` → 当作 head 处的"虚拟异常"（不占 ROB 项）：写 mepc/mcause → 冲刷回滚全部在飞项 → 重定向 mtvec |
| EBREAK | 提交时给出信号（同 2.6） |
| MRET | head 项 → 冲刷回滚 `[head+1, tail)` → 重定向 mepc |
| FENCE.I | ★8 从"IDU 译码即触发 icache.fencei"改为 **COMMIT 触发**：head 项 is_fencei → 发 icache.fencei（握手）→ 完成后 head++ 并冲刷前端 |
| IAF（取指异常） | IFU 把 `state` 塞进 ROB 项（现有 IFU_IDU_IO.state 通道原样保留进 REN/ROB） |
| LAF / SAF | LSU 执行时检测 `rresp/bresp ≠ 0` → 写回时把 `state` 写进对应 ROB 项 |

- `csr.scala` 新增异常端口：`exception { valid, pc→mepc, cause→mcause, mtvec }`
- **提交检查顺序**：`state.state`（异常）> `is_ebreak` > 正常提交
- 中断采样窗口：head 可提交且该周期无提交（或提交间隙）——防止中断夹在两条指令的提交中间

**验收**：cpu-tests + coremark（含 CSR/异常路径）+ difftest。

---

## 阶段 4：乱序发射（真正的 OoO）

### 4.1 RS 保留站（8 项）—— `core/rs.scala`

**原理**：把"发射"与"执行"解耦。指令重命名后进 RS 等源就绪；源就绪且 FU 空闲即可发射，
不再受程序序约束（顺序核的"最新未发射"限制解除）。

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
- 入站（REN 拍）：BusyTable 查就绪初始化 `srcX_ready`、PRF 组合读值（与顺序版同一套源）
- **广播（CDB）**：FU 完成 `(pdest, value)` → 组合匹配全部 8 项：`srcX_phys == pdest` → `srcX_val := value; srcX_ready := 1`
- 发射：每周期挑**最老的就绪项**（`busy && src1_ready && src2_ready`，priority encoder）；
  按 `fu` 送到对应 FU 的输入锁存。DIV/load 发射后不占发射带宽（FU 自己忙，别的指令照发）
- 背压链：RS 满 → 停 REN → 停 IFU
- store 指令也进 RS（占 ALU 槽算地址）；纯 store 的 `pdest = 无`（reg_write=0 不分配）

### 4.2 FU 拆分（★10，本阶段最容易被漏的部分）

**顺序版 EXU 是"1 拍执行 + div 冻结"的单一实体；乱序版必须拆成 4 个互相独立的执行实体**，
每个带自己的输入锁存与 busy 标志：

```
RS 发射 ──> ALU FU（1 拍）    ：alu.scala + 结果锁存
        ──> BRANCH FU（1 拍） ：比较器 + PC 加法器（pc4_imm/pc4_rs2 搬进来）+ mispred 判定 + 重定向
        ──> LSU FU（N 拍）    ：现有 lsu.scala 读状态机（去掉 store 路径）
        ──> DIV FU（32 拍）   ：div.scala 原样 + req 握手
```

- 每个 FU 输出统一 `(result, pdest, rob_idx, valid)` 进 CDB 广播（见 4.4）
- **BRANCH FU 是顺序版 EXU 里 PC/对账逻辑的搬家**：`jump_pc`、`exu_pc`、`correct_pc` mux、
  mispred 判定、BPU 更新信号的生成，全在 BRANCH FU
- 取消顺序版的 `div_busy 冻结`：DIV FU 的 `req_ready` 就是它自己的忙信号，只挡 DIV 指令

### 4.3 乱序的效果

```
addi x1, x0, 1      // 发射 → ALU，1 拍
lw   x2, 0(x1)      // 发射 → LSU，等内存 N 拍
addi x3, x0, 2      // ★ 不等 load，直接执行
add  x4, x3, x1     // ★ 依赖 x3（已就绪）
div  x5, x4, x2     // ★ 发射进 DivUnit，32 拍内别的指令照跑
```

### 4.4 写回与在飞单元的冲刷（★3，乱序正确性的关键）

- **写回路径统一走 CDB 门控**：`can_wb = ROB(rob_idx).valid && !ROB(rob_idx).done`
  - 成立才：写 PRF(口1)、清 BusyTable、`ROB.done := 1`、`dest_val := result`
  - 分支回滚已把 ROB 项 invalid、物理号回收进 FreeList——**在飞结果必须被丢弃**，
    否则新指令可能已分到那个物理号，被旧结果污染
- DIV FU / LSU FU 都输出 rob_idx，这套核对同样适用（load 的 mem_read 也要核对）
- BRANCH FU 的 mispred 信号**不受该门控约束**（冲刷本身就是要发生的事，只要回滚逻辑幂等）

### 4.5 分支对账 + 冲刷 + 回滚（乱序版）

```
BRANCH FU 出 mispred：
1. 重定向 IFU（correct_pc）
2. 定位 ROB 中该分支项（rob_idx 随指令带）
3. 回滚 [分支+1, tail)：恢复 RAT + 回收 FreeList + invalid（同阶段 2，但分支可能还没到 head）
4. 分支项自身 valid/done 保留，等提交
```

### 4.6 BPU 更新提交化（★4，阶段 4 必做）

- 顺序核里 BPU 在执行时更新是安全的（执行序≈程序序）；**乱序后执行序 ≠ 程序序**：
  - GShare 的 `ghr`（全局历史）按执行序移位 = 历史错乱
  - RAS 的 push/pop 按执行序 = 返回栈错乱（ret 预测全错）
- 改动：BPU 的 `update_valid/...` 输入改接 **COMMIT**（对分支/call/ret 提交项发更新）
- `bp_index`（预测时索引）随指令流进 ROB，提交时用它更新 BHT（已有机制，只是时机变了）
- BRANCH FU 只负责执行正确性 + 重定向，**不再更新 BPU**
- 分支未提交时的第二次 mispred：不允许——乱序后分支在其后指令提交前就执行了，
  重定向后其后指令全被回滚，不会二次提交。ROB 保证提交序，天然免疫

**验收**：cpu-tests + difftest 全过；microbench IPC 明显提升。

---

## 阶段 5：load / store 内存序

### 5.1 问题与原理

- store 提交才写内存；**load 乱序提前执行，可能读到"前面 store 还没写"的旧值**——错误
- 解决方案（本设计）：load 执行前扫描 ROB 中未决 store（`valid && issued && is_store && !done`）：
  地址相等 → load 进 LSU 的 WAIT 状态，等该 store 提交后再重发 AXI 读
- 用到的信号：ROB 组合扫描输出（16 项比较，全组合）vs 当前 load 地址

### 5.2 store 提交路径 + AXI 仲裁（★6）

- store 的 `store_addr/store_data` 在发射（ALU FU 算地址）时存入 ROB（RS 里已备好 rs2 值）
- 提交写内存：复用现有 LSU 的 AXI 写握手逻辑，但**状态机与 load 读状态机分离**：
  - 现状 LSU 是"读/写共用一个 s_IDLE/s_WOEK 状态机"
  - 改成两个小状态机（load 读机、store 提交写机）+ AXI 口仲裁
  - 仲裁：每周期最多一个 AXI 交易。**读优先**（load 在飞堵着后面指令的依赖，写可以拖）；
    写机提交 store 时若读机在飞，写等待（提交暂停，ROB 满则停 REN——无环：总线终会响应）
- 死锁检查：load WAIT 不占总线（读机空闲）；store 提交每拍 1 条；commit 等总线、总线不被 load 独占
- MMIO store：提交写 AXI 到从设备（xbar 已存在），流程不变，只是时机移到提交

**验收**：cpu-tests + difftest；写一个"store 后紧跟同地址 load"的微测试（`sw x1,0(x2); lw x3,0(x2)` 无中间依赖）。

---

## 阶段 6：前端增强（FQ + 分支预测器升级）

### 6.1 FQ 取指缓冲（4 项）—— `core/fq.scala`
- IFU 输出 → FQ，FQ → REN；冲刷时整队清空
- 原理：把"取指带宽"与"重命名背压"解耦（REN 停时 IFU 仍可取满 FQ）
- 参考 NutShell IFU 的指令队列

### 6.2 分支预测器升级
- 当前 GShare（BHT_SIZE 已是 1024？`consts.scala` 里 BHT_SIZE=1024——**v3 计划里写 128 是过时的**，
  实际已 1024；瓶颈可能在于 ghr 宽度/训练，先跑数据再决定）：
  - 先测当前 1024 项命中率，若仍 <85% 再考虑 TAGE
- 乱序核里冲刷代价更高，预测器收益放大：
  - **BTB**：配合 FQ/深流水时做（取指阶段就要出 target）
  - **TAGE+FTB+RAS**：性能冲刺。必须在乱序里做——TAGE 的推测更新恢复与 ROB 的冲刷/回滚语义耦合
- 注意：阶段 4 已把 BPU 更新移到提交，TAGE 天然继承这个机制（提交更新 + 回滚不需要恢复 ghr，
  因为 ghr 只在提交时更新，不存在"推测更新"）

**验收**：全部回归 + 命中率数据 + IPC。

---

## 阶段 7：性能调优与收尾

- 参数扫描：ROB 16→32、RS 8→16、PRF 48→80、FQ 4→8，每档全套回归
- perf 事件：`EVENT_ROB_STALL`、`EVENT_RS_EMPTY`、`EVENT_COMMIT`、`EVENT_LOAD_WAIT`、`EVENT_MISPREDICT`（沿用现有 PM 机制 + perf.cpp 打印）
- 综合检查；更新文档

---

## 文件改动总览（v4 更新）

| 文件 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|---|
| `MulDiv`（已完成） | ★ | | | | | | | |
| `config.scala` | muldiv 开关 | | | | | | | 调参 |
| `core/prf.scala`（新） | | ★ | 接线 | | | | | |
| `core/busytable.scala`（新） | | ★ | 接线 | | | | | |
| `core/rename.scala`（新） | | ★ | 接线 | | | | | |
| `core/rob.scala`（新） | | ★ | 接线 | | | 加扫描口 | | |
| `core/rs.scala`（新） | | | | | ★ | | | |
| `core/fq.scala`（新） | | | | | | | ★ | |
| `core/idu.scala` | | | 改造 | | | | | |
| `core/exu.scala` | | | 改造 | | ★拆分为独立 FU（ALU/BRANCH/LSU/DIV，各带锁存） | | | |
| `core/lsu.scala` | | | 改造(去store) | | 改造(独立读机) | 改造(写机+仲裁) | | |
| `core/wbu.scala`/`refile.scala` | | | 删除 | | | | | |
| `core/csr.scala` | | | | ★exception端口 | | | | |
| `unit/bpu.scala` | | | | | ★更新输入改接 COMMIT | | ★BTB/TAGE | |
| `core/core.scala` | | | 重连 | 重连 | 重连 | 重连 | 重连 | |
| `cpu/cpu.cpp`（两个 csrc） | | | ★路径适配 | | | | | |
| `cpu/regs.cpp`（两个 csrc） | | | ★arch_rdata 组合端口 | | | | | |
| `unit/PerfMonitor.scala` | | | | | | | | ★ |

## 风险清单（每阶段开始前过一遍）

1. ★1 **x0 判据**：一切分配/回滚/回收用 `reg_write && rd≠0`；`rd≠0` 会误伤 store/branch。
2. **RAT 回滚与重命名互斥**：回滚周期禁止重命名。
3. **PRF 双写口**：执行写 + 提交写（CSR 结果），别省口 2。
4. **BusyTable 同周期读写**：清位（写回）优先级 > 置位（rename），端口时序想清楚。
5. ★3 **在飞单元冲刷**：DIV/load 结果写回必须核对 `ROB(rob_idx).valid && !done`，否则污染回收的物理号。
6. ★4 **BPU/RAS 提交化**：阶段 4 一开乱序，ghr/RAS 立即改提交更新，别拖。
7. ★6 **AXI 读写口仲裁**：load 执行读 + store 提交写共用 dmem，做两个独立状态机 + 读优先仲裁。
8. ★7 **C++ 双文件双 csrc**：cpu.cpp（4 处信号）+ regs.cpp（arch 读）在 NPC 与 SoC 两个 csrc 都要改。
9. ★5 **load_addr 进 ROB**：difftest MMIO 检测和阶段 5 冲突检测都靠它。
10. **CSR 组合读**：CSRRS 要"读旧写新"，确认读口组合（现有已满足）。
11. **死锁三连查**：ROB满→停REN→停IFU；RS满→停REN；load等store、store等commit、commit等总线、load不占总线——确认无环。
12. **每阶段单独验收**：阶段 2 结束必须 cpu-tests+difftest 全绿再动阶段 3。

## 预期收益

| 指标 | 当前（顺序核） | 乱序单发（预期） |
|---|---|---|
| microbench IPC | 0.67（train） | 0.8+（隐藏 load 延迟 + DivUnit 并行） |
| 分支命中率 | GShare 1024 项（先测当前实际值） | 稳定后 TAGE→90%+ |
| coremark | - | 乱序 + BTB 后显著 |
