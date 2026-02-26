VERILATOR = verilator
TOP_MODULE = top

ifeq (,$(wildcard .config))
$(warning .config file not found, using default SOC mode)
endif

include .config
include Makefile.diff

VSRC_PIPELINE_CHISEL = $(shell find pipeline_chisel_vsrc -maxdepth 1 -name "*.v" -o -name "*.sv")
CSRC_PIPELINE_CHISEL_SOC = $(filter-out %/ref_so.cpp %/ref.cpp, $(shell find pipeline_chisel_soc_csrc -name "*.cpp" -o -name "*.c"))
CSRC_PIPELINE_CHISEL_NPC = $(shell find pipeline_chisel_npc_csrc -name "*.cpp" -o -name "*.c")
VSRC_MINIRV_PERIP = $(shell find ../ysyxSoC/perip -name "*.v" -o -name "*.sv")
VSRC_SOC_TOP = ../ysyxSoC/build/ysyxSoCFull.v

VSRC_CACHE_DIR     = vsrc
VSRC_CACHE_NPC_DIR = $(VSRC_CACHE_DIR)/npc
VSRC_CACHE_SOC_DIR = $(VSRC_CACHE_DIR)/soc

VSRC_CACHE_NPC     = $(VSRC_CACHE_NPC_DIR)/ysyx_25020039.sv
VSRC_CACHE_NETLIST = $(VSRC_CACHE_NPC_DIR)/ysyx_25020039.netlist.syn.v

CONFIG_PIPELINE_CHISEL_SOC_VAL := $(shell grep CONFIG_PIPELINE_CHISEL_SOC .config | cut -d'=' -f2)
CONFIG_PIPELINE_CHISEL_NPC_VAL := $(shell grep CONFIG_PIPELINE_CHISEL_NPC .config | cut -d'=' -f2)
CONFIG_NVBOARD_VAL             := $(shell grep CONFIG_NVBOARD .config | cut -d'=' -f2)

VERILATOR_FLAGS =

ifeq ($(CONFIG_PIPELINE_CHISEL_SOC_VAL),y)
VSRC = $(shell find $(VSRC_CACHE_SOC_DIR) -name "*.v" -o -name "*.sv" 2>/dev/null) \
       $(VSRC_MINIRV_PERIP) $(VSRC_SOC_TOP)
CSRC = $(CSRC_PIPELINE_CHISEL_SOC)
CHISEL_WORK_DIR   := pipeline_chisel_vsrc
CHISEL_BUILD_TOOL := mill
TOP_MODULE = ysyxSoCFull
VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
CFLAGS += -std=c++17
$(info [INFO] Using pipeline_chisel SOC Mode)
else
VSRC = $(shell find $(VSRC_CACHE_NPC_DIR) -name "*.v" -o -name "*.sv" 2>/dev/null)
CSRC = $(CSRC_PIPELINE_CHISEL_NPC)
CHISEL_WORK_DIR   := pipeline_chisel_vsrc
CHISEL_BUILD_TOOL := mill
TOP_MODULE = ysyx_25020039
CHISEL_ENV := YSYXSOC=false NPC=true
$(info [INFO] Using pipeline_chisel NPC Mode)
endif

C_INCLUDE_DIRS =
ifeq ($(CONFIG_PIPELINE_CHISEL_SOC_VAL),y)
C_INCLUDE_DIRS += \
	-Ipipeline_chisel_soc_csrc \
	-I$(NPC_HOME)/pipeline_chisel_soc_csrc/include \
	-I$(NPC_HOME)/include
else
C_INCLUDE_DIRS += \
	-Ipipeline_chisel_npc_csrc \
	-I$(NPC_HOME)/pipeline_chisel_npc_csrc/include \
	-I$(NPC_HOME)/include
endif

V_INCLUDE_DIRS  = -Ipipeline_chisel_vsrc \
                  -I../ortl \
                  -I../ysyxSoC/perip/uart16550/rtl \
                  -I../ysyxSoC/perip/spi/rtl

INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)

OBJ_DIR   = obj_dir
EXEC      = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
PROGRAM  ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin

CFLAGS       = -Wall -std=c++11 -g
CXXFLAGS     = $(CFLAGS)
LDFLAGS_BASE = -lreadline -lSDL2 -ldl

VERILATOR_FLAGS += -Wno-WIDTHEXPAND

