VERILATOR = verilator
TOP_MODULE = top

ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig' first)
endif

include .config
include Makefile.diff

CHISEL_WORK_DIR := pipeline_chisel_vsrc

VSRC_CACHE_DIR     = vsrc
VSRC_CACHE_NPC_DIR = $(VSRC_CACHE_DIR)/npc
VSRC_CACHE_SOC_DIR = $(VSRC_CACHE_DIR)/soc
VSRC_CACHE_NPC     = $(VSRC_CACHE_NPC_DIR)/ysyx_25020039.sv
VSRC_CACHE_SOC     = $(VSRC_CACHE_SOC_DIR)/ysyx_25020039.sv
VSRC_CACHE_NETLIST = $(VSRC_CACHE_NPC_DIR)/ysyx_25020039.netlist.syn.v
VSRC_NPC_MODELS    = $(VSRC_CACHE_NPC_DIR)/ysyx_25020039_sim_models.sv

CSRC_PIPELINE_CHISEL = $(shell find pipeline_chisel_csrc -name "*.cpp" -o -name "*.c")
CSRC_PIPELINE_CHISEL_SOC = $(filter-out %/ref_so.cpp %/ref.cpp, $(shell find pipeline_chisel_soc_csrc -name "*.cpp" -o -name "*.c"))

ifndef CONFIG_ITRACE
CSRC_PIPELINE_CHISEL := $(filter-out %/disasm.cpp %/itrace.cpp, $(CSRC_PIPELINE_CHISEL))
CSRC_PIPELINE_CHISEL_SOC := $(filter-out %/disasm.cpp %/itrace.cpp, $(CSRC_PIPELINE_CHISEL_SOC))
endif

CONFIG_PIPELINE_CHISEL_VAL := $(shell grep -w CONFIG_PIPELINE_CHISEL .config | cut -d'=' -f2)
CONFIG_PIPELINE_CHISEL_SOC_VAL := $(shell grep -w CONFIG_PIPELINE_CHISEL_SOC .config | cut -d'=' -f2)
CONFIG_PIPELINE_CHISEL_NPC_VAL := $(shell grep -w CONFIG_PIPELINE_CHISEL_NPC .config | cut -d'=' -f2)
CONFIG_NVBOARD_VAL := $(shell grep CONFIG_NVBOARD .config | cut -d'=' -f2)

VSRC_MINIRV_PERIP = $(shell find ../ysyxSoC/perip -name "*.v" -o -name "*.sv")
VSRC_SOC_TOP = ../ysyxSoC/build/ysyxSoCFull.v

VERILATOR_FLAGS =

ifeq ($(CONFIG_PIPELINE_CHISEL_VAL),y)
  ifeq ($(CONFIG_PIPELINE_CHISEL_SOC_VAL),y)
    VSRC = $(shell find $(VSRC_CACHE_SOC_DIR) -name "*.v" -o -name "*.sv" 2>/dev/null) \
           $(VSRC_MINIRV_PERIP) $(VSRC_SOC_TOP)
    CSRC = $(CSRC_PIPELINE_CHISEL_SOC)
    CHISEL_BUILD_TOOL := mill
    CHISEL_MAIN := TopMainSoC
    TOP_MODULE = ysyxSoCFull
    VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
    $(info [INFO] Using pipeline_chisel SOC Mode)
  else
    VSRC = $(shell find $(VSRC_CACHE_NPC_DIR) -name "*.v" -o -name "*.sv" 2>/dev/null)
    CSRC = $(CSRC_PIPELINE_CHISEL)
    CHISEL_BUILD_TOOL := mill
    CHISEL_MAIN := TopMain
    TOP_MODULE = ysyx_25020039
    $(info [INFO] Using pipeline_chisel NPC Mode)
  endif
endif

C_INCLUDE_DIRS =

ifeq ($(CONFIG_PIPELINE_CHISEL_SOC_VAL),y)
  C_INCLUDE_DIRS += \
    -Ipipeline_chisel_soc_csrc \
    -I$(NPC_HOME)/pipeline_chisel_soc_csrc/include \
    -I$(NPC_HOME)/include
