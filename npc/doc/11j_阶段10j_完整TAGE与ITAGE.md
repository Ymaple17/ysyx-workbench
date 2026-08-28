# 阶段 10j：完整 TAGE 与 ITAGE

## 学习导航

- **理论目标**：理解多历史 tagged predictor、provider/alternate、usefulness、受控分配，以及历史相关间接目标预测。
- **最小实现**：base GShare + 3 张 TAGE 表；PC-only 间接目标表 + 2 张 ITAGE 表；只在 commit 训练。
- **当前参考核**：阶段 12b 已把 TAGE 扩为 4×256（历史 `3/8/16/32`），ITAGE 扩为 4×128（历史 `8/16/24/48`），并加入 64-entry loop provider、16-bit 两目标 path history 和对应 speculative recovery；本章正文保留 10j 的最小完整实现快照。
- **后续扩展**：speculative history、FTQ snapshot 和 RAS 投机恢复已在 10m 完成；当前可继续做更完整的预测 meta 保存、TAGE-SC、loop predictor 和更长历史。
- **验收方式**：编译、BPU 单测、完整 OoO 单测、cpu-tests+difftest、microbench；比较 direction/target miss、TAGE/ITAGE 命中和 IPC。

---

## 1. 为什么 10e 还不够

10e 只有一张 tagged override，而且和 base BHT 使用同一个 GShare 索引。它能减少一部分 PC 冲突，但不能同时记住很短、中等和跨基本块的分支历史。

10j 把方向预测拆成多种历史尺度：

```text
T0: 1024-entry base GShare, 2-bit counter + valid
T1: 256-entry tagged table, history = 2
T2: 256-entry tagged table, history = 5
T3: 256-entry tagged table, history = 10
```

每张 TAGE 表项保存 `valid + 8-bit tag + 3-bit direction counter + 2-bit usefulness`。

ITAGE 使用类似结构预测非 return JALR 的目标：

```text
I0: 256-entry PC-only target table
I1: 128-entry tagged target table, history = 4
I2: 128-entry tagged target table, history = 10
```

return 仍交给 16 项 RAS，直接 JAL 仍由立即数得到目标。

---

## 2. 查询：provider 与 alternate

每张历史表用 PC 和折叠历史共同生成 index/tag：

```scala
def historyIndex(pc: UInt, history: UInt, length: Int, width: Int): UInt =
  pc(width + 1, 2) ^ foldHistory(history, length, width)

def historyTag(pc: UInt, history: UInt, length: Int): UInt =
  pcFold(pc) ^ foldHistory(history, length, 8) ^ rotatedHistoryFold(history, length, 8)
```

同一个分支可能命中多张表：

- 最长历史命中者是 **provider**；
- 次长历史命中者是 **alternate**；
- 没有 tagged 命中时，base GShare 是 provider。

当前策略：

```text
provider 强，或 usefulness > 0  -> 使用 provider
provider 刚分配、计数器弱、u=0 -> 暂时使用 alternate
```

这避免新表项只看过一次结果就压过已经稳定的短历史/base 预测。`bp_tagged_hit` 表示至少一张 TAGE 表命中，`bp_tage_use_alt` 表示本次最终采用 alternate。

---

## 3. 更新：为什么仍在 commit

预测器状态只允许已退休指令修改：

```text
EX 产生真实方向/目标
  -> ROB 保存 actual_taken / actual_target
  -> commit 按程序序更新 BPU
```

这样 flush 掉的错误路径不会污染 GHR、TAGE、ITAGE 和 RAS。

### 3.1 恢复取指时历史

当前流水已经随指令保存：

```text
bp_index = pcIndex XOR ghr_at_fetch
```

提交时同时有 `update_pc` 和 `update_index`，因此可恢复本次取指使用的 10-bit committed-history 快照：

```text
ghr_at_fetch = update_index XOR pcIndex(update_pc)
```

这让 10j 不必扩大 FQ/RS/ROB 的预测 meta，但也带来明确限制：

- 历史长度不能超过当前 10-bit GHR；
- provider 在 commit 时按快照重新查询，而不是保存预测时的完整 provider meta；
- GHR 不在预测时投机前移，同一批在飞分支看到的历史偏旧。

所以它是完整教学数据通路，不是工业级时序/精度上限。

### 3.2 TAGE 训练

提交一个条件分支时：

1. base 2-bit counter 总是训练；
2. 若 tagged provider 存在，训练 provider 的 3-bit counter；
3. provider 与 alternate 方向不同时，用真实结果增减 usefulness；
4. 最终预测错误或 base 尚未训练时，只向更长历史表分配一项；
5. 优先选择 invalid 或 usefulness=0 的表项；
6. 没有可替换项时，衰减更长表的 usefulness。

核心源码形态：

```scala
val needAllocate = !baseWasTrained || updateDirection.taken =/= actualTaken
candidate(i) := longerThanProvider &&
  (!tableValid(i)(index) || tableUseful(i)(index) === 0.U)

when(needAllocate && candidate.asUInt.orR) {
  // Allocate only one longer-history entry.
}
```

### 3.3 ITAGE 训练

I0 PC-only 表是 alternate target provider。I1/I2 按最长历史 tag 命中选择 provider：

- target 一致：增加 confidence；
- target 改变：写入新 target，并把 confidence 降低；
- 没有可靠目标或目标错误：向更长历史 ITAGE 表受控分配；
- tagged provider 未达到强置信度前，继续使用 I0。

这样保留 10e 对普通函数指针的稳定预测，同时让相同 JALR PC 在不同历史下可以得到不同目标。

---

## 4. 模块边界

