#ifndef __COMMON_H__
#define __COMMON_H__

#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <assert.h>
#include <stdlib.h>
#include <stdarg.h>
// macro concatenation
#define concat_temp(x, y) x ## y
#define concat(x, y) concat_temp(x, y)
#define concat3(x, y, z) concat(concat(x, y), z)
#define concat4(x, y, z, w) concat3(concat(x, y), z, w)
#define concat5(x, y, z, v, w) concat4(concat(x, y), z, v, w)
void panic(const char* fmt, ...) __attribute__((noreturn));

typedef uint32_t word_t;
typedef uint32_t paddr_t;
#define ANSI_NONE           "\033[0m"
#define ANSI_BOLD           "\033[1m"
#define ANSI_RED            "\033[31m"
#define ANSI_GREEN          "\033[32m"
#define ANSI_YELLOW         "\033[33m"
#define ANSI_BLUE           "\033[34m"
#define ANSI_CYAN           "\033[36m"
#define ANSI_FG_RED         ANSI_RED
#define ANSI_FG_GREEN       ANSI_GREEN
#define ANSI_FG_BLUE        ANSI_BLUE
#define ANSI_FG_YELLOW      ANSI_YELLOW
#define ANSI_FG_CYAN        ANSI_CYAN
#define ANSI_RESET          ANSI_NONE
#define ANSI_FMT(str, fmt) fmt str ANSI_NONE

#define STR1(R) #R
#define STR2(R) STR1(R)

extern word_t img_size;

#endif
