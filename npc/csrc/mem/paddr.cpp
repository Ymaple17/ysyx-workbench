#include <mem/paddr.h>
#include <cpu/cpu.h>
#include <sys/time.h>
#include <cpu/difftest.h>
#include <mem/host.h>
#include <device/io.h>
#include <utils.h>

// Define word_t as 32-bit for clarity
typedef uint32_t word_t;
typedef uint32_t paddr_t;

static uint8_t pmem[CONFIG_MSIZE] = {};
static uint32_t rtc_port_base[2]; // 检查是否需要使用，否则可移除

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

void trace_mread(paddr_t addr) {
    printf("mtrace: read at " FMT_PADDR "\n", addr);
}

bool in_pmem(paddr_t addr) {
    return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

void trace_mwrite(paddr_t addr, word_t data, uint8_t mask) {
    printf("mtrace: write at " FMT_PADDR ", data=" FMT_WORD ", mask=%x\n", addr, data, mask);
}

// C function for reading physical memory (32-bit)
word_t paddr_read_c(paddr_t addr, int len) {  
    IFDEF(CONFIG_MTRACE, trace_mread(addr));
    if (likely(in_pmem(addr))) {
        return host_read(guest_to_host(addr), len);
    }
    return device_read(addr, len);
}

// C function for writing physical memory (32-bit)
void paddr_write_c(paddr_t addr, int len, word_t data) {
    IFDEF(CONFIG_MTRACE, trace_mwrite(addr, data, (len == 1) ? 0x01 : (len == 2) ? 0x03 : 0x0f));
    if (likely(in_pmem(addr))) { 
        host_write(guest_to_host(addr), len, data); 
        return; 
    }
    device_write(addr, len, data);
}

static const uint32_t img[] = {
    0x00000297,  // auipc t0,0
    0x0002b823,  // sd  zero,16(t0) (will be treated as sw in 32-bit)
    0x0102b503,  // ld  a0,16(t0)  (will be treated as lw in 32-bit)
    0x00100073,  // ebreak (used as nemu_trap)
    0xdeadbeef,  // some data
};

long load_img(char* img_file) {
    if (img_file == nullptr) {
        Log("No image is given. Use the default build-in image.");
        memcpy(guest_to_host(RESET_VECTOR), img, sizeof(img));
        return 4096; // built-in image size
    }

    FILE *fp = fopen(img_file, "rb");
    Assert(fp, "Can not open '%s'", img_file);

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);

    Log("The image is %s, size = %ld", img_file, size);

    fseek(fp, 0, SEEK_SET);
    int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
    assert(ret == 1);

    fclose(fp);
    return size;
}
