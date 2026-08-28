# 阶段 4e：MRET @ ROB head commit

## 学习导航
- **理论目标**：本章先理解：MRET 可进 RS/EX 标 done，但 **不在 EX leave 当 mispred**；仅 head commit 时锁存 mepc，下一拍 PC←mepc 并冲后续。
- **最小实现**：mret 可先在 EX 标 done，但只在 ROB head 锁存 `mepc`，下一拍 flush 年轻项并跳转；不能把它当普通分支 mispredict。
- **当前参考核**：阶段 12f 仍与当前 NEMU 裸机口径对齐，只令 `dnpc=mepc`，未实现 U/S 模式和完整 mstatus 恢复。
- **后续扩展**：4f 把 `fence.i` 的 ICache 失效和前端重启也放到 ROB head。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：4c / 4d 绿（异常的 commit→下一拍 `flush_all` 时序和 ebreak 提交点已通）。  
**目标**：MRET 可进 RS/EX 标 done，但 **不在 EX leave 当 mispred**；仅 head commit 时锁存 mepc，下一拍 PC←mepc 并冲后续。  
**本章阶段快照**：已落地 — 与 NEMU 对齐（仅 `dnpc=mepc`，不恢复 mstatus）。

**铁律**：MRET 的架构效果（改 PC、丢弃 younger）只发生在 commit；EX 最多认出 `JUMP_MERT`，禁止走分支 mispred 冲刷。

---

## 1. 原理

### 1.1 本核 / NEMU 子集语义

完整 MRET 会恢复 privilege / `mstatus.MIE` 等。本教学核对齐：

```c
// nemu/.../riscv32/inst.c
#define MRET() { s->dnpc = CSR(0x341); }  // 只写 dnpc=mepc
```

```text
提交 MRET → 下一拍前端重定向到 mepc
不要在 MRET 时改写 mstatus（CSR 也无 MPP 恢复逻辑）
```

`csr.scala` 的 `mstatus` 仅可读（初值 `0x1800`）；irq 只写 `mepc/mcause`。MRET **不**走 `csr.io.write`。

### 1.2 为何 EX mispred 路径危险

控制表：`MRET → JUMP_MERT`；PC 选择有 `MEPC → csr.read.mepc`。若「leave EX 且 jump≠NONE」一律当分支完成：

```text
br_done 含 JUMP_MERT → 常判 taken → 预测多半顺序 → mispred
→ correct_pc=mepc，selective flush_idx=该 mret
```

| 问题 | 后果 |
|------|------|
| 尚未到 head 就冲前端 | 过早跳 mepc；与提交序错位 |
| selective 以 EX 中 mret 为界 | 保留/杀掉的集合错 |
| head commit 再 `mret_flush` | **双冲刷**，抢 `correct_pc` |
| BPU 把 MRET 当 jump | GHR/RAS 被污染 |

正确模型（同 fencei）：

```text
EX：只标 ROB.done，不改架构 PC
COMMIT：head 是 MRET → 锁存 mepc；停 issue；挡 younger wb
下一拍：flush_all + IF ← 锁存 mepc
```

---

## 2. 为何旧路径炸

| 项 | 旧（当分支） | **4e** |
|----|--------------|--------|
| 改 PC | EX leave → mispred → mepc | **commit 下一拍** `mret_flush`，PC←`mret_mepc_r` |
| 冲刷 | selective / `mis_predict_r` | **`flush_all`** |
| `br_done` | 含 `JUMP_MERT` | **排除** |
| BPU | `cm_is_jump` 含 MERT | **排除** |
| mstatus | 若「补全」恢复 | **跳过**（对齐 NEMU） |

顺序核上 EX≈马上提交可能蒙混；3d+4c 后必须拆开，否则 yield-os / CTE 会 DIFFTEST 或 HANG。

---

## 3. 时序（对齐 fencei）

```text
T0：head=mret && done → commit_fire
    mret_commit=1；stop_issue
    wb_young_mret 挡 younger 写 PRF
    mret_mepc_r := csr.read.mepc

T1：mret_flush = RegNext(mret_commit)
    rob/rs flush_all；IF/ID flush
    ifu.correct_pc = mret_mepc_r
    rename/busy 空窗口 rebuild
```

**禁止** T0→T1 提交 younger：`commit_fire` 加 `!mret_flush`。  
**禁止** EX 对 `JUMP_MERT` 产生 `mis_predict`：`br_done` 排除之。

| | fencei（4f） | mret（4e） | ecall（4c） |
|--|-------------|------------|-------------|
| T0 | commit（可等 I$ ready） | commit；锁存 mepc | commit；写 mepc/mcause |
| T1 | flush_all；PC=pc+4 | flush_all；PC=mepc | flush_all；PC=mtvec |
| EX | 无 | **排除 br_done** | 认 state |

---

## 4. 接口与源码要点

### 4.1 译码

`control.scala`：`MRET → JUMP_MERT`（一般不改）。  
`pc.scala` 仍可有 `JUMP_MERT→MEPC`；只要 `br_done` 排除，就不会用它去 mispred。

### 4.2 core：提交 / 冲刷 / 挡写回

