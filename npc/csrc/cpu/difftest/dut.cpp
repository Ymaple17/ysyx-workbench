#include <cpu/cpu.h>
#include <dlfcn.h>
#include <mem/paddr.h>

typedef void (*memcpy_func_t)(paddr_t addr, void *buf, size_t n,
                              bool direction);
typedef void (*regcpy_func_t)(void *dut, bool direction);
typedef void (*exec_func_t)(uint64_t n);
typedef void (*init_func_t)();
typedef void (*raise_intr_func_t)(uint64_t NO);

memcpy_func_t ref_difftest_memcpy = nullptr;
regcpy_func_t ref_difftest_regcpy = nullptr;
exec_func_t ref_difftest_exec = nullptr;
raise_intr_func_t ref_difftest_raise_intr = nullptr;

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

struct diff_CPU_State {
  word_t gpr[32];
  paddr_t pc;

  // control and status registers
  word_t mcause;
  word_t mepc;
  word_t mstatus;
  word_t mtvec;
};

static bool is_skip_ref = false;

void difftest_skip_ref() {
  is_skip_ref = true;
}

void init_difftest(char *ref_so_file, long img_size) {
  assert(ref_so_file);

  void *handle;
  handle = dlopen(ref_so_file, RTLD_LAZY);
  assert(handle);

  ref_difftest_memcpy = (memcpy_func_t)dlsym(handle, "difftest_memcpy");
  assert(ref_difftest_memcpy);

  ref_difftest_regcpy = (regcpy_func_t)dlsym(handle, "difftest_regcpy");
  assert(ref_difftest_regcpy);

  ref_difftest_exec = (exec_func_t)dlsym(handle, "difftest_exec");
  assert(ref_difftest_exec);

  ref_difftest_raise_intr =
      (raise_intr_func_t)dlsym(handle, "difftest_raise_intr");

  init_func_t ref_difftest_init = (init_func_t)dlsym(handle, "difftest_init");
  assert(ref_difftest_init);

  Log("Differential testing: %s", ANSI_FMT("ON", ANSI_FG_GREEN));
  Log("The result of every instruction will be compared with %s. "
      "This will help you a lot for debugging, but also significantly reduce "
      "the performance. ",
      ref_so_file);

  ref_difftest_init();
  ref_difftest_memcpy(RESET_VECTOR, guest_to_host(RESET_VECTOR), img_size,
                      DIFFTEST_TO_REF);
  // ref_difftest_regcpy(&cpu_state, DIFFTEST_TO_REF);
}

#define CHECKDIFF(r1, r2, fmt, ...)                            \
  if (r1 != r2) {                                              \
    printf("difftest fail at " fmt ", expect %#x got %#x\n",   \
           ##__VA_ARGS__, (word_t)(r1), (word_t)(r2));         \
    npc_state.state = NPC_ABORT;                               \
    npc_state.halt_pc = pc;                                    \
    npc_state.halt_code = 1;                                   \
    return;                                                    \
  }

static void checkregs(diff_CPU_State &ref_state, paddr_t pc) {
  for (int i = 0; i < ARRLEN(ref_state.gpr); i++) {
    CHECKDIFF(ref_state.gpr[i], cpu.gpr[i], "gpr[%d]", i);
  }

  CHECKDIFF(ref_state.mcause, cpu.mcause, "mcause");
  CHECKDIFF(ref_state.mepc, cpu.mepc, "mepc");
  CHECKDIFF(ref_state.mtvec, cpu.mtvec, "mtvec");
  CHECKDIFF(ref_state.mstatus, cpu.mstatus, "mstatus");

  CHECKDIFF(ref_state.pc, cpu.pc, "pc");
}

void difftest_step(paddr_t pc) {
  if (is_skip_ref) {
    // WARNINIG: extra copy: cpu -> cpu_state -> ref
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }

  ref_difftest_exec(1);

  diff_CPU_State ref_state;
  ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);

  checkregs(ref_state, pc);
}
