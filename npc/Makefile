BUILD_DIR = ./build

export PATH := $(PATH):$(abspath ./utils)

# File and directory definitions
TOPNAME = Top
VSRCS_DIR = $(abspath ./vsrc)
CSRCS_DIR = $(abspath ./csrc)
VSRCS = $(wildcard $(VSRCS_DIR)/*.v)  # All Verilog files in vsrc/
# 递归包含 csrc/ 及其子目录下的所有 .c 和 .cpp 文件
CSRCS = $(shell find $(CSRCS_DIR) -name "*.c" -o -name "*.cpp")
NPC_MAIN = $(abspath ./npc-main.cpp)
BIN = $(abspath $(BUILD_DIR)/$(TOPNAME))

# Default image file and arguments
ARGS ?= --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so
IMG ?= ../am-kernels/tests/cpu-tests/build/dummy-riscv32e-npc.bin

# Define LDFLAGS for linking Capstone, SDL2, and readline
LDFLAGS = -lcapstone -lSDL2 -lreadline

# Add debug flag to CFLAGS
CFLAGS = -g  # 启用调试信息

# Phony targets
.PHONY: build sim wave clean run gdb

# Build target
$(BIN): $(VSRCS) $(CSRCS) $(NPC_MAIN)
	@mkdir -p $(BUILD_DIR)
	verilator --build --exe -cc -O0 --x-assign unique --x-initial unique --noassert --timing --trace \
	-Wno-lint \
	--top-module $(TOPNAME) --Mdir $(BUILD_DIR)/obj_dir -o $(BIN) \
	-CFLAGS "$(CFLAGS) -I$(CSRCS_DIR)/include -I$(CSRCS_DIR)/include/mem -I$(CSRCS_DIR)/include/cpu -I$(CSRCS_DIR)/include/device -I$(CSRCS_DIR)/include/tracer -I$(CSRCS_DIR)/include/monitor -I$(CSRCS_DIR)/include/monitor/sdb -I/usr/include/capstone" \
	-LDFLAGS "$(LDFLAGS)" \
	$(VSRCS) $(CSRCS) $(NPC_MAIN)

# Main targets
build: $(BIN)

run: build
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!
	$(BIN) $(IMG) $(ARGS)

wave: sim
	gtkwave waveform.vcd

clean:
	-rm -rf $(BUILD_DIR)

gdb: build
	gdb --args $(BIN) $(IMG) $(ARGS)

# Include external Makefile (if needed)
include ../Makefile
