# 阶段 15a：MMU 与虚拟内存（SV32，后置可选大项）

## 学习导航
- **理论目标**：本章先理解：给核加 **RISC-V SV32 分页**（页表遍历 + TLB + 权限检查），支持用户态/内核态切换。
- **最小实现**：先实现 satp/SV32 译码和阻塞式 PTW，再分别加入小型 iTLB/dTLB、权限检查与 page-fault 精确提交；正确前不要并行化 PTW。
- **当前参考核**：**未做虚拟内存**。阶段 13 先收紧双发 IPC，阶段 14 再完成真四发；当前仍使用直接物理地址，无 satp/PTW/TLB，无 U/S 模式。
- **后续扩展**：按本章顺序逐步加入 iTLB/dTLB、PTW、权限检查与异常提交。
- **验收方式**：定向覆盖页表两级遍历、TLB hit/miss、跨页取指/访存、权限错误、sfence.vma 和 flush；再跑能使用虚拟地址的 OS/用户态 workload 与参考模型。

---
**前置**：阶段 14 的真四发主线完成验收后；当前仍是直接物理地址裸机核。确实有跑 OS/用户程序需求时再动。
**目标**：给核加 **RISC-V SV32 分页**（页表遍历 + TLB + 权限检查），支持用户态/内核态切换。  
**本章阶段快照**：**未做**（现状：直接物理地址访存，无 satp/PTW/TLB，无 U/S 模式）。

> 这是「入门级超标量」之外最大的系统能力。SV32 是运行带分页 OS 的必要基础之一，但远不充分：还需要对应特权级/CSR、异常中断与定时器、A 扩展或软件替代、设备模型和启动链。教学核可选，但不属于阶段 11 的 IPC 主线。

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
| 权限 | 检查 PTE 的 V/R/W/X/U/A/D，并结合当前特权级与 `mstatus.SUM/MXR/MPRV`；PTE 本身没有 S 位 |
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
load/store 地址生成 → TLB 翻译与权限/页错误检查 → 物理地址进入 SQ/LQ/SB/DCache
可用 VA 页内 offset 做早期保守过滤，但最终同址/别名判断必须用 PA
```

仅按虚拟地址做最终 store-load 消歧会漏掉不同 VA 映射到同一 PA 的 alias。最小可靠集成应在 store 标记 `addr_ready`、允许提交或进入 StoreBuffer 前完成翻译和 fault 记录；独立 SQ、LQ、StoreBuffer 与 DCache 保存物理地址。若为了并行先做 VA 检查，只能作为不会漏冲突的早期筛选，之后仍要用 PA 复核。

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
| 把 PTE 当成有 S 位 | 权限判断与规范不符 | PTE 只有 U 位；S-mode 访问还要结合 SUM/MXR，MPRV 会改变数据访问有效特权级 |
| SQ/SB 保存 VA 做最终比较 | synonym alias 漏依赖 | 翻译完成后用 PA 消歧和访存 |

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

→ [16b_阶段15b_多核与一致性.md](16b_阶段15b_多核与一致性.md)：继续系统扩展科普；或回到 [README](README.md)。
