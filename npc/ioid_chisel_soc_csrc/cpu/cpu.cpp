#include <stdint.h>
#include "VysyxSoCFull.h"
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../../obj_dir/VysyxSoCFull___024root.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../include/memory.h"
#include "../../include/generated/autoconf.h"
#include "../include/difftest.h"

#ifdef CONFIG_NVBOARD
#include <nvboard.h>
#endif

#define MAX_INST_TO_PRINT 10
CPU_State cpu;
extern VysyxSoCFull* top;
bool rst_done = false;
#define WAVE_time 0

#ifdef ENABLE_WAVEFORM
extern VerilatedFstC* tfp;
#else
extern void* tfp;
#endif

static vluint64_t main_time = 0;
static bool g_print_step = false;

extern "C" void sim_exit(){
  set_npc_state(NPC_END, read_pc_from_top(), read_gpr_from_top(10));
}

static inline bool in_flash(uint32_t addr) {
  return addr - 0x30000000 < 0x01000000;
}

static inline bool in_sram(uint32_t addr) {
  return addr - 0x0f000000 < 0x00002000;
}

static inline bool in_sdram(uint32_t addr) {
  return addr - 0xa0000000 < 0x04000000;
}

static inline bool in_psram(uint32_t addr) {
  return addr - 0x80000000 < 0x00400000;
}

static inline bool is_mmio_addr(uint32_t addr) {
  return !in_flash(addr) && !in_sram(addr) && !in_sdram(addr) && !in_psram(addr);
}

double sc_time_stamp(){
  return main_time;
}

static inline void update_cpu_state();

#define RESET_CYCLES 20

void single_cycle();

void init_npc_cpu(){
  top->clock = 0;
  top->reset = 1;
  top->eval();

  for (int i = 0; i < RESET_CYCLES; i++) {
    single_cycle();
  }

  top->reset = 0;
  rst_done = true;

  update_cpu_state();
}

void single_cycle() {
  top->clock = 1;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) tfp->dump(main_time);
  #endif
  main_time++;

  top->clock = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) tfp->dump(main_time);
  #endif
  main_time++;
}

static inline void update_cpu_state() {
    cpu.pc = read_pc_from_top();
    cpu_pc = cpu.pc;
    for(int i = 0; i < 32; i++){
        cpu.gpr[i] = read_gpr_from_top(i);
        cpu_gpr[i] = cpu.gpr[i];
    }
}

