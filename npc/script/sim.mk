CSRCS += $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRCS += $(abspath ./npc-main.cpp)
INC_PATH = $(abspath ./csrc/include)
CFLAGS += -I$(INC_PATH)
CFLAGS += $(shell llvm-config --cxxflags) -fPIE # llvm
CFLAGS := $(filter-out -D__STDC_FORMAT_MACROS, $(CFLAGS)) # lib verilator has define this macro
LDFLAGS += -lreadline $(shell llvm-config --libs) # llvm
LDFLAGS += -lSDL2 # sdl