# 阶段 4a：ROB 补齐 CSR / state 字段

## 学习导航
- **理论目标**：本章先理解：CSR/state 元数据进入 ROB，为提交写 CSR 做准备；**本档不断开 WBU 写**（可与 4b 同提交）。
- **最小实现**：把 CSR/state 编号、写值、异常种类等精确提交所需元数据随动态指令写入 ROB；本档可暂时保留旧 WBU 行为。
- **当前参考核**：阶段 12f 的 ROB 继续携带这些字段并按提交处理；本章只补元数据，不宣称副作用已经迁移。
- **后续扩展**：4b 关闭 WBU 架构 CSR 写，改由 commit 写 CSR，并对在飞 CSR 串行化。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：3d 绿。  
**目标**：CSR/state 元数据进入 ROB，为提交写 CSR 做准备；**本档不断开 WBU 写**（可与 4b 同提交）。  
**仓库**：已与 4b 一并落地（见 [05b](05b_阶段4b_CSR提交写.md)）。

**铁律**：commit 要用的信息必须在 **enq 进 ROB**（或 wb 写回项内）；不能指望提交拍再读 IDU/WBU 当前流水。

---

## 1. 原理

### 1.1 为何先补字段

`ROBEntry` 早已有 CSR / state 槽位，但 enq 未接线则永远是复位 0：

```text
commit_bits.csr_write / csr_sel / csr_waddr / csr_rd1 / rs1_val / state
  → 全 0 → 4b 无法在 commit 重建 wdata
```

| 档 | 做什么 | 行为变化 |
|----|--------|----------|
| **4a** | enq 填齐；波形能看见 | **仍 WBU 写 CSR**（≈3d） |
| **4b** | commit 用字段写；关 WBU.wen | 架构写点搬家 |

先 4a：字段错只在波形暴露；直接改写点会「字段错 + 写点错」叠在一起。

### 1.2 与「写回 ≠ 提交」同构

3a：BPU 要 `bp_index/jump/actual_taken` 活到 commit。  
4a：CSR 要 `csr_* / rs1_val` 活到 commit。

```text
ID 译码 / 组合读 CSR → enq 快照进 ROB
  →（可选）wb 覆盖 state / dest_val
  → commit_bits 带出 → 4b 写 CSR
```

乱序后完成序 ≠ head；提交只能信 **head 项内存的字段**。

### 1.3 为何本档不断开 WBU

| 若 4a 就关 WBU.wen | 后果 |
|--------------------|------|
| ROB 字段仍错 | CSR 两边都不写 → 全挂 |
| 无法 A/B | 分不清字段问题还是写点问题 |

过渡验收：**行为与 3d 一致**；只验证「字段已进 ROB」。

---

## 2. ROBEntry 字段表（对照 `unit/rob.scala`）

| 字段 | 类型 | enq 来源 | wb 改？ | commit 用途（4b） |
|------|------|----------|---------|-------------------|
| `csr_write` | Bool | `idu.signals.wbu.csr_write` | 否 | `wen = cm_fire && csr_write` |
| `csr_sel` | UInt(2) | `idu.signals.wbu.csr_sel` | 否 | 选 wdata |
| `csr_waddr` | UInt(12) | `idu.csr_waddr`（inst[31:20]） | 否 | 写地址 |
| `csr_rd1` | UInt(32) | `idu.csr_rd1`（ID 组合读） | 否 | CSRRS 旧值 |
| `rs1_val` | UInt(32) | `id_src1`（含旁路） | 否 | CSRRW/CSRRS 源 |
| `state` | `State` | `idu.state` | **是** `wb_state` | 4c 异常；本档先存齐 |
| `pc` | UInt(32) | `idu.pc` | 否 | `CSR_PC` |

`csr_sel`（`common/consts.scala` → `CSR_SEL`）：

| 常量 | 值 | 指令 | commit wdata |
|------|-----|------|--------------|
| `CSR_RD1` | 1 | CSRRW | `rs1_val` |
| `CSR_XOR` | 2 | CSRRS | `rs1_val \| csr_rd1` |
| `CSR_PC` | 3 | （扩展） | `pc` |

`CSR_XOR` 是仓库沿用的旧常量名，**不代表异或运算**；CSRRS 的组合语义是按位 OR。新写设计更适合命名为 `CSR_OR`，但本讲义保留旧名以便对照源码。

非 CSR：`csr_write=0`，其余可为 0。

---

## 3. enq 必填（`core/core.scala`）

```scala
rob.io.enq_bits.csr_write := idu.io.out.bits.signals.wbu.csr_write
rob.io.enq_bits.csr_sel   := idu.io.out.bits.signals.wbu.csr_sel
rob.io.enq_bits.csr_waddr := idu.io.out.bits.csr_waddr
rob.io.enq_bits.csr_rd1   := idu.io.out.bits.csr_rd1
rob.io.enq_bits.rs1_val   := id_src1
rob.io.enq_bits.state     := idu.io.out.bits.state
```

