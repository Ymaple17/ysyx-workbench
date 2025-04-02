# 包含上层 Makefile（如果需要）
include ../Makefile
# 基本工具和变量
CC = gcc
VERILATOR = verilator
WORK_DIR = $(shell pwd)
BUILD_DIR = $(WORK_DIR)/build
OBJ_DIR = $(BUILD_DIR)/obj_dir
TOPNAME = top
BIN = $(BUILD_DIR)/$(TOPNAME)
# 源文件目录
VSRCS_DIR = $(abspath ./vsrc)
CSRCS_DIR = $(abspath ./csrc)
TEST_DIR = $(abspath ./test)

# 查找源文件
VSRCS = $(shell find $(VSRCS_DIR) -name "*.v" -or -name "*.sv")
CSRCS = $(shell find $(CSRCS_DIR) -name "*.c" -or -name "*.cpp" -or -name "*.cc")
TEST_CSRCS = $(shell find $(TEST_DIR) -name "*.cpp" -or -name "*.cc" -or -name "*.c" 2>/dev/null)

# 默认镜像文件
IMG ?=

# 编译和链接选项
INC_PATH = $(CSRCS_DIR)/include $(CSRCS_DIR)/src/isa/include
INCLUDES = $(addprefix -I, $(INC_PATH))
CFLAGS = -O2 -MMD -Wall -Og $(INCLUDES) -ggdb3 -g
LDFLAGS = $(shell sdl2-config --libs) -lSDL2_image $(shell llvm-config --libs) -O2 -Og -ggdb3 -ldl -pie -lreadline

# Verilator 选项
VERILATOR_FLAGS = -Wall --cc --trace -O3 --x-assign fast --x-initial fast --noassert -MMD --build \
                  --top-module $(TOPNAME) \
                  -CFLAGS "$(CFLAGS)" \
                  -LDFLAGS "$(LDFLAGS)"

# 仿真参数
override ARGS ?= --log=$(BUILD_DIR)/npc-log.txt
#override ARGS += --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so
NPC_FLAGS = $(ARGS) $(IMG)

# 确保构建目录存在
$(shell mkdir -p $(BUILD_DIR))

# 默认目标
.PHONY: default all sim run wave clean test
default: $(BIN)

# 构建目标
$(BIN): $(VSRCS) $(CSRCS)
	@rm -rf $(OBJ_DIR)
	@echo "Building $(BIN)"
	@echo "Using source files: $(VSRCS) $(CSRCS)"
	@echo "Include paths: $(INCLUDES)"
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN)) $^

# 测试目标
test: $(VSRCS) $(TEST_CSRCS)
	@rm -rf $(OBJ_DIR)
	@echo "Building test executable"
	$(VERILATOR) $(VERILATOR_FLAGS) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN)) $^
	./$(BIN)

# 仿真目标
sim: $(BIN)
	@$^ $(NPC_FLAGS)

run: sim

# 波形查看
wave: sim
	gtkwave dump.vcd

# 调试目标
gdb: $(BIN)
	@gdb --args $(BIN) $(NPC_FLAGS)

# 清理目标
clean:
	rm -rf $(BUILD_DIR) dump.vcd

# 所有目标
all: default

# 默认 NEMU_HOME（如果未定义）
NEMU_HOME ?= $(WORK_DIR)/../nemu修改我npc的makefile，为其加上路径
