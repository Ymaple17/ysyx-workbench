# 阶段 11a：MMU 与虚拟内存（SV32，后置可选大项）

## 学习导航
- **理论目标**：本章先理解：给核加 **RISC-V SV32 分页**（页表遍历 + TLB + 权限检查），支持用户态/内核态切换。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：**未做**（现状：直接物理地址访存，无 satp/PTW/TLB，无 U/S 模式）。本章已经从阶段 10 后移，当前参考核先不实现虚拟内存。
- **后续扩展**：等阶段 10 的 StoreBuffer、访存层次、宽提交/多 CDB、宽取指和预测器路线稳定后，再把本章作为系统扩展进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 10 的 IPC 上限扩展已经收口；确实有跑 OS/用户程序需求时再动。  
**目标**：给核加 **RISC-V SV32 分页**（页表遍历 + TLB + 权限检查），支持用户态/内核态切换。  
**仓库状态**：**未做**（现状：直接物理地址访存，无 satp/PTW/TLB，无 U/S 模式）。

> 这是「入门级超标量」之外最大的系统能力。没有它，核只能跑裸机（AM/cpu-tests）；有了它才能跑 Linux 类系统。教学核可选，但不属于当前 Stage10 的 IPC 主线。

---

## 1. 为什么难

虚拟内存把「一次访存」变成「可能多次访存」：

```text
虚拟地址 → satp(页表根) → 查 PTE（一级）→ 查 PTE（二级）→ 物理地址 → 真访存
                            ↑ 页表也在内存里，可能要读 2 次内存
```

- **PTW（Page Table Walker）**：硬件遍历页表，产生额外内存访问  
- **TLB**：缓存 VA→PA 映射，命中免遍历  
- **权限**：U/S 模式、PTE 的 V/R/W/X/U/G/A/D 位检查  
- **与乱序/投机交互**：错误路径的访存不能提交页错误；TLB 缺失处理不能死锁

---

## 2. 结构概览

```text
LSU/IFU → 虚拟地址
    ↓
TLB（快表）：
  命中 → PA 直接走 DCache/ICache
  未命中 → PTW 遍历（读内存 1–2 次）→ 填 TLB → 重试
权限错误/PTE 非法 → 页错误异常（走 4c 异常路径）
```

| 部件 | 建议 |
|------|------|
| satp 寄存器 | CSR 里加（M 模式可写，S 模式生效） |
| TLB | 分离 I-TLB / D-TLB，各 16–32 项，全相联或组相联 |
| PTW | 小状态机：读第 1 级 → 读第 2 级 → 填 TLB |
| 权限 | 读 PTE.V/R/W/X；U 位（用户页）；S 位（仅 S/M） |
| 异常 | 页错误 → 4c 异常路径（mcause=12/13/15） |

---

## 3. 与乱序核的集成难点

### 3.1 投机页错误不能提交

```text
错误路径 load 触发页错误 → 不能直接进异常（4c 说未到 head 不改 CSR）
做法：PTW 的错误只「记录在 ROB 项/请求」，到 head commit 才走 4c
（与现有 state 字段机制一致：LAF/SAF 已经这么干）
```

### 3.2 TLB 缺失不能停死流水

```text
D-TLB miss → PTW 走内存（读总线）
此时 LSU 不能占死总线 → PTW 请求与正常 load 仲裁（类似 8d store buffer 优先级）
```

### 3.3 与 MLP / SQ 交互

```text
load 先查 SQ（虚拟地址消歧）→ 再 TLB → DCache（物理地址）
store buffer 里存的是虚拟地址还是物理？→ 简单做：提交写时再查 TLB/PTW
```

### 3.4 fence 语义

```text
sfence.vma：清 TLB（新指令类型，需进译码表）
9a 的 TAGE 更新、4f 的 fence.i 都是 commit 化——sfence.vma 同理
```

---

## 4. 最小落地切片

| 步 | 内容 | 验收 |
|----|------|------|
| 1 | satp 寄存器 + S 模式基础（mstatus.MPP 等） | csr 测例 |
| 2 | 固定映射 TLB（1:1 VA=PA 也可先跑通通路） | cpu-tests 不回归 |
| 3 | 真分页：PTW + 两级表遍历 | 自写页表小测例 |
| 4 | 权限/页错误 → 4c 异常路径 | 错误页测例 |
| 5 | sfence.vma | 页表替换测例 |
| 6 | 与 TLB 缺失的流水交互、MLP 共存 | 全回归 |

---

## 5. 踩坑

| 坑 | 现象 | 处理 |
|----|------|------|
| 投机页错误提前提交 | mcause 错 | 记 ROB 项，head 才处理 |
| PTW 占死总线 | 正常 load 饿死 | 独立请求 + 仲裁 |
| TLB 与 cache 地址空间混 | 物理地址发给了虚地址缓存 | 明确 TLB 后接 DCache |
| sfence.vma 不提交化 | 旧映射残留 | 与 fence.i 同套路 |
| U/S 位判断错 | 用户程序访问内核页 | 严格按 PTE.U 与当前模式 |

---

## 6. 验收（自勾）

- [ ] 能画 VA→PTW→TLB→PA 数据流  
- [ ] 能说页错误为何要 head 提交  
- [ ] 自写页表 + 权限 + sfence 测例过  
- [ ] 全 cpu-tests 不回归

## 相关代码（改动点）

- `core/csr.scala` — satp  
- `unit/tlb.scala` / `unit/ptw.scala`（新增）  
- `core/lsu.scala` / `core/ifu.scala` — 地址翻译接入  
- `unit/control.scala` — sfence.vma  
- `core/core.scala` — 页错误异常路径

## 下一步

→ [12b_阶段11b_多核与一致性.md](12b_阶段11b_多核与一致性.md)：继续系统扩展科普；或回到 [README](README.md)。
