# ioid 五级流水核：FPGA 上板与改核计划（良好档）

> 平台：正点原子领航者 **Zynq-7020（xc7z020clg400-1）**  
> 代码基线：`npc/ioid_chisel_vsrc`  
> 目标口径：**良好档（B）** —— CoreMark + 游戏兼顾  
> 主频：**100MHz**  
> 文档用途：后面改核 / 上板时按阶段勾选执行，不依赖口头记忆

---

## 0. 目标与门禁（写进验收）

### 0.1 对外承诺（答辩可报）

| 项 | 目标 |
|---|---|
| CPU 主频 | **100MHz**，setup WNS ≥ 0、TNS = 0；CPU-only 目标 WNS ≥ +0.5ns |
| CoreMark/MHz | 冻结门禁 **≥ 1.8**，良好目标 **≥ 2.0**，冲刺 **≥ 2.2** |
| IPC（CoreMark） | 冻结门禁 **≥ 0.70**，良好目标 **≥ 0.75**，冲刺 **≥ 0.82** |
| 绝对 CoreMark | **≥ 180** @100MHz，良好目标 **≥ 200**，冲刺 **≥ 220** |
| Runner 集成帧率 | 持续 **≥ 30fps**（单机平均 ≥45fps，整机 1% low ≥24fps） |
| CPU 资源上限 | LUT **≤ 12k** / FF **≤ 15k** / BRAM36 **≤ 12** / DSP **≤ 8** |
| 整机资源上限 | 各主要资源目标 **≤75%**，硬上限 **≤80%** |

### 0.2 对内硬门禁（做不到不准进下一阶段）

| 阶段 | 硬门禁 |
|---|---|
| C0 结束 | Hello+UART 上板成功；100MHz 收敛；CPU LUT\<8k |
| C1 结束 | CoreMark 可复现；IPC≥0.70 且 CM/MHz≥1.8；良好目标 IPC≥0.75 且 CM/MHz≥2.0 |
| C2 结束 | 核配置冻结打 tag；MMIO uncacheable 正确；资源不超上限 |

### 0.3 明确不做（避免拖垮 7020）

- 不上乱序 / 多发射 / 宽 ROB
- 不用 `Reg`/`RegInit(Vec)` 堆 8KB cache / 1024 BHT（会吃光 LUT/FF）
- 不把帧缓冲 / GPU / CNN 寄存器做成可缓存
- 不为冲 IPC 盲目加大 cache；超资源时 **优先保 DCache**

---

## 1. 现状诊断（改之前先看懂）

### 1.1 关键文件

| 模块 | 路径 | 现状要点 |
|---|---|---|
| 顶层 SoC | `ioid_chisel_vsrc/src/main/scala/soc.scala` | `ysyx_25020039`，AXI master/slave |
| Core | `.../core/core.scala` | IF-ID-EX-MEM-WB；前递；load-use stall；BPU 更新；误预测 `RegNext` 冲刷 |
| IFU | `.../core/ifu.scala` | 取指 FSM + BPU |
| IDU | `.../core/idu.scala` | 译码 / RF / fencei hold |
| EXU | `.../core/exu.scala` | ALU + **阻塞 DIV**；MUL 走 ALU |
| LSU | `.../core/lsu.scala` | AXI 访存，**无 DCache** |
| WBU | `.../core/wbu.scala` | 写 RF/CSR |
| ALU | `.../unit/alu.scala` | **组合 MUL**（`*`） |
| DIV | `.../unit/div.scala` | restoring，约 32 拍 |
| ICache | `.../unit/icache.scala` | 64×4way×32B，**Reg 阵列** |
| BPU | `.../unit/bpu.scala` | GShare BHT=1024 + RAS=16，**不预测 JALR** |
| 配置 | `.../config.scala` | `NPC_Config` / `SoC_Config` |
| 生成入口 | `.../top.scala` | `TopMain` / `TopMainSoC` |

### 1.2 上板前必须修的 P0

