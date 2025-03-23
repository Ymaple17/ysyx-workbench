#include <cstdlib>
#include <cstdint>
#include <cstdbool>
#include <cstdio>
#include <cassert>
#include <cstring>

#include <macro.h>
#include <debug.h>
#include <conf.h>

#define CONFIG_RV64 1

typedef MUXDEF(CONFIG_RV64, long long unsigned int, unsigned int) word_t;
typedef MUXDEF(CONFIG_RV64, long long int, int) sword_t;
#define FMT_WORD MUXDEF(CONFIG_RV64, "0x%016llx", "0x%08x")

typedef MUXDEF(CONFIG_RV64, unsigned long long, unsigned int) paddr_t;
#define FMT_PADDR MUXDEF(CONFIG_RV64, "0x%016llx", "0x%08x")

extern int device_access_st;
