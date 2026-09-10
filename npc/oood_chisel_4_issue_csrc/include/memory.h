#ifndef __PMEM_H__
#define __PMEM_H__

#include <stdint.h>

extern "C" void paddr_read(long long raddr, long long *rdata);

extern "C" void paddr_write(long long waddr, long long wdata, char wmask);

uint64_t pmem_read(uint32_t addr, int len);

void pmem_write(uint32_t addr, int len, uint64_t data);

#endif
