# board_soc 上板操作手册

本文档对应当前 `board_soc` 参考板核：`board_pipeline` 的 FPGA-safe CPU
快照 + 纯 PL AXI BRAM + PL UART/GPIO。目标是让你在替换 CPU、修改程序或
调节频率后，可以重复完成仿真、综合、下载和串口验收。

## 1. 当前结构

当前工程不使用 Zynq PS、DDR、Linux 或 AXI GP 端口，所有逻辑都在 PL 内：

```text
Navigator 50 MHz U18
        |
        v
rtl/board_clock.sv  -- MMCM, 默认输出 84 MHz
        |
        +--> ysyx_25020039 CPU 快照
        |          |
        |          +--> AXI4 master --> 地址路由 --> 128 KiB AXI BRAM
        |                                      \-> UART/GPIO
        |
        +--> UART/GPIO 与心跳逻辑
```

默认时钟参数为：输入 50 MHz、MMCM VCO 1050 MHz、CPU/外设输出 84 MHz。
CPU reset 会等待 MMCM `LOCKED`，然后再保持 65,536 个 CPU 周期，保证下载
后即使复位键没有按下也能稳定启动。仿真时没有 Xilinx primitive model，
`board_clock.sv` 自动退化为 50 MHz 直通，仿真 UART 因此使用 50 MHz 对应的
分频值；综合时才启用真实 MMCM 和所选频率的 UART 分频值。

## 2. 硬件和软件准备

硬件：

| 项目 | 当前配置 |
| --- | --- |
| FPGA | 正点原子领航者 Zynq-7020，`xc7z020clg400-1` |
| JTAG | Vivado Hardware Manager 可见 FPGA 目标 |
| 板载时钟 | `clk_50m`，引脚 `U18` |
| 外部复位 | `sys_rst_n`，引脚 `N16`，低有效 |
| PL UART TX | `uart_txd`，引脚 `J15`，连接板载 CH340 |
| 心跳 LED | `led[0]`，引脚 `H15` |
| 核状态 LED | `led[1]`，引脚 `L15` |
| Windows 串口 | 当前机器为 `COM11` |

串口参数固定为 `115200 8N1`：115200 baud、8 个数据位、无校验、1 个停止
位。先确认跳线已经切到 PL UART 路径，并关闭占用 `COM11` 的串口工具；
Vivado 下载使用 JTAG，串口只负责观察程序输出。

软件：

- Vivado 2020.2，安装路径按当前机器为 `D:\Xilinx\Vivado\2020.2\bin\vivado.bat`。
- PowerShell。
- VM 中的 ysyx-workbench，当前地址为 `qiu@192.168.56.128`。
- VM 中可用的 `mill`、RISC-V 交叉编译器和 NPC 依赖。

以下命令默认在本地目录执行：

```powershell
cd C:\Users\qiu\Documents\Codex\2026-08-19\ni-en\work\board_soc
$vivado = 'D:\Xilinx\Vivado\2020.2\bin\vivado.bat'
```

## 3. 把 CPU 核接入 board_soc

### 3.1 必须保持的顶层模块

当前 wrapper 实例化的模块名是 `ysyx_25020039`。CPU 至少需要提供以下
端口，方向以 CPU 视角为准：

- `clock`、`reset`、`io_interrupt`、`io_ebreak`。
- AXI read address：`io_master_araddr`、`arvalid`、`arid`、`arlen`、
  `arsize`、`arburst`、`arready`。
- AXI read data：`io_master_rdata`、`rresp`、`rvalid`、`rlast`、`rid`、
  `rready`。
- AXI write address：`io_master_awaddr`、`awvalid`、`awid`、`awlen`、
  `awsize`、`awburst`、`awready`。
- AXI write data/response：`io_master_wdata`、`wstrb`、`wvalid`、`wlast`、
  `wready`、`bresp`、`bvalid`、`bid`、`bready`。
- 现有生成核还带有完整的 `io_slave_*` 端口；`board_soc_top.sv` 将其绑到
  无效值。若你改了模块接口，应该只在 wrapper 里做适配，不要让 BRAM/UART
  路由依赖某一个核的内部信号。

