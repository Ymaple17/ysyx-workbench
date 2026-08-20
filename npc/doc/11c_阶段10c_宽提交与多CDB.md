# 阶段 10c：宽提交与多 CDB（提交带宽）

## 学习导航
- **理论目标**：本章先理解：提交从 1 宽变 **2 宽**；结果总线从单 CDB 升级为**多 CDB / 仲裁网络**，支撑每拍多条指令写回。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：**未做**（现状：提交 1 宽、单 CDB）。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 9 已完成性能画像，并确认提交 / CDB 是主要瓶颈。  
**目标**：提交从 1 宽变 **2 宽**；结果总线从单 CDB 升级为**多 CDB / 仲裁网络**，支撑每拍多条指令写回。  
**仓库状态**：**未做**（现状：提交 1 宽、单 CDB）。

> 8b 后发射 2 宽、执行多 FU，但**提交 1 宽**会在长串无依赖指令时成为瓶颈：后端每拍进 2，出来只能 1，ROB 慢慢被填满，最终还是停。

---

## 1. 提交带宽的瓶颈

```text
8b：rename 每拍 2 → ROB 每拍 +2
提交每拍 1 → ROB 净增 1/拍 → 跑长序列时 ROB 满 → 停发射
```

只要执行/写回足够快（多 FU + MLP），提交 1 宽就是天花板。  
**超标量主流做法**：提交 2–4 宽，且**每拍可提交多条**（只要 head 起连续 done 即可）。

---

## 2. 宽提交设计

### 2.1 提交条件

```text
每拍最多 COMMIT_WIDTH 条：
  从 head 起，连续 done 的指令依次提交
  遇到未 done → 停（保持程序序）
  遇到 fence/异常 → 特殊处理（见下）
```

### 2.2 ROB 改动

```scala
commit_fire(0..W-1)  // 每宽一个
commit_idx(i) = head + i
// ROB 内部：head += 已提交条数（连续段）
```

### 2.3 与特殊指令的交互（4a–4g 保持）

| 指令 | 宽提交行为 |
|------|-----------|
| 普通 ALU | 直接提交（1 条占 1 个提交槽） |
| store（5a） | **单独**：写完总线那拍才提交，不可与别条同拍（或占一整拍） |
| fencei/mret/异常 | **独占提交拍**（head 停，处理完再走） |
| ecall/ebreak | 同 4c/4d，独占 |

**简化策略**：store/特殊指令只占提交宽的第 0 槽，第 1 槽仅在「第 0 槽是普通指令」时可用。这样把复杂交互降到最低。

### 2.4 rename 侧

```text
提交 2 条 → rename 的 arch_rat 更新 2 个（或同拍 2 次）
freelist 归还 2 个 old_phys
→ rename.scala 的 commit 口参数化 COMMIT_WIDTH
```

---

## 3. 多 CDB（结果总线）

### 3.1 现状

```text
单 CDB：多 FU 完成 → 仲裁选 1 → can_wb 那 1 条写 PRF/唤醒
```

### 3.2 目标

```text
CDB × 2（或 PRF 双写口直连 + 2 个唤醒端口）
每拍最多 2 条写回：2 个 winner，2 个 pdest 同时广播
```

```scala
// 仲裁：每 FU 的 can_wb 向量 → 拍内 2 个 winner（不冲突）
val wb_req  = VecInit(alu.can_wb, lsu.can_wb, div.can_wb, ...)
val win0 = PriorityEncoder(wb_req)
val win1 = PriorityEncoder(wb_req & ~UIntToOH(win0))
```

### 3.3 RS 唤醒端口

```text
RS 现有 cdb_valid/cdb_pdest/cdb_val 单口
→ 加 cdb1_* 第二口；每个 entry 两个源分别匹配两个 CDB
```

### 3.4 PRF 写口

```text
PRF 现单写口（wen1/waddr1/wdata1）
→ 加 wen2/waddr2/wdata2（PRF 文件本来就是双口或可扩）
```

---

## 4. 与 8a 仲裁的关系

8a 的「单 CDB 赢家」升级为「双赢家」：

```text
拍内最多 2 个完成 → 都写回
>2 个完成 → 优先级取 2，其余下一拍
```

**组合环注意**：`can_wb` 依赖 flush 门控；多 CDB 只增加并行度，不改变 flush 语义。

---

## 5. 接线步骤建议

1. `ooo_params` 加 `COMMIT_WIDTH=2`、`CDB_NUM=2`  
2. ROB commit 口参数化；`head += n`  
3. rename commit 双口（arch_rat 2 更新）  
4. PRF 双写口；RS 双唤醒口  
5. 特殊指令独占第 0 槽策略  
6. 计数器：每拍提交条数、CDB 冲突次数  
7. 回归：长链无依赖测例（如 `sum`）看提交是否 2/拍

---

## 6. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 提交遇未 done 就乱跳 | 顺序破坏 | 连续 done 段才提交 |
| store 提交与普通同拍 | 写总线时序冲突 | store 独占/占第 0 槽 |
| fence/异常同拍多条 | 语义错 | 特殊指令独占提交拍 |
| CDB 双写 PRF 冲突 | 双驱动 | 双口 or 仲裁 |
| RS 唤醒少一口 | 源醒不来 | cdb0/cdb1 双匹配 |
| freelist 归还延迟 | 假 FL 满 | 提交 2 归还 2 |

---

## 7. 验收（自勾）

- [ ] 能画提交 2 宽与「连续 done 段」  
- [ ] 能说 store/特殊指令的独占策略  
- [ ] 每拍提交 ≥1.5 条均值（计数器）  
- [ ] 双 CDB 同时写回波形  
- [ ] cpu-tests 全绿；`sum`/`fib` IPC 提升

## 相关代码（改动点）

- `common/ooo_params.scala` — COMMIT_WIDTH / CDB_NUM  
- `unit/rob.scala` — 宽提交  
- `unit/rename.scala` — 双 commit  
- `unit/prf.scala` / `unit/rs.scala` — 双写口/双唤醒  
- `core/core.scala` — 双赢家仲裁

## 下一步

→ [11d](11d_阶段10d_MMU与虚拟内存.md)：虚拟内存（可选大项）。
