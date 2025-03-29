#include <verilated.h>
#include <VTop.h>
#include <verilated_vcd_c.h>

extern VerilatedContext* contextp;
extern VTop* top;
extern VerilatedVcdC* tfp;

void a_single_cycle();  // 时钟驱动函数
void cpu_reset(int n);  // 复位函数（从 cpu-exec.cpp）
