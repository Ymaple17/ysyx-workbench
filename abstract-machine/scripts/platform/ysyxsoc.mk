AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c \
		   riscv/ysyxsoc/gpu.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/input.c \
           riscv/ysyxsoc/uart.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

# AM_SRCS := riscv/ysyxsoc/trm.c 

CFLAGS    += -fdata-sections -ffunction-sections
CFLAGS    += -I$(AM_HOME)/am/src/riscv/ysyxsoc/include
LDSCRIPTS += $(AM_HOME)/scripts/ysyxsoc_linker.ld 
# LDFLAGS   += --defsym=_pmem_start=0x30000000 --defsym=_entry_offset=0x0

LDFLAGS   += --defsym=_stack_size=2K
LDFLAGS   += --gc-sections -e _start
# LDFLAGS += -Map=$(IMAGE).map
# LDFLAGS   += -e _start

NPCFLAGS  := -l $(shell dirname $(IMAGE).elf)/ysyxsoc-log.txt
NPCFLAGS  += -e $(IMAGE).elf
NPCFLAGS  += --diff=$(NPC_HOME)/build/riscv32-nemu-interpreter-so
# NPCFLAGS += -b  # 批处理模式
MAINARGS_MAX_LEN = 64
MAINARGS_PLACEHOLDER = the_insert-arg_rule_in_Makefile_will_insert_mainargs_here
CFLAGS += -DMAINARGS_MAX_LEN=$(MAINARGS_MAX_LEN) -DMAINARGS_PLACEHOLDER=$(MAINARGS_PLACEHOLDER)

insert-arg: image
	@python $(AM_HOME)/tools/insert-arg.py $(IMAGE).bin $(MAINARGS_MAX_LEN) $(MAINARGS_PLACEHOLDER) "$(mainargs)"

#   @$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin  @$(OBJCOPY) -S -O binary $(IMAGE).elf $(IMAGE).bin
image: image-dep
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin
	

run: insert-arg
	$(MAKE) -C $(NPC_HOME) run \
		ARGS="$(NPCFLAGS)" \
		PROGRAM="$(abspath $(IMAGE).bin)"
		

step: insert-arg
	$(MAKE) -C $(NPC_HOME) step \
		PROGRAM="$(abspath $(IMAGE).bin)" \
		RUN_MODE=step \
		ARGS="$(NPCFLAGS)"

.PHONY: image insert-arg run gdb