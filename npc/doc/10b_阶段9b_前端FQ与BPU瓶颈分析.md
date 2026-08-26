# 阶段 9b：前端 / FQ / BPU 瓶颈分析

## 学习导航
- **理论目标**：本章先理解：判断 IPC 低是因为后端算不动，还是前端喂不饱、分支错太多、FQ backpressure 太重。
- **最小实现**：补 FQ full/empty、fetch stall、方向错/目标错/未预测 JALR、head/store wait 分类，并用同一 workload 计算各项占比。
- **当前参考核**：正文 FQ Full、BPU miss、store wait 是 Stage9b 快照；10a-10m 已分别处理这些瓶颈，当前剩余指标见最终摘要。
- **后续扩展**：9c 对 FQ/BHT 等参数做单变量扫描；只有结构瓶颈被数据证明后才进入阶段 10。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：9a 已固定 baseline。  
**目标**：判断 IPC 低是因为后端算不动，还是前端喂不饱、分支错太多、FQ backpressure 太重。  
**本章阶段快照**：Stage9a baseline 已固定；当章统计显示 FQ Full、BPU miss、head store commit wait 都值得优先检查。

---

## 1. 先问三个问题

1. IFU 是否经常因为 FQ 满而停？
2. FQ 是否经常因为 flush / redirect 被清空？
3. 分支预测错误是否制造了大量无效取指和重命名气泡？

如果这三个问题答案都是“是”，先别急着加后端宽度。后端再宽，前端不给指令也白搭。

---

## 2. 最小实现

先补前端相关计数器：

```scala
when(fq.io.enq.valid && !fq.io.enq.ready) { fqFullCnt := fqFullCnt + 1.U }
when(!fq.io.deq.valid) { fqEmptyCnt := fqEmptyCnt + 1.U }
when(is_bp_flush) { bpFlushCnt := bpFlushCnt + 1.U }
```

然后跑同一份 microbench，比较：

- FQ full 高：取指端太积极或后端消费不均衡
- FQ empty 高：取指 / 预测 / ICache 供给不足
- bp flush 高：预测质量或 redirect 恢复节奏有问题

---

## 3. Stage9b 本章基线

Stage9b 入口前端已经有 FetchQueue 和轻量 BPU，但不是高级预测器。本章只做诊断和小修：

- 检查 flush 后 FQ 是否确实清干净
- 检查 redirect PC 是否及时送回 IFU
- 检查 BPU 更新是否只在 commit 后发生
- 检查 FQ backpressure 是否把 IFU 长时间压住

Stage9a microbench(test) 基线数据：

| 指标 | 数值 | 含义 |
|------|------|------|
| FQ Full | `351450` | IFU 经常被 FQ backpressure 压住 |
| FQ Empty | `74796` | 后端也会遇到前端供给空洞 |
| BPU hit rate | `64.16%` | 分支预测质量偏低 |
| BP flushes | `50756` | flush / redirect 成本高 |
| wait store commit | `215525` | 顺序 store 提交也会拖住 head |

因此 9b 的优先级建议：

1. 先确认 `FQ Full` 的来源：是 FQ 太小、flush 后恢复慢，还是后端消费节奏导致堆积。
2. 再确认 BPU 的更新 / redirect 路径，不先上 TAGE。
3. store commit wait 记录为阶段 10a 证据，阶段 9 暂不直接上 store buffer。

---

## 4. 后续扩展

- 若 BPU 确认是主瓶颈，再去阶段 10e 做 TAGE / ITAGE。
- 若取指带宽确认不足，再去阶段 10d 做宽取指和前端带宽。
- 若 FQ Full 是后端消费不稳导致，回头检查 RS / CDB / commit。

---

## 5. 验收方式

- 功能回归不变：compile + unit + cpu-tests
- microbench(test) 通过
- 统计报告能说明：FQ Full / FQ Empty / BPU miss 哪个是当前主因

---

