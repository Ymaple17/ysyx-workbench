# 设定 Verilog 源文件和 Testbench
VERILOG_SRCS = top.v RegisterFile.v alu.v instruction_memory.v control_unit.v
TESTBENCH = tb_cpu.v

# 输出的 C++ 源文件和可执行文件
OBJ_DIR = obj_dir
TOP_MODULE = tb_cpu
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)

# Verilator 编译命令
VERILATOR = verilator
VERILATOR_FLAGS = --cc --exe --trace --top-module

# 编译步骤
all: $(EXEC)

$(EXEC): $(VERILOG_SRCS) $(TESTBENCH)
	verilator $(VERILATOR_FLAGS) $(VERILOG_SRCS) $(TESTBENCH)
	make -C $(OBJ_DIR) -j -f V$(TOP_MODULE).mk

clean:
	rm -rf $(OBJ_DIR) $(EXEC)

run: $(EXEC)
	$(EXEC)


