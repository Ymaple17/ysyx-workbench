VERILATOR = verilator
TOP_MODULE = top

ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig' first)
endif

include .config
include Makefile.diff

VSRC_IOID_CHISEL = $(shell find ioid_chisel_vsrc -maxdepth 1 -name "*.v" -o -name "*.sv")
CSRC_IOID_CHISEL = $(shell find ioid_chisel_csrc -name "*.cpp" -o -name "*.c")#ioid_chisel
CSRC_IOID_CHISEL_SOC = $(filter-out %/ref_so.cpp %/ref.cpp, $(shell find ioid_chisel_soc_csrc -name "*.cpp" -o -name "*.c"))#ioid_chisel_soc

CONFIG_IOID_CHISEL_VAL := $(shell grep -w CONFIG_IOID_CHISEL .config | cut -d'=' -f2)
CONFIG_IOID_CHISEL_SOC_VAL := $(shell grep -w CONFIG_IOID_CHISEL_SOC .config | cut -d'=' -f2)
CONFIG_IOID_CHISEL_NPC_VAL := $(shell grep -w CONFIG_IOID_CHISEL_NPC .config | cut -d'=' -f2)
CONFIG_NVBOARD_VAL := $(shell grep CONFIG_NVBOARD .config | cut -d'=' -f2)

VSRC_MINIRV_PERIP = $(shell find ../ysyxSoC/perip -name "*.v" -o -name "*.sv")
VSRC_SOC_TOP = ../ysyxSoC/build/ysyxSoCFull.v

VERILATOR_FLAGS =

ifeq ($(CONFIG_IOID_CHISEL_VAL),y)
  ifeq ($(CONFIG_IOID_CHISEL_SOC_VAL),y)
    VSRC = $(VSRC_IOID_CHISEL) $(VSRC_MINIRV_PERIP) $(VSRC_SOC_TOP)
    CSRC = $(CSRC_IOID_CHISEL_SOC)
    CHISEL_WORK_DIR := ioid_chisel_vsrc
    CHISEL_BUILD_TOOL := mill
#     CHISEL_BUILD_TOOL := none
    CHISEL_MAIN := TopMainSoC
    TOP_MODULE = ysyxSoCFull
    VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
    CFLAGS += -std=c++17
    $(info [INFO] Using ioid_chisel SOC Mode)
  else
    VSRC = $(VSRC_IOID_CHISEL)
    CSRC = $(CSRC_IOID_CHISEL)
    CHISEL_WORK_DIR := ioid_chisel_vsrc
    CHISEL_BUILD_TOOL := mill
    CHISEL_MAIN := TopMain
#   CHISEL_BUILD_TOOL := none
    TOP_MODULE = ysyx_25020039
    $(info [INFO] Using ioid_chisel NPC Mode)
  endif
endif

C_INCLUDE_DIRS =

ifeq ($(CONFIG_IOID_CHISEL_SOC_VAL),y)
  C_INCLUDE_DIRS += \
    -Iioid_chisel_soc_csrc \
    -I$(NPC_HOME)/ioid_chisel_soc_csrc/include \
    -I$(NPC_HOME)/include
else ifeq ($(CONFIG_IOID_CHISEL_VAL),y)
  C_INCLUDE_DIRS += \
    -Iioid_chisel_csrc \
    -I$(NPC_HOME)/ioid_chisel_csrc/include \
    -I$(NPC_HOME)/include
endif

V_INCLUDE_DIRS =

ifeq ($(CONFIG_IOID_CHISEL_SOC_VAL),y)
  V_INCLUDE_DIRS += -Iioid_chisel_vsrc \
                    -I../ortl \
                    -I../ysyxSoC/perip/uart16550/rtl \
                    -I../ysyxSoC/perip/spi/rtl
else ifeq ($(CONFIG_IOID_CHISEL_VAL),y)
  V_INCLUDE_DIRS += -Iioid_chisel_vsrc
endif

INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
PROGRAM ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin
# PROGRAM ?= ../am-kernels/kernels/char-test/char-test.bin

