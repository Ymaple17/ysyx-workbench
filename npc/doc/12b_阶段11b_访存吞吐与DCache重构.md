# 阶段 11b：访存吞吐与 DCache 重构

## 学习导航
- **理论目标**：分清 load queue 深度、cache 命中延迟、cache 服务率、miss-level parallelism 和 store 可见性，理解为何“非阻塞 cache”不能只加一个 MSHR 参数。
- **最小实现**：先让 DCache hit response 支持同拍出入，再打通 burst refill；把 DCache 扩到合理 line/容量/相联度并实现 store-hit 更新与连续 store 合并，最后才增加 MSHR 和 LQ 深度。
- **当前参考核**：阶段 10m 为 512B direct-mapped DCache、8B line、1-entry MSHR、4-entry LQ、16-entry StoreBuffer；可 hit-under-miss，但外部数据事务仍基本串行，连续 hit 和连续 store 都有服务率瓶颈。本章 RTL 尚未实现。
- **后续扩展**：最小实现稳定后可做 2-4 个 MSHR、same-line miss merge、LQ8、write-back/write-allocate；乱序 load 越过未知 store 仍是独立的高风险扩展。
- **验收方式**：单测连续 hit、miss merge、burst `last`、store-load forwarding、同 line store 合并、flush stale response 和 MMIO；再以 hit rate、replay、SB full、总线利用率及 IPC 做 A/B。

---

## 1. 当前访存墙

当前数据侧测量：

| 指标 | 数值 | 判断 |
|------|-----:|------|
| DCache access | `65554` | 有足够样本 |
| hit / miss | `46729 / 18825` | hit rate 仅 `71.28%` |
| DCache busy replay | `54040` | 服务端拒绝/重放明显 |
| StoreBuffer full | `41278` | store drain 是提交墙之一 |
| LQ full | `7761` | 有压力，但不是第一顺位 |
| committed stores | `50776` | 每条仍形成独立外部写 |

源码结构解释了这些数字：

```text
DCache: 64 sets * 8B, direct-mapped = 512B
         1 MSHR, refill 由两个 32-bit 单拍读串行完成
         hitRespValid 未清空时不能接下一 hit

StoreBuffer: 16 entries
             每条 store 独立发 AW/W/B
             enqueue 和 drain 都使匹配 cache line 失效

Xbar/SRAM: 一次只推进一个 data transaction
           SRAM 当前单拍返回并置 rlast
```

这说明问题不只是 cache 太小，而是从 hit response 到外部总线的整条服务链都缺吞吐。

---

## 2. 先定义三种“快”

| 概念 | 问题 | 本章目标 |
|------|------|----------|
| Hit latency | 一次命中多久返回 | 可以仍为 1 拍流水 |
| Hit throughput | 能否每拍接一个命中 | **必须做到 1 hit/cycle** |
| Miss concurrency | 能同时挂几次 miss | 在 burst 和仲裁正确后做到 2-4 |

一个 1-cycle hit cache 若只能隔拍接请求，吞吐仍是 0.5 load/cycle。一个 4-MSHR cache 若总线只能单事务串行，也只是把等待搬到 MSHR 表里。

---

## 3. 11b-1：连续 hit 每拍接收

当前 response buffer 应改成标准弹性规则：

```scala
val respLeave = hitRespValid && io.cpuResp.ready
val respCanAccept = !hitRespValid || respLeave
val acceptHit = cpuHit && respCanAccept

when (acceptHit) {
  hitRespBits := newResp
  hitRespValid := true.B
}.elsewhen (respLeave) {
  hitRespValid := false.B
}
```

请求侧只有在本拍真的能生成并保存 response 时才 `ready`。连续命中的理想时序：

```text
N:   accept load A
N+1: return A + accept load B
N+2: return B + accept load C
```

定向测试必须包含 response 下游 backpressure，证明旧 response 未消费时新请求不会覆盖它。

---

## 4. 11b-2：先升级 burst 内存协议

要使用 32B cache line，先让底层真的理解 burst：

### 4.1 SRAM

- 在 AR handshake 时保存 `araddr/arlen/arsize/arburst/arid`。
- 按 beat 递增地址，直到 `beat == arlen` 才拉高 `rlast`。
- `rvalid && !rready` 时保持 `rdata/rid/rresp/rlast` 稳定。
- 越界/错误地址的已接收请求也必须返回 `SLVERR`，不能静默丢弃。

### 4.2 Xbar

- read owner 从 AR handshake 保持到 `rvalid && rready && rlast`。
- 不能看到第一个 `rvalid` 就释放 owner。
- MMIO、DCache refill 和 PTW（阶段 12）以后都应通过显式 request/response owner 仲裁。

### 4.3 DCache refill

- 一个 AR 请求声明整 line 的 beat 数。
- 每个 R beat 写 refill buffer 对应 word。
- 只在最后一个成功 beat 后原子安装 tag/data/valid。
- refill 期间发生匹配 store/invalidate 时置 `killInstall`，但仍把已接收 burst drain 完。

先把 burst 独立单测跑通，再改 cache geometry。否则 line 变大只会把 miss 惩罚乘上去；阶段 10b 的 32B line 曾因此退化到 IPC `0.3569`。

---

## 5. 11b-3：合理的 cache 几何

建议第一组可控参数：

```text
capacity: 2 KiB 或 4 KiB
line:     32 B
ways:     2
sets:     capacity / line / ways
replacement: 1-bit pseudo-LRU（2-way）
```

不要一次同时扫描容量、line 和 ways。建议 A/B 顺序：

