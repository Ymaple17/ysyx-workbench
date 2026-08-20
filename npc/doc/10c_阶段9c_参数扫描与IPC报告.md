# 阶段 9c：参数扫描与 IPC 报告

## 学习导航
- **理论目标**：本章先理解：用单变量扫描验证参数收益，形成可复现 IPC 报告。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：阶段 8 参考核参数可扫，但每次改参都必须重新回归。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：9a/9b 已有 baseline 和瓶颈假说。  
**目标**：用单变量扫描验证参数收益，形成可复现 IPC 报告。  
**仓库状态**：阶段 8 参考核参数可扫，但每次改参都必须重新回归。

---

## 1. 扫描规则

一次只改一个参数：

| 参数 | 常见候选 | 观察点 |
|------|----------|--------|
| `ROB_SIZE` | 16 / 24 / 32 | 窗口是否限制乱序收益 |
| `RS_SIZE` | 8 / 12 / 16 | 是否经常 RS full |
| `FQ_SIZE` | 4 / 8 / 16 | 前端解耦是否改善 |
| `N_PHYS` | 随 ROB 增加 | freelist 是否为空 |
| `CP_DEPTH` | 4 / 8 / 16 | 分支 checkpoint 是否不足 |

不要同时改 ROB、RS、FQ、BPU，否则结果解释不清。

---

## 2. 最小实现

为每次扫描记录一行：

```text
tag, ROB, RS, FQ, N_PHYS, CP_DEPTH, IPC, BPU_hit, FQ_full, ROB_full, RS_full, FL_empty, result
baseline, 16, 8, 4, 48, 4, 0.4119, 64.16%, 351450, 0, 0, 0, PASS
stage9b, 16, 8, 8, 48, 4, 0.4122, 64.43%, 302451, 0, 0, 0, PASS
```

如果 IPC 提升但某个正确性测试失败，这个点记为失败，不纳入推荐参数。

---

## 3. 当前参考核

当前参考核已经能稳定跑 microbench(test)，适合开始扫描。优先顺序建议：

1. `FQ_SIZE`
2. BPU 小参数
3. `ROB_SIZE + N_PHYS`
4. `RS_SIZE`
5. `CP_DEPTH`

---

## 4. 后续扩展

- 自动化脚本批量生成参数组合。
- 输出 CSV，并用表格 / 图形展示 IPC 与 stall 变化。
- 给每个推荐参数写一句“为什么选它”。

---

## 5. 验收方式

- 每个参数点至少通过 compile + cpu-tests smoke
- 推荐参数必须通过 unit + microbench(test)
- IPC 报告里保留失败点和原因，不只保留好看的结果

---

## 6. Stage9c 起点

Stage9b 已经把 `FQ_SIZE=8` 作为当前参考核默认值。Stage9c 如果继续扫 FQ，建议比较 `4 / 8 / 16`，但推荐值需要同时看 IPC、FQ Full、IFU Pipeline Stall、wrong-path fetch 增量，而不是只看单个 IPC。

当前参考点：

```text
stage9b, ROB=16, RS=8, FQ=8, N_PHYS=48, CP_DEPTH=4, IPC=0.4122, BPU_hit=64.43%, FQ_full=302451, result=PASS
```

---

## 7. Stage9c 实测报告

完整 CSV 见 `stage9c_scan_results.csv`。本轮只做单变量扫描，复用 Stage9a/9b 已有结果，再补跑两个新点：

| tag | FQ | BHT | IPC | BPU hit | FQ Full | IFU Pipeline Stall | IFU Fetch | 结论 |
|------|---:|---:|---:|---:|---:|---:|---:|------|
| stage9a_baseline | 4 | 1024 | `0.4119` | `64.16%` | `351450` | `241856` | `860380` | 原始基线 |
| stage9b_bpu_fq4 | 4 | 1024 | `0.4121` | `64.30%` | `350867` | `241335` | `858549` | BPU 冷启动小幅改善 |
| stage9b_fq8 | 8 | 1024 | `0.4122` | `64.43%` | `302451` | `215042` | `904436` | 当前推荐点 |
| stage9c_fq16 | 16 | 1024 | `0.4089` | `63.76%` | `245932` | `182876` | `960319` | 不推荐 |
| stage9c_bht2048 | 8 | 2048 | `0.4103` | `64.59%` | `322744` | `235586` | `892033` | 不推荐 |

FQ 扫描结论：

- `FQ=8` 比 `FQ=4` 更稳，FQ Full 明显下降，IPC 小幅上升。
- `FQ=16` 虽然继续降低 FQ Full 和 IFU Pipeline Stall，但 IPC 下降，IFU Fetch 增加到 `960319`。这说明更深的 FQ 会保留更多错路取指，flush 代价和后端压力反而变重。
- 因此当前参考核保留 `FQ_SIZE=8`。

BHT 扫描结论：

- `BHT_SIZE=2048` 让 BPU hit rate 从 `64.43%` 小升到 `64.59%`，方向错也略降。
- 但 IPC 从 `0.4122` 降到 `0.4103`，CDB conflicts 和 store commit wait 上升。这个结果说明简单加大 BHT 表不是当前最划算的优化。
- 当前方向预测问题更像“预测器形态不够强 / 历史更新时机偏保守 / 间接跳转无 BTB”，不是单纯容量不够。

最终推荐参数：

```scala
ROB_SIZE  = 16
RS_SIZE   = 8
FQ_SIZE   = 8
BHT_SIZE  = 1024
BHT_INIT  = 1
BHT_COLD_STATIC = true
CP_DEPTH  = 4
```

验收记录：

- 新扫点 `FQ=16`：compile 通过，microbench(test) PASS
- 新扫点 `BHT=2048`：compile 通过，microbench(test) PASS
- 最终参数已恢复为 `FQ=8 / BHT=1024`，并重新 `./mill -i mychisel.compile` 通过
