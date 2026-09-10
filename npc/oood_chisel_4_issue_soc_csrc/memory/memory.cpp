#include <elf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include "svdpi.h"
#include "VysyxSoCFull.h"
#include "VysyxSoCFull___024root.h"
#include "../include/common.h"
#include "../include/macro.h"
#include "../include/state.h"
#include "../include/timer.h"
#include "../include/regs.h"
#include "../include/trace.h"
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
#define CONFIG_SRAM_BASE 0x0f000000
#define CONFIG_SRAM_SIZE 8192
#define CONFIG_SDRAM_BASE 0xa0000000
#define CONFIG_SDRAM_SIZE (64*1024*1024)

#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)//物理内存的起始地址（Memory Base）物理内存的大小（Memory Size）
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)//PMEM_LEFT 和 PMEM_RIGHT 定义了 NPC 物理内存的范围
#define MROM_LEFT  ((paddr_t)CONFIG_SOC_MROM_BASE)
#define MROM_RIGHT ((paddr_t)CONFIG_SOC_MROM_BASE + mrom_size - 1)
#define FLASH_LEFT ((paddr_t)CONFIG_FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)CONFIG_FLASH_BASE + flash_size - 1)
#define PSRAM_LEFT ((paddr_t)CONFIG_SOC__PSRAM_BASE)
#define PSRAM_RIGHT ((paddr_t)CONFIG_SOC__PSRAM_BASE + CONFIG_SOC__PSRAM_SIZE - 1)
#define SRAM_LEFT  ((paddr_t)CONFIG_SRAM_BASE)
#define SRAM_RIGHT ((paddr_t)CONFIG_SRAM_BASE + CONFIG_SRAM_SIZE - 1)
#define SDRAM_LEFT  ((paddr_t)CONFIG_SDRAM_BASE)
#define SDRAM_RIGHT ((paddr_t)CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE - 1)

extern VysyxSoCFull* top;

uint8_t pmem[memory_size] = {};
uint8_t mrom[mrom_size] = {};
uint8_t flash[flash_size] = {};
uint8_t psram[CONFIG_SOC__PSRAM_SIZE] = {};
// Shadow memory for trace visibility
uint8_t sdram[CONFIG_SDRAM_SIZE] = {};

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

