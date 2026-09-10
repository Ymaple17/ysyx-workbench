#ifndef __PMEM_H__
#define __PMEM_H__

#include <stdint.h>
#include "common.h"

extern "C" void paddr_read(long long raddr, long long *rdata);

extern "C" void paddr_write(long long waddr, long long wdata, char wmask);

extern "C" void pmem_write(paddr_t addr, word_t data, int len);
extern "C" word_t pmem_read(paddr_t addr, int len);

bool preload_ysyxsoc_elf(const char *elf_path);

#endif
