#include <dlfcn.h>
#include <capstone/capstone.h>
#include <common.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code, size_t code_size, uint64_t address, size_t count, cs_insn **insn);
static void (*cs_free_dl)(cs_insn *insn, size_t count);
static csh handle;

void init_disasm() {
    void *dl_handle = dlopen("tools/capstone/repo/libcapstone.so.5", RTLD_LAZY);
    if (!dl_handle) {
        fprintf(stderr, "Failed to open libcapstone.so.5: %s\n", dlerror());
        exit(1);
    }

    cs_err (*cs_open)(cs_arch, cs_mode, csh*) = (cs_err (*)(cs_arch, cs_mode, csh*))dlsym(dl_handle, "cs_open");
    if (!cs_open) {
        fprintf(stderr, "Failed to load cs_open: %s\n", dlerror());
        exit(1);
    }

    cs_disasm_dl = (size_t (*)(csh, const uint8_t*, size_t, uint64_t, size_t, cs_insn**))dlsym(dl_handle, "cs_disasm");
    if (!cs_disasm_dl) {
        fprintf(stderr, "Failed to load cs_disasm: %s\n", dlerror());
        exit(1);
    }

    cs_free_dl = (void (*)(cs_insn*, size_t))dlsym(dl_handle, "cs_free");
    if (!cs_free_dl) {
        fprintf(stderr, "Failed to load cs_free: %s\n", dlerror());
        exit(1);
    }

    cs_arch arch = CS_ARCH_RISCV;
    cs_mode mode = CS_MODE_RISCV32;    // 针对 RV32E，使用标准 RV32 模式，无扩展

    int ret = cs_open(arch, mode, &handle);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "Failed to initialize Capstone: %d\n", ret);
        exit(1);
    }
}

extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte) {
	cs_insn *insn;
	size_t count = cs_disasm_dl(handle, code, nbyte, pc, 0, &insn);
  assert(count == 1);
  int ret = snprintf(str, size, "%s", insn->mnemonic);
  if (insn->op_str[0] != '\0') {
    snprintf(str + ret, size - ret, "\t%s", insn->op_str);
  }
  cs_free_dl(insn, count);
}
