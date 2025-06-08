#include <memory/paddr.h>
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/ifetch.h>
#include "isa.h"

#include "verilated.h"
#include "verilated_dpi.h"
#include "verilated_vcd_c.h"
#include "Vtop.h"

#define CLOCK_ADDRESS 0xa0000048 
#define UART_BASE_ADDR 0xa00003F8  
#define CLOCK_ADDR_LEN 8  
#define UART_ADDR_LEN  8  

// 全局变量
VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;
Vtop* top = NULL;

// 性能计数器
static uint64_t cycle_count = 0;

// 前向声明
void step_and_dump_wave();

/******************** 内存访问接口 ********************/
extern "C" void npc_pmem_read(int raddr, int *rdata, char ren) {
    *rdata = 0;  // 默认返回0
    
    // 读使能控制（原核心逻辑）
    if (!ren) {
        return;
    }
    
    // 地址对齐（参考pmem_read逻辑）
    raddr = raddr & ~0x3u;  // 清除低两位，确保按4字节对齐
    
    // 时钟地址处理（整合pmem_read逻辑）
    if (raddr >= CLOCK_ADDRESS && raddr < CLOCK_ADDRESS + CLOCK_ADDR_LEN) {
        uint64_t us = get_time();
        if (raddr == CLOCK_ADDRESS) {
            *rdata = (uint32_t)(us & 0xFFFFFFFF);  // 低32位
            return;
        } else if (raddr == CLOCK_ADDRESS + 4) {
            *rdata = (uint32_t)((us >> 32) & 0xFFFFFFFF);  // 高32位
            return;
        }
    }
    
    // 串口地址处理（整合pmem_read逻辑）
    if (raddr >= UART_BASE_ADDR && raddr < UART_BASE_ADDR + UART_ADDR_LEN) {
        return;  // 串口地址保持返回0（*rdata已初始化为0）
    }
    
    // PMEM范围检查（原核心逻辑）
    if (raddr >= PMEM_LEFT && raddr <= PMEM_RIGHT) {
        *rdata = paddr_read(raddr, 4);
    } else {
        // 非复位期的非法访问警告（原核心逻辑）
        if (cycle_count > 10) {
            printf("Warning: Invalid read at address 0x%08x\n", raddr);
        }
    }
}

extern "C" void npc_pmem_write(int waddr, int wdata, char len, char wen) {
    if (!wen) return;  // 写使能控制（原核心逻辑）

    // 地址对齐（参考pmem_write逻辑）
    waddr = waddr & ~0x3u;  // 清除低两位，确保按4字节对齐

    // 串口地址处理（整合pmem_write逻辑）
    if (waddr >= UART_BASE_ADDR && waddr < UART_BASE_ADDR + UART_ADDR_LEN) {
        putchar(static_cast<char>(wdata & 0xFF));  // 输出串口数据（低8位）
        return;
    }

    // PMEM范围检查（原核心逻辑）
    if (waddr >= PMEM_LEFT && waddr <= PMEM_RIGHT) {
        paddr_write((paddr_t)waddr, len, wdata);  // 调用原写入函数
    } else {
        // 非法地址警告（原核心逻辑）
        printf("Warning: Invalid write at address 0x%08x\n", waddr);
    }
}

/******************** 波形记录辅助函数 ********************/
void step_and_dump_wave() {
    if (tfp) {
        tfp->dump(contextp->time());
    }
}

/******************** 仿真控制函数 ********************/
void single_cycle() {
    // 时钟低电平阶段
    top->clk = 0;
    top->eval();
    
    // 时钟高电平阶段（上升沿触发）
    top->clk = 1;
    top->eval();
    
    // 更新周期计数器和波形
    cycle_count++;
    contextp->timeInc(1);
    step_and_dump_wave();
}

static void reset(int n) {
    printf("Resetting for %d cycles...\n", n);
    top->rst = 1; 
    for (int i = 0; i < n; i++) {
        top->clk = !top->clk;
        top->eval();
        contextp->timeInc(1);
        step_and_dump_wave();
    }
    top->rst = 0; 
    printf("Reset complete.\n");
}

/******************** 初始化与清理 ********************/
void init_sim() {
    printf("Initializing simulation...\n");
    
    contextp = new VerilatedContext;
    tfp = new VerilatedVcdC;
    top = new Vtop;
    
    // 设置波形跟踪
    contextp->traceEverOn(true);
    top->trace(tfp, 0);
    tfp->open("dump.vcd");
    
    // 初始化时钟
    top->clk = 0;
    // 初始化复位信号，假设低电平有效
    top->rst = 0; 
    cycle_count = 0;
    
    printf("Simulation initialized.\n");
}

void sim_exit() {
    printf("Simulation completed after %lu cycles.\n", cycle_count);
    
    // 确保最后一次状态被记录
    step_and_dump_wave();
    
    // 清理资源
    if (tfp) {
        tfp->close();
    }
    if (top) {
        delete top;
    }
    if (tfp) {
        delete tfp;
    }
    if (contextp) {
        delete contextp;
    }
    
    printf("Simulation resources cleaned up.\n");
}

/******************** 指令跟踪接口 ********************/
extern "C" void trace_inst(word_t pc, uint32_t inst);

extern "C" void init_module() {
    reset(10);
    printf("Initial PC = 0x%08x\n", top->tmp_pc);
}

extern "C" void npc_run_once(Decode *s) {
    // 在时钟低电平时获取指令
    top->inst = inst_fetch(&top->pc, 4);
    // 执行一个完整时钟周期
    single_cycle();
    
    // 更新解码结构
    s->snpc = top->snpc;
    s->dnpc = top->dnpc;
    s->pc   = top->pc;
    s->isa.inst.val = top->inst;
    trace_inst(top->pc, top->inst);
    
    // PC对齐检查
    if (top->pc % 4 != 0) {
        printf("Warning: Unaligned PC detected: 0x%08x\n", top->pc);
    }
}    
