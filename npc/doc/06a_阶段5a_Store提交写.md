# 阶段 5a：Store 架构写迁到 commit

## 学习导航
- **理论目标**：本章先理解：store 对内存的修改只发生在 **ROB head 提交**；LSU 只算地址/数据，写通道恒静默。
- **最小实现**：LSU 只计算 store 地址/数据/掩码并写 ROB；只有 head commit 状态机发 AW/W、等待 B 后才退休。
- **当前参考核**：5a/5b 最小语义已落地；普通 PMEM store 提交进 Stage12f 的 StoreBuffer16，可合并并组成写 burst，不等待总线 B；执行完成走私有 sideband，MMIO 仍直写独占。
- **后续扩展**：5b 去掉 `mem@head` 对 load 的全局限制，补更老 store 冲突检测与前递。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：4c 绿（异常已认 ROB head；精确异常要求未提交 store 可「没发生过」）。  
**目标**：store 对内存的修改只发生在 **ROB head 提交**；LSU 只算地址/数据，写通道恒静默。  
**本章阶段快照**：与 5b 一并落地（见 [06b](06b_阶段5b_Load冲突与去memhead.md)）。

**铁律**：执行序可以先算 `mem_addr` / `mem_wdata`，但 AXI 写（aw/w/b）只能由 **commit 状态机** 发起；wrong-path store 未提交则永不改内存。

---

## 1. 原理

### 1.1 为何执行时写会炸

精确异常 + 乱序：

```text
错误路径：store 已 leave LSU 并写完 AXI → mispred 杀该项
内存已脏，无法回滚 → 后续正确路径 load / difftest 必炸
```

mem@head 时代靠「访存只发 head」减少曝光，但 **已 leave 的 wrong-path store 仍可能写完**（mispred 故意不冲 LSU，防丢更老指令）。根本解法：**提交才写**。

### 1.2 两个事件

| 事件 | 做什么 | 总线 |
|------|--------|------|
| LSU / WB | 算地址与 store 数据；标 `done`；写 `mem_addr`/`mem_wdata`/`addr_ready` | **不碰写通道** |
| Commit SM | head 是 store 且地址齐 → aw/w → 等 b → **该拍** `commit_fire` | 独占 dmem 写 |

```text
difftest：每条指令仍一个 commit 脉冲
store 多拍写总线 → 在 b 握手那一拍再 commit_fire（写完再退休）
```

### 1.3 与 load 的分工（本档先做 store）

5a 只迁 store 写；load 仍可走 LSU 读。完整冲突检测与去 mem@head 见 **5b**。但 5a 落地时 dmem 仲裁（读←LSU、写←commit）必须一次做对，否则 5b 无法叠。

---

## 2. 为何旧路径炸

| 项 | 旧 | **5a** |
|----|----|--------|
| store AXI 写 | LSU 执行时 aw/w/b | **commit SM** |
| LSU store | 多拍占写通道 | **单拍完成**（`not_bus`） |
| ROB | 缺 / 未用 `mem_wdata` | wb 填 `mem_wdata` + `addr_ready` |
| commit_fire | head.done 即退 | **store 写完 b 才 fire** |
| wrong-path store | 可能已写脏 | 未提交 → SM 不启动 |

---

## 3. 时序 / 数据流

```text
enq：mem_valid/write/wmask；addr_ready=0；mem_wdata 空
LSU：store 单拍；透传 rd2 → store_data；不发 aw/w
wb：mem_addr、mem_wdata、addr_ready=1；done=1
commit：
  head 是 store 且 addr_ready（或同拍 head wb）
  → SM：IDLE→W(aw/w)→B
  → bvalid&&bready 那拍 store_commit_ready=1 → commit_fire
```

同拍 head 写回：ROB 寄存器当拍组合读仍可能旧，SM 用 `head_wb_store` 旁路 WBU 的地址/数据。

```text
head_addr_rdy = cm_bits.addr_ready || head_wb_store
head_st_addr  = Mux(head_wb_store, wbu.alu_result, cm.mem_addr)
head_st_data  = Mux(head_wb_store, wbu.store_data, cm.mem_wdata)
```

---

## 4. 接口与源码要点

### 4.1 ROB 字段

`unit/rob.scala`：

```scala
val mem_wdata  = UInt(32.W) // 原始 rs2（未移位）；commit 写时再按 addr 对齐
val addr_ready = Bool()     // wb 后地址/数据齐
// wb 时：
entries(wb_idx).mem_addr   := wb_mem_addr
entries(wb_idx).mem_wdata  := wb_mem_wdata
entries(wb_idx).addr_ready := true.B
```

enq 时 `addr_ready := false.B`。

### 4.2 LSU：写通道恒静默

```scala
// core/lsu.scala
io.dmem.awvalid := false.B
io.dmem.wvalid  := false.B
io.dmem.bready  := false.B
val is_store = mem_write && mem_valid
val not_bus  = !is_load   // store / 非访存：单拍
io.out.bits.store_data := io.in.bits.rd2
io.bus_busy := (state === s_WORK) || (state === s_FLUSH) ||
  (is_load && idle && (ar_handshake_done || io.dmem.arvalid))
```

