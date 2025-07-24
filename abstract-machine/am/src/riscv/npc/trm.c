#include <am.h>
#include <klib-macros.h>
#include <string.h>

# define npc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static char mainargs[MAINARGS_MAX_LEN] = MAINARGS_PLACEHOLDER; // defined in CFLAGS

void putch(char ch) {
}

void halt(int code) {
  npc_trap(code);
  while (1);
}

void _trm_init() {
  const char *actual_args = mainargs;
  if (strcmp(mainargs, MAINARGS_PLACEHOLDER) == 0) {
      actual_args = "";
  }
  int ret = main(actual_args);
  halt(ret);
}
