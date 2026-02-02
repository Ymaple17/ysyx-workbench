#include "Vysyx_25020039.h"
#include <stdint.h>
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../../obj_dir/Vysyx_25020039___024root.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../include/perf.h"
#include "../../include/generated/autoconf.h"

#ifdef CONFIG_NVBOARD
#include <nvboard.h>
#endif

#define MAX_INST_TO_PRINT 10
CPU_State cpu;
extern Vysyx_25020039* top;
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
    print_perf_stats(main_time / 2);
    uint32_t pc_val = read_pc_from_top();
    uint32_t a0_val = read_gpr_from_top(10);
    cpu.pc = pc_val;
    cpu.gpr[10] = a0_val;
    cpu_pc = pc_val;
    cpu_gpr[10] = a0_val;
    set_npc_state(NPC_END, pc_val, a0_val);
}

static inline bool in_flash(uint32_t addr) {
    return addr - 0x30000000 < 0x01000000;
}

static inline bool in_sram(uint32_t addr) {
    return addr - 0x0f000000 < 0x00002000;
}

static inline bool in_sdram(uint32_t addr) {
    return addr - 0xa0000000 < 0x04000000;
}

static inline bool in_psram(uint32_t addr) {
    return addr - 0x80000000 < 0x00400000;
}

static inline bool is_mmio_addr(uint32_t addr) {
    return !in_flash(addr) && !in_sram(addr) && !in_sdram(addr) && !in_psram(addr);
}


static inline void update_cpu_state() {
    cpu.pc = read_pc_from_top();
    cpu_pc = cpu.pc;
    for(int i = 0; i < 32; i++){
        uint32_t val = read_gpr_from_top(i);
        cpu.gpr[i] = val;
        cpu_gpr[i] = val;
    }
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
    top->reset = 1;
    
    for (int i = 0; i < start_time; i++) {
        single_cycle();
    }
    
    top->reset = 0;
    
    rst_done = true;
}

void init_npc_cpu(){
    reset();
    
    update_cpu_state();
}

void exec_once(){
    if (!rst_done) {
        printf("Warning: exec_once called before reset completion\n");
        return;
    }
    
    #ifdef CONFIG_ITRACE 
        uint32_t current_inst = top->instr;
        itrace_inst(top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc, current_inst);
        display_inst();
    #endif

    #ifdef CONFIG_NVBOARD
        nvboard_update();
    #endif

    // Execute one instruction cycle
    single_cycle();
    
    // Update CPU state after execution
    update_cpu_state();
}

void device_update();

#ifdef CONFIG_DIFFTEST
static bool difftest_step(bool wb_valid, bool lsu_mem_write, word_t mem_addr) {
    if (!wb_valid) {
        return true;
    }
    
    bool is_mmio_write = false;
    if (lsu_mem_write) {
        if (is_mmio_addr(mem_addr)) {
            is_mmio_write = true;
        }
    }
    
    if (is_mmio_write) {
        difftest_skip_ref();
        return true;
    } else {
        difftest_one_exec();
        
        if (!difftest_check_reg()) {
            printf("\n");
            printf("[difftest]========================================\n");
            printf("[difftest]  DIFFTEST FAILED\n");
            printf("[difftest]========================================\n");
            printf("[difftest] Instruction committed at PC = 0x%08x\n", cpu.pc);
            printf("[difftest]========================================\n");
            printf("\n");
            return false;
        }
        return true;
    }
}
#endif

static void execute(uint64_t n) {
    for (; n > 0; n--) {
        if (!Verilated::gotFinish()) {
            #ifdef CONFIG_DIFFTEST
            bool reset = top->reset;
            #endif

            exec_once();
            
            #ifdef CONFIG_DEVICE
                device_update();
            #endif

            #ifdef CONFIG_DIFFTEST
            bool wb_valid = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__lsu_wb_valid;
            bool lsu_mem_write = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__exu_lsu_MemWrite;
            word_t mem_addr = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__exu_lsu_process_result;    
            if (!reset && !difftest_step(wb_valid, lsu_mem_write, mem_addr)) {
                panic("[difftest] Register mismatch detected at PC = 0x%08x", cpu.pc);
            }
            #endif
        }
        
        // if (g_print_step) {
        //     printf("execute at pc = 0x%08x\n", top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc);
        // }
        
        if (npc_state.state != NPC_RUNNING) {
            break;
        }
    }
}

void cpu_exec(uint64_t n){
    g_print_step = (n < MAX_INST_TO_PRINT);

    switch (npc_state.state){
        case NPC_END: 
        case NPC_ABORT:
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
                    if (!difftest_check_reg()) {
                        printf("Warning: Final difftest check failed\n");
                    }
                #endif
                printf("npc: " ANSI_FG_GREEN "%s" ANSI_RESET " at pc = 0x%08x\n", 
                       out, read_pc_from_top());
            }
            else{
                out = (char *)"ABORT";
                #ifdef CONFIG_DIFFTEST
                    difftest_one_exec(); 
                #endif
                printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                       out, read_pc_from_top()); 
            }
            break;
            
        case NPC_ABORT: 
            out = (char *)"ABORT"; 
            printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                   out, read_pc_from_top()); 
            break;
            
        default: 
            out = (char *)"HIT BAD TRAP"; 
            printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
                   out, read_pc_from_top()); 
            break;
    }
}