1. **ICache 用 Reg 实现** → FPGA 综合成海量 FF/LUT（生成 `ICache.sv` 可达数万行）  
2. **BHT=1024 用 Reg** → 同样吃资源  
3. **组合 MUL** → 100MHz 可能过，但余量差，且和前递叠在一起危险  
4. **无 DCache** → CoreMark/游戏数据全走总线，IPC 难稳到良好档  

### 1.3 P1（冲良好档需要）

1. 误预测 `mis_predict_r = RegNext(...)` 多 1 拍冲刷  
2. 不预测 JALR（无 BTB）  
3. Xbar 上 imem/dmem 串行仲裁（命中 cache 后影响变小，但仍建议 I/D 分口）  

---

## 2. 目标微架构（C2 冻结规格）

```
PC
 └─ IF  : ICache(BRAM) + BPU(GShare+RAS+BTB)
 └─ ID  : Decode + RegFile
 └─ EX  : ALU + MUL(2拍,DSP) + DIV(多拍) + BranchResolve
 └─ MEM : DCache(BRAM) / MMIO bypass → AXI
 └─ WB  : RF/CSR writeback
```

### 2.1 推荐参数（先小后大）

| 部件 | 起步（C0/C1） | 冲刺（资源有余再开） |
|---|---|---|
| ICache | **4KB**：32 set × 4 way × 32B，hit=1（SyncReadMem） | 8KB：64×4×32 |
| DCache | **4KB**：64 set × 2 way × 32B，写穿+写分配，hit=1 | 保持 4KB（优先稳） |
| BHT | **256** | 512 |
| RAS | **8** | 8~16 |
| BTB | **64** | 128 |
| MUL | **2 拍** | 2 拍 |
| DIV | 现有迭代 | 可选早期终止 |
| 主频 | 50M 验证 → **100M** | 100M（不盲目冲 120） |

### 2.2 地址与可缓存属性（上板后必须遵守）

| 区间 | 用途 | Cache |
|---|---|---|
| 指令/数据紧耦合 BRAM（若有） | 启动代码/小数据 | 可不经 D$ 或固定映射 |
| DDR 通用数据 | 堆/栈/游戏逻辑数据 | **D$ 可缓存** |
| `0x44B0_0000` DMA | 外设 | **强制 uncacheable** |
| `0x44C0_0000` CNN | 外设 | **强制 uncacheable** |
| `0x44D0_0000` HDMI | 外设 | **强制 uncacheable** |
| `0x44E0_0000` OV5640 | 外设 | **强制 uncacheable** |
| `0x44F0_0000` 3D GPU | 外设 | **强制 uncacheable** |
| `0x44F0_1000` UART/GPIO/Timer | 外设 | **强制 uncacheable** |
| 帧缓冲/深度/纹理 DDR 窗口 | GPU/显示 | **强制 uncacheable**（或非缓存窗口） |

> 规则：凡是“CPU 写寄存器立刻要硬件看见”或“硬件写内存 CPU 马上要读到”的区域，一律 bypass DCache。
>
> 指标优先级：整机稳定可演示 > GPU/CNN 正确且实时 > CPU 达到 CoreMark/MHz 门禁 > 继续追高 IPC。IPC 用于定位流水线瓶颈，不作为牺牲整机资源和时序的理由。

---

## 3. 改核实施计划（按提交粒度）

> 原则：**每步可仿真回归 → 再综合看资源/时序 → 再上板**。  
> 仿真优先用现有 `ioid` NPC/SoC 流程（`make menuconfig` 选 IOID）。

---

### 阶段 C0：FPGA 可综合骨架（先能上板）

**目标**：核在 7020 上 100MHz 跑 Hello/UART；资源进预算。

#### C0-1 ICache 改 BRAM（P0，最先做）

**改哪些文件**
- `unit/icache.scala`（主改）
- `core/core.scala`（若构造参数变化）
- 必要时 `config.scala` 增加 cache 参数

