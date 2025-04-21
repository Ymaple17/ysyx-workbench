include ../Makefile

# 基础配置
CC = gcc
CXX = g++
WORK_DIR = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)
TOPNAME = top

# 自动创建构建目录
$(shell mkdir -p $(BUILD_DIR))

# 包含路径
INC_PATH := $(WORK_DIR)/csrc/include $(WORK_DIR)/csrc/src/isa/include $(INC_PATH)
INCLUDES = $(addprefix -I, $(INC_PATH))

# 编译选项
CFLAGS := -O2 -MMD -Wall -Og $(INCLUDES) $(CFLAGS)
CFLAGS += -ggdb3 -g
#CFLAGS += -DCONFIG_DEVICE  # 确保启用设备模块
CFLAGS += -Wno-unused-variable -Wno-shadow  # 添加警告抑制选项

# 链接选项
LDFLAGS += $(shell sdl2-config --libs) -lSDL2_image
LDFLAGS += $(shell llvm-config --libs)
LDFLAGS += -O2 -Og -ggdb3 -ldl -pie -lreadline -pthread -latomic

# 包含自动生成的配置
-include $(NPC_HOME)/include/config/auto.conf
-include $(NPC_HOME)/include/config/auto.conf.cmd
include $(NPC_HOME)/scripts/config.mk
include $(NPC_HOME)/scripts/build.mk

# 递归查找所有源文件
CSRCS += $(shell find $(abspath ./csrc) -name "*.c" -o -name "*.cpp" -o -name "*.cc")
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v" -o -name "*.sv")

# 确保包含必要的源文件
CSRCS += $(wildcard $(NPC_HOME)/csrc/src/device/io/mmio.c)
CSRCS += $(wildcard $(NPC_HOME)/csrc/src/monitor/sdb/sdl_utils.c)  # 假设SDL实现在这里

# Verilator配置
VERILATOR = verilator
VERILATOR_FLAGS += $(addprefix -LDFLAGS , $(LDFLAGS))
VERILATOR_FLAGS += $(addprefix -CFLAGS , $(CFLAGS))
VERILATOR_FLAGS += -Wall --cc --trace
VERILATOR_FLAGS += -O3 --x-assign fast --x-initial fast --noassert
VERILATOR_FLAGS += -MMD --build

# 构建目标
default: $(BIN)

$(BIN): $(VSRCS) $(CSRCS)
	@echo "Building with sources:"
	@echo "  C/C++: $(CSRCS)"
	@echo "  Verilog: $(VSRCS)"
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) $(VSRCS) \
		--exe $(CSRCS) \
		--Mdir $(OBJ_DIR) -o $(abspath $(BIN))

# 测试目标
test: $(VSRCS) $(test_CSRCS)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) $(VSRCS) \
		--exe $(test_CSRCS) \
		--Mdir $(OBJ_DIR) -o $(abspath $(BIN))
	./build/$(TOPNAME)

# 其他目标
all: default

sim: $(BIN)
	@$^ $(NPC_FLAGS)

run: sim

wave: sim
	gtkwave dump.vcd

gdb: $(BIN)
	@gdb --args $(BIN) $(NPC_FLAGS)

clean:
	rm -rf $(BUILD_DIR)
	rm -f dump.vcd

.PHONY: default all sim wave run clean test
