# 阶段 10a：独立 SQ 与 StoreBuffer

## 学习导航

- **理论目标**：理解 store/load 消歧和 store 提交解耦：load 不能读旧值，但 store 也不应该一直卡住 ROB head 等总线 B 响应。
- **最小实现**：把 Stage8d 的 ROB-backed `StoreQueue` 改成独立状态表；普通 PMEM store 在 commit 时进入 4 项 `StoreBuffer`，之后由 buffer 后台按 FIFO 写总线。
- **当前参考核**：**已完成 Stage10a**。`StoreQueue` 已不再读取 `rob.io.entries`，而是在 dispatch/wb/commit/flush 接口下维护自己的 store 表；StoreBuffer 已接入 commit、load forwarding 和 AXI 写出。
- **后续扩展**：更大/可配置 SQ，SQ commit 数据直接驱动 StoreBuffer、store 合并、更大 StoreBuffer、DCache 写合并、非阻塞 cache/MLP。
- **验收方式**：`./mill -i mychisel.compile`、`unit.OoOUnitTest`、cpu-tests smoke、`microbench mainargs=test`；重点比较 `Wait Store Commit`、`StoreBuffer Full/Enq/Drain/Fwd` 和 IPC。

---

**前置**：阶段 8d 已跑通 load-store 消歧；阶段 9 已确认 `Wait Store Commit` 很高。  
**目标**：让未提交 store 由独立 SQ 消歧，让已提交但未写完的 store 进入 StoreBuffer，减少 ROB/store/LSU 的耦合。  
**仓库状态**：Stage10a 已接线并通过回归。

---

## 1. 为什么先做 10a

Stage9 推荐点的 microbench 数据里：

```text
IPC               = 0.4122
Wait Store Commit = 215845
```

这说明很多周期 ROB head 是 store，而且地址/数据已经算出，却还要等 AXI 写事务完成后才能提交。StoreBuffer 的价值是把这两件事拆开：

```text
提交点：store 进入 StoreBuffer，ROB 可以继续退休后续指令
写总线：StoreBuffer 按 FIFO 顺序慢慢发 AW/W，等 B 后出队
```

独立 SQ 的价值则是把“未提交 store 的地址/数据/掩码”从 ROB 表中拆出来：

```text
Stage8d: ROB entries -> StoreQueue 组合扫描
Stage10a: dispatch/wb/commit/flush -> StoreQueue 自己维护 store 表
```

这样 RS 只需要看 SQ 给出的 unresolved-store bitmask，load 只需要查 SQ 的 wait/forward 结果。

---

## 2. 独立 SQ 的实现

当前参考核采用 **ROB-indexed independent SQ**：SQ entry 的物理索引仍等于 ROB idx，所以不需要额外的 SQ rename 指针；但 SQ 已经有自己的寄存器表，不再读 `rob.io.entries`。

```scala
class StoreQueueEntry extends Bundle {
  val valid = Bool()
  val addr_ready = Bool()
  val addr = UInt(32.W)
  val data = UInt(32.W)
  val mask = UInt(4.W)
}
```

SQ 接口按生命周期分成四类：

```scala
val alloc0_valid = Input(Bool())
val alloc0_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
val alloc0_mask  = Input(UInt(4.W))
val alloc1_valid = Input(Bool())
val alloc1_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
val alloc1_mask  = Input(UInt(4.W))

val wb_valid = Input(Bool())
val wb_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
val wb_addr  = Input(UInt(32.W))
val wb_data  = Input(UInt(32.W))
val wb_mask  = Input(UInt(4.W))

val commit_valid = Input(Bool())
val commit_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))

val flush     = Input(Bool())
val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
val flush_all = Input(Bool())
```

核心接线要点：

```scala
rs.io.rob_st_pending := sq.io.unresolved_mask

sq.io.alloc0_valid := en_ren && id0_is_store
sq.io.alloc0_rob   := rob.io.enq_idx
sq.io.alloc0_mask  := idu.io.out.bits.signals.lsu.mem_wmask(3, 0)

sq.io.wb_valid := can_wb && rob.io.entries(wb_idx).valid &&
  rob.io.entries(wb_idx).mem_valid && rob.io.entries(wb_idx).mem_write
sq.io.wb_rob  := wb_idx
sq.io.wb_addr := wbu.io.in.bits.alu_result
sq.io.wb_data := wbu.io.in.bits.store_data

sq.io.commit_valid := cm_fire && cm_is_store
sq.io.commit_rob   := rob.io.head
```

load 查询规则：

```text
older store 地址未知                  -> wait
older store 同 word 且 partial cover  -> wait
older store 同 word 且 full cover     -> forward
多个 older store 命中                 -> 取最年轻的 older store
```

这一步完成后，RS 的 load blocker 不再扫描 ROB 项；StoreQueue 也不再从 ROB 表读 store 地址/数据做消歧。

> 当前为了降低改动风险，ROB 里仍保留 `mem_addr/mem_wdata/addr_ready` 作为 commit 兼容镜像，StoreBuffer enqueue 仍从 ROB head 的 commit 数据取值。下一步可以让 SQ 在 commit 时直接输出 store entry，再进一步瘦身 ROB。

---

## 3. StoreBuffer 实现

新增 `unit/store_buffer.scala`：

```scala
class StoreBufferEntry extends Bundle {
  val addr = UInt(32.W)
  val data = UInt(32.W)
  val mask = UInt(4.W)
}
```

模块内部是一个 4 项 FIFO：

