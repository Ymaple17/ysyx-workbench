# 阶段 10m：持续双取指与 FTQ

## 学习导航
- **理论目标**：理解“接口宽度为 2”和“平均每拍持续供给接近 2 条”是两回事；用 fetch block、缓冲和 FTQ 管理预测元数据与恢复。
- **最小实现**：先解耦 ICache→FetchBuffer→FQ，再支持稳定双指令 block 与 cache-line 边界；最后加入 FTQ 和 speculative history 精确恢复。
- **当前参考核**：本章作为阶段 10m 已严格收官，历史结果为两次 IPC `0.6381`、平均取指宽度 `1.3858`、`FTQ Stale Recover=0`；阶段 12f 在其上继续增加同拍 CDB wake/select、访存关键路径、FQ/FetchBuffer credit、BRU 和 path/loop recovery。10m 结构均保留，但不再是仓库最终水位。
- **后续扩展**：更宽 fetch block、uop/fetch-block cache、多 bank ICache、解码队列、loop buffer。
- **验收方式**：定向验证 taken mask、跨 line、backpressure、redirect 和 FTQ 回滚；性能上同时看有效取指宽度、FQ 压力、错路入队、redirect latency 与 IPC。

> 10h 旧后端曾证明：只把阈值放宽到 `space=6` 会让 IPC 从 `0.4343` 降到 `0.3902`。10m 保留同一个 credit 数字，但前提已经改变：FetchBuffer/FTQ 解耦、LQ/replay、FU 同拍 refill 和双整数 ALU 已全部接入。因此“参数值相同”不代表“设计点相同”。

---

## 1. 持续供给链

前端吞吐由整条链的最窄处决定：

```text
next-PC / predictor
  → ICache request
  → fetch block response
  → predecode + predicted mask
  → FetchBuffer
  → FQ
  → 2-wide decode/rename
```

10m 的入口基线中，packet 虽能带两条指令，但 slot1 只有在同一可用 cache line、预测允许且 FQ 几乎为空时出现。任一 backpressure 都直接顶回 ICache/PC，导致“偶尔取两条，长期仍近似单取”。

### 当前参考核的数据流

```text
ICache response
  ├─ 原子分配 FTQ entry（idx + generation）
  └─ 写入 2-entry FetchBuffer
          ↓
      2-wide FetchQueue
          ↓
      decode / rename / ROB / RS
          ↓
  ftqIdx + generation 随动态指令一直到执行恢复与提交释放
```

`fetchBuffer.io.in.fire` 必须与 `ftq.io.alloc.fire` 同拍相等，源码用 assertion 固定这条不变量。完整 flush 会清 FetchBuffer/FQ/FTQ；普通分支恢复则先用 `ftqIdx + generation` 读取 snapshot，只截断目标 block 之后的年轻 FTQ 项，保留仍可能提交的更老 snapshot。

---

## 2. 先加 FetchBuffer 解耦

参考核在 ICache 与 FQ 之间使用 2-entry packet buffer：

```scala
class IFU_IDU_IO extends Bundle {
  val inst = UInt(32.W)
  val pc = UInt(32.W)
  // 省略 state / bp_* 字段
  val ftq_idx = UInt(FTQ_PTR_W.W)
  val ftq_generation = UInt(FTQ_GEN_W.W) // 当前为 8 bit
}

class IFUPacket extends Bundle {
  val valid = Vec(FETCH_WIDTH, Bool())
  val bits = Vec(FETCH_WIDTH, new IFU_IDU_IO)
}
```

FetchBuffer 实际缓存 `IFUPacket`；`basePc/predictedNextPc/cfiSlot/GHR/RAS` 属于同拍原子分配的 FTQ entry，不在 packet 中复制。

数据流保持标准 ready/valid：

```text
ICache response 只有在 FetchBuffer ready 时被消费
FetchBuffer bits 在 valid && !ready 时保持稳定
FQ 一次接收 PopCount(validMask) 条
redirect 时清空 FetchBuffer/FQ 错误流，并由 FTQ 选择性截断年轻 generation
```

