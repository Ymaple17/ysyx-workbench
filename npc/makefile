VERILATOR = verilator
TOP_MODULE = top

VSRC_CHISEL_MUL = $(shell find mul_chisel_vsrc -maxdepth 1 -name "*.v" -o -name "*.sv")
CSRC_CHISEL_MUL = $(shell find mul_chisel_csrc -name "*.cpp" -o -name "*.c")#mul_chisel

VSRC_CHISEL_RISCV32E = $(shell find chisel_vsrc -maxdepth 1 -name "*.v" -o -name "*.sv")#chisel
CSRC_CHISEL_RISCV32E = $(shell find chisel_csrc -name "*.cpp" -o -name "*.c")#chisel

VSRC_RISCV32E = $(shell find single_vsrc -name "*.v" -o -name "*.sv")#rv32e
VSRC_MULVSRC = $(shell find mul_vsrc -name "*.v" -o -name "*.sv")
#VSRC_MINIRISCV = $(shell find miniriscv_vsrc -name "*.v" -o -name "*.sv")
VSRC_MINIRISCV = /home/qiu/ysyx-workbench/npc/ysyx_25020039.v
VSRC_MUL_SOC = $(shell find mul_soc_vsrc -name "*.v" -o -name "*.sv")

CONFIG_MINIRV_VAL := $(shell grep CONFIG_MINIRV .config | cut -d'=' -f2)
CONFIG_SINGLE_VAL := $(shell grep CONFIG_SINGLE .config | cut -d'=' -f2)
CONFIG_MUL_VAL := $(shell grep CONFIG_MUL .config | cut -d'=' -f2)
CONFIG_CHISEL_MUL_VAL := $(shell grep CONFIG_CHISEL_MUL .config | cut -d'=' -f2)
CONFIG_CHISEL_RISCV32E_VAL := $(shell grep CONFIG_CHISEL_RISCV32E .config | cut -d'=' -f2)
CONFIG_NVBOARD_VAL := $(shell grep CONFIG_NVBOARD .config | cut -d'=' -f2)

VSRC_MINIRV_PERIP = $(shell find ../ysyxSoC/perip -name "*.v" -o -name "*.sv")
VSRC_MINIRV_TOP = ../ysyxSoC/ready-to-run/D-stage/ysyxSoCFull.v
VSRC_SOC_TOP = ../ysyxSoC/build/ysyxSoCFull.v

VERILATOR_FLAGS =

ifeq ($(CONFIG_CHISEL_MUL_VAL),y)
  VSRC = $(VSRC_CHISEL_MUL) $(VSRC_MINIRV_PERIP) $(VSRC_SOC_TOP)
  CSRC = $(CSRC_CHISEL_MUL)
  CHISEL_WORK_DIR := mul_chisel_vsrc
  TOP_MODULE = ysyxSoCFull
  VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
  CFLAGS += -std=c++17
  $(info [INFO] Using chisel_mul implementation)
else ifeq ($(CONFIG_CHISEL_RISCV32E_VAL),y)
  VSRC = $(VSRC_CHISEL_RISCV32E)
  CSRC = $(CSRC_CHISEL_RISCV32E)
  CHISEL_WORK_DIR := chisel_vsrc
  $(info [INFO] Using chisel_riscv32e implementation)
else ifeq ($(CONFIG_MINIRV_VAL),y)
  VSRC = $(VSRC_MINIRISCV) $(VSRC_MINIRV_PERIP) $(VSRC_MINIRV_TOP)
  CSRC = $(shell find minirv_csrc -name "*.cpp" -o -name "*.c")#minirv
  TOP_MODULE = ysyxSoCFull
  VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
  $(info [INFO] Using MiniRISCV implementation)
else ifeq ($(CONFIG_MUL_VAL),y)
  VSRC = $(VSRC_MULVSRC)
  CSRC = $(shell find mul_csrc -name "*.cpp" -o -name "*.c")#mul
  $(info [INFO] Using mul implementation)
else ifeq ($(CONFIG_SINGLE_VAL),y)
  VSRC = $(VSRC_RISCV32E) 
  CSRC = $(shell find single_csrc -name "*.cpp" -o -name "*.c")#rv32e
  $(info [INFO] Using single_vsrc implementation)
