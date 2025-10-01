#include "svdpi.h"
#include "VysyxSoCFull.h"
#include "../../obj_dir/VysyxSoCFull___024root.h"
#include "../include/common.h"
#include "../include/macro.h"
#include "../include/state.h"
#include "../include/timer.h"
#include "../../include/generated/autoconf.h"

#define memory_size 128*1024*1024

#define FLASH_SIZE (16 * 1024 * 1024)
#define CONFIG_FLASH_BASE 0x30000000
uint8_t flash[FLASH_SIZE] = {};

#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_RTC_MMIO 0xa0000048
#define CONFIG_SERIAL_MMIO 0xa00003f8
#define CONFIG_I8042_DATA_MMIO 0xa0000060
#define CONFIG_VGA_CTL_MMIO 0xa0000100
#define CONFIG_FB_ADDR 0xa1000000
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define FLASH_LEFT  ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + FLASH_SIZE - 1)

extern VysyxSoCFull* top;

uint8_t pmem[memory_size] = {};
void print_register_values();
extern int skip;

uint8_t *guest_to_host(uint64_t paddr){ 
  return pmem + paddr - CONFIG_MBASE; 
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

static inline bool in_flash(paddr_t addr) {
  return (addr - CONFIG_FLASH_BASE < FLASH_SIZE);
}

static inline void out_of_bound(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of pmem [0x%08x, 0x%08x] at pc = 0x%08x", addr, PMEM_LEFT, PMEM_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
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

uint8_t *guest_to_flash(uint64_t paddr){ 
  return flash + paddr - CONFIG_FLASH_BASE; 
}

static inline void out_of_flash(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of flash [0x%08x, 0x%08x] at pc = 0x%08x", addr, FLASH_LEFT, FLASH_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
  npc_state.state=NPC_ABORT;
}

extern "C" void flash_read(int32_t addr, int32_t *data) {
  paddr_t flash_addr = (paddr_t)((addr+CONFIG_FLASH_BASE) & ~0x3u);
  if(likely(in_flash(flash_addr))) {
    *data = host_read(guest_to_flash(flash_addr), 4);
    return;
  }
  out_of_flash(flash_addr);
}