**怎么改**
1. 去掉 `RegInit(0.U.asTypeOf(Vec(set, ...)))` 大数据阵列  
2. 数据/tag/valid 改为 `SyncReadMem`（或分 bank 的 Mem）  
3. 接受 **命中 1 拍延迟**：
   - 周期 N：给地址，发起读  
   - 周期 N+1：拿到 data/tag，比较 hit，返回给 IFU  
4. 起步参数：`set=32, way=4, block=32` → 4KB  
5. refill 逻辑保持 burst/逐字填充；fencei 仍可按 set 清 valid  

**验收**
- [ ] NPC 仿真：原有指令测试 / 简单程序不过分回退  
- [ ] 生成 SV 后 ICache 不再是“巨型 Reg 展开”  
- [ ] Vivado 综合：ICache 主要映射到 **Block RAM**，不是满 LUT  

**常见坑**
- `SyncReadMem` 同地址写读冲突：明确 read-under-write 行为，refill 与 hit 互斥  
- IFU 假设同拍 hit：要改 IFU ready/valid 时序，允许 +1 latency  

#### C0-2 MUL 改 2 拍（DSP）

**改哪些文件**
- 新建 `unit/mul.scala`（建议）或在 `exu.scala` 内做
- `unit/alu.scala`：删除 `ALU_MUL*` 组合乘
- `core/exu.scala`：像 DIV 一样用 `mul_busy` 阻塞

**怎么改**
1. ALU 只保留加/逻辑/移位/比较  
2. MUL 单元：
   - 拍1：锁操作数，DSP 乘法  
   - 拍2：按 `MUL/MULH/MULHSU/MULHU` 选 63:32 或 31:0，拉高 `result_valid`  
3. flush/kill 时清 busy  

**验收**
- [ ] `mul/mulh` 相关测试通过  
- [ ] 综合报告出现 DSP 使用（或明确推断为 DSP）  
- [ ] 100MHz 时序明显好转（关键路径离开组合乘）  

#### C0-3 BPU 缩小并 Mem 化

**改哪些文件**
- `common/consts.scala` 中 `BPU_Config`
- `unit/bpu.scala`

**怎么改**
```scala
// 建议起步
BHT_SIZE = 256
RAS_SIZE = 8
```
1. BHT 尽量 `SyncReadMem(256, UInt(2.W))`（注意更新/预测端口）  
2. 若双口麻烦：BHT=256 用分布式 RAM 也可，先保证功能  
3. 保持：随指令传 `bp_index`；`bp_taken` 必须 `bp_valid` 门控  
4. 仍可不预测 JALR（BTB 放到 C1）  

**验收**
- [ ] 分支程序功能正确  
- [ ] 资源明显下降（相对 BHT=1024 Reg）  

#### C0-4 最小上板 SoC（核侧）

**目标硬件形态（阶段 C 上板）**

```
Zynq PS（先可仅作时钟/复位/JTAG 辅助，或不用 PS 跑程序）
        │
   FCLK 100M / 复位
        │
   ┌────▼──────────────────────────────┐
   │ ysyx_25020039 (PL RISC-V)         │
   │  CPU + I$ + (暂可无D$)            │
   │  AXI ↔ 互联                       │
   │   ├─ 指令/数据 BRAM               │
   │   ├─ UART                         │
   │   ├─ GPIO(LED)                    │
   │   └─ Timer                        │
   └───────────────────────────────────┘
```

**本阶段软件**
- 链接到 BRAM  
- `printf` 走 UART  
- LED 心跳  

**验收**
- [ ] 串口输出 `Hello`  
- [ ] LED 闪烁  
- [ ] 100MHz bitstream 稳定  

> C0 结束打 tag：`ioid-c0-fpga-skeleton`

---

### 阶段 C1：冲 IPC / CoreMark（良好档关键）

#### C1-1 增加 DCache（最重要）

