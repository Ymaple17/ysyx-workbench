# 阶段 14c：四路 Rename、FreeList 与 ROB

## 学习导航
- **理论目标**：掌握四条程序序指令在同拍形成 prefix RAT/free state 的方法，以及部分分配、同拍 RAW/WAW 和 checkpoint snapshot。
- **最小实现**：最多分配四个 pdest 和四个 ROB entry，lane i 的源映射看到所有更早 lane 的目的映射；资源不足时只接受连续前缀或整包进入 Rename Queue。
- **当前参考核**：`WideRenameCompat` 与 `WideROBCompat` 已接入四路分配/提交主干，冻结点 slot2/slot3 实际提交 `76172/54294`；同拍 RAW/WAW、owner reserve、checkpoint 与 wrap-around 均有定向回归。
- **后续扩展**：Stage15c 若扩到六宽，先用 WIDTH=4/6 随机参考模型证明资源守恒，再考虑 banked RAT、更多 checkpoint 或 move elimination。
- **验收方式**：随机参考模型加定向 RAW/WAW/free/checkpoint/flush/wrap 测试，四路分配后 freelist/arch RAT/ROB count 守恒。

---

## 1. Prefix rename

```text
state0 = committed input RAT/free
lane0 -> state1
lane1 reads state1 -> state2
lane2 reads state2 -> state3
lane3 reads state3 -> state4
```

lane i 的 `psrc1/psrc2` 从 `state_i.RAT` 读取；若更早 lane 写同一架构寄存器，天然形成同拍 RAW。每个有效 `reg_write && rd != 0` 消耗一个不同 physical register。

## 2. 同拍 WAW

若四条都写 x5：

```text
lane0 old=p_old new=p0
lane1 old=p0    new=p1
lane2 old=p1    new=p2
lane3 old=p2    new=p3
final RAT[x5]=p3
```

提交时依次释放每条的 old phys，最终保留 p3。不能把四条 old mapping 都写成拍前 p_old。

## 3. ROB 四入队

ROB 需要输出四个 `{idx,generation}` 身份，并处理尾指针绕回。`count_next = count + enq_count - commit_count - flushed_younger_count` 必须在同一状态方程中计算，避免多次赋值覆盖。

## 4. Checkpoint

每个 control checkpoint 保存该 control 之后、下一 lane 之前的 prefix state。若 lane1 是 branch，snapshot 应包含 lane0 和 lane1 自身 rename，不能包含 lane2/3。

## 5. 验收清单（学习者自勾）

- [ ] 四路同拍 RAW/WAW 正确
- [ ] FreeList 一拍最多分配四个不同 phys
- [ ] ROB wrap 和部分 enqueue 正确
- [ ] branch checkpoint 位于正确 prefix 边界
- [ ] flush 后 RAT/free/ROB 守恒

下一章：[15d_阶段14d_四路Dispatch与分布式Issue.md](15d_阶段14d_四路Dispatch与分布式Issue.md)。
