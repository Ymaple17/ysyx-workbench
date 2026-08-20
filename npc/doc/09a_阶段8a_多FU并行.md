# 阶段 8a：多 FU 并行执行（超标量第一步）

## 学习导航
- **理论目标**：本章先理解：把「单执行槽（d_reg→EXU→LSU→WBU 串行）」拆成 **多执行单元**（ALU / DIV / LSU 各占一个派遣口），RS 每拍可向多个 FU 同时发射。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：**参考实现已完成**（ALU / DIV / LSU 三条执行链，单 CDB 仲裁写回）。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 6 绿（FQ 已接线）；阶段 7 建议先做一轮参数扫描拿到 baseline。  
**目标**：把「单执行槽（d_reg→EXU→LSU→WBU 串行）」拆成 **多执行单元**（ALU / DIV / LSU 各占一个派遣口），RS 每拍可向多个 FU 同时发射。  
**仓库状态**：**参考实现已完成**（ALU / DIV / LSU 三条执行链，单 CDB 仲裁写回）。

> 这是从「乱序单发」到「超标量」的**第一刀**：不改发射宽度，先把「一条指令占死执行通路」的瓶颈拆掉。DIV 或 load 卡住时，ALU 仍能干活。

---

## 1. 原理：单 FU 的瓶颈在哪

### 1.1 现状数据通路

```text
RS ──(每拍 1 条)──> d_reg ──> EXU ──> LSU ──> WBU ──> can_wb → CDB
                          ↑ 单派遣口，同时只有 1 条在飞
```

- DIV 多拍：`d_reg` 持有到 `leave_ex`，期间 **任何** 指令不能进 EX  
- load 等 `rvalid`：同样占住 LSU，且压住后面的 store/ALU  
- `olderCtrl` 只挡分支，但**执行槽本身**就是串行的

### 1.2 多 FU 目标形态

```text
RS ── port0 ──> d_alu ──> ALU  ──┐
   └─ port1 ──> d_lsu ──> LSU  ──┼──> 仲裁/多 CDB ──> PRF/ROB.wb
   └─ port2 ──> d_div ──> DIV  ──┘
```

- 每拍可同时发射最多 **N（FU 数）** 条，彼此独立执行  
- 谁先完成谁写回：**乱序写回** 天然成立（ROB 提交仍顺序）

### 1.3 为什么这不算「多发射」

多发射（8b）指的是 **rename 每拍入队 2+ 条**；8a 只把**执行端**拆开，rename 仍 1 宽。  
这样 8a 的接线改动集中在 RS→EX 段，风险小，先把并行执行跑通。

### 1.4 为什么先执行、后发射（与 8b 的边界）

- 8b 的 2 宽 rename 会**放大后端任何结构瓶颈**：执行端若仍单槽，双发射只是把指令堆进 RS——每拍仍只有 1 条能执行，IPC 不升反增排队压力  
- 8a 单独做的风险局部：不碰 rename/RAT/freelist，DIV/LSU 内部状态机零改动，单发射下结构冲突少、好定位  
- 顺序固定 8a→8b：8b 的 RS 双 enq、ROB 双 enq 建立在「多 FU 已存在」之上，反过来会同时背两个改动的锅

---

## 2. 设计规格

### 2.1 FU 划分（教学核建议）

| FU | 接收 | 说明 |
|----|------|------|
| ALU | 算术/逻辑/跳转（非 MUL/DIV/REM） | 1–2 拍 |
| DIV | MUL/DIV/REM | 多拍，独立占用 |
| LSU | load/store（mem_valid） | 仍单 LSU，内部状态机不变 |
| CSR/其他 | 走 ALU 口 | 结果在 WBU 写 PRF |

也可按 `3 × ALU + 1 × DIV + 1 × LSU` 扩展，但**先做 3 口**：alu / lsu / div。

### 2.2 RS 多口发射

RS 当前 `issue_valid/issue_bits/issue_idx` 单口（`unit/rs.scala`：`issueOH = isOldestReady.asUInt` + `PriorityEncoder`）。8a 需要**每口一组候选 + 口间互斥**：ALU 口只收非 MUL/DIV/REM，DIV 口只收 MUL/DIV/REM，LSU 口只收 `mem_valid`；同一拍一条指令只能被一个口选走（交叉互斥）。

候选/互斥/优先级（Chisel 风格，复用现 `canIssue` 与 `isOldestReady`）：

```scala
val isMulDiv = VecInit((0 until n).map { i =>
  val c = entries(i).exu_alu_control
  (c === CTRL_MUL) || (c === CTRL_DIV) || (c === CTRL_REM) })
val aluCan = VecInit((0 until n).map(i => canIssue(i) && !isMulDiv(i)))
val lsuCan = VecInit((0 until n).map(i => canIssue(i) && entries(i).lsu_mem_valid))
val divCan = VecInit((0 until n).map(i => canIssue(i) && isMulDiv(i)))
val aluOH = oldestReady(aluCan)                 // one-hot
val divOH = oldestReady(divCan) & ~aluOH        // 互斥：被 ALU 选走则排除
val lsuOH = oldestReady(lsuCan) & ~(aluOH | divOH)
io.issue_alu_valid := aluOH.orR
io.issue_alu_idx   := PriorityEncoder(aluOH)
// div / lsu 同构；flush 门控同现 issue_valid
```

