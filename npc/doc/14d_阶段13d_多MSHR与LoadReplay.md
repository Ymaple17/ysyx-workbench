# 阶段 13d：多 MSHR 与 Load Replay

## 学习导航
- **理论目标**：理解多 MSHR 只有在下游能区分多个 outstanding read 时才产生真正 MLP，并用画像判断是否值得增加身份复杂度。
- **最小实现**：保留 Stage11 的一个 active request 加一个 secondary slot、LQ identity/replay 与 same-line merge；Stage13 只把 DCache 从 64 sets 扩到 128 sets。
- **当前参考核**：DCache 4 KiB、32B line、direct-mapped；Stage13f 命中率 `98.02%`、LQ full `422`、load replay `755`、memory-order violation `0`。没有多 MSHR bank。
- **后续扩展**：可并行 AXI ID 的 MSHR bank、banked DCache、双 AGU、store-set predictor、write-back/2-way L1D。
- **验收方式**：secondary request、same-line merge、flush/reuse、未知 older store、StoreBuffer forward 和 replay 定向测试；零 stale response 与零 memory-order violation。

---

## 1. 当前请求模型

```text
active request      正在访问 DCache/AXI 的 load
secondary request   active 被占用时保留一个不同请求
same-line waiter    refill 后复用同一 line，避免重复 miss
LQ identity         lq slot + generation/ROB identity
```

LQ scheduler 在本地重试 DCache busy、older-store wait 和 CDB busy，不由 core 顶层状态机逐条指挥。

## 2. 为什么本阶段没有实现 MSHR bank

当前 SRAM/xbar 路径仍是单 read owner，AXI 事务没有为多个 DCache miss 建立可乱序返回的独立 owner/ID 协议。在这个边界下，多 MSHR entry 主要增加排队容量，不能让多个 refill 真正并行。

Stage13f 最终回归中：

```text
DCache accesses       51590
DCache hits/misses    50568 / 1022
DCache hit rate       98.02%
secondary alloc       1303
same-line merge       871
LQ full/replay        422 / 755
```

因此先保留已有 secondary/replay 协议，并用 4 KiB 容量把严格 IPC 从 `1.4943` 提到 `1.5134`。多 MSHR 留到总线和 cache bank 能共同支持时再做。

## 3. 完整多 MSHR 应该怎样做

后续 MSHR entry 至少需要：

```text
valid, line_addr, axi_id, refill_state
waiters[] = { lq_slot, generation, rob_identity, word_offset }
pending_store_conflict
```

同 line miss 合并 waiter，不同 line 占不同 entry；response 只按完整身份唤醒 LQ。flush 后旧 response 只排空，不能写入已经复用的 LQ/ROB 槽。

## 4. 验收清单（学习者自勾）

- [ ] 能区分 secondary queue 和真正并行 MSHR
- [ ] same-line waiter 按完整身份合并
- [ ] flush 后旧 response 只排空不写回
- [ ] youngest older store forwarding 正确
- [ ] 容量调整同时改善命中率和严格 cycles

下一章：[14e_阶段13e_分布式完成网络.md](14e_阶段13e_分布式完成网络.md)。
