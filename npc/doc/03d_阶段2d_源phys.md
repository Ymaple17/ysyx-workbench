# 阶段 2d：源 phys + Busy stall + pdest 前递

## 学习导航
- **理论目标**：本章先理解：源操作数与写侧统一为 **物理寄存器模型**；这是顺序后端最后一档，之后才允许谈 RS。
- **最小实现**：rename 输出 `psrc1/psrc2`，BusyTable 决定源就绪，PRF/前递都按 phys 索引；保持顺序发射并通过 difftest。
- **当前参考核**：阶段 11d 仍遵守同一 phys 数据模型，但已由 RS/CDB 同拍唤醒和双发射取代这里的顺序 stall。
- **后续扩展**：阶段 3a 先拆开 writeback 与 commit，再接 RS；2d 未过不能直接跳到乱序发射。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：2c 绿。  
**目标**：源操作数与写侧统一为 **物理寄存器模型**；这是顺序后端最后一档，之后才允许谈 RS。

---

## 1. 原理：前递键必须与存储键一致

2c 状态：

```text
PRF 写入键 = pdest（物理）
前递比较键 = arch rd
源读取     = arch_rf
```

在「有旁路且程序序」时可能运气好，但：

- 同一 arch 的新旧 phys 并存时，旁路比 arch 会 **接到错误生产者**  
- 进 RS 后没有 arch 前递网，必须 `psrc` + busy/CDB  

所以 2d 一次改齐：

```text
psrc = RAT[rs]
读 PRF[psrc]
旁路：psrc === stage.pdest
stall：busy(psrc) 且不可旁路；或 load 未返回
```

---

## 2. 设计

### 2.1 源

```scala
id_psrc1 = rename.io.rs1_phys
id_psrc2 = rename.io.rs2_phys
prf.raddr1/2 := id_psrc1/2
// 初始 fallback
rdata = prf.rdata
// 进入 EX 的操作数
rd1 := RegEnable(PhysForward(psrc1, ren1, prf.rdata1), en_id_ex)
```

### 2.2 前递 / load-use

```scala
PhysForward(psrc, ren, fallback):
  hit EX  if exu 写使能 && psrc===exu.pdest && !load
  hit LSU if ...
  hit WBU if ...

PhysLoadStall:
  EX 是 load 且 psrc 命中 → stall
  LSU 是 load 未 rvalid 且命中 → stall

PhysBypassable:
  能旁路到值（含 LSU load 已返回）→ 不算 busy 堵
```

### 2.3 Busy

```scala
set: en_id_ex && do_rename → set(pdest)
clr: wb 写 PRF 时 clr(pdest)
查询: raddr = psrc
stall: ren && !ready && !Bypassable
```

### 2.4 flush 清 busy

对 ROB 上 `after flush_idx` 且 valid 的写项：

```scala
killBusy |= (1 << e.new_phys)
busy.clr_mask := killBusy
```

与 rename rebuild 同拍。

### 2.5 arch_rf

仅 **提交**（2d 可仍跟 WBU）更新，专供 difftest；**执行不再读 arch_rf**。

### 2.6 ROB

建议 `enq.src1_phys/src2_phys := psrc`，供调试与 3b。

---

## 3. 与 2c 的差异清单

| 项 | 2c | 2d |
|----|----|----|
| 源数据 | arch_rf | PRF[psrc] |
| 前递键 | arch waddr | pdest |
| busy stall | 可选 | 需要 |
| clr_mask | 可选 | 需要 |

---

## 4. 踩坑

| 现象 | 原因 | 处理 |
|------|------|------|
| 2c 绿 2d 炸 | 前递仍比 arch | 全改 PhysForward |
| flush 后卡死 | 被杀 pdest 仍 busy | clr_mask |
| load-use 数据错 | 只靠 busy 不认 load 延迟 | PhysLoadStall |
| 同拍 set 新 pdest 与旁路 | 注意 bypass 不看「自己刚 set」 | 源是旧指令 psrc |

---

## 5. 验收（自勾）

- [ ] 能说明「写 phys + 源 arch」为何本质错误  
- [ ] dummy/add 及若干 cpu-tests 自测绿  
- [ ] 波形确认 stall/bypass 与 pdest 对齐  
- [ ] **不把参考实现通过当成自己清单已勾**  

---

## 6. 下一阶段入口

2d 完成后，顺序后端数据面齐备。  
下一步 **3a**：把 `wb_fire⇒commit` 拆开，BPU 更新搬到 commit，为乱序提交序打地基。  
见 [04a_阶段3a_提交语义.md](04a_阶段3a_提交语义.md)。


---

## 附录 A：PhysForward 完整伪代码

```scala
def PhysForward(psrc: UInt, ren: Bool, fallback: UInt): UInt = {
  val exu_hit = exu_wen && ren && psrc=/=0 && psrc===exu.pdest && !exu_is_load
  val lsu_hit = lsu_wen && ren && psrc=/=0 && psrc===lsu.pdest && !exu_hit
  val wbu_hit = wbu_wen && ren && psrc=/=0 && psrc===wbu.pdest && !exu_hit && !lsu_hit
  MuxCase(fallback, Seq(
    exu_hit -> exu_wdata,
    lsu_hit -> lsu_wdata,
    wbu_hit -> wbu_wdata
  ))
}
```

`exu_wen` 定义必须含 `pdest=/=0` 与 `valid`。

---

## 附录 B：stall 汇总

```scala
idu.is_stall :=
  PhysLoadStall(psrc1, ren1) ||
  PhysLoadStall(psrc2, ren2) ||
  (ren1 && !busy.ready1 && !Bypassable(psrc1,ren1)) ||
  (ren2 && !busy.ready2 && !Bypassable(psrc2,ren2)) ||
  rob.full || fl_stall
```

---

## 附录 C：从 2c 迁到 2d 的检查表

- [ ] 删除 arch_rf 作为执行源  
- [ ] 所有 RsForward 改为 PhysForward  
- [ ] busy set/clr/mask 接好  
- [ ] ROB 记录 src_phys  
- [ ] 回归 dummy/add/load-store  
