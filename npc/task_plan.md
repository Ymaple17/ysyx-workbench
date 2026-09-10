# OOOD ysyxSoC 接入计划

## 目标

让 `npc/oood_chisel_vsrc` 的乱序核通过 ysyxSoC 仿真运行 MicroBench `test` 规模，同时不终止或干扰正在 NPC 中运行的 CoreMark。

## 硬约束

- 不执行 `kill`、`pkill`、`killall`。
- 不清理或重建正在由 CoreMark PID 7505 使用的 `npc/obj_dir`。
- SoC 构建使用独立输出目录或 ysyxSoC 自有构建目录。
- 保留工作树已有修改，先审计再增量适配。
- 验收标准是 ysyxSoC 路径运行 MicroBench `test` 规模并正常结束。

## 阶段

| 阶段 | 状态 | 内容 | 门禁 |
|---|---|---|---|
| S0 | in_progress | 审计 Kconfig、Makefile、SoC wrapper、已有改动和进程 | 找出 NPC/SoC 差异及安全构建入口 |
| S1 | pending | 建立不碰 NPC CoreMark 的隔离构建路径 | 构建目录与 PID 7505 使用目录不重合 |
| S2 | pending | 补齐 oood 的 ysyxSoC 顶层、filelist 和配置接入 | ysyxSoC 仿真可编译链接 |
| S3 | pending | 运行最小 smoke 与 MicroBench test | test 规模正常结束且结果通过 |
| S4 | pending | 更新文档、记录命令和结果 | 结果可复现，CoreMark 仍在运行 |

## 错误记录

| 错误 | 次数 | 处理 |
|---|---:|---|
| Windows `Test-NetConnection` 多主机探测未返回结果 | 1 | 改用带 2 秒超时的 `TcpClient` 探测，确认 192.168.56.128:22 在线 |
