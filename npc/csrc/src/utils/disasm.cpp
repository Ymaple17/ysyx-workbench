#include <dlfcn.h>
#include <cerrno>
#include <capstone/capstone.h>
#include <common.h>

static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code, size_t code_size, uint64_t address, size_t count, cs_insn **insn);
static void (*cs_free_dl)(cs_insn *insn, size_t count);

static csh handle;

extern "C" void init_disasm() {
    void *dl_handle = dlopen("/home/qiu/ysyx-workbench/npc/tools/capstone/repo/libcapstone.so.5", RTLD_LAZY);
    if (!dl_handle) {
        fprintf(stderr, "Failed to open libcapstone.so.5: %s\n", dlerror());
        exit(1);
    }
    auto cs_open_func = reinterpret_cast<cs_err (*)(cs_arch, cs_mode, csh*)>(dlsym(dl_handle, "cs_open"));
    if (!cs_open_func) {
        fprintf(stderr, "Failed to load cs_open: %s\n", dlerror());
        exit(1);
    }

    cs_disasm_dl = reinterpret_cast<size_t (*)(csh, const uint8_t*, size_t, uint64_t, size_t, cs_insn**)>(dlsym(dl_handle, "cs_disasm"));
    if (!cs_disasm_dl) {
        fprintf(stderr, "Failed to load cs_disasm: %s\n", dlerror());
        exit(1);
    }

    cs_free_dl = reinterpret_cast<void (*)(cs_insn*, size_t)>(dlsym(dl_handle, "cs_free"));
    if (!cs_free_dl) {
        fprintf(stderr, "Failed to load cs_free: %s\n", dlerror());
        exit(1);
    }
    cs_arch arch = CS_ARCH_RISCV;
    cs_mode mode = static_cast<cs_mode>(CS_MODE_RISCV32 | CS_MODE_RISCVC);

    int ret = cs_open_func(arch, mode, &handle);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "Failed to initialize Capstone: %d\n", ret);
        exit(1);
    }

#ifdef CONFIG_ISA_x86
    auto cs_option_func = reinterpret_cast<cs_err (*)(csh, cs_opt_type, size_t)>(dlsym(dl_handle, "cs_option"));
    if (!cs_option_func) {
        fprintf(stderr, "Failed to load cs_option: %s\n", dlerror());
        exit(1);
    }

    ret = cs_option_func(handle, CS_OPT_SYNTAX, CS_OPT_SYNTAX_ATT);
    if (ret != CS_ERR_OK) {
        fprintf(stderr, "Failed to set Capstone option: %d\n", ret);
        exit(1);
    }
#endif
}
extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte) {
    cs_insn *insn;
    size_t count = cs_disasm_dl(handle, code, nbyte, pc, 0, &insn);
    if (count != 1) {
        fprintf(stderr, "Disassembly failed or unexpected instruction count: %zu\n", count);
        if (count > 0) cs_free_dl(insn, count);
        snprintf(str, size, "??");
        return;
    }

    int ret = snprintf(str, size, "%s", insn->mnemonic);
    if (ret < 0 || ret >= size) {
        snprintf(str, size, "??");
        cs_free_dl(insn, count);
        return;
    }

    if (insn->op_str[0] != '\0') {
        int remaining = size - ret;
        if (remaining > 0) {
            snprintf(str + ret, remaining, "\t%s", insn->op_str);
        }
    }

    cs_free_dl(insn, count);
}
