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
#include "../include/perf.h"
#include "../../include/generated/autoconf.h"
#include "../include/difftest.h"
#include "../monitor/axi_monitor.h"

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

vluint64_t main_time = 0;
static bool g_print_step = false;


extern "C" void sim_exit(){
  print_perf_stats(main_time / 2);
  set_npc_state(NPC_END, 0, read_gpr_from_top(10));
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

#ifdef CONFIG_AXI_MONITOR
  axi_monitor_init();
#endif

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

    #ifdef CONFIG_AXI_MONITOR
      axi_monitor_check();
    #endif

      bool wbu_valid = top->rootp->ysyx_25020039__DOT__core__DOT__wbu_io_in_valid;
      bool reset = top->reset;

      if (wbu_valid && !reset && rst_done) {
        word_t commit_pc = top->rootp->ysyx_25020039__DOT__core__DOT__wbu_io_in_bits_r_pc;

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

    if (npc_state.state == NPC_END) {
      npc_state.halt_pc = read_commit_pc_from_top();
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
               out, npc_state.halt_pc);
      }
      else{
        out = (char *)"ABORT";
        #ifdef DIFFTEST
          difftest_one_exec();
        #endif
        printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n",
               out, npc_state.halt_pc);
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