## 6. 本轮参考核实现

Stage9b 没有提前引入 TAGE、宽取指或 StoreBuffer，而是先做三个小步：

1. BPU 冷启动优化：BHT 初值从强不跳改为弱不跳；BHT 增加 valid 位，未训练项按 backward-taken / forward-not-taken 预测。
2. FQ 深度小幅增大：`FQ_SIZE` 从 4 改为 8，用更深的取指队列吸收后端短暂停顿。
3. BPU miss 细分计数：把 flush 拆成方向错、目标错、未预测 JALR，方便判断下一步是不是该做更强预测器。

源码骨架：

```scala
object BPU_Config {
  val BHT_INIT = 1
  val BHT_COLD_STATIC = true
}

val bht = RegInit(VecInit(Seq.fill(BHT_SIZE)(BHT_INIT.U(2.W))))
val bht_valid = RegInit(VecInit(Seq.fill(BHT_SIZE)(false.B)))
val bht_predict_taken =
  if (BHT_COLD_STATIC) Mux(bht_valid(bht_index), bht_value(1), imm.io.imm_ext(31))
  else bht_value(1)

when(io.update_valid && io.update_is_branch) {
  bht_valid(io.update_index) := true.B
}
```

```scala
PM(conf, clock, EVENT_BPU_DIR_MISPRED, 1.U,
  mis_predict && d_alu_bits.bp_valid && (is_ch =/= predict_taken))
PM(conf, clock, EVENT_BPU_TARGET_MISPRED, 1.U,
  mis_predict && d_alu_bits.bp_valid && target_mispredict)
PM(conf, clock, EVENT_BPU_UNPREDICTED, 1.U,
  mis_predict && !d_alu_bits.bp_valid)
```

## 7. Stage9b 实测结果

同一份 `microbench mainargs=test`，difftest 开启：

| 指标 | Stage9a baseline | BPU 冷启动 | BPU + FQ=8 |
|------|------|------|------|
| IPC | `0.4119` | `0.4121` | `0.4122` |
| Total Cycles | `1268745` | `1267668` | `1268071` |
| BPU Hit Rate | `64.16%` | `64.30%` | `64.43%` |
| Predicted branch dir/target miss | `42840` | `42614` | `42492` |
| BP Flushes | `50756` | `50529` | `50408` |
| FQ Full | `351450` | `350867` | `302451` |
| FQ Empty | `74796` | `75634` | `74952` |
| IFU Pipeline Stall | `241856` | `241335` | `215042` |
| Wait Store Commit | `215525` | - | `215845` |

BPU + FQ=8 版本新增的 miss 分类：

| 分类 | 数值 | 含义 |
|------|------|------|
| Direction Miss | `41909` | Stage9b 的主要 BPU 问题仍是方向预测 |
| Target Miss | `583` | 目标预测不是主要瓶颈 |
| Unpredicted JALR | `7916` | 间接跳转还没有 BTB/间接预测支持 |

`42492 = 41909 + 583` 只统计已预测分支的方向/目标错误；再加 `7916` 次未预测 JALR，才得到总 `BP Flushes=50408`。因此不能把 `42492` 单独标成全部 BPU flush/mispredict。

结论：Stage9b 的小步优化方向正确，但 IPC 提升很小。当章最大的真实限制仍然不是 FQ 容量本身，而是方向预测质量、顺序 store commit 等更深层瓶颈。FQ=8 可以保留，因为它显著降低了 FQ Full 和 IFU Pipeline Stall；更激进的 FQ 参数扫描放到 Stage9c。

验收结果：

- `./mill -i mychisel.compile` 通过
- `unit.OoOUnitTest` 28/28 通过
- cpu-tests smoke：`dummy`、`add`、`add-longlong`、`bit`、`load-store`、`shift`、`string` 通过
- `microbench mainargs=test` 通过，`npc HIT GOOD TRAP at pc=0x800055f0`
