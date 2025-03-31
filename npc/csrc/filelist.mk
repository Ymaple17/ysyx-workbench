CSRCS += $(NPC_HOME)/csrc/npc_main.cpp
CSRCS += $(NPC_HOME)/csrc/src/sim_init.cpp
DIRS += $(NPC_HOME)/csrc/src/cpu 
DIRS += $(NPC_HOME)/csrc/src/monitor
DIRS += $(NPC_HOME)/csrc/src/utils
DIRS += $(NPC_HOME)/csrc/src/engine
DIRS += $(NPC_HOME)/csrc/src/memory

ifdef mainargs
ASFLAGS += -DBIN_PATH=\"$(mainargs)\"
endif
