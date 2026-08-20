#include <stdint.h>
#include <string.h>
#include "verilated_dpi.h"
#include "../../obj_dir/Vysyx_25020039___024root.h"
#include "../include/common.h"
#include "Vysyx_25020039.h"
#include "../../include/generated/autoconf.h"

extern Vysyx_25020039* top;
uint32_t cpu_gpr[32] = {0};
uint32_t cpu_pc = 0x80000000;

static uint32_t read_arch(int idx) {
  auto *r = top->rootp;
  switch (idx) {
    case 0: return 0;
    case 1: return r->io_arch_rdata_1;
    case 2: return r->io_arch_rdata_2;
    case 3: return r->io_arch_rdata_3;
    case 4: return r->io_arch_rdata_4;
    case 5: return r->io_arch_rdata_5;
    case 6: return r->io_arch_rdata_6;
    case 7: return r->io_arch_rdata_7;
    case 8: return r->io_arch_rdata_8;
    case 9: return r->io_arch_rdata_9;
    case 10: return r->io_arch_rdata_10;
    case 11: return r->io_arch_rdata_11;
    case 12: return r->io_arch_rdata_12;
    case 13: return r->io_arch_rdata_13;
    case 14: return r->io_arch_rdata_14;
    case 15: return r->io_arch_rdata_15;
    case 16: return r->io_arch_rdata_16;
    case 17: return r->io_arch_rdata_17;
    case 18: return r->io_arch_rdata_18;
    case 19: return r->io_arch_rdata_19;
    case 20: return r->io_arch_rdata_20;
    case 21: return r->io_arch_rdata_21;
    case 22: return r->io_arch_rdata_22;
    case 23: return r->io_arch_rdata_23;
    case 24: return r->io_arch_rdata_24;
    case 25: return r->io_arch_rdata_25;
    case 26: return r->io_arch_rdata_26;
    case 27: return r->io_arch_rdata_27;
    case 28: return r->io_arch_rdata_28;
    case 29: return r->io_arch_rdata_29;
    case 30: return r->io_arch_rdata_30;
    case 31: return r->io_arch_rdata_31;
    default: return 0;
  }
}

uint32_t read_gpr_from_top(int idx) { return read_arch(idx); }
uint32_t read_pc_from_top() {
  return top->rootp->ysyx_25020039__DOT__core__DOT__ifu__DOT__pc_reg;
}
uint32_t read_commit_pc_from_top() {
  return top->rootp->io_commit_pc;
}

static inline void refresh_cpu_regs() {
  for (int i = 0; i < 32; i++) cpu_gpr[i] = read_gpr_from_top(i);
  cpu_pc = read_pc_from_top();
}

const char *regs[] = {
"$0", "$ra", "$sp", "$gp", "$tp", "$t0", "$t1", "$t2",
"$s0", "$s1", "$a0", "$a1", "$a2", "$a3", "$a4", "$a5",
"$a6", "$a7", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
"$s8", "$s9", "$s10", "$s11", "$t3", "$t4", "$t5", "$t6",
};

void print_register_values() {
  refresh_cpu_regs();
  printf("The 32 General-Purpose Register is:\n");
  for(int i = 0; i < 32; i++){
    printf(ANSI_FG_GREEN "%-4s: " ANSI_FG_BLUE "0x%08x" " " ANSI_NONE, regs[i], cpu_gpr[i]);
    if(i%5 == 4) printf("\n");
  }
  printf("\nProgram Counter:\n");
  printf(ANSI_FG_RED "%-4s: " ANSI_FG_BLUE "0x%08x" ANSI_NONE"\n", "$pc", cpu_pc);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
  int i;
  for(i = 0; i<=31; i++){
    if(strcmp(regs[i], s) == 0){ *success = true; break; }
  }
  if (i > 31) { *success = false; return 0; }
  uint32_t val = read_gpr_from_top(i);
  cpu_gpr[i] = val;
  return val;
}
