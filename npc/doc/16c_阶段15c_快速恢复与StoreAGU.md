# 阶段 15c：Fast Redirect 与独立 Store AGU

## 学习导航
- **理论目标**：理解预测准确率与预测错误代价是两个独立杠杆，以及 Store 地址和 Store 数据为什么应在乱序 LSU 中分开推进。
- **最小实现**：先统计 `BRU resolve -> correct-path fetch/FQ enqueue` 延迟和 `Store RS -> SQ addr_ready` 延迟；只有预算成立才分别实现 epoch 化 Fast Redirect 和独立 Store Address Sidecar。
- **当前参考核**：BRU 已在 execute resolve 同拍驱动 IFU/FTQ/BPU recovery，并非 commit-time redirect。Fast Redirect 的理想上界仅 `1819` cycles，已跳过。独立 Store Address Sidecar 已通过三次同生成物严格运行，稳定为 `160304` cycles、`368781` commits、IPC `2.3005`，成为 Stage15 工作冻结值。
- **后续扩展**：保留恢复和 Store readiness 计数器。Store Data Sidecar 与后续 path Trace 均因整核负收益撤销；Stage15 到此冻结，六宽、双 Load 与依赖预测只作为后续可选重构。
- **验收方式**：focused Store/SQ/LQ 测试、TopMain/firtool、`load-store`、严格 difftest、同二进制三次 cycles A/B；正确性修复独立保留，性能候选必须让总 cycles 与目标 counter 同时下降。

---

## 1. 先测，不先改

新增稳定事件：

```text
redirect_resolve_count
redirect_to_fetch_cycles
redirect_to_fq_enqueue_cycles
store_issue_to_addr_ready_cycles
load_replay_unknown_store_cycles
```

判断：

- 若错误恢复平均只有 1 周期，Fast Redirect 上限太低，直接跳过。
- 若平均为 3～5 周期，减少 1 周期就可能回收接近 `BP Flushes` 的周期数。
- 若 Store 地址大多同拍 ready，独立 AGU 不值得做。
- 若老 Store 长时间占 unknown dependency，Store Address Sidecar 优先级高于继续扩大 StoreBuffer。

这些计数属于路线选择接口，保留在最终 perf 结构中；临时私有 Verilator 层次探针不进入冻结代码。

当前同镜像测量：

```text
BP Flushes                    4694
Recover Capture/FQ Done       4677 / 4677
Recover To Capture/FQ Cycles  6496 / 6496
Average Recover To FQ         1.3889 cycles
Stale response drain          1205 cycles
ICache wait                   546 cycles
Resource wait                 11 cycles
```

已完成恢复相对一周期理想值只有 `6496 - 4677 = 1819` cycles 的可回收上限；即使全部兑现，`163119` cycles 也仅约降至 `161300`，IPC 约 `2.286`。因此按本阶段的效率规则跳过 Fast Redirect 实现。

同一生成物的 Store 路径测量保持 `163119 cycles / 368787 commits / IPC 2.2608`，且得到：

```text
Store address candidate cycles       57445
Address-ready / data-wait slot-cycles 20004
Data-ready / address-wait slot-cycles  6252
Fully-ready Store blocked cycles       1220
Store issues                           49706
Dual-Load + Store-address opportunity   2806
Replay Store Wait                      25683
```

这不是可直接相加的节拍数，但它证明地址/数据耦合窗口显著大于 Fast Redirect 的 `1819` cycles 上界，因此下一项施工切换到 Store Address Sidecar。

## 2. Fast Redirect Engine：本轮不施工

当前 Core 已经完成本节最关键的一半：`mis_predict` 直接来自 BRU execute resolve，`is_bp_flush/correct_pc/bp_recover_*` 同拍送到 IFU，而 ROB/RS/SQ/LQ cleanup 使用同一恢复身份。不存在“等待分支提交后再改 PC”的固定气泡。

剩余可做的是把 blocking ICache/IFU 改成 epoch 化多请求或可取消请求，使新目标不等待旧 response；测量表明该工程的绝对上限仅 `1819` cycles，故现在不改协议。下面保留完整合同，供 Store AGU 之后重新排序时使用。

