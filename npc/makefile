VERILATOR = verilator
TOP_MODULE = top

# CSRC = $(shell find csrc -name "*.cpp" -o -name "*.c")#rv32e
CSRC = $(shell find chisel_csrc -name "*.cpp" -o -name "*.c")#chisel


# VSRC_RISCV32E = $(shell find riscv32e_vsrc -name "*.v" -o -name "*.sv")#rv32e
#VSRC_RISCV32E = $(shell find single_vsrc -name "*.v" -o -name "*.sv")
VSRC_RISCV32E = $(shell find chisel_vsrc -maxdepth 1 -name "*.v" -o -name "*.sv")#chisel
VSRC_MULVSRC = $(shell find mul_vsrc -name "*.v" -o -name "*.sv")
VSRC_MINIRISCV = $(shell find miniriscv_vsrc -name "*.v" -o -name "*.sv")

CONFIG_MINIRV_VAL := $(shell grep CONFIG_MINIRV .config | cut -d'=' -f2)
CONFIG_MUL_VAL := $(shell grep CONFIG_MUL .config | cut -d'=' -f2)

ifeq ($(CONFIG_MUL_VAL),y)
VSRC = $(VSRC_MULVSRC)
$(info [INFO] Using mul_vsrc implementation)
else ifeq ($(CONFIG_MINIRV_VAL),y)
VSRC = $(VSRC_MINIRISCV)
$(info [INFO] Using MiniRISCV implementation)
else
VSRC = $(VSRC_RISCV32E)
$(info [INFO] Using RISC-V32E implementation)
endif

C_INCLUDE_DIRS = -Icsrc \
                 -I$(NPC_HOME)/csrc/include \
                 -I$(NPC_HOME)/include \
                 -Ichisel_csrc \
                 -I$(NPC_HOME)/chisel_csrc/include

V_INCLUDE_DIRS = -Iriscv32e_vsrc \
                 -Iriscv32e_vsrc/include \
                 -Iminiriscv_vsrc \
                 -Iminiriscv_vsrc/include\
                 -Imul_vsrc \
                 -Imul_vsrc/include\
                 -Isingle_npc \
				 -Isingle_vsrc/include


INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
PROGRAM ?= $(BUILD_DIR)/dummy.bin

ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig' first)
endif

include .config

CFLAGS = -Wall -std=c++11 -g
CXXFLAGS = $(CFLAGS)
VERILATOR_FLAGS =
LDFLAGS_BASE = -lreadline -lSDL2 -ldl


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

$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR))

all: $(EXEC)
	$(info [SUCCESS] Build completed: $(EXEC))

$(EXEC): $(VSRC) $(CSRC)
	$(info [BUILD] Compiling Verilator simulation with $(VSRC))
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) \
		$(INCLUDE_DIRS) \
		$(VERILATOR_FLAGS) \
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "$(LDFLAGS_BASE) $(LDFLAGS_DIFFTEST)"

sim: $(EXEC) $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM))
	$(EXEC) $(PROGRAM) $(ARGS)

run:
	$(MAKE) sim 

$(PROGRAM):
	@mkdir -p $(@D)
	@if [ ! -f "$@" ]; then \
		touch "$@"; \
		$(info [INFO] Created empty program: $@); \
	fi

clean:
	$(info [CLEAN] Removing build artifacts...)
	rm -rf $(OBJ_DIR) $(BUILD_DIR)
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif

include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run minirun step ministep clean
