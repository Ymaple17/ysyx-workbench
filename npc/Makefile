TOP_MODULE = double1
OBJ_DIR = obj_dir
VERILATOR_FLAGS = --cc --exe --build -trace -trace-fst -j 0 -Wall
SIM_SRC = sim_main.cpp
VERILOG_SRC = $(TOP_MODULE).v
SIM_EXE = $(OBJ_DIR)/V$(TOP_MODULE)
all:sim

sim:$(SIM_EXE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(SIM_EXE)

$(SIM_EXE): $(VERILOG_SRC) $(SIM_SRC)
	verilator $(VERILATOR_FLAGS) $(VERILOG_SRC) $(SIM_SRC)

clean:
	rm -rf $(OBJ_DIR) wave.fat

.PHONY: all sim clean
include ../Makefile
