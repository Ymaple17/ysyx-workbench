#include <dlfcn.h>

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
    #ifdef CONFIG_DIFFTEST
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
    //ref_difftest_memcpy(MEM_START, guest_to_host(MEM_START), img_size, DIFFTEST_TO_REF);
    ref_difftest_memcpy(CONFIG_FLASH_BASE, flash_guest_to_host(CONFIG_FLASH_BASE), img_size, DIFFTEST_TO_REF);
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
        printf("\n[difftest] ========== PC MISMATCH (Fetch/Commit) ==========\n");
        printf("[difftest] PC: REF = 0x%08x, DUT = 0x%08x\n", ref.pc, cpu.pc);
        printf("[difftest] ================================================\n");
        for (int i = 0; i < 16; i++) {
             printf("[difftest] %-4s: REF = 0x%08x, DUT = 0x%08x\n", regs[i], ref.gpr[i], cpu.gpr[i]);
        }
        return false;
    }

    // 检查所有通用寄存器
    for (int i = 0; i < 16; i++) {
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