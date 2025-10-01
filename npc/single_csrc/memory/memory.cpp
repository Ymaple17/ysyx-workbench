#include "svdpi.h"
#include "Vtop.h"
#include "../include/common.h"
#include "../include/state.h"
#include "../include/timer.h"
#include "../../include/generated/autoconf.h"

#define memory_size 128*1024*1024
#define CONFIG_RTC_MMIO 0xa0000048
#define CONFIG_SERIAL_MMIO 0xa00003f8
#define CONFIG_I8042_DATA_MMIO 0xa0000060
#define CONFIG_VGA_CTL_MMIO 0xa0000100
#define CONFIG_FB_ADDR 0xa1000000
#define MEM_BASE 0x80000000
uint8_t pmem[memory_size] = {};

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

uint64_t mmio_read(uint32_t addr, int len);
void mmio_write(uint32_t addr, int len, uint64_t data);

uint64_t pmem_read(uint32_t addr, int len) {
  uint64_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

void pmem_write(uint32_t addr, int len, uint64_t data) {
  host_write(guest_to_host(addr), len, data);
}

extern "C" int paddr_read (uint32_t raddr) {
  if(raddr >= CONFIG_RTC_MMIO&&raddr<CONFIG_RTC_MMIO+8){
    return mmio_read(raddr, 4);
  }
  // if(raddr == CONFIG_RTC_MMIO + 4){
  //   return mmio_read(raddr, 4) << 32;
    
  // }
  // if (raddr >= CONFIG_RTC_MMIO&&raddr<CONFIG_RTC_MMIO+8) {
	//   return mmio_read (raddr, 8);
	// }
   if(raddr == CONFIG_I8042_DATA_MMIO){
    return mmio_read(raddr, 4);
  }

  if(raddr >=CONFIG_SERIAL_MMIO&&raddr<CONFIG_SERIAL_MMIO+8){
    return 0;
  }
   if(raddr >= CONFIG_VGA_CTL_MMIO && raddr < CONFIG_VGA_CTL_MMIO + 8){
    return mmio_read(raddr & ~0x3, 4);
  }
  if(raddr >= CONFIG_FB_ADDR && raddr < CONFIG_FB_ADDR + 300 * 400 * 4){
    return mmio_read(raddr & ~0x3, 4);
  } 

  if (raddr >= 0x80000000 && raddr <0x80000000 + memory_size) {
    return host_read (guest_to_host (raddr & ~0x3u), 4);
  }
    printf ("read 越界地址为: 0x%08x\n", raddr);
    npc_state.state = NPC_ABORT;
    return 0;
}



extern "C" void paddr_write(uint32_t waddr, uint32_t wdata, uint8_t wmask) {
  wmask &= 0x0F;
  if ((waddr >= CONFIG_VGA_CTL_MMIO && waddr < CONFIG_VGA_CTL_MMIO + 8) ||
      (waddr >= CONFIG_FB_ADDR && waddr <CONFIG_FB_ADDR + 300 * 400 * 4)) {
    for (int i = 0; i < 4; i++) {
      if (wmask & (1 << i)) {
        mmio_write((waddr & ~0x3) + i, 1, (wdata >> (8 * i)) & 0xFF);
      }
    }
    return;
  }
  if (waddr >= CONFIG_SERIAL_MMIO && waddr < CONFIG_SERIAL_MMIO + 8) {
    //printf("PC=0x%08x 重复写入串口: 数据=0x%02x\n", top->imem_pc, (wdata & 0xFF));
    for (int i = 0; i < 4; i++) {
      if (wmask & (1 << i)) {
        mmio_write(waddr + i, 1, (wdata >> (8 * i)) & 0xFF);
      }
    }
    return;
  }

  if (waddr >= MEM_BASE && waddr < MEM_BASE + memory_size) {
    for (int i = 0; i < 4; i++) { 
      if ((wmask >> i) & 0x1) {
        host_write(guest_to_host((waddr & ~0x3u) + i), 1, wdata);
      }
      wdata >>= 8;
    }
    return;
  }
  printf("write越界地址为: 0x%08x\n", waddr);
  npc_state.state = NPC_ABORT;
}