**改哪些文件**
- 新建 `unit/dcache.scala`
- `core/lsu.scala`：CPU 侧先打 DCache，miss/bypass 再走 AXI
- `core/core.scala`：连接；load-use 与 forward 对接 D$ hit
- `config.scala`：D$ 参数；uncache 地址判断

**建议规格**
- 4KB，2-way，32B line  
- **写穿 + 写分配**（实现简单，先稳 IPC）  
- hit latency = 1  
- MMIO / 帧缓冲：**bypass**

**LSU 行为**
1. 若地址 uncacheable → 直接走现有 AXI FSM  
2. 若 cacheable：
   - load hit：1 拍后返回，可前递  
   - load miss：refill line，再返回  
   - store：写 cache + 写穿透到总线（或先写 cache 再写总线）  

**验收**
- [ ] 功能测试：load/store/跨行/fence 语义按你现有模型对齐  
- [ ] CoreMark 相对 C0 明显升 IPC  
- [ ] 综合：D$ 主要在 BRAM  

#### C1-2 前递与 load-use 对齐 D$

**改哪些文件**
- `core/core.scala` 的 `RsForward` / `RsStall`

**怎么改**
1. EX 阶段是 load 且数据未就绪 → stall（保持）  
2. MEM 阶段 D$ hit 且 `rvalid` → **允许前递给 ID/EX**，减少气泡  
3. 目标：常见 load-use 停顿 **≤1**  

**验收**
- [ ] 定向汇编用例：`lw; add rd, rs, ...` 气泡符合预期  
- [ ] Perf：load 相关 stall 下降  

#### C1-3 缩短误预测惩罚

**改哪些文件**
- `core/core.scala`（redirect / flush）
- `core/ifu.scala`（同拍接受 correct_pc）

**怎么改**
1. 去掉“误预测先 `RegNext` 再冲刷”的额外一拍（或仅保留必要打拍，但 redirect 尽快）  
2. EX 解析分支当拍产生 `correct_pc` + `flush`  
3. 目标惩罚：**≤3 拍**（含流水已取错误路）  

**验收**
- [ ] 分支测例功能正确（无死锁、无错路径提交）  
- [ ] `EVENT_BPU_MISPRED` 次数合理；IPC 上升  

#### C1-4 轻量 BTB（预测 JALR）

**改哪些文件**
- `unit/bpu.scala` 或新建 `unit/btb.scala`
- `core/ifu.scala` / `core/core.scala` 更新接口

**怎么改**
1. 64 项：tag(PC) + target  
2. 仅对 `jalr`（非 ret）预测；ret 继续走 RAS  
3. EX 更新 BTB  

**验收**
- [ ] CoreMark IPC 再抬一截  
- [ ] 资源仍在上限内  

#### C1-5 CoreMark 上板测量

**步骤**
1. 移植 CoreMark（riscv32-unknown-elf-gcc）  
2. 迭代次数保证运行 **>10s**  
3. UART 打印：`Iterations / total ticks / CoreMark/MHz / IPC`  
4. 固定编译选项，写入本文档附录（见 §8）  

**门禁**
- [ ] IPC ≥ 0.70；良好目标 ≥0.75  
- [ ] CM/MHz ≥ 1.8；良好目标 ≥2.0  
- [ ] 冲刺记录是否达到 IPC 0.82 / CM/MHz 2.2（达到则写入答辩主数据）  
- [ ] 板上正式运行 ≥10 秒，两组规定 seed 的 CRC 全部通过；记录编译器、参数、代码/数据存储位置和内存/缓存频率比  

> C1 结束打 tag：`ioid-c1-coremark-good`

---

### 阶段 C2：冻结核 + 为系统集成做接口

#### C2-1 配置冻结

在 `config.scala`（或单独 `FpgaConfig`）固定：

```text
FREQ           = 100MHz
ICACHE         = 4KB (or 8KB if util ok)
DCACHE         = 4KB 2way WT+WA
BHT            = 256/512
RAS            = 8
BTB            = 64/128
MUL_LATENCY    = 2
DIV            = iterative
```

