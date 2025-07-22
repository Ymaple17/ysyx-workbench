VERILATOR = verilator
TOP_MODULE = top
CSRC = $(shell find csrc -name "*.cpp") $(shell find csrc -name "*.cc")
VSRC = $(shell find vsrc -name "*.v" -o -name "*.sv")
C_INCLUDE_DIRS = -Icsrc -I$(NPC_HOME)/csrc/include -I$(NPC_HOME)/include
V_INCLUDE_DIRS = -Ivsrc -Ivsrc/include
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

# 确保构建目录存在
$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR))

# 主目标：编译Verilator仿真器
all: $(EXEC)
	$(info [SUCCESS] Build completed: $(EXEC))

# Verilator编译规则
$(EXEC): $(VSRC) $(CSRC)
	$(info [BUILD] Compiling Verilator simulation...)
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) \
		$(INCLUDE_DIRS) \
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		$(VERILATOR_FLAGS) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "-lreadline"

# 运行仿真
sim: $(EXEC) $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM) in $(RUN_MODE) mode)
	$(EXEC) $(PROGRAM) $(RUN_MODE) $(ARGS)

# 批处理模式运行
run:
	$(MAKE) sim RUN_MODE=run

# 交互模式运行
step:
	$(MAKE) sim RUN_MODE=step

# 生成空的目标程序（默认）
$(PROGRAM):
	@mkdir -p $(@D)
	@if [ ! -f "$@" ]; then \
		touch "$@"; \
		$(info [INFO] Created empty program: $@); \
	fi

# 清理编译产物（不清理capstone）
clean:
	$(info [CLEAN] Removing build artifacts...)
	rm -rf $(OBJ_DIR) $(BUILD_DIR)
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif
	$(info [CLEAN] Done)

# 包含menuconfig配置
include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run step clean
