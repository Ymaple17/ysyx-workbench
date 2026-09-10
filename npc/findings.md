# OOOD ysyxSoC 接入发现

- VM：`qiu@192.168.56.128`，仓库 `/home/qiu/ysyx-workbench`，分支 `ysyx-b-stage`。
- CoreMark make 链 PID：5269、5337、5338；仿真主进程 PID：7505。
- PID 7505 的 cwd 是 `/home/qiu/ysyx-workbench/npc`，正在使用 `npc/obj_dir/Vysyx_25020039`。
- 当前工作树已有 oood Chisel 生成 RTL 和 NPC 配置修改，必须保留。
- 已存在 `npc/oood_chisel_soc_csrc`、`npc/oood_chisel_vsrc`、`npc/oood_chisel_csrc`，并存在未跟踪配置标记 `npc/include/config/oood/chisel/soc.h`。
- ysyxSoC 子模块本身有未提交状态，需先只读审计。
