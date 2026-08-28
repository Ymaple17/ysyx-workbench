# 阶段 12e：Load 关键路径与瓶颈补强

## 学习导航
- **理论目标**：理解 DCache hit、LQ identity/replay、CDB backpressure 与 ROB retirement 之间的固定拍数，学会在不破坏内存序的前提下做 response bypass。
- **最小实现**：新分配 load 在没有更老待调度项时可当拍查询 SQ/DCache；DCache hit 或 SQ/StoreBuffer forward 在没有更早 `sResult` 时直通 LQ writeback，CDB 不能接收时仍落入 `sResult`。
- **当前参考核**：LQ response/forward 直通的独立 A/B 为 IPC `1.0740`、cycles `486437`；最终核还加入 fresh allocation scheduling、弹性 EXU→LSU 地址 skid 和 store 私有完成。Stage12f 的 head-load wait 为 `17941`、LSU address refill 为 `53035`。
- **后续扩展**：多 bank DCache、LQ8、第二个真实 read transaction 和未知 store speculation 仍可研究；当前 LQ Full `1072`、DCache hit rate `97.03%`，不需要为完成阶段 12 强行扩大。
- **验收方式**：覆盖 hit/forward 直通、CDB 背压落盘、stored-result 年龄优先、flush/stale response、同周期 store/load 次序；完整 difftest 后比较 head-load wait、load latency 和 IPC。

---

## 1. 原始固定延迟

```text
cycle N:   LQ request, DCache flow hit response
edge N:    entry.state := sResult
cycle N+1: oldest sResult -> CDB
edge N+1:  PRF/Busy/ROB complete
```

当 CDB 本来空闲时，`sResult` 这一拍只是结构缓冲。直通后：

```text
response/forward -> 与已有 sResult 做年龄/优先级仲裁 -> CDB
not accepted     -> 保存 meta/data，entry.state := sResult
accepted         -> sComplete 或释放 entry
```

新分配直发再去掉前一拍固定空泡：

```scala
freshSchedValid := !residentSchedValid && io.alloc.fire && mmioCanRun
schedEntry := Mux(residentSchedValid, residentEntry, freshEntry)
```

已有 resident `sWait` 永远优先，fresh load 只使用本拍空闲调度带宽；若 SQ 依赖或 DCache ready 阻塞，分配项已经在时钟沿落入 LQ，下一拍按普通 resident entry 重试。

## 2. 必须保留的边界

- 已有 `sResult` 优先，避免新响应长期插队。
- response 的 RID generation 和 ROB/PC/pdest identity 必须匹配。
- selective flush 当拍的响应不能完成已杀 entry。
- CDB 不 ready 时响应仍要被 LQ 吸收，不能依赖总线重发。
- 未知旧 store speculation 关闭时，load 仍必须等待 SQ；直通不改变消歧。

## 3. 同周期 store 与 DCache

退休 store 在时钟沿更新 cache line。更老 store 已由 SQ/StoreBuffer 向更年轻 load 转发；更年轻 store 不能覆盖更老 load 的当前读值。因此组合 hit response 不应直接混入同拍 retire-store 数据，只有已注册的 StoreBuffer drain 可以参与 cache 端局部合并。

这个边界同时切断：

```text
load response -> CDB -> same-cycle store retire -> DCache -> load response
```

但切组合环不是唯一理由，核心理由是明确同周期架构顺序。

## 4. 瓶颈迁移

直通后要重新读取：

```text
head wait by type
load average latency / replay causes
CDB blocked
FQ empty / slot1 credit block
branch miss / ready wait
```

若 head-load 显著下降但 CDB blocked 上升，下一步才考虑第三 CDB或更强仲裁；若 FQ empty 成为第一名，则回到 12c。不能按旧基线继续堆结构。

## 5. 弹性 EXU→LSU 地址级

直通 LQ 后，地址路径仍有一个无条件寄存器：

```text
RS -> d_lsu(reg) -> EXU address -> lsu_stage(reg) -> LQ
```

`exu_lsu` 位于已注册的 `d_lsu` 后，因此 `lsu_stage` 可以改成一项 elastic skid：空且 LSU ready 时组合 flow-through；阻塞时才 capture；resident pop 的同拍允许 capture 下一项。flush/stale identity 仍在接受点检查。这样没有把 RS select 的 ready 组合回送到 LSU，避免新增环。

## 6. Store 私有完成

store 写回只携带 ROB/SQ identity、地址和数据，不产生 PRF value。继续占两个 data CDB 会制造无意义的 5→2 竞争。当前 store 从 data-CDB 候选中移除，使用 identity/flush-guarded private sideband 同时标记 ROB done、更新 SQ、释放 RS；架构可见写仍只在 ROB commit 进入 StoreBuffer。

这项改动把 CDB conflict `23832 -> 9411`，但独立 IPC 仅 `1.2073 -> 1.2080`。它作为模块边界清理保留，不宣称是最终大幅提速来源。

## 7. 最终关键路径账本

```text
Head Wait total     120998 -> 27500
Head Wait Load      101360 -> 17941
Head Wait ALU        12814 -> 9366
Head Wait Store       5416 -> 191
Head Wait Control     1408 -> 2
LSU Address Refill           53035
LQ Full                       1072
Load Replay                   1167
Mem Order Violation              0
```

源码落点：

| 文件 | Stage12e 内容 |
|------|---------------|
| `unit/load_queue.scala` | fresh schedule、response/forward bypass、backpressure result storage |
| `core/core.scala` | elastic EXU→LSU skid、store private completion 与 ROB/RS/SQ 接线 |
| `unit/rob.scala` | private completion 的 same-cycle done/commit identity bypass |
| `unit/rs.scala` | store/BRU private completion free 与 selective flush |
| `core/lsu.scala` | SQ/StoreBuffer/LQ/DCache 请求、顺序与 response identity 边界 |

## 8. 验收清单（你完成后自勾）

- [ ] 能画出原始与直通 load 时序
- [ ] 能解释 fresh allocation 为什么不会越过 resident `sWait`
- [ ] stored result、incoming response、forward 的优先级明确
- [ ] CDB 背压时响应不会丢失
- [ ] 同周期 retire store 不组合覆盖更老 load
- [ ] cpu-tests+difftest、stale/violation 检查和 microbench 全绿

下一章：[13f_阶段12f_IPC1.3验收与回归.md](13f_阶段12f_IPC1.3验收与回归.md)。