有缓冲后，ICache 响应和 FQ 短暂满不再同拍耦合。此时才能重新扫描水位，而不是直接把 `WIDE_FETCH_MIN_SPACE` 当作最终架构。

---

## 3. fetch block 的有效槽规则

两条 32-bit 指令的基础规则：

```text
slot0 pc = basePc
slot1 pc = basePc + 4

slot0 invalid → slot1 必须 invalid
slot0 predicted taken → slot1 invalid
slot0 not taken && slot1 valid → 可同时入队
slot1 predicted taken → 两条都入队，nextPc = slot1 target
均不 taken → nextPc = basePc + 8
```

预测优先级：

1. redirect/exception/mret/fence.i；
2. slot0 taken；
3. slot1 taken；
4. 顺序 `basePc+8`。

不要让两个槽分别更新 PC；它们共同产生一个 `predictedNextPc`。

---

## 4. 跨 cache-line 取指

当前 line 尾的处理是：若下一条 cache line 已命中，则直接返回其 word0 作为 slot1；否则本拍只返回 slot0，下一次请求再取 `PC+4`，保证不丢、不重、不乱序。更宽实现仍有两种常见方向：

### 4.1 leftover/carry

先返回 line 尾 slot0，把下一 line 的第一条留到下一 packet。实现简单、正确，但边界处只有 1-wide。

### 4.2 两 bank 或 block-aligned fetch

ICache 能同时读相邻 bank/line，拼成一个 2-slot block。需要：

- 两路 tag/data 命中；
- 任一 line miss 时保存部分结果；
- refill 和失效覆盖两个 line；
- 响应仍用同一个 FTQ/generation 身份。

教学核建议先做 carry，确认所有边界正确，再评估双 bank。不要简单发两个互不关联的总线请求，否则 redirect/stale response 和顺序会迅速复杂化。

---

## 5. FTQ 保存什么

FQ 保存“待 decode 的指令”；FTQ 保存“一个预测 fetch block 的控制元数据”。建议 entry：

```scala
class FTQAlloc extends Bundle {
  val basePc = UInt(32.W)
  val validMask = UInt(2.W)
  val predictedNextPc = UInt(32.W)
  val cfiSlot = UInt(2.W)
  val ghr = UInt(log2Ceil(BHT_SIZE).W)
  val ras = Vec(RAS_SIZE, UInt(32.W))
  val rasPtr = UInt(log2Ceil(RAS_SIZE).W)
  val rasCount = UInt(log2Ceil(RAS_SIZE + 1).W)
}

class FTQEntry extends FTQAlloc {
  val valid = Bool()
  val generation = UInt(FTQ_GEN_W.W) // 当前为 8 bit
  val pending = UInt(log2Ceil(FETCH_WIDTH + 1).W)
}
```

同一 fetch block 的两条指令引用同一个 `ftqIdx + generation`，再用 slot 编号区分；`pending` 记录这个 block 还有几条动态指令未提交。当前实现保存恢复所需的 GHR/RAS 状态，但没有把完整 TAGE provider/alternate meta 或 FQ tail snapshot 写进 FTQ；不能把建议字段误写成已实现字段。

---

## 6. speculative history 与恢复

10j 主要在 commit 端维护真实历史，安全但预测器看不到所有年轻分支。10m 已引入：

```text
预测一个 block
  → 按 slot0→slot1 的预测结果更新 speculative GHR/RAS
  → FTQ 保存更新前 snapshot

分支 resolve 正确
  → 保留 speculative state

分支 mispredict
  → 从对应 FTQ snapshot 恢复
  → 再顺序注入该分支的真实方向/目标
  → 选择性杀掉年轻 FTQ 项，并清 FQ/fetch-buffer 错误流
```

commit-time history 仍作为已退休的可信边界；speculative history 是可回滚的前端状态。两者不要混成一个寄存器然后靠“全部清零”恢复。

