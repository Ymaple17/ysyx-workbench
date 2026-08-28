# 阶段 3b：RS 壳（仍最老发射）

## 学习导航
- **理论目标**：本章先理解：引入保留站 **结构** 与 enq/issue/flush 时序；发射策略暂时与顺序核相同——**只发程序序最老且源就绪** 的指令。
- **最小实现**：实现 RS enq/ready/issue/free/flush，但只允许程序序最老且源就绪项发射，先验证结构时序而不改变执行顺序。
- **当前参考核**：阶段 12f 的 RS 已支持多 FU、双普通 ALU、同拍 CDB wake/select 和局部 oldest-ready 选择，控制流另由 BRQ/BRU 调度；本章是同一 entry 生命周期的保守起点。
- **后续扩展**：3c 把 RS 输出真正接到派遣寄存器、EX 和 CDB，3d 再允许越过未就绪最老项。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：3a 自测通过（cpu-tests 全绿）。  
**目标**：引入保留站 **结构** 与 enq/issue/flush 时序；发射策略暂时与顺序核相同——**只发程序序最老且源就绪** 的指令。  
**故意不做什么**：越过最老未就绪去发后条（真乱序，见 04c §9）；用 RS **完全替换** StageConnect 驱动 EX 是 3c 交付（发射策略仍可最老）。

---

## 1. 原理：为什么先做「壳」

若直接乱序发射：

- RS 分配/释放 bug  
- 唤醒 bug  
- 提交/门控 bug  

会缠在一起。3b 只验证：

```text
REN ─enq→ ROB
    ─enq→ RS
    ─→（数据通路）EX …   // 见 §3 两种接法
flush 清 RS（与 ROB 对齐）
满则停 REN
```

且 issue 选择与 2d/3a「一次只进一条最老」同构，功能应接近 3a。

---

## 2. RS 项字段（仓库 `unit/rs.scala`）

```scala
class RSEntry extends Bundle {
  val valid, rob_idx
  val src1_ready, src2_ready, src1_phys, src2_phys, src1_val, src2_val
  val pdest, old_phys, do_rename
  val pc, inst, imm_ext, waddr, is_ebreak, csr_*, state
  val bp_valid, bp_taken, bp_target, bp_index
  val exu_alu_srcA/B, exu_alu_control, exu_jump
  val lsu_mem_*, wbu_reg_write, wbu_reg_write_sel, wbu_csr_*
}
```

---

## 3. 模块接口 `unit/rs.scala`

```scala
enq_fire, enq_bits → full, count
rob_head                         // age = rob_idx - head
issue_valid, issue_bits, issue_fire
cdb_valid, cdb_pdest, cdb_val    // 3b 可先不接真实 CDB
flush, flush_idx, flush_all      // irq 全清；mispred 清 age>flush_idx
```

### 3.1 发射规则（3b）

```text
在 valid 项中找 age 最小者（程序序最老）
仅当其 src1_ready && src2_ready 才 issue_valid
禁止：最老未 ready 时发更年轻项
```

实现：`age = rob_idx - rob_head`（截断到 ROB_PTR_W）；`isOldest(i) = valid(i) && ∀j. !(valid(j) && age(j)<age(i))`。

### 3.2 flush（关键，与 ROB 对齐）

| 原因 | 行为 |
|------|------|
| irq / `flush_all` | 全部 valid=0 |
| mispred | 清 `age(rob_idx) > age(flush_idx)` 的项（**保留分支及更老**） |

**禁止** mispred 时整表清空：否则 ROB 里仍保留未发射的正确路径项，RS 已空 → **永久无 issue → 死锁**。

### 3.3 full 与同拍 issue+enq

```text
full = 无空槽 && 本拍不 issue
同拍 issue 腾出的槽可 enq（freeMask | OH(issue_idx)）
```

---

## 4. 与 core 的两种接法

### 4.1 接法 A（3b 推荐先做）：StageConnect 数据通路 + RS 影子

```text
保留：StageConnect(idu_ren, exu.in)   // 与 3a 相同，功能已绿
同步：en_id_ex 时 ROB.enq + RS.enq
影子：rs.issue_fire := RegNext(en_id_ex && rs.issue_valid)
      // 延迟一拍弹出，避免 rs.full ↔ issue_fire 组合环
```

