#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#include "Vadder.h"  //adder.v会被编译成Vadder.h
#include "verilated.h"

#include "verilated_vcd_c.h" //可选，如果要导出vcd则需要加上

int main(int argc, char** argv) {
    VerilatedContext* contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vadder* adder = new Vadder{contextp};


    VerilatedVcdC* tfp = new VerilatedVcdC; //初始化VCD对象指针
    contextp->traceEverOn(true); //打开追踪功能
    adder->trace(tfp, 0); //
    tfp->open("wave.vcd"); //设置输出的文件wave.vcd

    int i=20;
    while (!contextp->gotFinish() && i>=0) {
        int a = rand() & 1;
        int b = rand() & 1;
        adder->a = a;
        adder->b = b;
        adder->eval();
        printf("a = %d, b = %d, sum = %d\n", a, b, adder->sum);

        tfp->dump(contextp->time()); //dump wave
        contextp->timeInc(1); //推动仿真时间

        assert(adder->sum == a + b);

        i--;
    }
    delete adder;
    tfp->close();
    delete contextp;
    return 0;
}