CFLAGS = -Wall -std=c++11 -g
CXXFLAGS = $(CFLAGS)
LDFLAGS_BASE = -lreadline -lSDL2 -ldl
VERILATOR_FLAGS += -Wno-WIDTHEXPAND -Wno-PINMISSING
ifeq ($(CONFIG_NVBOARD_VAL),y)
  ifndef NVBOARD_HOME
    $(error NVBOARD_HOME is not set)
  endif
  NVBOARD_USR_INC_PATH = -I$(NVBOARD_HOME)/usr/include
  NVBOARD_LIB = -L$(NVBOARD_HOME)/build -l:nvboard.a -lSDL2 -lSDL2_image -lSDL2_ttf

  CFLAGS += -DCONFIG_NVBOARD -DNVBOARD $(NVBOARD_USR_INC_PATH)
  NXDC_FILES = constr/top.nxdc
  SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
  $(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@
  
  include $(NVBOARD_HOME)/scripts/nvboard.mk
  CSRC += $(SRC_AUTO_BIND)
  LDFLAGS_BASE += $(LDFLAGS) $(NVBOARD_LIB)
endif

ifdef CONFIG_WAVEFORM
  CFLAGS += -DENABLE_WAVEFORM
  VERILATOR_FLAGS += --trace-fst
else
  $(info [INFO] CONFIG_WAVEFORM is disabled)
endif

ifdef CONFIG_DIFFTEST
  NEMU_LIB_ABS_PATH = $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
  ifeq (,$(wildcard $(NEMU_LIB_ABS_PATH)))
    $(error NEMU共享库不存在: $(NEMU_LIB_ABS_PATH)，请先编译NEMU)
  endif
  CFLAGS += -DDIFFTEST \
            -DDIFFTEST_LIB=\"$(NEMU_LIB_ABS_PATH)\"
  ifdef CONFIG_ENABLE_MEMORY_CHECK
    CFLAGS += -DENABLE_MEMORY_CHECK
    $(info [INFO] Memory check in difftest enabled)
  endif
  LDFLAGS_DIFFTEST = $(NEMU_LIB_ABS_PATH)
  $(info [INFO] DiffTest enabled，使用绝对路径链接: $(NEMU_LIB_ABS_PATH))
else
  LDFLAGS_DIFFTEST =
  $(info [INFO] CONFIG_DIFFTEST is disabled)
endif

ifdef CONFIG_ITRACE
  LIBCAPSTONE = tools/capstone/libcapstone.so.6
  CFLAGS += -DCONFIG_ITRACE -Itools/capstone/include
  $(info [INFO] Instruction tracing enabled)
  $(OBJ_DIR)/csrc/disasm.o: $(LIBCAPSTONE)
  $(LIBCAPSTONE):
	$(info [BUILD] Compiling capstone library...)
	$(MAKE) -C tools/capstone  
else
  $(info [INFO] CONFIG_ITRACE is disabled)
endif

chisel-gen:
ifdef CHISEL_WORK_DIR
	$(info [CHISEL] Check build tool: $(CHISEL_BUILD_TOOL))
	cd $(CHISEL_WORK_DIR) && \
	if [ "$(CHISEL_BUILD_TOOL)" = "mill" ]; then \
	    ./mill -i mychisel.runMain $(CHISEL_MAIN); \
	elif [ "$(CHISEL_BUILD_TOOL)" = "sbt" ]; then \
	    sbt run; \
	else \
	    $(info [CHISEL]  pipeline_chisel: skip all build commands, SV/Verilog files are ready!) \
	    true; \
	fi
else
	$(info [CHISEL] Not a Chisel configuration, skip SV generation)
endif

CHISEL_MAIN ?= TopMain

chisel-clean:
ifdef CHISEL_WORK_DIR
	$(info [CHISEL] Check build tool: $(CHISEL_BUILD_TOOL))
	cd $(CHISEL_WORK_DIR) && \
	if [ "$(CHISEL_BUILD_TOOL)" = "mill" ]; then \
	    mill -i clean; \
	elif [ "$(CHISEL_BUILD_TOOL)" = "sbt" ]; then \
	    sbt clean; \
	else \
	    $(info [CHISEL]  pipeline_chisel: skip clean commands, no build artifacts to clean!) \
	    true; \
	fi
else
	$(info [CHISEL] Not a Chisel configuration, skip Chisel clean)
endif

$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR))

CONFIG_HASH := $(shell md5sum .config | cut -d' ' -f1)
TOP_STAMP = $(OBJ_DIR)/.build_stamp
$(TOP_STAMP): .config
	@rm -rf $(OBJ_DIR)
	@mkdir -p $(OBJ_DIR)
	@echo "$(TOP_MODULE) $(CONFIG_HASH)" > $@

all: chisel-gen $(EXEC)
	$(info [SUCCESS] Build completed: $(EXEC))

$(EXEC): $(VSRC) $(CSRC) $(NVBOARD_ARCHIVE) $(TOP_STAMP)
	$(info [BUILD] Compiling Verilator simulation with $(VSRC))
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) \
		$(INCLUDE_DIRS) \
		$(VERILATOR_FLAGS) \
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "$(LDFLAGS_BASE) $(LDFLAGS_DIFFTEST)"

