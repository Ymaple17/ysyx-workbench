#include <stdint.h>
#include "Vtop.h"
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../../obj_dir/Vtop___024root.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../../include/generated/autoconf.h"
#include "../include/difftest.h"

#define MAX_INST_TO_PRINT 10
CPU_State cpu;
extern Vtop* top;
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
  set_npc_state(NPC_END, top->io_imem_pc, cpu_gpr[10]);
}

static inline bool in_pmem(uint32_t addr) {
  return addr - 0x80000000 < 0x8000000;
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
  rst_done = true;
}

void exec_once(){
  //negedge
  top->clock = 0;
  top->eval(); 
  #ifdef ENABLE_WAVEFORM
    if(tfp != nullptr && main_time > WAVE_time){
      tfp->dump(main_time); //dump wave
    }
  #endif
  main_time++;

  //posedge
  
  top->clock = 1;
  top->eval(); 
  cpu.pc = top->io_imem_pc;//rv32e
  #ifdef CONFIG_ITRACE 
    uint32_t current_inst = top->io_instr;
    itrace_inst(top->io_imem_pc, current_inst);
    display_inst();
  #endif
  for(int i = 0; i < 32; i++){
    //cpu.gpr[i] = top->rootp->top__DOT__u_RegisterFile__DOT__rg[i];//rv32e
    cpu.gpr[i] = top->rootp->top__DOT__core__DOT__refile__DOT__rf_ext__DOT__Memory[i];//chisel
  }
  #ifdef ENABLE_WAVEFORM
    if(tfp != nullptr && main_time > WAVE_time){
      tfp->dump(main_time); //dump wave
    }
  #endif
  main_time++; 
}

void device_update();

static void execute(uint64_t n) {
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();
    #ifdef CONFIG_DEVICE
      device_update();
    #endif

#ifdef CONFIG_DIFFTEST
      #ifdef CONFIG_MUL
        if (top->io_instr_complete) {
      #endif
      if (top->io_mem_valid && !in_pmem(top->io_mem_addr)) {
        difftest_skip_ref();
      } else { 
        difftest_one_exec(); 
        if (!difftest_check_reg()) { 
          panic("DiffTest: Register mismatch detected at PC = 0x%08x", cpu.pc);
        }
      }
      #ifdef CONFIG_MUL
      }
      #endif
#endif
    }
    // if (g_print_step) {
      
    //   printf("execute at pc = 0x%08x\n", top->io_imem_pc);
    // }
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
               out, top->io_imem_pc); 
      }
      else{
        out = (char *)"ABORT";
        #ifdef DIFFTEST
          difftest_one_exec(); 
        #endif
        printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
               out, top->io_imem_pc); 
      }
      break;
    case NPC_ABORT: 
      out = (char *)"ABORT"; 
      printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
             out, top->io_imem_pc); 
      break;
    default: 
      out = (char *)"HIT BAD TRAP"; 
      printf("npc: " ANSI_FG_RED "%s" ANSI_RESET " at pc = 0x%08x\n", 
             out, top->io_imem_pc); 
      break;
  }
}
