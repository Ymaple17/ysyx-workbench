# 阶段 10a：独立 SQ 与 Store Buffer

## 学习导航
- **理论目标**：本章先理解：把 store/load 消歧从 ROB-backed 版本升级为真正独立 SQ，并用 store buffer 隐藏提交写延迟。
- **最小实现**：先做能通过 difftest 的最小闭环，不把后续扩展提前塞进本章。
- **当前参考核**：**未做**（当前参考核是 ROB-backed `StoreQueue`，store 仍按 ROB head 顺序提交写）。
- **后续扩展**：正文里的选做、进阶或阶段 10 内容只作为方向，等最小实现和回归稳定后再进入。
- **验收方式**：涉及 RTL 时至少跑 `./mill -i mychisel.compile`、相关单测和 cpu-tests；涉及性能时再跑 `microbench mainargs=test` 并记录 before/after。

---
**前置**：阶段 8d 的 ROB-backed StoreQueue 已跑通；阶段 9 已确认内存侧值得继续优化。  
**目标**：把 store/load 消歧从 ROB-backed 版本升级为真正独立 SQ，并用 store buffer 隐藏提交写延迟。  
**仓库状态**：**未做**（当前参考核是 ROB-backed `StoreQueue`，store 仍按 ROB head 顺序提交写）。

---

## 1. 理论目标

独立 SQ 负责记录在飞 store 的程序序、地址、数据、mask；load 查询 SQ 决定：

- 更老 store 地址未知：等待
- 地址已知且完全覆盖：转发
- 部分重叠：等待
- 无冲突：访问内存

Store Buffer 负责把已提交 store 排队写出，让 ROB head 不必一直等写总线完成。

---

## 2. 最小实现

第一步只做独立 SQ，不急着上合并 buffer：

```scala
class SQEntry extends Bundle {
  val valid = Bool()
  val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  val addr_ready = Bool()
  val addr = UInt(32.W)
  val wdata = UInt(32.W)
  val wmask = UInt(4.W)
}
```

先保证功能等价于 8d，再考虑提交端 store buffer。

---

## 3. 当前参考核

当前参考核的 `unit/sq.scala` 已经把消歧逻辑模块化，但输入仍是 `rob.io.entries`。这给独立 SQ 留好了接口方向：把 entries 来源从 ROB 换成 SQ 表即可。

---

## 4. 后续扩展

- Store Buffer 按序出队
- MMIO store 不进 buffer
- fence.i / mret / exception 前 drain
- 同地址 store 合并

---

## 5. 验收方式

- StoreQueue 单元测试覆盖 wait / forward / partial overlap
- `load-store`、`string`、microbench(test) 通过 difftest
- store buffer 版本额外跑 fence.i / MMIO / 自修改相关测例
