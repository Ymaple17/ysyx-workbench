#include <stdint.h>
#include <string.h>
#include "verilated_dpi.h"
#include "../../obj_dir/Vysyx_25020039___024root.h"
#include "../include/common.h"
#include "Vysyx_25020039.h"
#include "../../include/generated/autoconf.h"

extern Vysyx_25020039* top;
uint32_t cpu_gpr[16] = {0};
uint32_t cpu_pc = 0x80000000;


uint32_t read_gpr_from_top(int idx) {
    if (idx == 0) return 0;
    return top->rootp->ysyx_25020039__DOT__core__DOT__refile__DOT__ysyx_25020039_rf_ext__DOT__Memory[idx];
}

uint32_t read_pc_from_top() {
  return top->rootp->ysyx_25020039__DOT__core__DOT__ifu__DOT__pc_reg;
}

static inline void refresh_cpu_regs() {
  for (int i = 0; i < 16; i++) {
    cpu_gpr[i] = read_gpr_from_top(i);
  }
  cpu_pc = read_pc_from_top();
}

const char *regs[] = {
"$0", "$ra", "$sp", "$gp", "$tp", "$t0", "$t1", "$t2",
"$s0", "$s1", "$a0", "$a1", "$a2", "$a3", "$a4", "$a5",
// "$a6", "$a7", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
// "$s8", "$s9", "$s10", "$s11", "$t3", "$t4", "$t5", "$t6",
};

void print_register_values() {
  refresh_cpu_regs();
  printf("The 32 General-Purpose Register is:\n");
  for(int i = 0; i < 16; i++){
    printf(ANSI_FG_GREEN "%-4s: " ANSI_FG_BLUE "0x%08x" " " ANSI_NONE, regs[i], cpu_gpr[i]);
    if(i%5 == 4)
      printf("\n");
  }
  printf("\n");
  printf("Program Counter:\n");
  printf(ANSI_FG_RED "%-4s: " ANSI_FG_BLUE "0x%08x" ANSI_NONE"\n", "$pc", cpu_pc);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
  int i;
  for(i = 0; i<16; i++){
    if(strcmp(regs[i], s) == 0){
      *success = true;
      break;
    }
  }
  if (i > 15) {
    *success = false;
    return 0;
  }
  uint32_t val = read_gpr_from_top(i);
  cpu_gpr[i] = val;
  return val;
}