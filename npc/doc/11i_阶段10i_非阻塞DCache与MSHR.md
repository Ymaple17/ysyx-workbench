# 阶段 10i：非阻塞 DCache 与 MSHR

## 学习导航
- **理论目标**：理解 blocking DCache 为什么会让 load miss 拖住后端；理解 MSHR、hit-under-miss、多 outstanding load、load replay 分别解决哪一层问题。
- **最小实现**：实现 1-entry MSHR，让 DCache 在一个 cacheable miss/refill 未完成时仍可服务已命中的 cache line；LSU 侧加入返回 tag 和 stale 身份保护。
- **当前参考核**：10i 的 active MSHR 与 hit-under-miss 保留；阶段 12f 包含 1 个 secondary miss slot、same-line merge、同拍 hit response，并使用 LQ4 generation identity/replay/bypass。这里仍不是多个可并行 refill 的完整 MSHR 表；本章 MLP-on 退化的是“有 MSHR、无完整 LQ”的历史实验。
- **后续扩展**：load queue / replay 与更精确的响应身份已在 10l 完成；当前仍可继续做 miss merge、多 MSHR、store-set/选择性 replay 和总线 burst/多 outstanding。
- **验收方式**：`./mill -i mychisel.compile`、`OoOUnitTest`、cpu-tests smoke、`microbench(test)` 全绿；比较 `DCache Hit Under Miss`、`Head Not Ready`、`Bus Wait Cycles`、IPC，而不是只看“有没有 MSHR 结构”。

---

## 1. 为什么不是一打开 MLP 就变快

blocking DCache 的问题很直观：

```text
load miss 期间 cache 忙
后续无关 load 即使命中也进不来
ROB head 如果等 load，commit 会停住
```

所以 10i 先做了一个 1-entry MSHR：cacheable miss 被记录在 MSHR 里，DCache refill 后再把结果返回；若此时 CPU 又访问一个已经在 cache 中的 line，DCache 可以直接返回 hit，这就是最小的 hit-under-miss。

但乱序核里“DCache 能 hit-under-miss”不等于“整个访存系统能 MLP 提速”。如果 LSU 太早释放 issue 槽，却没有完整 load queue / replay / 依赖重放策略，就可能让更多错误路径或低价值访存进入系统，反而增加前端压力和 head wait。本阶段的实测正好说明了这一点。

---

## 2. 本阶段源码改动

### 2.1 DCache：1-entry MSHR + hit-under-miss

`unit/dcache.scala` 从 blocking 状态机改成两个响应缓冲加一个 MSHR：

```scala
val hitRespValid = RegInit(false.B)
val missRespValid = RegInit(false.B)

val mshrValid = RegInit(false.B)
val mshrCacheable = RegInit(false.B)
val mshrAddr = RegInit(0.U(32.W))
val mshrId = RegInit(0.U(4.W))
val mshrFillIdx = RegInit(0.U(wordW.W))
val mshrFillLine = RegInit(VecInit(Seq.fill(words)(0.U(32.W))))
```

关键行为：

```scala
val canAcceptHit = cpuHit && !hitRespValid
val canAcceptMissOrBypass = !mshrValid && !missRespValid
io.cpu.arready := Mux(cpuHit, canAcceptHit, canAcceptMissOrBypass)
```

含义是：

- cache hit 只需要 hit response buffer 为空，即使 MSHR 正在 refill 也可以接收。
- cache miss / MMIO bypass 仍然只能占用 1-entry MSHR，所以它不是多 MSHR。
- StoreBuffer enqueue/drain 的双 invalidate 继续保留；如果 invalidation 命中正在 refill 的 line，`mshrKilled` 会阻止旧 line 安装。

### 2.2 LSU：返回 tag 和 stale response 防护

`core/lsu.scala` 增加两个 load response metadata slot：

```scala
private val loadSlots = 2
val loadValid = RegInit(VecInit(Seq.fill(loadSlots)(false.B)))
val loadMeta = Reg(Vec(loadSlots, new LSU_WBU_IO))
val loadMemRd = Reg(Vec(loadSlots, UInt(3.W)))
val loadAddr = Reg(Vec(loadSlots, UInt(32.W)))
```

发读请求时用 AXI `arid` 记录 slot，返回时用 `rid` 找回元数据：

```scala
io.dmem.arid := allocSlot
val respSlot = io.dmem.rid(loadSlotW - 1, 0)
val respSlotValid = io.dmem.rvalid && loadValid(respSlot)
val staleResp = io.dmem.rvalid && !loadValid(respSlot)
io.dmem.rready := (respSlotValid && io.out.ready) || staleResp
```

