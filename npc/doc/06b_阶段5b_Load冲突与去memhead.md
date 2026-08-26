# 阶段 5b：Load 冲突检测 + 去掉 mem@head

## 学习导航
- **理论目标**：本章先理解：去掉 mem@head；load 与更老未提交 store 正确转发/等待；无总线死锁。
- **最小实现**：load 可在非 ROB head 执行；检查所有更老 store，完整覆盖则前递，地址未知/部分重叠则等待，无冲突才读总线。
- **当前参考核**：本章 21/21、35/35 与 microbench PASS 是阶段快照；阶段 10m 已用独立 SQ + StoreBuffer + LQ/replay 模块化同一规则。
- **后续扩展**：阶段 6 加 FetchQueue；内存侧随后在 8d/10a/10l 演进为独立 SQ、StoreBuffer 和 LoadQueue。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：5a（store@commit；dmem 读←LSU、写←commit SM）。  
**目标**：去掉 mem@head；load 与更老未提交 store 正确转发/等待；无总线死锁。  
**本章阶段快照**：参考实现已落地 — **单测 21/21 + cpu-tests 35/35 + microbench(test) PASS**。

**铁律**：放开访存发射后，必须按 **程序序** 查更老 store；`!addr_ready` 的更老 store 在 **RS** 挡 load，禁止进 LSU 空等（单 FU 死锁）。

---

## 1. 原理

### 1.1 mem@head 在挡什么

阶段 5 前：`mem_valid ⇒ 仅 rob_idx==head 可 issue`。这用顺序发射掩盖了：

```text
年轻 load 先于年老 store 完成 → 可能读到旧内存 / 错过转发
load 占 AR 等更老 store → 与 commit 写抢 xbar → 死锁
```

5a 已让 store 提交才写内存；5b 放开发射后，**语义正确性**靠 ROB 扫描 + RS 门控。

### 1.2 三个层次

| 层 | 职责 |
|----|------|
| RS | load：存在更老 `rob_st_pending`（valid store 且 `!addr_ready`）→ **不发** |
| core 组合扫描 | load 已在 LSU：同字全覆盖→转发；部分重叠→`st_fwd_wait`（不发 AR） |
| LSU | 无 wait/fwd → AXI 读；fwd → 单拍扩展写回；wait → 不 ready |

```text
程序序： ... older store ... load ...
执行序： load 可能先 ready；必须看见 older store 的地址/数据约束
```

### 1.3 年龄

用相对 head 的距离，禁止裸比指针：

```scala
def robAge(idx, head) = (idx - head)(PTR_W-1, 0)
// older ⇔ eAge < ld_age
```

---

## 2. 为何旧路径 / 半吊子方案炸

| 项 | mem@head 时代 | 只去 mem@head 不做冲突 | **5b** |
|----|---------------|------------------------|--------|
| 访存发射 | 仅 head | any-ready | any-ready + olderCtrl + **st_pending** |
| load vs 更老 store | 靠顺序掩盖 | 读错 / 脏序 | 扫 ROB：fwd / wait |
| 等 `!addr_ready` | 少见 | 在 LSU 等 → **死锁** | **RS 不发** |
| 转发数据 | — | 漏符号扩展 | 按 `mem_rd` 扩展 |

典型死锁：

```text
load 已进 LSU，st_fwd_wait 等更老 store 的 addr_ready
store 卡在 RS/EX 后面（单 LSU）→ 永远齐不了 → 挂死
```

---

## 3. 时序 / 规则

### 3.1 RS：`rob_st_pending`

```scala
// core：驱动
rs.io.rob_st_pending := VecInit(rob.io.entries.map(e =>
  e.valid && e.mem_valid && e.mem_write && !e.addr_ready
)).asUInt

// rs：load 门控
val olderStPend = ∃k: rob_st_pending(k) && age(k) < age(load.rob_idx)
canIssue(load) := ready && !olderCtrl && !olderStPend
```

部分重叠等待可在 LSU（`st_fwd_wait`），此时更老 store **已** `addr_ready`，可完成执行/写回，不堵发射。

### 3.2 ROB 扫描（core）

```text
对 LSU 当前 load：
  older = valid store && age < ld_age
  sameWord = addr_ready && 同字地址
  fullCover = (load_mask ⊆ store_mask) → stFwdHits
  overlap && !fullCover → stWaitHits

选“最年轻的 older store”（所有命中项里 age 最大、离 load 最近者）作为转发源
st_fwd_wait = any wait
st_fwd_valid = any fwd && !wait
```

mask 按字节使能左移 offset；数据 `storeShiftData` 后再右移到 load 的 byte lane。

### 3.3 LSU 行为

```scala
can_issue_load = is_load && !st_fwd_wait && !st_fwd_valid
fwd_done       = is_load && st_fwd_valid && !st_fwd_wait
// 前递也必须按 mem_rd 做符号/零扩展（否则 lbu → 0xffffff80）
ready := not_bus || fwd_done || dmem.rvalid
```