sim: all $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM))
	$(EXEC) $(PROGRAM) $(ARGS) 

run:
	$(MAKE) sim 

$(PROGRAM):
	@mkdir -p $(@D)
	@if [ ! -f "$@" ]; then \
		touch "$@"; \
		echo "[INFO] Created empty program: $@"; \
	fi

clean: chisel-clean
	$(info [CLEAN] Removing build artifacts...)
	rm -rf $(OBJ_DIR)
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif

sta:
	$(MAKE) chisel-gen
	rm -rf ../yosys-sta/result/ysyx_25020039-500MHz
	$(MAKE) -C ../yosys-sta sta DESIGN=ysyx_25020039 RTL_FILES="$(abspath $(filter-out %/SRAM.sv %/DPI_Mem.v, $(VSRC_IOID_CHISEL)))"
	cat ../yosys-sta/result/ysyx_25020039-500MHz/synth_stat.txt

#IMG ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.elf
# IMG ?= ../rt-thread-am/bsp/abstract-machine/build/rtthread-riscv32e-npc.elf
IMG_ELF ?= ../am-kernels/benchmarks/microbench/build/microbench-riscv32e-npc.elf
IMG_BIN ?= ../am-kernels/benchmarks/microbench/build/microbench-riscv32e-npc.bin


mainargs ?= test
MICROBENCH_DIR = ../am-kernels/benchmarks/microbench

.PHONY: build-microbench

build-microbench:
	@echo "[BUILD] Building microbench with ARCH=riscv32e-npc mainargs=$(mainargs)..."
	@$(MAKE) -C $(MICROBENCH_DIR) ARCH=riscv32e-npc mainargs=$(mainargs) insert-arg

iverilog: chisel-gen build-microbench
	@echo "[BUILD] Compiling with Icarus Verilog..."
	@mkdir -p $(OBJ_DIR)
	@echo "[INFO] Converting $(IMG_BIN) to mem.hex (byte-wise)..."
	@hexdump -v -e '1/1 "%02x\n"' $(IMG_BIN) > mem.hex
	iverilog -g2012 -D__ICARUS__ -D MAINARGS=\"$(mainargs)\" -o $(OBJ_DIR)/sim.vvp tb_top.sv $(VSRC_IOID_CHISEL)
	@echo "[RUN] Running simulation with mainargs=$(mainargs)..."
	vvp $(OBJ_DIR)/sim.vvp $(mainargs)

NETLIST_FILE = ../yosys-sta/result/ysyx_25020039-500MHz/ysyx_25020039.netlist.syn.v
CELLS_FILE = ../yosys-sta/nangate45/sim/cells.v
IVERILOG_BIN_NETLIST = $(OBJ_DIR)/sim_netlist.vvp

$(NETLIST_FILE): chisel-gen
	@echo "[SYNTH] Generating Netlist..."
	@mkdir -p $(OBJ_DIR)
	@python3 scripts/filter_sram.py $(CHISEL_WORK_DIR)/ysyx_25020039.sv $(OBJ_DIR)/ysyx_25020039_no_sram.sv $(OBJ_DIR)/SRAM.sv
	@rm -rf ../yosys-sta/result/ysyx_25020039-500MHz
	$(MAKE) -C ../yosys-sta syn DESIGN=ysyx_25020039 PDK=nangate45 RTL_FILES="$(abspath $(filter-out %/SRAM.sv %/DPI_Mem.v %/SimUART.v %/ysyx_25020039.sv, $(VSRC_IOID_CHISEL))) $(abspath $(OBJ_DIR)/ysyx_25020039_no_sram.sv) $(abspath scripts/blackbox_sim.v)"

$(IVERILOG_BIN_NETLIST): $(NETLIST_FILE) tb_top.sv
	@echo "[BUILD] Compiling netlist with Icarus Verilog..."
	@iverilog -g2012 -D__ICARUS__ -o $@ \
		tb_top.sv \
		$(NETLIST_FILE) \
		$(CELLS_FILE) \
		$(OBJ_DIR)/SRAM.sv \
		$(CHISEL_WORK_DIR)/DPI_Mem.v \
		$(filter-out %/ysyx_25020039.sv %/SRAM.sv %/DPI_Mem.v, $(VSRC_IOID_CHISEL))

sim-netlist: $(IVERILOG_BIN_NETLIST)
	@echo "[RUN] Running netlist simulation..."
	@riscv64-linux-gnu-objcopy -O verilog --adjust-vma=-0x80000000 $(IMG) mem.hex
	@vvp -n $<

include Makefile.lib
include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run chisel-gen chisel-clean clean sta