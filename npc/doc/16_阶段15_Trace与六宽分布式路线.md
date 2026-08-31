# 阶段 15：IPC 2.3005 测量冻结与后续结构路线

## 学习导航
- **理论目标**：理解从 IPC `2.2` 继续提升时，困难分支、动态路径供给、Store/Load 服务率和退休密度如何共同限制性能，并学会用离线 Oracle 与整核 A/B 共同修正路线。
- **最小实现**：保留 Stage14 冻结核和每个 Stage15 正收益点；先用提交/分支轨迹证明候选上限，再做小模块、focused test、整核 A/B 和回退，不为了章节完整强留负收益结构。
- **当前参考核**：Stage15 checkpoint 冻结于独立 Store Address Sidecar：`160304` cycles、`368781` commits、IPC `2.3005`；同生成物三次严格运行一致，Trace 默认关闭。
- **后续扩展**：HardBranchHelper 和真实 path Trace 均已拒绝，双 Load、macro-op fusion、解耦多块前端与六宽留作后续可选升级。IPC `2.5/3.0` 是预算目标，不再是本阶段完成条件。
- **验收方式**：MicroBench test 十项 PASS、NPC/NEMU 双端 GOOD TRAP、无 mismatch/HANG/ABORT；同二进制三次结果一致。train 规模作为补充验证单独记录：独立 NEMU 已以 `66,172,980` 条指令到达 GOOD TRAP，NPC 长测已取消且没有 IPC，不替换 test 冻结基线。

---

## 1. 当前周期预算

当前工作二进制以 `368781` commits 为例：

```text
Stage15 工作冻结点    160304 cycles / IPC 2.3005
未来 IPC 2.5000 预算  147513 cycles
未来仍需减少           12791 cycles，约 8.0%
未来 IPC 3.0000 预算  122927 cycles
```

四宽不是当前硬上限：平均 fetch width 为 `3.1557`，有提交周期的平均退休宽度约为 `2.77`。主要损失仍来自错误路径与供给空洞、Store/Load replay 和 cache/writeback 互锁；`99.50%` ICache 命中率同时说明纯 next-line miss 预取的独立预算很小。

## 2. 新路线

| 子阶段 | 内容 | 当前状态 | 退出条件 |
|--------|------|----------|----------|
| [15a](16a_阶段15a_TraceStream前端.md) | 困难分支 Oracle、HardBranchHelper 与真实路径 Trace | helper 已跳过；Trace 为 `166195 / 2.2192`，拒绝并默认禁用 | 作为负例保留协议测试，不再扩大表或调置信度 |
| [15b](16b_阶段15b_写回式双端口L1D.md) | victim FIFO、write-back L1D、dirty eviction skid | victim FIFO 已保留；true-L1/skid 未转正，暂缓 | 仅在 Load/bank counter 再次证明预算时重开 |
| [15c](16c_阶段15c_快速恢复与StoreAGU.md) | Fast Redirect 测量 + 独立 Store AGU | 已接受 IPC `2.3005` | Fast Redirect 因上界低跳过；Store Sidecar 三次复现并保留 |
| [15d](16d_阶段15d_IPC2.5冻结与自适应收敛.md) | branch Oracle -> path Trace -> 自适应止损 | 已在 IPC `2.3005` 冻结，Trace 为负 | 结果账本、三次复现、失败候选完整回退 |
| [15e](16e_阶段15e_六宽分布式后端与IPC3.md) | 2x3 cluster 六宽后端 | 延期可选 | 重启性能路线且四宽确实饱和时再施工 |

MMU/SV32 和多核一致性继续留在 Stage16，不与性能收敛混在一起。

## 3. 测量驱动合同

每个性能候选依次经过：

```text
计数/离线模型给出可回收周期
  -> focused protocol test
  -> TopMain/firtool
  -> cpu-test + difftest
  -> 同二进制 MicroBench A/B
  -> repeat
  -> keep / one structural follow-up / revert
```

规则：

1. 正确性修复必须保留，不受 IPC 门槛限制。
2. 纯性能候选没有明确可回收周期时，不进入长时间整核构建。
3. 候选第一次为负，只允许一次针对已定位抵消项的结构补强；仍为负就回退。
4. 参数、容量和阈值扫描不能替代结构归因。
5. 发现更高收益且适合当前核的方法时，先修订本章和对应子章，再改变实施顺序。
6. 讲义描述的是可验证合同，不是不可修改的任务清单。

## 4. 当前 A/B 结论

