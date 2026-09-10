#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include "verilated_dpi.h"
#include "VysyxSoCFull__Dpi.h"
#include "../include/common.h"
#include "VysyxSoCFull.h"
#include "../../include/generated/autoconf.h"

extern VysyxSoCFull* top;
uint32_t cpu_gpr[32] = {0};
uint32_t cpu_pc = 0x30000000;

static void select_probe_scope() {
  static svScope probe_scope = nullptr;
  if (probe_scope == nullptr) {
    static const char *const candidates[] = {
      "top.ysyxSoCFull.asic.cpu.cpu.oood_soc_sim_probe",
      "TOP.ysyxSoCFull.asic.cpu.cpu.oood_soc_sim_probe",
      "ysyxSoCFull.asic.cpu.cpu.oood_soc_sim_probe",
    };
    for (const char *name : candidates) {
      probe_scope = svGetScopeFromName(name);
      if (probe_scope != nullptr) break;
    }
    if (probe_scope == nullptr) {
      fprintf(stderr, "OOOD SoC DPI probe scope was not found\n");
      assert(probe_scope != nullptr);
    }
  }
  svSetScope(probe_scope);
}

uint32_t read_commit_mask_from_top() {
  select_probe_scope();
  return oood_soc_commit_mask();
}

uint32_t read_commit_pc_from_top(int lane) {
  select_probe_scope();
  return oood_soc_commit_pc(lane);
}

uint32_t read_commit_mem_addr_from_top(int lane) {
  select_probe_scope();
  return oood_soc_commit_mem_addr(lane);
}

bool read_commit_is_load_from_top(int lane) {
  select_probe_scope();
  return oood_soc_commit_is_load(lane) != 0;
}

uint32_t read_gpr_from_top(int idx) {
  if (idx < 0 || idx > 31) return 0;
  select_probe_scope();
  return oood_soc_read_gpr(idx);
}

uint32_t read_pc_from_top() {
  static uint32_t last_commit_pc = 0x30000000;
  uint32_t mask = read_commit_mask_from_top();
  for (int lane = 3; lane >= 0; lane--) {
    if (mask & (1u << lane)) {
      last_commit_pc = read_commit_pc_from_top(lane);
      break;
    }
  }
  return last_commit_pc;
}

static inline void refresh_cpu_regs() {
  for (int i = 0; i < 32; i++) {
    cpu_gpr[i] = read_gpr_from_top(i);
  }
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
    if(i%5 == 4)
      printf("\n");
  }
  printf("\n");
  printf("Program Counter:\n");
  printf(ANSI_FG_RED "%-4s: " ANSI_FG_BLUE "0x%08x" ANSI_NONE"\n", "$pc", cpu_pc);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
  int i;
  for(i = 0; i<=31; i++){
    if(strcmp(regs[i], s) == 0){
      *success = true;
      break;
    }
  }
  if (i > 31) {
    *success = false;
    return 0;
  }
  uint32_t val = read_gpr_from_top(i);
  cpu_gpr[i] = val;
  return val;
}