AXI 约束也必须保持：读返回用 `rlast` 表示最后一拍；写地址和写数据遵守
独立握手；ID 原样返回；BRAM 地址落在 `0x8000_0000` 到 `0x8001_ffff`。
UART/GPIO 地址是 `0x1000_0000` 区域。

### 3.2 生成 Chisel FPGA 快照

在 VM 中进入 NPC：

```fish
ssh qiu@192.168.56.128
cd /home/qiu/ysyx-workbench/npc
make menuconfig
```

选择 `CHISEL_NPC`、`BOARD_PIPELINE_CHISEL_NPC` 或你实际维护的 OoO NPC 配置。当前
Makefile 的 Chisel NPC 路径约定如下：

```text
配置             Chisel 目录       生成入口
CHISEL                    chisel_vsrc                  ./mill -i mychisel.runMain TopMain
BOARD_PIPELINE_CHISEL     board_pipeline_vsrc          ./mill -i mychisel.runMain TopMain
OOOD_CHISEL_2             oood_chisel_2_issue_vsrc     ./mill -i mychisel.runMain TopMain
OOOD_CHISEL               oood_chisel_4_issue_vsrc     ./mill -i mychisel.runMain TopMain
```

最稳妥的生成命令是直接使用 Makefile 目标，因为它会根据 `.config` 选对
目录和入口：

```fish
make chisel-gen
```

如果要手动生成当前 pipeline 快照：

```fish
cd /home/qiu/ysyx-workbench/npc/board_pipeline_vsrc
./mill -i mychisel.runMain TopMain
```

`TopMainSoC` 只用于 ysyxSoC 的整 SoC 模式，不能把它生成的整 SoC 顶层
直接塞进本工程。`board_soc` 需要的是 CPU 模块和它的子模块。

### 3.3 复制 RTL

不要直接把 NPC 的仿真顶层复制进来。复制生成的 CPU RTL 时：

1. 保留生成的 `ysyx_25020039.sv` 及其依赖模块。
2. 排除 `pmem_dpi.v`、NPC `top.sv`、DPI、Verilator 仿真 harness 和
   ysyxSoC 整 SoC 顶层。
3. 将剩余 `.sv`/`.v` 文件复制到本地的 `rtl\cpu`，覆盖之前先备份或提交
   当前可工作的版本。
4. 确认 `ysyx_25020039.sv` 的模块名和端口仍与 `rtl\board_soc_top.sv`
   一致。

例如，在 Windows 端从已经导出的快照目录复制：

```powershell
Remove-Item .\rtl\cpu\*.sv -Force
Copy-Item C:\path\to\board_pipeline_vsrc\*.sv .\rtl\cpu\ -Force
```

复制后先做语法检查，再启动 Vivado。不要把 OOOD 四发射版本接入这个
`board_soc` 作为“只改一个文件”的替换：它可能有不同的 AXI 时序、资源规模
和 reset/存储器假设，应该先做接口适配和独立资源评估。

## 4. 编译和准备程序

### 4.1 CoreMark

当前板上 CoreMark 的源码在 `firmware/coremark`。它使用
`rv32em/ilp32e`、`-O2`、严格对齐，链接到 `0x8000_0000`，镜像最大不能
超过 128 KiB。`core_portme.c` 会读取 UART 的 `0x1000_0010` 时钟寄存器，
所以频率改变后 CoreMark 的毫秒和 Marks 仍按真实 CPU 频率计算。

VM 交叉编译的通用操作：

```fish
cd /home/qiu/ysyx-workbench
mkdir -p /tmp/board_coremark
# 将 board_soc/firmware/coremark 同步到 /tmp/board_coremark 后执行：
cd /tmp/board_coremark
./build_coremark.sh
cp build/board_image.hex /home/qiu/ysyx-workbench/board_soc/board_image.hex
```

本地 `board_soc\firmware\coremark` 是版本记录和可复现来源；如果你在
本地修改了 `core_portme.c`，要先同步该文件再执行 VM 构建。生成的
`board_image.hex` 是按 32 位字初始化 BRAM 的 Verilog `$readmemh` 格式，
不是 ELF，也不是原始 binary。

