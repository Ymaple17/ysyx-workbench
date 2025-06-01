/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <cpu/difftest.h>
#include "../local-include/reg.h"
/*
bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  // Compare the registers one by one
  for (int i = 0; i < 32; ++i) {
    if (cpu.gpr[i] != ref_r->gpr[i]) {
      // If any register value is different, print the mismatch
      printf("[difftest]  Register mismatch at %s: DUT=%d REF=%d\n", regs[i], cpu.gpr[i], ref_r->gpr[i]);
      return false;
    }
  }

  // Compare the program counter (pc)
  if (cpu.pc != ref_r->pc) {
    printf("[difftest]  PC mismatch: DUT=%d REF=%d\n", cpu.pc, ref_r->pc);
    return false;
  }

  // If all checks pass, the registers are consistent
  return true;
}*/

extern const char *regs[];

bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc) {
  if(cpu.pc != ref_r -> pc)
  {
    printf("[difftest]PC = 0x%x, Difftest failed at PC, Difftest=0x%x, NEMU=0x%x\n", cpu.pc, ref_r -> pc, cpu.pc);
    return false;
  }

  for(int integer_register_index = 0; integer_register_index < 32; integer_register_index = integer_register_index + 1)
  {
    if(cpu.gpr[integer_register_index] != ref_r -> gpr[integer_register_index])
    {
      printf("[difftest]PC = 0x%x, Difftest Reg Compare failed at %-3s, Difftest=0x%x, NEMU=0x%x\n", cpu.pc, regs[integer_register_index], ref_r -> gpr[integer_register_index], cpu.gpr[integer_register_index]);
      return false;
    }
}

  // M-State CSR checkings
  //printf("PC = 0x%lx, Difftest success\n", cpu.pc);
  if (ref_r->csrs.mstatus != cpu.csrs.mstatus) {
        printf("mstatus is different! ref: 0x%08x, current: 0x%08x\n", ref_r->csrs.mstatus, cpu.csrs.mstatus);
        return false;
    }
    if (ref_r->csrs.mcause != cpu.csrs.mcause) {
        printf("mcause is different! ref: 0x%08x, current: 0x%08x\n", ref_r->csrs.mcause, cpu.csrs.mcause);
        return false;
    }
    if (ref_r->csrs.mepc != cpu.csrs.mepc) {
        printf("mepc is different! ref: 0x%08x, current: 0x%08x\n", ref_r->csrs.mepc, cpu.csrs.mepc);
        return false;
    }
    if (ref_r->csrs.mtvec != cpu.csrs.mtvec) {
        printf("mtvec is different! ref: 0x%08x, current: 0x%08x\n", ref_r->csrs.mtvec, cpu.csrs.mtvec);
        return false;
    }
  return true;
}

void isa_difftest_attach() {
}
