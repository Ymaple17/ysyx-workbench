#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../include/common.h"
#include "../include/trace_diff.h"
#include "../include/state.h"
#include "../include/regs.h"
#include "../include/difftest.h"

static void (*chk_init)(int) = NULL;
static void (*chk_memcpy)(uint32_t, void*, uint32_t, bool) = NULL;
static void (*chk_regcpy)(void*, bool) = NULL;
static void (*chk_exec)(uint64_t) = NULL;
static uint32_t (*chk_read_signal)(const char*) = NULL;

static void* handle = NULL;
static bool trace_diff_enabled = false;

// We need a separate CPU state struct for the check model
static CPU_State chk_state;

// Define a structure for registered signals
#define MAX_TRACE_SIGNALS 32
struct TraceSignal {
    const char* name;
    uint32_t* dut_ptr;
};
static struct TraceSignal trace_signals[MAX_TRACE_SIGNALS];
static int trace_signal_count = 0;


// Hardcoded for now, or move to common
#define DIFFTEST_TO_DUT 0
#define DIFFTEST_TO_REF 1 

void trace_diff_init() {
    trace_diff_enabled = false;
    chk_init = NULL;
    chk_memcpy = NULL;
    chk_regcpy = NULL;
    chk_exec = NULL;
}

void trace_diff_load(const char *ref_so_file, long long img_size) {
    if (!ref_so_file) return;

    printf(ANSI_YELLOW "[trace_diff] Loading reference SO: %s\n" ANSI_RESET, ref_so_file);
    
    if (handle) return; // Already loaded

    handle = dlopen(ref_so_file, RTLD_LAZY);
    if (!handle) {
        // Try absolute path if relative failed, or just panic
        panic("Failed to load reference SO: %s\nError: %s", ref_so_file, dlerror());
    }

    chk_init = (void (*)(int)) dlsym(handle, "ref_so_init");
    chk_memcpy = (void (*)(uint32_t, void*, uint32_t, bool)) dlsym(handle, "ref_so_memcpy");
    chk_regcpy = (void (*)(void*, bool)) dlsym(handle, "ref_so_regcpy");
    chk_exec = (void (*)(uint64_t)) dlsym(handle, "ref_so_exec");
    chk_read_signal = (uint32_t (*)(const char*)) dlsym(handle, "ref_so_read_signal");

    if (!chk_init || !chk_memcpy || !chk_regcpy || !chk_exec) {
        panic("Failed to resolve symbols in reference SO. Ensure it implements ref_so_* interface.");
    }

    // Initialize the reference model
    chk_init(0);
    
    extern uint8_t *flash_guest_to_host(uint64_t paddr);
    #define CONFIG_FLASH_BASE 0x30000000
    chk_memcpy(CONFIG_FLASH_BASE, flash_guest_to_host(CONFIG_FLASH_BASE), img_size, DIFFTEST_TO_REF);
    
    extern uint32_t cpu_pc;
    // extern uint32_t cpu_gpr[32];
    
    chk_state.pc = cpu_pc;
    for(int i=0; i<16; i++) chk_state.gpr[i] = cpu_gpr[i];
    
    // Sync state TO the reference model
    chk_regcpy(&chk_state, DIFFTEST_TO_REF);

    trace_diff_enabled = true;
    printf(ANSI_GREEN "[trace_diff] Reference model initialized successfully.\n" ANSI_RESET);
}

void trace_diff_check_inst(uint64_t n) {
    if (!trace_diff_enabled) return;

    // 1. Step the Reference (Instruction based now)
    chk_exec(n);

    chk_regcpy(&chk_state, DIFFTEST_TO_DUT); 

    // 3. Compare with DUT State (Architectural)
    extern uint32_t cpu_pc;
    // extern uint32_t cpu_gpr[32];
    if (cpu_pc != chk_state.pc) {
        printf(ANSI_RED "[trace_diff] PC Mismatch! \n" ANSI_RESET);
        printf("Expected (Ref WB): " ANSI_GREEN "0x%08x" ANSI_RESET "\n", chk_state.pc);
        printf("Actual   (DUT WB): " ANSI_RED "0x%08x" ANSI_RESET "\n", cpu_pc);
        npc_state.state = NPC_ABORT;
    }
    
    for (int i = 0; i < 16; i++) {
        if (cpu_gpr[i] != chk_state.gpr[i]) {
             printf(ANSI_RED "[trace_diff] GPR[%d] Mismatch! \n" ANSI_RESET, i);
             printf("Expected: 0x%08x, Actual: 0x%08x\n", chk_state.gpr[i], cpu_gpr[i]);
             npc_state.state = NPC_ABORT;
             break;
        }
    }
    
    // 4. Compare Custom Signals
    // We only compare if we are sure phases align.
    if (chk_read_signal) {
        for (int i = 0; i < trace_signal_count; i++) {
            // SKIP PC_IF check for now as it is phase-sensitive
            if (strcmp(trace_signals[i].name, "PC_IF") == 0) continue; 
            
            uint32_t ref_val = chk_read_signal(trace_signals[i].name);
            uint32_t dut_val = *trace_signals[i].dut_ptr;
            
            if (dut_val != ref_val) {
                 printf(ANSI_RED "[trace_diff] Signal '%s' Mismatch!\n" ANSI_RESET, trace_signals[i].name);
                 printf("Expected (Ref): 0x%08x\n", ref_val);
                 printf("Actual   (DUT): 0x%08x\n", dut_val);
                 npc_state.state = NPC_ABORT;
                 break;
            }
        }
    }
}

void trace_diff_register(const char *name, uint32_t *ptr) {
    if (trace_signal_count >= MAX_TRACE_SIGNALS) {
        printf("[trace_diff] Warning: Max signals limit reached, ignoring %s\n", name);
        return;
    }
    trace_signals[trace_signal_count].name = strdup(name);
    trace_signals[trace_signal_count].dut_ptr = ptr;
    trace_signal_count++;
}

