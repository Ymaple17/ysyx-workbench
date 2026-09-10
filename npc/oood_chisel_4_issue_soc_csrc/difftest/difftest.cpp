#include <dlfcn.h>
#include <elf.h>
#include <string.h>
#include <vector>

#include "../include/common.h"
#include "../include/difftest.h"
#include "../include/state.h"
#include "../../include/generated/autoconf.h"


extern CPU_State cpu;

extern const char *regs[];

extern uint8_t flash[];

void (*ref_difftest_memcpy)(word_t addr, void *buf, word_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(word_t n) = NULL;
void (*ref_difftest_raise_intr)(word_t NO) = NULL;

static bool is_skip_ref = false;

uint8_t *flash_guest_to_host(uint64_t paddr);

word_t mem_img_size = -1;

#define CONFIG_FLASH_BASE 0x30000000
#define CONFIG_SRAM_BASE 0x0f000000
#define CONFIG_SRAM_SIZE 0x00002000
#define CONFIG_SDRAM_BASE 0xa0000000
#define CONFIG_SDRAM_SIZE 0x04000000

static bool in_preloaded_soc_memory(uint32_t addr, uint32_t size) {
    const uint64_t end = (uint64_t)addr + size;
    const bool sram = addr >= CONFIG_SRAM_BASE &&
        end <= (uint64_t)CONFIG_SRAM_BASE + CONFIG_SRAM_SIZE;
    const bool sdram = addr >= CONFIG_SDRAM_BASE &&
        end <= (uint64_t)CONFIG_SDRAM_BASE + CONFIG_SDRAM_SIZE;
    return sram || sdram;
}

static bool sync_ysyxsoc_elf_segments(const char *elf_path) {
    FILE *fp = fopen(elf_path, "rb");
    if (fp == NULL) return false;

    Elf32_Ehdr ehdr = {};
    const bool valid = fread(&ehdr, sizeof(ehdr), 1, fp) == 1 &&
        memcmp(ehdr.e_ident, ELFMAG, SELFMAG) == 0 &&
        ehdr.e_ident[EI_CLASS] == ELFCLASS32 &&
        ehdr.e_ident[EI_DATA] == ELFDATA2LSB &&
        ehdr.e_phentsize == sizeof(Elf32_Phdr);
    if (!valid) {
        fclose(fp);
        return false;
    }

    unsigned synced = 0;
    for (uint16_t i = 0; i < ehdr.e_phnum; i++) {
        Elf32_Phdr phdr = {};
        if (fseek(fp, ehdr.e_phoff + i * sizeof(phdr), SEEK_SET) != 0 ||
            fread(&phdr, sizeof(phdr), 1, fp) != 1) {
            fclose(fp);
            return false;
        }
        if (phdr.p_type != PT_LOAD || phdr.p_memsz == 0) continue;
        if (phdr.p_filesz > phdr.p_memsz ||
            !in_preloaded_soc_memory(phdr.p_vaddr, phdr.p_memsz)) continue;

        std::vector<uint8_t> segment(phdr.p_memsz, 0);
        if (phdr.p_filesz != 0) {
            if (fseek(fp, phdr.p_offset, SEEK_SET) != 0 ||
                fread(segment.data(), phdr.p_filesz, 1, fp) != 1) {
                fclose(fp);
                return false;
            }
        }
        ref_difftest_memcpy(phdr.p_vaddr, segment.data(), phdr.p_memsz,
                            DIFFTEST_TO_REF);
        synced++;
    }

    fclose(fp);
    fprintf(stderr, "[difftest] synchronized %u preloaded SoC ELF segment(s)\n", synced);
    return synced != 0;
}

void panic(const char* fmt, ...) {
  va_list args;
  va_start(args, fmt);
  fprintf(stderr, "Panic: ");
  vfprintf(stderr, fmt, args);
  fprintf(stderr, "\n");
  va_end(args);
  exit(1);
}

void difftest_init(const char* ref_so_file, word_t img_size){
    #ifdef DIFFTEST
    assert(ref_so_file != NULL);
    assert(img_size >= 0);
    printf("[difftest] initializing diifferential testing, the ref-so-file is %s, img-size is %d\n", ref_so_file, img_size);
    mem_img_size = img_size;
    assert(mem_img_size >= 0);
    assert(mem_img_size == img_size);

    void *handle;
    handle = dlopen(ref_so_file, RTLD_LAZY);
    assert(handle);

    ref_difftest_memcpy = (void (*)(word_t, void*, word_t, bool)) dlsym(handle, "difftest_memcpy");
    assert(ref_difftest_memcpy);

    ref_difftest_regcpy = (void (*)(void*, bool)) dlsym(handle, "difftest_regcpy");
    assert(ref_difftest_regcpy);

    ref_difftest_exec = (void (*)(word_t)) dlsym(handle, "difftest_exec");
    assert(ref_difftest_exec);

    ref_difftest_raise_intr = (void (*)(word_t)) dlsym(handle, "difftest_raise_intr"); // Not implemented in NEMU
    assert(ref_difftest_raise_intr);

    void (*ref_difftest_init)(int) = (void (*)(int)) dlsym(handle, "difftest_init");
    assert(ref_difftest_init);

    cpu.pc = 0x30000000;
    printf("[difftest] initialized PC = 0x%x\n", cpu.pc);

    ref_difftest_init(1234);
    ref_difftest_memcpy(CONFIG_FLASH_BASE, flash_guest_to_host(CONFIG_FLASH_BASE), img_size, DIFFTEST_TO_REF);
    const char *preload_elf = getenv("OOOD_SOC_PRELOAD_ELF");
    if (preload_elf != NULL && preload_elf[0] != '\0') {
        assert(sync_ysyxsoc_elf_segments(preload_elf));
    }
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);

    #else
    printf("[difftest] not enabled\n");
    #endif

    return;
}

void difftest_skip_ref(){
    is_skip_ref = true;
    assert(is_skip_ref);
    return;
}

void difftest_one_exec(){
    if(is_skip_ref){
        ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
        is_skip_ref = false;
        assert(!is_skip_ref);
        return;
    }
    ref_difftest_exec(1);
    return;
}

bool difftest_check_reg(){
    if(is_skip_ref){
        return true;
    }

    CPU_State ref;
    ref_difftest_regcpy(&ref, DIFFTEST_TO_DUT);
    // assert(&ref != NULL);
    if (cpu.pc != ref.pc) {
        printf("\n[difftest] ========== PC MISMATCH ==========\n");
        printf("[difftest] PC: REF = 0x%08x, DUT = 0x%08x\n", ref.pc, cpu.pc);
        printf("[difftest] ====================================\n\n");
        return false;
    }

    // 检查所有通用寄存器
    for (int i = 0; i < NR_GPRs; i++) {
        if (cpu.gpr[i] != ref.gpr[i]) {
            printf("\n[difftest] ========== REGISTER MISMATCH ==========\n");
            printf("[difftest] PC = 0x%08x\n", cpu.pc);
            printf("[difftest] %-4s: REF = 0x%08x, DUT = 0x%08x\n", 
                   regs[i], ref.gpr[i], cpu.gpr[i]);
            printf("[difftest] $a0 (x10) = 0x%08x\n", cpu.gpr[10]);
            printf("[difftest] =========================================\n\n");
            return false;
        }
    }
    return true;
}