ifeq ($(CONFIG_NVBOARD_VAL),y)
ifndef NVBOARD_HOME
$(error NVBOARD_HOME is not set)
endif
NVBOARD_USR_INC_PATH = -I$(NVBOARD_HOME)/usr/include
NVBOARD_LIB          = -L$(NVBOARD_HOME)/build -l:nvboard.a -lSDL2 -lSDL2_image -lSDL2_ttf
CFLAGS              += -DCONFIG_NVBOARD -DNVBOARD $(NVBOARD_USR_INC_PATH)
NXDC_FILES           = constr/top.nxdc
SRC_AUTO_BIND        = $(OBJ_DIR)/auto_bind.cpp

$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

include $(NVBOARD_HOME)/scripts/nvboard.mk
CSRC         += $(SRC_AUTO_BIND)
LDFLAGS_BASE += $(LDFLAGS) $(NVBOARD_LIB)
endif

ifdef CONFIG_WAVEFORM
CFLAGS          += -DENABLE_WAVEFORM
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
CFLAGS     += -DCONFIG_ITRACE -Itools/capstone/include
$(info [INFO] Instruction tracing enabled)
$(OBJ_DIR)/csrc/disasm.o: $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(info [BUILD] Compiling capstone library...)
	$(MAKE) -C tools/capstone
else
$(info [INFO] CONFIG_ITRACE is disabled)
endif

IVERILOG     = iverilog
VVP          = vvp
IVERILOG_DIR = $(BUILD_DIR)/iverilog
IVERILOG_BIN            = $(IVERILOG_DIR)/sim
IVERILOG_BIN_NETLIST_IV = $(IVERILOG_DIR)/sim_netlist
MEM_HEX = $(IVERILOG_DIR)/mem.hex

CELLS ?= ../yosys-sta/nangate45/sim/cells.v
IMG   ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin

IVERILOG_VSRC = $(filter-out %/ysyx_25020039.sv, $(VSRC_PIPELINE_CHISEL)) \
                $(VSRC_CACHE_NPC)

IVERILOG_NETLIST_VSRC = $(filter-out %/ysyx_25020039.sv %/SimUART.v, $(VSRC_PIPELINE_CHISEL))

IVERILOG_FLAGS_IV = -g2012 -D__ICARUS__ \
                    -Ipipeline_chisel_vsrc \
                    -s tb_top \
                    -o $(IVERILOG_BIN)

IVERILOG_NETLIST_FLAGS_IV = -g2012 -D__ICARUS__ \
                             -Ipipeline_chisel_vsrc \
                             -s tb_top \
                             -o $(IVERILOG_BIN_NETLIST_IV)

$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR) $(IVERILOG_DIR) \
                 $(VSRC_CACHE_NPC_DIR) $(VSRC_CACHE_SOC_DIR))

npc:
	@mkdir -p $(VSRC_CACHE_NPC_DIR)
	cd $(CHISEL_WORK_DIR) && USE_DPIC=false YSYXSOC=false NPC=true ./mill -i mychisel.runMain TopMain
	@cp $(CHISEL_WORK_DIR)/ysyx_25020039.sv $(VSRC_CACHE_NPC)
	$(info [NPC] Chisel 编译完成 → $(VSRC_CACHE_NPC))

