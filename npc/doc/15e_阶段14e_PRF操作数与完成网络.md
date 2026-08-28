# 阶段 14e：PRF 操作数与完成网络

## 学习导航
- **理论目标**：理解四路 rename 理论上带来八个源读取需求，以及多 FU 完成怎样扩展而不构造不可综合的大型多端口 PRF/CDB 交叉网。
- **最小实现**：采用 operand capture/PRF 复制或 banked read，扩展到足够的 completion lanes，并保持 accepted identity 的统一门控。
- **当前参考核**：PRF/RS 围绕双 dispatch、两个 data CDB 构建；control/store 有私有 completion，不能直接宣称支撑四发。
- **后续扩展**：clustered PRF、physical register banking、result bypass mesh、wakeup-select 分级流水。
- **验收方式**：八源最坏情况、bank conflict、同拍四 FU 完成、completion backpressure、flush/reuse、依赖链同拍 wakeup 定向测试和利用率计数。

---

## 1. Operand delivery 选择

| 路线 | 特点 |
|------|------|
| 8 读端口单 PRF | 逻辑直接，面积/时序最差 |
| PRF 复制 | 写端广播，读端简单，容量成本上升 |
| Banked PRF | 面积较好，需要 bank conflict/replay |
| Rename/dispatch operand capture | ready 源提前读入 RS，未 ready 源靠 wakeup value |

教学参考核优先复用已有 RS operand capture，并对不足的读带宽加入可计数 backpressure，而不是隐藏错误值。

## 2. Completion

四发不要求每拍恰好四结果，但必须避免两个 CDB 把持续 FU 完成压回 issue。推荐组合：

```text
ALU cluster local completion lanes
Load reserved/age-aware data lane
DIV/BRU data result enter oldest arbiter
store/control no-value private completion
```

所有 data completion 最终统一更新 PRF、Busy、ROB、RS wakeup；局部化只改变传输组织，不改变架构语义。

## 3. 验收清单（学习者自勾）

- [ ] 八源需求不会读错或静默丢失
- [ ] bank conflict 有 replay/backpressure
- [ ] 四 FU 完成身份门控一致
- [ ] completion 不再把四发压成双发

下一章：[15f_阶段14f_四提交恢复与Difftest.md](15f_阶段14f_四提交恢复与Difftest.md)。
