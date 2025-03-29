# ------------------------ bin2hex ----------------------- #
CROSS_COMPILE ?= riscv64-linux-gnu-
CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
OBJDUMP = $(CROSS_COMPILE)objdump
LD = $(CROSS_COMPILE)ld
BUILD_DIR = ./build

IMG_NAME = img
OUTPUT = $(NPC_HOME)/$(IMG_NAME).bin
OBJS = $(BUILD_DIR)/$(IMG_NAME).o

$(shell mkdir -p $(BUILD_DIR))

$(OUTPUT) : $(BUILD_DIR)/$(IMG_NAME).elf
	$(OBJCOPY) -O binary $< $@

$(BUILD_DIR)/$(IMG_NAME).elf: $(OBJS)
	$(LD) -Ttext=0 -nostdlib -o $(BUILD_DIR)/$(IMG_NAME).elf $<
	$(OBJDUMP) -M no-aliases $@ -d > $(BUILD_DIR)/$(IMG_NAME).txt

$(OBJS): $(IMG_NAME).S
	$(CC) -march=rv64g -c $< -o $@

all: $(OUTPUT)

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: all clean