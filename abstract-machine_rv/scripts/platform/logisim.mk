AM_SRCS := platform/logisim/trm.c \
           platform/logisim/ioe.c \
           platform/nemu/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
CFLAGS    += -I$(AM_HOME)/am/src/platform/nemu/include
LDSCRIPTS += $(AM_HOME)/scripts/linker.ld
LDFLAGS   += --defsym=_pmem_start=0x0 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start

image: image-dep
	@$(OBJDUMP) -M no-aliases -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin
	python $(AM_HOME)/tools/logisim-ysyx-img.py $(IMAGE).bin

MININPCFLAGS  := -l $(shell dirname $(IMAGE).elf)/npc-log.txt
MININPCFLAGS  += -e $(IMAGE).elf
MININPCFLAGS  += --diff=$(NPC_HOME)/build/riscv32-nemu-interpreter-so

run: insert-arg
	$(MAKE) -C $(NPC_HOME) minirun \
		ARGS="$(MININPCFLAGS)" \
		PROGRAM="$(abspath $(IMAGE).bin)" \
		

step: insert-arg
	$(MAKE) -C $(NPC_HOME) ministep \
		ARGS="$(MININPCFLAGS)" \
		PROGRAM="$(abspath $(IMAGE).bin)" \
		RUN_MODE=step 
		

.PHONY: image insert-arg run gdb