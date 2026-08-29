#include <stdint.h>
#include <stdlib.h>
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
  top->io_interrupt = 0; // 4g：仿真默认不拉外部中断（冒烟时再人工拉高）
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
struct DiffTestState {
  bool check_pending;
  bool is_mmio;
} dt_state = {false, false};
static word_t last_wbu_pc = 0;

#endif

#define HANG_TIMEOUT_CYCLES 10000
#define PC_STUCK_COMMITS 20000

static void execute(uint64_t n) {
  uint64_t hang_counter = 0;
  static word_t stuck_pc = 0;
  static int stuck_cnt = 0;
  static bool stuck_initialized = false;
  static bool pipe_trace_initialized = false;
  static bool pipe_trace_enabled = false;
  static uint32_t pipe_trace_lo = 0;
  static uint32_t pipe_trace_hi = UINT32_MAX;
  static uint32_t trace_rob_pc[32] = {};
  static uint32_t last_alu_pc = UINT32_MAX;
  static uint32_t last_lsu_pc = UINT32_MAX;
  static unsigned last_alu_rob = UINT32_MAX;
  static unsigned last_lsu_rob = UINT32_MAX;
  static uint32_t recent_commit_pc[32] = {};
  static unsigned recent_commit_pos = 0;
  auto record_commit = [&](uint32_t pc) {
    recent_commit_pc[recent_commit_pos & 31u] = pc;
    recent_commit_pos++;
  };
  if (!pipe_trace_initialized) {
    pipe_trace_initialized = true;
    const char *range = getenv("NPC_PIPE_TRACE");
    if (range != nullptr) {
      pipe_trace_enabled = true;
      unsigned lo = 0;
      unsigned hi = UINT32_MAX;
      if (sscanf(range, "%x:%x", &lo, &hi) == 2) {
        pipe_trace_lo = lo;
        pipe_trace_hi = hi;
      }
    }
  }
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();

      // Temporary pipeline tracing used during Stage14 bring-up.
#if 0
      if (getenv("NPC_CTRL_TRACE") != nullptr) {
        auto *r = top->rootp;
        const unsigned long long trace_cycle = main_time / 2;
        const bool targetBranch = r->ysyx_25020039__DOT__core__DOT__d_bru_valid &&
          r->ysyx_25020039__DOT__core__DOT__d_bru_bits_pc == 0x80000ee0;
        const bool targetEnq =
          r->ysyx_25020039__DOT__core__DOT____Vcellinp__brq__io_enq_bits_pc == 0x80000ee0;
        const bool targetCommit = r->io_commit_valid2 &&
          r->io_commit_pc2 == 0x80000ee0;
        const bool targetWindow = trace_cycle >= 147170 && trace_cycle <= 147190;
        if (targetBranch || targetEnq || targetCommit || targetWindow) {
          printf("[ctrl-trace] cycle=%llu d_v=%u d_pc=0x%08x d_rob=%u "
                 "resolved=%u out_v=%u taken=%u leave=%u flush=%u no_dest=%u "
                 "rd1=0x%08x rd2=0x%08x inst=0x%08x commit2=%u "
                 "cm2_taken=%u cm2_target=0x%08x\n",
                 trace_cycle,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_ctrl_resolved,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_bits_br_taken,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__bru_leave,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__flush_bru,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_ctrl_no_dest,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rd1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rd2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_inst,
                 (unsigned)r->io_commit_valid2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_actual_taken,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_actual_target);
          CData *brq_valid[] = {
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_0_valid,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_1_valid,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_2_valid,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_3_valid,
          };
          CData *brq_rob[] = {
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_0_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_1_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_2_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_3_rob_idx,
          };
          CData *brq_ready[] = {
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_0_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_1_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_2_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_3_src1_ready,
          };
          CData *brq_phys[] = {
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_0_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_1_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_2_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_3_src1_phys,
          };
          IData *brq_value[] = {
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_0_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_1_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_2_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__brq__DOT__entries_3_src1_val,
          };
          printf("[ctrl-trace] brq ");
          for (int i = 0; i < 4; i++) {
            if (*brq_valid[i]) {
              printf("s%d={rob%u r%u p%u v%08x} ", i,
                     (unsigned)*brq_rob[i], (unsigned)*brq_ready[i],
                     (unsigned)*brq_phys[i], (unsigned)*brq_value[i]);
            }
          }
          printf("issue_p=%u/%u\n",
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___brq_io_issue_bits_src1_phys,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___brq_io_issue_bits_src2_phys);
          printf("[ctrl-trace] enq pc=0x%08x rob=%u p=%u r=%u view_r=%u view_v=0x%08x "
                 "busy15=%u id2_src1=0x%08x hits=%u/%u/%u/%u/%u\n",
                 (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__brq__io_enq_bits_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__brq__io_enq_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__brq__io_enq_bits_src1_phys,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__brq__DOT__enqView_src1_ready,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__brq__DOT__enqView_src1_ready,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__brq__DOT__enqView_src1_val,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__busy__DOT__impl__DOT__busy_15,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1_cdb_hit,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1_cdb1_hit,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1_cdb2_hit,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1_cdb3_hit,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id2_src1_exu_hit);
          printf("[ctrl-trace] wb={%u:p%u=0x%08x@%08x %u:p%u=0x%08x@%08x "
                 "%u:p%u=0x%08x@%08x %u:p%u=0x%08x@%08x}\n",
                 (unsigned)r->io_debug_prf_wen_0, (unsigned)r->io_debug_prf_waddr_0,
                 (unsigned)r->io_debug_prf_wdata_0, (unsigned)r->io_debug_prf_wpc_0,
                 (unsigned)r->io_debug_prf_wen_1, (unsigned)r->io_debug_prf_waddr_1,
                 (unsigned)r->io_debug_prf_wdata_1, (unsigned)r->io_debug_prf_wpc_1,
                 (unsigned)r->io_debug_prf_wen_2, (unsigned)r->io_debug_prf_waddr_2,
                 (unsigned)r->io_debug_prf_wdata_2, (unsigned)r->io_debug_prf_wpc_2,
                 (unsigned)r->io_debug_prf_wen_3, (unsigned)r->io_debug_prf_waddr_3,
                 (unsigned)r->io_debug_prf_wdata_3, (unsigned)r->io_debug_prf_wpc_3);
        }
        if (r->ysyx_25020039__DOT__core__DOT__d_lsu_valid &&
            r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc == 0x80000ee0) {
          printf("[ctrl-trace] lsu cycle=%llu pc=0x%08x rob=%u wr=%u addr=0x%08x "
                 "base=0x%08x data=0x%08x imm=0x%08x query=%u/q%u "
                 "sq_wait=%u sq_unknown=0x%08x sq_unresolved=0x%08x "
                 "ar=%u arid=%u araddr=0x%08x sq_fwd=%u sq_data=0x%08x sb_fwd=%u "
                 "head_st={a%08x d%08x} enq=%u/%u\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_signals_lsu_mem_write,
                 (unsigned)(r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd1 +
                   r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_imm_ext),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_imm_ext,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_wait_load,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_older_unresolved_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_araddr,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_data,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___stbuf_io_ld_fwd_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__head_st_addr,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__head_st_data,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__stbuf__io_enq_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__stbuf__io_enq1_valid);
        }
        const bool targetLsu = r->ysyx_25020039__DOT__core__DOT__d_lsu_valid &&
          r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc == 0x80000ee0;
        const bool targetQuery = r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_valid &&
          r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob >= 24 &&
          r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob <= 31;
        if (targetQuery || targetLsu || targetWindow) {
          printf("[ctrl-trace] query cycle=%llu rob=%u sq_wait=%u sq_unknown=0x%08x "
                 "sq_unresolved=0x%08x sq_fwd=%u stbuf_fwd=%u ar=%u arid=%u "
                 "araddr=0x%08x\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_wait_load,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_older_unresolved_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___stbuf_io_ld_fwd_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_araddr);
        }
        if (targetQuery || targetLsu || targetWindow) {
          CData *lq_valid[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
          };
          CData *lq_state[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state,
          };
          CData *lq_rob[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
          };
          IData *lq_pc[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
          };
          IData *lq_addr[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_addr,
          };
          IData *lq_mem_read[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_mem_read,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_mem_read,
          };
          IData *lq_bypass[] = {
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores,
          };
          IData *stbuf_addr[] = {
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_0_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_1_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_2_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_3_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_4_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_5_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_6_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_7_addr,
          };
          IData *stbuf_data[] = {
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_0_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_1_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_2_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_3_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_4_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_5_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_6_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_7_data,
          };
          CData *stbuf_mask[] = {
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_0_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_1_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_2_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_3_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_4_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_5_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_6_mask,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_7_mask,
          };
          printf("[ctrl-trace] lq cycle=%llu queryRob=%u dmemR=%u/0x%08x "
                 "sb_tail=%u sb_count=%u sb_free=%u ",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                 (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rvalid,
                 (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rdata,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__tail,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__count,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___stbuf_io_free);
          for (int i = 0; i < 8; i++) {
            if (*lq_valid[i] && (*lq_pc[i] == 0x80004f08 || targetWindow)) {
              printf("s%d={st%u rob%u a%08x v%08x b%08x} ", i,
                     (unsigned)*lq_state[i], (unsigned)*lq_rob[i],
                     (unsigned)*lq_addr[i], (unsigned)*lq_mem_read[i],
                     (unsigned)*lq_bypass[i]);
            }
          }
          for (int i = 0; i < 8; i++) {
            if (*stbuf_mask[i] != 0) {
              printf("sb%d={a%08x d%08x m%x} ", i,
                     (unsigned)*stbuf_addr[i], (unsigned)*stbuf_data[i],
                     (unsigned)*stbuf_mask[i]);
            }
          }
          printf("\n");
          if (targetWindow) {
            printf("[ctrl-trace] resolve valid=%u rob=%u addr=0x%08x mask=0x%x "
                   "violation_w=%u mem_flush=%u flush_now=%u commit=%u head=%u "
                   "candidates=0x%02x\n",
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_rob,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_addr,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__mem_violation_w,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__memoryRecoveryWins,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__flush_now,
                   (unsigned)r->io_commit_valid,
                   (unsigned)r->io_debug_rob_head_pc,
                   (unsigned)(r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_0 |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_1 << 1) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_2 << 2) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_3 << 3) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_4 << 4) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_5 << 5) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_6 << 6) |
                     (r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__violationCandidates_7 << 7)));
            printf("[ctrl-trace] deps sq4={v%u r%u a%08x d%08x m%x} "
                   "sq14={v%u r%u a%08x d%08x m%x} "
                   "can_bypass=%u fwd_mask=0x%08x direct_bypass=0x%08x "
                   "rvalid=%u arid=%u rdata=0x%08x resp_cur=%u resp_out=%u\n",
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_4_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_4_addr_ready,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_4_addr,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_4_data,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_4_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_14_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_14_addr_ready,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_14_addr,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_14_data,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__sq__DOT__entries_14_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__canBypassUnknown,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_older_unresolved_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__directBypassedStores,
                   (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rvalid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arid,
                   (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rdata,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__respMatchesCurrent,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__respMatchesOutstanding);
          }
        }
      }

      #if 0
      if (getenv("NPC_LQ_TRACE") != nullptr) {
        auto *r = top->rootp;
        auto trace_lq_entry = [&](int i, CData *valid, CData *state,
                                  CData *rob, IData *pc, IData *addr,
                                  IData *bypass) {
          if (*valid && *pc >= 0x80004f00 && *pc <= 0x80004f10) {
            printf("[lq-trace] cycle=%llu slot=%d state=%u rob=%u pc=0x%08x "
                   "addr=0x%08x bypass=0x%08x\n",
                   (unsigned long long)(main_time / 2), i,
                   (unsigned)*state, (unsigned)*rob, (unsigned)*pc,
                   (unsigned)*addr, (unsigned)*bypass);
          }
        };
        trace_lq_entry(0,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_0_bypassedStores);
        trace_lq_entry(1,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_1_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_1_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_1_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_1_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_1_bypassedStores);
        trace_lq_entry(2,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_2_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_2_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_2_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_2_bypassedStores);
        trace_lq_entry(3,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_3_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_3_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_3_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_3_bypassedStores);
        trace_lq_entry(4,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_4_bypassedStores);
        trace_lq_entry(5,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_5_bypassedStores);
        trace_lq_entry(6,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__entries_6_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores);
        trace_lq_entry(7,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores);
        if (r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_valid) {
          printf("[lq-trace] cycle=%llu resolve rob=%u addr=0x%08x mask=0x%x "
                 "sq_unresolved=0x%08x violation=%u\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_addr,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__mem_violation_w);
        }
      }

      #endif
      if (getenv("NPC_MEM_TRACE") != nullptr) {
        auto *r = top->rootp;
        const unsigned long long trace_cycle = main_time / 2;
        if ((trace_cycle >= 3800 && trace_cycle <= 3920) ||
            (trace_cycle >= 10900 && trace_cycle <= 11070)) {
          const bool prf_wen[] = {
            r->io_debug_prf_wen_0, r->io_debug_prf_wen_1,
            r->io_debug_prf_wen_2, r->io_debug_prf_wen_3,
          };
          const unsigned prf_waddr[] = {
            (unsigned)r->io_debug_prf_waddr_0, (unsigned)r->io_debug_prf_waddr_1,
            (unsigned)r->io_debug_prf_waddr_2, (unsigned)r->io_debug_prf_waddr_3,
          };
          const uint32_t prf_wdata[] = {
            (uint32_t)r->io_debug_prf_wdata_0, (uint32_t)r->io_debug_prf_wdata_1,
            (uint32_t)r->io_debug_prf_wdata_2, (uint32_t)r->io_debug_prf_wdata_3,
          };
          const uint32_t prf_wpc[] = {
            (uint32_t)r->io_debug_prf_wpc_0, (uint32_t)r->io_debug_prf_wpc_1,
            (uint32_t)r->io_debug_prf_wpc_2, (uint32_t)r->io_debug_prf_wpc_3,
          };
          for (unsigned lane = 0; lane < 4; lane++) {
            if (prf_wen[lane] && prf_waddr[lane] == 33 &&
                trace_cycle >= 3800 && trace_cycle <= 3920) {
              printf("[prf-recover] cycle=%llu lane=%u pc=0x%08x p33=0x%08x\n",
                     trace_cycle, lane, prf_wpc[lane], prf_wdata[lane]);
            }
            if (prf_wen[lane] && prf_wpc[lane] == 0x800026e8 &&
                trace_cycle >= 10900 && trace_cycle <= 11070) {
              printf("[load-focus] cycle=%llu wb_lane=%u pc=0x%08x rob_hint=%u "
                     "pdest=%u data=0x%08x\n",
                     trace_cycle, lane, prf_wpc[lane],
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
                     prf_waddr[lane], prf_wdata[lane]);
              CData *lq_valid[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
              };
              CData *lq_rob[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
              };
              IData *lq_pc[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
              };
              IData *lq_addr[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_addr,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_addr,
              };
              IData *lq_bypass[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores,
              };
              CData *lq_state[] = {
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state,
                &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state,
              };
              printf("[load-focus] LQ:");
              for (int i = 0; i < 8; i++) {
                if (*lq_valid[i]) {
                  printf(" %d{rob=%u pc=0x%08x addr=0x%08x bypass=0x%08x st=%u}",
                         i, (unsigned)*lq_rob[i], (unsigned)*lq_pc[i],
                         (unsigned)*lq_addr[i], (unsigned)*lq_bypass[i],
                         (unsigned)*lq_state[i]);
                }
              }
              printf("\n");
            }
          }
        }
        if (trace_cycle >= 10900 && trace_cycle <= 11070) {
          auto print_lq_target = [&](int slot, CData *valid, IData *pc,
                                     CData *rob, IData *addr, IData *bypass,
                                     CData *state) {
            if (*valid && *pc == 0x800026e8) {
              printf("[load-focus] cycle=%llu lq_slot=%d rob=%u addr=0x%08x "
                     "bypass=0x%08x state=%u\n", trace_cycle, slot,
                     (unsigned)*rob, (unsigned)*addr, (unsigned)*bypass,
                     (unsigned)*state);
            }
          };
          print_lq_target(0,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state);
          print_lq_target(1,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state);
          print_lq_target(2,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state);
          print_lq_target(3,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state);
          print_lq_target(4,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state);
          print_lq_target(5,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state);
          print_lq_target(6,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state);
          print_lq_target(7,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_addr,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores,
            &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state);
          if (r->ysyx_25020039__DOT__core__DOT__d_lsu_valid &&
              r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc == 0x800026e8) {
            printf("[load-focus] cycle=%llu issue rob=%u pdest=%u addr=0x%08x "
                   "head_pc=0x%08x sq_wait=%u sq_fwd=%u unknown=0x%08x "
                   "can_bypass=%u sched_bus=%u sched_fwd=%u sched_idx=%u "
                   "arvalid=%u arready=%u fwd_data=0x%08x "
                   "alloc=%u/%u query=%u/%u "
                   "resolve=%u/%u\n",
                   trace_cycle,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rob_idx,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pdest,
                   (unsigned)(r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd1 +
                     r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_imm_ext),
                   (unsigned)r->io_debug_rob_head_pc,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_wait_load,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_older_unresolved_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__canBypassUnknown,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedulerBus,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedulerForward,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedIdx,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
                   (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_arready,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_data,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT____Vcellinp__lq__io_alloc_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT___lq_io_alloc_ready,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_rob);
          }
          if (r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_valid) {
            printf("[load-focus] cycle=%llu query rob=%u sched_idx=%u can_bypass=%u "
                   "sched_bus=%u sched_fwd=%u arvalid=%u resp_out=%u resp_cur=%u\n",
                   trace_cycle,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedIdx,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__canBypassUnknown,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedulerBus,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__schedulerForward,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__respMatchesOutstanding,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__respMatchesCurrent);
          }
        }
        if (trace_cycle >= 10900 && trace_cycle <= 11070) {
          const uint32_t commit_pc[] = {
            (uint32_t)r->io_commit_pc,
            (uint32_t)r->io_commit_pc1,
            (uint32_t)r->io_commit_pc2,
            (uint32_t)r->io_commit_pc3,
          };
          const bool commit_valid[] = {
            (bool)r->io_commit_valid, (bool)r->io_commit_valid1,
            (bool)r->io_commit_valid2, (bool)r->io_commit_valid3,
          };
          for (unsigned lane = 0; lane < 4; lane++) {
            if (commit_valid[lane] &&
                (commit_pc[lane] == 0x800026ac || commit_pc[lane] == 0x800026e8 ||
                 commit_pc[lane] == 0x800026f0)) {
              printf("[load-focus] cycle=%llu commit_lane=%u pc=0x%08x a5=0x%08x "
                     "lq0_valid=%u lq0_rob=%u lq0_pc=0x%08x lq0_addr=0x%08x "
                     "lq0_bypass=0x%08x lq0_state=%u unresolved=0x%08x\n",
                     trace_cycle, lane, commit_pc[lane], (unsigned)read_gpr_from_top(5),
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
                     (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask);
            }
          }
        }
        if (trace_cycle >= 3875 && trace_cycle <= 3910 &&
            r->ysyx_25020039__DOT__core__DOT__d_bru_valid) {
          printf("[bru-recover] cycle=%llu pc=0x%08x rob=%u pdest=%u rd1=0x%08x "
                 "imm=0x%08x jump=%u rename=%u cp=%u bp=%u/%u target=0x%08x "
                 "mis=%u restore_idx=%u rat1=%u out=%u result=0x%08x\n",
                 trace_cycle,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_pdest,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rd1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_imm_ext,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_signals_exu_jump,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_do_rename,
                 0U,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_bp_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_bp_taken,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_bp_target,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__mis_predict_dbg,
                 0U,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rename__DOT__impl__DOT__rat_1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_bits_alu_result);
        }
        if (r->ysyx_25020039__DOT__core__DOT__d_lsu_valid &&
            r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc == 0x80003810) {
          printf("[mem-recover] cycle=%llu d-lsu rob=%u pdest=%u data=0x%08x rat1=%u "
                 "rf32=0x%08x rf33=0x%08x id_psrc2=%u id_src2=0x%08x flush=%u "
                 "stage=%u stage_rob=%u stage_pc=0x%08x\n",
                 trace_cycle,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pdest,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rename__DOT__impl__DOT__rat_1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__prf__DOT__impl__DOT__rf_32,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__prf__DOT__impl__DOT__rf_33,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id_psrc2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id_src2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__flush_lsu_d,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_stage_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_stage_bits_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_stage_bits_pc);
        }
        if (r->ysyx_25020039__DOT__core__DOT__mem_violation_w) {
          printf("[mem-focus] cycle=%llu violation store_rob=%u addr=0x%08x mask=0x%x "
                 "flush_idx=%u redirect=0x%08x rob_tail=%u rob_count=%u\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_addr,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_mask,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__flush_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__correct_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__tail,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__count);
          printf("[mem-recover] lq_rob=%u lq_pc=0x%08x rat1=%u rb_base1=%u\n",
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rename__DOT__impl__DOT__rat_1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rb_base_1);
          CData *rob_valid[] = {
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_0_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_1_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_2_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_3_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_4_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_5_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_6_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_7_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_8_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_9_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_11_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_12_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_13_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_14_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_15_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_16_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_17_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_18_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_19_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_20_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_21_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_22_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_23_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_24_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_25_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_26_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_27_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_28_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_29_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_30_valid,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_31_valid,
          };
          IData *rob_pc[] = {
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_0_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_1_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_2_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_3_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_4_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_5_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_6_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_7_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_8_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_9_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_11_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_12_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_13_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_14_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_15_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_16_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_17_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_18_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_19_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_20_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_21_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_22_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_23_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_24_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_25_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_26_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_27_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_28_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_29_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_30_pc,
            &r->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_31_pc,
          };
          printf("[mem-recover] ROB:");
          for (int i = 0; i < 32; i++) {
            if (*rob_valid[i]) printf(" %d:0x%08x", i, (unsigned)*rob_pc[i]);
          }
          printf("\n");
        }
        const bool load_addr_hit = r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid &&
          r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_araddr == 0x8000efa4;
        const bool load_resp_hit = r->ysyx_25020039__DOT___xbar_io_dmem_rvalid &&
          r->ysyx_25020039__DOT__core__DOT___dcache_io_cpu_rid != 0;
        if (load_addr_hit || load_resp_hit) {
          printf("[mem-trace] cycle=%llu ar=%u arid=%u araddr=0x%08x "
                 "query=%u qrob=%u sq_wait=%u sq_fwd=%u "
                 "resp=%u rid=%u rdata=0x%08x\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_araddr,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_ld_query_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_wait_load,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_fwd_valid,
                 (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rvalid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___dcache_io_cpu_rid,
                 (unsigned)r->ysyx_25020039__DOT___xbar_io_dmem_rdata);
        }
        if (load_addr_hit) {
          printf("[mem-focus] cycle=%llu dcache line157 valid=%u tag=0x%05x words="
                 "%08x,%08x,%08x,%08x,%08x,%08x,%08x,%08x stbuf_count=%u\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_valid,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_tag,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_0,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_3,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_4,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_5,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_6,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__dcache__DOT__lines_157_data_7,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__count);
          IData *st_addr[] = {
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_0_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_1_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_2_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_3_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_4_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_5_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_6_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_7_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_8_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_9_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_10_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_11_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_12_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_13_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_14_addr,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_15_addr,
          };
          IData *st_data[] = {
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_0_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_1_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_2_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_3_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_4_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_5_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_6_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_7_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_8_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_9_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_10_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_11_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_12_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_13_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_14_data,
            &r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__entries_15_data,
          };
          for (int i = 0; i < 16; i++) {
            if (*st_addr[i] == 0x8000efa4 || *st_addr[i] == 0x8000efa0) {
              printf("[mem-focus] stbuf[%d] addr=0x%08x data=0x%08x\n",
                     i, (unsigned)*st_addr[i], (unsigned)*st_data[i]);
            }
          }
        }
        if (r->ysyx_25020039__DOT__core__DOT___stbuf_io_drain_addr == 0x8000efa4 &&
            r->ysyx_25020039__DOT___core_io_dmem_wvalid) {
          printf("[mem-focus] cycle=%llu store-drain addr=0x%08x data=0x%08x mask=0x%x "
                 "stbuf_state=%u beat=%u burst=%u count=%u\n",
                 (unsigned long long)(main_time / 2),
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___stbuf_io_drain_addr,
                 (unsigned)r->ysyx_25020039__DOT___core_io_dmem_wdata,
                 (unsigned)r->ysyx_25020039__DOT___core_io_dmem_wstrb,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__state,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__writeBeat,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__burstCount,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__stbuf__DOT__count);
        }
      }

      if (pipe_trace_enabled && rst_done && !top->reset) {
        auto *r = top->rootp;
        const unsigned long long trace_cycle = main_time / 2;
        const bool alu_valid = r->ysyx_25020039__DOT__core__DOT__d_alu_valid;
        const uint32_t alu_pc = r->ysyx_25020039__DOT__core__DOT__d_alu_bits_pc;
        const unsigned alu_rob = r->ysyx_25020039__DOT__core__DOT__d_alu_bits_rob_idx;
        const bool lsu_valid = r->ysyx_25020039__DOT__core__DOT__d_lsu_valid;
        const uint32_t lsu_pc = r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc;
        const unsigned lsu_rob = r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rob_idx;
        if (alu_valid) trace_rob_pc[alu_rob & 31u] = alu_pc;
        if (lsu_valid) trace_rob_pc[lsu_rob & 31u] = lsu_pc;
        if (alu_valid && alu_pc >= pipe_trace_lo && alu_pc <= pipe_trace_hi &&
            (alu_pc != last_alu_pc || alu_rob != last_alu_rob)) {
          printf("[PIPE] ALU issue pc=0x%08x rob=%u pdest=%u src=0x%08x/0x%08x ctl=%u\n",
                 alu_pc, alu_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_pdest,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_rd1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_rd2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_signals_exu_alu_control);
          last_alu_pc = alu_pc;
          last_alu_rob = alu_rob;
        }
        if (lsu_valid && lsu_pc >= pipe_trace_lo && lsu_pc <= pipe_trace_hi &&
            (lsu_pc != last_lsu_pc || lsu_rob != last_lsu_rob)) {
          printf("[PIPE] LSU issue pc=0x%08x rob=%u pdest=%u base=0x%08x data=0x%08x imm=0x%08x wr=%u\n",
                 lsu_pc, lsu_rob,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pdest,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rd2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_imm_ext,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_signals_lsu_mem_write);
          last_lsu_pc = lsu_pc;
          last_lsu_rob = lsu_rob;
        }
        if (trace_cycle >= 147155 && trace_cycle <= 147185) {
          CData *rs_valid[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_valid,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_valid,
          };
          CData *rs_issued[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_issued,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_issued,
          };
          CData *rs_rob[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_rob_idx,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_rob_idx,
          };
          CData *rs_s1_ready[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_src1_ready,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_src1_ready,
          };
          CData *rs_s1_phys[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_src1_phys,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_src1_phys,
          };
          IData *rs_s1_val[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_src1_val,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_src1_val,
          };
          IData *rs_pc[] = {
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_0_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_1_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_2_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_3_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_4_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_5_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_6_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_7_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_8_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_9_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_10_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_11_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_12_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_13_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_14_pc,
            &r->ysyx_25020039__DOT__core__DOT__rs__DOT__impl__DOT__entries_15_pc,
          };
          printf("[PIPE] RS cycle=%llu ", trace_cycle);
          for (int i = 0; i < 16; i++) {
            if (*rs_valid[i] && *rs_pc[i] >= pipe_trace_lo && *rs_pc[i] <= pipe_trace_hi) {
              printf("s%d={pc%08x rob%u i%u p%u r%u v%08x} ", i,
                     (unsigned)*rs_pc[i], (unsigned)*rs_rob[i],
                     (unsigned)*rs_issued[i], (unsigned)*rs_s1_phys[i],
                     (unsigned)*rs_s1_ready[i], (unsigned)*rs_s1_val[i]);
            }
          }
          printf("\n");
          printf("[PIPE] RN cycle=%llu rat7=%u rat12=%u p2=%u p3=%u p3d=%u r3=%u d3=%u src3=0x%08x dep3=%u\n",
                 trace_cycle,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rename__DOT__impl__DOT__rat_7,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__rename__DOT__impl__DOT__rat_12,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___rename_io_rs1_phys2,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___rename_io_rs1_phys3,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT___rename_io_dest_phys3,
                 0U,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id3_doren,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id3_src1,
                 (unsigned)r->ysyx_25020039__DOT__core__DOT__id3_dep1);
        }
      }
#endif
    #ifdef CONFIG_DEVICE
      device_update();
    #endif

#ifdef CONFIG_AXI_MONITOR
      axi_monitor_check();
#endif

      // OOOD：提交点为 ROB COMMIT 导出信号
      bool wbu_valid = top->rootp->io_commit_valid;
      bool reset = top->reset;

      if (wbu_valid && !reset && rst_done) {
        hang_counter = 0;
        word_t commit_pc = top->rootp->io_commit_pc;
        record_commit(commit_pc);

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
        // ---- 检查上一条指令（dt_check_pending 在上一次提交末尾置位）----
        if (dt_state.check_pending) {
          update_cpu_state();   // 必须先读 DUT 状态：MMIO skip 的同步要用最新的寄存器
          // 上一条是 MMIO：跳过 NEMU 对它的执行，同步寄存器后继续
          if (dt_state.is_mmio) {
            difftest_skip_ref();
            cpu.pc = last_wbu_pc + 4;
            cpu_pc = cpu.pc;
            difftest_one_exec();   // is_skip_ref 置位时内部只做 regcpy（同步），不执行
          }
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
            auto *r = top->rootp;
            printf("[debug] cycle=%llu commit=0x%08x violation=%u resolve=%u resolve_rob=%u "
                   "resolve_mask=0x%x unresolved=0x%08x\n",
                   (unsigned long long)(main_time / 2), (unsigned)commit_pc,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__mem_violation_w,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_rob,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__lq_store_resolve0_mask,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask);
            CData *lq_valid[] = {
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
            };
            CData *lq_state[] = {
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state,
            };
            CData *lq_rob[] = {
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
            };
            IData *lq_pc[] = {
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
            };
            IData *lq_bypass[] = {
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores,
              &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores,
            };
            for (int i = 0; i < 8; i++) {
              if (*lq_valid[i]) {
                printf("[debug] lq[%d] state=%u rob=%u pc=0x%08x bypass=0x%08x\n",
                       i, (unsigned)*lq_state[i], (unsigned)*lq_rob[i],
                       (unsigned)*lq_pc[i], (unsigned)*lq_bypass[i]);
              }
            }
            printf("[debug] recent commits:");
            const unsigned recent_count = recent_commit_pos < 32 ? recent_commit_pos : 32;
            for (unsigned i = 0; i < recent_count; i++) {
              const unsigned pos = (recent_commit_pos - recent_count + i) & 31u;
              printf(" 0x%08x", recent_commit_pc[pos]);
            }
            printf("\n");
            npc_state.state = NPC_ABORT;
            break;
          }
          dt_state.check_pending = false;
        }


        // ---- PC 检查：DUT 提交 PC 必须与 NEMU 当前 PC 一致（防静默错位）----
        {
          CPU_State ref_state;
          ref_difftest_regcpy(&ref_state, DIFFTEST_TO_DUT);
          if (commit_pc != ref_state.pc) {
            printf("\n[difftest] ========== PC MISMATCH ==========\n");
            printf("[difftest] PC: REF = 0x%08x, DUT(commit) = 0x%08x\n", ref_state.pc, commit_pc);
            printf("[difftest] ===================================\n\n");
            npc_state.state = NPC_ABORT;
            break;
          }
        }

        // ---- 检测当前指令是否 MMIO ----
        word_t wbu_alu = top->rootp->io_commit_mem_addr;
        uint32_t inst = pmem_read(commit_pc, 4);
        bool is_store = ((inst & 0x7f) == 0x23);
        bool is_load_wb = top->rootp->io_commit_is_load;

        dt_state.is_mmio = (is_store || is_load_wb) && is_mmio_addr(wbu_alu);
        last_wbu_pc = commit_pc;

        if (!dt_state.is_mmio) {
          difftest_one_exec();
        }

        auto check_extra_commit = [&](int lane, word_t commit_pc_lane,
                                      word_t mem_addr, bool is_load) -> bool {
#ifdef CONFIG_ITRACE
          {
            uint32_t inst_lane = pmem_read(commit_pc_lane, 4);
            itrace_inst(commit_pc_lane, inst_lane);
            display_inst();
          }
#endif

          CPU_State ref_state_lane;
          ref_difftest_regcpy(&ref_state_lane, DIFFTEST_TO_DUT);
          if (commit_pc_lane != ref_state_lane.pc) {
            printf("\n[difftest] ========== PC MISMATCH lane%d ==========\n", lane);
            printf("[difftest] PC: REF = 0x%08x, DUT(commit%d) = 0x%08x\n",
                   ref_state_lane.pc, lane, commit_pc_lane);
            auto *r = top->rootp;
            printf("[debug] bundle p0=0x%08x p1=%s0x%08x p2=%s0x%08x p3=%s0x%08x\n",
                   (unsigned)top->rootp->io_commit_pc,
                   top->rootp->io_commit_valid1 ? "" : "-",
                   (unsigned)top->rootp->io_commit_pc1,
                   top->rootp->io_commit_valid2 ? "" : "-",
                   (unsigned)top->rootp->io_commit_pc2,
                   top->rootp->io_commit_valid3 ? "" : "-",
                   (unsigned)top->rootp->io_commit_pc3);
            printf("[debug] cm2_taken=%u cm2_target=0x%08x cm2_bpu=%u cm2_ctrl_wb=%u "
                   "d_bru_v=%u d_bru_rob=%u bru_out_v=%u bru_taken=%u mis=%u commit2_idx=%u\n",
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_actual_taken,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_actual_target,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_bpu_update,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__cm2_ctrl_wb,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__d_bru_bits_rob_idx,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_valid,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_bru_io_out_bits_br_taken,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT__mis_predict_dbg,
                   (unsigned)r->ysyx_25020039__DOT__core__DOT___rob_io_commit2_idx);
            printf("[debug] recent commits:");
            const unsigned recent_count = recent_commit_pos < 32 ? recent_commit_pos : 32;
            for (unsigned i = 0; i < recent_count; i++) {
              const unsigned pos = (recent_commit_pos - recent_count + i) & 31u;
              printf(" 0x%08x", recent_commit_pc[pos]);
            }
            printf("\n");
            printf("[difftest] =========================================\n\n");
            npc_state.state = NPC_ABORT;
            return false;
          }

          uint32_t inst_lane = pmem_read(commit_pc_lane, 4);
          bool is_store = ((inst_lane & 0x7f) == 0x23);
          bool is_mmio = (is_store || is_load) && is_mmio_addr(mem_addr);
          if (is_mmio) {
            printf("[difftest] unexpected MMIO commit on lane%d pc=0x%08x addr=0x%08x\n",
                   lane, commit_pc_lane, mem_addr);
            npc_state.state = NPC_ABORT;
            return false;
          }
          last_wbu_pc = commit_pc_lane;
          record_commit(commit_pc_lane);
          dt_state.is_mmio = false;
          difftest_one_exec();
          return true;
        };

        bool extra_commits_ok = true;
        if (top->rootp->io_commit_valid1) {
          extra_commits_ok = check_extra_commit(1,
            top->rootp->io_commit_pc1,
            top->rootp->io_commit_mem_addr1,
            top->rootp->io_commit_is_load1);
        }
        if (extra_commits_ok && top->rootp->io_commit_valid2) {
          extra_commits_ok = check_extra_commit(2,
            top->rootp->io_commit_pc2,
            top->rootp->io_commit_mem_addr2,
            top->rootp->io_commit_is_load2);
        }
        if (extra_commits_ok && top->rootp->io_commit_valid3) {
          extra_commits_ok = check_extra_commit(3,
            top->rootp->io_commit_pc3,
            top->rootp->io_commit_mem_addr3,
            top->rootp->io_commit_is_load3);
        }
        if (!extra_commits_ok) {
          break;
        }
        dt_state.check_pending = true;
#endif
      }
    }

    if (npc_state.state == NPC_END) {
      npc_state.halt_pc = read_commit_pc_from_top();
    }

    if (npc_state.state != NPC_RUNNING) {
      break;
    }
    if (++hang_counter > HANG_TIMEOUT_CYCLES) {
      printf("[HANG] No commit for %d cycles, simulation stopped\n", HANG_TIMEOUT_CYCLES);
#if 0
      // Historical deep hierarchy dump.  Stage14 deliberately moves ROB/RS
      // state behind vectorized modules, so diagnostics must use stable
      // top-level probes instead of Verilator's private generated names.
      {
        auto *r = top->rootp;
        unsigned rc = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__count;
        unsigned rh = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__id; // head
        unsigned rt = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__tail;
        unsigned long long fb = (unsigned long long)r->ysyx_25020039__DOT__core__DOT__rename__DOT__freeBits;
        printf("[HANG] last_commit_pc=0x%08x rob_count=%u head=%u tail=%u freeBits=0x%llx free_pop=%d\n",
               (unsigned)stuck_pc, rc, rh, rt, fb, __builtin_popcountll(fb));
        printf("[HANG] lq_head_alloc_pc=0x%08x lq_remove=%u wb_reject=0x%x\n",
               (unsigned)r->io_debug_lq_head_alloc_pc,
               (unsigned)r->io_debug_lq_head_remove_reason,
               (unsigned)r->io_debug_wb_head_reject_flags);
        // print all valid ROB entries
        CData *vv[] = {
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_0_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_1_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_2_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_3_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_4_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_5_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_6_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_7_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_8_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_9_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_10_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_11_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_12_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_13_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_14_valid,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_15_valid,
        };
        CData *dd[] = {
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_0_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_1_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_2_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_3_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_4_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_5_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_6_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_7_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_8_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_9_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_10_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_11_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_12_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_13_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_14_done,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_15_done,
        };
        IData *pp[] = {
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_0_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_1_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_2_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_3_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_4_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_5_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_6_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_7_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_8_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_9_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_10_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_11_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_12_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_13_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_14_pc,
          &r->ysyx_25020039__DOT__core__DOT__rob__DOT__entries_15_pc,
        };
        for (int i = 0; i < 16; i++) {
          if (*vv[i])
            printf("[HANG] rob[%d] done=%u pc=0x%08x%s\n", i, (unsigned)*dd[i],
                   (unsigned)*pp[i], i == (int)rh ? " <-HEAD" : "");
        }
        unsigned alu_v = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_valid;
        unsigned alu_pc = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_pc;
        unsigned alu_rb = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_alu_bits_rob_idx;
        unsigned div_v = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_div_valid;
        unsigned div_pc = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_div_bits_pc;
        unsigned div_rb = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_div_bits_rob_idx;
        unsigned lsu_v = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_valid;
        unsigned lsu_pc = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_pc;
        unsigned lsu_rb = (unsigned)r->ysyx_25020039__DOT__core__DOT__d_lsu_bits_rob_idx;
        printf("[HANG] alu(v=%u pc=0x%08x rob=%u) div(v=%u pc=0x%08x rob=%u) lsu(v=%u pc=0x%08x rob=%u)\n",
               alu_v, alu_pc, alu_rb, div_v, div_pc, div_rb, lsu_v, lsu_pc, lsu_rb);
        CData *lq_valid[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
        };
        CData *lq_state[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
        };
        CData *lq_generation[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_generation,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_generation,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_generation,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_generation,
        };
        CData *lq_rob[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
        };
        IData *lq_pc[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
        };
        IData *lq_addr[] = {
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_addr,
          &r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_addr,
        };
        for (int i = 0; i < 4; i++) {
          if (*lq_valid[i]) {
            printf("[HANG] lq[%d] state=%u gen=%u rob=%u pc=0x%08x addr=0x%08x\n",
                   i, (unsigned)*lq_state[i], (unsigned)*lq_generation[i],
                   (unsigned)*lq_rob[i], (unsigned)*lq_pc[i], (unsigned)*lq_addr[i]);
          }
        }
        unsigned ifu_st = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu__DOT__state;
        unsigned mtvec = (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_1;
        unsigned mepc  = (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_2;
        unsigned mcause= (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_3;
        unsigned ifu_npc = (unsigned)r->ysyx_25020039__DOT__core__DOT___ifu_io_pc_bits_next_pc;
        unsigned ifu_inv = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu_io_in_valid;
        unsigned corr = 0u;
        printf("[HANG] ifu_state=%u mtvec=0x%08x mepc=0x%08x mcause=0x%x next_pc=0x%08x corr=0x%08x in_v=%u\n",
               ifu_st, mtvec, mepc, mcause, ifu_npc, corr, ifu_inv);
        unsigned cmst = (unsigned)r->ysyx_25020039__DOT__core__DOT__cm_st_state;
        unsigned xb = (unsigned)r->ysyx_25020039__DOT__xbar__DOT__state;
        unsigned awv = (unsigned)r->ysyx_25020039__DOT___core_io_dmem_awvalid;
        unsigned wv  = (unsigned)r->ysyx_25020039__DOT___core_io_dmem_wvalid;
        unsigned swst = (unsigned)r->ysyx_25020039__DOT__sram__DOT__w_state;
        unsigned srst = (unsigned)r->ysyx_25020039__DOT__sram__DOT__r_state;
        unsigned dw = (unsigned)r->ysyx_25020039__DOT__xbar__DOT__dmem_is_write;
        unsigned icst = (unsigned)r->ysyx_25020039__DOT__core__DOT__icache__DOT__state;
        unsigned ftqh = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu__DOT__ftq__DOT__head;
        unsigned ftqt = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu__DOT__ftq__DOT__tail;
        unsigned ftqc = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu__DOT__ftq__DOT__count;
        printf("[HANG] cm_st_state=%u xbar_state=%u dmem_wr=%u sram_w=%u\n",
               cmst, xb, dw, swst);
        printf("[HANG] icache_state=%u sram_r=%u ftq(head=%u tail=%u count=%u)\n",
               icst, srst, ftqh, ftqt, ftqc);
        printf("[HANG] core_awvalid=%u core_wvalid=%u\n", awv, wv);
        CData *rsv[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_valid,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_valid,
        };
        CData *rs1[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_src1_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_src1_ready,
        };
        CData *rs2[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_src2_ready,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_src2_ready,
        };
        CData *rsi[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_issued,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_issued,
        };
        CData *rsr[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_rob_idx,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_rob_idx,
        };
        IData *rsp[] = {
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_0_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_1_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_2_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_3_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_4_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_5_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_6_pc,
          &r->ysyx_25020039__DOT__core__DOT__rs__DOT__entries_7_pc,
        };
        for (int i = 0; i < 8; i++) {
          if (*rsv[i])
            printf("[HANG] rs[%d] rob=%u issued=%u rdy=%u/%u pc=0x%08x\n", i,
                   (unsigned)*rsr[i], (unsigned)*rsi[i], (unsigned)*rs1[i],
                   (unsigned)*rs2[i], (unsigned)*rsp[i]);
        }
      }
#endif
      {
        auto *r = top->rootp;
        printf("[HANG] lq0 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_0_bypassedStores);
        printf("[HANG] lq1 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_1_bypassedStores);
        printf("[HANG] lq2 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_2_bypassedStores);
        printf("[HANG] lq3 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_3_bypassedStores);
        printf("[HANG] lq4 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_4_bypassedStores);
        printf("[HANG] lq5 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_5_bypassedStores);
        printf("[HANG] lq6 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_6_bypassedStores);
        printf("[HANG] lq7 v=%u st=%u rob=%u pc=0x%08x addr=0x%08x mask=0x%08x\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_state,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_meta_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_addr,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__lq__DOT__entries_7_bypassedStores);
      }
      printf("[HANG] last_commit_pc=0x%08x lq_head_alloc_pc=0x%08x lq_remove=%u wb_reject=0x%x\n",
             (unsigned)stuck_pc,
             (unsigned)top->rootp->io_debug_lq_head_alloc_pc,
             (unsigned)top->rootp->io_debug_lq_head_remove_reason,
             (unsigned)top->rootp->io_debug_wb_head_reject_flags);
      printf("[HANG] rob_head=0x%08x valid=%u done=%u mem=%u ctrl=%u rob=%u rs=%u brq=%u fq=%u brq_issue=%u bru_d=%u unresolved=0x%08x older_unresolved=0x%08x\n",
             (unsigned)top->rootp->io_debug_rob_head_pc,
             (unsigned)top->rootp->io_debug_rob_head_valid,
             (unsigned)top->rootp->io_debug_rob_head_done,
             (unsigned)top->rootp->io_debug_rob_head_mem,
             (unsigned)top->rootp->io_debug_rob_head_ctrl,
             (unsigned)top->rootp->io_debug_rob_count,
             (unsigned)top->rootp->io_debug_rs_count,
             (unsigned)top->rootp->io_debug_brq_count,
             (unsigned)top->rootp->io_debug_fq_count,
             (unsigned)top->rootp->io_debug_brq_issue_valid,
             (unsigned)top->rootp->io_debug_bru_dispatch_valid,
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT___sq_io_unresolved_mask,
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT___sq_io_older_unresolved_mask);
      CData *rob_done[] = {
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_0_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_1_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_2_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_3_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_4_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_5_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_6_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_7_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_8_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_9_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_11_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_12_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_13_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_14_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_15_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_16_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_17_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_18_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_19_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_20_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_21_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_22_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_23_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_24_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_25_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_26_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_27_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_28_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_29_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_30_done,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_31_done,
      };
      CData *rob_valid[] = {
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_0_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_1_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_2_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_3_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_4_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_5_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_6_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_7_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_8_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_9_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_11_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_12_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_13_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_14_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_15_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_16_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_17_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_18_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_19_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_20_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_21_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_22_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_23_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_24_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_25_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_26_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_27_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_28_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_29_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_30_valid,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_31_valid,
      };
      IData *rob_pc[] = {
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_0_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_1_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_2_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_3_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_4_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_5_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_6_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_7_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_8_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_9_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_11_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_12_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_13_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_14_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_15_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_16_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_17_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_18_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_19_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_20_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_21_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_22_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_23_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_24_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_25_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_26_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_27_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_28_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_29_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_30_pc,
        &top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_31_pc,
      };
      printf("[HANG] ROB:");
      for (int i = 0; i < 32; i++) {
        if (*rob_valid[i]) printf(" %d:%c:0x%08x", i, *rob_done[i] ? 'D' : 'W', (unsigned)*rob_pc[i]);
      }
      printf("\n");
      printf("[HANG] rob10 mem_valid=%u mem_write=%u addr_ready=%u done=%u\n",
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_mem_valid,
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_mem_write,
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_addr_ready,
             (unsigned)top->rootp->ysyx_25020039__DOT__core__DOT__rob__DOT__impl__DOT__entries_10_done);
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