#### C2-2 总线与外设窗口

1. CPU 对外保持 **AXI4 master**（可继续 `ysyx_25020039` 接口）  
2. 上板最小系统用 AXI Interconnect / SmartConnect  
3. 实现 `is_mmio(addr)` / `is_uncacheable(addr)` 统一函数，LSU/D$ 共用  

#### C2-3 中断

1. 外部中断接 Timer / GPU done / CNN done（按集成阶段加）  
2. 现阶段保证：中断能进 trap，不破坏流水 flush 语义  

#### C2-4 资源/时序终检

综合后填写：

| 项 | 结果 | 上限 |
|---|---|---|
| LUT | ____ | ≤12k（CPU） |
| FF | ____ | — |
| BRAM36 | ____ | ≤12 |
| DSP | ____ | ≤8 |
| WNS @100M | ____ | ≥0 |

超限砍法（按顺序）：
1. I$ 8KB → 4KB  
2. BHT 512 → 256  
3. BTB 128 → 64  
4. **不要先砍 D$**  

> C2 结束打 tag：`ioid-c2-freeze` —— 之后 GPU/CNN 集成默认不改流水线。

---

## 4. 上板流程（领航者 Zynq-7020）如何做

> 下面按“第一次把 RISC-V 核弄上板”写。HDMI/摄像头等复用你毕设阶段 A/B，这里聚焦 **核 + 最小外设**。

### 4.1 目录与产物建议

```text
npc/
  ioid_chisel_vsrc/          # Chisel 源码
  ioid_fpga/                 # （建议新建）上板工程脚本与包装
    rtl/                     # 导出的 SV + 包装层
    constr/                  # XDC
    bd/                      # 可选：记录 BD 配置
    sw/                      # 裸机/BRAM 程序、链接脚本
    scripts/                 # Vivado tcl
```

### 4.2 步骤总览

```
① Chisel 生成 SV
② 包一层 FPGA top（时钟/复位/UART/GPIO）
③ Vivado 建工程 / BD（PS 提供时钟或纯 PL 晶振）
④ 加 XDC（时钟 + UART + LED）
⑤ synth → impl → bitstream
⑥ 生成 MEM/BRAM 初始化或通过 JTAG 下载程序
⑦ 串口验证 Hello → CoreMark
⑧ 再挂 HDMI/GPU/CNN（后续阶段）
```

### 4.3 ① 生成 Verilog

在 `npc/ioid_chisel_vsrc`：

```bash
# 视你现有习惯：sbt 或 mill
sbt "runMain TopMainSoC"
# 或 NPC 配置
sbt "runMain TopMain"
```

要点：
- 上板用 **SoC 风格配置**（`ysyxsoc=true` 那套复位/入口若与板级地址不一致，要改成板级地址映射）  
- 去掉仅仿真 DPI：`useDPIC=false` 的配置生成一版（或黑盒 stub）  
- 确认顶层模块名：`ysyx_25020039`（可再包 `riscv_fpga_top`）  

**上板专用配置建议**（新增 `Fpga_Config`）：

```scala
object Fpga_Config {
  def apply(): CoreConfig =
    new CoreConfig(
      xlen = 32,
      useDPIC = false,    // 无 DPI
      ysyxsoc = false,
      npc = false,
      statistics = false  // 上板可关；测 IPC 时再开仿真统计
    )
}
```

### 4.4 ② FPGA 顶层包装（必须）

Chisel 核是 AXI 口，板上还要：

| 信号 | 来源 |
|---|---|
| `clock` | PS FCLK_CLK0=100M，或板载 50M → MMCM 倍到 100M |
| `reset` | PS 复位 或 按键复位（注意高有效，和核一致） |
| UART TX/RX | 接到领航者 USB-UART 引脚 |
| LED | GPIO |
| 程序存储 | BRAM Controller 或本地 `SyncReadMem` ROM/RAM |