| 情况 | AR | 完成 |
|------|----|------|
| 全覆盖转发 | 不发 | 单拍 `fwd_ext` |
| 部分重叠 | 不发 | 等 wait 消失 |
| 无匹配 | 发 AR | 等 rvalid |

---

## 4. 接口与源码要点

### 4.1 去掉 mem@head

`unit/rs.scala`：不再要求 `rob_idx==rob_head`。改为 `any-ready` + `olderCtrl`（更老未决跳转挡后发）+ load 的 `olderStPend`。

### 4.2 转发扩展（易漏）

```scala
val fwd_ext = MuxLookup(mem_rd, …)(Seq(
  RBYTE  -> sext8, RHALF -> sext16, RWORD -> word,
  RBYTEU -> zext8, RHALFU -> zext16
))
io.out.bits.mem_read := Mux(fwd_done, fwd_ext, mem_from_bus)
```

### 4.3 与 5a 仲裁共存

```text
load 因 wait 不发 AR → 不占 bus_busy → commit store 可写
load 已在 WORK → bus_busy → commit SM 停在 IDLE
cm_writing → 关新 AR（即使另一条 load ready）
```

### 4.4 flush

错误路径 store：`valid=0` → 不再匹配 fwd/pending，**不会**进 commit SM。  
已发出的 load：irq 时 LSU flush；mispred 不冲 LSU（与既有策略一致），靠 `can_wb` 门控。

---

## 5. 接线步骤

1. 确认 5a：`addr_ready`/`mem_wdata`、commit SM、LSU 不写。  
2. core 驱动 `rs.rob_st_pending`。  
3. RS：删 mem@head；加 load 的 `olderStPend`。  
4. core：实现 `stFwdHits` / `stWaitHits` / 最年轻 older 优先；接 `lsu.st_fwd_*`。  
5. LSU：`can_issue_load` / `fwd_done` / `fwd_ext`。  
6. 自建：sw+lw、中间插 div、sb/lb 部分重叠、wrong-path store。  
7. 全量 cpu-tests + microbench(test)。

---

## 6. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 在 LSU 等 `!addr_ready` | 单 FU 死锁 | RS `rob_st_pending` 挡 |
| 转发漏扩展 | `lbu` 得 `0xffffff80` | `fwd_ext` 按 `mem_rd` |
| 部分重叠当全字 fwd | 数据错 | `fullCover` vs `overlap` |
| 选最老的匹配 store | 连续同地址 store 时读到旧值 | 在全部 older 命中中选年龄最大的项，即离 load 最近的 store |
| 年龄用指针直比 | 环绕错 | `robAge` 相对 head |
| 去 mem@head 忘 olderCtrl | 越过未决分支 | 保留 jump 门控 |
| wait 时仍拉 arvalid | 占总线 | `can_issue_load` 含 `!wait` |
| store 未 wb 就 fwd | 地址垃圾 | 要求 `addr_ready` |

---

## 7. 验收（自勾）

- [ ] 能画 load 与更老 store 的 RS / core 扫描 / LSU 分工  
- [ ] 知为何不能在 LSU 里等 `!addr_ready`  
- [ ] 能默写 fullCover / 部分重叠 / 无匹配 三种结果  
- [ ] 知转发必须做符号/零扩展  
- [ ] 知 `rob_st_pending` 位如何定义  
- [ ] 单测 + 35/35 + microbench(test)  
- [ ] 能口述 wrong-path store 被杀后 load 看不到其值  

---

## 8. 相关代码

- `unit/rs.scala` — `rob_st_pending`；去 mem@head；`olderStPend`  
- `core/core.scala` — ROB 扫描前递；`loadMaskBytes` / `storeMaskBytes`  
- `core/lsu.scala` — `st_fwd_*`；`fwd_ext`；`can_issue_load`  
- `unit/rob.scala` — `addr_ready` / `mem_wdata`（5a 已加）  

---

## 9. 下一步

阶段 5 完成。  

→ [07_阶段6_前端.md](07_阶段6_前端.md)：前端 / BPU。  
→ [08_阶段7_调优.md](08_阶段7_调优.md)：调优。

---

## 附录 A：决策表 + 微测

```text
更老 store !addr_ready     → RS 不发 load
更老 store 同字全覆盖      → 转发（扩展）
更老 store 同字部分重叠    → LSU wait，不发 AR
无同字重叠 / 无更老 store  → AXI 读

微测：sw+lw；中间插 div；wrong-path store；sb/lhu 部分重叠；连续命中未提交 store（无死锁）
```

| 规格（06 总览） | 本核 |
|----------------|------|
| 扫 ROB / 未齐地址 | 扫 ROB；**RS 挡** `!addr_ready` |
| 全覆盖 / 部分重叠 / 无匹配 | fwd+扩展 / wait / AXI |
| 去 mem@head | RS `canIssue` 已改 |
