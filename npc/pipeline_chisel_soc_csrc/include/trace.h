// csrc/trace/trace.h
#ifndef TRACE_H
#define TRACE_H

#include <cstdint>

// 函数声明，参数类型需与定义完全一致
void itrace_inst(uint32_t pc, uint32_t inst);
void display_inst();

void init_mtrace();
void mtrace(char type, uint32_t addr, int len, uint32_t data);

#endif  // TRACE_H