else 
  VSRC = $(VSRC_MUL_SOC) $(VSRC_MINIRV_PERIP) $(VSRC_SOC_TOP)
  CSRC = $(shell find mul_soc_csrc -name "*.cpp" -o -name "*.c")#mul_soc
  TOP_MODULE = ysyxSoCFull
  VERILATOR_FLAGS += --no-timing --timescale "1ns/1ns" --timescale-override "1ns/1ns"
  #VERILATOR_FLAGS += --autoflush
  $(info [INFO] Using mul_soc implementation)
endif

C_INCLUDE_DIRS =

ifeq ($(CONFIG_SINGLE_VAL),y)
  C_INCLUDE_DIRS += \
    -Isingle_csrc \
    -I$(NPC_HOME)/single_csrc/include \
    -I$(NPC_HOME)/include \
    -Ichisel_csrc \
    -I$(NPC_HOME)/chisel_csrc/include
else ifeq ($(CONFIG_MINIRV_VAL),y)
  C_INCLUDE_DIRS += \
    -Iminirv_csrc \
    -I$(NPC_HOME)/minirv_csrc/include \
    -I$(NPC_HOME)/include
else ifeq ($(CONFIG_MUL_VAL),y)
  C_INCLUDE_DIRS += \
    -Imul_chisel_csrc \
    -I$(NPC_HOME)/mul_chisel_csrc/include \
    -Imul_csrc \
    -I$(NPC_HOME)/mul_csrc/include \
    -I$(NPC_HOME)/include
else ifeq ($(CONFIG_CHISEL_RISCV32E_VAL),y)
  C_INCLUDE_DIRS += \
    -Iriscv32e_csrc \
    -I$(NPC_HOME)/riscv32e_csrc/include \
    -I$(NPC_HOME)/include
else
  C_INCLUDE_DIRS += \
    -Imul_soc_csrc \
    -I$(NPC_HOME)/mul_soc_csrc/include \
    -I$(NPC_HOME)/include
endif

V_INCLUDE_DIRS =

ifeq ($(CONFIG_SINGLE_VAL),y)
  V_INCLUDE_DIRS += -Iriscv32e_vsrc \
                    -Iriscv32e_vsrc/include
else ifeq ($(CONFIG_MINIRV_VAL),y)
  V_INCLUDE_DIRS += -Iminiriscv_vsrc \
                    -Iminiriscv_vsrc/include \
                    -I../ysyxSoC/perip/uart16550/rtl \
                    -I../ysyxSoC/perip/spi/rtl
else ifeq ($(CONFIG_MUL_VAL),y)
  V_INCLUDE_DIRS += -Imul_vsrc \
                    -Imul_vsrc/include
else ifeq ($(CONFIG_CHISEL_MUL_VAL),y)
  V_INCLUDE_DIRS += -Imul_chisel_vsrc \
                    -I../ysyxSoC/perip/uart16550/rtl \
                    -I../ysyxSoC/perip/spi/rtl
else 
  V_INCLUDE_DIRS += -Imul_soc_vsrc \
                    -Imul_soc_vsrc/include \
                    -I../ysyxSoC/perip/uart16550/rtl \
                    -I../ysyxSoC/perip/spi/rtl
endif

INCLUDE_DIRS = $(C_INCLUDE_DIRS) $(V_INCLUDE_DIRS)
OBJ_DIR = obj_dir
EXEC = $(OBJ_DIR)/V$(TOP_MODULE)
BUILD_DIR ?= build
PROGRAM ?= ../ysyxSoC/ready-to-run/D-stage/dummy.bin
# PROGRAM ?= ../am-kernels/kernels/char-test/char-test.bin

ifeq (,$(wildcard .config))
$(error .config file not found, please run 'make menuconfig' first)
endif

include .config

CFLAGS = -Wall -std=c++11 -g
CXXFLAGS = $(CFLAGS)
LDFLAGS_BASE = -lreadline -lSDL2 -ldl

