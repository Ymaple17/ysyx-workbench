VERILATOR = verilator
TOP_MODULE = top

CSRC = $(shell find csrc -name "*.cpp" -o -name "*.c")

VSRC_RISCV32E = $(shell find riscv32e_vsrc -name "*.v" -o -name "*.sv")
VSRC_MINIRISCV = $(shell find miniriscv_vsrc -name "*.v" -o -name "*.sv")

VSRC = $(VSRC_RISCV32E)

C_INCLUDE_DIRS = -Icsrc \
                 -I$(NPC_HOME)/csrc/include \
                 -I$(NPC_HOME)/include

V_INCLUDE_DIRS = -Iriscv32e_vsrc \
                 -Iriscv32e_vsrc/include \
                 -Iminiriscv_vsrc \
                 -Iminiriscv_vsrc/include

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
else
$(info [INFO] CONFIG_WAVEFORM is disabled)
endif

ifdef CONFIG_DIFFTEST
CFLAGS += -DDIFFTEST \
          -DDIFFTEST_LIB=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so
ifdef CONFIG_ENABLE_MEMORY_CHECK
CFLAGS += -DENABLE_MEMORY_CHECK
$(info [INFO] Memory check in difftest enabled)
endif
# 添加NEMU共享库路径到链接选项
LDFLAGS_DIFFTEST = -L$(NEMU_HOME)/build -lriscv32-nemu-interpreter-so
$(info [INFO] DiffTest enabled)
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
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "$(LDFLAGS_BASE) $(LDFLAGS_DIFFTEST)"  # 合并链接选项

sim: $(EXEC) $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM))
	$(EXEC) $(PROGRAM) $(ARGS)

run:
	$(MAKE) sim 

minirun:
	$(MAKE) sim  VSRC=$(VSRC_MINIRISCV)

step:
	$(MAKE) sim 

ministep:
	$(MAKE) sim VSRC=$(VSRC_MINIRISCV)

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