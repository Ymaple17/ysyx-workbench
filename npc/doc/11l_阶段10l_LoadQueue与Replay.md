# 阶段 10l：Load Queue 与 Replay

## 学习导航
- **理论目标**：分清 Load Queue、MSHR 与 replay 的职责，建立多条 load 在飞、精确内存依赖和错误路径回收的完整模型。
- **最小实现**：先加入带 generation 的 LQ 和保守 store 依赖；再加入可重放的瞬时失败；最后选做“越过未知地址 store”与违例恢复。
- **当前参考核**：**已实现并保留 4-entry LQ/replay**。`LSU_MLP_ENABLE=true`，AXI response ID 为 `generation + lqIdx`；阶段 11d 又加入新请求同拍 cache response 匹配。`LQ_SPECULATE_UNKNOWN_STORES=false`，因此默认不越过未知 older store。10l 的 IPC `0.4743` 仍是阶段快照。
- **后续扩展**：多 MSHR、同 line miss merge、选择性 replay、store-set 预测、总线多 outstanding。
- **验收方式**：先证明 stale response、flush、转发和违例恢复正确，再看 outstanding load 数、replay 原因、hit-under-miss 和 IPC。

> MSHR 记录“哪条 cache line 正在 miss/refill”；LQ 记录“哪些动态 load 仍在飞”；replay 处理“这条 load 本次没能安全完成”。三者不能互相代替。

---

## 1. 10i 为什么有 MSHR 仍不够

10i 已让 DCache 在一个 miss 未完成时服务已命中的 line，但 LSU 没有能够长期保存多条 load 状态的队列。直接允许新 load 发射会带来：

- 响应回来时找不到可靠的动态指令身份；
- 端口忙、依赖未决或 CDB 冲突时没有统一重试位置；
- ROB/LQ 索引复用后，旧响应可能写脏新指令；
- 未知地址 older store 与 younger load 的违例无法检测和恢复。

因此性能实验只有 2 次 hit-under-miss，却增加了 head wait 和错误路径压力。10l 先补正确性容器，再谈 MLP。

---

## 2. LQ entry 与身份

参考核文件是 `scala/unit/load_queue.scala`。核心 entry 为：

```scala
class LoadQueueEntry(indexWidth: Int) extends Bundle {
  val valid = Bool()
  val generation = UInt((4 - indexWidth).W)
  val meta = new LSU_WBU_IO
  val addr = UInt(32.W)
  val memRd = UInt(3.W)
  val state = UInt(2.W)
  val bypassedStores = UInt(OoOParams.ROB_SIZE.W)
}
```

对外请求 ID 至少包含 `lqIdx + generation`：

```scala
io.dmem.arid := Cat(entry.generation, lqIdx)
```

当前总线 `ARID/RID` 宽度固定为 4 bit，所以源码令 `generationWidth = 4-indexWidth`，并限制 LQ 深度不超过 8。若扩 LQ 或改变 AXI ID 宽度，应从总线参数推导 tag 宽度，而不是继续硬编码常数 4。

ROB/PC/pdest 身份保存在 `meta` 中，返回路由由 `generation + lqIdx` 找到动态 LQ 项。响应只有在 generation、slot、entry valid 和 `sWaitResp` 状态同时匹配时才能写回，否则静默 drain，并计入 stale counter。

---

## 3. 生命周期

```text
RS/EX 地址生成后进入 LSU
  └─ LSU 输入握手时分配 LQ entry，记录 robIdx/pc/destPhys/地址
LQ schedule
  └─ 查询 SQ/StoreBuffer
      ├─ 可完整前递：形成完成结果
      ├─ 必须等待 older store：replayPending
      └─ 可访问 cache：带 LoadTag 发请求
response
  └─ 校验 lqIdx + generation + robIdx
      ├─ 正常：送 CDB，CDB accepted 后 completed
      └─ stale：丢弃
CDB accepted
  ├─ 保守模式：立即释放 entry，generation++
  └─ 未知 store 投机模式：进入 sComplete，保留到 ROB commit
flush
  └─ 杀掉年轻 entry；generation 递增使旧响应失效
```

注意：当前参考核的 LQ 实例位于 `core/lsu.scala` 内，不是在 rename/dispatch 时预分配；只有 load 完成地址生成并被 LSU/LQ 输入接受后才占 LQ。DCache 返回也不等于 load 完成，只有结果被 CDB 接收、PRF/ROB 的目标身份仍匹配后，才能进入上述释放/完成状态。

---

## 4. 分三档实现

