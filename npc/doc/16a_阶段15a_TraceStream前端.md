# 阶段 15a：困难分支 Oracle 与路径型 Trace 前端

## 学习导航
- **理论目标**：理解残余困难分支、instruction-word cache、loop stream 和跨控制流 Trace 的区别，以及为什么 predictor/trace 都必须携带可恢复的动态身份。
- **最小实现**：先用提交/分支轨迹比较 local history、path history、loop phase 和 target context，只有离线 Oracle 预计可回收约 `3000` cycles 才实现通用 HardBranchHelper；随后以已验证的动态包模型为门，实现逐槽 PC 的 path Trace，而不重复 word cache。
- **当前参考核**：Stage15 当前开发参考点为 Store Address Sidecar IPC `2.3005`，不启用 committed trace。修正 ICache owner 后的真实 path Trace 仍退化到 `166195` cycles、IPC `2.2192`，已拒绝；实验模块和 focused test 只作为教学负例保留。
- **后续扩展**：15a 到此关闭，不扩大 Trace 表、不扫描置信度。banked dual-Load、macro-op fusion 和解耦多块前端均延期为冻结后的可选方向；纯 next-line prefetch 不单独施工。
- **验收方式**：Oracle 采用按时间切分和冷启动模拟，禁止读取未来结果；RTL 禁止 benchmark-PC 特化。helper 和 Trace 分别做 focused test、TopMain、cpu-test+difftest、同二进制 A/B；任一性能候选低于 `0.5%` cycles 收益且一次结构修正后仍无效就回退。

---

## 1. 已完成的 15a0 实验

15a0 使用 commit-filled、direct-mapped 的四字 word buffer：

```scala
class CommittedStreamWord extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
}
```

两轮整核 A/B：

| 候选 | stream hits | cycles | IPC | 结论 |
|------|------------:|-------:|----:|------|
| 只允许非 control 四字块 | 49168 | 167053 | 2.2078 | 中性，拒绝 |
| 允许 committed control 指令字 | 85678 | 167053 | 2.2078 | 中性，拒绝 |

原因不是 hit 太少，而是 16 KiB ICache 命中本来就能在同样时序内提供指令。commit-filled word buffer 既不能预取冷 miss，也没有跨动态控制路径，所以没有减少任何周期。

**结论**：不要继续扫 word-buffer 容量、路数或替换策略。

## 2. 困难分支离线 Oracle

当前 `4574` 次错误预测中，方向错误 `3596`、目标错误 `978`；三个最热类别合计 `2306` 次。Stage14 的 statistical corrector 已用减少 `900` 次方向错误回收 `2402` cycles，说明继续针对残余模式比扩大整张 TAGE 更值得先验证。

轨迹每条动态 control 至少记录：

```text
pc, actualTaken, actualTarget
base/tage/sc/loop/itage prediction and confidence
globalHistory, localHistory, targetPath, loopPhase
fetchEpoch, robAge, resolve-to-FQ penalty
```

离线依次比较：

1. `PC + local history`：寻找单分支自身相关性。
2. `PC + path history`：区分到达同一分支的调用/控制路径。
3. `PC + local + path + loop phase`：覆盖循环退出和嵌套模式。
4. target context：只预测间接目标，不混入方向 owner。
5. chooser：helper 仅在与现有 provider 分歧且自身置信度足够时接管。

训练/测试必须按动态时间切分，并模拟有限表 tag/index、替换、冷启动和 commit-time update。无限字典只给理论上界，不能作为施工依据。进入 RTL 的门是：有限模型在多个 MicroBench 子项上净减少足够错误，按本核实测 penalty 预计回收约 `3000` cycles；否则直接记录拒绝并转真实 Trace。

实际脚本 `scripts/stage15_branch_oracle.py` 对 `368782` 条退休 PC 做在线 replay，解码出 `52488` 条条件分支和 `7376` 条非 return JALR。所有预测都发生在当前 outcome 更新之前；256-entry、2-way、16-bit tag 的结果如下：

