# 阶段 15a：commit-filled Trace/Stream 前端

## 学习导航
- **理论目标**：理解“预测下一块地址”和“直接提供一段已经验证过的动态指令流”的差别，以及 trace 身份、失效和恢复为何比容量更重要。
- **最小实现**：先做 commit-filled 四字 L0 word stream：四个连续 PC tag 全命中且没有 control/system/fence 时，只替换 IFU 的指令字来源；BPU、FTQ、FetchBuffer 和 fallback 保持原路径。A/B 为正后再扩展带 path context 的 TraceEntry。
- **当前参考核**：Stage14 只有四槽 IFU+ICache+FQ/FTQ，平均 fetch width `3.1895`，`FQ Empty=12567`，尚无 trace/stream cache。
- **后续扩展**：多分支 trace、decoded-uop cache、way prediction、跨 trace stitching、loop stream；只有最小实现 A/B 为正才增加容量或路径数。
- **验收方式**：定向覆盖 fill/hit/miss、taken branch 跨块、path alias、flush、fence.i、自修改代码失效、fallback、FQ partial ready；全核要求 difftest-clean 且降低 `FQ Empty` 或 redirect refill cycles。

---

## 1. 两层数据结构

### 1.1 15a0：四字 L0 word stream

```scala
class CommittedStreamWord extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
}

// 64-entry direct-mapped PC -> committed instruction word
// lookup 同时验证 pc, pc+4, pc+8, pc+12 四个完整 tag
```

它不缓存动态身份，也不自行拥有 redirect。命中只将 `fetchInst(0..3)` 从 ICache 响应切到已提交指令字；本拍 BPU 重新读取这些指令，随后仍原子分配 FetchBuffer 与 FTQ。四 tag 中任一 miss、包含 control/system/fence，或 IFU 已有未完成请求时，直接走旧 ICache 路径。

### 1.2 15a1：带路径上下文的 TraceEntry

```scala
class TraceEntry extends Bundle {
  val valid       = Bool()
  val startPc     = UInt(32.W)
  val pathTag     = UInt(PATH_TAG_W.W)
  val length      = UInt(log2Ceil(TRACE_UOPS + 1).W)
  val lanePc      = Vec(TRACE_UOPS, UInt(32.W))
  val inst        = Vec(TRACE_UOPS, UInt(32.W))
  val controlMask = UInt(TRACE_UOPS.W)
  val predictedNextPc = UInt(32.W)
  val exitKind    = UInt(TRACE_EXIT_W.W)
}
```

固定宽 RV32 也需要逐 lane PC：taken branch 之后的下一条动态指令不一定是 `startPc + lane*4`。第一版存原始 instruction 和最小预译码，不缓存 PRF/ROB/FU 身份；后者属于每次动态执行，不能跨实例复用。

## 2. 为什么由 commit 填充

```text
fetch fill  -> 容易把错误路径长期写入 trace
commit fill -> 只记录真实退休路径，训练慢一些但语义清楚
```

15a0 不需要 builder：四路 commit 各自按 PC 写 word array，同 index 冲突时年轻 lane 胜出。15a1 才按 lane0 到 lane3 顺序追加动态路径；遇到 control、容量上限、非连续上下文、异常/特殊指令或 fence.i 时结束 stream。两层都只收集指令字、PC、控制类型和实际 next PC，不复制动态 rename/预测训练身份。

## 3. lookup、所有权与 fallback

```text
fetch PC + path context
  -> trace hit: TracePacket -> FetchBuffer/FQ
  -> trace miss: existing IFU -> ICache/BPU -> FetchBuffer/FQ
```

两条路径共享一个明确的 packet owner，不能同拍各自推进 PC。FQ backpressure 时命中结果必须保持稳定；redirect/exception/memory-order violation 由统一 recovery generation 杀死旧 packet。trace 只预测供给路径，branch execute 仍比较实际结果并产生最老 redirect。

## 4. BPU 与 trace 的边界

trace 命中不能拿“上次提交时的 provider index”训练当前 BPU，因为表项可能已被替换。最小实现采用独立 trace prediction metadata：

- trace 自己只记录 pathTag、exit 与 predictedNextPc。
- branch resolution 单独统计 `trace_correct/trace_miss/trace_exit_miss`。
- TAGE/ITAGE 的训练仍使用本次动态指令携带的有效 metadata；无法提供新 metadata 的 control 不允许用旧快照更新 predictor。

第一版可以只让无内部 control 的短 stream 命中，先证明旁路合同；第二门再允许一个已提交 taken control 跨块。

## 5. 失效规则

- reset：全部 invalid。
- `fence.i` 提交：全部 invalid，并等待旧 IFU/trace packet drain。
- 写可执行内存：本教学核没有完善 I/D coherence，保守依赖 `fence.i` 全清。
- flush：杀动态 packet，但不必删除由更早 committed path 填充的 entry。
- pathTag 不匹配：miss，禁止猜测命中。

## 6. 施工切片

| 步 | 内容 | 退出条件 |
|----|------|----------|
| 1 | 64-entry commit-filled word buffer | fill、完整 tag、alias、fence.i 单测通过 |
| 2 | 无内部 control 的四字 lookup/fallback | 不发 ICache 请求；FTQ/BPU/FQ 所有权与旧 IFU 等价 |
| 3 | 一个 taken control 的跨块 stream | per-lane PC/nextPc/flush 正确 |
| 4 | fence.i/generation/pathTag | stale packet 不进入 FQ |
| 5 | perf counters 与全核 A/B | cycles 下降才保留 |

## 7. 计划源码落点

- `src/main/scala/unit/committed_stream_buffer.scala`：15a0 word/tag/valid 与四路 commit fill。
- `src/main/scala/core/ifu.scala`：指令来源选择、ICache fallback、hit/miss 计数，继续复用现有 BPU/FTQ/FetchBuffer。
- `src/main/scala/core/core.scala`：四路 commit fill 与 `fence.i` 失效。
- `src/test/scala/unit/CommittedStreamBufferTest.scala`：fill/hit、direct-map alias 与 invalidate 定向测试。
- `trace_cache.scala` / TracePacket：只在 15a0 A/B 为正且需要跨 taken control 时新增。

## 8. 验收清单（学习者自勾）

- [ ] 能解释 commit fill 为什么不等于永不 flush
- [ ] trace lane PC 能跨 taken branch
- [ ] hit/miss/fallback 只有一个 PC owner
- [ ] fence.i 与 generation 能杀死 stale packet
- [ ] A/B 能证明前端事件与总 cycles 同时改善

下一章：[16b_阶段15b_写回式双端口L1D.md](16b_阶段15b_写回式双端口L1D.md)。