### 4.1 10l-a：LQ + 保守依赖

第一档不允许 load 越过地址未知的 older store：

```text
存在 older store && !store.addrValid → load 等待
所有 older store 地址已知：
  完整覆盖 → 从最新 older store 前递
  部分覆盖 → 等待或走合并路径
  无重叠   → 访问 DCache
```

这一档已经能让多条彼此独立的 load 在 LQ 内等待不同响应，同时避免内存违例检测的复杂度。

### 4.2 10l-b：统一 replay

把“暂时做不了”统一编码成 replay 原因：

```scala
object ReplayReason extends ChiselEnum {
  val none, olderStoreUnknown, partialForward,
      dcacheBusy, mshrFull, responseBusy, cdbBusy = Value
}
```

replay 仲裁建议在 LQ 内做 oldest-ready，而不是把所有 load 塞回通用 RS。这样地址、依赖 mask 和 cache 请求身份都留在访存域，控制更分布式。

每次失败只设置 `replayPending`；下一次资源 ready 时重新发请求。不要在一个周期内组合地自旋。

### 4.3 10l-c：越过未知 store 与违例恢复（实验开关）

这是可选高阶档。允许 load 越过地址未知的 older store 后，当 older store 地址生成时，SQ 对已执行的 younger load 做 CAM 比较：

```text
older store 地址解析
  → 找到 younger && completed/issued 的 load
  → byte mask 有重叠
  → 该 load 当时没有从此 store 前递
  => memory-order violation
```

最小正确恢复不是局部修补 PRF，而是：

```text
记录最老违例 load
从该 load 的 PC 做 recovery/flush
杀掉它和所有年轻指令
重新取指执行
```

等这条路径稳定后，再考虑选择性 replay。对教学核，整段恢复更容易与 RAT/checkpoint/ROB 的既有 flush 语义统一。

---

## 5. SQ、StoreBuffer 与部分前递

load 查询顺序应是：

```text
先查未提交 SQ，再查已提交但未 drain 的 StoreBuffer，最后查 DCache
```

同地址存在多条 older store 时，按字节选择最新 older 数据。若当前实现不支持多来源 byte merge，最小策略是：

- 单条 store 完整覆盖 load mask：前递；
- 多条或部分覆盖：等待相关 store 提交/drain 后 replay；
- 绝不能拿部分新数据加无验证的 cache 旧数据。

MMIO load 不进入普通 LQ 投机路径，继续序列化到 ROB head 和总线空闲边界。

---

## 6. MSHR 与多 outstanding

推荐扩展顺序：

1. LQ 可容纳多条 load，但 DCache 仍 1-entry MSHR；
2. 允许 hit-under-miss，响应都带 `LoadTag`；
3. 增加多个 MSHR；
4. 相同 line 的 miss merge 到同一 MSHR waiter 列表；
5. 总线支持多个 outstanding ID 后，再并发发 refill。

多个 LQ entry 不意味着多个 cache miss 能并行；多个 MSHR 也不意味着总线能乱序返回。每层都要独立定义容量和 ready/valid。

---

## 7. 与 ROB、CDB 和 flush 的接口

更大设计可把 LQ 前移到 dispatch 分配，并拆开 issue/cache 接口；下面是这种扩展接口，**不是当前 `load_queue.scala` 的逐字段签名**：

```scala
class LoadWriteback extends Bundle {
  val tag = new LoadTag
  val data = UInt(32.W)
  val exception = UInt(EXC_W.W)
}

class LoadQueueIO extends Bundle {
  val alloc = Flipped(Vec(OoOParams.DISPATCH_WIDTH, Decoupled(new LqAlloc)))
  val issue = Flipped(Decoupled(new LqIssue))
  val dcacheReq = Decoupled(new DCacheReq)
  val dcacheResp = Flipped(Decoupled(new DCacheResp))
  val wb = Decoupled(new LoadWriteback)
  val commit = Input(Vec(OoOParams.COMMIT_WIDTH, new CommitInfo))
  val flush = Input(new FlushInfo)
}
```

关键不变量：

- entry 未分配时绝不接受对应响应；
- 同一动态 load 只向 CDB 成功写回一次；
- flush 后的旧响应不能改 PRF、BusyTable 或 ROB；
- LQ 可以乱序完成；未知-store 投机模式保留到 ROB commit，当前保守模式在 CDB 验收后即可提前释放，因为不再需要违例跟踪；
- load fault 记录进 ROB，到 head 才精确触发。

---

## 8. 必做测试