| 类型 | 最好模型 | coverage | accuracy | 估计 cycles | 结论 |
|------|----------|---------:|---------:|------------:|------|
| direction | local context | `78.84%` | `99.26%` | `717.3` | 低于施工门 |
| indirect target | path context | `92.10%` | `99.44%` | `1956.1` | 与现有 path-ITAGE 高度重叠 |

两个估计不能直接相加：target helper 覆盖的正是现有 ITAGE 路线，而此前有限表 `GHR XOR path` 已把 IPC 从 `2.2533` 拉低到 `2.1792`。因此 HardBranchHelper 在本轮 **Oracle 拒绝，不进入 RTL**。详细模型结果见 [stage15_branch_oracle_results.csv](stage15_branch_oracle_results.csv)；脚本保留，后续 workload 或预测器结构变化时可重跑。

候选结构保持通用：

```scala
class HardBranchEntry extends Bundle {
  val tag        = UInt(...)
  val signature  = UInt(...) // folded local/path/phase context
  val direction  = Bool()
  val target     = UInt(32.W)
  val confidence = UInt(...)
  val usefulness = UInt(...)
}
```

热点 PC 只用于解释收益，禁止写入 `pc === 0x...` 的特化条件。

## 3. 为什么 15a0 失败不否定真实 Trace

真实 Trace 改变的是动态路径包：

```scala
class TraceEntry extends Bundle {
  val valid           = Bool()
  val startPc         = UInt(32.W)
  val pathTag         = UInt(PATH_TAG_W.W)
  val length          = UInt(log2Ceil(TRACE_UOPS + 1).W)
  val lanePc          = Vec(TRACE_UOPS, UInt(32.W))
  val inst            = Vec(TRACE_UOPS, UInt(32.W))
  val controlMask     = UInt(TRACE_UOPS.W)
  val predictedNextPc = UInt(32.W)
  val exitKind        = UInt(TRACE_EXIT_W.W)
}
```

taken branch 后的下一条动态指令不一定是 `startPc + lane*4`，因此必须保存逐槽 PC。Trace 命中还必须生成本次动态实例的新 epoch 和预测身份，不能复用上次提交时已经过期的 TAGE/ITAGE metadata。

## 4. 已做的路径上限分析

提交轨迹离线建模得到：

```text
当前动态路径包平均宽度      3.2295
256-entry confidence-2 模型 3.5163
错误 trace hit             44
```

这证明跨控制流动态包存在供给空间，但不是硬件收益保证。有限 tag/index、错误 hit 恢复、FTQ lane-PC 身份和实际时序仍需付出成本。

混合 `GHR XOR path` 的有限 ITAGE 已从 IPC `2.2533` 回退到 `2.1792`，说明离线无限字典的高准确率不能直接等价为有限硬件表。

## 5. lookup 与恢复所有权

```text
fetch PC + path context
  -> trace hit: TracePacket -> FetchBuffer/FQ
  -> trace miss: existing IFU -> ICache/BPU -> FetchBuffer/FQ
```

必须满足：

- 同拍只有一个 PC owner。
- FQ backpressure 时 packet 保持稳定。
- Fast Redirect 提升 epoch 后，旧 trace/ICache response 都不能进入 FQ。
- branch resolution 仍选择程序序最老 redirect。
- trace 内 control 的 GHR/path/RAS 推测推进可恢复。
- `fence.i` 清除 trace valid，并等待旧动态 packet 被 epoch 杀死。

## 6. 真实 Trace 的进入门槛

在 HardBranchHelper A/B 后重新测：

```text
BP flush count
FQ empty 的 mutually-exclusive 原因
动态 path packet width 与有限表 wrong-hit
helper 未覆盖的 direction/target miss
trace hit 后可避免的 ICache/FQ/line-tail 等待
```

有限 256-entry confidence-2 模型已经达到平均宽度 `3.5163`、wrong-hit `44`，允许进入第一版路径 Trace 设计；但整核保留门仍是至少 `0.5%` cycles。第一版只支持一个内部 taken control 和四个逐槽 PC，先验证恢复域，不直接扩大 trace 深度。

