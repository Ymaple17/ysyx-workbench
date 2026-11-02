#include <dlfcn.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "VysyxSoCFull.h"
#include "VysyxSoCFull___024root.h"
#include "verilated.h"
#include "../include/common.h"
#include "../include/state.h"
#include "../include/difftest.h"

VysyxSoCFull *top = NULL;
VerilatedContext* contextp = NULL;

extern uint8_t pmem[];
extern uint8_t flash[];

const char* img_path = NULL;

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

void ref_so_init(int port) {
    contextp = new VerilatedContext;
    top = new VysyxSoCFull{contextp};
    
    // Reset sequence
    top->reset = 1;
    top->reset = 1;
    for(int i=0; i<10; i++) {
        top->clock = 0; top->eval();
        top->clock = 1; top->eval();
    }
    top->reset = 0;
    
    printf("[npc-ref] Golden NPC initialized (Internal SO)\n");
}

void ref_so_exec(uint64_t n) {
    // Run until 'n' instructions commit
    while(n > 0) {
        bool committed = false;
        int max_cycles = 100000; // Timeout to prevent infinite loop
        
        while (!committed && max_cycles > 0) {
            top->clock = 1; top->eval();
            top->clock = 0; top->eval();
            
            // Check commit signal (WBU valid)
            if (top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid) {
                committed = true;
            }
            max_cycles--;
        }
        
        if (!committed) {
             printf("[npc-ref] Error: Reference model timeout (no commit in %d cycles)\n", 100000);
             break;
        }
        n--;
    }
}

void ref_so_raise_intr(uint64_t NO) {
}

void ref_so_regcpy(void *dut, bool direction) {
    CPU_State *s = (CPU_State*)dut;
    if (direction == DIFFTEST_TO_REF) {
        top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc = s->pc;
        
        top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__pc_reg = s->pc;

        for (int i=1; i<32; i++) {
            top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__refile__DOT__rf_ext__DOT__Memory[i] = s->gpr[i];
        }
    } else {
        // Copy REF -> DUT (Checking)
        s->pc = get_pc();
        for (int i = 0; i < 32; i++) {
            s->gpr[i] = get_gpr(i);
        }
    }
}

void ref_so_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
    if (direction == DIFFTEST_TO_REF) { // Sync to REF
        if (in_flash(addr)) {
             uint8_t *dest = flash + (addr - 0x30000000);
             memcpy(dest, buf, n);
        } else if (addr >= 0x80000000 && addr < 0x88000000) {
             uint8_t *dest = pmem + (addr - 0x80000000);
             memcpy(dest, buf, n);
        }
    }
}

// Interface to read arbitrary signals by name
uint32_t ref_so_read_signal(const char* name) {
    if (!top || !top->rootp) return 0;

    // Add your custom signal mappings here
    if (strcmp(name, "PC_WB") == 0) {
        return top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;
    }
    if (strcmp(name, "PC_IF") == 0) {
        return top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_out_bits_pc;
    }
    if (strcmp(name, "alu_result") == 0) {
        return top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_alu_result;
    }

    printf("[npc-ref] Warning: Unknown signal read request: %s\n", name);
    return 0;
}

}