- commit-filled word stream 即使有 `85678` hits，cycles 仍与 Stage14 完全相同；它只是重复已命中的 ICache 指令字，已拒绝。
- confidence-zero PC-tagged 间接目标 fallback 将 IPC 提高到 `2.2533`，保留。
- 可变 Store owner 与不可变 AXI snapshot 分离后达到 IPC `2.2623`，这是当前 Stage15 开发基线。
- true-L1 显著降低写回流量和 Store backpressure，但 refill install 仍受 dirty victim 写回 credit 影响；修正 MMIO 和 Store hit 后为 IPC `2.2608`，继续只做一个 eviction-skid 补强。
- 继续扩大 ITAGE/mixed history 已证实会增加有限表 alias，已拒绝。
- dirty-victim skid 整核 cycle-neutral；恢复延迟理想上界只有 `1819` cycles，因此两项都已止损。
- Store Address Sidecar 将地址与数据 readiness 分离，三次稳定达到 `160304 / 2.3005`，相对同源工作点减少 `2815` cycles，已保留。
- Store Data Sidecar 虽将 `SQ Wait` 从 `24017` 降到 `22920`，总周期却回退到 `160377`；等待已经移出关键路径，候选已完整撤销。
- 困难分支有限表 Oracle 已完成：direction helper 最好约 `717` cycles；target helper 约 `1956` cycles 且与现有 path-ITAGE 重叠。两者均未通过 `3000` cycles 独立施工门，不新增 HardBranchHelper RTL。
- 真实 path Trace 修正了 ICache outstanding owner 后通过 `CommittedTraceTest 4/4`、TopMain、`load-store` 和完整 difftest，但严格 MicroBench 为 `166195 / 368825 / 2.2192`。它命中 `34055` 次并把平均 fetch width 提到 `3.3552`，同时 target miss `978 -> 2021`、BP flush `4582 -> 5633`、FQ Full `9951 -> 15701`、Fetch Wait Resource `11582 -> 16498`，比参考点多 `5891` cycles，已拒绝并默认禁用。
- Sidecar 改变时序后暴露的 LQ MMIO secondary/primary 饥饿属于正确性缺陷，已用 cacheable-only secondary 和 ROB-head MMIO primary priority 修复，不与性能候选绑定。

详细结果见 [stage15_completion_results.csv](stage15_completion_results.csv)。

## 5. 冻结后的提高方向

当前 Sidecar 参考点：

```text
BP Flushes             4582
FQ Empty              11705
Replay Store Wait     24017
Head Wait Load         4054
Wait Store Commit      7006
StoreBuffer Full       3654
CDB Conflicts          1838
```

恢复延迟已证明不是当前大预算，Store Data Sidecar 的严格 A/B 也证明剩余 SQ data wait 不在全局关键路径。有限表 Oracle 没有通过 helper 施工门，而真实 Trace 虽提高包宽，却把错误目标和队列压力推到关键路径。离线 `44` 次 wrong-hit 没有覆盖在线替换、同拍训练/查询和后续预测器状态扰动，不能继续当作整核收益保证。若以后重新启动性能优化，建议顺序为：

```text
互斥周期归因和 train/ref 规模复测
  -> 解耦多块 BPU/FTQ/ICache，减少 FQ 空洞
  -> banked dual-Load + 双 MSHR 接收 + Load replay/service 重构
  -> Store Sets/依赖预测，减少 unknown-store 轮询
  -> 动态指令对足够时再做 macro-op fusion
  -> 供给与访存不再主导后，才评估 2x3 cluster 六宽
```

不能把热点 PC 硬编码进 RTL；热点只用于选择通用上下文特征。不要重新扩大本轮已失败的 Trace 表；纯 next-line prefetch 也不单独施工，因为 test 规模仅 `678` 次 ICache miss。

公开设计与论文依据见 15a：香山 V3 的 BPU/FTQ/ICache 解耦、SonicBOOM 的双 Load 服务，以及残余 TAGE 错误和动态 Trace 的研究。Trace 在本核的负结果说明公开结构只能提供候选，不替代本核同二进制 A/B。

## 6. 分布式控制边界

适合分布：FU-local issue、Store AGU、L1D bank/victim owner、completion credit、前端 epoch 检查。

必须保留唯一排序点：ROB 程序序、oldest redirect/exception、architectural rename map、Store 对外可见顺序、MMIO/fence 边界。

Fast Redirect 可以先恢复前端，但不能让后台清理选择一个比当前最老 redirect 更年轻的恢复点。

## 7. 验收清单（学习者自勾）

- [ ] 能从实际 commits 推导 IPC `2.5` 的 cycles 门
- [ ] 能区分冻结参考、开发基线和未接受候选
- [ ] 能解释为什么 15a0 失败不等于真 Trace 无效
- [ ] 能解释为什么热点 Oracle 可以指导通用预测器、但不能产生 PC 特化 RTL
- [ ] 能说明 Fast Redirect 前台恢复与后台清理的 epoch 合同
- [ ] 能说明 Store Data Sidecar 为什么局部计数下降仍可能整核负收益
- [ ] 能按 A/B 结果修改路线并回退负收益结构

下一章：[16a_阶段15a_TraceStream前端.md](16a_阶段15a_TraceStream前端.md)。
