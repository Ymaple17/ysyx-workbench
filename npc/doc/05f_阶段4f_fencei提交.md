# 阶段 4f：FENCE.I @ ROB head

## 学习导航
- **理论目标**：本章先理解：仅当 fencei 成为 ROB head 时刷 ICache；握手完成前停住 head；完成后冲前端，PC=pc+4。
- **最小实现**：fence.i 到 ROB head 后等待 store 侧排空并完成 ICache invalidate，再 flush 前端并从 `pc+4` 重取。
- **当前参考核**：该路径已被阶段 10m 的 ICache/FetchBuffer/FQ/FTQ flush 继承；`t2_fencei_smc` PASS 是本章定向快照。
- **后续扩展**：4g 在提交间隙接外部中断，不能与 head side effect 或 redirect 竞争。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：4c–4e 绿（异常、ebreak、mret 的精确提交与 redirect 时序已通）。阶段 4f 先完成 fence.i@head；进入阶段 5a 实现 store@commit 后，再用严格自修改代码用例验收“store 全局可见后刷新 I$”的完整链路。  
**目标**：仅当 fencei 成为 ROB head 时刷 ICache；握手完成前停住 head；完成后冲前端，PC=pc+4。  
**本章阶段快照**：已落地 — baseline `t2_fencei_smc` PASS。

**铁律**：fencei 的架构效果（I$ 失效 + 后续取新码）必须发生在 **commit 语义点**，不能在 ID 提前刷。

---

## 1. 原理：自修改代码需要什么

### 1.1 语义（本核子集）

```text
sw    …          // 改内存里的指令字（D 侧可见）
fence.i          // 保证：之后的取指看到新指令
jal   …          // 跳到刚改过的地址
```

| 事 | 为何 |
|----|------|
| 失效 ICache | 否则命中旧码，SMC 永远看不到 store |
| 丢掉 fencei **之后**已取/译/执行的指令 | 那些可能带着旧码路径 |

### 1.2 为何必须认 ROB head

```text
older store 可能还在 ROB 未提交
younger 可能已经进 I$/FQ/ID
若 ID 一见 fencei 就刷 I$：
  → store 尚未写内存，刷完再取仍是旧码
  → 或 store 写了但 younger 已用旧码执行完
```

正确顺序：先提交完 fencei 之前所有指令（含改码 store）→ fencei 成 head 刷 I$ → 提交后下一拍冲 younger → 从 `pc+4` 重取。  
与 4c/4e「commit → RegNext → flush_all」同一模板。

---

## 2. 为何旧路径炸（ID hold 立刻刷）

| 项 | 旧 | **4f** |
|----|----|--------|
| 刷 I$ | ID 立刻 `icache.fencei` + hold | **head 且 commit_valid 才 valid** |
| 进 ROB | hold 卡住可能不 enq | **像普通指令 enq**（`is_fencei`） |
| 冲刷 | ID 握手下一拍冲 IF/ID | **commit 后一拍 `fencei_flush`** |
| 与 store | 可能 store 未架构写就刷 | **依赖 5a：store 先 commit 写** |

顺序核「碰巧」能过；乱序 + store@commit 后必炸。`idu.scala`：`fencei 像普通指令进 ROB，不再 hold 等 icache`。

---

## 3. 时序 / 规格

### 3.1 T0 / T1

```text
T0：head=fencei && done && icache.ready
    → commit_fire；fencei_commit；stop_issue；挡 younger wb
    → 锁存 fencei_pc_r := cm.pc
T1：fencei_flush = RegNext(fencei_commit)
    → rob/rs flush_all；IF/ID flush；PC := fencei_pc_r + 4
```

### 3.2 禁止冲刷前多提交

T0→T1 若再 `commit_fire` 一条 jal：difftest 会看到本应被冲掉的指令 → PC 错位。

```scala
rob.io.commit_fire := commit_valid && !fencei_flush && …
  && (!cm_is_fencei || fencei_commit_ready)
```

### 3.3 Irrevocable：valid 保持到 ready

ICache 清 set 多拍。`Irrevocable`：valid=1 后 ready 前不得拉低。

```scala
head_fencei_pending = commit_valid && !is_irq && !ext_irq_fire &&
  cm_is_fencei && !cm_writing
icache.io.fencei.valid := head_fencei_pending
fencei_commit_ready    := icache.io.fencei.ready
```

ready 前 `commit_fire` 挡住 → head 停住 → valid 持续；ready 当拍提交，下一拍 head 离开 valid 自然落。

---

## 4. 接口与源码要点

### 4.1 ROB / RS

`is_fencei` 随 enq 写入；fencei 仍走 RS→EX→WB 标 `done`（无副作用）；**刷 I$ 不在 EX**。

### 4.2 ICache

`Flipped(Irrevocable(…))`；`s_IFU_AR` 见 valid → `s_FENCEI`；逐 set 清；`ready := (cnt===set-1) && state===s_FENCEI`。

### 4.3 与 store SM 互斥

| 场景 | 行为 |
|------|------|
| head=store | `head_store_pending`；`!cm_is_fencei` |
| head=fencei | `head_fencei_pending`；`!cm_writing` |
| store 写中途 | 禁止拉 `icache.fencei` |

### 4.4 挡 younger / 断组合环

