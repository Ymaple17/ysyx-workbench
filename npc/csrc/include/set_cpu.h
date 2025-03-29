#pragma once

#define set_gpr(i) cpu.gpr[i] = dut.rootp->Top__DOT__regfile__DOT__regfile[i]

// 声明 set_cpu 函数
void set_cpu();
