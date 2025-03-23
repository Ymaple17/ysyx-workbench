#include <cpu/cpu.h>
#include <cpu/difftest.h>
#include <mem/paddr.h>
#include <module.h>
#include <tracer/inst_tracer.h>
#include <tracer/wave_tracer.h>
#include <mem/mem.h>

#define MAX_INST_TO_PRINT 10

void device_update();
int wp_difftest();

CPU_state cpu;
static bool g_print_step = false;
static uint64_t cycle_cnt = 0;
static uint64_t inst_cnt = 0;
static uint64_t commit_cnt = 0;
int device_access_st = 0;

mem_t mem;
mem_t mem2;

void dpic_ebreak(paddr_t pc, uint32_t code) {
  npc_state.state = NPC_END;
  npc_state.halt_pc = pc;
  npc_state.halt_code = code;
}

#ifdef CONFIG_FTRACE
void trace_func_call(paddr_t pc, paddr_t target, bool is_tail);
void trace_func_ret(paddr_t pc);

void dpic_ftrace(paddr_t pc, paddr_t dnpc, uint8_t dest, word_t imm, uint8_t is_ret) {
  if (is_ret) {
    trace_func_ret(pc);
  } else if (dest == 0) {
    trace_func_call(pc, dnpc, true);
  } else if (dest == 1) {
    trace_func_call(pc, dnpc, false);
  }
}
#endif

// sync CPU_state
static void set_cpu() { 
  #include "set_cpu.h"
}

static void single_cycle() {
  ++cycle_cnt;
  dut.clock = 0;
  dut.eval();
  IFDEF(CONFIG_WTRACE, wavetracer.dump_single());
  dut.clock = 1;
  dut.eval();

  // imem
  dut.io_icacheAXI_ar_ready = true;
  dut.io_icacheAXI_r_valid = mem.r_valid();
  dut.io_icacheAXI_r_bits_data = mem.r_data();
  dut.io_icacheAXI_r_bits_last = mem.r_last();

  mem_input_t in;
  in.reset = dut.reset;
  in.ar_valid = dut.io_icacheAXI_ar_valid;
  in.ar_addr = dut.io_icacheAXI_ar_bits_addr;
  in.ar_len = dut.io_icacheAXI_ar_bits_len;
  in.ar_size = dut.io_icacheAXI_ar_bits_size;
  in.r_ready = dut.io_icacheAXI_r_ready;
  in.aw_valid = false;
  in.w_valid = false;

  mem.tick(in, "icache");

  // dmem
  dut.io_dcacheAXI_ar_ready = mem2.ar_ready();
  dut.io_dcacheAXI_r_valid = mem2.r_valid();
  // printf("r_fire = %d, ar_fire = %d, dcache_has_r_data = %d, rdata = " FMT_WORD "\n", mem2.r_valid() && dut.io_dcacheAXI_r_ready, mem2.ar_ready() && dut.io_dcacheAXI_ar_valid, mem2.has_r_data(), mem2.r_data());
  dut.io_dcacheAXI_r_bits_data = mem2.r_data();
  dut.io_dcacheAXI_r_bits_last = mem2.r_last();
  dut.io_dcacheAXI_aw_ready = mem2.aw_ready();
  dut.io_dcacheAXI_w_ready = mem2.w_ready();
  dut.io_dcacheAXI_b_valid = mem2.b_valid();

  mem_input_t in2;
  in2.reset = dut.reset;
  in2.ar_valid = dut.io_dcacheAXI_ar_valid;
  in2.ar_addr = dut.io_dcacheAXI_ar_bits_addr;
  in2.ar_len = dut.io_dcacheAXI_ar_bits_len;
  in2.ar_size = dut.io_dcacheAXI_ar_bits_size;
  in2.r_ready = dut.io_dcacheAXI_r_ready;
  in2.aw_valid = dut.io_dcacheAXI_aw_valid;
  in2.aw_len = dut.io_dcacheAXI_aw_bits_len;
  in2.aw_size = dut.io_dcacheAXI_aw_bits_size;
  in2.aw_addr = dut.io_dcacheAXI_aw_bits_addr;
  in2.w_valid = dut.io_dcacheAXI_w_valid;
  in2.w_strb = dut.io_dcacheAXI_w_bits_strb;
  in2.w_data = dut.io_dcacheAXI_w_bits_data;
  in2.w_last = dut.io_dcacheAXI_w_bits_last;
  in2.b_ready = dut.io_dcacheAXI_b_ready;

  mem2.tick(in2, "dcache");

  dut.eval();
  IFDEF(CONFIG_WTRACE, wavetracer.dump_single());
}

void cpu_reset(int n) {
  dut.reset = 1;
  while (n-- > 0) single_cycle();
  cpu.pc = 0x80000000;
  cpu.mstatus = 0xa00001800;
}

void dpic_commit(unsigned long long pc, unsigned int inst, unsigned long long npc, char dm_access) {
  // printf("commit %08x at " FMT_PADDR " npc=" FMT_PADDR "\n", inst, pc, npc);
  commit_cnt--;
  inst_cnt++;
  set_cpu();
  cpu.pc = npc;

  if (device_access_st && dm_access) {
    device_access_st--;
    difftest_skip_ref();
  }

  IFDEF(CONFIG_ITRACE, itracer.trace(pc, inst, g_print_step));
  IFDEF(CONFIG_WATCHPOINT, if (wp_difftest()) npc_state.state = NPC_STOP);
  device_update();
  IFDEF(CONFIG_DIFFTEST, difftest_step(pc));
}

static void exec(uint64_t n) {
  commit_cnt = n;

  if (dut.reset == 1) {
    dut.reset = 0;
    dut.eval();
    set_cpu();
  }

  while (commit_cnt > 0) {
    single_cycle();
    if (npc_state.state != NPC_RUNNING) break;
  }
}

void cpu_exec(uint64_t n) {
  g_print_step = (n <= MAX_INST_TO_PRINT);

  switch (npc_state.state) {
    case NPC_END:
    case NPC_ABORT:
      printf(
          "Program execution has ended. To restart the program, exit NPC and "
          "run again.\n");
      return;
    default:
      npc_state.state = NPC_RUNNING;
  }

  exec(n);

  switch (npc_state.state) {
    case NPC_RUNNING:
      npc_state.state = NPC_STOP;
      break;
    case NPC_END:
    case NPC_ABORT:
    if (npc_state.halt_code != 0) itracer.dump();
      // IFDEF(CONFIG_ITRACE, );
      Log("npc: %s \n\thalt_code=%d\n\thalt_pc=" FMT_PADDR "\n\t%lu instructions executed\n\tIPC=%0.6f",
          npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) : 
          (npc_state.halt_code == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
          ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED)),
          npc_state.halt_code, npc_state.halt_pc, inst_cnt, (double)inst_cnt/cycle_cnt);
      // fallthrough
  }
}