**不要**用 `cm_fire` 挡 younger（`can_wb→commit→cm_fire→can_wb` 环）：

```scala
wb_young_fencei = head.valid && is_fencei && done &&
  fencei_commit_ready && !fencei_flush && age(wb)>0
```

结构冲刷必须 `RegNext`：若当拍组合 `fencei_flush` 喂回 `can_wb`，会自我振荡。

---

## 5. 接线步骤

1. 拆掉 ID→icache；`idu.ifu_signals.ready := true.B`。  
2. ROB/RS 带 `is_fencei`；正常 enq。  
3. `head_fencei_pending` → `icache.fencei.valid`；ready → `fencei_commit_ready`。  
4. `commit_fire`：`!fencei_flush && (!is_fencei || ready)`。  
5. `fencei_commit` / `RegNext`→`fencei_flush`；锁存 PC；`correct_pc=pc+4`。  
6. `flush_all` + `stop_issue` / ID flush（T0 组合停灌入）。

---

## 6. 踩坑

| 坑 | 处理 |
|----|------|
| ID 仍立刻刷 | 删 hold 路径 |
| valid 抖落 | Irrevocable：ready 前保持 |
| 组合环 flush↔can_wb | 结构冲刷 RegNext |
| 冲刷前多提交 jal | `!fencei_flush` 门控 commit_fire |
| 与 store SM 抢 | `!cm_writing`；head 类型互斥 |
| 用 cm_fire 挡 younger | 改用 head+done+ready |
| fencei 未标 done | 仍须 WB 置 done |
| 刷后 PC=fencei 本身 | 用 `pc+4` |

---

## 7. `t2_fencei_smc.S` 解读

```text
patch: addi a0,x0,1 ; ret
sw 改成 addi a0,x0,2 → fence.i → jal patch  // 必须 a0=2
再改成 3，再 fence.i，再 jal               // 必须 a0=3
```

过测 ⇒ store 已在 fencei 前提交写内存；fencei@head 清旧 I$；flush 后重取到新码；两次都对说明不是碰巧 miss。

---

## 8. 验收（自勾）

- [ ] 能默写：为何 ID 立刻刷在乱序+store@commit 下必错  
- [ ] 能画 T0 commit / T1 flush_all，并指出 `pc+4`  
- [ ] 知 Irrevocable：ready 前 valid 不落  
- [ ] 知与 store SM 互斥及组合环断点  
- [ ] `t2_fencei_smc` PASS + cpu-tests 不回归  

---

## 9. 相关代码

- `core/core.scala` — `fencei_commit` / `fencei_flush` / `head_fencei_pending` / `wb_young_fencei`  
- `core/idu.scala` — 无 fencei_hold  
- `unit/rob.scala` / `rs.scala` — `is_fencei`  
- `unit/icache.scala` — Irrevocable fencei、`s_FENCEI`  
- `npc/tests/baseline_fix/t2_fencei_smc.S`  

---

## 下一步

→ [05g_阶段4g_外部中断.md](05g_阶段4g_外部中断.md)：提交间隙采样外部中断（同一套 T0/T1 flush_all 模板）。

---

## 附录 A：t2_fencei_smc 读法

测例在 `npc/tests/baseline_fix/t2_fencei_smc.S`，核心：

```text
1. 向 patch 地址 sw 一条新指令（自修改）
2. fence.i
3. jal patch；检查返回值是否为新语义
4. 再改一次、再 fence.i、再 jal
```

若 fencei 仍在 ID 立刻刷 I$：可能刷到「尚未提交的 store」之前，或冲刷时序与 ROB 不一致，jal 仍取到旧 I$ 行。  
若 fencei@head 但冲刷前多提交了 jal：difftest 会出现 **PC MISMATCH**（REF 已在 patch，DUT 仍提交 jal）。

对照波形时盯：

| 信号 | 期望 |
|------|------|
| `cm_bits.is_fencei` | head 为 fencei 时为 1 |
| `icache.fencei.valid/ready` | valid 保持到 ready |
| `commit_fire` | 仅 ready 当拍对 fencei 为真 |
| `fencei_flush` | 下一拍为真；ROB/RS 清空 |
| `correct_pc` | `fencei_pc+4` |

## 附录 B：与阶段 5 / 6 的关系

- **阶段 5 store@commit**：自修改的 `sw` 必须先提交写内存，fencei 才有「可见的新码」可刷。顺序上应先 5a，再严卡 4f 验收（本仓库已并行落地）。  
- **阶段 6 FQ**：fencei flush 必须同时清 FQ，否则队列里旧指令会在刷 I$ 后再次进入后端。

## 附录 C：实现检查清单（对照源码）

```text
[ ] ROBEntry / RSEntry 有 is_fencei；enq 从 idu.is_fencei 填入
[ ] idu 无 fencei_hold；ifu_signals.ready 在 core 侧恒真（不再驱动 icache）
[ ] icache.fencei 仅 head_fencei_pending 驱动
[ ] commit_fire 含 (!cm_is_fencei || fencei_commit_ready) && !fencei_flush
[ ] fencei_flush = RegNext(fencei_commit)；flush_all 含 fencei_flush
[ ] wb_young_fencei 挡 younger 写回
[ ] baseline t2 + cpu-tests 全绿
```
