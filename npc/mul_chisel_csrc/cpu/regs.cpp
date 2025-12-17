#include <stdint.h>
#include <string.h>
#include "verilated_dpi.h"
#include "../../obj_dir/VysyxSoCFull___024root.h"
#include "../include/common.h"
#include "VysyxSoCFull.h"
#include "../include/regs.h"
#include "../../include/generated/autoconf.h"

extern VysyxSoCFull* top;
uint32_t* rgs[32] = {0};
uint32_t cpu_gpr[32] = {0};
uint32_t cpu_pc = 0x80000000;
//#define cpu_gpr top->rootp->top__DOT__u_RegisterFile__DOT__rg

void init_rgs_array() {
    rgs[0] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_0;
    rgs[1] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_1;
    rgs[2] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_2;
    rgs[3] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_3;
    rgs[4] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_4;
    rgs[5] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_5;
    rgs[6] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_6;
    rgs[7] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_7;
    rgs[8] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_8;
    rgs[9] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_9;
    rgs[10] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_10;
    rgs[11] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_11;
    rgs[12] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_12;
    rgs[13] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_13;
    rgs[14] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_14;
    rgs[15] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_15;
    rgs[16] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_16;
    rgs[17] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_17;
    rgs[18] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_18;
    rgs[19] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_19;
    rgs[20] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_20;
    rgs[21] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_21;
    rgs[22] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_22;
    rgs[23] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_23;
    rgs[24] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_24;
    rgs[25] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_25;
    rgs[26] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_26;
    rgs[27] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_27;
    rgs[28] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_28;
    rgs[29] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_29;
    rgs[30] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_30;
    rgs[31] = &top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__regfile__DOT__regs_31;
}
 
const char *regs[] = {
"$0", "$ra", "$sp", "$gp", "$tp", "$t0", "$t1", "$t2",
"$s0", "$s1", "$a0", "$a1", "$a2", "$a3", "$a4", "$a5",
"$a6", "$a7", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
"$s8", "$s9", "$s10", "$s11", "$t3", "$t4", "$t5", "$t6",
};


void print_register_values() {
  printf("The 32 General-Purpose Register is:\n");
  for(int i = 0; i < 32; i++){
    printf(ANSI_FG_GREEN "%-4s: " ANSI_FG_BLUE "0x%08x" " " ANSI_NONE, regs[i], *rgs[i]);
    if(i%5 == 4)
      printf("\n");
  }
  printf("\n");
  printf("Program Counter:\n");
  printf(ANSI_FG_RED "%-4s: " ANSI_FG_BLUE "0x%08x" ANSI_NONE"\n", "$pc", top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__npc__DOT__ifu__DOT__pc_reg);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
  int i;
  for(i = 0; i<=31; i++){
    if(strcmp(regs[i], s) == 0){
      break;
    }
    *success = true;
  }
  return *rgs[i];
}
