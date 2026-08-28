# 阶段 2c：写侧 freelist

## 学习导航
- **理论目标**：本章先理解：打开 **真分配** `pdest=dest_phys`；commit 回收 `old_phys`；flush **重建** free/RAT。
- **最小实现**：仅对 `reg_write && rd!=0` 分配新 phys，ROB 保存 `old_phys`，提交回收旧映射，flush 从已提交映射重建 RAT/FreeList。
- **当前参考核**：阶段 12f 使用 2-wide `Rename2`、PRF64 和 checkpoint；本章单宽规则仍是所有宽化版本必须保持的不变量。
- **后续扩展**：2d 把源寄存器、Busy stall 和前递键也统一到 phys 域，完成顺序后端迁移。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：2b 绿。  
**目标**：打开 **真分配** `pdest=dest_phys`；commit 回收 `old_phys`；flush **重建** free/RAT。  
**源操作数仍用 arch_rf + arch 前递**（先不要读 RAT 源）。

---

## 1. 原理：只改「写侧模型」

```text
写：每次 do_rename → 新物理槽
   PRF[new] 在写回时更新
   提交后 arch 意义落在 new 上，old 可回收

源：暂时仍按「架构寄存器最新已提交/旁路值」
   用 arch_rf + 流水前递（ioid 同款）
```

为何源不一起改？

- 前递网仍按 **arch waddr** 比中  
- 若源改 phys、前递仍 arch → 必错  
- 分两步：2c 写侧，2d 源侧 + 前递改 pdest  

---

## 2. Rename 打开

```scala
rename.io.fire := en_id_ex
rename.io.rs1/rs2/rd/reg_write := ...
// 输出
id_pdest    = rename.io.dest_phys
id_oldphys  = rename.io.old_phys
id_dorename = rename.io.do_rename && !rename.io.fl_empty
```

### 2.1 freelist 空

```scala
fl_stall = id_doren && rename.io.fl_empty
// idu_ren.valid/ready 与 is_stall 都要考虑 fl_stall
```

### 2.2 ROB / Bundle

```scala
enq.new_phys := id_pdest
enq.old_phys := id_oldphys
idu_ren.pdest/old_phys/do_rename := ...
```

### 2.3 写 PRF

```scala
prf.waddr := wb_pdest   // 不再是 arch rd
```

### 2.4 提交回收（2c 仍可跟 WBU 绑）

```scala
rename.commit_fire := wb && do_rename
cm_old/new/arch_rd := 自 WBU 透传字段
arch_rf(rd) := wdata   // difftest 快照仍按 arch
```

---

## 3. flush 重建（核心）

### 3.1 禁止

```text
freeBits := ((1<<N_PHYS)-1) ^ ((1<<32)-1)   // 一键 32..N_PHYS-1 空闲
```

错误路径与正确路径共享过 freelist 时，硬重置会 **重复分配** 或 **泄漏**。

### 3.2 正确思路

冲刷后仍然有效的映射 =  

1. **已提交** `arch_rat`（可含同拍 commit）  
2. 再加上 ROB 上 **保留区间** `[rb_head, flush_idx]` 里每条指令按序覆盖的 `new_phys`  

空闲集 = 全体 phys − 上述占用 − 保留项的 `old_phys`（old 要等 commit 才真正 free）。

```text
rb_base[i] = arch_rat[i] （叠同拍 commit 的 new）
for off in 0..kept-1:
  e = ROB[rb_head+off]
  if e 写寄存器: rb_rat[e.arch_rd] = e.new_phys

used = bit0 | ∪ rb_base[1..31] | ∪ kept.new | ∪ kept.old
free = ~used
rename.rebuild = 1
rename.rebuild_rat = rb_rat
rename.rebuild_free = free
```

`rb_head`：若本拍 commit 了 head，则从 head+1 起算保留（与 ROB 指针更新一致）。

### 3.3 与 rename 模块

`Rename` 已提供 `rebuild/rebuild_rat/rebuild_free` 优先分支；core 负责 **组合算出** 两表。

---

## 4. Busy（本阶段可选）

可开始 `set(pdest)@enq`、`clr@wb`，但源不用 busy stall 也能靠 arch 前递活（2d 再强制 busy）。

---

## 5. 时序与互斥

- `en_id_ex` 必须在 flush 当拍为假（`!exu.is_flush`），避免错误路径 alloc  
- rebuild 与 alloc 同拍：rebuild 优先，不 alloc  

---

## 6. 踩坑

| 现象 | 原因 | 处理 |
|------|------|------|
| 开 fire 后 a0/sp 错 | 源仍 arch 且 flush 回收错；或 restore 误用 | 源保持 arch；用 rebuild 而非瞎 restore |
| freelist 很快 empty | commit 未 free old；或 rebuild 漏 | 查 cm_old、kept.old |
| 多路 `used(arch_rat(a)) := true.B` | 重复地址和连接优先级不等价于集合并集 | 对各映射生成 one-hot 后 OR（阶段 1 已写） |
| 同拍 commit+flush | 基线漏叠 | rb_base 含 commit |

### 6.1 历史结论

| 组合 | 结果 |
|------|------|
| 恒等写 + arch 源 | 绿（2a） |
| freelist 写 + arch 源 + 错误 flush | 炸 |
| freelist 写 + arch 源 + 正确 rebuild | 可绿（2c） |
| freelist 写 + phys 源 | 2d |

---

## 7. 验收（自勾）

- [ ] 波形见 pdest≥32  
- [ ] flush 后 freeBits 不是「简单的 32..N_PHYS-1 全 1」硬重置形态
- [ ] dummy/add 绿  
- [ ] 能讲清 rebuild 输入如何从 ROB 扫出  


---

## 附录 A：core 打开 freelist 的最小 diff

```scala
// 之前
rename.io.fire := false.B
id_pdest := Mux(doren, rd, 0.U)

// 之后
rename.io.fire := en_id_ex
id_pdest := rename.io.dest_phys
id_old   := rename.io.old_phys
// ROB/enq/Bundle 全部用 id_pdest/id_old
// 源：继续 arch_rf + RsForward(arch)
```

---

## 附录 B：rebuild 组合逻辑伪代码（与仓库一致思路）

```scala
cm_this = 本拍是否 commit head
rb_head = cm_this ? head+1 : head
kept_n  = 保留条数（irq 可为 0）

rb_base = arch_rat
if (cm_do_ren) rb_base[cm_rd] = cm_new

rb_steps(0) = rb_base
for off in 0 until ROB_SIZE:
  take = off < kept_n && entry 写寄存器
  rb_steps(off+1)[rd] = take ? new_phys : rb_steps(off)[rd]

// free
archUsed = bit0 | or_i (1<<rb_base[i])
free = ~archUsed
for each kept write entry:
  free &= ~(1<<new); free &= ~(1<<old)

rename.rebuild = flush_now
rename.rebuild_rat = rb_steps.last
rename.rebuild_free = free
```

注意：`fold`/`steps` 用 Vec 链式 Mux，避免在 for 里 `var` 与硬件语义不清。

---

## 附录 C：验证 freelist 的波形信号

- `rename.freeBits`（可 dontTouch 或 printf）  
- `dest_phys` 第一次应为 32  
- 连续消耗完复位时的 `N_PHYS-32` 个空闲槽后应接近 empty，或已经依靠 commit 回收
- flush 后 free 数量应 **增加**（回收错误路径 new），而不是跳回固定掩码  