else ifeq ($(CONFIG_PIPELINE_CHISEL_VAL),y)
  C_INCLUDE_DIRS += \
    -Ipipeline_chisel_csrc \
    -I$(NPC_HOME)/pipeline_chisel_csrc/include \
    -I$(NPC_HOME)/include
endif

V_INCLUDE_DIRS =

ifeq ($(CONFIG_PIPELINE_CHISEL_SOC_VAL),y)
  V_INCLUDE_DIRS += -I$(VSRC_CACHE_SOC_DIR) \
                    -I../ortl \
                    -I../ysyxSoC/perip/uart16550/rtl \
                    -I../ysyxSoC/perip/spi/rtl
else ifeq ($(CONFIG_PIPELINE_CHISEL_VAL),y)
  V_INCLUDE_DIRS += -I$(VSRC_CACHE_NPC_DIR)
endif

INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
PROGRAM ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin
# PROGRAM ?= ../am-kernels/kernels/char-test/char-test.bin

CFLAGS = -Wall -std=c++17 -g
CXXFLAGS = $(CFLAGS)
LDFLAGS_BASE = -lreadline -lSDL2 -ldl
VERILATOR_FLAGS += -Wno-WIDTHEXPAND -Wno-PINMISSING --public-flat-rw
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

# -----------------------------------------------------------------------
# Local RTL regeneration (requires mill; NOT needed by CI)
#   make npc   -> regenerate NPC-mode RTL into vsrc/npc  (PC reset 0x80000000)
#   make soc   -> regenerate SoC-mode RTL into vsrc/soc  (PC reset 0x30000000)
# -----------------------------------------------------------------------

npc:
	@mkdir -p $(VSRC_CACHE_NPC_DIR)
	cd $(CHISEL_WORK_DIR) && USE_DPIC=false ./mill -i mychisel.runMain TopMain
	@cp $(CHISEL_WORK_DIR)/ysyx_25020039.sv $(VSRC_CACHE_NPC_DIR)/
	python3 scripts/extract_sim_models.py $(VSRC_CACHE_NPC) $(VSRC_NPC_MODELS)
	@echo "[INFO] NPC-mode RTL updated: $(VSRC_CACHE_NPC)"

soc:
	@mkdir -p $(VSRC_CACHE_SOC_DIR)
	cd $(CHISEL_WORK_DIR) && USE_DPIC=true ./mill -i mychisel.runMain TopMainSoC
	@cp $(CHISEL_WORK_DIR)/ysyx_25020039.sv $(VSRC_CACHE_SOC_DIR)/
	@echo "[INFO] SoC-mode RTL updated: $(VSRC_CACHE_SOC)"

chisel-gen: soc

chisel-clean:
	cd $(CHISEL_WORK_DIR) && ./mill -i clean

$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR))

CONFIG_HASH := $(shell md5sum .config | cut -d' ' -f1)
TOP_STAMP = $(OBJ_DIR)/.build_stamp
$(TOP_STAMP): .config
	@rm -rf $(OBJ_DIR)
	@mkdir -p $(OBJ_DIR)
	@echo "$(TOP_MODULE) $(CONFIG_HASH)" > $@

all: $(EXEC)
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

clean:
	$(info [CLEAN] Removing build artifacts...)
	rm -rf $(OBJ_DIR) $(BUILD_DIR)/iverilog
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif

sta:
	rm -rf ../yosys-sta/result/ysyx_25020039-500MHz
	$(MAKE) -C ../yosys-sta sta DESIGN=ysyx_25020039 RTL_FILES="$(abspath $(VSRC_CACHE_SOC))"
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

iverilog: build-microbench
	@echo "[BUILD] Compiling with Icarus Verilog..."
	@mkdir -p $(OBJ_DIR)
	@echo "[INFO] Converting $(IMG_BIN) to mem.hex (byte-wise)..."
	@hexdump -v -e '1/1 "%02x\n"' $(IMG_BIN) > mem.hex
	iverilog -g2012 -D__ICARUS__ -D MAINARGS=\"$(mainargs)\" -o $(OBJ_DIR)/sim.vvp tb_top.sv $(VSRC_CACHE_NPC)
	@echo "[RUN] Running simulation with mainargs=$(mainargs)..."
	vvp $(OBJ_DIR)/sim.vvp $(mainargs)