RAS 也必须 snapshot 或记录可逆操作。slot0 call、slot1 ret 同 block 时，预测更新顺序固定 slot0 → slot1。目标分支处于 slot0 还是 slot1，必须用 `recoverPc != ftq.basePc` 判断；fetch block 的 `basePc` 不保证 8B 对齐，直接看 `pc(2)` 会在 `basePc=...4` 时判错槽位。

---

## 7. 推荐流水分段

```text
F0: 选择 next PC，发起 ICache block lookup
F1/F2: ICache 返回，predecode 两槽并组合 BPU/TAGE/ITAGE 结果
F3: FetchBuffer 有空间且 FTQ 有空项时，原子分配 FTQ entry 并写 FetchBuffer
F4: 写 FQ，送 2-wide decode
```

当前实现把 FTQ 分配放在“接受 ICache response / 写 FetchBuffer”这一拍，并断言 `fetchBuffer.in.fire == ftq.alloc.fire`，因此不会留下没有 packet 的 FTQ 项。若选择在 F0 提前分配，则必须另做 reservation、取消和 stale-response 协议；不能只移动分配时机。分配后每级携带 `ftqIdx + generation`，redirect 用 generation 丢弃旧流。

FTQ full、FetchBuffer full、FQ 无空间都应局部 backpressure；PC 只有在请求被下一级接受时推进。

---

## 8. 和后端宽度的联动

持续双取指应放在 10k/10l 之后评估：

- 10k 提高不同指令类型的双退休概率；
- 10l 让 load 阻塞不必长期占住 head/LSU；
- 10m 才能把更多正确路径指令稳定送入后端。

若 rename/RS/ROB/commit 长期阻塞，前端只是更快填满 FQ。验收必须同时看：

```text
fetch valid inst / fetch block
FQ enqueue2 与 FQ full
rename/dispatch 2-wide
commit2 与 Head Not Ready
wrong-path fetch / BP flush
IPC
```

---

## 9. 必做测试

| 用例 | 预期 |
|------|------|
| 连续顺序指令 | 稳定产生双有效 block |
| slot0 predicted taken | slot1 被 mask，next PC 为 slot0 target |
| slot1 predicted taken | 两槽有效，next PC 为 slot1 target |
| line 尾取指 | 无丢指令、重复指令或错序 |
| FQ backpressure | FetchBuffer bits 保持稳定 |
| outstanding ICache 时 redirect | 旧响应按 generation 丢弃 |
| FTQ wrap-around | 旧 meta 不关联到新 block |
| slot1 mispredict | 恢复到对应 snapshot 并杀年轻项 |
| 年轻分支先于更老分支 resolve | 年轻项可先截断；更老分支的 FTQ snapshot 必须保留 |
| 分支恢复与双提交边界 | redirect 不与 architectural commit 同拍修改 FTQ |
| call/ret 同 block | RAS 更新与回滚顺序正确 |
| fence.i | 清 ICache、FetchBuffer、FQ/FTQ 错误流 |
| 错路取到非法地址 | 已握手的 AXI 读必须返回 `SLVERR`，flush drain 不能永久等待 |

建议给 ICache 加随机响应延迟，并在测试中随机拉低 FQ ready，专门检查 ready/valid 稳定性。

---

## 10. 性能计数与验收

参考核已接入：

```text
FETCH_BLOCK
FETCH_VALID_INST
FETCH_BLOCK_2VALID
FETCH_BUFFER_FULL
FTQ_FULL
FETCH_LINE_TAIL
FETCH_REDIRECT_BUBBLE
SPEC_GHR_ROLLBACK / RAS_ROLLBACK
FTQ_HIGH_WATER / FTQ_STALE_RECOVER
ALU1_ISSUE / DUAL_ALU_ISSUE / FU_REFILL / WB_AGE_REORDER
```

