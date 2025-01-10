TOP_MODULE = double1
OBJ_DIR = obj_dir
VERLATOR_FLAGS = --cc --exe --build -trace -trace-fst -j 0 -Wall
SIM_SRC = sim_main.cpp
VERILOG_SRC = $(TOP_MADULE).V
SIM_EXE = $(OBJ_DIR)/V$(TOP_MADULE)
all:sim

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(SIM_EXE)

$(SIM_EXE): $(VERILOG_SRC) $(SIM_SRC)
	verilator $(VERILATOR_FLAGS) $(VERILOG_SRC) $(SIM_SRC)

clean:
	rm -rf $(OBJ_DIR) wave.fat

include ../Makefile