ifeq ($(CONFIG_NVBOARD_VAL),y)
  ifndef NVBOARD_HOME
    $(error NVBOARD_HOME is not set)
  endif
  NVBOARD_USR_INC_PATH = -I$(NVBOARD_HOME)/usr/include
  NVBOARD_LIB = -L$(NVBOARD_HOME)/build -l:nvboard.a -lSDL2 -lSDL2_image -lSDL2_ttf

  CFLAGS += -DCONFIG_NVBOARD -DNVBOARD $(NVBOARD_USR_INC_PATH)
  NXDC_FILES = constr/top.nxdc
  SRC_AUTO_BIND = $(OBJ_DIR)/auto_bind.cpp
  $(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@
  
  include $(NVBOARD_HOME)/scripts/nvboard.mk
  CSRC += $(SRC_AUTO_BIND)
  LDFLAGS_BASE += $(LDFLAGS) $(NVBOARD_LIB)
endif

ifdef CONFIG_WAVEFORM
  CFLAGS += -DENABLE_WAVEFORM
  VERILATOR_FLAGS += --trace-fst
else
  $(info [INFO] CONFIG_WAVEFORM is disabled)
endif

ifdef CONFIG_DIFFTEST
  NEMU_LIB_ABS_PATH = $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
  ifeq (,$(wildcard $(NEMU_LIB_ABS_PATH)))
    $(error NEMU共享库不存在: $(NEMU_LIB_ABS_PATH)，请先编译NEMU)
  endif
  CFLAGS += -DDIFFTEST \
            -DDIFFTEST_LIB=\"$(NEMU_LIB_ABS_PATH)\"
  ifdef CONFIG_ENABLE_MEMORY_CHECK
    CFLAGS += -DENABLE_MEMORY_CHECK
    $(info [INFO] Memory check in difftest enabled)
  endif
  LDFLAGS_DIFFTEST = $(NEMU_LIB_ABS_PATH)
  $(info [INFO] DiffTest enabled，使用绝对路径链接: $(NEMU_LIB_ABS_PATH))
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

chisel-gen:
ifdef CHISEL_WORK_DIR
	$(info [CHISEL] Generating SV files in $(CHISEL_WORK_DIR)...)
	# 进入 Chisel 工作目录执行 sbt run（生成 SV）
	cd $(CHISEL_WORK_DIR) && sbt run
	$(info [CHISEL] SV files generated successfully)
else
	$(info [CHISEL] Not a Chisel configuration, skip SV generation)
endif

chisel-clean:
ifdef CHISEL_WORK_DIR
	$(info [CHISEL] Cleaning Chisel artifacts in $(CHISEL_WORK_DIR)...)
	cd $(CHISEL_WORK_DIR) && sbt clean
	$(info [CHISEL] Chisel artifacts cleaned successfully)
else
	$(info [CHISEL] Not a Chisel configuration, skip Chisel clean)
endif

$(shell mkdir -p $(BUILD_DIR) $(OBJ_DIR))

all: chisel-gen $(EXEC)
	$(info [SUCCESS] Build completed: $(EXEC))

$(EXEC): $(VSRC) $(CSRC) $(NVBOARD_ARCHIVE)
	$(info [BUILD] Compiling Verilator simulation with $(VSRC))
	$(VERILATOR) --cc $(VSRC) --exe $(CSRC) \
		$(INCLUDE_DIRS) \
		$(VERILATOR_FLAGS) \
		--build -j $(shell nproc) --top-module $(TOP_MODULE) \
		--Mdir $(OBJ_DIR) \
		-CFLAGS "$(CFLAGS)" \
		--LDFLAGS "$(LDFLAGS_BASE) $(LDFLAGS_DIFFTEST)"

sim: all $(PROGRAM)
	$(info [RUN] Starting simulation: $(PROGRAM))
	$(EXEC) $(PROGRAM) $(ARGS) 

run:
	$(MAKE) sim 

$(PROGRAM):
	@mkdir -p $(@D)
	@if [ ! -f "$@" ]; then \
		touch "$@"; \
		$(info [INFO] Created empty program: $@); \
	fi

clean: chisel-clean
	$(info [CLEAN] Removing build artifacts...)
	rm -rf $(OBJ_DIR) $(BUILD_DIR)
ifdef CONFIG_WAVEFORM
	rm -f $(CONFIG_WAVEFORM_FILE)
endif


include $(NPC_HOME)/scripts/config.mk

.PHONY: all sim run chisel-gen chisel-clean  clean
