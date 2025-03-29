#include <cstdlib>
#include <cstdint>
#include <cstdbool>
#include <cstdio>
#include <cassert>
#include <cstring>

#include <macro.h>
#include <debug.h>
#include <conf.h>

// Fixed to 32-bit architecture
typedef unsigned int word_t;      // 32-bit unsigned integer
typedef int sword_t;              // 32-bit signed integer
#define FMT_WORD "0x%08x"         // 32-bit format string

typedef unsigned int paddr_t;     // 32-bit physical address
#define FMT_PADDR "0x%08x"        // 32-bit address format string

extern int device_access_st;
