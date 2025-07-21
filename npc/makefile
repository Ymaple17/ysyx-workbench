VERILATOR = verilator
TOP_MODULE = top
CSRC = $(wildcard csrc/*.cpp) $(shell find csrc -name "*.cc")
VSRC = $(shell find vsrc -name "*.v" -o -name "*.sv")
C_INCLUDE_DIRS = -Icsrc -I$(NPC_HOME)/csrc/include -I$(NPC_HOME)/include
V_INCLUDE_DIRS = -Ivsrc -Ivsrc/include
INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
RUN_MODE ?= run
PROGRAM ?= $(BUILD_DIR)/dummy.bin

override ARGS ?= --log=$(BUILD_DIR)/npc-log.txt
override ARGS += --diff=$(NPC_HOME)/build/riscv32-nemu-interpreter-so
NPC_FLAGS = $(ARGS)

# 检查 .config 文件是否存在
ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig')
endif

# 包含 Kconfig 生成的配置文件
include .config

# 编译标志
CFLAGS = -Wall -std=c++11


# 波形功能宏定义
ifdef CONFIG_WAVEFORM
CFLAGS += -DENABLE_WAVEFORM
CFLAGS += -DWAVEFORM_FILE=\\\"$(CONFIG_WAVEFORM_FILE)\\\"
VERILATOR_FLAGS += --trace
else
$(info CONFIG_WAVEFORM is not enabled)
endif

# 差分测试宏定义
ifdef CONFIG_DIFFTEST
$(info CONFIG_DIFFTEST is enabled, adding -DDIFFTEST)
CFLAGS += -DDIFFTEST
endif

ifdef CONFIG_ENABLE_MEMORY_CHECK
CFLAGS += -DENABLE_MEMORY_CHECK
endif

# 确保构建目录存在
$(shell mkdir -p $(BUILD_DIR))

all: $(EXEC)

$(EXEC): $(VSRC) $(CSRC)
	$(info Building with CFLAGS=$(CFLAGS) VERILATOR_FLAGS=$(VERILATOR_FLAGS))
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) $(INCLUDE_DIRS) \
		--build -j 0 --top-module $(TOP_MODULE) $(VERILATOR_FLAGS) \
		--Mdir $(OBJ_DIR) \
		--LDFLAGS "-lreadline" \
		+libext+.v+.sv \
		-CFLAGS "$(CFLAGS)"

sim: $(EXEC) $(PROGRAM)
	$(EXEC) $(PROGRAM) $(RUN_MODE) $(NPC_FLAGS)

run:
	$(MAKE) sim RUN_MODE=run

step:
	$(MAKE) sim RUN_MODE=step

$(PROGRAM):
	@mkdir -p $(@D)
	@echo -n > $@
	@echo "Created empty dummy program: $@"

clean:
	rm -rf $(OBJ_DIR) $(BUILD_DIR)
ifdef CONFIG_WAVEFORM
	rm -f *.vcd
endif

# 包含 config.mk 用于 menuconfig
include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run step clean