# board_soc

`board_soc` is the pure-PL FPGA shell for the current `board_pipeline` CPU.
It combines the CPU snapshot in `rtl/cpu`, a burst-capable AXI BRAM, and a
local UART/GPIO peripheral. The shell is intended for board bring-up and
CoreMark measurements; it does not use the Zynq PS, DDR, or Linux.

## Hardware

The current Navigator Zynq-7020 build uses:

| Signal | Pin | Function |
| --- | --- | --- |
| `clk_50m` | `U18` | 50 MHz board clock |
| `sys_rst_n` | `N16` | Active-low reset input |
| `uart_txd` | `J15` | PL USB-UART TX to the onboard CH340 |
| `led[0]` | `H15` | Heartbeat |
| `led[1]` | `L15` | Core status / breakpoint indicator |

The USB-UART RX pin in the official pin table is `T19`, but this shell only
transmits. The separate PL RS232/RS485 connector (`M15`/`K14`) is not the
onboard CH340 path. On the current host, the CH340 enumerates as `COM11`.

The CPU reset is held for 65,536 clock cycles after the external reset is
released. This gives a deterministic power-up reset even when the board is
programmed with the reset button already released.

## Clock And Frequency

The top level receives the 50 MHz board clock on `clk_50m` and generates the
CPU/peripheral clock with `rtl/board_clock.sv`. The current implementation uses
a Xilinx 7-series MMCM with a 1050 MHz VCO. The default output is 84 MHz
(feedback multiplier 21.0 and output divider 12.5), and the reset path waits
for `LOCKED` before releasing the CPU.

The old direct-clock sweep is retained as a timing history. It constrained the
input port directly and did not change the physical clock:

| Constraint | Route WNS | Route TNS | Result |
| ---: | ---: | ---: | --- |
| 50 MHz | +2.772 ns | 0 | pass |
| 60 MHz | +0.727 ns | 0 | pass |
| 65 MHz | +0.723 ns | 0 | pass |
| 70 MHz | +0.093 ns | 0 | pass |
| 71 MHz | +0.670 ns | 0 | pass |
| 75 MHz | +0.133 ns | 0 | pass |
| 80 MHz | +0.200 ns | 0 | pass |
| 82.5 MHz | +0.194 ns | 0 | pass |
| 84 MHz | +0.006 ns | 0 | pass |
| 85 MHz | -0.250 ns | -1.412 ns | fail |

The authoritative generated-clock build is `vivado/out_84mhz_mmcm_coremark/`.
Its routed timing report shows `mmcm_clkout` at 84.000 MHz with WNS +0.134 ns,
TNS 0, and WHS +0.041 ns. The physical board result is 20 CoreMark
iterations in 105 ms, 8,890,331 cycles, and 556 Marks with all expected CRCs.

The clock is selected at build time. For example, the default 84 MHz build is:

```text
$env:BOARD_SOC_CPU_FREQ_MHZ='84.0'
$env:BOARD_SOC_OUT_DIR='out_84mhz_mmcm_coremark'
vivado.bat -mode batch -source vivado/build_vivado.tcl
vivado.bat -mode batch -source vivado/program_board_soc.tcl
```

The `build_vivado.tcl` script uses the same variables. Change only
`BOARD_SOC_CPU_FREQ_MHZ` and use a new output directory for an A/B build, then
check the generated clock frequency and routed WNS/TNS before programming. The
UART divider and the clock-frequency register are derived from the same value.

## Address Map

| Address | Device | Behavior |
| --- | --- | --- |
| `0x8000_0000` - `0x8001_ffff` | AXI RAM | 128 KiB initialized BRAM window |
| `0x1000_0000` | UART TX | Low byte is appended to the TX FIFO |
| `0x1000_0003` | UART LCR | Software-visible 8-bit control register |
| `0x1000_0004` | LED | Low two bits drive `led[1:0]` |
| `0x1000_0005` | UART LSR | Bit 5 is set while TX FIFO has space |
| `0x1000_0008` | Cycle counter low | Read-only low 32 bits |
| `0x1000_000c` | Cycle counter high | Read-only high 32 bits |
| `0x1000_0010` | Clock frequency | Read-only CPU clock in Hz |

The UART is 115200 8N1 with a 256-byte FIFO. Firmware writes directly to the
TX register and does not poll the LSR for normal output, which avoids making
the benchmark depend on a serial-drain latency. The AXI RAM and board wrapper
remain separate from the CPU so a later DDR/PS or peripheral fabric can
replace this shell without changing the CPU interface.

## Build

The normal workflow is:

1. Generate/copy the desired `board_pipeline_vsrc` FPGA CPU snapshot into
   `rtl/cpu`.
2. Build a firmware image and copy its word-oriented hex file to
   `board_image.hex`.
3. Run Vivado 2020.2 in batch mode:

```text
vivado.bat -mode batch -source vivado/build_vivado.tcl
```

The default bitstream is written to
`vivado/out_84mhz_mmcm_coremark/board_soc.bit`. The checked CoreMark image can
be regenerated in the VM with:

```text
cd firmware/coremark
./build_coremark.sh
cp build/board_image.hex ../../board_image.hex
```

The script uses `riscv64-linux-gnu-gcc` with `rv32em`, `ilp32e`, strict
alignment, and `-O2`, then checks that the binary fits in the 128 KiB BRAM.

## Programming and Validation

With Vivado 2020.2 and the board connected through JTAG, run from this
directory:

```text
vivado.bat -mode batch -source vivado/program_board_soc.tcl
```

Open the host's `USB-SERIAL CH340` port at 115200 8N1. A capture can be made
with:

```text
powershell -NoProfile -File tools/capture_uart.ps1 -PortName COM11 -DurationSeconds 20
```

The current physical validation on COM11 received the complete 84 MHz CoreMark
result:

```text
Total time (ms)  : 105
Iterations       : 20
Finished in 8890331 cycles.
CoreMark PASS       556 Marks
```

The CoreMark CRC fields also matched the expected values (`e714`, `1fd7`,
`8e3a`, and `4983`). This confirms the complete path from CPU execution and
AXI BRAM initialization through the PL UART TX pin `J15` and the CH340.

## Scope and Provenance

This is a small pure-PL board SoC for bring-up, not a full Linux-capable Zynq
system. The AXI RAM is pinned from the MIT-licensed
`alexforencich/verilog-axi` repository at commit `516bd5d`. The upstream
repository is deprecated in favor of Taxi, but this pinned snapshot has the
AXI4 RAM interface required by the current CPU and keeps the educational shell
easy to audit. For a future larger SoC, evaluate Taxi or LiteX separately.
