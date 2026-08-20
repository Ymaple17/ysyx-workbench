# 阶段 9b：前端 / FQ / BPU 瓶颈分析

## 学习导航
- **理论目标**：本章先理解：判断 IPC 低是因为后端算不动，还是前端喂不饱、分支错太多、FQ backpressure 太重。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：阶段 8 参考核已跑通；当前统计显示 FQ Full 和 BPU miss 都值得优先检查。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：9a 已固定 baseline。  
**目标**：判断 IPC 低是因为后端算不动，还是前端喂不饱、分支错太多、FQ backpressure 太重。  
**仓库状态**：阶段 8 参考核已跑通；当前统计显示 FQ Full 和 BPU miss 都值得优先检查。

---

## 1. 先问三个问题

1. IFU 是否经常因为 FQ 满而停？
2. FQ 是否经常因为 flush / redirect 被清空？
3. 分支预测错误是否制造了大量无效取指和重命名气泡？

如果这三个问题答案都是“是”，先别急着加后端宽度。后端再宽，前端不给指令也白搭。

---

## 2. 最小实现

先补前端相关计数器：

```scala
when(fq.io.enq.valid && !fq.io.enq.ready) { fqFullCnt := fqFullCnt + 1.U }
when(!fq.io.deq.valid) { fqEmptyCnt := fqEmptyCnt + 1.U }
when(is_bp_flush) { bpFlushCnt := bpFlushCnt + 1.U }
```

然后跑同一份 microbench，比较：

- FQ full 高：取指端太积极或后端消费不均衡
- FQ empty 高：取指 / 预测 / ICache 供给不足
- bp flush 高：预测质量或 redirect 恢复节奏有问题

---

## 3. 当前参考核

当前参考核前端已经有 FetchQueue 和轻量 BPU，但不是高级预测器。阶段 9b 只做诊断和小修：

- 检查 flush 后 FQ 是否确实清干净
- 检查 redirect PC 是否及时送回 IFU
- 检查 BPU 更新是否只在 commit 后发生
- 检查 FQ backpressure 是否把 IFU 长时间压住

---

## 4. 后续扩展

- 若 BPU 确认是主瓶颈，再去阶段 10e 做 TAGE / ITAGE。
- 若取指带宽确认不足，再做宽取指和多条预测。
- 若 FQ Full 是后端消费不稳导致，回头检查 RS / CDB / commit。

---

## 5. 验收方式

- 功能回归不变：compile + unit + cpu-tests
- microbench(test) 通过
- 统计报告能说明：FQ Full / FQ Empty / BPU miss 哪个是当前主因
