#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include "verilated_fst_c.h"
#include "VysyxSoCFull.h"
#include "svdpi.h"
#include "include/common.h"
#include "include/init.h"
#include "include/memory.h"
#include "include/sdb.h"
#include "include/state.h"
#include "include/cpu.h"
#include "include/difftest.h"
#include "../include/generated/autoconf.h"

#ifdef CONFIG_NVBOARD
#include <nvboard.h>
extern void nvboard_bind_all_pins(VysyxSoCFull* top);
#endif

void difftest_init(const char* ref_so_file, word_t img_size);
const char* img_path = NULL;
void init_disasm();

// Global objects
VysyxSoCFull *top = new VysyxSoCFull("top");

#ifdef ENABLE_WAVEFORM
VerilatedFstC* tfp = new VerilatedFstC;
#else
void* tfp = nullptr;
#endif

// Cleanup function
void cleanup() {
    if (top) {
        top->final();
        delete top;
        top = nullptr;
    }
    
    #ifdef ENABLE_WAVEFORM
    if (tfp) {
        tfp->close();
        delete tfp;
        tfp = nullptr;
    }
    #endif
}

int main(int argc, char **argv){
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <program_file>\n", argv[0]);
        return 1;
    }
    
    img_path = argv[1];
    
    // Initialize Verilator
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true);  // Enable tracing before any simulation
    
    #ifdef CONFIG_NVBOARD
    nvboard_bind_all_pins(top);
    nvboard_init();
    #endif

    #ifdef ENABLE_WAVEFORM
        if (tfp) {
            top->trace(tfp, 99);  // Trace 99 levels of hierarchy
            tfp->open("wave.fst");
            printf("Waveform tracing enabled, output: wave.fst\n");
        }
    #endif
    
    // Initialize system state
    set_npc_state(NPC_STOP, 0x30000000, 0);
    
    try {
        // Initialize components in correct order
        init_mem();
        
        #ifdef CONFIG_ITRACE
            init_disasm();
        #endif
        
        #ifdef CONFIG_DEVICE
            init_device();
        #endif
        
        // Initialize and reset CPU
        init_npc_cpu();
        
        // Initialize SDB (Simple Debugger)
        init_sdb();
        
        // Set initial state
        npc_state.state = NPC_RUNNING;
        
        #ifdef CONFIG_DIFFTEST
            extern word_t img_size;  // Should be defined in memory initialization
            difftest_init(STR2(DIFFTEST_LIB), img_size);
        #endif
        
        printf("NPC initialization completed successfully\n");
        
        // Enter main simulation loop
        sdb_mainloop();
        
    } catch (const std::exception& e) {
        fprintf(stderr, "Exception during simulation: %s\n", e.what());
        cleanup();
        return 1;
    } catch (...) {
        fprintf(stderr, "Unknown exception during simulation\n");
        cleanup();
        return 1;
    }
    
    // Clean shutdown
    cleanup();
    printf("Simulation completed successfully\n");
    return 0;
}