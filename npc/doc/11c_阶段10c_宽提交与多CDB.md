# 阶段 10c：宽提交与多 CDB

## 学习导航

- **理论目标**：理解提交带宽和写回带宽为什么会限制 IPC；知道 2-wide commit、双 CDB、双唤醒、双 PRF 写口之间的关系。
- **最小实现**：在不破坏现有 difftest 单提交接口的前提下，先把结果总线从单 CDB 扩成 2 CDB，让一拍最多两个 FU 结果进入 PRF/ROB/RS。
- **当前参考核**：`CDB_NUM=2` 和 `COMMIT_WIDTH=2` 均已保留；10k 已放宽 lane1 类型，10m 又加入第二整数 ALU，四类结果 `ALU0/LSU/ALU1/DIV` 由独立的 4→2 `WritebackArbiter` 按 ROB age 选择最老两项。本章仍按 10c 历史顺序解释为什么先扩写回再扩退休。
- **后续扩展**：10f/10g/10m 已完成双退休、ROB/PRF 扩容、持续双取指与双整数 ALU；当前可继续研究更多 CDB、分类型 issue queue 和 banked PRF。
- **验收方式**：`mychisel.compile`、`OoOUnitTest`、cpu-tests + difftest、`microbench(test)` 全部通过；记录 before/after IPC 和 CDB 计数器。

---

## 1. 为什么先做多 CDB

8a/8b 之后，后端已经有多 FU 和 2-wide dispatch。如果 ALU、LSU、DIV 同拍完成，而写回端只有一个 CDB，就会出现：

```text
多 FU 完成 -> 仲裁只放行 1 条 -> 其他结果等下一拍
```

这种瓶颈和“发射是否多宽”无关。只要结果不能及时写 PRF、清 Busy、唤醒 RS，后续指令就会继续等。

10c 入口基线还有一个现实约束：C++ difftest 只看单个提交事件：

```text
io_commit_valid
io_commit_pc
io_commit_mem_addr
io_commit_is_load
io_arch_rdata[0..31]
```

所以如果 RTL 内部直接一拍提交两条，而 C++ 仍只执行一次 NEMU step，对拍会错位。阶段 10c 的参考实现先落地 **2 CDB 写回**，保持 **1-wide commit**，这样能继续稳定跑 difftest。

---

## 2. 10c 本章阶段快照改了什么

### 2.1 参数

`common/ooo_params.scala`：

```scala
// 10c 当章保留点
val COMMIT_WIDTH = 1
val CDB_NUM = 2

// 10f 阶段快照
val COMMIT_WIDTH = 2
```

这里 `COMMIT_WIDTH=1` 是 10c 的阶段性保留点，不是否定 2-wide commit 的目标；它表示当时先让双 CDB 写回在单提交 difftest 下稳定落地。

### 2.2 PRF

PRF 原本已有双写口：

```text
wen1/waddr1/wdata1
wen2/waddr2/wdata2
```

10c 把第二个写口真正接到第二路 WBU：

```scala
prf.io.wen1   := wb_wen
prf.io.waddr1 := wb_pdest
prf.io.wdata1 := wb_val

prf.io.wen2   := wb1_wen
prf.io.waddr2 := wb1_pdest
prf.io.wdata2 := wb1_val
```

### 2.3 BusyTable

BusyTable 从单清除口扩成双清除口：

```scala
clr_en/clr_addr
clr_en2/clr_addr2
```

两路 CDB 同拍写回时，两个 `pdest` 都必须清 busy。否则第二路虽然写进 PRF，rename/dispatch 仍会认为它未 ready。

### 2.4 ROB

10c 阶段 ROB 保持单提交，但写回端扩成两路：

```scala
wb_fire/wb_idx/wb_val/...
wb1_fire/wb1_idx/wb1_val/...
```

`commit_valid` 也允许 head 在第二路 CDB 同拍完成：

```scala
commit_valid := head.valid && (head.done || wb_is_head || wb1_is_head)
```

注意：这不是 2-wide commit，只是“head 如果从任一路 CDB 回来，可以同拍提交”。

### 2.5 RS

RS 增加第二个唤醒口：

```scala
cdb_valid/cdb_pdest/cdb_val
cdb1_valid/cdb1_pdest/cdb1_val
```

每个 entry 的两个源操作数都要同时比较两个 CDB。新入队 entry 也要做同拍 CDB capture，否则会错过“刚写回、刚入队”的值。

RS 还增加第二个完成回收口：

```scala
free_rob_fire/free_rob_idx
free_rob1_fire/free_rob1_idx
```

这个点很关键。10c 调试时遇到过一个真实 bug：旧 LSU 结果因为 ROB idx 复用被身份保护挡住，但旧的 `free_rob` 仍按 rob_idx 删除了新 RS entry，导致 head 永远等不到完成。修法是：RS 回收必须跟 `can_wb/can_wb1` 走，也就是只回收“ROB 身份校验通过、确实接受写回”的结果。

### 2.6 Core 仲裁

原来是三选一：

```text
ALU / LSU / DIV -> PriorityEncoder -> WBU0
```

现在是三选二：

```text
winner0 = PriorityEncoder(req)
winner1 = PriorityEncoder(req & ~UIntToOH(winner0))
```

两路 winner 分别进入 `wbu` 和 `wbu1`。每一路都做同样的身份保护：

```text
ROB entry valid
pc / arch_rd / new_phys 匹配
entry 还没 done
不在 flush / irq / fencei / mret kill 范围内
```

写回被接受后才会：