soc:
	@mkdir -p $(VSRC_CACHE_SOC_DIR)
	cd $(CHISEL_WORK_DIR) && ./mill -i mychisel.runMain TopMain
	@cp $(CHISEL_WORK_DIR)/*.sv $(VSRC_CACHE_SOC_DIR)/ 2>/dev/null || true
	@cp $(CHISEL_WORK_DIR)/*.v  $(VSRC_CACHE_SOC_DIR)/ 2>/dev/null || true
	$(info [SOC] Chisel 编译完成 → $(VSRC_CACHE_SOC_DIR)/)

chisel-gen:
ifdef CHISEL_WORK_DIR
	cd $(CHISEL_WORK_DIR) && \
	if [ "$(CHISEL_BUILD_TOOL)" = "mill" ]; then \
		$(CHISEL_ENV) ./mill -i mychisel.runMain TopMain; \
	elif [ "$(CHISEL_BUILD_TOOL)" = "sbt" ]; then \
		$(CHISEL_ENV) sbt run; \
	else \
		true; \
	fi
else
	$(info [CHISEL] Not a Chisel configuration, skip SV generation)
endif

chisel-clean:
ifdef CHISEL_WORK_DIR
	cd $(CHISEL_WORK_DIR) && \
	if [ "$(CHISEL_BUILD_TOOL)" = "mill" ]; then \
		mill -i clean; \
	elif [ "$(CHISEL_BUILD_TOOL)" = "sbt" ]; then \
		sbt clean; \
	else \
		true; \
	fi
else
	$(info [CHISEL] Not a Chisel configuration, skip Chisel clean)
endif

all: $(EXEC)
	$(info [SUCCESS] Build completed: $(EXEC))

$(EXEC): $(VSRC) $(CSRC) $(NVBOARD_ARCHIVE)
	@if [ -z "$(VSRC)" ]; then \
		echo "[ERROR] vsrc/ 为空，请先运行 make npc 或 make soc"; \
		exit 1; \
	fi
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) \
		$(INCLUDE_DIRS) \
		$(VERILATOR_FLAGS) \
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "$(LDFLAGS_BASE) $(LDFLAGS_DIFFTEST)"

sim: all $(PROGRAM)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(EXEC) $(PROGRAM) $(ARGS)
	$(info [INFO] Simulation completed successfully)

run:
	$(MAKE) sim

$(PROGRAM):
	@mkdir -p $(@D)
	@if [ ! -f "$@" ]; then touch "$@"; fi

VERILOG_OUT = $(BUILD_DIR)/ysyx_25020039.sv

verilog:
	@mkdir -p $(BUILD_DIR)
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) 不存在，请先运行 make npc"; \
		exit 1; \
	fi
	@cat $(CHISEL_WORK_DIR)/Ebreak.v $(VSRC_CACHE_NPC) > $(VERILOG_OUT)
	@echo "[INFO] Generated verilog: $(VERILOG_OUT)"

$(MEM_HEX): $(IMG)
	@mkdir -p $(IVERILOG_DIR)
	hexdump -v -e '1/1 "%02x\n"' $< > $@

$(IVERILOG_BIN): $(VSRC_CACHE_NPC) $(MEM_HEX)
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) 不存在，请先运行 make npc"; \
		exit 1; \
	fi
	$(IVERILOG) $(IVERILOG_FLAGS_IV) \
		$(IVERILOG_VSRC) \
		tb_top.sv

sim-iverilog: $(MEM_HEX) $(IVERILOG_BIN)
	cd $(IVERILOG_DIR) && $(VVP) -n $(abspath $(IVERILOG_BIN))

syn-npc:
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) 不存在，请先运行 make npc"; \
		exit 1; \
	fi
	rm -rf ../yosys-sta/result/ysyx_25020039-npc-500MHz
	$(MAKE) -C ../yosys-sta syn DESIGN=ysyx_25020039 \
		RTL_FILES="$(abspath $(filter-out %/ysyx_25020039.sv, $(VSRC_PIPELINE_CHISEL))) \
		$(abspath $(VSRC_CACHE_NPC))"
	@cp ../yosys-sta/result/ysyx_25020039-npc-500MHz/ysyx_25020039.netlist.syn.v \
		$(VSRC_CACHE_NETLIST)

$(IVERILOG_BIN_NETLIST_IV): $(VSRC_CACHE_NETLIST) $(CELLS) $(MEM_HEX)
	@if [ ! -f "$(VSRC_CACHE_NETLIST)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NETLIST) 不存在，请先运行 make syn-npc"; \
		exit 1; \
	fi
	$(IVERILOG) $(IVERILOG_NETLIST_FLAGS_IV) \
		$(VSRC_CACHE_NETLIST) \
		$(CELLS) \
		$(IVERILOG_NETLIST_VSRC) \
		tb_top.sv

sim-iverilog-netlist: $(MEM_HEX) $(IVERILOG_BIN_NETLIST_IV)
	$(VVP) -n $(IVERILOG_BIN_NETLIST_IV)

sta:
	@if [ ! -f "$(VSRC_CACHE_NPC)" ]; then \
		echo "[ERROR] $(VSRC_CACHE_NPC) 不存在，请先运行 make npc"; \
		exit 1; \
	fi
	rm -rf ../yosys-sta/result/ysyx_25020039-500MHz
	$(MAKE) -C ../yosys-sta sta DESIGN=ysyx_25020039 \
		RTL_FILES="$(abspath $(filter-out %/SRAM.sv %/DPI_Mem.v %/ysyx_25020039.sv, $(VSRC_PIPELINE_CHISEL))) \
		$(abspath $(VSRC_CACHE_NPC))"
	cat ../yosys-sta/result/ysyx_25020039-500MHz/synth_stat.txt

clean: 
	rm -rf $(OBJ_DIR) $(BUILD_DIR)
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif

include Makefile.lib
include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run npc soc chisel-gen chisel-clean \
        clean clean-npc clean-soc clean-vsrc \
        sta verilog sim-iverilog sim-iverilog-netlist syn-npc