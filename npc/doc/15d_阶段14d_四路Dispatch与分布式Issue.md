# 阶段 14d：四路 Dispatch 与分布式 Issue

## 学习导航
- **理论目标**：理解四路 dispatch 的难点是异构资源组合信用，而四路 issue 的难点是年龄选择、FU 互斥和不建立集中式全局调度器。
- **最小实现**：四槽 packet 按类型路由到 integer RS、BRQ、LQ/SQ；资源按 lane prefix 预留，保留每 FU local oldest-ready。
- **当前参考核**：四路 dispatch credit 和分布式 FU issue 已接入；冻结点 `Dispatch Slot1/2/3=117122/94958/70813`，`Quad ALU Issues=4734`。当前仍最多一条 control，访存受现有 LSU/L1D bank 服务约束。
- **后续扩展**：Stage15 先提高前端/L1D 服务率，再按计数决定 2x3 clustered backend、第二 BRU 或更多 memory admission，避免扩宽空转。
- **验收方式**：四普通 ALU、混合 ALU/DIV/load/branch、资源差一项、flush 同拍 wakeup、FU backpressure 定向测试；slot2/3 dispatch 和 issue 计数必须大于零。

---

## 1. Prefix credit

```text
for lane <- 0 until 4:
  classify lane
  check remaining local credits
  reserve if accepted
  stop at first unaccepted lane
```

不能让四条中的 lane3 缺 BRQ 就错误接受 lane4，也不能让一个资源缺口绕过程序序。若使用 Rename Queue，可以在 queue head 做 compact/group dispatch，但 ROB 程序序保持不变。

## 2. 最小类型限制

```text
ordinary integer: up to 4 accepted
control:          at most 1 per cycle
memory:           at most 1 per cycle
CSR/fence/MMIO:   exclusive
```

限制应输出 blocker counter。后续只有当 memory/control lane limit 成为主墙才增加端口。

## 3. Issue

各 FU 继续在自己的 eligible mask 内找 oldest-ready，并用跨口互斥避免同一 RS entry 被多个口选择。fresh-enqueue bypass 只能在该 lane 已完成 rename/ROB/RS 原子分配后触发。

## 4. 验收清单（学习者自勾）

- [ ] 四路资源预留遵循程序序前缀
- [ ] 同一 entry 不被两个 FU issue
- [ ] selective flush 不丢 older wakeup
- [ ] slot2/slot3 有真实 dispatch/issue
- [ ] 没有全核集中 stall FSM

下一章：[15e_阶段14e_PRF操作数与完成网络.md](15e_阶段14e_PRF操作数与完成网络.md)。
