#include "svdpi.h"      // DPI-C 头文件
#include "verilated.h"  // Verilator 核心库

// DPI-C 函数实现
void nemu_trap() {
    printf("EBREAK detected, stopping simulation\n");
    Verilated::gotFinish(1);  // 设置仿真结束标志
}
