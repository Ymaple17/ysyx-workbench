TOPNAME = top
NXDC_FILES = constr/top.nxdc	
INC_PATH ?=	
VERILATOR = verilator
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert
 
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir	#OBJ_DIR = ./build/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)	#BIN = ./build/top
 
default: $(BIN)	#终极目标为default，依赖./build/top
$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!  
$(shell mkdir -p $(BUILD_DIR))
 
# constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@
 
# project source
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRCS += $(SRC_AUTO_BIND)
# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk
# 包含NVBoard相关Makelile规则
 
# rules for verilator
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""
LDFAGS += -lSDL2 -lSDL2_image

$(BIN): $(VSRCS) $(CSRCS) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR) # 生成新的对象之前，删除旧的对象文件
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))
 
all: default
 
run: $(BIN)
	@$^
 
clean:
	rm -rf $(BUILD_DIR)
 
.PHONY: default all clean run


	