void exec_once(){
  #ifdef CONFIG_NVBOARD
    nvboard_update();
  #endif
  top->clock = 0;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if(tfp != nullptr && main_time > WAVE_time){
      tfp->dump(main_time);
    }
  #endif
  if (rst_done) {
  }
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
extern void (*ref_difftest_memcpy)(word_t addr, void *buf, word_t n, bool direction);
static bool dt_check_pending = false;
static int skip_pending = 0;
#endif

#define HANG_TIMEOUT_CYCLES 10000
#define PC_STUCK_COMMITS 20000

static void execute(uint64_t n) {
  uint64_t hang_counter = 0;
  static word_t stuck_pc = 0;
  static int stuck_cnt = 0;
  static bool stuck_initialized = false;
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();
    #ifdef CONFIG_DEVICE
      device_update();
    #endif

      bool lsu_valid = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_valid;
      bool reset = top->reset;

      if (lsu_valid && !reset && rst_done) {
        hang_counter = 0;
        word_t commit_pc = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;

        if (!stuck_initialized || commit_pc != stuck_pc) {
          stuck_pc = commit_pc;
          stuck_cnt = 0;
          stuck_initialized = true;
        } else {
          stuck_cnt++;
        }

#ifdef CONFIG_ITRACE
        {
          uint32_t inst = pmem_read(commit_pc, 4);
          itrace_inst(commit_pc, inst);
          display_inst();
        }
#endif

#ifdef DIFFTEST
        uint32_t inst = 0;
        ref_difftest_memcpy(commit_pc, &inst, 4, DIFFTEST_TO_DUT);
        bool is_store = ((inst & 0x7f) == 0x23);
        bool is_load  = ((inst & 0x7f) == 0x03);

        if (is_store || is_load) {
          uint32_t rs1 = (inst >> 15) & 0x1f;
          int32_t imm;
          if (is_store) {
            imm = ((int32_t)(inst >> 25) << 5) | ((inst >> 7) & 0x1f);
            imm = (imm << 20) >> 20;
          } else {
            imm = (int32_t)inst >> 20;
          }
          word_t rs1_val = read_gpr_from_top(rs1);
          word_t eaddr = rs1_val + imm;
          if (is_mmio_addr(eaddr)) {
            skip_pending = 2;
          }
        }

        if (dt_check_pending) {
          update_cpu_state();
          CPU_State ref_state;
          ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);
          if (skip_pending == 0) {
            cpu.pc = commit_pc;
            cpu_pc = cpu.pc;

            if (!difftest_check_reg()) {
              printf("\n[difftest] ========== MISMATCH at commit PC = 0x%08x ==========\n", commit_pc);
              for (int i = 0; i < 32; i++) {
                if (cpu.gpr[i] != ref_state.gpr[i]) {
                  printf("[difftest] %-4s: REF = 0x%08x, DUT = 0x%08x\n",
                         regs[i], ref_state.gpr[i], cpu.gpr[i]);
                  if (i == 10) {
                    word_t s1_val = read_gpr_from_top(9);
                    printf("[DBG] s1=0x%08x DUT:", s1_val);
                    for (int k = 0; k < 8; k++) {
                      uint8_t b = 0xff;
                      if ((s1_val + k) >= 0x0f000000 && (s1_val + k) < 0x0f002000) {
                        b = pmem_read(s1_val + k, 1);
                      }
                      printf(" %02x", b);
                    }
                    printf(" REF:");
                    for (int k = 0; k < 8; k++) {
                      uint8_t b = 0;
                      ref_difftest_memcpy(s1_val + k, &b, 1, DIFFTEST_TO_DUT);
                      printf(" %02x", b);
                    }
                    printf("\n[DBG] flash@0x30006b00 DUT:");
                    for (int k = 0; k < 12; k++) {
                      printf(" %02x", pmem_read(0x30006b00 + k, 1));
                    }
                    printf(" REF:");
                    for (int k = 0; k < 12; k++) {
                      uint8_t b = 0;
                      ref_difftest_memcpy(0x30006b00 + k, &b, 1, DIFFTEST_TO_DUT);
                      printf(" %02x", b);
                    }
                    printf("\n");
                  }
                }
              }
              printf("[difftest] =========================================\n\n");
              npc_state.state = NPC_ABORT;
              break;
            }
          }
          dt_check_pending = false;
        }

        if (skip_pending == 2) {
          skip_pending = 1;
        } else if (skip_pending == 1) {
          skip_pending = 0;
          cpu.pc = commit_pc;
          cpu_pc = cpu.pc;
          ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
          ref_difftest_exec(1);
        } else {
          difftest_one_exec();
        }
        dt_check_pending = true;
#endif
      }
    }

    if (npc_state.state == NPC_END) {
      npc_state.halt_pc = top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__wbu_io_in_bits_r_pc;
    }

    if (npc_state.state != NPC_RUNNING) {
      break;
    }
    if (++hang_counter > HANG_TIMEOUT_CYCLES) {
      printf("[HANG] No commit for %d cycles, simulation stopped\n", HANG_TIMEOUT_CYCLES);
#ifdef ENABLE_WAVEFORM
      if (tfp != nullptr) tfp->flush();
#endif
      set_npc_state(NPC_ABORT, read_pc_from_top(), 1);
      break;
    }
    if (stuck_cnt > PC_STUCK_COMMITS) {
      printf("[STUCK] PC 0x%08x committed %d times without change, simulation stopped\n",
             stuck_pc, PC_STUCK_COMMITS);
#ifdef ENABLE_WAVEFORM
      if (tfp != nullptr) tfp->flush();
#endif
      set_npc_state(NPC_ABORT, stuck_pc, 1);
      break;
    }
#ifdef ENABLE_WAVEFORM
    if (hang_counter % 50000 == 0 && tfp != nullptr) tfp->flush();
#endif
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