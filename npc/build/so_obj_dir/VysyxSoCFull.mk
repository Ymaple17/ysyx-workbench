# Verilated -*- Makefile -*-
# DESCRIPTION: Verilator output: Makefile for building Verilated archive or executable
#
# Execute this makefile from the object directory:
#    make -f VysyxSoCFull.mk

default: /home/qiu/ysyx-workbench/npc/build/npc-ref.so

### Constants...
# Perl executable (from $PERL)
PERL = perl
# Path to Verilator kit (from $VERILATOR_ROOT)
VERILATOR_ROOT = /usr/local/share/verilator
# SystemC include directory with systemc.h (from $SYSTEMC_INCLUDE)
SYSTEMC_INCLUDE ?= 
# SystemC library directory with libsystemc.a (from $SYSTEMC_LIBDIR)
SYSTEMC_LIBDIR ?= 

### Switches...
# C++ code coverage  0/1 (from --prof-c)
VM_PROFC = 0
# SystemC output mode?  0/1 (from --sc)
VM_SC = 0
# Legacy or SystemC output mode?  0/1 (from --sc)
VM_SP_OR_SC = $(VM_SC)
# Deprecated
VM_PCLI = 1
# Deprecated: SystemC architecture to find link library path (from $SYSTEMC_ARCH)
VM_SC_TARGET_ARCH = linux

### Vars...
# Design prefix (from --prefix)
VM_PREFIX = VysyxSoCFull
# Module prefix (from --prefix)
VM_MODPREFIX = VysyxSoCFull
# User CFLAGS (from -CFLAGS on Verilator command line)
VM_USER_CFLAGS = \
	-fPIC -D_NPC_SO_BUILD \

# User LDLIBS (from -LDFLAGS on Verilator command line)
VM_USER_LDLIBS = \
	-shared \

# User .cpp files (from .cpp's on Verilator command line)
VM_USER_CLASSES = \
	cpu \
	perf \
	regs \
	device \
	keyboard \
	map \
	mmio \
	serial \
	timer \
	vga \
	ref_so \
	init \
	memory \
	expr \
	sdb \
	state \
	watchpoint \
	mtrace \

# User .cpp directories (from .cpp's on Verilator command line)
VM_USER_DIR = \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/cpu \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/difftest \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/init \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/memory \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/monitor \
	/home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/trace \


### Default rules...
# Include list of all generated classes
include VysyxSoCFull_classes.mk
# Include global rules
include $(VERILATOR_ROOT)/include/verilated.mk

### Executable rules... (from --exe)
VPATH += $(VM_USER_DIR)

cpu.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/cpu/cpu.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
perf.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/cpu/perf.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
regs.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/cpu/regs.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
device.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/device.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
keyboard.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/keyboard.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
map.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/map.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
mmio.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/mmio.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
serial.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/serial.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
timer.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/timer.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
vga.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/device/vga.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
ref_so.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/difftest/ref_so.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
init.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/init/init.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
memory.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/memory/memory.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
expr.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/monitor/expr.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
sdb.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/monitor/sdb.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
state.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/monitor/state.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
watchpoint.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/monitor/watchpoint.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<
mtrace.o: /home/qiu/ysyx-workbench/npc/pipeline_chisel_soc_csrc/trace/mtrace.cpp
	$(OBJCACHE) $(CXX) $(CXXFLAGS) $(CPPFLAGS) $(OPT_FAST) -c -o $@ $<

### Link rules... (from --exe)
/home/qiu/ysyx-workbench/npc/build/npc-ref.so: $(VK_USER_OBJS) $(VK_GLOBAL_OBJS) $(VM_PREFIX)__ALL.a $(VM_HIER_LIBS)
	$(LINK) $(LDFLAGS) $^ $(LOADLIBES) $(LDLIBS) $(LIBS) $(SC_LIBS) -o $@


# Verilated -*- Makefile -*-
