#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <signal.h>
#include "verilated_fst_c.h"
#include "VysyxSoCFull.h"
#include "svdpi.h"
#include "include/common.h"
#include "include/init.h"
#include "include/memory.h"
#include "include/sdb.h"
#include "include/state.h"
#include "include/cpu.h"
#include "include/difftest.h"
#include "../include/generated/autoconf.h"
void difftest_init(const char* ref_so_file, word_t img_size);
const char* img_path = NULL;
void init_disasm();
VysyxSoCFull *top = new VysyxSoCFull("top");
#ifdef ENABLE_WAVEFORM
VerilatedFstC* tfp = new VerilatedFstC;
#else
#endif

static void sig_handler(int sig) {
  printf("\n[CTRL-C] Simulation interrupted\n");
  set_npc_state(NPC_ABORT, 0x30000000, 1);
}

int main(int argc, char **argv){
  signal(SIGINT, sig_handler);

  if (argc < 2) {
    fprintf(stderr, "Usage: %s <program_file>\n", argv[0]);
    return 1;
  }
  img_path = argv[1];
  Verilated::commandArgs(argc, argv); 
  #ifdef ENABLE_WAVEFORM
    Verilated::traceEverOn(true);
    top->trace(tfp, 0);
    tfp->open("wave.fst");
  #endif

  set_npc_state(NPC_STOP, 0x30000000, 0);
  init_mem();
  init_npc_cpu();
  init_sdb();
  #ifdef CONFIG_ITRACE
    init_disasm();
  #endif
  #ifdef CONFIG_DEVICE
    init_device();
  #endif
  npc_state.state = NPC_RUNNING;
  #ifdef DIFFTEST
    difftest_init(STR2(DIFFTEST_LIB), img_size);
  #endif
  sdb_mainloop();

  top->final();
  #ifdef ENABLE_WAVEFORM
    tfp->close();
    delete tfp;
  #endif
  delete top;
 
  return 0;
}