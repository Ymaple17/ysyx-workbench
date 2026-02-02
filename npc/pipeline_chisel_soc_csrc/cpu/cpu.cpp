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
#include "../include/perf.h"
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
extern void (*ref_difftest_regcpy)(void *dut, bool direction);

static void check_pc_and_sync(word_t wbu_pc) {
    CPU_State ref_state;
    ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);
    if (wbu_pc != ref_state.pc) {
        printf("\n[difftest] ========== PC MISMATCH (Fetch/Commit) ==========\n");
        printf("[difftest] PC: REF = 0x%08x, DUT = 0x%08x\n", ref_state.pc, wbu_pc);
        printf("[difftest] ================================================\n");
        npc_state.state = NPC_ABORT;
    }
}

struct DiffTestState {
    bool check_pending;
    bool is_mmio;
} dt_state = {false, false};

#endif

static void execute(uint64_t n) {
    for (; n > 0; n--) {
        if (npc_state.state != NPC_RUNNING) break;

        exec_once();

        #ifdef CONFIG_DEVICE
        device_update();
        #endif

        if (Verilated::gotFinish()) {
            break;
        }

        #ifdef CONFIG_DIFFTEST
        bool reset = top->reset;
        
        // 1. Deferred Check (Verify result of previous instruction)
        if (!reset && dt_state.check_pending) {
            if (dt_state.is_mmio) {
                difftest_skip_ref();
                difftest_one_exec();
            }

            // Sync DUT PC to Ref PC to prevent mismatches caused by pipeline bubbles
            CPU_State ref_state;
            ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);
            cpu.pc = ref_state.pc;
            cpu_pc = ref_state.pc;

            if (!difftest_check_reg()) {
                npc_state.state = NPC_ABORT;
                break;
            }
            dt_state.check_pending = false;
        }

        // 2. Current Commit (Capture instruction)
        bool wb_valid = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid;
        if (wb_valid && !reset) {
            word_t wbu_pc = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;
            word_t wbu_alu = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_alu_result;
            bool lsu_mem_write = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write;
            uint8_t wbu_reg_write_sel = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_signals_wbu_reg_write_sel;
            bool is_load_wb = (wbu_reg_write_sel == 4);

            check_pc_and_sync(wbu_pc);
            if (npc_state.state == NPC_ABORT) break;

            dt_state.is_mmio = (lsu_mem_write || is_load_wb) && is_mmio_addr(wbu_alu);
            
            if (!dt_state.is_mmio) {
                difftest_one_exec();
            }
            dt_state.check_pending = true;
        }
        #endif
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