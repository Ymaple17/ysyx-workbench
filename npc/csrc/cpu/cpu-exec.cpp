#include <cpu/cpu.h>
#include <cpu/difftest.h>
#include <mem/paddr.h>
#include <module.h>
#include <tracer/inst_tracer.h>
#include <tracer/wave_tracer.h>
#include <mem/mem.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include <svdpi.h>
#include "VTop.h"
#include "sim.h"

extern word_t paddr_read_c(paddr_t addr, int len);
extern void paddr_write_c(paddr_t addr, int len, word_t data);

#define MAX_INST_TO_PRINT 10

//void device_update();
int wp_difftest();

CPU_state cpu;
static bool g_print_step = false;
static uint64_t cycle_cnt = 0;
static uint64_t inst_cnt = 0;
static uint64_t commit_cnt = 0;
int device_access_st = 0;

mem_t mem;
mem_t mem2;

static void single_cycle() {
    a_single_cycle();
    cycle_cnt++;
}

// DPI 函数保持不变
extern "C" void dpic_ebreak(const svBitVecVal* pc, const svBitVecVal* code) {
    uint32_t pc_val = *pc;
    uint32_t code_val = *code;
    npc_state.state = NPC_END;
    npc_state.halt_pc = pc_val;
    npc_state.halt_code = code_val;
}

#ifdef CONFIG_FTRACE
void trace_func_call(paddr_t pc, paddr_t target, bool is_tail);
void trace_func_ret(paddr_t pc);

extern "C" void dpic_ftrace(paddr_t pc, paddr_t dnpc, uint8_t dest, word_t imm, uint8_t is_ret) {
    if (is_ret) {
        trace_func_ret(pc);
    } else if (dest == 0) {
        trace_func_call(pc, dnpc, true);
    } else if (dest == 1) {
        trace_func_call(pc, dnpc, false);
    }
}
#endif

extern "C" void dpic_commit(const svBitVecVal* pc, const svBitVecVal* inst, const svBitVecVal* npc, char dm_access) {
    uint64_t pc_val = *(uint32_t*)pc;
    uint32_t inst_val = *inst;
    uint64_t npc_val = *(uint32_t*)npc;
    commit_cnt--;
    inst_cnt++;
    cpu.pc = npc_val;

    if (device_access_st && dm_access) {
        device_access_st--;
        //difftest_skip_ref();
    }

    IFDEF(CONFIG_ITRACE, itracer.trace(pc_val, inst_val, g_print_step));
    IFDEF(CONFIG_WATCHPOINT, if (wp_difftest()) npc_state.state = NPC_STOP);
    //device_update();
    //IFDEF(CONFIG_DIFFTEST, difftest_step(pc_val));
}

extern "C" void paddr_read(const svBitVecVal* addr, svBitVecVal* data) {
    uint32_t addr_val = *addr;
    uint32_t data_val = paddr_read_c(addr_val, 4);
    *data = data_val;
}

extern "C" void paddr_write(const svBitVecVal* addr, const svBitVecVal* data, char mask) {
    uint32_t addr_val = *addr;
    if (addr_val < 0x80000000 || addr_val >= 0x88000000) {
        fprintf(stderr, "ERROR: Invalid paddr_write address: 0x%08x\n", addr_val);
        npc_state.state = NPC_ABORT;
        npc_state.halt_pc = addr_val;
        npc_state.halt_code = 1;
        return;
    }
    uint32_t data_val = *data;
    int len;
    switch (mask) {
        case 0x01: len = 1; break;
        case 0x03: len = 2; break;
        case 0x0f: len = 4; break;
        case 0x00: return;
        default:
            fprintf(stderr, "ERROR: paddr_write wrong mask: 0x%02x\n", mask);
            npc_state.state = NPC_ABORT;
            npc_state.halt_pc = addr_val;
            npc_state.halt_code = 1;
            return;
    }
    paddr_write_c(addr_val, len, data_val);
}

static void exec(uint64_t n) {
    commit_cnt = n;
    if (top->reset == 1) {
        top->reset = 0;
        top->eval();
    }
    while (commit_cnt > 0) {
        single_cycle();
        if (npc_state.state != NPC_RUNNING) break;
    }
}

void cpu_reset(int n) {
    top->reset = 1;
    while (n-- > 0) single_cycle();
    top->reset = 0;
    cpu.pc = 0x80000000;
    cpu.mstatus = 0x1800;
}

void cpu_exec(uint64_t n) {
    g_print_step = (n <= MAX_INST_TO_PRINT);
    switch (npc_state.state) {
        case NPC_END:
        case NPC_ABORT:
            printf("Program execution has ended. To restart the program, exit NPC and run again.\n");
            return;
        default:
            npc_state.state = NPC_RUNNING;
    }
    exec(n);
    switch (npc_state.state) {
        case NPC_RUNNING:
            npc_state.state = NPC_STOP;
            break;
        case NPC_END:
        case NPC_ABORT:
            if (npc_state.halt_code != 0) itracer.dump();
            Log("npc: %s \n\thalt_code=%d\n\thalt_pc=" FMT_PADDR "\n\t%lu instructions executed\n\tIPC=%0.6f",
                npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) : 
                (npc_state.halt_code == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
                ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED)),
                npc_state.halt_code, npc_state.halt_pc, inst_cnt, (double)inst_cnt/cycle_cnt);
    }
}