### 4.2 自定义裸机程序

程序必须：

- 使用 `rv32em` 和 `ilp32e` ABI，避免生成不支持的指令。
- 链接地址放在 `0x8000_0000`，栈顶放在 128 KiB RAM 末端。
- 通过 `0x1000_0000` 的低 8 位写 UART TX。
- 用 `bin_to_hex.py` 把 binary 转成 `board_image.hex`。
- 检查 binary 大小不超过 `131072` 字节。

最小输出示例：

```c
static volatile unsigned char *const uart =
    (volatile unsigned char *)0x10000000u;
void _start(void) {
  *uart = 'O';
  *uart = 'K';
  *uart = '\\n';
  for (;;) {}
}
```

程序镜像替换后必须重新综合，因为 BRAM 初始化内容会写入 bitstream；只
重新下载旧 bitstream 不会加载新的 hex 文件。

## 5. 仿真验收

### 5.1 语法检查

在本地安装 Icarus Verilog 后，运行：

```powershell
& 'D:\iverilog\bin\iverilog.exe' -g2012 -D__ICARUS__ `
  -s board_soc_top -o .\board_soc_syntax.vvp `
  .\rtl\*.sv .\rtl\cpu\*.sv .\vendor\verilog-axi\rtl\axi_ram.v
```

出现 `BOARD_SOC_MMCM_SYNTAX_PASS` 对应的成功状态即可；Icarus 对现有
Chisel 生成 RTL 可能报告 `always_*` 常量选择 warning，这些不是语法失败。

### 5.2 UART/程序仿真

使用现成 CoreMark testbench：

```powershell
& 'D:\iverilog\bin\iverilog.exe' -g2012 -D__ICARUS__ `
  -s tb_board_soc_coremark -o .\board_soc_coremark.vvp `
  .\tb_board_soc_coremark.sv .\rtl\*.sv .\rtl\cpu\*.sv `
  .\vendor\verilog-axi\rtl\axi_ram.v
& 'D:\iverilog\bin\vvp.exe' .\board_soc_coremark.vvp
```

仿真 testbench 使用 50 MHz fallback clock 和 434 的 UART 分频值；它检查
UART 串流中的 `PASS`。仿真长时间无输出时，先检查 reset 是否释放、
`board_image.hex` 是否存在，以及 CPU 是否仍在使用 `0x8000_0000` 的入口。

## 6. Vivado 调频、综合和下载

### 6.1 选择频率

频率由一个环境变量控制，不要手工修改 generated XDC 的时钟周期：

```powershell
$env:BOARD_SOC_CPU_FREQ_MHZ = '84.0'
$env:BOARD_SOC_OUT_DIR = 'out_84mhz_mmcm_coremark'
& $vivado -mode batch -source .\vivado\build_vivado.tcl
```

要快速做另一个频点，只改频率和输出目录，例如：

```powershell
$env:BOARD_SOC_CPU_FREQ_MHZ = '80.0'
$env:BOARD_SOC_OUT_DIR = 'out_80mhz_mmcm'
& $vivado -mode batch -source .\vivado\build_vivado.tcl
```

当前 `board_clock.sv` 固定 VCO 为 1050 MHz，MMCM 输出分频值必须是 Xilinx
允许的范围和 0.125 步进；84 MHz 对应 12.5，是已验证频点。每次调频后
检查：

```powershell
Select-String .\vivado\$env:BOARD_SOC_OUT_DIR\timing_route.rpt `
  -Pattern 'mmcm_clkout|WNS|TNS|WHS|THS'
```

只有 `mmcm_clkout` 的频率正确且 WNS/TNS/WHS/THS 满足要求时，才下载该
bitstream。旧变量 `BOARD_SOC_CLOCK_PERIOD_NS` 仍兼容，但新流程只推荐使用
`BOARD_SOC_CPU_FREQ_MHZ`。

### 6.2 JTAG 下载

默认下载脚本已经指向当前 84 MHz bitstream：

```powershell
$env:BOARD_SOC_OUT_DIR = 'out_84mhz_mmcm_coremark'
& $vivado -mode batch -source .\vivado\program_board_soc.tcl
```

