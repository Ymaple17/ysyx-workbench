#include <common.h>
#include <memory/paddr.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/ifetch.h>
#include "isa.h"

#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"
#include "Vtop.h"


VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;

Vtop* top;


extern "C" void npc_pmem_read(int raddr, int *rdata, char ren){
    if(ren /*&& raddr >= PMEM_LEFT && raddr <= PMEM_RIGHT*/)
        *rdata = paddr_read(raddr, 4);  //这里直接取出数据即可，数据的扩展在mem模块中进行

    /* else */
    /*     *rdata = 0; */
}

extern "C" void npc_pmem_write(int waddr, int wdata, char len, char wen){
    if(wen /*&& waddr >= PMEM_LEFT && waddr <= PMEM_RIGHT*/)
        paddr_write((paddr_t)waddr, len, wdata);
}
void single_cycle(){
	top->clk = 0;top->eval();contextp->timeInc(1);
  top->clk = 1;top->eval();contextp->timeInc(1);
}


static void reset(int n) {
	top->rst = 1;
	while (n -- > 0) single_cycle();
	top->rst = 0;

  top->clk = !top->clk;
  top->eval();
  contextp->timeInc(10);
	}

void init_sim(){
	contextp = new VerilatedContext;
	tfp = new VerilatedVcdC;
	top = new Vtop;
	contextp->traceEverOn(true);
	top->trace(tfp, 0);
	tfp->open("dump.vcd");
  top->clk = 0;
	}

void step_and_dump_wave(){
	contextp->timeInc(1);
	tfp -> dump(contextp->time());
}

extern "C" void init_module(){
    reset(10);
    printf("Current pc = 0x%08x\n", top->pc);
}
void sim_exit(){
	step_and_dump_wave();
	tfp->close();
	}

extern "C" void trace_inst(word_t pc, uint32_t inst);

extern "C" void npc_run_once(Decode *s){

    top->clk = !top->clk;   //产生上升沿
    top->inst = inst_fetch(&top->pc, 4);

    s->snpc = top->snpc;
    s->dnpc = top->dnpc;
    s->pc   = top->pc;

    s->isa.inst.val = top->inst; //将取出的指令传入仿真环境
    top->eval();
    IFDEF(CONFIG_WAVE, step_and_dump_wave());

    top->clk = !top->clk;  //产生下降沿
    top->eval();
    IFDEF(CONFIG_WAVE, step_and_dump_wave());
    

    trace_inst(top->pc, top->inst);
}

