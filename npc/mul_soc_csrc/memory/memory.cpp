#include "svdpi.h"
#include "VysyxSoCFull.h"
#include "../../obj_dir/VysyxSoCFull___024root.h"
#include "../include/common.h"
#include "../include/macro.h"
#include "../include/state.h"
#include "../include/timer.h"
#include "../../include/generated/autoconf.h"

#define memory_size 128*1024*1024
#define mrom_size 4*1024
#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_RTC_MMIO 0xa0000048
#define CONFIG_SERIAL_MMIO 0xa00003f8
#define CONFIG_I8042_DATA_MMIO 0xa0000060
#define CONFIG_VGA_CTL_MMIO 0xa0000100
#define CONFIG_FB_ADDR 0xa1000000
#define CONFIG_SOC_MROM_BASE 0x20000000
#define CONFIG_SOC__PSRAM_BASE 0x80000000
#define CONFIG_SOC__PSRAM_SIZE 0x00400000
#define CONFIG_FLASH_BASE 0x30000000
#define flash_size 0x01000000
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)//物理内存的起始地址（Memory Base）物理内存的大小（Memory Size）
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)//PMEM_LEFT 和 PMEM_RIGHT 定义了 NPC 物理内存的范围
#define MROM_LEFT  ((paddr_t)CONFIG_SOC_MROM_BASE)
#define MROM_RIGHT ((paddr_t)CONFIG_SOC_MROM_BASE + mrom_size - 1)
#define FLASH_LEFT ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + flash_size - 1)
#define PSRAM_LEFT ((paddr_t)CONFIG_SOC__PSRAM_BASE)
#define PSRAM_RIGHT ((paddr_t)CONFIG_SOC__PSRAM_BASE + CONFIG_SOC__PSRAM_SIZE - 1)

extern VysyxSoCFull* top;

uint8_t pmem[memory_size] = {};
uint8_t mrom[mrom_size] = {};
uint8_t flash[flash_size] = {};
uint8_t psram[CONFIG_SOC__PSRAM_SIZE] = {};

void print_register_values();
extern int skip;

uint8_t *guest_to_host(uint64_t paddr){ 
  return pmem + paddr - CONFIG_MBASE; 
}

uint8_t *soc_mrom_guest_to_host(uint64_t paddr){
  return mrom + paddr - CONFIG_SOC_MROM_BASE;
}

uint8_t *flash_guest_to_host(uint64_t paddr){
  return flash + paddr - CONFIG_FLASH_BASE;
}

uint8_t *psram_guest_to_host(uint64_t paddr){
  return psram + paddr - CONFIG_SOC__PSRAM_BASE;
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

static inline bool in_mrom(paddr_t addr) {
  return (addr - CONFIG_SOC_MROM_BASE < mrom_size);
}

static inline bool in_flash(paddr_t addr) {
  return (addr - CONFIG_FLASH_BASE < flash_size);
}

static inline bool in_psram(paddr_t addr) {
  return (addr - CONFIG_SOC__PSRAM_BASE < CONFIG_SOC__PSRAM_SIZE);
}

static inline void out_of_bound(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of pmem [0x%08x, 0x%08x] at pc = 0x%08x", addr, PMEM_LEFT, PMEM_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
  npc_state.state=NPC_ABORT;
}

static inline void out_of_mrom(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of mrom [0x%08x, 0x%08x] at pc = 0x%08x", addr, MROM_LEFT, MROM_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
  npc_state.state=NPC_ABORT;
}

static inline void out_of_flash(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of flash [0x%08x, 0x%08x] at pc = 0x%08x", addr, FLASH_LEFT, FLASH_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
  npc_state.state=NPC_ABORT;
}

static inline void out_of_psram(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of psram [0x%08x, 0x%08x] at pc = 0x%08x", addr, PSRAM_LEFT, PSRAM_RIGHT, top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pc);
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

extern "C" void flash_read(int32_t addr, int32_t *data) {
  paddr_t flash_addr = (paddr_t)((addr+CONFIG_FLASH_BASE) & ~0x3u);
  if(likely(in_flash(flash_addr))) {
    *data = host_read(flash_guest_to_host(flash_addr), 4);
    return;
  }
  out_of_flash(flash_addr);
 }

extern "C" void mrom_read(int32_t addr, int32_t *data) {
  paddr_t mrom_addr = (paddr_t)(addr & ~0x3u);
  if(likely(in_mrom(mrom_addr))) {
    *data = host_read(soc_mrom_guest_to_host(mrom_addr), 4);
    return;
  }
  out_of_mrom(mrom_addr);
}

extern "C" void psram_read(int32_t addr, int32_t *data) {
  paddr_t psram_addr = (paddr_t)(addr+CONFIG_SOC__PSRAM_BASE);
  if(likely(in_psram(psram_addr))) {
    *data = host_read(psram_guest_to_host(psram_addr), 4);
    return;
  }
  out_of_psram(psram_addr);
 }

extern "C" void psram_write(int32_t addr, int32_t data, int32_t wcount) {
  paddr_t base_addr = (paddr_t)(addr + CONFIG_SOC__PSRAM_BASE);
  //printf("psram_write: addr=0x%x, wcount=%d\n", addr, wcount);
  for (int i = 0; i < wcount; i++) {
    paddr_t cur_addr = base_addr + i;
    if (likely(in_psram(cur_addr))) {
      uint8_t byte = (data >> (8 * (wcount - 1 - i))) & 0xFF;
      host_write(psram_guest_to_host(cur_addr), 1, byte);
    } else {
      out_of_psram(cur_addr);
      return;
    }
  }
}