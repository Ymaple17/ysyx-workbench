# 阶段 10j：完整 TAGE 与 ITAGE

## 学习导航
- **理论目标**：理解现代前端为什么使用多历史长度的 tagged predictor，以及间接跳转目标为什么也需要历史相关预测。
- **最小实现**：在 10e 的 tagged override 基础上，拆出多张几何历史表，实现 provider/alternate 选择和 commit-time 训练。
- **当前参考核**：未做。10e 当前是最小高级 BPU：base BHT/RAS + tagged override + indirect target table。
- **后续扩展**：speculative history、FTQ 回滚、TAGE-SC、loop predictor、return-address 栈投机修复。
- **验收方式**：记录 direction miss、target miss、unpredicted jalr、tagged/indirect hit、IPC；必须对比 10e 最小 BPU。

---

## 1. 为什么最后做

预测器越强，收益越依赖下游能不能消化正确路径指令。若提交仍 1-wide 或窗口太小，完整 TAGE 的收益会被掩盖。因此本章放在：

```text
10f 双提交
10g 扩窗口
10h 宽取指
10i 访存 MLP
```

之后。

---

## 2. 最小结构

```text
T0：base bimodal / GShare
T1：短历史 tagged table
T2：中历史 tagged table
T3：长历史 tagged table

预测：最长历史 tag 命中者作为 provider
更新：commit 时更新 provider；错误时向更长历史表分配
```

ITAGE 则把预测值从 taken/not-taken 换成 indirect target，并使用类似历史和 tag 结构。

---

## 3. 验收清单（自勾）

- [ ] 能解释 provider / alternate 的作用
- [ ] 能解释为什么预测历史最好支持投机更新和 flush 回滚
- [ ] direction miss 明显下降，cpu-tests 不回归
- [ ] full regression 通过

下一步：[12a_阶段11a_MMU与虚拟内存.md](12a_阶段11a_MMU与虚拟内存.md)。
