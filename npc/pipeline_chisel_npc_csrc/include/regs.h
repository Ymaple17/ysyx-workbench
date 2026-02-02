#ifndef __REGS_H__
#define __REGS_H__


extern uint32_t cpu_gpr[32];
extern uint32_t cpu_pc;
void print_register_values();
uint32_t isa_reg_str2val(const char *s, bool *success);
uint32_t read_gpr_from_top(int idx);
uint32_t read_pc_from_top();
#endif
