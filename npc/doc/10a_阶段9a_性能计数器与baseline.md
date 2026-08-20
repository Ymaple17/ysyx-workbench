# 阶段 9a：性能计数器与 baseline

## 学习导航
- **理论目标**：本章先理解：固定一个可复现 baseline，把 IPC、stall、分支、访存、提交等指标记录下来，后续优化只和这个基线比较。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：阶段 8 参考核已跑通；当前 microbench(test) baseline IPC 约 `0.4119`。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 8 参考核能稳定通过 difftest / cpu-tests / microbench(test)。  
**目标**：固定一个可复现 baseline，把 IPC、stall、分支、访存、提交等指标记录下来，后续优化只和这个基线比较。  
**仓库状态**：阶段 8 参考核已跑通；当前 microbench(test) baseline IPC 约 `0.4119`。

---

## 1. 为什么先做 baseline

阶段 8 之后，核已经“能乱序”，但不能靠感觉判断哪里慢。阶段 9a 的任务不是改结构，而是建立一张可靠的性能体检表。

要记录的最小指标：

- `IPC = commit instructions / cycles`
- ROB / RS / freelist / FQ stall
- BPU predictions / mispredicts / hit rate
- LSU average latency / load wait / store wait
- CDB 仲裁失败或多 FU 同拍完成冲突
- commit head 被 store / fence / exception 阻塞的周期

---

## 2. 最小实现

先复用已有统计打印，缺哪个补哪个，不要顺手改调度策略。

```scala
when(commit_fire) { commitCnt := commitCnt + 1.U }
when(rob_full_stall) { robFullCnt := robFullCnt + 1.U }
when(rs_full_stall)  { rsFullCnt := rsFullCnt + 1.U }
when(fl_stall)       { flEmptyCnt := flEmptyCnt + 1.U }
```

原则：计数器只观察，不反向影响流水线。

---

## 3. 当前参考核

当前阶段 8 参考核已知 microbench(test) 结果：

- `MicroBench PASS`
- `npc HIT GOOD TRAP at pc=0x800055f0`
- IPC 约 `0.4119`
- FQ Full 很高，BPU hit rate 约 `64%`

这说明优先怀疑前端供给和分支预测，而不是马上去做宽提交或 cache。

---

## 4. 后续扩展

- 把统计输出写成 CSV / JSON，方便多轮扫描画表。
- 加 per-benchmark 统计，避免只看 microbench 总分。
- 加差异报告：本次 IPC、stall 相对 baseline 增减多少。

---

## 5. 验收方式

- `./mill -i mychisel.compile`
- cpu-tests smoke 不回归
- `microbench mainargs=test` 通过并打印完整统计
- 把 baseline 结果写入调优记录，后续每个优化都和它比较
