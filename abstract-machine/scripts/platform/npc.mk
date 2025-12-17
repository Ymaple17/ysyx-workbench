AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe.c \
           riscv/npc/timer.c \
           riscv/npc/input.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
CFLAGS    += -I$(AM_HOME)/am/src/riscv/npc/include
LDFLAGS   += -T $(AM_HOME)/scripts/linker.ld \
             --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0 \
             --gc-sections -e _start

NPCFLAGS  := -l $(shell dirname $(IMAGE).elf)/npc-log.txt
NPCFLAGS  += -e $(IMAGE).elf
NPCFLAGS  += --diff=$(NPC_HOME)/build/riscv32-nemu-interpreter-so
# NPCFLAGS += -b  # 批处理模式
MAINARGS_MAX_LEN = 64
MAINARGS_PLACEHOLDER = the_insert-arg_rule_in_Makefile_will_insert_mainargs_here
CFLAGS += -DMAINARGS_MAX_LEN=$(MAINARGS_MAX_LEN) -DMAINARGS_PLACEHOLDER=$(MAINARGS_PLACEHOLDER)

insert-arg: image
	@python $(AM_HOME)/tools/insert-arg.py $(IMAGE).bin $(MAINARGS_MAX_LEN) $(MAINARGS_PLACEHOLDER) "$(mainargs)"

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