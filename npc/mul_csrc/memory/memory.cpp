#include "svdpi.h"
#include "Vtop.h"
#include "../include/common.h"
#include "../include/macro.h"
#include "../include/state.h"
#include "../include/timer.h"
#include "../../include/generated/autoconf.h"

#define memory_size 128*1024*1024
#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_RTC_MMIO 0xa0000048
#define CONFIG_SERIAL_MMIO 0xa00003f8
#define CONFIG_I8042_DATA_MMIO 0xa0000060
#define CONFIG_VGA_CTL_MMIO 0xa0000100
#define CONFIG_FB_ADDR 0xa1000000
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)//物理内存的起始地址（Memory Base）物理内存的大小（Memory Size）
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)//PMEM_LEFT 和 PMEM_RIGHT 定义了 NPC 物理内存的范围

extern Vtop* top;

uint8_t pmem[memory_size] = {};
void print_register_values();
extern int skip;

uint8_t *guest_to_host(uint64_t paddr){ 
  return pmem + paddr - 0x80000000; 
}

extern "C" inline uint64_t host_read(void *addr, int len) {
  switch (len) {
    case 1: return *(uint8_t  *)addr;
    case 2: return *(uint16_t *)addr;
    case 4: return *(uint32_t *)addr;
    case 8: return *(uint64_t *)addr;
    default: assert(0);
  }
}

extern "C" inline void host_write(void *addr, int len, uint64_t data) {
  switch (len) {
    case 1: *(uint8_t  *)addr = data; return;
    case 2: *(uint16_t *)addr = data; return;
    case 4: *(uint32_t *)addr = data; return;
    case 8: *(uint64_t *)addr = data; return;
    default: assert(0);
  }
}

static inline bool in_pmem(paddr_t addr) {
  return (addr - CONFIG_MBASE < CONFIG_MSIZE);
}

static inline void out_of_bound(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of pmem [0x%08x, 0x%08x] at pc = 0x%08x", addr, PMEM_LEFT, PMEM_RIGHT, top->imem_pc);
  npc_state.state=NPC_ABORT;
}

uint64_t mmio_read(uint32_t addr, int len);
void mmio_write(uint32_t addr, int len, uint64_t data);

extern "C" word_t pmem_read(paddr_t addr, int len) 
{

  if(likely(in_pmem(addr))) return host_read(guest_to_host(addr), len);
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

extern "C" void pmem_write(paddr_t addr,word_t data,int len) 
{
  if(likely(in_pmem(addr)))
  {
    host_write(guest_to_host(addr), len, data);
    return;
  }  
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