- ID stall 含 `rs.full`  
- 源仍在 ID 用 PhysForward + busy stall 保证入队时 ready  
- **验证**：RS 单测 + 全 cpu-tests；波形可见 RS 与 REN 同填同清  

### 4.2 接法 B（完整壳）：仅 RS 驱动 EX

```text
删除 StageConnect(idu_ren, EX)
REN → RS.enq；RS.issue → 派遣寄存器 → EX.in
flush 时：
  - 分支仍在 EX 时 **不能** 无条件 is_flush 掉 EX（否则 ROB head 永 done）
  - 只杀 youngerThan(flush_idx) 的派遣/EX 项
  - LSU/WBU 用 in.valid 判 young 易与 StageConnect 成 **组合环** → 顺序 3b 可仅 irq 冲 LSU/WBU
```

接法 B 曾踩坑见 §6；功能未稳前用接法 A 交 3b。

---

## 5. Busy / freelist

- 与 3a 相同：rename 时 set、wb 时 clr、flush 时 `clr_mask` 或 **rebuild**  
- flush 后建议按「保留且 !done 的 new_phys」重建 busy，防杀项 busy 残留 → ID 永久 stall  
- ROB 空且本拍不 rename 时可 sweep busy（勿在每拍 ROB 空时强制 rebuild 盖掉刚 set）

---

## 6. 踩坑（实现实录）

| 现象 | 原因 | 处理 |
|------|------|------|
| mispred 后整机无 commit，ROB head 不 done | `exu.is_flush` 杀掉仍在 EX 的**分支** | 分支 rob_idx==flush_idx 不得 flush EX；或先用接法 A |
| mispred 整表清 RS，ROB 仍有正确路径 | RS/ROB 不一致 | 选择性 flush（age > flush_idx） |
| `rs.full` 与 `issue_fire` 组合环 | issue_fire 用 en_id_ex，full 又挡 en_id_ex | `issue_fire` 用 `RegNext` 或拆数据通路 |
| LSU young flush 组合环 | `is_flush` 依赖 `in.valid`，StageConnect 又依赖 flush | 顺序核 LSU/WBU 仅 irq 冲 |
| hang 时 rob_count=0、PC 停旧址 | 派遣路径丢指令 / 冲刷策略 | 接法 A 先绿；再查 IFU redirect |
| freeBits 的 popcount 等于复位可分配容量、ROB 空 | 正常（32 个初始架构映射不在 freelist，其余 `N_PHYS-32` 个槽空闲） | 勿误判 freelist 耗尽 |
| BusyTable 单测 | 增加 rebuild 口后 idle 需 poke | 见 OoOUnitTest |

### 6.1 hang 诊断（仿真）

```text
打印：last_commit_pc, rob.count/head/tail, freeBits popcount, 各 rob[i].valid/done/pc
若 count=0 仍无取指前进 → 查 IFU/is_flush/correct_pc，不是 ROB 满
若 head.valid && !done → 查该指令是否被误 flush 或未进 WBU
```

---

## 7. 单测（`OoOUnitTest` RS）

1. enq 两条 ready → issue 最老  
2. 最老 not ready、次老 ready → **不 issue**；CDB 唤醒后发最老  
3. flush_all 清空  
4. selective flush 保留更老 rob_idx  

```bash
cd oood_chisel_vsrc && ./mill -i mychisel.test
```

---

## 8. 回归

```bash
make all
# 全量 cpu-tests（35）
for f in $AM_HOME/tests/cpu-tests/build/*-riscv32e-npc.bin; do
  obj_dir/Vysyx_25020039 "$f" -b --diff=$NEMU_HOME/build/riscv32-nemu-interpreter-so
done
```

参考实现（接法 A + RS 单测）曾 **35/35**；你的验收请自行勾选。

---

## 9. 验收（自勾）

- [ ] 能口述 RS 最老发射与「禁止越过」  
- [ ] 能说明 mispred 时 RS 与 ROB 必须对齐 flush  
- [ ] RS 单测通过  
- [ ] 全量 cpu-tests + difftest  
- [ ] 知道接法 A vs B 的差别与风险  

---

## 10. 下一步

[04c_阶段3c_乱序发射.md](04c_阶段3c_乱序发射.md)：接法 B（RS→EX）+ CDB 口；**发射仍最老 ready**。真乱序见 04c §9。