传统路径：

```text
BRU resolve
  -> 全局 recovery/flush
  -> IFU 收到 redirect
  -> 正确目标重新请求
```

快速路径：

```text
BRU resolve
  -> oldest redirect arbiter
  -> FastRedirect{targetPc, robIdx, epochNext}
  -> IFU 立即切 PC/发请求

ROB/RS/LQ/FTQ younger cleanup
  -> 下一拍按同一 robIdx/epoch 后台完成
```

Fast Redirect 只提前正确路径供给，不提前提交或删除架构状态。

### 2.1 epoch 合同

- IFU、ICache response、Trace packet、FetchBuffer 和 FTQ entry 都携带或受 fetch epoch 约束。
- redirect 接受时 epoch 递增；旧 epoch 的迟到 response 丢弃。
- 同拍多个 redirect/violation 只允许程序序最老者产生新 epoch。
- 更老 exception/interrupt 可以覆盖更年轻 branch redirect。
- 后台 cleanup 不能再次把 PC 改回旧目标。

### 2.2 组合路径边界

不允许把 `FQ.ready`、ICache combinational response 或 Store commit credit 反向接入 BRU redirect valid。必要时用一项 irrevocable redirect queue 保存目标和身份。

## 3. Store Address Sidecar

当前主路径把 Load 和 Store 地址执行放在同一 LSU issue 资源附近。新结构拆开：

```text
Load RS  -> Load AGU0/1 -> LQ -> DCache
Store RS -> Store AGU   -> SQ.addr/size/mask ready
Store data dependency   -> SQ.data ready，允许稍后到达
commit                  -> 只消费已经解析的 SQ entry
```

收益来自 Store 地址更早可见：

- Load 可以更早证明与老 Store 不冲突。
- SpecLoadTracker dependency 更早清除或触发精确 violation。
- Store 不再为了地址计算与主 Load AGU 争用。
- Store 数据未 ready 时不必继续把地址标成 unknown。

### 3.1 顺序合同

- SQ 在 rename/dispatch 时获得 ROB identity。
- Store AGU 写地址必须做 ROB identity/generation 检查，flush 后迟到结果不能污染复用槽。
- 地址 ready 与数据 ready 分开，但 commit 仍要求该 Store 的地址、数据和异常状态齐全。
- 对 Load 的 forwarding/partial mask 最终仍按物理地址和程序序判断。
- MMIO Store 只有到 ROB head 才能产生不可逆设备写。

### 3.2 已接受的最小实现

第一版不重写完整 Store 数据所有权：

1. WideRS 为“`src1_ready && !src2_ready`”的 resident Store 维护独立 `addr_issued` 状态。
2. 独立一拍 Store AGU 只计算 `base + imm`，输出 ROB identity、PC、地址和 mask。
3. SQ 增加 address-only resolve 口，只置 `addr_ready/addr`，不伪造 Store data 或 done。
4. LQ 同拍接收 Store resolve，尽早清除 unknown dependency 或触发 violation。
5. 原有完整 Store issue 稍后携带真实 data 完成 ROB/SQ 写回和提交。

这个切片先验证解除 `Replay Store Wait` 的收益；若收益成立，再决定是否把 Store data 也彻底移出通用 RS。

源码落点：

```text
unit/store_address_sidecar.scala  独立一拍 base+imm 地址流水
unit/wide_rs.scala                resident Store 地址候选与一次性发射位
unit/sq.scala                     addr_ready/data_ready 分离与地址独立写口
unit/load_queue.scala             Store resolve 与 MMIO 主/次端口调度
core.scala                        identity/flush 校验和 LQ/SQ 连接
PerfMonitor.scala + perf.cpp      稳定 issue/resolve/readiness 计数
```

### 3.3 整核 A/B 结果

行为中性的计数版本为：

```text
163119 cycles / 368787 commits / IPC 2.2608
Replay Store Wait                      25683
Address-ready / data-wait slot-cycles 20004
```