`ICACHE_STALE_RESP`、`WRONG_PATH_ENQ` 和精确的 redirect-to-fetch latency histogram 仍可作为后续观测增强；本章不把尚未接入的计数器写成当前能力。

关键派生指标：

```text
avg_fetch_width = FETCH_VALID_INST / FETCH_BLOCK
slot1_util      = FETCH_BLOCK_2VALID / FETCH_BLOCK
```

通过条件不是 slot1 利用率单独升高，而是：

- 所有 redirect/line-crossing/backpressure 测试通过；
- cpu-tests+difftest 全绿；
- `avg_fetch_width`、dispatch2、commit2 形成同向提升；
- FQ full、wrong-path enqueue、Head Not Ready 没有抵消收益；
- microbench IPC 优于 10j `0.4357`，或能用计数器明确定位下一处瓶颈。

---

## 11. 当前源码落点

| 文件 | 作用 |
|------|------|
| `scala/unit/ftq.scala` | 16-entry FTQ、pending slot 计数、双提交释放、snapshot、generation、选择性年轻项截断 |
| `scala/unit/fetch_buffer.scala` | 2-entry circular packet buffer、同拍 pop/push、flush |
| `scala/core/ifu.scala` | fetch block 流水、next PC、redirect |
| `scala/unit/icache.scala` | line 边界、tagged response、相邻已命中 line 的 slot1 |
| `scala/unit/fq.scala` | 双入队、ftqIdx/slot 元数据 |
| `scala/unit/bpu.scala` | speculative GHR/RAS 与恢复接口 |
| `scala/unit/writeback_arbiter.scala` | 4→2 oldest-result CDB 仲裁，处理 ROB 环回并避免固定 FU 优先级饥饿 |
| `scala/core/idu.scala` | 2-wide 消费和 block 元数据下传 |
| `scala/core/core.scala` | redirect 优先级、全局 generation 接线 |
| `scala/bus/xbar.scala` / `scala/sim/sram.scala` | 按真实握手锁存 owner；所有已接受读请求都返回 OKAY/SLVERR |
| `scala/unit/PerfMonitor.scala` | fetch block/FTQ/redirect 计数 |
| `scala_test/unit/OoOUnitTest.scala` | 前端边界与随机 backpressure 测试 |

---

## 12. 后端必须接住前端宽度

10m 第一版前端已经把平均 fetch width 拉高，但 IPC 只有 `0.4723`。计数器说明瓶颈转移到了执行端：ALU/DIV 的 dispatch register 在结果离开后必须空一拍，且普通整数操作只有一个 ALU 口。参考核做了两项分布式改动。

### 12.1 FU 同拍 pop/refill

```scala
val canLoadAlu = !stopIssue && (!dAluValid || aluLeave || flushAlu)
val takeAlu = canLoadAlu && rs.io.issue_alu_valid

when(aluLeave || !dAluValid) {
  dAluValid := takeAlu
  when(takeAlu) { dAluBits := packIssue(rs.io.issue_alu_bits) }
}
```

RS 在 ALU/DIV issue fire 时立即标记 entry issued；结果离开和下一条进入可以同拍发生。该改动把 IPC 从 `0.4723` 提到 `0.5316`。

### 12.2 第二整数 ALU

RS 用本地 age 比较选“最老 ready 普通操作”到 ALU0，再从剩余项中选“次老 ready 普通整数操作”到 ALU1：

```scala
val alu0OH = oldestOH(ordinaryReady)
val alu1Eligible = ready.zipWithIndex.map { case (r, i) =>
  r && !alu0OH(i) && !isLoad(i) && !isMulDiv(i) &&
  jump(i) === JUMP_NONE && !csrOrSpecial(i)
}
val alu1OH = oldestOH(alu1Eligible)
```

分支、CSR、异常、ebreak、fence 等仍只走 ALU0；ALU1 只执行无特殊副作用的普通整数指令。四类结果 `ALU0 / LSU / ALU1 / DIV` 进入局部 4→2 `WritebackArbiter`，按 ROB age 选最老两项，避免固定 FU 优先级饿死 ALU1/DIV；它只仲裁已经完成的结果，不成为全局 issue scheduler。双 ALU 首次通过后仍是收官前候选点，修完 FTQ/总线协议并做严格重复回归后才成为最终保留点：

