# 乱序双发 CPU（OoO）设计文档

**工程**：`npc/oood_chisel_vsrc` + `npc/oood_chisel_csrc`  
**顺序基线（对照）**：`npc/ioid_chisel_vsrc` + `ioid_chisel_csrc`

## 文档怎么组织

- **一阶段一篇**：原理 + 为什么这样设计 + **接口/源码** + 接线步骤 + 时序 + 踩坑 + 验收。  
- **没有**单独的「施工_」文档——动手内容写在对应阶段文里。  
- 验收清单 **只由你勾选**；仓库里的参考实现 ≠ 你已学完。

## 阅读顺序

| 步 | 文档 | 你学到什么 |
|----|------|------------|
| 1 | [00_入门与路线.md](00_入门与路线.md) | 发射/写回/提交、重命名、路线图、仓库地图 |
| 2 | [01_阶段0_乘除与基线.md](01_阶段0_乘除与基线.md) | DIV kill、fencei、RAS 基线 |
| 3 | [02_阶段1_四件套.md](02_阶段1_四件套.md) | PRF/Busy/Rename/ROB 完整接口与实现要点 |
| 4 | [03a](03a_阶段2a_恒等PRF.md) → [03b](03b_阶段2b_ROB元数据.md) → [03c](03c_阶段2c_写侧freelist.md) → [03d](03d_阶段2d_源phys.md) | 顺序后端拆四档，每档可单独 difftest |
| 5 | [04a](04a_阶段3a_提交语义.md) → [04b](04b_阶段3b_RS壳.md) → [04c](04c_阶段3c_乱序发射.md) → [04d](04d_阶段3d_真乱序发射.md) | 提交 → RS 壳 → RS→EX → **真乱序** |
| 6 | [05](05_阶段4_异常CSR.md) → [05a](05a_阶段4a_ROB补CSR字段.md) → [05b](05b_阶段4b_CSR提交写.md) → [05c](05c_阶段4c_异常提交化.md) → [05d](05d_阶段4d_ebreak提交.md) → [05e](05e_阶段4e_mret提交.md) → [05f](05f_阶段4f_fencei提交.md) → [05g](05g_阶段4g_外部中断.md) | 异常/CSR 提交化 |
| 7 | [06](06_阶段5_内存序.md) → [06a](06a_阶段5a_Store提交写.md) → [06b](06b_阶段5b_Load冲突与去memhead.md) | 内存序（5a/5b 已落地） |
| 8 | [07](07_阶段6_前端.md) → [08](08_阶段7_调优.md) | 前端 FQ（基础已落地）/ 参数扫描 |
| 9 | [09a](09a_阶段8a_多FU并行.md) → [09b](09b_阶段8b_多发射.md) → [09c](09c_阶段8c_全投机.md) → [09d](09d_阶段8d_内存乱序.md) | **阶段 8 超标量化**（多 FU → 多发射 → 全投机 → 内存乱序） |
| 10 | [10a](10a_阶段9a_性能计数器与baseline.md) → [10b](10b_阶段9b_前端FQ与BPU瓶颈分析.md) → [10c](10c_阶段9c_参数扫描与IPC报告.md) → [10d](10d_阶段9d_小步优化与回归方法.md) | **阶段 9 性能画像与调优**（从“能跑”到“知道为什么快/慢”） |
| 11 | [11a](11a_阶段10a_独立SQ与StoreBuffer.md) → [11b](11b_阶段10b_访存层次与非阻塞缓存.md) → [11c](11c_阶段10c_宽提交与多CDB.md) → [11d](11d_阶段10d_宽取指与前端带宽.md) → [11e](11e_阶段10e_高级分支预测.md) → [11f](11f_阶段10f_真正双提交与退休.md) → [11g](11g_阶段10g_扩大乱序窗口与参数扫描.md) → [11h](11h_阶段10h_重新启用宽取指.md) → [11i](11i_阶段10i_非阻塞DCache与MSHR.md) → [11j](11j_阶段10j_完整TAGE与ITAGE.md) → [11k](11k_阶段10k_放宽双提交类型.md) → [11l](11l_阶段10l_LoadQueue与Replay.md) → [11m](11m_阶段10m_持续双取指与FTQ.md) | **阶段 10 IPC 上限扩展**（内存序 / cache / 写回 / 前端 / 预测 / 真双提交 / 窗口 / LQ/replay / FTQ） |
| 12 | [阶段11路线](12_阶段11_IPC从0.6到1.md) → [12a](12a_阶段11a_依赖链与执行吞吐.md) → [12b](12b_阶段11b_访存吞吐与DCache重构.md) → [12c](12c_阶段11c_持续前端与有效宽度.md) → [12d](12d_阶段11d_IPC1验收与回归.md) | **阶段 11 IPC 0.6 → 1.0**（依赖链 / 访存吞吐 / 有效前端 / 严格收口） |
| 13 | [13a](13a_阶段12a_MMU与虚拟内存.md) → [13b](13b_阶段12b_多核与一致性.md) | **阶段 12 系统扩展**（MMU/SV32、多核一致性，后置可选） |