| 用例 | 要证明什么 |
|------|------------|
| miss 后跟独立 hit | hit-under-miss 真正发生 |
| 两条 load 响应反序 | 写回身份正确，提交仍按序 |
| DCache/MSHR/CDB 忙 | replay 后只完成一次 |
| SQ 完整前递 | 不访问 DCache |
| 部分重叠 store/load | 等待或正确 byte merge |
| flush 后旧响应返回 | stale drain，不污染新指令 |
| LQ/ROB index 绕回 | generation 防止 ABA |
| younger load 越过 unknown store | 地址冲突后触发 recovery |
| MMIO load | 不投机、不 merge |

额外加入随机延迟内存测试，让响应次序和延迟每次变化，避免只在固定一拍时序上“恰好正确”。

---

## 9. 计数器与验收

建议计数：

```text
LQ_ALLOC / LQ_FULL / LQ_HIGH_WATER
LOAD_OUTSTANDING_MAX
LOAD_REPLAY_TOTAL
REPLAY_STORE_UNKNOWN / DCACHE_BUSY / MSHR_FULL / CDB_BUSY
LOAD_FORWARD / PARTIAL_FORWARD_WAIT
MEM_ORDER_VIOLATION / VIOLATION_FLUSH
STALE_LOAD_RESP
MSHR_MERGE / HIT_UNDER_MISS
```

验收顺序：

```bash
cd $NPC_HOME/oood_chisel_vsrc
./mill -i mychisel.compile
./mill -i mychisel.test.testOnly unit.OoOUnitTest
cd $NPC_HOME
scripts/stage9_regress.sh --mode cpu --tag stage10l_cpu
scripts/stage9_regress.sh --mode microbench --tag stage10l_mlp
```

先要求所有身份、flush、forward、fault 定向测试通过，再打开 MLP 做 A/B。若 outstanding 增加但 IPC 下降，要检查 head wait、LQ full、replay 风暴和总线利用率，不能仅凭“多条在飞”验收。

---

## 10. 相关源码与建议改动点

| 文件 | 作用 |
|------|------|
| `scala/unit/load_queue.scala` | LQ entry、round-robin retry、generation response、stale guard 和 violation compare |
| `scala/core/lsu.scala` | 地址生成、LQ/SQ/DCache 接线 |
| `scala/unit/sq.scala` | older store mask、forward、violation compare |
| `scala/unit/store_buffer.scala` | 已提交 store 的前递查询 |
| `scala/unit/dcache.scala` | 10l 当时为 1-entry MSHR；11b 已增加 secondary slot、same-line merge 与同拍 hit response |
| `scala/unit/rob.scala` | load exception、commit/free、recovery |
| `scala/core/core.scala` | flush/CDB/ROB 身份接线 |
| `scala/common/ooo_params.scala` | 当前 `LQ_SIZE` 与功能开关；增加多 MSHR 时再引入 `MSHR_NUM` |
| `scala/unit/PerfMonitor.scala` | replay/violation/outstanding 计数 |
| `scala_test/unit/OoOUnitTest.scala` | LQ/SQ/DCache 定向与随机延迟测试 |

### 当前参考核边界与实测

保留配置是：

```scala
val LSU_MLP_ENABLE = true
val LQ_SIZE = 4
val LQ_SPECULATE_UNKNOWN_STORES = false
```

因此“多条 load 在 LQ 中等待/完成、命中越过 miss、带身份重放”是当前能力；“load 越过未知地址 store 后发现冲突”已有 compare/recovery 接口，但默认参数不走该投机路径。不要把实验硬件存在写成默认策略已经启用。

| 指标 | 10k | 10l |
|------|-----|-----|
| microbench IPC | `0.4565` | `0.4743` |
| cycles | `1144512` | `1101938` |
| LQ high water | - | `4` |
| load replay | - | `49739` |
| mem-order violation | - | `0`（保守配置） |

---

## 11. 验收清单（自勾）

- [ ] 能分清 LQ、MSHR、replay 各自保存什么；
- [ ] stale response 不能写入复用后的 ROB/LQ entry；
- [ ] load 可乱序完成但仍随 ROB 按序退休；
- [ ] 部分 store-forward 不会拼出未经验证的数据；
- [ ] 若实现越过未知 store，违例 recovery 定向测试通过；
- [ ] MLP-on/off 都跑过完整 correctness 与 IPC A/B。

下一章：[11m_阶段10m_持续双取指与FTQ.md](11m_阶段10m_持续双取指与FTQ.md)。
