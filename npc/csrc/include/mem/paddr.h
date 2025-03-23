#pragma once

#include <common.h>
#include <VTop__Dpi.h>

#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_PC_RESET_OFFSET 0x0

#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

uint8_t* guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);
// void paddr_read(paddr_t addr, uint64_t* data);
// void paddr_write(paddr_t addr, uint64_t data, uint8_t mask);

long load_img(char* img_path);

#define PAGE_SHIFT        12
#define PAGE_SIZE         (1ul << PAGE_SHIFT)
#define PAGE_MASK         (PAGE_SIZE - 1)
