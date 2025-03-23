#pragma once

#include <verilated.h>
#include <verilated_vcd_c.h>
#include <verilated_dpi.h>
#include <common.h>
#include <VTop.h>
#include <VTop___024root.h>
#include <VTop__Dpi.h>

// #include <VTop__Dpi.h>

// cpustate, hold the pointer to underlying sv data
// for convenient access
typedef struct {
  word_t gpr[32];
  paddr_t pc;
  
  // control and status registers
  word_t mcause;
  word_t mepc;
  word_t mstatus;
  word_t mtvec;
} CPU_state;

extern CPU_state cpu;

void cpu_reset(int n);
void cpu_exec(uint64_t n);

void dump_gpr();
word_t reg_str2val(const char *s, bool *success);
uint32_t get_inst();