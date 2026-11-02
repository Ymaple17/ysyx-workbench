#include <dlfcn.h>
#include <capstone/capstone.h>
#include <cassert>
#include <cstdio>

// capstone全局资源
static csh handle = 0;
static size_t (*cs_disasm_dl)(csh, const uint8_t*, size_t, uint64_t, size_t, cs_insn**) = nullptr;
static void (*cs_free_dl)(cs_insn*, size_t) = nullptr;
static const char* (*cs_strerror_dl)(cs_err) = nullptr;
static cs_err (*cs_errno_dl)(csh) = nullptr;

void init_disasm() {
    void *dl_handle = dlopen("/home/qiu/ysyx-workbench/npc/tools/capstone/libcapstone.so.6", RTLD_LAZY);
    if (!dl_handle) {
        assert(false);
    }

    auto cs_open_dl = reinterpret_cast<cs_err (*)(cs_arch, cs_mode, csh*)>(
        dlsym(dl_handle, "cs_open")
    );
    if (!cs_open_dl) {
        assert(false);
    }

    cs_disasm_dl = reinterpret_cast<size_t (*)(csh, const uint8_t*, size_t, uint64_t, size_t, cs_insn**)>(
        dlsym(dl_handle, "cs_disasm")
    );
    cs_free_dl = reinterpret_cast<void (*)(cs_insn*, size_t)>(
        dlsym(dl_handle, "cs_free")
    );
    cs_strerror_dl = reinterpret_cast<const char* (*)(cs_err)>(
        dlsym(dl_handle, "cs_strerror")
    );
    cs_errno_dl = reinterpret_cast<cs_err (*)(csh)>(
        dlsym(dl_handle, "cs_errno")
    );

    if (!cs_disasm_dl || !cs_free_dl || !cs_strerror_dl || !cs_errno_dl) {
        assert(false);
    }

    cs_arch arch = CS_ARCH_RISCV;
    cs_mode mode = static_cast<cs_mode>(CS_MODE_RISCV32 | CS_MODE_RISCVC);
    cs_err ret = cs_open_dl(arch, mode, &handle);
    if (ret != CS_ERR_OK) {
        assert(false);
    }
}

void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte) {
    if (!str || size <= 0 || !code || nbyte <= 0 || nbyte > 4) {
        assert(false);
    }
    if ((uintptr_t)code % 2 != 0) { 
    snprintf(str, size, "<unaligned address>");
    return;
}

    // 增强压缩指令检测
    uint16_t inst_word = code[0] | (code[1] << 8);
    if (nbyte == 4 && (inst_word & 0x3) != 0x3) {
        nbyte = 2;
    }

    cs_insn *insn = nullptr;
    size_t count = cs_disasm_dl(handle, code, nbyte, pc, 0, &insn);

    if (count == 0) {
        cs_err err = cs_errno_dl(handle);
        snprintf(str, size, "<invalid> (err: %s)", cs_strerror_dl(err));
        return;
    }
    if (count != 1 || !insn) {
        snprintf(str, size, "<invalid count=%zu>", count);
        return;
    }

    int ret = snprintf(str, size, "%s", insn->mnemonic);
    if (insn->op_str[0] != '\0' && ret > 0 && ret < size) {
        snprintf(str + ret, size - ret, "\t%s", insn->op_str);
    }
    cs_free_dl(insn, count);
}