| 指标 | 10j 基线 | 双 ALU 候选 | 10m 最终（两次一致） |
|------|----------|-------------|---------------------|
| IPC | `0.4357` | `0.6329` | **`0.6381`** |
| cycles | `1199577` | `825390` | **`818892`** |
| commits | `522676` | `522424` | `522548` |
| average fetch width | - | `1.3513` | `1.3858` |
| fetch slot1 util | `5.35%` | `35.13%` | `38.58%` |
| commit slot1 util | `6.08%` | `50.55%` | `51.23%` |
| head not ready | `546907` | `371257` | `363162` |
| FTQ stale recover | - | 收官前未清零 | **`0`** |

最终点相对 10j IPC 提升约 `46.5%`。完整扫描过程见 [stage10_completion_results.csv](stage10_completion_results.csv)。

### 12.3 分支恢复必须是 one-shot event

执行结果可能因 CDB 仲裁在 ALU0 驻留多拍。若 `mis_predict` 直接由 `d_alu_valid` 形成 level 信号，同一动态分支会重复 flush：第一次已清 FTQ，后续拍就成为 stale recover。参考核给 ALU0 驻留项增加 `d_alu_ctrl_resolved`，只在该项第一次解析时产生 redirect/BPU 统计事件；dispatch register 被替换或清空时复位该位。

### 12.4 FTQ 恢复必须选择性截断

更年轻分支可能先 resolve，而更老但未 ready 的分支仍留在 ROB/RS。若任何 mispredict 都清空整个 FTQ，后者稍后 resolve 时就找不到 snapshot。当前 `FTQ` 按环形 age 保留 `head..recoverIdx`，只使目标之后的 entry 失效并递增 generation；若目标是 block 的 slot0，还会把同 block 的错路 slot1 从 `validMask/pending` 中移除。`PopCount(valid)==count`、只按 fetch 顺序释放、redirect 不与 commit 竞争均由 assertion 固定。

### 12.5 已接受的总线请求必须有响应

分支预测可以短暂取到错误路径甚至非法地址，这本身不构成架构错误。但 SRAM 曾在 `arready=1` 时接受非法 `AR`，却只对合法地址进入响应状态，导致 IFU flush drain 永远等不到 `rvalid`。修复后，SRAM 对每个已握手读都进入 WAIT/DATA，非法地址返回 `SLVERR`；xbar 也只在真实 AR/AW handshake 后锁存 owner，并在 IMEM 响应握手后释放。这个协议不变量比“正确路径不会访问非法地址”的假设更可靠。

### 12.6 最终严格验收

- 固定入口：`scripts/stage10_final.sh`；
- `mychisel.compile` PASS；`OoOUnitTest` 68/68；`chisel-gen` 无 elaboration warning；
- `dummy/add/add-longlong/bit/load-store/shift/string` 7/7 cpu-tests+difftest；
- 两次 microbench 都满足 PASS + GOOD TRAP + 无 HANG/ABORT/mismatch，摘要逐字段一致；
- 两次均为 IPC `0.6381`，`FTQ Stale Recover=0`。

---

## 13. 验收清单（自勾）

- [ ] 能解释 packet 宽度 2 为什么不等于持续 2-wide；
- [ ] slot0/slot1 taken 的 validMask 与 next PC 都正确；
- [ ] 跨 line 不丢、不重、不乱序；
- [ ] redirect 后旧 ICache/FTQ/FQ 数据不能进入后端；
- [ ] speculative GHR/RAS 能精确回滚；
- [ ] 用整条吞吐链而非单个 slot1 指标判断是否保留。

下一章：[12_阶段11_IPC从0.6到1.md](12_阶段11_IPC从0.6到1.md)。
