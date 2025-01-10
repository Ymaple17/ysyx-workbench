#include "Vcounter.h"
#include "verilated.h"
#include "verilated_fst_c.h"

int main(int argc, char** argv) {
    VerilatedContext* contextp = new VerilatedContext;
    // 初始化命令行参数
    contextp->commandArgs(argc, argv);
    // 创建Verilog模块实例
    Vcounter* top = new Vcounter(contextp);

    // 打开FST波形文件
    VerilatedFstC* tfp = new VerilatedFstC;
    contextp->trace(tfp, 0);
    tfp->open("waveform.fst");

    // 模拟时钟信号，运行一定周期
    int clk_period = 5;  // 假设时钟周期为5个时间单位
    for (int i = 0; i < 20; i++) {
        top->clk = (i % clk_period) < (clk_period / 2);
        top->reset = (i == 5);
        top->eval();
        tfp->dump(contextp->time());
        contextp->timeInc(1);
    }

    tfp->close();
    delete top;
    delete contextp;
    delete tfp;

    return 0;
}