`bus_busy`：有未完成 load 事务时 commit 写必须等（xbar 单事务）。

### 4.3 commit store SM

```scala
val s_CM_IDLE :: s_CM_W :: s_CM_B :: Nil = Enum(3)
// IDLE：head_store_pending && !lsu.bus_busy → 锁存 addr/wdata/wstrb → W
// W：aw 与 w 可乱序完成；都完成后 → B
// B：bvalid&&bready → IDLE；同拍 store_commit_ready=1

store_commit_ready := (cm_st_state === s_CM_B) && io.dmem.bvalid && io.dmem.bready
rob.io.commit_fire := … && (!cm_is_store || store_commit_ready)
```

进入写事务前，`commit_gap` 必须排除 store 状态机忙碌，因此外部中断不能在事务中途被采样。AW 或 W 任一通道一旦握手，事务就不可取消；即使之后出现 redirect 请求，也必须保持状态直到另一通道和 B 响应完成。**mispred 不得**打断正在 commit 的更老 store，否则 AXI/xbar 会卡死。

### 4.4 dmem 仲裁

```text
读 ← LSU（ar/r）；写 ← commit SM（aw/w/b）
cm_writing 期间：关掉 LSU 的 arvalid/rvalid 旁路，禁止新 AR
LSU 侧 awready/wready/bvalid 恒假（写口不接 LSU）
```

### 4.5 xbar 铁律

`bus/xbar.scala`：进入 DMEM 时 `dmem_is_write := awvalid`；退出必须匹配读/写完成条件。残留 `rvalid` 打断写会卡死从设备。另：**dmem 优先于 imem**，避免 IF 连取时 awready 永不来。

---

## 5. 接线步骤

1. ROB 增加/接通 `mem_wdata`、`addr_ready`；enq 清零，wb 写入。  
2. LSU：关掉 aw/w/b；store 改单拍；透传 `store_data`。  
3. core：实现 commit SM；`commit_fire` 门控 `store_commit_ready`。  
4. 同拍 head wb 旁路地址/数据。  
5. dmem：读 LSU、写 SM；`cm_writing` 禁新 AR。  
6. irq 可复位 SM；mispred **不要**复位正在进行的 commit 写。  
7. `io.commit_mem_addr` 对 store 用 `cm_st_addr`（与写同一拍）。  
8. 跑 cpu-tests；再叠 5b。

---

## 6. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| LSU 仍写总线 | wrong-path 脏内存 | 写通道恒 0 |
| done 即 commit | 写未完成就退休 | 等 b 才 `store_commit_ready` |
| 同拍 head 用 ROB 旧值 | 地址/数据错 | `head_wb_store` Mux |
| mispred 打断 commit 写 | AXI/xbar 挂死 | 只允许 irq 取消 |
| 写中途仍发 AR | xbar 读写真并发失败 | `cm_writing` 关 AR |
| IF 抢总线 | store awready 不来 | xbar dmem 优先 |
| wdata 未按 offset 移位 | 字节/半字写错位 | `<< (off<<3)` + wstrb |
| `bus_busy` 未等 | 与 load 事务重叠 | IDLE 入口检查 `!lsu.bus_busy` |

---

## 7. 验收（自勾）

- [ ] 能说明为何执行时 store 与精确异常冲突  
- [ ] store 仅 commit SM 写内存  
- [ ] LSU 写通道恒静默；store 单拍  
- [ ] 知 `addr_ready` / `mem_wdata` 何时写入  
- [ ] 知 mispred 为何不能取消更老 commit 写  
- [ ] wrong-path store 未提交不改内存  
- [ ] cpu-tests 绿（可与 5b 一并验）  

---

## 8. 相关代码

- `core/core.scala` — commit store SM、`store_commit_ready`、dmem 仲裁、`head_wb_store`  
- `core/lsu.scala` — store 不写总线；`bus_busy`；`store_data`  
- `unit/rob.scala` — `mem_wdata` / `addr_ready`  
- `bus/xbar.scala` — `dmem_is_write`；dmem 优先  

---

## 9. 下一步

→ [06b](06b_阶段5b_Load冲突与去memhead.md)：去掉 mem@head；ROB 扫描前递；`rob_st_pending` 防死锁。

---

## 附录 A：SM 状态口诀

```text
IDLE + head_store_pending + !bus_busy → 锁存 → W
W：aw_done && w_done → B
B：b 握手 → IDLE，同拍 commit_fire（store）
中断采样条件排除 W/B 状态；已接受的事务必须排空到 B 握手，禁止强制回 IDLE
mispred → 不碰 SM
```

---

## 附录 B：与 difftest

```text
commit_valid = cm_fire          // store 在 b 拍才为真
commit_mem_addr = cm_st_addr    // 与实际 awaddr 一致
commit_is_load 仍按 reg_write_sel === MEM_SEL
```

写多拍期间 head 停住（`commit_valid` 可为真但 `commit_fire` 假），对外仍是「一指令一次提交边沿」。
