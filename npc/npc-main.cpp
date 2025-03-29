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


VerilatedContext* contextp = nullptr;
VTop* top = nullptr;
VerilatedVcdC* tfp = nullptr;

void init_monitor(int, char *[]);
void sdb_mainloop();

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
int main(int argc, char** argv) {
    init_monitor(argc, argv);
    init_verilator(argc, argv);
    sdb_mainloop();
    cleanup_verilator();

    return exit_code();
}
