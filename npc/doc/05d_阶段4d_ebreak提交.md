# 阶段 4d：ebreak @ commit

## 学习导航
- **理论目标**：本章先理解：仿真结束 / trap 只在 **ROB head 提交** ebreak 时触发；wrong-path ebreak 永不 `sim_exit`。
- **最小实现**：只在 ebreak 成为 ROB head 且真正提交时产生 `sim_exit`；被 flush 的 ebreak 不得结束仿真。
- **当前参考核**：该规则已被阶段 11d 继承；正文 35/35 是本章阶段快照，最终 cpu-tests 口径见 README。
- **后续扩展**：4e 用同样的“head 锁存、下一拍 redirect”模式实现 mret。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：4c 绿（异常/ecall 已认 ROB head commit）。  
**目标**：仿真结束 / trap 只在 **ROB head 提交** ebreak 时触发；wrong-path ebreak 永不 `sim_exit`。  
**本章阶段快照**：已落地（cpu-tests 靠 ebreak 收尾，35/35）。

**铁律**：ebreak 是架构可见的「程序结束/陷入」事件，必须挂在 `commit_fire` 上，不能挂在 ID 译码或 WBU valid。

---

## 1. 原理

### 1.1 精确异常视角

阶段 4 总览：E 之前已全部 commit；E 及之后像没执行过。  
ebreak 在本核主要当 **仿真 trap / 测例收尾**（对齐 NEMU `NEMUTRAP`），不写 mepc/mcause，但副作用仍只能在成为架构序下一条已提交指令时外泄：

| 路径 | 副作用 |
|------|--------|
| `useDPIC=true` | BlackBox `Ebreak` → DPI-C `sim_exit()` |
| `useDPIC=false` | `io.ebreak` 拉高，由 tb / SoC 收 |

```text
is_ebreak_fire = commit_fire && commit_bits.is_ebreak
```

### 1.2 为何不能在 ID / WBU

| 触发点 | 乱序后 |
|--------|--------|
| ID 译出即 exit | wrong-path ebreak 结束仿真；真实 head 未到 |
| WBU `valid` 即 exit | 写回 ≠ 提交；younger 可先 WBU |
| EX leave | 完成序 ≠ 架构序 |

```text
ROB:  [div(older)] [ebreak(younger, wrong-path)]
EX/WB: ebreak 先完成 → 旧路径 WBU 调 sim_exit → 「提前毕业」
随后 mispred 杀 ebreak → 本不该结束
```

cpu-tests 几乎都以 ebreak 收尾；误触发会出现假 PASS、difftest 未对齐就 exit、分支测例间歇挂。

### 1.3 与 commit_fire 同拍

ebreak 不像 ecall 要先写 CSR 再 flush，**提交拍即架构点**：

```text
T0：head=ebreak && done → commit_fire
     → ebreak_cm.is_ebreak → sim_exit / io.ebreak
```

禁止 `RegNext(cm_fire && is_ebreak)`：多一拍会让 tb 看到多提交或超时竞态。

---

## 2. 为何旧路径炸

| 项 | 旧 | **4d** |
|----|----|--------|
| DPIC `sim_exit` | WBU `valid && is_ebreak` | **`cm_fire && cm_bits.is_ebreak`** |
| 非 DPIC 导出 | ID / WBU | **core 顶层 `io.ebreak` @ commit** |
| WBU 内 `Ebreak` | 接真实信号 | **输入恒 `false.B`** |

顺序核「WBU≈commit」碰巧能绿；3a/3d 后 WBU 路径必然在 wrong-path 误杀仿真。

---

## 3. 时序 / 规格

### 3.1 标志位活到 commit

```text
IDU: is_ebreak := (inst === 0x00100073)
  → ROB/RS.enq_bits.is_ebreak
  → EXU/LSU 透传（写回不用）
  → cm_bits.is_ebreak
  → ebreak_cm.io.is_ebreak := cm_fire && cm_bits.is_ebreak
```

ebreak 仍须进 ROB 并标 done，否则到不了 head。本阶段 **不为 ebreak 单独 flush_all**（exit 后进程收尾）。

### 3.2 与 4c ecall

| | ecall（4c） | ebreak（4d） |
|--|-------------|--------------|
| 提交拍 | 写 mepc/mcause | `sim_exit` / `io.ebreak` |
| 下一拍 | `flush_all` + PC←mtvec | **无** |
| `commit_fire` | 是 | 是 |