**推荐第一次上板拓扑（最稳）**

1. Vivado BD：  
   - Zynq PS：只开 **FCLK0=100M** + 复位  
   - AXI Interconnect  
   - Block Memory（指令/数据，或双端口拆分）  
   - AXI UARTLite / 你自研 UART  
   - AXI GPIO  
2. `ysyx_25020039` 作为 **AXI Master** 挂到 Interconnect  
3. 核的 `io_slave` 可悬空按规范拉默认（若暂时不用）  

> 若你更熟纯 PL：不用 PS 跑 Linux，只把 PS 当时钟源，完全可以。

### 4.5 ③ 地址映射（最小系统）

| 基址 | 大小 | 从设备 |
|---|---|---|
| `0x8000_0000` | 64KB~256KB | ITCM/指令 BRAM（按你链接脚本） |
| `0x8000_0000` 或分离 `0x8010_0000` | 同左 | DTCM/数据 BRAM（可与指令同 BRAM 分区） |
| `0x44F0_1000` | 4KB | UART |
| `0x44F0_2000` | 4KB | GPIO |
| `0x44F0_3000` | 4KB | Timer |

> 地址可按你现有习惯改，但 **链接脚本、启动代码、译码器必须一致**。  
> 复位 PC：与 BRAM 程序入口一致（ioid 里 NPC 默认 `0x8000_0000` 较适合板级 BRAM）。

### 4.6 ④ XDC 约束要点

1. **时钟**
```tcl
create_clock -name clk_100m -period 10.000 [get_ports clock]
# 若有 MMCM：
# create_generated_clock ...
```

2. **复位**：false path 或异步约束按实现选择  

3. **UART/LED 引脚**：以领航者原理图/官方约束为准（不要抄错 bank）  

4. **bitstream**
```tcl
set_property BITSTREAM.GENERAL.COMPRESS TRUE [current_design]
```

5. 第一次时序紧：先 **50MHz** 出画面/出串口，再切 100M  

### 4.7 ⑤ 综合实现检查清单

每次出 bit 前记录：

- [ ] Utilization：LUT/BRAM/DSP（CPU only + 全设计）  
- [ ] Timing Summary：WNS/TNS @ 目标频率  
- [ ] 关键路径是否在 MUL/ALU/Cache hit 比较器/转发  
- [ ] 是否误把大阵列综合成 LUT（搜 “Distributed RAM / LUT as memory” 异常偏高）  

### 4.8 ⑥ 程序如何进 BRAM

三种常用方式（选一种固化）：

**方式 A：Vivado `updatemem`（推荐迭代）**
1. 链接生成 `.bin` / `.mem`  
2. `updatemem` 写入 bitstream 的 BRAM  
3. 重新下载 bit  

**方式 B：生成时 `$readmemh`**
- 仅适合仿真/早期；上板工程改程序要重综合，慢  

**方式 C：UART/JTAG 引导加载**
- 后期再做；最小系统阶段不必  

启动代码最小逻辑：
1. 设 `sp`  
2. 清 `.bss`，拷 `.data`（若需要）  
3. 跳 `main`  
4. `main` 里 UART 打印  

### 4.9 ⑦ 上板调试顺序（很重要）

1. **空核心跳**：GPIO 翻转（可用定时器或软件 loop）——确认时钟复位  
2. **UART Hello**  
3. **读回指令 BRAM**（ILA 看 IFU PC 是否在跑）  
4. **简单算术/分支程序**  
5. **CoreMark**  
6. 再接 HDMI 控制寄存器写（阶段 C6）  

**ILA 建议探针**
- `pc` / `ifu.state` / `exu.valid`  
- `dmem.arvalid/rvalid` / `imem.*`  
- `flush` / `mis_predict`  
- UART tx 移位状态（可选）  

### 4.10 ⑧ 频率策略

