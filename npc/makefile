VERILATOR = verilator
TOP_MODULE = top
CSRC = $(shell find csrc -name "*.cpp") $(shell find csrc -name "*.cc")

VSRC_RISCV32E = $(shell find riscv32e_vsrc -name "*.v" -o -name "*.sv")
VSRC_MINIRISCV = $(shell find miniriscv_vsrc -name "*.v" -o -name "*.sv")

VSRC = $(VSRC_RISCV32E)

C_INCLUDE_DIRS = -Icsrc -I$(NPC_HOME)/csrc/include -I$(NPC_HOME)/include
V_INCLUDE_DIRS = -Iriscv32e_vsrc -Iriscv32e_vsrc/include -Iminiriscv_vsrc -Iminiriscv_vsrc/include
INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
RUN_MODE ?= run
PROGRAM ?= $(BUILD_DIR)/dummy.bin

# 检查 .config 文件是否存在
ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig' first)
endif

# 包含 Kconfig 生成的配置文件
include .config

# 编译标志基础配置
CFLAGS = -Wall -std=c++11 -g #-O2
VERILATOR_FLAGS =

# 波形功能宏定义
ifdef CONFIG_WAVEFORM
CFLAGS += -DENABLE_WAVEFORM
CFLAGS += -DWAVEFORM_FILE=\\\"$(CONFIG_WAVEFORM_FILE)\\\"
VERILATOR_FLAGS += --trace
$(info [INFO] Waveform recording enabled: $(CONFIG_WAVEFORM_FILE))
else
$(info [INFO] CONFIG_WAVEFORM is disabled)
endif

# 差分测试宏定义
ifdef CONFIG_DIFFTEST
CFLAGS += -DDIFFTEST
ifdef CONFIG_ENABLE_MEMORY_CHECK
CFLAGS += -DENABLE_MEMORY_CHECK
$(info [INFO] Memory check in difftest enabled)
endif
$(info [INFO] DiffTest enabled)
else
$(info [INFO] CONFIG_DIFFTEST is disabled)
endif

ifdef CONFIG_ITRACE
LIBCAPSTONE = tools/capstone/libcapstone.so.6
CFLAGS += -DCONFIG_ITRACE -Itools/capstone/include
VERILATOR_FLAGS += -LDFLAGS "-ldl"
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
		$(VERILATOR_FLAGS) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "-lreadline"


sim: $(EXEC) $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM) in $(RUN_MODE) mode)
	$(EXEC) $(PROGRAM) $(RUN_MODE) $(ARGS)

run:
	$(MAKE) sim RUN_MODE=run

minirun:
	$(MAKE) sim RUN_MODE=run VSRC=$(VSRC_MINIRISCV)

step:
	$(MAKE) sim RUN_MODE=step

ministep:
	$(MAKE) sim RUN_MODE=step VSRC=$(VSRC_MINIRISCV)


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
	$(info [CLEAN] Done)


include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run minirun step ministep clean
