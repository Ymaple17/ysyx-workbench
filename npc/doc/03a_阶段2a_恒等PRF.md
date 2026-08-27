# 阶段 2a：恒等 pdest + PRF

## 学习导航
- **理论目标**：本章先理解：把架构寄存器堆换成 **PRF**，但映射仍是 **恒等** `pdest = arch_rd`（`rename.fire=0`）。
- **最小实现**：用 PRF 替换架构寄存器堆的数据通路，但保持 `psrc=arch`、`pdest=arch_rd`，确认仅换存储位置不会改变提交轨迹。
- **当前参考核**：阶段 11d 已是真重命名和 64 项 PRF；本章恒等映射是可回归的接线台阶，不是当前最终策略。
- **后续扩展**：2b 只增加 ROB 元数据与生命周期，仍不打开 freelist 真分配。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 1 四件套单测通过。  
**目标**：把架构寄存器堆换成 **PRF**，但映射仍是 **恒等** `pdest = arch_rd`（`rename.fire=0`）。  
**验收焦点**：dummy/add + difftest 绿；C++ 走 `commit_*` / `arch_rdata`。

---

## 1. 为什么先做「假重命名」

若一步打开 freelist：

- PRF 读写下标错  
- 提交导出错  
- freelist 回收错  

会叠在一起。恒等映射把问题收成：

```text
「只要把值写进 PRF[arch]，读出 PRF[arch]，提交快照对，就和 Refile 同构」
```

通过后再改 `pdest` 的来源（2c），差分范围清晰。

---

## 2. 目标结构

```text
IFU → IDU → EXU → LSU → WBU
         │              │
      PRF 读(raddr=rs)  PRF 写(waddr=rd 或 pdest=rd)
         │              │
      前递仍可比 arch    arch_rf 快照 @ WBU（给 difftest）
```

- `rename` 可例化但 **fire=false**，dest 不用  
- `pdest` 建议 **已经在 Bundle 里传递**，取值暂时等于 `waddr`  

---

## 3. Bundle 扩展（为后续阶段）

在 `IDU_EXU_IO` / `EXU_LSU_IO` / `LSU_WBU_IO` 增加并 **逐级透传**：

```scala
rob_idx   // 2b 才用，可先绑 0
pdest     // 2a: = waddr（rd）
old_phys  // 2a: = waddr
do_rename // 2a: false
```

**为什么 2a 就要透传？**  
若 2c 才加字段，要改 exu/lsu 一长串赋值；2a 先打通通路，2c 只改 core 里赋值源。

---

## 4. core 接线要点

### 4.1 PRF 读

```scala
val id_rs1 = idu.io.in.bits.inst(19,15)
val id_rs2 = idu.io.in.bits.inst(24,20)
prf.io.raddr1 := id_rs1
prf.io.raddr2 := id_rs2
idu.io.refile.rdata1 := prf.io.rdata1  // 复用 IDU 原 refile 读口接线
idu.io.refile.rdata2 := prf.io.rdata2
```

IDU 内部仍用 `refile.raddr` 从指令抽 rs——你只需在 core 把「读数据」换成 PRF 输出。

### 4.2 前递（仍 arch）

与 ioid 相同：`RsForward(rs, ren, prf.rdata)`，命中 EX/LSU/WBU 的 **waddr**。  
load-use：`RsStall`。

### 4.3 写 PRF + arch_rf

```scala
val wb_fire = wbu.io.in.valid && !wbu.io.is_flush
val pdest   = wbu.io.in.bits.pdest  // 2a 等于 rd
when(wb_fire && reg_write && pdest=/=0) {
  prf.io.wen1 := true
  prf.io.waddr1 := pdest
  prf.io.wdata1 := wbu.wdata
  arch_rf(rd) := wbu.wdata
}
```

### 4.4 恒等 pdest 赋值（ID→EX 包装）

```scala
id_pdest = Mux(reg_write && rd=/=0, rd, 0.U)
idu_ren.bits.pdest := id_pdest
idu_ren.bits.do_rename := false.B
```

### 4.5 导出

```scala
io.commit_valid := wb_fire
io.commit_pc    := wbu.pc
io.commit_mem_addr := wbu.alu_result
io.commit_is_load  := reg_write_sel === MEM_SEL
io.arch_rdata(i)   := Mux(i===0, 0.U, arch_rf(i))
```

---

## 5. C++ / difftest

`oood_chisel_csrc/cpu/cpu.cpp`：

- 提交条件：`top->rootp->io_commit_valid`  
- PC：`io_commit_pc`  
- 寄存器：`io_arch_rdata[i]`（经 `read_gpr_from_top`）  

**不要**再 `...refile__DOT__Memory[i]`。

改完 Chisel 后 `make all` 重生 Verilator 头文件。

---

## 6. 时序注意

- PRF 组合读 + 同拍 WBU 写：ID 读到的是 **旧值**，靠前递补 RAW——与同步 regfile 语义一致。  
- `arch_rf` 与 PRF 同拍更新，difftest 在 commit 边沿采样应一致。

---

## 7. 踩坑

| 现象 | 原因 | 处理 |
|------|------|------|
| 全 0 寄存器差分 | C++ 仍读 refile | 改 arch_rdata |
| 有时对有时错 | 前递漏 WBU 或 load-use | 对照 ioid 前递 |
| 综合优化掉 PRF | 未连接 | dontTouch(commit) 调试期可加 |

---

## 8. 验收（自勾）

- [ ] 画出 2a 数据通路（读/写/前递/导出）  
- [ ] dummy + difftest GOOD  
- [ ] add + difftest GOOD  
- [ ] 说明 pdest 为何先等于 arch_rd  


---

## 附录 A：与 ioid 对照改哪些文件

| 文件 | 动作 |
|------|------|
| `unit/prf.scala` | 阶段1已有，2a 接线 |
| `core/core.scala` | 例化 PRF；读口接 ID；写口接 WBU；导出 commit |
| `core/idu.scala` 等 | Bundle 加 pdest/rob_idx 透传（值在 core 包一层） |
| `oood_chisel_csrc/cpu/cpu.cpp` | commit 采样 |
| `oood_chisel_csrc/cpu/regs.cpp` | arch_rdata |

`refile`：可不删，但 `core` 不再例化它作为数据通路。

---

## 附录 B：最小 `core` 伪代码骨架

```scala
val prf = Module(new PRF(conf))
// rename/rob 可先例化但不 fire（为 2b 留位置）

// 读
prf.io.raddr1 := id_rs1
prf.io.raddr2 := id_rs2
idu.io.refile.rdata1 := prf.io.rdata1
idu.io.refile.rdata2 := prf.io.rdata2

// 前递（同 ioid，键是 arch）
exu.io.in.bits.rd1 := RegEnable(RsForward(...), en)
exu.io.in.bits.rd2 := RegEnable(RsForward(...), en)

// 写
val wb = wbu.io.in.valid && !wbu.io.is_flush
prf.io.wen1 := wb && wbu.wen && waddr=/=0
prf.io.waddr1 := waddr   // 恒等
prf.io.wdata1 := wbu.wdata
when(prf.io.wen1) { arch_rf(waddr) := wbu.wdata }

// 导出
io.commit_valid := wb
io.commit_pc := wbu.pc
// ...
```

---

## 附录 C：调试清单

1. 只跑 dummy，看 commit PC 序列是否 0x80000000, +4, …  
2. 对比 NEMU 与 arch_rdata 是否逐步一致  
3. 若全错，先确认 Verilator 顶层是否真是 oood 生成的 `io_commit_*`  