| 步骤 | 频率 | 目的 |
|---|---|---|
| 首次出串口 | 50MHz | 降时序难度 |
| C0 验收 | 100MHz | 对齐系统时钟 |
| 集成 GPU/CNN 后 | 仍 100MHz | 不够再优关键路径，不先提频 |

---

## 5. 仿真与回归（改一步跑一步）

### 5.1 本地仿真（改核主战场）

```bash
cd npc
make menuconfig   # 选择 CONFIG_IOID_CHISEL / SOC 版本按需要
make              # 按你现有流程
```

建议固定一套 smoke：
- [ ] 基础指令（AM/CPU test）  
- [ ] load/store  
- [ ] 分支/jal/jalr/ret  
- [ ] mul/div  
- [ ] fencei（若上板要用）  
- [ ] CoreMark（仿真可短迭代，上板长迭代）  

### 5.2 性能计数

`statistics=true` 时用现有 `PerfMonitor`：
- IPC = `EVENT_EXU_COMP / cycles`
- BPU hit、I$ miss、LSU latency  

上板若无 DPI：用 **CSR cycle/instret**（若已实现）或 Timer 墙钟估算 CoreMark。

---

## 6. 与毕设大系统的衔接（核侧只做这些）

| 毕设阶段 | 核侧状态 |
|---|---|
| A HDMI | 不依赖核 |
| B OV5640 | 不依赖核 |
| **C CPU** | 执行本文 C0→C1→C2 |
| D GPU 游戏 | 核冻结；驱动写 GPU MMIO；帧缓冲 uncache |
| E CNN | 核发启动/轮询或中断；权重区 uncache |
| F 集成 | 只改地址图/中断/仲裁，不改流水线 |
| G 可选 | 再考虑加大 cache / 提频 |

游戏流畅依赖 **GPU+DDR**；核的任务是：
- 100MHz 稳定  
- 游戏逻辑与提交命令足够快（通常远小于 5ms/帧）  
- CoreMark 达到良好档数字用于答辩  

---

## 7. 详细任务清单（可直接勾选）

### C0
- [ ] C0-1 ICache → SyncReadMem（4KB）+ IFU 1-beat hit  
- [ ] C0-2 MUL 2 拍，ALU 去组合乘  
- [ ] C0-3 BHT≤256，RAS=8，资源下降  
- [ ] C0-4 `Fpga_Config`（无 DPI）  
- [ ] C0-5 Vivado 最小系统（clk/reset/BRAM/UART/GPIO）  
- [ ] C0-6 XDC + 50M 出 Hello  
- [ ] C0-7 提到 100M，WNS≥0  
- [ ] C0-8 记录 Utilization，打 tag `ioid-c0-fpga-skeleton`

### C1
- [ ] C1-1 DCache 4KB + MMIO bypass  
- [ ] C1-2 load-use/前递对齐 D$  
- [ ] C1-3 误预测惩罚缩短  
- [ ] C1-4 BTB64 预测 JALR  
- [ ] C1-5 CoreMark 上板（>10s）  
- [ ] C1-6 达到冻结门禁 IPC≥0.70、CM/MHz≥1.8  
- [ ] C1-7 良好目标/冲刺数据记录（0.75/2.0；0.82/2.2）  
- [ ] C1-8 tag `ioid-c1-coremark-good`

### C2
- [ ] C2-1 参数冻结进配置对象  
- [ ] C2-2 uncache 地址表与文档一致  
- [ ] C2-3 资源终检 ≤ 上限  
- [ ] C2-4 tag `ioid-c2-freeze`  
- [ ] C2-5 答辩数据表定稿（见 §8）  

---

## 8. 答辩数据记录模板（出数后填）

### 8.1 CoreMark

| 项 | 数值 |
|---|---|
| 编译器 / 优化等级 | |
| 主频 | 100MHz |
| Iterations | |
| Total ticks/cycles | |
| IPC | |
| CoreMark/MHz | |
| 绝对分 | |
| 是否开 D$ | 是/否 |
| I$/D$ 容量 | |

