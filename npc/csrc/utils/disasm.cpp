/***************************************************************************************
* Copyright (c) [Year] [Author]
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

#include <cassert>
#include <string>
#include <sstream>
#include <capstone/capstone.h>
#include "common.h" // 假设这是项目自定义的头文件，未改动

// 全局变量
static csh handle = 0;

void init_disasm() {
    // 检查 Capstone 是否支持 RISC-V
#ifndef CS_ARCH_RISCV
    fprintf(stderr, "ERROR: Capstone does not support RISC-V. Please recompile Capstone with RISC-V support.\n");
    exit(1);
#endif

    // 根据配置选择架构和模式
    cs_arch arch;
    cs_mode mode;

    // 假设项目是 RISC-V 32 位架构（根据上下文）
    // 如果需要支持多架构，可以通过编译时宏定义选择
#ifdef CONFIG_ISA_riscv
    arch = CS_ARCH_RISCV;
    mode = CS_MODE_RISCV32 | CS_MODE_RISCV_C; // 32 位 RISC-V，支持压缩指令集 (RVC)
#elif defined(CONFIG_ISA_x86)
    arch = CS_ARCH_X86;
    mode = CS_MODE_32;
#elif defined(CONFIG_ISA_mips32)
    arch = CS_ARCH_MIPS;
    mode = CS_MODE_MIPS32;
#elif defined(CONFIG_ISA_loongarch32r)
    arch = CS_ARCH_LOONGARCH;
    mode = CS_MODE_LOONGARCH32;
#else
    fprintf(stderr, "ERROR: Unsupported ISA. Please define CONFIG_ISA_riscv, CONFIG_ISA_x86, CONFIG_ISA_mips32, or CONFIG_ISA_loongarch32r.\n");
    exit(1);
#endif

    // 初始化 Capstone
    cs_err ret = cs_open(arch, mode, &handle);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "ERROR: Failed to initialize Capstone disassembler: %d\n", ret);
        exit(1);
    }

    // 关闭详细模式（可选）
    ret = cs_option(handle, CS_OPT_DETAIL, CS_OPT_OFF);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "ERROR: Failed to set Capstone option (CS_OPT_DETAIL): %d\n", ret);
        exit(1);
    }

#ifdef CONFIG_ISA_x86
    // 如果是 x86 架构，设置 AT&T 语法
    ret = cs_option(handle, CS_OPT_SYNTAX, CS_OPT_SYNTAX_ATT);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "ERROR: Failed to set Capstone option (CS_OPT_SYNTAX): %d\n", ret);
        exit(1);
    }
#endif
}

void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte) {
    cs_insn *insn = nullptr;
    size_t count = cs_disasm(handle, code, nbyte, pc, 0, &insn);
    if (count != 1) {
        // 如果反汇编失败，输出无效指令
        snprintf(str, size, "0x%08lx: (invalid instruction)", pc);
        return;
    }

    // 使用 std::stringstream 构建反汇编字符串
    std::stringstream ss;
    ss << insn->mnemonic;
    if (insn->op_str[0] != '\0') {
        ss << "\t" << insn->op_str;
    }

    // 将结果复制到输出缓冲区
    std::string result = ss.str();
    strncpy(str, result.c_str(), size - 1);
    str[size - 1] = '\0'; // 确保字符串以空字符结尾

    // 释放内存
    cs_free(insn, count);
}