10j 没有把预测控制塞进 core：

```text
core.scala
  commit actual direction/target
          |
          v
ifu.scala -------------------- PerfMonitor
  | dual query                   | hit/alloc counters
  v                              v
bpu.scala
  +-- base GShare
  +-- TAGE direction tables
  +-- base indirect target table
  +-- ITAGE target tables
  +-- RAS
```

`core.scala` 仍只负责提供退休后的真实信息；选择 provider、训练 counter、分配表项都封装在 `unit/bpu.scala`。

---

## 5. 参数与硬件代价

当前参数在 `common/consts.scala`：

```scala
val BHT_SIZE = 1024
val TAGE_TABLE_SIZE = 256
val TAGE_HISTORY_LENGTHS = Seq(2, 5, 10)
val INDIRECT_TARGET_SIZE = 256
val ITAGE_TABLE_SIZE = 128
val ITAGE_HISTORY_LENGTHS = Seq(4, 10)
```

三张 TAGE 表约保存 `3 * 256 * (1+8+3+2) = 10752 bit`。两张 ITAGE 表约保存 `2 * 128 * (1+8+32+2) = 11008 bit`，还要加 base 表和 RAS。

当前 Chisel 用寄存器向量表达，适合教学和仿真；做 FPGA/ASIC 时应进一步考虑 SRAM 映射、双查询端口复制、读延迟和前端时序。

---

## 6. 新增性能计数

| 计数器 | 含义 |
|--------|------|
| `Tagged Hits` | TAGE 至少一张 tagged table 命中 |
| `TAGE Alternate` | 弱且无 usefulness 的 provider 让位给 alternate |
| `TAGE Allocations` | commit 时发生 TAGE 分配 |
| `Indirect Hits` | I0 或 ITAGE 给出高置信度 JALR 目标 |
| `ITAGE Hits` | 最终目标来自历史 tagged table |
| `ITAGE Allocations` | commit 时发生 ITAGE 分配 |

这些字段已进入 `stage9_regress.sh` 的 CSV。

---

## 7. A/B 扫描

完整数据见 [stage10j_scan_results.csv](stage10j_scan_results.csv)。

| 配置 | IPC | Direction Miss | TAGE Hit | ITAGE Hit | 决策 |
|------|-----|----------------|----------|-----------|------|
| 10i 最小高级 BPU | `0.4342` | `43716` | `101007`（单表） | 无 | 对照 |
| TAGE 256/表 | `0.4357` | `42618` | `97411` | `8856` | **保留** |
| TAGE 512/表 | `0.4339` | `42864` | `102348` | `8870` | 拒绝 |

256 项版本相对 10i：

- 方向错减少 `1098`；
- IPC 从 `0.4342` 提升到 `0.4357`；
- TAGE alternate 使用 `4824` 次，分配 `2393` 次；
- ITAGE 命中 `8856` 次，分配 `37` 次。

512 项虽然 tagged hit 更多，但 IPC 下降、前端 stall 增加。命中次数多不等于预测更准，更不等于整核更快，所以保留 256。

---

## 8. 验收记录

2026-08-21，在 VM `/home/qiu/ysyx-workbench/npc`：

- `./mill -i mychisel.compile`：PASS；
- `OoOUnitTest`：48/48 PASS；
- 新增 BPU 单测：TAGE 分配/provider 命中、ITAGE 稳定目标晋升；
- cpu-tests smoke+difftest：`dummy add add-longlong bit load-store shift string` 全部 PASS；
- microbench(test)：PASS，`npc HIT GOOD TRAP at pc=0x800055f0`；
- 保留点：TAGE 256 项/表，IPC `0.4357`。

恢复保留参数后再次执行 Mill compile 和 `make chisel-gen`，VM 源码与生成 RTL 已对齐。

---

## 9. 为什么 IPC 仍不到 1

10j 减少了一千多次方向错，但 10j 本章阶段快照仍不是“持续双宽”：

- fetch slot1 利用率约 `5.35%`；
- commit slot1 利用率约 `6.08%`；
- `Head Not Ready=546907`；
- `FQ Full=217210`；
- lane1 仍不退休访存、控制流和特殊指令。

因此更强 BPU 只能减少部分 flush，不能单独把 IPC 推到 1。下一条性能主线应是放宽双提交类型、改善持续双取指，或实现真正 load queue/replay；不是继续无上限扩大 TAGE。

---

## 10. 验收清单（自勾）

- [ ] 能解释 provider、alternate、usefulness 和受控分配；
- [ ] 能从 `bp_index` 推出为什么当前只能恢复 10-bit 取指历史；
- [ ] 能说明 commit history 与 speculative history 的差别；
- [ ] 能解释为什么 512 项 tagged hit 更多但 IPC 更低；
- [ ] 能运行 full regression，并读懂 10j 的六个 BPU 计数器。

---

## 11. 相关源码

- `common/consts.scala`：TAGE/ITAGE 容量与历史长度；
- `unit/bpu.scala`：折叠历史、provider/alternate、训练、分配、RAS；
- `core/ifu.scala`：双查询、next PC 选择、预测器计数；
- `core/core.scala`、`unit/rob.scala`：commit-time 真实方向/目标更新；
- `unit/PerfMonitor.scala`、`oood_chisel_csrc/cpu/perf.cpp`：性能事件；
- `scala_test/unit/OoOUnitTest.scala`：TAGE/ITAGE 定点单测；
- `scripts/stage9_regress.sh`：回归和 CSV 提取。

下一步：[11k_阶段10k_放宽双提交类型.md](11k_阶段10k_放宽双提交类型.md)。
