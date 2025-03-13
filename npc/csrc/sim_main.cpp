#include "verilated.h"
#include <bits/stdc++.h>
#include "verilated_vcd_c.h"
#include "Vtop.h"

VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;
static Vtop* top;

extern "C" void nemu_trap() {
    printf("NEMU trap (ebreak) detected, stopping simulation\n");
    Verilated::gotFinish(1);
}

void step_and_dump_wave() {
    top->eval();               
    contextp->timeInc(1);      // 增加仿真时间步长
    tfp->dump(contextp->time()); 
}

void sim_init() {
    contextp = new VerilatedContext;// 创建仿真上下文
    tfp = new VerilatedVcdC;
    top = new Vtop;          
    contextp->traceEverOn(true);// 启用波形跟踪
    top->trace(tfp, 0);   // 启动波形追踪    
    tfp->open("wave.vcd");    
}

void sim_exit() {
    step_and_dump_wave();     
    tfp->close();         
    delete top;
    delete contextp;
    delete tfp;
}

int main() {
    sim_init();
    top->clk = 0;
    for (int i = 0; i < 5; i++) {
        top->clk = !top->clk;
        step_and_dump_wave();
    }

    for (int i = 0; i < 100; i++) {
        top->clk = !top->clk;
        step_and_dump_wave();
    }
    sim_exit(); 
    return 0;
}