**参考实现水位（≠ 你的学习勾选）**：源码已完成并严格验收 **阶段 11d 教学参考核**。它是真乱序双发核：fetch、rename/dispatch 和 commit 主干均为 2-wide；RS 向 `ALU0/ALU1/LSU/DIV` 四类分布式端口各选本地 oldest-ready，完成结果经 4→2 age arbiter 进入 2 CDB。阶段 11 在 10m 上加入 accepted-CDB 同拍 wake/select/value bypass、弹性 LSU、无目的控制流 ROB/RS 直接完成、predicted-not-taken branch+lane1、bimodal/TAGE tournament；访存侧加入 AXI read/write burst、16-entry StoreBuffer 合并/连续写、`64×32B=2KiB` direct-mapped DCache、同拍 hit response、1 active MSHR + 1 secondary slot/same-line merge，以及 LQ4 tagged replay。当前仍保留 `ROB_SIZE=32`、`N_PHYS=64`、`RS_SIZE=8`、`FQ_SIZE=8`、`FETCH_BUFFER_SIZE=2`、`FTQ_SIZE=16`、`WIDE_FETCH_MIN_SPACE=6`、`LQ_SPECULATE_UNKNOWN_STORES=false`；TAGE 为 3×256 项、历史 2/5/10，ITAGE 为 2×128 项、历史 4/10。最终 full regression 通过 `OoOUnitTest` **80/80**、7/7 cpu-tests+difftest、零 compile/chisel-gen warning；两轮 microbench 的 98 列摘要除 `tag` 外其余 97 个字段一致：IPC **`1.0117`**（`516339` cycles / `522374` commits）、DCache hit rate `97.29%`、`FTQ Stale Recover=0`、`Mem Order Violation=0`。结果与目录见 [阶段 11d](12d_阶段11d_IPC1验收与回归.md)。你的学习验收清单仍由你自己完成；MMU 与多核一致性继续后移到阶段 12。

## 原则

1. 本阶段 **cpu-tests + difftest 自测绿** 再往下。  
2. **2d 不过 → 不做 3b**；**3a 不过 → 不做 3c**；**3c 绿 → 3d**；**3d 绿 → 4a/4b**；**4b 绿 → 4c**；**4c 绿 → 5a/5b**；**6 绿 → 7（先扫基线）→ 8a–8d（超标量）→ 9（性能画像）→ 10（双发主干）→ 11（IPC 1.0 收口）→ 12（系统扩展）**。  
3. freelist flush **禁止**重置成「32..N_PHYS-1 全空闲」。
4. 文档与仓库字段冲突 → **先改文档**（以 `unit/`、`core/` 真实代码为准）。  
5. 四件套在 **`unit/`**，流水级在 **`core/`**，参数在 **`common/ooo_params.scala`**。  
6. 验收清单助手 **不代勾**。  
7. 讲义里「目标规格」≠「已接线」——各章文首现状框为准。

## 回归

```bash
cd $NPC_HOME
scripts/stage9_regress.sh --mode full --tag final_check
# 阶段 10 最终收官：full regression + 重复 microbench + IPC/FTQ/双 ALU 门槛
scripts/stage10_final.sh
# 阶段 11 最终收官：完整回归 + 同一二进制复跑 + IPC >= 1.0
scripts/stage11_final.sh
```

脚本默认覆盖本阶段验收口径：逐项单测、`cpu-tests`、`microbench(test)`。microbench 只有同时出现 `MicroBench PASS` 与 `HIT GOOD TRAP`、没有 HANG/ABORT/difftest mismatch、且 IPC/cycles/commits 三项非空时才算通过。

## 参考

- 姚永斌《超标量处理器设计》第 3 章  
- 香山 rename / rob / rs；NutShell ISU/WBU  