- `enq`：commit 阶段把已提交 PMEM store 放入 buffer
- `dmem`：后台按 FIFO 发 AXI AW/W/B
- `ld_*`：给 LSU 的 load 查询接口

普通 store 的 commit 条件从“B 响应回来”改成：

```text
普通 PMEM store：head 地址/数据 ready 且 StoreBuffer 可入队
MMIO store：继续走直接写状态机，等 B 响应后才提交
```

MMIO/串口输出仍保持老语义，不会被普通 StoreBuffer 延迟破坏。

---

## 4. load 同时查 SQ 和 StoreBuffer

```text
未提交 older store：查 StoreQueue
已提交但未写完 store：查 StoreBuffer
```

优先级是：

```text
StoreQueue wait      -> load 等
StoreQueue forward   -> 用 StoreQueue 数据
StoreBuffer wait     -> load 等
StoreBuffer forward  -> 用 StoreBuffer 数据
否则                 -> 发内存读
```

因为 SQ 里的 store 一定比 StoreBuffer 里的 store 更年轻，所以 SQ 前递优先级更高。

当前总线仍是单事务模型：

```text
LSU read 与 StoreBuffer write 不并发
direct MMIO write 优先级高于 StoreBuffer write
StoreBuffer 后台写时，LSU 暂停发新 AR
```

以下提交会等 StoreBuffer drain：

- `fence.i`
- `mret`
- `ebreak`
- 已记录异常的 head 指令

这样控制流/仿真结束点不会越过尚未写出的已提交 store。

---

## 5. 性能计数器

新增事件：

```text
StoreBuffer Enq
StoreBuffer Drain
StoreBuffer Full
StoreBuffer Fwd
```

`perf.cpp` 会在 `[Writeback / Commit Bottlenecks]` 和 `[LSU Statistics]` 中打印这些指标。`scripts/stage9_regress.sh` 的 microbench CSV 也已扩展这些列。

---

## 6. 回归结果

Stage10a 在 VM 上通过：

```text
./mill -i mychisel.compile
unit.OoOUnitTest: 35/35
cpu-tests: dummy, add, add-longlong, bit, load-store, shift, string
microbench mainargs=test: PASS
```

microbench 对比：

| 指标 | Stage9 推荐点 | Stage10a 独立 SQ + StoreBuffer |
|------|---------------|--------------------------------|
| IPC | `0.4122` | `0.4320` |
| Total Cycles | `1267124` | `1209428` |
| Commit Instructions | `522194` | `522522` |
| Wait Store Commit | `215845` | `41834` |
| FQ Full | `302451` | `224886` |
| Head Not Ready | `425522` | `533416` |
| CDB Conflicts | `23999` | `32855` |
| StoreBuffer Full | 无 | `40264` |
| StoreBuffer Enq / Drain | 无 | `50784 / 50784` |
| StoreBuffer Fwd | 无 | `1613` |

解释：

- StoreBuffer 成功把 store commit 等待大幅压低，IPC 提升到 `0.4320`。
- 独立 SQ 没改变本轮 microbench 的 IPC，但把消歧状态从 ROB 拆出，为后续真正 LSQ、宽提交和 DCache 做准备。
- `StoreBuffer Full=40264` 说明 4 项 buffer 已经会满，后续可以扫 8 项或与 DCache 写合并。
- `Head Not Ready` 和 `CDB Conflicts` 上升，说明 store 瓶颈缓解后，压力转移到写回/提交/前端等其他位置。

---

## 7. 后续扩展

已经完成：

```text
dispatch 分配 SQ entry
store 写回填 SQ addr/data/mask
load 查 SQ 表
store commit 清 SQ entry
flush/selective flush 清 SQ entry
store commit 入 StoreBuffer
StoreBuffer 后台写出
```

还可以继续做：

- SQ commit 直接输出 store entry，StoreBuffer enqueue 不再依赖 ROB 的 store 元数据镜像
- `SQ_SIZE` 从 ROB_SIZE 中独立出来，并加入容量/满停顿处理
- StoreBuffer 扩到 8/16 项并做参数扫描
- DCache 写合并或 write combining
- 10b 的非阻塞访存层次，把 StoreBuffer 后台写和 load miss 更好地并行起来

---

## 8. 验收（自勾）

- [ ] 能说明 StoreQueue 和 StoreBuffer 的职责区别
- [ ] 能画 dispatch->SQ、store wb->SQ、load query SQ、commit->StoreBuffer 的路径
- [ ] 能解释为什么 MMIO store 不走普通 buffer
- [ ] 能解释 `Wait Store Commit` 下降后为什么其他瓶颈会冒出来
- [ ] 跑通 compile、OoOUnitTest、cpu-tests smoke、microbench(test)

## 相关代码

- `common/ooo_params.scala` — `SQ_SIZE`、`STORE_BUFFER_SIZE`
- `unit/sq.scala` — 独立 StoreQueue 状态表、load wait/forward、flush/commit 清理
- `unit/store_buffer.scala` — StoreBuffer FIFO、AXI 写出、load 查询/前递
- `core/core.scala` — SQ 生命周期接线、store commit ready、MMIO direct path、dmem 仲裁、load 查询合并
- `unit/PerfMonitor.scala` / `oood_chisel_csrc/include/perf.h` / `oood_chisel_csrc/cpu/perf.cpp` — Stage10a 计数器
- `src/test/scala/unit/OoOUnitTest.scala` — StoreQueue 与 StoreBuffer 单测
- `scripts/stage9_regress.sh` — microbench CSV 扩展

## 下一步

→ [11b](11b_阶段10b_访存层次与非阻塞缓存.md)：访存层次与非阻塞缓存。