uint8_t *sdram_guest_to_host(uint64_t paddr){
  return sdram + paddr - CONFIG_SDRAM_BASE;
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

static inline bool in_sram(paddr_t addr) {
  return (addr - CONFIG_SRAM_BASE < CONFIG_SRAM_SIZE);
}

static inline bool in_sdram(paddr_t addr) {
  return (addr - CONFIG_SDRAM_BASE < CONFIG_SDRAM_SIZE);
}

static inline uint32_t sdram_read_word(paddr_t addr) {
  uint32_t ram_addr = (uint32_t)(addr - CONFIG_SDRAM_BASE);
  uint32_t chip = (ram_addr >> 26) & 0x1;
  uint32_t bank = (ram_addr >> 11) & 0x3;
  uint32_t row = (ram_addr >> 13) & 0x1fff;
  uint32_t col = (ram_addr >> 2) & 0x1ff;
  uint32_t index = (row << 9) | col;
  uint16_t low = 0;
  uint16_t high = 0;

  if (chip == 0) {
    low = top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u0__DOT__sdram_16_u0__DOT__bank[bank][index];
    high = top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u0__DOT__sdram_16_u1__DOT__bank[bank][index];
  } else {
    low = top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u1__DOT__sdram_16_u0__DOT__bank[bank][index];
    high = top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u1__DOT__sdram_16_u1__DOT__bank[bank][index];
  }

  return ((uint32_t)high << 16) | low;
}

static inline void sdram_write_word(paddr_t addr, uint32_t data) {
  uint32_t ram_addr = (uint32_t)(addr - CONFIG_SDRAM_BASE);
  uint32_t chip = (ram_addr >> 26) & 0x1;
  uint32_t bank = (ram_addr >> 11) & 0x3;
  uint32_t row = (ram_addr >> 13) & 0x1fff;
  uint32_t col = (ram_addr >> 2) & 0x1ff;
  uint32_t index = (row << 9) | col;

  if (chip == 0) {
    top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u0__DOT__sdram_16_u0__DOT__bank[bank][index] = data & 0xffff;
    top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u0__DOT__sdram_16_u1__DOT__bank[bank][index] = data >> 16;
  } else {
    top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u1__DOT__sdram_16_u0__DOT__bank[bank][index] = data & 0xffff;
    top->rootp->ysyxSoCFull__DOT__sdram__DOT__sdram_32_u1__DOT__sdram_16_u1__DOT__bank[bank][index] = data >> 16;
  }
  memcpy(sdram_guest_to_host(addr), &data, sizeof(data));
}

static bool preload_segment(paddr_t addr, const std::vector<uint8_t> &data) {
  bool is_sram_segment = in_sram(addr) &&
      data.size() <= (size_t)(SRAM_RIGHT - addr + 1);
  bool is_sdram_segment = in_sdram(addr) &&
      data.size() <= (size_t)(SDRAM_RIGHT - addr + 1);
  if (!is_sram_segment && !is_sdram_segment) return false;
  if ((addr & 3u) != 0) return false;

  for (size_t offset = 0; offset < data.size(); offset += 4) {
    uint32_t word = 0;
    size_t bytes = data.size() - offset;
    if (bytes > sizeof(word)) bytes = sizeof(word);
    memcpy(&word, data.data() + offset, bytes);
    paddr_t word_addr = addr + offset;
    if (is_sram_segment) {
      uint32_t index = (word_addr - CONFIG_SRAM_BASE) / 4;
      top->rootp->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[index] = word;
    } else {
      sdram_write_word(word_addr, word);
    }
  }
  return true;
}

bool preload_ysyxsoc_elf(const char *elf_path) {
  FILE *fp = fopen(elf_path, "rb");
  if (fp == nullptr) {
    fprintf(stderr, "Cannot open preload ELF: %s\n", elf_path);
    return false;
  }

  Elf32_Ehdr ehdr = {};
  bool valid = fread(&ehdr, sizeof(ehdr), 1, fp) == 1 &&
      memcmp(ehdr.e_ident, ELFMAG, SELFMAG) == 0 &&
      ehdr.e_ident[EI_CLASS] == ELFCLASS32 &&
      ehdr.e_ident[EI_DATA] == ELFDATA2LSB &&
      ehdr.e_phentsize == sizeof(Elf32_Phdr);
  if (!valid) {
    fprintf(stderr, "Unsupported preload ELF: %s\n", elf_path);
    fclose(fp);
    return false;
  }

  unsigned loaded_segments = 0;
  for (uint16_t i = 0; i < ehdr.e_phnum; i++) {
    Elf32_Phdr phdr = {};
    if (fseek(fp, ehdr.e_phoff + i * sizeof(phdr), SEEK_SET) != 0 ||
        fread(&phdr, sizeof(phdr), 1, fp) != 1) {
      fclose(fp);
      return false;
    }
    if (phdr.p_type != PT_LOAD || phdr.p_memsz == 0) continue;
    if (!in_sram(phdr.p_vaddr) && !in_sdram(phdr.p_vaddr)) continue;
    if (phdr.p_filesz > phdr.p_memsz) {
      fclose(fp);
      return false;
    }

    std::vector<uint8_t> segment(phdr.p_memsz, 0);
    if (phdr.p_filesz != 0) {
      if (fseek(fp, phdr.p_offset, SEEK_SET) != 0 ||
          fread(segment.data(), phdr.p_filesz, 1, fp) != 1) {
        fclose(fp);
        return false;
      }
    }
    if (!preload_segment(phdr.p_vaddr, segment)) {
      fprintf(stderr, "Invalid preload segment: addr=0x%08x size=0x%x\n",
              phdr.p_vaddr, phdr.p_memsz);
      fclose(fp);
      return false;
    }
    loaded_segments++;
  }

  fclose(fp);
  printf("[OOOD-SOC] Preloaded %u ELF segments into ysyxSoC SRAM/SDRAM\n",
         loaded_segments);
  return loaded_segments != 0;
}


static inline void out_of_bound(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of pmem [0x%08x, 0x%08x] at pc = 0x%08x", addr, PMEM_LEFT, PMEM_RIGHT, read_pc_from_top());
  npc_state.state=NPC_ABORT;
}

static inline void out_of_mrom(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of mrom [0x%08x, 0x%08x] at pc = 0x%08x", addr, MROM_LEFT, MROM_RIGHT, read_pc_from_top());
  npc_state.state=NPC_ABORT;
}

static inline void out_of_flash(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of flash [0x%08x, 0x%08x] at pc = 0x%08x", addr, FLASH_LEFT, FLASH_RIGHT, read_pc_from_top());
  npc_state.state=NPC_ABORT;
}

static inline void out_of_sdram(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of sdram [0x%08x, 0x%08x] at pc = 0x%08x", addr, SDRAM_LEFT, SDRAM_RIGHT, read_pc_from_top());
  npc_state.state=NPC_ABORT;
}

static inline void out_of_psram(paddr_t addr) {
  print_register_values();
  printf("[npc]address = 0x%08x is out of bound of psram [0x%08x, 0x%08x] at pc = 0x%08x", addr, PSRAM_LEFT, PSRAM_RIGHT, read_pc_from_top());
  npc_state.state=NPC_ABORT;
}

uint64_t mmio_read(uint32_t addr, int len);
void mmio_write(uint32_t addr, int len, uint64_t data);

extern "C" word_t pmem_read(paddr_t addr, int len) 
{
  word_t ret = 0;
  if(likely(in_pmem(addr))) {
    ret = host_read(guest_to_host(addr), len);
    mtrace('R', addr, len, ret);
    return ret;
  }
  
  // Also check flash if not in pmem
  if (likely(in_flash(addr))) {
      ret = host_read(flash_guest_to_host(addr), len);
      return ret;
  }
  
  if (likely(in_mrom(addr))) {
      ret = host_read(soc_mrom_guest_to_host(addr), len);
      return ret;
  }
  
  if (likely(in_sram(addr))) {
      uint32_t offset = addr - CONFIG_SRAM_BASE;
      uint32_t index = offset / 4;
      uint32_t word = top->rootp->ysyxSoCFull__DOT__asic__DOT__axi4ram__DOT__mem_ext__DOT__Memory[index];
      int shift = (offset & 3) * 8;
      ret = (word >> shift);
      if (len < 4) {
          ret &= ((1ULL << (len * 8)) - 1);
      }
      return ret;
  }

  if (likely(in_sdram(addr))) {
      uint32_t word = sdram_read_word(addr & ~0x3u);
      int shift = (addr & 0x3u) * 8;
      ret = (word >> shift);
      if (len < 4) {
        ret &= ((1u << (len * 8)) - 1);
      }
      return ret;
  }

  IFDEF(CONFIG_DEVICE, {
      ret = mmio_read(addr, len);
      mtrace('R', addr, len, ret);
      return ret;
  });
  out_of_bound(addr);
  return 0;
}

extern "C" void pmem_write(paddr_t addr,word_t data,int len) 
{
  mtrace('W', addr, len, data);
  if(likely(in_pmem(addr)))
  {
    host_write(guest_to_host(addr), len, data);
    return;
  }
  
  if(likely(in_sram(addr)))
  {
    return;
  }

  if(likely(in_sdram(addr)))
  {
    return;
  }
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}

extern "C" void flash_read(int32_t addr, int32_t *data) {
  paddr_t flash_addr = (paddr_t)((addr+CONFIG_FLASH_BASE));
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
