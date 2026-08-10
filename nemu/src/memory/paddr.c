/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
// static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
static uint8_t mrom[0x1000] PG_ALIGN = {};
static uint8_t sram[0x2000] PG_ALIGN = {};
static uint8_t flash[16 * 1024 * 1024] PG_ALIGN = {};
static uint8_t psram[128 * 1024 * 1024] PG_ALIGN = {};
static uint8_t sdram[64 * 1024 * 1024] PG_ALIGN = {};
static uint8_t periph[2 * 1024 * 1024] PG_ALIGN = {};
#endif

void trace_read(paddr_t addr, int len);
void trace_write(paddr_t addr, int len, word_t data);

bool in_mrom(paddr_t addr) { return addr >= 0x20000000 && addr < 0x20000000 + 0x1000; }
bool in_flash(paddr_t addr) { return addr >= 0x30000000 && addr < 0x30000000 + 16 * 1024 * 1024; }
bool in_sram(paddr_t addr) { return addr >= 0x0f000000 && addr < 0x0f000000 + 0x2000; }
bool in_psram(paddr_t addr) { return addr >= 0x80000000 && addr < 0x80000000 + 128 * 1024 * 1024; }
bool in_sdram(paddr_t addr) { return addr >= 0xa0000000 && addr < 0xa0000000 + 64 * 1024 * 1024; }
bool in_periph(paddr_t addr) { return addr >= 0x10000000 && addr < 0x10000000 + 2 * 1024 * 1024; }

uint8_t* guest_to_host(paddr_t paddr) { 
  if (in_psram(paddr)) return psram + (paddr - 0x80000000);
  if (in_mrom(paddr)) return mrom + (paddr - 0x20000000);
  if (in_flash(paddr)) return flash + (paddr - 0x30000000);
  if (in_sram(paddr)) return sram + (paddr - 0x0f000000);
  if (in_sdram(paddr)) return sdram + (paddr - 0xa0000000);
  if (in_periph(paddr)) return periph + (paddr - 0x10000000);
  return NULL;
}

paddr_t host_to_guest(uint8_t *haddr) { 
  if (haddr >= psram && haddr < psram + 128*1024*1024) return 0x80000000 + (haddr - psram);
  if (haddr >= mrom && haddr < mrom + 0x1000) return 0x20000000 + (haddr - mrom);
  if (haddr >= flash && haddr < flash + 16*1024*1024) return 0x30000000 + (haddr - flash);
  if (haddr >= sram && haddr < sram + 0x2000) return 0x0f000000 + (haddr - sram);
  if (haddr >= sdram && haddr < sdram + 64*1024*1024) return 0xa0000000 + (haddr - sdram);
  if (haddr >= periph && haddr < periph + 2*1024*1024) return 0x10000000 + (haddr - periph);
  return 0; // Should not happen usually
}

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem at pc = " FMT_WORD,
      addr, cpu.pc);
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  // IFDEF(CONFIG_MEM_RANDOM, memset(pmem, rand(), CONFIG_MSIZE));
  IFDEF(CONFIG_MEM_RANDOM, memset(psram, rand(), 128*1024*1024));
  IFDEF(CONFIG_MEM_RANDOM, memset(sdram, rand(), 64*1024*1024));
  IFDEF(CONFIG_MEM_RANDOM, memset(sram, rand(), 0x2000));
  
  Log("physical memory area initialized for SoC map");
}

word_t paddr_read(paddr_t addr, int len) {
  IFDEF(CONFIG_MTRACE, trace_read(addr, len));
  // if (in_pmem(addr)) return pmem_read(addr, len); // Removed legacy check
  if (in_psram(addr) || in_mrom(addr) || in_flash(addr) || in_sram(addr) || in_sdram(addr) || in_periph(addr))
     return pmem_read(addr, len);
  
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  IFDEF(CONFIG_MTRACE, trace_write(addr, len,data));
  // if (in_pmem(addr)) { pmem_write(addr, len, data); return; } // Removed legacy check
  
  if (in_psram(addr) || in_mrom(addr) || in_flash(addr) || in_sram(addr) || in_sdram(addr) || in_periph(addr)) {
     pmem_write(addr, len, data); return;
  }

  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