### 8.2 资源

| 项 | CPU only | 全系统 |
|---|---|---|
| LUT | | |
| FF | | |
| BRAM36 | | |
| DSP | | |
| WNS | | |

### 8.3 对比（建议 PPT 必放）

| 版本 | IPC | CM/MHz | LUT | 备注 |
|---|---|---|---|---|
| 改前 ioid（仿真） | | | | Reg I$，无 D$ |
| C0 | | | | BRAM I$ + MUL2 |
| C1 | | | | +D$ +BTB |
| C2 冻结 | | | | 答辩主数据 |

---

## 9. 风险与兜底

| 风险 | 表现 | 兜底 |
|---|---|---|
| I$ 仍综合成 LUT | Utilization 爆炸 | 检查是否真用 SyncReadMem；加 `(* ram_style = "block" *)`（SV 包装） |
| 100M 不收敛 | WNS\<0 | 先 50M；MUL/Cache 再打拍；减小 way |
| CoreMark/MHz \<1.8 | 核性能不够 | 确认 D$ 命中；减少 Xbar 串行；查 mispredict 惩罚；固定并披露 `-O2/-O3` 编译参数 |
| D$ 与外设一致性 bug | GPU/UART 配了不生效 | 扩大 uncache 窗口；MMIO 完全 bypass |
| 上板死机 | 无串口/PC 不动 | ILA 看复位释放、PC、imem 是否回数据 |
| 资源撞车 CNN/GPU | 整机放不下 | 按 §3 C2 砍法缩 I$/BPU；CNN PE 16→8 是系统侧兜底 |

---

## 10. 推荐执行节奏（串行）

| 周次（建议） | 内容 |
|---|---|
| 第 1 周 | C0-1~C0-3 仿真打通 |
| 第 2 周 | C0 上板 Hello @50M→100M |
| 第 3 周 | C1-1 DCache 仿真+上板 |
| 第 4 周 | C1-3/C1-4 + CoreMark 达标 |
| 第 5 周 | C2 冻结 + 文档/数据填完，进入 GPU 集成 |

时间可按实际拉长；**门禁不能跳**。

---

## 11. 一句话版本（防迷路）

> 先把 ioid 改成 **BRAM I$/D$ + 2 拍 MUL + 小 BPU/BTB** 的 100MHz 五级核，用 **DCache 和不拖泥带水的冲刷** 打到良好档 CoreMark；用 **资源上限** 给 GPU/CNN 留命；上板按 **时钟→BRAM→UART→ILA→CoreMark** 的顺序来，核冻结后再挂显示和手势系统。

---

## 附录 A. 关键代码锚点（改时搜索）

- 组合乘：`unit/alu.scala` 中 `ALU_MUL*`  
- I$ Reg 阵列：`unit/icache.scala` 中 `RegInit(...Vec(set...))`  
- 误预测多一拍：`core/core.scala` 中 `mis_predict_r` / `is_bp_flush`  
- load-use：`core/core.scala` 中 `RsStall`  
- BHT 大小：`common/consts.scala` 中 `BPU_Config`  
- 生成入口：`top.scala` 中 `TopMain` / `TopMainSoC`  

## 附录 B. 编译选项建议（CoreMark）

定稿后不要每周换：

```text
riscv32-unknown-elf-gcc -O2 -march=rv32im -mabi=ilp32
# 或 rv32imc（若你核支持 C 扩展；ioid 若未实现 C，勿开）
```

把最终命令行原样贴进 §8.1。

## 附录 C. 与「绝对分 ≥200」的关系

- 良好档主承诺：**CM/MHz ≥1.8**  
- 冲刺：**≥2.0 → @100MHz 绝对分 ≥200**  
- 若冲刺未到：答辩报 1.8+ 对比提升曲线，仍属良好；游戏帧率与系统演示不受影响  

---

**文档结束。** 下一步建议直接从 **C0-1 ICache BRAM 化** 开工。
