#include <mem/paddr.h>
#include <cpu/cpu.h>
#include <mem/host.h>
#include <utils.h>
#include <string.h>
#include <assert.h>
#include <stdio.h>

typedef uint32_t word_t;
typedef uint32_t paddr_t;

static uint8_t pmem[CONFIG_MSIZE] = {};

uint8_t* guest_to_host(paddr_t paddr) {
    return pmem + paddr - CONFIG_MBASE;
}

paddr_t host_to_guest(uint8_t *haddr) {
    return haddr - pmem + CONFIG_MBASE;
}

#ifdef CONFIG_MTRACE
static void trace_mread(paddr_t addr) {
    printf("mtrace: read at " FMT_PADDR "\n", addr);
}

static void trace_mwrite(paddr_t addr, word_t data, uint8_t mask) {
    printf("mtrace: write at " FMT_PADDR ", data=" FMT_WORD ", mask=%x\n", addr, data, mask);
}
#endif

// Make in_pmem static since it is only used here.
bool in_pmem(paddr_t addr) {
    return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

word_t paddr_read_c(paddr_t addr, int len) {
    #ifdef CONFIG_MTRACE
    trace_mread(addr);
    #endif

    if (likely(in_pmem(addr))) {
        return host_read(guest_to_host(addr), len);
    }
    Log("地址超出物理内存范围: " FMT_PADDR, addr);
    return 0;
}

void paddr_write_c(paddr_t addr, int len, word_t data) {
    #ifdef CONFIG_MTRACE
    trace_mwrite(addr, data, (len == 1) ? 0x01 : (len == 2) ? 0x03 : 0x0f);
    #endif

    if (likely(in_pmem(addr))) {
        host_write(guest_to_host(addr), len, data);
        return;
    }
    Log("地址超出物理内存范围: " FMT_PADDR, addr);
}

// ... (load_img function remains the same)