1. burst 正确，仍保持 8B/direct-mapped，确认协议改造不退化。
2. 只改 line 到 32B，观察 miss、refill beat 和 IPC。
3. 只扩容量到 2KiB/4KiB。
4. 在容量固定时比较 direct-mapped 与 2-way。

每个点记录 access/hit/miss、conflict replacement、refill cycles、总线 beats 和 IPC。

---

## 6. 11b-4：store 不应把 cache 当旁观者

当前所有 cacheable store 都失效 DCache，再由 StoreBuffer 写穿到内存。这保证了简单正确性，但浪费局部性并制造 `SB Full`。

### 6.1 最小 store-hit update

store enqueue 后仍由 StoreBuffer 保证提交后外写，但若 DCache 当前命中：

```text
newWord = (oldWord & ~byteMask) | (storeData & byteMask)
```

直接按 byte mask 更新 cache data，不再失效该 line。这样随后 load 可从 StoreBuffer forwarding 或更新后的 DCache 得到新值。

必须处理 refill race：若同 line 正在 refill，优先由 StoreBuffer forwarding 保证正确，并阻止旧 refill 覆盖新 store 数据；更完整实现可把 store merge 进 refill buffer。

### 6.2 StoreBuffer write combining

相邻、同一自然对齐 word/line 的已提交 cacheable store 可以在 buffer 尾部合并：

```text
merge when same line/word and no ordering barrier/MMIO boundary
mergedData = byte-wise youngest value
mergedMask = oldMask | newMask
```

两条 commit lane 同拍 enqueue 时要按程序序合并，lane1 对重叠 byte 的值最终生效。MMIO、fence 前后的 store 不合并。

### 6.3 写回 cache 是后续扩展

write-back/write-allocate 能进一步减少外部写，但会引入 dirty eviction、flush/fence drain 和一致性责任。阶段 11b 最小实现先做 store-hit update + write combining；若仍无法清除 `SB Full`，再升级 write-back。

---

## 7. 11b-5：最后增加 MSHR 和 LQ

在 hit throughput、burst 和 store path 都稳定后，才把 MSHR 从 1 扩到 2-4：

| 字段 | 作用 |
|------|------|
| valid / lineAddr | miss 身份 |
| waiters | 同 line 多个 load 的 LQ identity |
| refill buffer / beat | 已收到的数据 |
| victim way / killInstall | 安装位置与失效竞态 |
| AXI id / generation | 回包路由与 flush stale guard |

同 line miss 应 merge 到已有 MSHR，不重复发 AR；不同 line miss 在总线可仲裁时并行挂起。LQ 可随之从 4 扩到 8，但每条 response 必须通过 generation+slot/ROB identity 找到原 load。

`LQ_SPECULATE_UNKNOWN_STORES` 仍保持 `false`。越过未知 older store 需要 violation detector 和精确 replay，是独立实验，不应和 cache 重构同批打开。

---

## 8. 分布式模块边界

| 模块 | 自己负责 | 不应负责 |
|------|----------|----------|
| LQ | load 身份、状态、replay、response 匹配 | cache replacement |
| SQ | 未提交 store 的地址/数据和年龄查询 | 外部写事务 |
| StoreBuffer | 已提交 store、forward、merge、drain | ROB retirement policy |
| DCache | tag/data/MSHR/refill/store-hit update | 架构提交 |
| Xbar | request owner、burst 路由、公平仲裁 | load/store 程序序 |
| core | 连接 commit/flush/ready-valid | 汇总成中央访存状态机 |

该边界允许以后替换 cache 或总线，而不重写 ROB 和 RS。

---

## 9. 正确性测试矩阵

| 场景 | 必须证明 |
|------|----------|
| 连续 hit + resp ready | 每拍一请求/一响应，无隔拍气泡 |
| 连续 hit + resp stall | response 稳定，请求不会覆盖 |
| burst refill | beat 地址、`rid/rresp/rlast` 正确 |
| refill 同 line store | 旧数据不能在 store 后重新安装 |
| SQ/SB/DCache 同址 | 返回 youngest older store 的逐 byte 合并值 |
| 两条 store 同拍提交 | 容量检查和合并按程序序 |
| flush 后迟到 R | 被 drain，但不能写 PRF/ROB/LQ 新身份 |
| MMIO | 不缓存、不合并、按提交序完成 |
| fence | 等待要求的旧 store/事务达到可见点 |

---

## 10. 性能验收

至少记录：

```text
DCache hit throughput / hit rate / misses
refill bursts / beats / miss merge
DCache busy replay
StoreBuffer full / merge count / external store writes
LQ full / outstanding high-watermark
bus read/write busy cycles
Head Wait by load/store
IPC / cycles / commits
```

本章不只看 hit rate。一个命中率提高但 miss 惩罚或总线阻塞更大的设计可能更慢；保留点必须同时改善总周期，并通过完整 difftest。

---

## 11. 验收清单（你完成后自勾）

- [ ] DCache 连续 hit 已达到 1 request/cycle
- [ ] SRAM/Xbar/DCache burst 语义通过 backpressure 和错误响应测试
- [ ] 32B line、容量和相联度经过单变量 A/B 后选出保留点
- [ ] cacheable store 支持 store-hit update 和安全 write combining
- [ ] 多 MSHR/same-line merge 在服务端变宽后接入，LQ 身份与 stale guard 正确
- [ ] `DCache Busy Replay`、`StoreBuffer Full`、load/store head wait 和总周期显著下降
- [ ] 全量 cpu-tests+difftest 与 microbench 严格回归通过

下一章：[12c_阶段11c_持续前端与有效宽度.md](12c_阶段11c_持续前端与有效宽度.md)。
