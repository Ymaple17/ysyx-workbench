# 阶段 15：Trace/Stream、写回式 L1D 与六宽分布式路线

## 学习导航
- **理论目标**：理解 IPC 从 `2.2` 往 `3.0` 走时，关键不再是增加孤立端口，而是同时提高有效指令供给、脏数据服务率和后端可持续宽度。
- **最小实现**：先加入 commit-filled Trace/Stream 前端并保留原 IFU fallback，再把 L1D 改成 banked write-back，最后只在计数证明四宽后端已饱和时扩成六宽分布式后端。
- **当前参考核**：入口基线是 Stage14 源码可复现冻结核：`167053` cycles、`368817` commits、IPC `2.2078`。Stage15 结构尚未实现，不能把本章目标写成当前能力。
- **后续扩展**：长期预算 IPC `3.0`；本轮达到固定二进制严格 IPC `>=2.5` 后允许先冻结，其余六宽、多 control/memory 服务或更深 trace 能力留待继续。MMU/多核不混入本阶段，顺延到 Stage16。
- **验收方式**：每个结构门均做同二进制 A/B、完整单测与 difftest；候选必须降低 cycles 且能由 counters 解释。最终冻结要求 MicroBench PASS、双端 GOOD TRAP、无 mismatch/HANG/ABORT、两次结果可重复且 IPC `>=2.5`。

---

## 1. 周期预算

以 Stage14 的 `368817` commits 为口径：

```text
IPC 2.2078 -> 167053 cycles   Stage15 入口
IPC 2.5000 -> 147527 cycles   本轮冻结门
IPC 3.0000 -> 122939 cycles   长期结构预算
```

到 `2.5` 需要再减少约 `19353` cycles，约为 Stage14 周期的 `11.6%`。单项小参数通常不够，因此本阶段按服务结构切分，而不是继续扫 predictor/cache/queue 容量。

## 2. 三个结构门

| 子阶段 | 结构变化 | 主要目标计数 | 失败时怎么办 |
|--------|----------|--------------|--------------|
| [15a](16a_阶段15a_TraceStream前端.md) | commit-filled Trace/Stream + 原 IFU fallback | `FQ Empty`、redirect refill、有效 fetch width | 保留原 IFU，回退 trace 候选，不污染后端 |
| [15b](16b_阶段15b_写回式双端口L1D.md) | dirty line、writeback queue、banked dual access | `Wait Store Commit`、`StoreBuffer Full`、head load wait | 保留 MMIO bypass 和阻塞式兼容模式 |
| [15c](16c_阶段15c_六宽分布式后端.md) | 2x3 cluster、六 rename/ROB/commit、banked PRF/completion | dispatch/commit width、CDB conflict、ROB head wait | 若四宽未饱和，不打开六宽参数 |
| [15d](16d_阶段15d_IPC2.5冻结与IPC3路线.md) | 严格回归与冻结 | cycles/commits/IPC/repeat | 未到 `2.5` 不虚报完成，记录剩余结构 |

## 3. 为什么是这个顺序

```text
前端空洞 -> 后端无事可做
store/load 服务慢 -> ROB head 堵住，增加 issue width 无效
前端和 L1D 都能持续供给 -> 扩后端宽度才可能转化为 commit
```

Stage14 已有平均 fetch width `3.1685`，但 `FQ Empty=12275`；有四 MSHR 和双 load 服务，但 `Wait Store Commit=7472`、`Head Wait Load=7705`；四提交已使用，但 slot3 只有 `54294` 次。先改供给与服务率，比直接把 `CORE_WIDTH=6` 更可能产生可解释收益。

## 4. 分布式控制边界

可以分布：

- FU-local issue queue、ready select 和 credit。
- PRF bank read/write 仲裁。
- completion bank 与局部 bypass。
- Trace/IFU 两条供给路径的本地 valid/ready。
- L1D bank、MSHR 和 writeback queue owner。

必须保持全局或有唯一排序点：

- ROB 程序序与 oldest exception/redirect。
- architectural rename map 的提交顺序。
- store 对外可见顺序、MMIO 与 fence 边界。
- 多个 branch/memory violation 同拍时的最老恢复选择。

“分布式”不是删除所有仲裁器，而是让资源所有权靠近资源，只把架构顺序保留为全局合同。

## 5. 不属于 Stage15 的内容

- SV32、TLB、PTW、U/S 模式：见 [17a_阶段16a_MMU与虚拟内存.md](17a_阶段16a_MMU与虚拟内存.md)。
- 多核、MESI、原子一致性：见 [17b_阶段16b_多核与一致性.md](17b_阶段16b_多核与一致性.md)。
- SMT2：可提高多线程总吞吐，但当前 MicroBench 是单线程，不是本阶段 IPC 解法。
- benchmark-PC 特化、关闭 difftest、改变计数起止：禁止。

## 6. 验收清单（学习者自勾）

- [ ] 能从周期预算推导 `2.5/3.0` 的 cycles 门
- [ ] 能解释为什么前端/L1D 在六宽之前
- [ ] 能画出分布式资源与全局排序点
- [ ] 能为每个结构门设计保留与回退路径
- [ ] 能复现 Stage15 固定二进制 A/B 账本

下一章：[16a_阶段15a_TraceStream前端.md](16a_阶段15a_TraceStream前端.md)。