## 7. 源码状态与计划落点

- `unit/committed_trace.scala`：实验性 builder/cache，当前命中使用路径禁用。
- `core/ifu_commit_wiring.scala`：隔离 commit feedback，避免继续扩大 `Core.<init>`。
- `core/ifu.scala`：保留原 IFU/BPU/FTQ owner。
- `CommittedTraceTest.scala`：实验协议测试保留为反例和后续 Trace 基础。
- `scripts/stage15_branch_oracle.py`：有限表在线 replay、冷启动/替换/置信度和收益报告；已完成并在 VM 重跑一致。
- `unit/hard_branch_helper.scala`：本轮不新增；Oracle 没有通过约 `3000` cycles 的 RTL 施工门。

实验源码存在不代表当前核启用了 Trace；当前性能基线以 `traceUse=false` 为准。

## 8. 整核 A/B 与拒绝结论

第一版真实 Trace 已具备逐槽 PC、fresh BPU metadata、trace-selected GHR/path 推进、FTQ lane-PC 恢复和 `fence.i` invalidate。最初版本还允许 Trace 在 IFU `s_WORK` 状态抢占未完成 ICache 请求；结构修正把 Trace 限定到 `s_IDLE`，并用断言固定单一事务 owner。修正版结果为：

| 指标 | Sidecar 参考点 | path Trace | 变化 |
|------|---------------:|-----------:|-----:|
| cycles | `160304` | `166195` | `+5891` |
| IPC | `2.3005` | `2.2192` | `-0.0813` |
| average fetch width | `3.1557` | `3.3552` | `+0.1995` |
| trace hits | `0` | `34055` | `+34055` |
| target miss | `978` | `2021` | `+1043` |
| BP flush | `4582` | `5633` | `+1051` |
| FQ Full | `9951` | `15701` | `+5750` |
| Fetch Wait Resource | `11582` | `16498` | `+4916` |

`CommittedTraceTest 4/4`、TopMain、`load-store` 与完整 MicroBench difftest 均通过，说明这是性能拒绝而不是功能失败。Trace 提高了局部供给宽度，却用更多错误目标和队列背压抵消收益；已经用完一次有明确原因的结构修正机会，因此按 Stage15 合同回退，不再扫描 entry 数、路数或 confidence。

## 9. 公开设计依据

- [Branch Prediction Is Not a Solved Problem](https://arxiv.org/abs/1906.08170)：残余错误集中在少数困难分支，单纯扩大预测器容量的边际收益有限。
- [XiangShan V3 FTQ](https://docs.xiangshan.cc/projects/design/en/kunminghu-v3/frontend/FTQ/) 与 [ICache](https://docs.xiangshan.cc/projects/design/en/kunminghu-v3/frontend/ICache/)：BPU/FTQ 可在正式取指前运行并驱动预取，前端以显式队列和恢复身份解耦。
- [Trace Cache](https://american.cs.ucdavis.edu/academic/readings/papers/s01_3.pdf)：跨动态基本块供给需要记录路径，而不是只缓存顺序指令字。

这些来源说明结构方向，不提供本核 IPC 承诺；本核只接受自身严格 A/B。

## 10. 验收清单（学习者自勾）

- [ ] 能解释为什么 85678 次 word hit 仍不省周期
- [ ] Oracle 没有读取未来结果，有限表结果与无限字典上界分开
- [ ] HardBranchHelper 没有 benchmark-PC 特化
- [ ] 能根据 `717.3/1956.1` 的预算解释本轮为何跳过 helper
- [ ] 能区分指令字缓存与动态路径 Trace
- [ ] trace lane PC 可以跨 taken branch
- [ ] 新动态实例不复用旧 predictor metadata
- [ ] epoch 能杀死旧 trace/ICache packet
- [ ] 能根据剩余周期预算决定是否继续 Trace
- [ ] 能根据 `166195/2.2192` 解释为什么命中率和包宽提升不等于整核提速

下一章：[16b_阶段15b_写回式双端口L1D.md](16b_阶段15b_写回式双端口L1D.md)。
