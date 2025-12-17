#include "VysyxSoCFull.h"
#include <stdint.h>
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../../obj_dir/VysyxSoCFull___024root.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../../include/generated/autoconf.h"

#ifdef CONFIG_NVBOARD
#include <nvboard.h>
#endif

#define MAX_INST_TO_PRINT 10
CPU_State cpu;
extern VysyxSoCFull* top;
bool rst_done = false;
#define WAVE_time 0

#ifdef ENABLE_WAVEFORM
extern VerilatedFstC* tfp;
#else
extern void* tfp;
#endif

static vluint64_t main_time = 0;
#define start_time 10
static bool g_print_step = false;
 
extern "C" void sim_exit(){
  set_npc_state(NPC_END, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc, cpu_gpr[10]);
}

static inline bool in_pmem(uint32_t addr) {
  return addr - 0x80000000 < 0x8000000;
}

void single_cycle() {
    if (!Verilated::gotFinish()) {
        // Rising edge
        top->clock = 1;
        top->eval();
        
        #ifdef ENABLE_WAVEFORM
            if (tfp != nullptr) {
                tfp->dump(main_time);
            }
        #endif
        main_time++;
        
        // Falling edge
        top->clock = 0;
        top->eval();
        
        #ifdef ENABLE_WAVEFORM
            if (tfp != nullptr) {
                tfp->dump(main_time);
            }
        #endif
        main_time++;
    }
}

void reset() { 
    // Assert reset signals
    top->reset = 1;
    
    // Run reset cycles
    for (int i = 0; i < start_time; i++) {
        single_cycle();
    }
    
    // Deassert reset signals
    top->reset = 0;
    
    rst_done = true;
}

void init_npc_cpu(){
    reset();
}

void exec_once(){
    if (!rst_done) {
        printf("Warning: exec_once called before reset completion\n");
        return;
    }
    
    // Capture state before execution
    cpu.pc = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc;
    
    #ifdef CONFIG_ITRACE 
        uint32_t current_inst = top->instr;
        itrace_inst(top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc, current_inst);
        display_inst();
    #endif
    
    // Update register file
    for(int i = 0; i < 32; i++){
        cpu.gpr[i] = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__regs[i];
    }

    #ifdef CONFIG_NVBOARD
        nvboard_update();
    #endif

    // Execute one instruction cycle
    single_cycle();
    
    // Update PC after execution
    cpu.pc = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc;
}

void device_update();

static void execute(uint64_t n) {
    for (; n > 0; n--) {
        if (!Verilated::gotFinish()) {
            exec_once();
            
            #ifdef CONFIG_DEVICE
              device_update();
            #endif

            #ifdef CONFIG_DIFFTEST
                if (top->rootp->top__DOT__wb_valid && !top->reset) {
                    if (top->wen && !in_pmem(top->mem_addr)) {
                        difftest_skip_ref();
                    } else { 
                        difftest_one_exec(); 
                        if (!difftest_check_reg()) { 
                            panic("DiffTest: Register mismatch detected at PC = 0x%08x", cpu.pc);
                        }
                    }             
                    }
            #endif
        }
        
        // if (g_print_step) {
        //     printf("execute at pc = 0x%08x\n", top->imem_pc);
        // }
        
        if (npc_state.state != NPC_RUNNING) {
            break;
        }
    }
}

void cpu_exec(uint64_t n){
    g_print_step = (n < MAX_INST_TO_PRINT);

    switch (npc_state.state){
        case NPC_END: case NPC_ABORT:
            printf("Program execution has ended. To restart the program, exit NPC and run again.\n");
            printf("halt_pc = 0x%x halt_ret = %d\n", npc_state.halt_pc, npc_state.halt_ret);
            return;
        default: 
            npc_state.state = NPC_RUNNING;
    }

    execute(n);
    
    char *out = NULL;
    switch(npc_state.state){
        case NPC_RUNNING: 
            out = (char *)"stop"; 
            npc_state.state = NPC_STOP; 
            break;
        case NPC_END: 
            if(npc_state.halt_ret == 0){
                out = (char *)"HIT GOOD TRAP"; 
                #ifdef CONFIG_DIFFTEST
                    difftest_one_exec();
                #endif
                printf("npc: " ANSI_FG_GREEN "%s" ANSI_RESET " at pc = 0x%08x\n", 
                       out, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc); 
            }
            else{
                out = (char *)"ABORT";
                #ifdef CONFIG_DIFFTEST
                    difftest_one_exec(); 
                #endif
                printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                       out, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc); 
            }
            break;
        case NPC_ABORT: 
            out = (char *)"ABORT"; 
            printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                   out, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc); 
            break;
        default: 
            out = (char *)"HIT BAD TRAP"; 
            printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                   out, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc); 
            break;
    }
}