```scala
val cm_is_mret = cm_bits.jump === JUMP_MERT
mret_commit := cm_fire && cm_is_mret
mret_flush  := RegNext(mret_commit, false.B)
val mret_mepc_r = RegEnable(csr.io.read.mepc, 0.U, mret_commit)

val br_done = leave_ex && (jump =/= JUMP_NONE) && (jump =/= JUMP_MERT)

val wb_young_mret = head_e.valid && (head_e.jump === JUMP_MERT) && head_e.done &&
  !mret_flush && (robAge(wb_idx, rob.io.head) > 0.U)
// can_wb 加 && !wb_young_mret
// commit_fire 加 && !mret_flush
```

```scala
rob.io.flush_all := is_irq || fencei_flush || mret_flush
rob.io.flush := flush_now && !is_irq && !fencei_flush && !mret_flush
stop_issue := … || mret_commit || …
ifu.io.is_flush := is_irq || fencei_flush || mret_flush || mis_predict_r
```

### 4.3 `correct_pc` 优先级

```scala
ifu.io.correct_pc := Mux(is_irq, irq_mtvec_r,
  Mux(mis_predict_r, correct_pc_r,
    Mux(fencei_flush, fencei_pc_r + 4.U,
      Mux(mret_flush, mret_mepc_r, 0.U))))
```

**irq > mispred > fencei > mret**。多源同拍时先查是否双触发。

### 4.4 BPU 排除

```scala
val cm_is_jump = cm_fire && (cm_jump =/= JUMP_NONE) && (cm_jump =/= JUMP_MERT)
```

### 4.5 mstatus 故意跳过

NEMU 只改 dnpc；NPC 也不 wen mstatus。若按手册「补全」恢复而 NEMU 没有，difftest **必炸**。

---

## 5. 接线步骤

1. MRET 能 enq 并在 EX 标 done。  
2. **先** `br_done` 排除 `JUMP_MERT`。  
3. 加 `mret_commit` / `mret_flush` / `mret_mepc_r`（抄 fencei）。  
4. `commit_fire`←`!mret_flush`；`can_wb`←`!wb_young_mret`。  
5. `flush_all` / `stop_issue` / IF·ID flush 接上。  
6. `correct_pc` mux 加入 mret（注意优先级）。  
7. `cm_is_jump` 排除；**不要**恢复 mstatus。  
8. cpu-tests + yield-os 冒烟。

---

## 6. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| EX 仍当 taken 分支 | 提前跳 / 双 flush | `br_done` 排除 |
| 冲刷前多提交 | difftest PC 错位 | `!mret_flush` 门控 commit |
| younger 在 T0 写 PRF | 架构寄存器脏 | `wb_young_mret` |
| BPU 被污染 | 此后分支乱飘 | `cm_is_jump` 排除 |
| 与 fencei 抢 mux | 错误 PC | 固定优先级 |
| 组合环 flush↔can_wb | 挂仿真 | 结构冲刷 `RegNext` |
| 恢复 mstatus | 与 NEMU DIFF | **跳过** |
| 用 selective 而非 flush_all | ROB 残留 | mret 已提交 → `flush_all` |
| T1 组合读 mepc 未锁存 | CSR 变化则偏 | `RegEnable` @ commit |

---

## 7. 验收（自勾）

- [ ] 能画 T0/T1（对齐 fencei，PC=mepc）  
- [ ] 能说明 EX mispred 对 MRET 为何危险  
- [ ] 知 `br_done` / `cm_is_jump` 两处排除  
- [ ] 知不恢复 mstatus 是为对齐 NEMU  
- [ ] 能默写 `correct_pc` 优先级  
- [ ] cpu-tests 绿  
- [ ] 有 yield-os 则 timeout 冒烟：有输出、无 DIFFTEST/HANG  

---

## 8. 下一步

→ [05f](05f_阶段4f_fencei提交.md)：同模板，PC=pc+4，多 icache 握手。  
→ [05g](05g_阶段4g_外部中断.md)：提交间隙采样，同样下一拍 `flush_all`。

---

## 附录 A：yield-os

```text
ecall 进内核（4c：mtvec）→ … → mret 返回（4e：mepc）
```

若 mret 仍走 EX mispred：handler 未提交完就跳回，或 commit 后再 flush → 与 NEMU 步数错位。  
冒烟：超时跑一段，有输出、无 DIFFTEST、无 HANG。

---

## 附录 B：代码锚点与时间线

| 文件 | 看什么 |
|------|--------|
| `core/core.scala` | `br_done`；`mret_*`；`wb_young_mret`；`correct_pc`；`cm_is_jump` |
| `unit/control.scala` | `MRET → JUMP_MERT` |
| `core/csr.scala` | 读 mepc；无 MRET 写 mstatus |
| `nemu/.../inst.c` | `MRET()` 宏 |

```text
EX leave：done=1；br_done=0 → 无 mis_predict
T0 commit：mret_commit；锁存 mepc；stop_issue；挡 younger wb
T1 mret_flush：flush_all；PC←mret_mepc_r
```

若 EX leave 就看到 `mis_predict` / `ifu.is_flush`，排除没做严。
