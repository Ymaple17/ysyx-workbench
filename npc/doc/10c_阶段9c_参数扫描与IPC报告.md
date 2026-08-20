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
