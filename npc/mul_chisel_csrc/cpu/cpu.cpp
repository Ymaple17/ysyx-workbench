#include <stdint.h>
#include "Vysyx_25020039.h"
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../../obj_dir/Vysyx_25020039___024root.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../include/memory.h"
#include "../../include/generated/autoconf.h"
#include "../include/difftest.h"

#define MAX_INST_TO_PRINT 10
CPU_State cpu;
extern Vysyx_25020039* top;
bool rst_done = false;
#define WAVE_time 0

#ifdef ENABLE_WAVEFORM
extern VerilatedFstC* tfp;
#else
extern void* tfp;
#endif

static vluint64_t main_time = 0;
static bool g_print_step = false;

#define COMMIT_FIFO_DEPTH 64
static word_t commit_pc_fifo[COMMIT_FIFO_DEPTH];
static int commit_pc_wptr = 0;
static int commit_pc_rptr = 0;
static int commit_pc_cnt = 0;

static inline void commit_pc_push(word_t pc) {
  if (commit_pc_cnt < COMMIT_FIFO_DEPTH) {
    commit_pc_fifo[commit_pc_wptr] = pc;
    commit_pc_wptr = (commit_pc_wptr + 1) % COMMIT_FIFO_DEPTH;
    commit_pc_cnt++;
  }
}

static inline word_t commit_pc_pop() {
  if (commit_pc_cnt > 0) {
    word_t pc = commit_pc_fifo[commit_pc_rptr];
    commit_pc_rptr = (commit_pc_rptr + 1) % COMMIT_FIFO_DEPTH;
    commit_pc_cnt--;
    return pc;
  }
  return 0xdeadbeef;
}

extern "C" void sim_exit(){
  set_npc_state(NPC_END, read_pc_from_top(), read_gpr_from_top(10));
}

static inline bool in_pmem(uint32_t addr) {
  return addr - 0x80000000 < 0x8000000;
}

static inline bool is_mmio_addr(uint32_t addr) {
  return !in_pmem(addr);
}

double sc_time_stamp(){
  return main_time;
}

void init_npc_cpu(){
  top->clock = 0;
  top->reset = 1;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;

  top->clock = 1;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;
  top->clock = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;

  top->reset = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;
  top->clock = 1;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;
  top->clock = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) {
      tfp->dump(main_time);
    }
  #endif
  main_time++;
  rst_done = true;

  cpu.pc = 0x80000000;
  cpu_pc = 0x80000000;
}

static inline void update_cpu_state() {
    for(int i = 0; i < 32; i++){
        cpu.gpr[i] = read_gpr_from_top(i);
        cpu_gpr[i] = cpu.gpr[i];
    }
}

void exec_once(){
  top->clock = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if(tfp != nullptr && main_time > WAVE_time){
      tfp->dump(main_time);
    }
  #endif
  main_time++;

  if (rst_done) {
    uint8_t ifu_state = top->rootp->ysyx_25020039__DOT__core__DOT__ifu__DOT__state;
    static uint8_t prev_ifu_state = 0;
    if (prev_ifu_state == 0 && ifu_state == 1) {
      uint32_t pc_now = read_pc_from_top();
      commit_pc_push(pc_now);
    }
    prev_ifu_state = ifu_state;
  }

  top->clock = 1;
  top->eval();

  if (!rst_done) return;

  #ifdef ENABLE_WAVEFORM
    if(tfp != nullptr && main_time > WAVE_time){
      tfp->dump(main_time);
    }
  #endif
  main_time++;
}

void device_update();

#ifdef DIFFTEST
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
static bool dt_check_pending = false;
#endif

static void execute(uint64_t n) {
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();
    #ifdef CONFIG_DEVICE
      device_update();
    #endif

      bool lsu_valid = top->rootp->ysyx_25020039__DOT__core__DOT___lsu_io_out_valid;
      bool reset = top->reset;

      if (lsu_valid && !reset && rst_done) {
        word_t commit_pc = commit_pc_pop();

#ifdef CONFIG_ITRACE
        {
          uint32_t inst = pmem_read(commit_pc, 4);
          itrace_inst(commit_pc, inst);
          display_inst();
        }
#endif

#ifdef DIFFTEST
        if (dt_check_pending) {
          update_cpu_state();
          CPU_State ref_state;
          ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);
          cpu.pc = ref_state.pc;
          cpu_pc = cpu.pc;

          if (!difftest_check_reg()) {
            printf("\n[difftest] ========== MISMATCH at commit PC = 0x%08x ==========\n", commit_pc);
            for (int i = 0; i < 32; i++) {
              if (cpu.gpr[i] != ref_state.gpr[i]) {
                printf("[difftest] %-4s: REF = 0x%08x, DUT = 0x%08x\n",
                       regs[i], ref_state.gpr[i], cpu.gpr[i]);
              }
            }
            printf("[difftest] =========================================\n\n");
            npc_state.state = NPC_ABORT;
            break;
          }
          dt_check_pending = false;
        }

        difftest_one_exec();
        dt_check_pending = true;
#endif
      }
    }

    if (npc_state.state != NPC_RUNNING) {
      break;
    }
  }
}

void cpu_exec(uint64_t n){
  g_print_step = (n < MAX_INST_TO_PRINT);

  switch (npc_state.state){
    case NPC_END: case NPC_ABORT:
      printf("Program execution has ended. To restart the program, exit NPC and run again.\n");
      printf("halt_pc = 0x%x halt_ret = %d\n", npc_state.halt_pc, npc_state.halt_ret);
      return;
    default: npc_state.state = NPC_RUNNING;
  }

  execute(n);

  char *out = NULL;
  switch(npc_state.state){
    case NPC_RUNNING:
      out = (char *)"stop";
      npc_state.state = NPC_STOP;
      break;
    case NPC_END:
      if(npc_state.halt_ret == 0){
        out = (char *)"HIT GOOD TRAP";
        #ifdef DIFFTEST
          difftest_one_exec();
        #endif
        printf("npc: " ANSI_FG_GREEN "%s" ANSI_RESET " at pc = 0x%08x\n",
               out, read_pc_from_top());
      }
      else{
        out = (char *)"ABORT";
        #ifdef DIFFTEST
          difftest_one_exec();
        #endif
        printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n",
               out, read_pc_from_top());
      }
      break;
    case NPC_ABORT:
      out = (char *)"ABORT";
      printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n",
             out, read_pc_from_top());
      break;
    default:
      out = (char *)"HIT BAD TRAP";
      printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n",
             out, read_pc_from_top());
      break;
  }
}