```text
写 PRF
清 Busy
标 ROB done
广播 RS CDB
回收 RS entry
更新 SQ store 地址/数据
```

这比“FU ready 拉高两个”严格得多。

---

## 3. 宽提交目标设计

真正 2-wide commit 的目标是：

```text
从 ROB head 开始，连续 done 的最多两条按程序序退休
```

普通指令可以占两个槽；特殊指令要保守：

| 指令 | 宽提交策略 |
|---|---|
| 普通 ALU / load | 可 2-wide |
| store | 建议独占或只允许 slot0，避免 StoreBuffer/MMIO 同拍语义复杂 |
| fence.i / mret / exception / ebreak | 独占提交拍 |
| branch/jump | 可提交，但 BPU update 也要能接收多条或限制在 slot0 |

要真正落地，还需要同步拓宽：

```text
ROB commit port x2
Rename arch_rat commit update x2
FreeList old_phys return x2
arch_rf update x2
BPU commit update x2 或 slot0-only
C++ difftest commit event x2
```

10c 阶段没有做这一步，是为了保持该阶段 difftest 自测绿；10f/10k 后已完成。

---

## 4. 验收结果

最终回归：

```bash
cd /home/qiu/ysyx-workbench/npc
scripts/stage9_regress.sh --mode full --tag stage10c_cdb2_store2_final \
  --cpu-timeout 240 --micro-timeout 900
```

结果：

| 项目 | 结果 |
|---|---|
| `mychisel.compile` | PASS |
| `OoOUnitTest` | PASS |
| cpu-tests smoke | dummy/add/add-longlong/bit/load-store/shift/string 全部 GOOD TRAP |
| microbench(test) | PASS |

关键数据：

| 指标 | 10b blocking DCache | 10c 2-CDB |
|---|---:|---:|
| IPC | `0.4171` | `0.4186` |
| Total cycles | `1252408` | `1248223` |
| Commit inst | `522417` | `522534` |
| CDB Conflicts | 未完全消除 | `0` |
| CDB Blocked Results | 未完全消除 | `0` |
| Head Not Ready | 未记录同口径 | `580912` |
| DCache hit rate | `72.06%` | `71.72%` |

10c 阶段结论：双 CDB 已经把写回冲突清掉，但 IPC 只小幅提升。更大的墙在前端和提交/访存等待：`FQ Full=269899`、`FQ Empty=75113`、`Head Not Ready=580912`。后续 10d/10e 验证了当时 1-wide commit/ROB16 下直接开启 slot1 会退化；10f-10m 已补双退休、扩窗口、LQ/FTQ 与第二 ALU，不能再把这里的旧瓶颈描述成当前状态。

---

## 5. 新增单测

`scala_test/unit/OoOUnitTest.scala` 增加了这些覆盖点：

- BusyTable：一拍清两个 writeback destination。
- ROB：一拍接受两个 writeback，两个 entry 都置 done。
- RS：两路 CDB 同拍唤醒两个源操作数。
- RS：一拍回收两个完成的 ROB entry，避免多 CDB 后 RS 假满或误删。

这些单测的意义是把 10c 的接口契约锁住，而不是只靠 microbench 间接碰到。

---

## 6. 踩坑记录

### 6.1 RS 回收不能只看 rob_idx

ROB idx 会复用。旧结果如果在 flush/reuse 后晚到，可能和新 entry 撞同一个 rob_idx。ROB 侧已经用 `pc/arch_rd/new_phys` 做身份保护，RS 侧也必须只在 `can_wb` 成立时回收。

错误做法：

```scala
rs.free_rob_fire := alu_leave || div_leave || lsu_leave
```

当前做法：

```scala
rs.free_rob_fire  := can_wb
rs.free_rob_idx   := wb_idx
rs.free_rob1_fire := can_wb1
rs.free_rob1_idx  := wb1_idx
```

### 6.2 不要让 StoreBuffer enqueue 组合旁路 load

调试时尝试过让 StoreBuffer load query 直接看本拍 `enq.fire`，但会形成组合环：

```text
load forward -> LSU out valid -> CDB winner -> head store addr
-> StoreBuffer enq.valid -> StoreBuffer load forward
```

正确方向是保持 StoreQueue/StoreBuffer 的时序边界清晰，并用 accepted writeback 语义保证 RS 不误删。

### 6.3 Verilator 内部信号名不能当稳定接口

C++ hang 打印里有一个内部信号：

```cpp
ysyx_25020039__DOT___core_io_dmem_rready
```

生成后被优化掉，导致 C++ 编译失败。这个只用于调试打印，不应作为功能接口依赖；当前已改成常量占位。

---

## 7. 后续任务

- 10f 已完成：拓宽 C++ difftest commit 协议，并接入真正 `COMMIT_WIDTH=2` 的最小 2-wide retire。
- 10f 已完成：为 commit 增加 slot0/slot1/commit2/slot1 block 计数器。
- 10f 已完成：宽提交时处理 `arch_rf` / `arch_rat` 双更新，以及同一架构寄存器两次提交的优先级。
- 10g/10h 已完成：扩 ROB/PRF 后重新评估 wide fetch；10m 再以 FetchBuffer/FTQ、FU refill 和双整数 ALU形成最终保留点。
- 当前 10m：`COMMIT_WIDTH=2`、`CDB_NUM=2`，`ALU0/LSU/ALU1/DIV` 经独立 4→2 oldest-result arbiter 写回；最终 IPC `0.6381`。

下一章：[11d_阶段10d_宽取指与前端带宽.md](11d_阶段10d_宽取指与前端带宽.md)。