1. **`csr_rd1`**：入队瞬间 CSR 组合读快照；older 未提交写的脏读由 4b `csr_inflight` 防。  
2. **`rs1_val`**：必须是 **就绪后** 的 `id_src1`；未 ready 抓垃圾 → 4b 静默写错。  
3. **`state`**：ID 可能已标 ecall；访存异常靠 wb 覆盖。

RS 另有 `wbu_csr_*` 供流水透传；**架构写靠 ROB commit 字段**，不靠 RS。

---

## 4. 与 `wb_state` 的关系

`unit/rob.scala`：

```scala
when(io.wb_fire && entries(io.wb_idx).valid) {
  entries(io.wb_idx).done     := true.B
  entries(io.wb_idx).dest_val := io.wb_val
  entries(io.wb_idx).state    := io.wb_state  // 覆盖 enq
  // mem_addr / actual_taken / addr_ready …
}
```

core：`rob.io.wb_state := wbu.io.in.bits.state`。

| 阶段 | `state` |
|------|---------|
| enq | IDU（译码 / IF 异常） |
| wb | WBU 携带（含 LSU 合并） |
| commit | `commit_bits.state`（4c 同拍 head wb 另 Mux） |

**CSR 字段 wb 不改**——wdata 入队时已定。4a 不改写点，但 `state` 通路必须完整，否则 4c 读到过期 enq 值。

---

## 5. 不断开 WBU 时如何验收

```text
1. 波形：CSR enq 后项内 csr_write=1，waddr/sel/rs1_val/csr_rd1 正确
2. 对比 IDU 出口与 enq_bits：同拍一致
3. 跑 3d 基线：行为不变（仍 WBU 写）
4. 可选：绑某字段为 0，确认 4b 前功能不变（写点仍在 WBU）
```

---

## 6. 接线步骤

1. 确认 `unit/rob.scala` 已有 §2 字段（一般已有，只缺驱动）。  
2. `core/core.scala` enq 按 §3 赋值（勿只写 RS 忘 ROB）。  
3. 确认 `wb_state` 已接。  
4. 波形：enq → 项内 → `commit_bits` 一致。  
5. **不要**改 `wbu.csr.wen` / `csr.io.write`（→ 4b）。

---

## 7. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 只填 RS 不填 ROB | commit_bits 全 0 | 写 `rob.io.enq_bits` |
| `rs1_val` 未旁路 | 偶发写错 | 用 `id_src1` |
| 未 ready 就 enq | 抓垃圾 | 不放行（4b 显式 stall） |
| 以为 wb 填 `csr_rd1` | 永远 0 | csr 字段只 enq |
| `state` 不 wb | 访存异常丢 | 接 `wb_state` |
| 4a 就关 WBU | 与字段 bug 耦合 | 本档保持 WBU 写 |

---

## 8. 验收（自勾）

- [ ] 能指出 `ROBEntry` CSR/state 字段（对照 `unit/rob.scala`）  
- [ ] 能默写 enq：`csr_write/sel/waddr/rd1`、`rs1_val`、`state`  
- [ ] 能说明 `wb_state` 覆盖 `state`、不覆盖 `csr_*`  
- [ ] 波形：CSR 入队后字段正确  
- [ ] 本档单独：行为与 3d 一致（仍 WBU 写 CSR）  
- [ ] 知 4b 才迁 `csr.wen` 到 commit  

---

## 9. 下一步

→ [05b](05b_阶段4b_CSR提交写.md)：`csr.wen` 仅 commit；关 WBU；`csr_inflight` + rs1 ready。

---

## 附录 A：源码锚点

| 概念 | 位置 |
|------|------|
| `ROBEntry` | `unit/rob.scala`：`csr_*`、`rs1_val`、`state` |
| wb 写 `state` | 同文件：`entries(wb_idx).state := io.wb_state` |
| enq | `core/core.scala`：`rob.io.enq_bits.csr_*` 等 |
| ID 读 CSR | `core/idu.scala`：`csr_rd1`、`csr_waddr` |
| 控制 | `unit/control.scala`：`csr_write` / `csr_sel` |

---

## 附录 B：与 3a BPU 对照

| | 3a BPU | 4a CSR |
|--|--------|--------|
| enq | `jump/bp_*/inst` | `csr_*/rs1_val/state` |
| wb 补 | `actual_taken` | `state`（及 dest_val） |
| commit 用 | `bpu_update_*` | （4b）`csr.io.write` |
| 本档改副作用？ | 是 | **否**（只存货） |

4a = 装货；4b = 提交时拆封写架构。

---

## 附录 C：波形清单

```text
T_enq: csr_write=1；waddr==inst[31:20]；rs1_val/csr_rd1 正确
T_wb:  state 可能更新；csr_* 保持 enq 值
T_cm:  commit_bits.csr_* 与该项一致
```

通过标准：**字段正确，且回归不差于 3d**。
