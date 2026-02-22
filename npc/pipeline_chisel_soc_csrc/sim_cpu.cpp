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
#include "include/trace_diff.h"
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
        fprintf(stderr, "Usage: %s <program_file> [--diff=ref_so_path]\n", argv[0]);
        return 1;
    }
    
    img_path = argv[1];
    
    char *diff_so_file = NULL;
    char *trace_so_file = NULL;

    for (int i = 2; i < argc; i++) {
        if (strncmp(argv[i], "--diff=", 7) == 0) {
            diff_so_file = argv[i] + 7;
        }
    }
    trace_so_file = diff_so_file;

    #ifdef CONFIG_TRACE_DIFF
        FILE *f = fopen("tools/npc-ref.so", "r");
        if (f) {
            fclose(f);
            trace_so_file = (char*)"tools/npc-ref.so";
            printf(ANSI_GREEN "[trace_diff] Target Reference: tools/npc-ref.so\n" ANSI_RESET);
        } else {
             printf(ANSI_YELLOW "[trace_diff] tools/npc-ref.so not found. Falling back to: %s\n" ANSI_RESET, trace_so_file ? trace_so_file : "None");
        }
    #endif
    
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
        
        init_npc_cpu();
        
        init_sdb();
    
    #ifdef CONFIG_TRACE_DIFF
    if (trace_so_file) {
        extern long long getFileSize(FILE *fp);
        FILE *fp = fopen(img_path, "rb");
        assert(fp);
        long long size = getFileSize(fp);
        fclose(fp);
        trace_diff_load(trace_so_file, size);
    }
    #endif

        // Set initial state
        npc_state.state = NPC_RUNNING;
        
        #ifdef CONFIG_DIFFTEST
            extern word_t img_size;  // Should be defined in memory initialization
            difftest_init(STR2(DIFFTEST_LIB), img_size);
        #endif
        
        printf("NPC initialization completed successfully\n");
        
        // Enter main simulation loop
        sdb_mainloop();

        #ifdef CONFIG_TRACE_DIFF
        if (npc_state.state == NPC_END && npc_state.halt_ret == 0) {
            printf(ANSI_GREEN "[trace_diff] PASSED: DUT matches Reference Model execution.\n" ANSI_RESET);
        }
        #endif
        
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