Sidecar 清理版连续三次严格运行完全一致：

```text
160304 cycles / 368781 commits / IPC 2.3005
Replay Store Wait                      24017
Address-ready / data-wait slot-cycles 17213
Store Address Sidecar issue/resolve    6869 / 6805
```

相对同源 WIP 基线减少 `2815` cycles，约 `1.73%`；相对先前 Stage15 开发比较点 `163031 / 2.2623` 也有明确正收益。`load-store` 仍精确为 `396 cycles / IPC 1.1944`，MicroBench 十项 PASS，NPC/NEMU 双端 GOOD TRAP。因此该结构保留。

Sidecar 改变时序后还暴露了一个既有 LQ 饥饿缺陷：secondary scheduler 可能选中只允许 primary service 的 MMIO Load，随后 cacheable retry 又可能长期挡住 ROB-head MMIO。修复合同是：secondary 只选 cacheable Load；ready 的 ROB-head MMIO 对 primary 有严格优先级。这个正确性修复不依赖 IPC 收益，单独保留并有 focused regression。

## 4. 分布式实现

推荐模块：

```text
StoreIssueQueue: oldest-ready address uop + local credit
StoreAGU: base + imm、mask/size、fault
SQ resolve port: {robIdx,generation,addr,mask,dataReady}
FastRedirectQueue: one-entry irrevocable redirect token
```

全局 Core 只保留 oldest recovery 和 ROB commit，避免继续扩大集中式 `Core.<init>`；连接逻辑放入独立 wiring/helper module。

## 5. 施工切片

| 步 | 内容 | 退出条件 |
|----|------|----------|
| 1 | recovery/store-address 延迟计数 | 已完成：Fast Redirect 上限 `1819`，Store addr/data wait `20004` |
| 2 | FastRedirect token + epoch | 本轮跳过：收益上界不足 |
| 3 | 前台 redirect、后台 cleanup | oldest recovery/exception/violation 测试通过 |
| 4 | StoreIssueQueue + StoreAGU | 已完成：地址可独立于 Load 和 Store data ready |
| 5 | SQ identity/flush/commit | 已完成：identity/flush、地址/数据分离、MMIO 主端口优先 |
| 6 | 分别做整核 A/B | 已完成：三次 `160304 / 2.3005`，保留 |

## 6. 性能判断

原先只按 flush 数估算的粗上限是：

```text
每次少 1 周期 -> 理论上限约 4694 cycles
每次少 2 周期 -> 理论上限约 9388 cycles
```

这是上限，不是承诺；与 FQ/ICache miss 重叠的周期不能重复计算。Store AGU 同理，以实际 `Head Wait Load`、unknown-store replay 和总 cycles 同时下降为准。

现在已有精确测量，Fast Redirect 可回收上限应使用 `1819`，而不是 `4694` 或 `9388`。Store Address Sidecar 实际回收 `2815` cycles，证明先做 Store 地址解耦的重排是正确的。这也是“先测后改”改变路线的实例。

后续最小 Store Data Sidecar 虽然让 `SQ Wait` 从 `24017` 降到 `22920`，严格周期却从 `160304` 回退到 `160377`，IPC 从 `2.3005` 降到 `2.2997`。这说明数据等待已经不是全局关键路径；候选已完整移除。15a/15d 随后也完成 Oracle 与 Trace 验证并止损，最终保留本章的 Store Address Sidecar 作为 Stage15 工作冻结点。

## 7. 验收清单（学习者自勾）

- [ ] 实现前先测过 redirect/store-address 延迟
- [ ] Fast Redirect 只提前前端，不提前架构提交
- [ ] 旧 epoch response 不进入新 FQ
- [ ] 同拍恢复选择程序序最老者
- [ ] Store 地址与 Store 数据可以独立 ready
- [ ] flush 后迟到 Store resolve 不污染复用 SQ 槽
- [ ] 每项结构单独 A/B 后才组合

下一章：[16d_阶段15d_IPC2.5冻结与自适应收敛.md](16d_阶段15d_IPC2.5冻结与自适应收敛.md)。
