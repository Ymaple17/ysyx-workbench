#include <dlfcn.h>
#include <capstone/capstone.h>
#include <common.h>

static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code,
    size_t code_size, uint64_t address, size_t count, cs_insn **insn);
static void (*cs_free_dl)(cs_insn *insn, size_t count);

static csh handle;

extern "C" void init_disasm() {
  void *dl_handle;
  // 使用你的 Capstone 库路径
  dl_handle = dlopen("/home/qiu/ysyx-workbench/npc/tools/capstone/repo/libcapstone.so.5", RTLD_LAZY);
  if (!dl_handle) {
    fprintf(stderr, "Failed to open libcapstone.so.5: %s\n", dlerror());
    exit(1);
  }

  // 使用 reinterpret_cast 转换 dlsym 返回的 void* 到函数指针类型
  cs_err (*cs_open_dl)(cs_arch arch, cs_mode mode, csh *handle) = nullptr;
  cs_open_dl = reinterpret_cast<cs_err (*)(cs_arch, cs_mode, csh*)>(dlsym(dl_handle, "cs_open"));
  if (!cs_open_dl) {
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

  // 为 RISC-V 32 位设置架构和模式
  cs_arch arch = CS_ARCH_RISCV;
  cs_mode mode = CS_MODE_RISCV32;

  int ret = cs_open_dl(arch, mode, &handle);
  if (ret != CS_ERR_OK) {
    fprintf(stderr, "Failed to initialize Capstone: %d\n", ret);
    exit(1);
  }
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
  if (insn->op_str[0] != '\0') {
    snprintf(str + ret, size - ret, "\t%s", insn->op_str);
  }
  cs_free_dl(insn, count);
}
