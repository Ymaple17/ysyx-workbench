// csrc/trace/trace.h
#ifndef TRACE_H
#define TRACE_H

#include <cstdint>

// 函数声明，参数类型需与定义完全一致
void itrace_inst(uint32_t pc, uint32_t inst);
void display_inst();

#endif  // TRACE_H