### 3.3 wrong-path

```text
mispred 杀 younger ebreak → valid=0 → 无 commit_fire → is_ebreak 口保持 0
仅 head 上 commit_fire 时才允许仿真结束
```

---

## 4. 接口与源码要点

### 4.1 DPI

`sim/ebreak.scala` 内联 `Ebreak.v`：`is_ebreak` 为 1 的拍组合调用 `sim_exit()`。驱动必须干净——只能是 commit 脉冲。

### 4.2 WBU 恒 0

```scala
// core/wbu.scala — 4d：不再结束仿真
val ebreak = Module(new Ebreak)
ebreak.io.is_ebreak := false.B
if (!conf.useDPIC) io.ebreak.get := false.B
```

留模块避免接口半残；语义上 WBU 路径永久关闭。若只改 core 忘改 WBU，双实例可能仍有一个接到旧信号。

### 4.3 core：唯一触发点

```scala
val ebreak_cm = Module(new Ebreak)
ebreak_cm.io.is_ebreak := cm_fire && cm_bits.is_ebreak
if (!conf.useDPIC) {
  io.ebreak.get := cm_fire && cm_bits.is_ebreak
}
```

| 配置 | 行为 |
|------|------|
| DPIC | 黑盒 `sim_exit` |
| 非 DPIC | 同条件导出 `io.ebreak`（soc 再引出） |

IDU：`is_ebreak := (inst === "h00100073".U)`，与 NEMU 编码一致。

---

## 5. 接线步骤

1. 确认 ROB enq/`commit_bits` 有 `is_ebreak`。  
2. core 例化 `Ebreak`，驱动 `cm_fire && cm_bits.is_ebreak`。  
3. WBU 内输入改 `false.B`；非 DPIC 改由 core 导出。  
4. 删 IDU/`exit` 上残留的 `sim_exit`。  
5. 跑 cpu-tests；用分支测例确认不会提前结束。

---

## 6. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 仍在 WBU 触发 | 乱序后偶发早退 | WBU 恒 0；只留 commit 实例 |
| `RegNext` 延一拍 | tb 超时 / 多提交 | 与 `commit_fire` **同拍** |
| enq 未存标志 | 永不 exit，测例超时 | 查 ROB 字段 |
| 双实例都接真信号 | 两边都亮 | 仅 `ebreak_cm` 为真 |
| 把 ebreak 当 4c 异常 | 多写 mepc、多 flush | 本阶段 **只** exit |
| 非 DPIC 忘导出 | 仿真不收尾 | `io.ebreak := cm_fire && …` |

---

## 7. 验收（自勾）

- [ ] 能说明精确异常下为何 ID/WBU 触发非法  
- [ ] 能指代码：唯一驱动 `cm_fire && cm_bits.is_ebreak`  
- [ ] WBU 内 `Ebreak` 输入恒 0  
- [ ] 理解 DPIC / 非 DPIC 两条导出  
- [ ] 全 cpu-tests 绿  
- [ ] 能口述 wrong-path ebreak 被 flush 后不会 `sim_exit`

---

## 8. 下一步

→ [05e](05e_阶段4e_mret提交.md)：MRET 不走 EX mispred，改 commit→下一拍 `flush_all`。  
→ [05f](05f_阶段4f_fencei提交.md)：同一 T0/T1 提交化模板。

---

## 附录 A：旧 WBU vs 4d

| 旧 | 4d |
|----|-----|
| `WBU: Ebreak(is_ebreak & valid)` | `WBU: Ebreak(false)` |
| core 不碰 | `core: Ebreak(cm_fire && cm.is_ebreak)` |
| 非 DPIC ← wbu | 非 DPIC ← core 同上条件 |
| 结束 ≈ 写回 | 结束 = **提交** |

---

## 附录 B：cpu-tests「卡死 / 假 PASS」

```text
未驱动 commit ebreak → 永不 sim_exit → 超时
仍在 WBU 触发 → 未到 NEMU trap 边界就 exit → 随机 FAIL / 假 PASS
```

验收锚点：**结束边沿 = 架构提交边沿**。

```text
idu.is_ebreak → rob.enq → cm_bits.is_ebreak
  → ebreak_cm = cm_fire && cm_bits.is_ebreak → sim_exit / io.ebreak
```

写回路径透传可留作调试，**不得**再驱动 exit。
