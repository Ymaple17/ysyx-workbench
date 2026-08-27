# 阶段 4b：CSR 架构写迁到 commit

## 学习导航
- **理论目标**：本章先理解：`csr.wen` 仅由 **commit** 拉高；关掉 WBU 对 CSR 的架构写；用 **在飞 CSR 串行化** 保住 ID 组合读正确性。
- **最小实现**：只允许 ROB commit 拉高 `csr.wen`，CSR 源值在 ready 后进入 ROB，并用 `csr_inflight` 保证组合读到正确架构值。
- **当前参考核**：本章阶段快照曾通过单测 21/21 与 cpu-tests 35/35；提交写 CSR 的语义继续保留在阶段 11d。
- **后续扩展**：4c 把 ecall 和同步异常也改成 head 记录、下一拍 redirect/flush。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：4a（ROB 已有 CSR 字段）；建议与 4a 同批合入。  
**目标**：`csr.wen` 仅由 **commit** 拉高；关掉 WBU 对 CSR 的架构写；用 **在飞 CSR 串行化** 保住 ID 组合读正确性。  
**本章阶段快照**：参考实现已落地 — **单测 21/21 + cpu-tests 35/35**（验收请自勾）。

**铁律**：CSR 是架构态；wrong-path / 非 head 完成不得写。读侧仍是 ID 组合读 → 必须限制「未提交 CSR 写」与「后继 CSR 读」重叠。

---

## 1. 原理：WBU 投机写的危害

### 1.1 写回 ≠ 提交（CSR 版）

| 路径 | 乱序后 |
|------|--------|
| WBU `valid && csr_write` 就 `wen` | younger / wrong-path 先写 mtvec |
| 随后 mispred 杀该项 | **CSR 已脏**；ROB `valid=0` 回滚不了寄存器堆 |
| 后继 / 异常用脏 mtvec | difftest 炸或跑飞 |

GPR 有 rename/PRF；**CSR 通常无重命名** → 更必须 commit 写。

### 1.2 相对 4a / 旧路径

| 项 | 旧（WBU 写） | **4b** |
|----|--------------|--------|
| `csr.wen` | `csr_write && wbu.valid` | **`cm_fire && cm_bits.csr_write`** |
| wdata | 流水 rd1/csr_rd1 | ROB **`rs1_val/csr_rd1/csr_sel`** |
| WBU.csr | 驱动写口 | **`wen` 恒 0** |
| 后继 CSR 入队 | 无限制 | **`csr_inflight` stall** |
| CSR 源未 ready | 可能带垃圾 enq | **`!id_s1_rdy` 也 stall** |

rd 写 PRF 仍可 WBU + `can_wb`（`CSR_DATA`）；本档不强制「rd 也 commit 写」。

### 1.3 精确异常

```text
CSR 写的架构可见点 = 该指令 commit_fire 那一拍
之前：像没写过；被 flush 的 CSR：永远不 wen
```

---

## 2. 为何要 `csr_inflight`

ID 仍 **组合读** CSR。若允许：

```text
csrw mtvec, t0     // 已 enq，未 commit → 架构仍旧
csrr a0, mtvec     // ID 读到旧值 → csr_rd1 错
第一条稍后 commit → 太晚
```

规则（与源码一致）：

```scala
val csr_inflight = VecInit(rob.io.entries.map(e => e.valid && e.csr_write)).asUInt.orR
val id_is_csr = idu.io.out.bits.signals.wbu.csr_write
val csr_stall = id_is_csr && (csr_inflight || !id_s1_rdy)
// → 并入 ren_block
```

| | 行为 |
|--|------|
| 非 CSR | **不受** inflight 影响（仍可 OoO） |
| 多条 CSR | ROB 内至多一条 `valid && csr_write` |
| cpu-tests | 主体几乎无 CSR；CTE / yield 托底 |

过渡方案：完整 CSR 重命名更重；串行化足够正确。

---

## 3. CSR 源必须 ready 再 enq（`rs1_val`）

`rs1_val` **只在 enq 抓一次**，commit 不再重读 PRF：

```text
未 ready 就 enq → rs1_val 垃圾 → commit 静默写错
```

故 `csr_stall` 含 `!id_s1_rdy`（含 busy / `PhysBypassable`）。

| 指令 | 源 | enq 快照 |
|------|-----|----------|
| CSRRW | rs1 | `rs1_val`；`csr_rd1`→rd |
| CSRRS | rs1 + 旧 CSR | `rs1_val`、`csr_rd1` |
| CSR_PC | pc | `cm_bits.pc` |

`csr_rd1` 靠 `csr_inflight` 保证读时无 older 未提交写。

---

## 4. commit 写

### 4.1 源码