# -----------------------------------------------------------------------
# CI targets (see ysyx docs basic/1.11)
#   make verilog                    -> build/ysyx_25020039.sv (PC reset 0x30000000)
#   make sim-iverilog IMG=xxx       -> iverilog sim, program at 0x80000000
#   make sim-iverilog-netlist IMG=xxx NETLIST=yyy CELLS=zzz
#
# NOTE: CI has no mill/sbt. The RTL in vsrc/npc and vsrc/soc is committed
# and used directly; regenerate it locally with 'make npc' / 'make soc'.
# -----------------------------------------------------------------------

VERILOG_OUT = $(BUILD_DIR)/ysyx_25020039.sv

IVERILOG_BIN            = $(BUILD_DIR)/iverilog/sim.vvp
IVERILOG_BIN_NETLIST_IV = $(BUILD_DIR)/iverilog/sim_netlist.vvp
MEM_HEX                 = $(BUILD_DIR)/iverilog/mem.hex

CELLS ?= ../yosys-sta/nangate45/sim/cells.v
NETLIST ?= $(VSRC_CACHE_NETLIST)
IMG   ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin

$(shell mkdir -p $(BUILD_DIR) $(BUILD_DIR)/iverilog)

# generate the single-file verilog for CI synthesis / netlist simulation
verilog:
	@mkdir -p $(BUILD_DIR)
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) does not exist. Please run 'make npc' locally and commit the result."; \
		exit 1; \
	fi
	@cat $(VSRC_CACHE_NPC) > $(VERILOG_OUT)
	@echo "[INFO] Generated verilog: $(VERILOG_OUT)"

$(MEM_HEX): $(IMG)
	@mkdir -p $(dir $@)
	hexdump -v -e '1/1 "%02x\n"' $< > $@

$(IVERILOG_BIN): $(MEM_HEX)
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) does not exist. Please run 'make npc' locally and commit the result."; \
		exit 1; \
	fi
	$(IVERILOG) -g2012 -D__ICARUS__ -o $@ \
		tb_top.sv \
		$(VSRC_CACHE_NPC)

sim-iverilog: $(MEM_HEX) $(IVERILOG_BIN)
	cd $(BUILD_DIR)/iverilog && $(VVP) -n $(abspath $(IVERILOG_BIN))

# synthesize the NPC-mode RTL (netlist used by sim-iverilog-netlist)
$(VSRC_CACHE_NETLIST): $(VSRC_CACHE_NPC)
	rm -rf ../yosys-sta/result/ysyx_25020039-500MHz
	$(MAKE) -C ../yosys-sta syn DESIGN=ysyx_25020039 PDK=nangate45 \
		RTL_FILES="$(abspath $(VSRC_CACHE_NPC))"
	@cp ../yosys-sta/result/ysyx_25020039-500MHz/ysyx_25020039.netlist.syn.v $@

ifeq ($(NETLIST),$(VSRC_CACHE_NETLIST))
  NETLIST_PREREQ = $(VSRC_CACHE_NETLIST)
else
  NETLIST_PREREQ =
endif

# NOTE: $(VSRC_NPC_MODELS) is committed; it is regenerated by 'make npc'
# (see the npc target) so that built flows (e.g. CI) do not need python.
$(IVERILOG_BIN_NETLIST_IV): $(MEM_HEX) $(NETLIST_PREREQ) $(VSRC_NPC_MODELS)
	@echo "[BUILD] Compiling netlist with Icarus Verilog..."
	$(IVERILOG) -g2012 -D__ICARUS__ -o $@ \
		tb_top.sv \
		$(NETLIST) \
		$(CELLS) \
		$(VSRC_NPC_MODELS)

sim-iverilog-netlist: $(IVERILOG_BIN_NETLIST_IV)
	cd $(BUILD_DIR)/iverilog && $(VVP) -n $(abspath $(IVERILOG_BIN_NETLIST_IV))

IVERILOG = iverilog
VVP = vvp

include Makefile.lib
include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run npc soc chisel-gen chisel-clean clean sta verilog sim-iverilog sim-iverilog-netlist