要点：
- `oldestReady` 即现 `isOldestReady`（`age(rob_idx)` 年龄比较），三路并行算，互斥只在最后一步掩码  
- one-hot 掩码保证**不可能**两口发同一条；优先级可换 round-robin，初版固定 **ALU > DIV > LSU**

### 2.3 派遣寄存器

每口沿用现有 `d_reg` 逻辑：**持有到 `leave_*`**（DIV 多拍持有，ALU/LSU 单拍）。`core.scala` 单份 `d_valid/d_bits` + `hold_ex/leave_ex` 复制三份，`packIssue` 复用：

```scala
val d_alu_valid = RegInit(false.B)
val d_alu_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))   // 每口独立
val hold_alu = d_alu_valid && !flush_d
alu.io.in.valid := hold_alu
alu.io.in.bits  := d_alu_bits
val leave_alu = hold_alu && alu.io.out.valid && alu.io.out.ready
// lsu / div 同构；flush_d 三份都接（mispred 杀 younger d_*）
```

### 2.4 结果总线（多 CDB 或仲裁）

- 多 FU 同时完成 → 写回端口可能冲突  
- **方案 A（推荐初版）**：单 CDB + 拍内优先级仲裁（ALU > LSU > DIV）；输家下一拍再写回（ROB.done 延后一拍）  
- **方案 B**：PRF 双写口 + CDB×2 + RS 双唤醒口（成本高，放到 8b 再说）

can_wb 仲裁的 Chisel 伪代码（单 CDB 赢家选取，门控沿用 `!done && !wb_young_*`）：

```scala
val leave_v = VecInit(
  leave_alu && (d_alu_bits.pdest =/= 0.U),
  leave_lsu && (d_lsu_bits.pdest =/= 0.U),
  leave_div && (d_div_bits.pdest =/= 0.U))      // 0=ALU 1=LSU 2=DIV
val winner = PriorityEncoder(leave_v.asUInt)     // 最高优先位赢家
can_wb   := leave_v.asUInt.orR && !wb_young_mis && !wb_young_flush
wb_pdest := MuxLookup(winner, 0.U)(Seq(
  0.U -> d_alu_bits.pdest, 1.U -> d_lsu_bits.pdest, 2.U -> d_div_bits.pdest))
wb_val   := MuxLookup(winner, 0.U)(Seq(
  0.U -> alu_wdata, 1.U -> lsu_wdata, 2.U -> div_wdata))
rob.io.wb_fire := can_wb
rob.io.wb_idx  := MuxLookup(winner, 0.U)(Seq(
  0.U -> d_alu_bits.rob_idx, 1.U -> d_lsu_bits.rob_idx, 2.U -> d_div_bits.rob_idx))
```

`cdb_val` 优先取赢家，RS 唤醒用赢家 pdest。输家不写 CDB：`rob.done` 仍 false，下一拍 `leave` 已结束——**输家的 done 由仲裁拍补置**（对非赢家 leave 再单独发一拍 `wb_fire`），别让 done 永远不置位。

### 2.5 leave_* 与 free_rob 多口

**禁止** `issue_fire` 弹槽（3d 铁律保持）；RS 按 rob_idx free，同拍多个 leave 各指各的 rob_idx 互不干扰。多口伪代码：

```scala
rs.io.free_rob_fire := leave_alu || leave_lsu || leave_div
rs.io.free_rob_idx  := Mux(leave_alu, d_alu_bits.rob_idx,
                       Mux(leave_lsu, d_lsu_bits.rob_idx, d_div_bits.rob_idx))
```

现 `freeByRob(i) := free_rob_fire && valid && (rob_idx === free_rob_idx)` 每次只清一个槽——三个 leave 是三个 `leave_ex`，槽位释放由 rob_idx 决定。想同拍清多个可拆成 `free_rob_valid_0/1/2 + free_rob_idx_0/1/2`，RS 各扫一遍。

---

## 3. 时序（示例）

```text
拍 T：ALU 口发射 addi，DIV 口发射 div，LSU 口发射 lw → 三条同时在飞
拍 T+3：div 完成，lw 完成 → 优先级仲裁：一个写 CDB，另一个下一拍写
```

---

## 4. 接线步骤建议

1. `unit/rs.scala`：加 3 个 `issue_*` 口（或参数化 `ISSUE_PORTS`），每口独立候选+互斥  
2. `core.scala`：`d_reg` → `d_alu/d_lsu/d_div` 三组派遣寄存器  
3. EXU/LSU 保持不动（各自 in/out Decoupled）  
4. 写回侧：三路 `leave_*` → 仲裁 → 单 CDB（或直连 PRF 双口）；`rob.wb` 用赢家  
5. `free_rob` 按各口 rob_idx；**每步跑 dummy/add；DIV 测例重点回归**（div 不再堵 add）

---

