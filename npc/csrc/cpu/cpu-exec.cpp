#include <cpu/cpu.h>
#include <cpu/difftest.h>
#include <mem/paddr.h>
#include <module.h>
#include <tracer/inst_tracer.h>
#include <tracer/wave_tracer.h>
#include <mem/mem.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include <svdpi.h> // 包含 svdpi.h 以使用 svBitVecVal
#include "VTop.h"

extern VerilatedContext* contextp;
extern VTop* top;
extern VerilatedVcdC* tfp;

#define MAX_INST_TO_PRINT 10

void device_update();
int wp_difftest();
CPU_state cpu;
static bool g_print_step = false;
static uint64_t cycle_cnt = 0;
static uint64_t inst_cnt = 0;
static uint64_t commit_cnt = 0;
int device_access_st = 0;

mem_t mem;
mem_t mem2;

extern word_t paddr_read_c(paddr_t addr, int len);
extern void paddr_write_c(paddr_t addr, int len, word_t data);

void single_cycle() {
    contextp->timeInc(1); // 增加时间戳
    top->clock = 0;
    top->eval();
    if (tfp) tfp->dump(contextp->time());
    contextp->timeInc(1);
    top->clock = 1;
    top->eval();
    if (tfp) tfp->dump(contextp->time());
    cycle_cnt++;
}


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

//#include "set_cpu.h"

// 实现 set_cpu 函数
/*void set_cpu() {
    set_gpr(0);
    set_gpr(1);
    set_gpr(2);
    set_gpr(3);
    set_gpr(4);
    set_gpr(5);
    set_gpr(6);
    set_gpr(7);
    set_gpr(8);
    set_gpr(9);
    set_gpr(10);
    set_gpr(11);
    set_gpr(12);
    set_gpr(13);
    set_gpr(14);
    set_gpr(15);
    set_gpr(16);
    set_gpr(17);
    set_gpr(18);
    set_gpr(19);
    set_gpr(20);
    set_gpr(21);
    set_gpr(22);
    set_gpr(23);
    set_gpr(24);
    set_gpr(25);
    set_gpr(26);
    set_gpr(27);
    set_gpr(28);
    set_gpr(29);
    set_gpr(30);
    set_gpr(31);
}*/

void cpu_reset(int n) {
    top->reset = 1;
    while (n-- > 0) single_cycle();
    top->reset = 0;
    cpu.pc = 0x80000000;
    cpu.mstatus = 0x1800; // 修正 mstatus 值
}

// DPI 函数：dpic_commit
extern "C" void dpic_commit(const svBitVecVal* pc, const svBitVecVal* inst, const svBitVecVal* npc, char dm_access) {
    uint64_t pc_val = *(uint32_t*)pc; // 假设 pc 是 32 位
    uint32_t inst_val = *inst;
    uint64_t npc_val = *(uint32_t*)npc; // 假设 npc 是 32 位
    commit_cnt--;
    inst_cnt++;
    //set_cpu();
    cpu.pc = npc_val;

    if (device_access_st && dm_access) {
        device_access_st--;
        difftest_skip_ref();
    }

    IFDEF(CONFIG_ITRACE, itracer.trace(pc_val, inst_val, g_print_step));
    IFDEF(CONFIG_WATCHPOINT, if (wp_difftest()) npc_state.state = NPC_STOP);
    device_update();
    IFDEF(CONFIG_DIFFTEST, difftest_step(pc_val));
}

// DPI 函数：paddr_read
extern "C" void paddr_read(const svBitVecVal* addr, svBitVecVal* data) {
    uint32_t addr_val = *addr;
    uint32_t data_val = paddr_read_c(addr_val, 4);
    *data = data_val;
}

// DPI 函数：paddr_write
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
        case 0x01: len = 1; break; // Byte
        case 0x03: len = 2; break; // Half-word
        case 0x0f: len = 4; break; // Word (32-bit)
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
        //set_cpu();
    }

    while (commit_cnt > 0) {
        single_cycle();
        if (npc_state.state != NPC_RUNNING) break;
    }
}

void cpu_exec(uint64_t n) {
    g_print_step = (n <= MAX_INST_TO_PRINT);

    switch (npc_state.state) {
        case NPC_END:
        case NPC_ABORT:
            printf(
                "Program execution has ended. To restart the program, exit NPC and "
                "run again.\n");
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
            // fallthrough
    }
}