也可以直接指定 bit 文件：

```powershell
$env:BOARD_SOC_BIT_FILE = (Resolve-Path `
  .\vivado\out_84mhz_mmcm_coremark\board_soc.bit).Path
& $vivado -mode batch -source .\vivado\program_board_soc.tcl
Remove-Item Env:BOARD_SOC_BIT_FILE -ErrorAction SilentlyContinue
```

Vivado 日志中应看到目标 `xc7z020_1`、program 成功和 startup status
`HIGH`。如果找不到目标，先在 Hardware Manager 确认 JTAG server
`localhost:3121` 和 USB 驱动，再重新执行脚本。

## 7. 串口运行与验收

建议在下载前先启动串口捕获，避免漏掉程序最开始的输出：

```powershell
Start-Process powershell -ArgumentList @(
  '-NoProfile', '-File', '.\tools\capture_uart.ps1',
  '-PortName', 'COM11', '-DurationSeconds', '60'
)
& $vivado -mode batch -source .\vivado\program_board_soc.tcl
```

也可以用 PuTTY、Tera Term 或串口助手打开 `COM11`，设置为 `115200 8N1`，
再下载 bitstream。下载完成后等待自动 reset 和程序输出，不需要通过串口
向 CPU 发送数据；当前 UART 是单向 TX 验证路径。

当前 84 MHz CoreMark 的验收输出应包含：

```text
Total time (ms)  : 105
Iterations       : 20
Finished in 8890331 cycles.
CoreMark PASS       556 Marks
```

期望 CRC 为 `e714`、`1fd7`、`8e3a`、`4983`。`led[0]` 是 heartbeat，
`led[1]` 是 CPU 状态/断点指示。Cycle counter 和频率寄存器可由裸机程序
从以下地址读取：

```text
0x10000008  cycle counter low
0x1000000c  cycle counter high
0x10000010  CPU clock Hz, current 84 MHz build returns 84000000
```

## 8. 常见问题

### 串口没有输出

确认使用的是 `J15` 对应的板载 CH340、跳线已切到 PL UART、`COM11` 没被
其他程序占用，并且串口工具已在下载前打开。PL UART RX `T19` 当前未使用，
电脑不需要向 CPU 发送数据。

### 修改频率后 CoreMark 时间不对

确认 CPU 是通过 `board_soc_top` 的 `cpu_clk` 工作，并且综合不是仿真宏
路径；确认 UART divider 和 `0x1000_0010` 都由同一次 `CPU_FREQ_KHZ`
generic 生成。不要只改 XDC 约束，那只会改变 timing analysis。

### Vivado 报 timing fail

换一个新的 `BOARD_SOC_OUT_DIR` 保存报告，先确认实际 `mmcm_clkout` 频率，
然后降低频率。不能因为 XDC 报告通过就认定板上实际频率已改变；必须使用
`board_clock.sv` 的 MMCM 路径并检查报告。

### 下载了旧程序

BRAM 内容在 bitstream 生成时固化。重新生成 `board_image.hex` 后，必须重新
运行 `build_vivado.tcl`，然后下载新生成的 `board_soc.bit`。

## 9. 最短可复现流程

```powershell
cd C:\Users\qiu\Documents\Codex\2026-08-19\ni-en\work\board_soc
$vivado = 'D:\Xilinx\Vivado\2020.2\bin\vivado.bat'
$env:BOARD_SOC_CPU_FREQ_MHZ = '84.0'
$env:BOARD_SOC_OUT_DIR = 'out_84mhz_mmcm_coremark'
& $vivado -mode batch -source .\vivado\build_vivado.tcl
Start-Process powershell -ArgumentList @('-NoProfile', '-File',
  '.\tools\capture_uart.ps1', '-PortName', 'COM11',
  '-DurationSeconds', '60')
& $vivado -mode batch -source .\vivado\program_board_soc.tcl
```

验收顺序是：Vivado bitstream 生成成功、route timing 通过、JTAG startup
status 为 `HIGH`、串口收到 `CoreMark PASS` 且 CRC 正确。四项缺一不可。
