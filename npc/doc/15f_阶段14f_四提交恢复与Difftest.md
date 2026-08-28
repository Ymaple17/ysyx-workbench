# 阶段 14f：四提交、恢复与 Difftest

## 学习导航
- **理论目标**：理解四提交仍必须逐条保持程序序，副作用和异常边界不能用并行 valid 掩盖顺序关系。
- **最小实现**：ROB 最多提交四条普通指令；按 lane0→3 依次更新 arch RAT、FreeList、BPU、SQ/StoreBuffer 和 C++ difftest，特殊指令建立独占边界。
- **当前参考核**：双提交，lane1 已支持普通 load/control/cacheable store；四路 commit 和 C++ 导出尚未实现。
- **后续扩展**：多 store commit、双分支训练、多个特殊副作用；最小版本允许每拍最多一个 store/control side effect。
- **验收方式**：四普通提交、同 rd WAW、lane1/2/3 exception、store/control 排序、interrupt 间隙、flush+commit 同拍和四路 difftest 定向测试。

---

## 1. Commit prefix

提交 valid 也必须形成连续前缀：lane i 不能提交时，所有更后 lane 禁止提交。普通 done 指令可连续退休；exception/interrupt/fence/MMIO/CSR 建立该拍边界或独占。

## 2. 同拍 arch RAT/free

四条按程序序更新：后 lane 同 rd 覆盖前 lane 的 arch mapping，但每条 old phys 都按自己的 ROB metadata 释放。FreeList push 使用集合并显式处理重复/phys0，不能依赖多个动态 Vec 写的 last-connect。

## 3. Redirect

若同拍多个 control 完成，只允许最老 mispredict 产生 redirect；更年轻完成即使结果已知也会被该 redirect 杀死。恢复使用对应 checkpoint/FTQ snapshot，不能恢复到拍前全局 arch RAT。

## 4. C++ Difftest

```cpp
for (int lane = 0; lane < COMMIT_WIDTH; ++lane) {
  if (!commit_valid[lane]) break;
  step_reference_once(commit_pc[lane]);
  compare_arch_state();
}
```

四条必须逐条 step/compare，不能一次 step 4 后只比较最终状态，否则中间副作用次序错误会被掩盖。

## 5. 验收清单（学习者自勾）

- [ ] commit valid 是连续前缀
- [ ] 四路同 rd arch RAT/free 正确
- [ ] 特殊/异常副作用不被后 lane 越过
- [ ] 最老 redirect 唯一生效
- [ ] C++ difftest 逐 lane 比较

下一章：[15g_阶段14g_IPC2.5验收与回归.md](15g_阶段14g_IPC2.5验收与回归.md)。