这不是完整 load queue。它只解决“一个 miss 外加少量返回身份”的问题；真正 MLP 还需要 replay、load violation 检查、miss merge 等机制。

### 2.3 Core：LSU stage 变成一项 skid queue

10i 调试时踩到一个很典型的坑：LSU 前面那一级 stage 如果只靠 `ready` 更新，可能在 load 已经提交后还残留旧的 `rob_idx/pc`。当 ROB head 前进后，这个旧 load 会被 SQ 年龄判断误认为还要等更老 store，形成死锁。

最终 core 做了两层保护：

```scala
val lsu_stage_stale = lsu_stage_valid &&
  (!lsuStageRob.valid || (lsuStageRob.pc =/= lsu_stage_bits.pc))
val lsu_stage_done = lsu_out_fire && lsu_stage_valid &&
  (lsu.io.out.bits.rob_idx === lsu_stage_bits.rob_idx) &&
  (lsu.io.out.bits.pc === lsu_stage_bits.pc)
val lsu_stage_pop = lsu_req_accept || lsu_stage_done || lsu_stage_stale
```

`d_lsu_valid` 也做同样的 ROB 身份保活：

```scala
val d_lsu_stale = d_lsu_valid &&
  (!dLsuRob.valid || (dLsuRob.pc =/= d_lsu_bits.pc))
```

这条经验很重要：乱序核里不要只相信一个小索引。ROB index 会复用，跨 flush / commit / response 边界时最好带上 `pc` 或更完整的 identity。

### 2.4 10i 阶段默认策略开关

`common/ooo_params.scala` 增加：

```scala
val LSU_MLP_ENABLE = false
```

10i 阶段默认 `false` 时，core 仍保持保守单 outstanding LSU 行为：

```scala
val lsuCanOverlap =
  if (OoOParams.LSU_MLP_ENABLE) true.B
  else (!lsu_stage_valid && !lsu.io.bus_busy)
```

这让 10i 的硬件骨架留在代码里，但当时的保留点不吃性能回退。10l 加入完整 LQ/replay 后已经重新打开 `LSU_MLP_ENABLE=true`。

---

## 3. 扫描结果

扫描表见 [stage10i_scan_results.csv](stage10i_scan_results.csv)。

| 配置 | 结果 | IPC | DCache Hit Under Miss | Head Not Ready | 结论 |
|------|------|-----|-----------------------|----------------|------|
| `LSU_MLP_ENABLE=true` | PASS | `0.3837` | `2` | `620317` | 功能正确，但 IPC 明显低于 10h，拒绝作为默认 |
| `LSU_MLP_ENABLE=false` | PASS | `0.4342` | `0` | `549753` | 保留默认；基本贴近 10h 的 `0.4343` |

解释：

- MLP-on 只有 2 次真实 hit-under-miss，收益几乎没有。
- MLP-on 同时让 `FQ Full`、`Head Not Ready`、BPU miss/flush 压力变差，说明更激进的 LSU 发射把系统推向更多错误路径/排队压力。
- MLP-off 保留了 DCache/MSHR 代码和单测，但参考核性能不退化，适合作为讲义继续往 10j 的稳定水位。

---

## 4. 验收记录

2026-08-21 在 VM `/home/qiu/ysyx-workbench/npc` 验收：

- `./mill -i mychisel.compile`：PASS
- `./mill -i mychisel.test.testOnly unit.OoOUnitTest`：47/47 PASS
- cpu-tests smoke：`dummy`、`add`、`add-longlong`、`bit`、`load-store`、`shift`、`string` PASS
- `microbench mainargs=test`：PASS，`npc HIT GOOD TRAP at pc=0x800055f0`
- 10i retained 默认：`LSU_MLP_ENABLE=false`，microbench IPC `0.4342`；当前 10m 默认为 `true`

---

## 5. 验收清单（自勾）

- [ ] 能讲清 blocking DCache、1-entry MSHR、hit-under-miss、多 MSHR 的区别。
- [ ] 能指出为什么 DCache 支持 hit-under-miss 不等于整核 IPC 一定上升。
- [ ] 能解释 stale response / ROB index 复用为什么要做 identity guard。
- [ ] 能用 `stage9_regress.sh --mode quick --tag xxx` 跑出 CSV，并比较 `LSU_MLP_ENABLE=true/false`。

下一章：[11j_阶段10j_完整TAGE与ITAGE.md](11j_阶段10j_完整TAGE与ITAGE.md)。