```scala
val cm_csr_wdata = MuxLookup(cm_bits.csr_sel, 0.U)(Seq(
  CSR_RD1 -> cm_bits.rs1_val,
  CSR_XOR -> (cm_bits.rs1_val | cm_bits.csr_rd1), // 旧常量名；CSRRS 实际是 OR
  CSR_PC  -> cm_bits.pc
))
csr.io.write.wen   := cm_fire && cm_bits.csr_write
csr.io.write.waddr := cm_bits.csr_waddr
csr.io.write.wdata := cm_csr_wdata
```

### 4.2 WBU 关掉

```scala
// core/wbu.scala
io.csr.wdata := 0.U
io.csr.wen   := false.B
io.csr.waddr := 0.U
```

禁止 `wbu.csr <> csr.write`（双驱动 / 盖 commit）。

### 4.3 irq 与 write 优先级

`core/csr.scala`：

```scala
when(io.write.wen && !io.irq) { rf(in_waddr) := io.write.wdata }
when(io.irq) {
  rf(csr_mcause) := io.irq_no
  rf(csr_mepc)   := io.irq_pc
}
```

同拍 `wen && irq` → **irq 赢**（普通写被 `!irq` 挡）。4b 保留该优先级；ecall 细节见 4c。

---

## 5. 过渡方案（本核选择）

| 方案 | 做法 | 本核 |
|------|------|------|
| A. CSR 重命名 | 物理副本 + 提交映射 | ❌ |
| B. 在飞写转发 | 旁路到后继读 | ❌ |
| C. **串行化** | `csr_inflight` 禁后继 CSR enq | ✅ |
| D. 仍 WBU 写 | 顺序核碰巧绿 | ❌ 乱序非法 |

另：**rd 仍 WBU→PRF**（经流水 `csr_rd1`），与「CSR 文件 @commit」分离。顺序核常仍同拍 wb+commit；差异在写口必须走 commit。

---

## 6. 接线步骤

1. 确认 4a：enq 已填 `csr_*` / `rs1_val`。  
2. core：§4.1 接 `csr.io.write`；删 WBU→CSR 写。  
3. WBU：`csr.wen := false.B`。  
4. 加 `csr_inflight` / `csr_stall` → `ren_block`。  
5. 确认 CSR 入队前 `id_s1_rdy`；跑单测 + cpu-tests。

---

## 7. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 只关 WBU 不填 `rs1_val` | commit wdata 错 | 回 4a |
| 无 `csr_inflight` | 后继读旧值 | §2 |
| 仍 `wbu.csr <> csr.write` | 双驱动 | 只留 commit 口 |
| stall 误伤非 CSR | IPC 掉 | 条件含 `id_is_csr` |
| 未等 `id_s1_rdy` | 偶发写错 | stall 含之 |
| 同拍 irq 盖 CSR | 知优先级 | `wen && !irq` |

---

## 8. 验收（自勾）

- [ ] 能说明 CSR 写点只在 commit（`cm_fire && cm_bits.csr_write`）  
- [ ] WBU `csr.wen` 恒 0  
- [ ] 能画 `csr_inflight` 时序；知 `rs1_val` 须 ready 再 enq  
- [ ] 知 irq 优先于 `write.wen`  
- [ ] 单测 + 35/35（或当前基线）  
- [ ] 知本档 **未** 做：ecall@commit、fencei@head、ebreak@commit、外部中断  

---

## 9. 下一步

→ [05c](05c_阶段4c_异常提交化.md) ecall/异常 @head；其后 05d–05g。

---

## 附录 A：旧 WBU vs 4b

| 旧 | 4b |
|----|-----|
| `WBU: csr.wen := csr_write && valid` | `WBU: csr.wen := false` |
| wdata ← 流水 | `cm_csr_wdata` ← ROB |
| 无 inflight | `csr_inflight \|\| !id_s1_rdy` |
| 写点 ≈ 写回 | 写点 = **提交** |

---

## 附录 B：时序草图

```text
T0: csrw enq → csr_inflight=1
T1: 后继 CSR @ID → csr_stall → 不 enq
Tk: head 且 done → cm_fire → csr.wen=1 → valid 清 → inflight=0
Tk+1: 后继可 enq；ID 读到新值
wrong-path: 已 enq 被杀 → valid=0 → 永不 wen
```

---

## 附录 C：代码锚点

| 文件 | 内容 |
|------|------|
| `core/core.scala` | enq；`csr_inflight`/`csr_stall`；`csr.io.write` |
| `core/wbu.scala` / `core/csr.scala` | `wen:=false`；`wen && !irq` |
| `core/idu.scala` / `unit/rob.scala` | 组合读；`ROBEntry` CSR 字段 |
| `common/consts.scala` | `CSR_RD1` / `CSR_XOR` / `CSR_PC`；其中 `CSR_XOR` 是历史命名，CSRRS 运算仍为 OR |

验收锚点：**架构 CSR 翻转边沿 = `commit_fire` 边沿**。
