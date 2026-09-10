include $(AM_HOME)/scripts/riscv32e-ysyxsoc.mk

# The ysyxSoC Verilator harness preloads ELF LOAD segments into SRAM/SDRAM.
# The board architecture keeps the normal Flash copy boot path.
CFLAGS += -DYSYXSOC_SIM_PRELOADED
ASFLAGS += -DYSYXSOC_SIM_PRELOADED
