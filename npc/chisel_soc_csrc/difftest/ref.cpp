#include <dlfcn.h>
#include <stdio.h>
#include <assert.h>
#include "VysyxSoCFull.h"
#include "VysyxSoCFull___024root.h"
#include "verilated.h"
#include "../include/common.h"
#include "../include/state.h"
#include "../include/difftest.h"

// External references to simulation objects
// We need to define them here because we are not linking with sim_cpu.cpp
VysyxSoCFull *top = NULL;
VerilatedContext* contextp = NULL;

// Memory simulation references from memory.cpp
// We can link with memory.cpp, so these will be available
extern uint8_t pmem[];
extern uint8_t flash[];

// Helper to access registers from top
// Adapted from regs.cpp
static uint32_t get_gpr(int idx) {
    if (idx == 0) return 0;
    return top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__rf_ext__DOT__Memory[idx];
}

static uint32_t get_pc() {
    return top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;
}

static inline bool in_flash(uint32_t addr) {
    return addr - 0x30000000 < 0x01000000;
}

extern "C" {

void difftest_init(int port) {
    // Initialize Verilator logic
    contextp = new VerilatedContext;
    top = new VysyxSoCFull{contextp};
    
    // Reset sequence
    top->reset = 1;
    top->clock = 0; top->eval();
    top->clock = 1; top->eval();
    top->clock = 0; top->eval();
    top->clock = 1; top->eval();
    top->reset = 0;
    
    printf("[npc-ref] Golden NPC initialized\n");
}

void difftest_exec(uint64_t n) {
    while(n > 0) {
        top->clock = 0; top->eval();
        top->clock = 1; top->eval();
        n--;
    }
}

void difftest_raise_intr(uint64_t NO) {
    printf("[npc-ref] difftest_raise_intr not implemented\n");
}

void difftest_regcpy(void *dut, bool direction) {
    CPU_State *s = (CPU_State*)dut;
    if (direction == DIFFTEST_TO_REF) {
        // Copy DUT -> REF
        // For NPC, this is hard because we can't easily force internal signals
        // in Verilator unless there are DPI-C setters or we wrote to signals directly.
        // For now, we only support REF -> DUT (Checking)
        // Or we warn.
        // But standard difftest flow usually syncs registers on start or events.
        // If we strictly check per instruction, we might only read REF.
    } else {
        // Copy REF -> DUT
        s->pc = get_pc();
        for (int i = 0; i < 32; i++) {
            s->gpr[i] = get_gpr(i);
        }
    }
}

void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
    // We assume the memory map matches typical NPC setup
    // 0x80000000 -> PMEM
    // 0x30000000 -> FLASH
    
    if (direction == DIFFTEST_TO_REF) { // Sync to REF
        uint8_t *dest = NULL;
        if (addr >= 0x80000000 && addr < 0x88000000) {
            // PMEM (Not implemented fully in this snippet, relying on memory.cpp logic if linked)
            // But if we use default memory.cpp, 'pmem' is accessible.
            // CAUTION: memory.cpp might rely on top->... for reading? No, usually arrays.
            // Let's assume standard pmem array.
            // But we can't see the array if it's static in memory.cpp?
            // User context showed memory.cpp has 'extern uint8_t pmem[]', let's use it.
            // Wait, memory.cpp usually defines it.
            // We need to check if memory.cpp exposes pmem.
            // Assuming 0x80000000 base
            // dest = pmem + (addr - 0x80000000); // Need to verify PMEM mapping
        } 
        else if (in_flash(addr)) {
             // flash is extern
             dest = flash + (addr - 0x30000000);
             memcpy(dest, buf, n);
        }
        else {
             // printf("[npc-ref] memcpy to unknown addr %x ignored\n", addr);
        }
    }
}

}
