#include <verilated.h>
#include <verilated_vcd_c.h>
#include <svdpi.h>
#include <verilated_vcd_c.h>

#include <VTop.h>
#include <VTop__Dpi.h>
#include <common.h>
#include <utils.h>
#include <cpu/cpu.h>
#include <module.h>
#include <tracer/wave_tracer.h>
#include "sim.h"

VerilatedContext* contextp = nullptr;
VTop* top = nullptr;
VerilatedVcdC* tfp = nullptr;

void a_single_cycle() {
    top->clock = 0; top->eval(); tfp->dump(contextp->time()); contextp->timeInc(1);
    top->clock = 1; top->eval(); tfp->dump(contextp->time()); contextp->timeInc(1);
}

void init_monitor(int, char *[]);
void sdb_mainloop();
void cpu_reset(int n);

void init_verilator(int argc, char **argv) {
    contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    contextp->traceEverOn(true);
    top = new VTop(contextp);
    tfp = new VerilatedVcdC;
    top->trace(tfp, 99);
    tfp->open("waveform.vcd");
    IFDEF(CONFIG_WTRACE, wavetracer.open(top));
}

void cleanup_verilator() {
    IFDEF(CONFIG_WTRACE, wavetracer.close());
    if (tfp) {
        tfp->close();
        delete tfp;
        tfp = nullptr;
    }
    if (top) {
        delete top;
        top = nullptr;
    }
    if (contextp) {
        delete contextp;
        contextp = nullptr;
    }
}

void dpic_ebreak(unsigned int pc, unsigned int code) {
    printf("%#08x: ebreak with code(%d)\n", pc, code);
    npc_state.state = NPC_END;
    npc_state.halt_pc = pc;
    npc_state.halt_code = code;
    cleanup_verilator();
}

void dpic_commit(unsigned int pc, unsigned int inst, unsigned int npc, char dmAccess) {
    printf("0x%08x: commit inst(0x%08x)\n", pc, inst);
    extern void dpic_commit(const svBitVecVal*, const svBitVecVal*, const svBitVecVal*, char);
    svBitVecVal pc_val = pc, inst_val = inst, npc_val = npc;
    dpic_commit(&pc_val, &inst_val, &npc_val, dmAccess);
}

int main(int argc, char** argv) {
    init_monitor(argc, argv);
    init_verilator(argc, argv);
    cpu_reset(10);  // 初始化硬件和 CPU 状态
    npc_state.state = NPC_RUNNING;  // 确保初始状态正确
    sdb_mainloop();
    cleanup_verilator();
    return exit_code();
}
