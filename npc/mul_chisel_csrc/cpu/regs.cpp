#include <stdint.h>
#include <string.h>
#include "verilated_dpi.h"
#include "../../obj_dir/Vtop___024root.h"
#include "../include/common.h"
#include "Vtop.h"
#include "../../include/generated/autoconf.h"

extern Vtop* top;
uint32_t cpu_gpr[32] = {0};
uint32_t cpu_pc = 0x80000000;
//#define cpu_gpr top->rootp->top__DOT__u_RegisterFile__DOT__rg
#define cpu_gpr top->rootp->top__DOT__npc__DOT__regfile_ext__DOT__Memory

 
const char *regs[] = {
"$0", "$ra", "$sp", "$gp", "$tp", "$t0", "$t1", "$t2",
"$s0", "$s1", "$a0", "$a1", "$a2", "$a3", "$a4", "$a5",
"$a6", "$a7", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
"$s8", "$s9", "$s10", "$s11", "$t3", "$t4", "$t5", "$t6",
};


void print_register_values() {
  printf("The 32 General-Purpose Register is:\n");
  for(int i = 0; i < 32; i++){
    printf(ANSI_FG_GREEN "%-4s: " ANSI_FG_BLUE "0x%08x" " " ANSI_NONE, regs[i], cpu_gpr[i]);
    if(i%5 == 4)
      printf("\n");
  }
  printf("\n");
  printf("Program Counter:\n");
  printf(ANSI_FG_RED "%-4s: " ANSI_FG_BLUE "0x%08x" ANSI_NONE"\n", "$pc", top->imem_pc);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
  int i;
  for(i = 0; i<=31; i++){
    if(strcmp(regs[i], s) == 0){
      break;
    }
    *success = true;
  }
  return cpu_gpr[i];
}
