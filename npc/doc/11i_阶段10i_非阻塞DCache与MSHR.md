# 阶段 10i：非阻塞 DCache 与 MSHR

## 学习导航
- **理论目标**：理解 blocking DCache 为什么会让 load miss 拖住后端，以及 MSHR 如何让多个未完成 cache miss 共存。
- **最小实现**：先做 1-entry MSHR，把 miss 请求从 LSU/DCache 主流水里解耦；再考虑多 MSHR、miss merge 和 hit-under-miss。
- **当前参考核**：未做。10b 当前是 64 set、8B line 的 blocking direct-mapped DCache。
- **后续扩展**：多 outstanding load、load replay、miss merge、store miss 合并、真正 AXI burst fill。
- **验收方式**：cpu-tests + microbench 全绿；重点比较 DCache miss 周期、Head Not Ready、LSU bus wait、IPC。

---

## 1. 为什么放在 10h 后

当提交宽度和前端供给都改善后，访存 miss 会重新变成硬瓶颈。blocking DCache 的问题是：

```text
miss 期间 cache 忙
后续无关 load 不能继续
ROB head 如果等 load，commit 停住
```

非阻塞 DCache 的目标是把 miss 记录到 MSHR，让 cache 能继续服务部分 hit 或其他可并行请求。

---

## 2. 最小切片

1. 1-entry MSHR：记录 miss addr、rob_idx、mask、返回目标。
2. miss 发出后，DCache 主状态机可以接收可命中的访问。
3. refill 回来后写 cache line，并把结果送回对应 load。
4. stale/flush identity 继续沿用 10b/10c 的 ROB 身份保护。

---

## 3. 验收清单（自勾）

- [ ] blocking 与 nonblocking 行为差异能讲清
- [ ] stale miss response 不会写坏 reused ROB entry
- [ ] microbench PASS，DCache miss 相关 stall 下降
- [ ] full regression 通过

下一章：[11j_阶段10j_完整TAGE与ITAGE.md](11j_阶段10j_完整TAGE与ITAGE.md)。
