# 阶段 2b：ROB 元数据（映射仍恒等）

## 学习导航
- **理论目标**：本章先理解：ROB 正确 enq / wb / commit / flush；**数据通路保持 2a 恒等**。
- **最小实现**：在恒等映射下接通 ROB enq、同身份 writeback、head commit、flush 截断及同拍计数，保持原顺序数据通路。
- **当前参考核**：阶段 10m 的 ROB 已扩为 32 项、双入队/双写回/双提交；本章只建立其最小生命周期和指针不变量。
- **后续扩展**：2c 打开写目的物理寄存器分配、`old_phys` 回收和 flush 重建。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：2a 绿。  
**目标**：ROB 正确 enq / wb / commit / flush；**数据通路保持 2a 恒等**。  
**本阶段最大坑**：StageConnect 幽灵指令、flush 与 commit 交互。

---

## 1. 原理：为什么先「只记账」

ROB 在完整 OoO 里既管提交，也管冲刷回收。先让它：

- 与 ID→EX 握手同拍入队  
- 与 WBU 同拍（顺序下）标记 done 并提交  
- mispred 时截断 tail  

而不去动 freelist，避免「指针错 + 回收错」双爆。

---

## 2. 目标结构

```text
IDU 译码 → idu_ren（含 rob_idx=enq_idx, pdest=arch）
            ↓ enq_fire
           ROB
            ↑ wb/commit @ WBU（顺序：wb_idx 可先绑 head）
IF/ID/EX 冲刷 + rob.flush
```

`rename.fire` 仍为 0。

---

## 3. 入队字段（最小集）

```scala
rob.io.enq_fire := en_id_ex  // idu_ren.fire && !exu.flush
enq.pc, inst, reg_write, reg_write_sel, arch_rd
enq.new_phys := id_pdest     // 仍是 arch
enq.old_phys := arch_rd
enq.issued   := true.B       // 顺序：入 EX 即已发射
enq.mem_*, is_ebreak, ...
// 建议同时写入 jump, bp_*，供 3a BPU@commit（可先写上）
```

`idu_ren.bits.rob_idx := rob.io.enq_idx`（= 当前 tail）。

**满**：`idu_ren.valid` 与 `out.ready` 均需 `!rob.full`；`is_stall` 含 `rob.full`。

---

## 4. 写回与提交（顺序简化）

2b 可用：

```scala
rob.io.wb_fire     := wb_fire
rob.io.wb_idx      := rob.io.head   // 顺序核：WBU 一定是最老
rob.io.commit_fire := wb_fire
```

这在 **顺序发射** 下正确，但 **不是** 最终语义。3a 要改成 `wb_idx=rob_idx` 且 `commit_fire=commit_valid`。

ROB 内部已支持：`commit_valid = head.valid && (done || wb_is_head)`。

---

## 5. 冲刷

```scala
mis_predict 在 EX 组合算出
mis_predict_r = RegNext(mis_predict)     // 与 correct_pc 延迟对齐
is_bp_flush = mis_predict_r
flush_idx = mis_rob_r   // RegEnable(exu.rob_idx, mis_predict)
// irq：flush_idx = head-1，清空在飞
rob.io.flush := is_bp_flush || is_irq
```

保留 flush_idx 指向的项（分支本身），清其后。

---

## 6. StageConnect：必须清 valid 寄存器

### 6.1 错误写法

```scala
prev.ready := next.ready
next.bits  := RegEnable(prev.bits, ..., prev.valid && next.ready)
next.valid := RegEnable(prev.valid, false.B, prev.ready) && !flush
```

问题：`flush` 只 **mask 输出**，不清除内部 valid 寄存器。  
若 flush 当拍 `ready=0`（下游 stall），寄存器仍保持 valid=1，flush 结束后 **幽灵指令** 进入下游。

### 6.2 正确写法

```scala
val valid = RegInit(false.B)
prev.ready := next.ready
when(flush) {
  valid := false.B
}.elsewhen(prev.ready) {
  valid := prev.valid
}
next.valid := valid && !flush
next.bits  := RegEnable(prev.bits, 0.U.asTypeOf(prev.bits),
                        prev.valid && next.ready && !flush)
```

### 6.3 典型故障现象（add）

```text
循环尾 bne @ 0x98
REF  fall-through 到 0x9c
DUT 跳过 0x6c，直接 commit 0x70  → PC MISMATCH
```

根因：错误路径上的 load 等在 flush 后复活提交。  
用 commit 轨迹打印每个 commit PC 可快速确认。

---

## 7. ROB flush 与 commit 同拍

```scala
// 错误：when(flush) { 只改 tail/count } 忽略 commit_fire
// 结果：软件以为提交了，head 没动 → count 膨胀 → full
```

正确：flush 分支内仍处理 `do_cm`，count 按 **commit 后 head..flush_idx** 重算。

---

## 8. 与流水冲刷策略

顺序核常：

- IF/ID/EX：mispred 冲  
- LSU/WBU：**仅 irq 冲**（更老指令应提交）

引入 ROB 后，错误路径若已 enq，靠 **ROB flush + StageConnect 清 valid** 杀掉；  
不要在 WBU 把错误路径当 commit（3a 门控后更严）。

---

## 9. 验收（自勾）

- [ ] 能画 enq/commit/flush 后 head/tail/count  
- [ ] 理解幽灵指令与 StageConnect 修复  
- [ ] dummy/add 绿  
- [ ] 能解释为何 2b 仍用恒等 pdest  


---

## 附录 A：commit 轨迹调试（推荐）

在 `cpu.cpp` 临时打印：

```cpp
if (wbu_valid) printf("[C] pc=%08x\n", commit_pc);
```

add 循环尾应看到：

```text
... 98 → 6c → 70 → ...  或最后 98 → 9c
```

若出现 `98 → 70` 跳过 `6c`，就是幽灵/冲刷问题。

---

## 附录 B：ROB 指针手算练习

初始 head=tail=0, count=0。

1. enq A,B,C → tail=3, count=3, head=0  
2. commit A → head=1, count=2  
3. flush_idx=1（保留 B）→ 清 C，tail=2, count=1（仅 B）  

若步骤 2 与 3 同拍：先 commit 再按新 head 算 kept。

---

## 附录 C：`en_id_ex` 与 flush

```scala
en_id_ex = idu_ren.valid && idu_ren.ready && !exu.io.is_flush
rob.enq_fire = en_id_ex
```

误预测当拍若仍 enq，错误路径进入 ROB，靠下一拍 flush 清掉。  
更严设计可 `!mis_predict` 也门控 enq（可选）。