## 5. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 同拍多口选到同一条 | 一条指令发射两次 | 口间互斥掩码 |
| leave 与 free_rob 错位 | ROB head 丢指令/hang | 按 rob_idx free，不按槽 |
| 双写 PRF 冲突 | 双驱动/组合环 | 仲裁或真双口 |
| DIV 未完成被别口顶掉 | 结果错 | 每口独立 hold 到 leave |
| 分支 mispred 时多口 in-flight | 杀不干净 | flush_d 每口都接；can_wb 仍按 flush 挡 |
| 输家 done 永远不置位 | ROB head 卡死 | 仲裁拍对非赢家 leave 补置 done |
| 三口同拍 leave | free_rob_idx 打架 | Mux 优先级 + 各自 rob_idx |

---

## 6. 波形验收要点

`gtkwave` 加信号：`d_alu/d_lsu/d_div_valid`、三口 `issue_*_valid`、`*_out.valid`、`can_wb`、`rob.count / rs.count`。**三 FU 同时在飞的那一拍应看到**：

- 同一拍 `d_alu_valid=1 且 alu.io.in.fire`、`d_lsu_valid=1 且 lsu.io.in.fire`、`d_div_valid=1 且 div.io.in.fire` 三者**同时**成立，三口 `in.ready` 同时拉高  
- 之后若干拍内 `alu/div/lsu` 的 `out.valid` 依次（乱序）出现  
- 三口 `issue_*_valid` 同拍**各自**为 1，且三个 `issue_*_idx` 互不相同  
- `d_div_valid` 连续保持多拍时 `d_alu_valid` 已换下一条 → DIV 不再堵 ALU 口  
- 同拍 `can_wb` 最多 1；两路同时 leave 时输家 `done` 下一拍补置  
- `rs.count` 不长期爆满、`rob.count` 稳步增长 → 入队/发射/写回平衡

---

## 7. IPC 预期

- 期望 **IPC 提升 15%–40%**（相对阶段 7 baseline）：`mul/div` 密集提升最明显，load 密集/长依赖链提升有限，无依赖 ALU 密集仍接近 1（单发射宽度上限）  
- 测法：`statistics` 加 `PM(EVENT_ISSUE_ALL, 1.U, issue_alu_fire || issue_lsu_fire || issue_div_fire)`，用「总发射数 ÷ 周期数」对比 baseline  
- **8a 上限是每拍 1 条入队**：IPC>1 必须等 8b（多发射），不要拿 8a 硬冲 IPC>1

---

## 8. 验收（自勾）

- [ ] 能画 RS 三口发射 + 互斥  
- [ ] 能说单 CDB 仲裁 vs 双 CDB 取舍  
- [ ] div/lw 同时进行时 add 不被堵（波形/计数器）  
- [ ] cpu-tests 全绿；IPC 高于 5b baseline  

## 相关代码（改动点）

- `unit/rs.scala` — 多 issue 口  
- `core/core.scala` — 多 d_reg、leave 仲裁、free_rob  
- `core/exu.scala` / `core/lsu.scala` — 基本不动  

## 下一步

→ [09b](09b_阶段8b_多发射.md)：rename/FQ/ROB 加宽到 2 宽。

## 本仓库实现对照
这一版的参考实现已经把执行端拆成 3 条独立链，代码主要落在：

- `work/remote_repo/scala/core/core.scala`
- `work/remote_repo/scala/unit/rs.scala`
- `work/remote_repo/scala/core/exu.scala`
- `work/remote_repo/scala/core/lsu.scala`

最核心的形态是 RS 按指令类型分口发射：

```scala
val aluOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && !isLoad(i) && !isMulDiv(i))))
val divOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isMulDiv(i))))
val lsuOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isLoad(i))))
```

`core.scala` 里对应把三个 issue 结果分别送进三组派遣寄存器，像这样：

```scala
when(rs.io.issue_alu_valid) { d_alu_valid := true.B; d_alu_bits := packIssue(rs.io.issue_alu_bits) }
when(rs.io.issue_div_valid) { d_div_valid := true.B; d_div_bits := packIssue(rs.io.issue_div_bits) }
when(rs.io.issue_lsu_valid) { d_lsu_valid := true.B; d_lsu_bits := packIssue(rs.io.issue_lsu_bits) }
```

所以 8a 的要点不是“更宽”，而是“执行端不再互相卡脖子”。

最终参考核里还有一个 LSU 细节：LSU 口分成“地址计算已送出”和“访存结果写回”两段，`core.scala` 用 `d_lsu_sent` 防止同一条 load 在等待返回时被反复送进 LSU：

```scala
exu_lsu.io.in.valid := hold_lsu && !d_lsu_sent
val lsu_addr_leave = hold_lsu && !d_lsu_sent && exu_lsu.io.out.valid && exu_lsu.io.out.ready
val lsu_leave = hold_lsu && lsu.io.out.valid && lsu.io.out.ready

when(lsu_leave) {
  d_lsu_valid := false.B
  d_lsu_sent := false.B
}.elsewhen(lsu_addr_leave) {
  d_lsu_sent := true.B
}
```

这个寄存器不是性能优化，而是正确性边界：没有它，旧 load 会重复占用 LSU 输入，可能把后面的 store/load 饿死。
