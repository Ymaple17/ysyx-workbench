#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "VysyxSoCFull.h"
#include "VysyxSoCFull___024root.h"
#include "verilated_dpi.h"
#include "verilated_fst_c.h"
#include "svdpi.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/common.h"
#include "../include/trace.h"
#include "../include/regs.h"
#include "../include/memory.h"
#include "../include/perf.h"
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
  const word_t halt_pc = read_pc_from_top();
  const word_t halt_ret = read_gpr_from_top(10);
  fprintf(stderr, "[OOOD-SOC] sim_exit pc=0x%08x a0=0x%08x\n",
          halt_pc, halt_ret);
  print_perf_stats(main_time / 2);
  fflush(stdout);
  fflush(stderr);
  set_npc_state(NPC_END, halt_pc, halt_ret);
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

struct CommitSnapshot {
  uint32_t mask;
  uint32_t pc[4];
  uint32_t mem_addr[4];
  bool is_load[4];
#ifdef DIFFTEST
  uint32_t gpr_before[32];
#endif
  bool reset;
};

static CommitSnapshot commit_snapshot = {};

#define RESET_CYCLES 20

void single_cycle();
static void trace_soc_commit_detail(uint64_t cycle, int lane, uint32_t pc, uint32_t mem_addr);
static void trace_soc_bus(uint64_t cycle);
static void trace_soc_writes(uint64_t cycle);
static void trace_frontend(uint64_t cycle);
static bool soc_bus_active = false;

void init_npc_cpu(){
  top->clock = 0;
  top->reset = 1;
  top->eval();

  for (int i = 0; i < RESET_CYCLES; i++) {
    single_cycle();
  }

  const char *preload_elf = getenv("OOOD_SOC_PRELOAD_ELF");
  if (preload_elf != nullptr && preload_elf[0] != '\0') {
    if (!preload_ysyxsoc_elf(preload_elf)) {
      fprintf(stderr, "Failed to preload ysyxSoC ELF: %s\n", preload_elf);
      abort();
    }
  }

  top->reset = 0;
  rst_done = true;

  update_cpu_state();
}

void single_cycle() {
  trace_soc_writes(main_time / 2);
  top->clock = 1;
  top->eval();
  #ifdef ENABLE_WAVEFORM
    if (tfp != nullptr) tfp->dump(main_time);
  #endif
  main_time++;

  top->clock = 0;
  top->eval();
  trace_soc_bus(main_time / 2);
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
  // Commit is a combinational fire signal for the edge that follows. Capture
  // it before the rising edge; after the edge the ROB has already advanced.
  commit_snapshot.mask = read_commit_mask_from_top();
  commit_snapshot.reset = top->reset;
  for (int lane = 0; lane < 4; lane++) {
    commit_snapshot.pc[lane] = read_commit_pc_from_top(lane);
    commit_snapshot.mem_addr[lane] = read_commit_mem_addr_from_top(lane);
    commit_snapshot.is_load[lane] = read_commit_is_load_from_top(lane);
    if (commit_snapshot.mask & (1u << lane)) {
      trace_soc_commit_detail(main_time / 2, lane, commit_snapshot.pc[lane],
                              commit_snapshot.mem_addr[lane]);
    }
  }
#ifdef DIFFTEST
  for (int i = 0; i < 32; i++) {
    commit_snapshot.gpr_before[i] = read_gpr_from_top(i);
  }
#endif
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

  // Commit updates the architectural RAT and PRF on the rising edge. Run one
  // low-phase evaluation before sampling the architectural read ports so a
  // same-cycle writeback/retire is visible to the harness.
  top->clock = 0;
  top->eval();

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

static bool difftest_step_group(const word_t *commit_pcs,
                                const word_t *commit_mem_addr,
                                const bool *commit_is_load,
                                int commit_count,
                                const uint32_t *dut_gpr_before) {
  static bool ref_sync_pending = false;

  // MMIO is implemented by ysyxSoC peripherals, while NEMU only models the
  // architectural CPU. Sync at the next commit using the DUT state captured
  // before that commit group, matching the NPC harness semantics.
  if (ref_sync_pending) {
    CPU_State sync = {};
    sync.pc = commit_pcs[0];
    for (int i = 0; i < NR_GPRs; i++) {
      sync.gpr[i] = dut_gpr_before[i];
    }
    ref_difftest_regcpy(&sync, DIFFTEST_TO_REF);
    ref_sync_pending = false;
  }

  for (int lane = 0; lane < commit_count; lane++) {
    CPU_State ref_before;
    ref_difftest_regcpy(&ref_before, DIFFTEST_TO_DUT);
    if (ref_before.pc != commit_pcs[lane]) {
      fprintf(stderr,
              "[difftest] PC mismatch before commit lane=%d: "
              "REF=0x%08x DUT=0x%08x\n",
              lane, ref_before.pc, commit_pcs[lane]);
      return false;
    }

    const uint32_t inst = pmem_read(commit_pcs[lane], 4);
    const bool is_store = (inst & 0x7f) == 0x23;
    const bool is_mmio = (is_store || commit_is_load[lane]) &&
        is_mmio_addr(commit_mem_addr[lane]);
    if (is_mmio) {
      ref_sync_pending = true;
      // cm_exclusive prevents a younger commit from sharing an MMIO group.
      // Keep the guard explicit so a future core change cannot silently let
      // the reference execute past an unmodelled peripheral access.
      if (lane + 1 != commit_count) {
        fprintf(stderr,
                "[difftest] MMIO commit was not the last lane: lane=%d pc=0x%08x\n",
                lane, commit_pcs[lane]);
      }
      break;
    }
    ref_difftest_exec(1);
  }

  if (ref_sync_pending) return true;

  CPU_State ref_after;
  ref_difftest_regcpy(&ref_after, DIFFTEST_TO_DUT);
  for (int i = 0; i < NR_GPRs; i++) {
    if (cpu.gpr[i] != ref_after.gpr[i]) {
      fprintf(stderr,
              "[difftest] GPR mismatch after %d commit(s), PC=0x%08x "
              "%s REF=0x%08x DUT=0x%08x\n",
              commit_count, commit_pcs[commit_count - 1], regs[i],
              ref_after.gpr[i], cpu.gpr[i]);
      return false;
    }
  }
  return true;
}
#endif

#define HANG_TIMEOUT_CYCLES 10000
#define PC_STUCK_COMMITS 20000

static uint64_t read_env_u64(const char *name) {
  const char *value = getenv(name);
  return value == nullptr ? 0 : strtoull(value, nullptr, 0);
}

static void trace_soc_commit_detail(uint64_t cycle, int lane, uint32_t pc, uint32_t mem_addr) {
  const bool detail_enabled = getenv("OOOD_SOC_TRACE_DETAIL") != nullptr;
  const bool bus_enabled = getenv("OOOD_SOC_TRACE_BUS") != nullptr;
  if (!detail_enabled && !bus_enabled) return;
  const bool copy_loop = pc == 0x0f0000a8U || pc == 0x0f0000acU ||
                         pc == 0x0f0000b4U || pc == 0x0f0000bcU ||
                         pc == 0x0f0000c0U;
  if (!copy_loop && pc != 0xa00001ccU) return;
  if (copy_loop) soc_bus_active = true;
  if (!detail_enabled) return;

  static uint64_t detail_hits = 0;
  const bool target_commit = pc == 0xa00001ccU;
  if (!target_commit && detail_hits++ >= 64 && (detail_hits & 0x3ff) != 0) return;

  const uint32_t inst = pmem_read(pc, 4);
  fprintf(stderr,
          "[OOOD-SOC-DETAIL] cycle=%llu lane=%d pc=0x%08x inst=0x%08x "
          "ra=0x%08x t0=0x%08x t1=0x%08x t2=0x%08x "
          "a0=0x%08x a1=0x%08x a2=0x%08x mem=0x%08x\n",
          (unsigned long long)cycle, lane, pc, inst,
          read_gpr_from_top(1), read_gpr_from_top(5), read_gpr_from_top(6),
          read_gpr_from_top(7), read_gpr_from_top(10), read_gpr_from_top(11),
          read_gpr_from_top(12), mem_addr);
  if (pc != 0xa00001ccU) return;

  const auto *root = top->rootp;
#define CORE(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__##name
  fprintf(stderr,
          "[OOOD-SOC-FETCH] ar=%u addr=0x%08x rvalid=%u rready=%u "
          "ifu_data=0x%08x ic_in_data=0x%08x ic_raw=0x%08x "
          "master_r=%u master_rlast=%u master_data=0x%08x "
          "cache_addr=0x%08x cache_state=%u count=%u\n",
          CORE(_ifu_io_imem_arvalid), CORE(_ifu_io_imem_araddr),
          CORE(ifu__DOT__io_imem_rvalid), CORE(_ifu_io_imem_rready),
          CORE(ifu__DOT__io_imem_rdata), CORE(_icache_io_in_rdata),
          CORE(icache__DOT__rdata),
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rvalid,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rlast,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rdata,
          CORE(icache__DOT__in_addr), CORE(icache__DOT__state),
          CORE(icache__DOT__count));
  fprintf(stderr,
          "[OOOD-SOC-ICLINE] w0 v=%u tag=%05x data=%08x,%08x,%08x,%08x,%08x,%08x,%08x,%08x "
          "w1 v=%u tag=%05x data=%08x,%08x,%08x,%08x,%08x,%08x,%08x,%08x\n",
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_valid,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_tag,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_0,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_1,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_2,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_3,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_4,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_5,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_6,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_0_data_7,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_valid,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_tag,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_0,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_1,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_2,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_3,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_4,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_5,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_6,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_1_data_7);
  fprintf(stderr,
          "[OOOD-SOC-ICLINE] w2 v=%u tag=%05x data=%08x,%08x,%08x,%08x,%08x,%08x,%08x,%08x "
          "w3 v=%u tag=%05x data=%08x,%08x,%08x,%08x,%08x,%08x,%08x,%08x\n",
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_valid,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_tag,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_0,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_1,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_2,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_3,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_4,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_5,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_6,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_2_data_7,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_valid,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_tag,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_0,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_1,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_2,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_3,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_4,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_5,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_6,
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__icache_14_set_3_data_7);
  fprintf(stderr,
          "[OOOD-SOC-DETAIL] cycle=%llu lane=%d pc=0x%08x "
          "rob_head=%u cm_valid=%u cm_done=%u reg_write=%u "
          "arch_rd=%u old=%u new=%u jump=%u "
          "cm_fire=%u do_ren=%u cm_new=%u cm_arch=%u "
          "rat1=%u archRat1=%u arch_out1=%u "
          "prf_arch_addr1=%u prf_arch_data1=0x%08x "
          "rf_40=0x%08x rf_1=0x%08x "
          "idu_waddr=%u idu_reg_write=%u idu_sel=%u idu_jump=%u "
          "cm_inst=0x%08x cm_pc=0x%08x "
          "entry23_pc=0x%08x entry23_inst=0x%08x entry23_rd=%u entry23_reg_write=%u entry23_new=%u "
          "can_wb=%u wb_wen=%u wb_pdest=%u wb_val=0x%08x "
          "wb_pc=0x%08x wb_next=0x%08x wb_reg_write=%u wb_sel=%u "
          "prf_wen1=%u prf_waddr1=%u prf_wdata1=0x%08x\n",
          (unsigned long long)cycle, lane, pc,
          CORE(rob__DOT__impl__DOT__io_head),
          CORE(_rob_io_commit_bits_valid),
          CORE(_rob_io_commit_bits_done),
          CORE(_rob_io_commit_bits_reg_write),
          CORE(_rob_io_commit_bits_arch_rd),
          CORE(_rob_io_commit_bits_old_phys),
          CORE(_rob_io_commit_bits_new_phys),
          CORE(_rob_io_commit_bits_jump),
          CORE(rename__DOT__impl__DOT__io_commit_fire_0),
          CORE(rename__DOT__impl__DOT__io_commit_do_rename_0),
          CORE(rename__DOT__impl__DOT__io_commit_new_phys_0),
          CORE(rename__DOT__impl__DOT__io_commit_arch_rd_0),
          CORE(rename__DOT__impl__DOT__rat_1),
          CORE(rename__DOT__impl__DOT__archRat_1),
          CORE(rename__DOT__impl__DOT__io_arch_rat_out_1),
          CORE(prf__DOT__impl__DOT__io_arch_raddr_1),
          CORE(prf__DOT__impl__DOT__io_arch_rdata_1),
          CORE(prf__DOT__impl__DOT__rf_40),
          CORE(prf__DOT__impl__DOT__rf_1),
          CORE(_idu_io_out_bits_waddr),
          CORE(_idu_io_out_bits_signals_wbu_reg_write),
          CORE(_idu_io_out_bits_signals_wbu_reg_write_sel),
          CORE(_idu_io_out_bits_signals_exu_jump),
          CORE(_rob_io_commit_bits_inst),
          CORE(_rob_io_commit_bits_pc),
          CORE(rob__DOT__impl__DOT__entries_23_pc),
          CORE(rob__DOT__impl__DOT__entries_23_inst),
          CORE(rob__DOT__impl__DOT__entries_23_arch_rd),
          CORE(rob__DOT__impl__DOT__entries_23_reg_write),
          CORE(rob__DOT__impl__DOT__entries_23_new_phys),
          CORE(can_wb),
          CORE(wb_wen),
          CORE(_wbArb_io_out_0_bits_pdest),
          CORE(_wbArb_io_out_0_bits_alu_result),
          CORE(_wbArb_io_out_0_bits_pc),
          CORE(_wbArb_io_out_0_bits_next_pc),
          CORE(_wbArb_io_out_0_bits_signals_wbu_reg_write),
          CORE(_wbArb_io_out_0_bits_signals_wbu_reg_write_sel),
          CORE(prf__DOT__io_wen1),
          CORE(prf__DOT__io_waddr1),
          CORE(prf__DOT__io_wdata1));
#undef CORE
}

static void trace_soc_bus(uint64_t cycle) {
  if (getenv("OOOD_SOC_TRACE_BUS") == nullptr) return;
  static bool active = false;
  static uint64_t next_dump = 0;
  if (soc_bus_active) active = true;
  if (!active || cycle < next_dump) return;
  next_dump = cycle + 1000;

  const auto *root = top->rootp;
#define CORE(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__##name
#define DEEP(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__##name
#define XBAR(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__xbar__DOT__##name
  fprintf(stderr,
          "[OOOD-SOC-BUS] cycle=%llu pc=0x%08x "
          "lsu_ar=%u/%u lsu_id=%u dcache_arready=%u "
          "dcache_r=%u/%u "
          "mq_active=%u mq_v=%u/%u/%u/%u mq_s=%u/%u/%u/%u "
          "mq_ar=%u addr=0x%08x ready=%u r=%u "
          "xbar_s=%u soc_ar=%u addr=0x%08x ready=%u soc_r=%u/%u\n",
          (unsigned long long)cycle, read_pc_from_top(),
          CORE(_lsu_io_dmem_arvalid), CORE(_lsu_io_dmem1_arvalid),
          CORE(_lsu_io_dmem_arid), CORE(_dcache_io_cpu_arready),
          CORE(_dcache_io_cpu_rvalid), CORE(_dcache_io_cpu_rid),
          DEEP(dcache__DOT__missQueue__DOT__activeValid),
          DEEP(dcache__DOT__missQueue__DOT__valid_0),
          DEEP(dcache__DOT__missQueue__DOT__valid_1),
          DEEP(dcache__DOT__missQueue__DOT__valid_2),
          DEEP(dcache__DOT__missQueue__DOT__valid_3),
          DEEP(dcache__DOT__missQueue__DOT__state_0),
          DEEP(dcache__DOT__missQueue__DOT__state_1),
          DEEP(dcache__DOT__missQueue__DOT__state_2),
          DEEP(dcache__DOT__missQueue__DOT__state_3),
          DEEP(dcache__DOT__missQueue__DOT__io_mem_arvalid),
          DEEP(dcache__DOT__missQueue__DOT__io_mem_araddr),
          DEEP(dcache__DOT__missQueue__DOT__io_mem_arready),
          DEEP(dcache__DOT__missQueue__DOT__io_mem_rvalid),
          XBAR(rState), XBAR(io_soc_arvalid), XBAR(io_soc_araddr),
          XBAR(io_soc_arready), XBAR(io_soc_rvalid), XBAR(io_soc_rready));
#undef XBAR
#undef DEEP
#undef CORE
}

static void trace_soc_writes(uint64_t cycle) {
  if (getenv("OOOD_SOC_TRACE_WRITES") == nullptr) return;

  const auto *root = top->rootp;
#define CPU(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__##name
  const bool aw_fire = CPU(io_master_awvalid) && CPU(io_master_awready);
  const bool w_fire = CPU(io_master_wvalid) && CPU(io_master_wready);
  const bool b_fire = CPU(io_master_bvalid) && CPU(io_master_bready);
  const uint32_t awaddr = CPU(io_master_awaddr);
  static uint32_t active_addr = 0;
  static uint32_t active_beat = 0;
  static uint32_t last_awvalid = 0;
  static uint32_t last_wvalid = 0;
  static uint32_t last_bvalid = 0;
  static uint32_t last_awaddr = 0;
  static uint32_t last_wdata = 0;
  static uint32_t last_wstrb = 0;
  const bool state_changed =
      last_awvalid != CPU(io_master_awvalid) ||
      last_wvalid != CPU(io_master_wvalid) ||
      last_bvalid != CPU(io_master_bvalid) ||
      last_awaddr != awaddr || last_wdata != CPU(io_master_wdata) ||
      last_wstrb != CPU(io_master_wstrb);

  if (state_changed &&
      (CPU(io_master_awvalid) || CPU(io_master_wvalid) || CPU(io_master_bvalid) ||
       last_awvalid || last_wvalid || last_bvalid)) {
    fprintf(stderr,
            "[OOOD-SOC-WRITE-STATE] cycle=%llu "
            "AW=%u/%u addr=0x%08x W=%u/%u data=0x%08x strb=0x%x last=%u "
            "B=%u/%u\n",
            (unsigned long long)cycle,
            (unsigned)CPU(io_master_awvalid), (unsigned)CPU(io_master_awready),
            awaddr, (unsigned)CPU(io_master_wvalid),
            (unsigned)CPU(io_master_wready), CPU(io_master_wdata),
            (unsigned)CPU(io_master_wstrb), (unsigned)CPU(io_master_wlast),
            (unsigned)CPU(io_master_bvalid), (unsigned)CPU(io_master_bready));
  }
  last_awvalid = CPU(io_master_awvalid);
  last_wvalid = CPU(io_master_wvalid);
  last_bvalid = CPU(io_master_bvalid);
  last_awaddr = awaddr;
  last_wdata = CPU(io_master_wdata);
  last_wstrb = CPU(io_master_wstrb);

  if (aw_fire) {
    active_addr = awaddr;
    active_beat = 0;
    fprintf(stderr,
            "[OOOD-SOC-WRITE] cycle=%llu AW addr=0x%08x len=%u size=%u\n",
            (unsigned long long)cycle, awaddr,
            (unsigned)CPU(io_master_awlen), (unsigned)CPU(io_master_awsize));
  }
  if (w_fire) {
    fprintf(stderr,
            "[OOOD-SOC-WRITE] cycle=%llu W addr=0x%08x beat=%u data=0x%08x "
            "strb=0x%x last=%u\n",
            (unsigned long long)cycle, active_addr + active_beat * 4U,
            active_beat, CPU(io_master_wdata), (unsigned)CPU(io_master_wstrb),
            (unsigned)CPU(io_master_wlast));
    active_beat++;
  }
  if (b_fire) {
    fprintf(stderr, "[OOOD-SOC-WRITE] cycle=%llu B resp=%u\n",
            (unsigned long long)cycle, (unsigned)CPU(io_master_bresp));
  }
#undef CPU
}

extern "C" void npc_sim_putc(char ch) {
  fputc(ch, stderr);
  fflush(stderr);
}

#if 0
static void trace_target_rob(uint64_t cycle) {
  static const bool enabled = read_env_u64("OOOD_SOC_TRACE_ROB") != 0;
  static bool memory_dumped = false;
  static bool old_match[32] = {};
  static uint8_t old_done[32] = {};
  if (!enabled) return;

  if (!memory_dumped) {
    for (uint32_t addr = 0xa0005670U; addr <= 0xa0005688U; addr += 4) {
      fprintf(stderr, "[SDRAM] addr=0x%08x data=0x%08x\n", addr,
              pmem_read(addr, 4));
    }
    memory_dumped = true;
  }

  const auto *root = top->rootp;
#define ROB_FIELD(slot, field) \
  root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_##field
#define TRACE_ROB_SLOT(slot) do { \
    const uint32_t pc = ROB_FIELD(slot, pc); \
    const bool match = ROB_FIELD(slot, valid) && pc >= 0xa0005670U && pc <= 0xa0005688U; \
    const uint8_t done = ROB_FIELD(slot, done); \
    if (match != old_match[slot] || (match && done != old_done[slot])) { \
      fprintf(stderr, \
              "[ROB] cycle=%llu slot=%d valid=%u done=%u jump=%u pc=0x%08x inst=0x%08x\n", \
              (unsigned long long)cycle, slot, ROB_FIELD(slot, valid), done, \
              ROB_FIELD(slot, jump), pc, ROB_FIELD(slot, inst)); \
    } \
    old_match[slot] = match; \
    old_done[slot] = done; \
  } while (0)

  TRACE_ROB_SLOT(0);  TRACE_ROB_SLOT(1);  TRACE_ROB_SLOT(2);  TRACE_ROB_SLOT(3);
  TRACE_ROB_SLOT(4);  TRACE_ROB_SLOT(5);  TRACE_ROB_SLOT(6);  TRACE_ROB_SLOT(7);
  TRACE_ROB_SLOT(8);  TRACE_ROB_SLOT(9);  TRACE_ROB_SLOT(10); TRACE_ROB_SLOT(11);
  TRACE_ROB_SLOT(12); TRACE_ROB_SLOT(13); TRACE_ROB_SLOT(14); TRACE_ROB_SLOT(15);
  TRACE_ROB_SLOT(16); TRACE_ROB_SLOT(17); TRACE_ROB_SLOT(18); TRACE_ROB_SLOT(19);
  TRACE_ROB_SLOT(20); TRACE_ROB_SLOT(21); TRACE_ROB_SLOT(22); TRACE_ROB_SLOT(23);
  TRACE_ROB_SLOT(24); TRACE_ROB_SLOT(25); TRACE_ROB_SLOT(26); TRACE_ROB_SLOT(27);
  TRACE_ROB_SLOT(28); TRACE_ROB_SLOT(29); TRACE_ROB_SLOT(30); TRACE_ROB_SLOT(31);
#undef TRACE_ROB_SLOT
#undef ROB_FIELD
}

#endif

static void trace_frontend(uint64_t cycle) {
  static const bool enabled = read_env_u64("OOOD_SOC_TRACE_FRONTEND") != 0;
  static uint32_t old_fetch_pc = 0;
  static uint32_t old_cache_addr = 0;
  static uint8_t old_ifu_state = 0xff;
  static uint8_t old_cache_state = 0xff;
  if (!enabled) return;

  const auto *root = top->rootp;
  const uint32_t fetch_pc = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc;
  const uint32_t cache_addr = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__in_addr;
  const uint8_t ifu_state = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__state;
  const uint8_t cache_state = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__state;
  const bool in_range = fetch_pc >= 0xa0000190U && fetch_pc <= 0xa00001e0U;
  const bool ic_arvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_arvalid;
  const bool ic_arready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_arready;
  const bool ic_rvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_rvalid;
  const bool ic_rready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_rready;
  const uint32_t ic_araddr = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_araddr;
  const uint32_t ic_rdata = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__io_out_rdata;
  const bool master_arvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_arvalid;
  const bool master_arready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_arready;
  const bool master_rvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rvalid;
  const bool master_rready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rready;
  const uint32_t master_araddr = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_araddr;
  const uint32_t master_rdata = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rdata;
  const bool bus_arvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_arvalid;
  const bool bus_arready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_arready;
  const bool bus_rvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_rvalid;
  const bool bus_rready = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_rready;
  const uint32_t bus_araddr = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_araddr;
  const uint32_t bus_rdata = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__auto_master_out_rdata;
  const bool bus_range = bus_araddr >= 0xa0000000U && bus_araddr <= 0xa0001000U;
  const bool work = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT__work;
  const bool rvalid = root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid;
  if ((in_range || (ic_araddr >= 0xa0000000U && ic_araddr <= 0xa0001000U) ||
       bus_range) &&
      (work || rvalid || ic_arvalid || ic_rvalid || master_arvalid || master_rvalid ||
       bus_arvalid || bus_rvalid ||
       fetch_pc != old_fetch_pc ||
                   cache_addr != old_cache_addr || ifu_state != old_ifu_state ||
                   cache_state != old_cache_state)) {
    fprintf(stderr,
            "[FE] cycle=%llu pc=%08x next=%08x valid=%u flush=%u ifu=%u work=%u "
            "cache_addr=%08x cache=%u count=%u rv=%u%u%u%u "
            "ic_ar=%u/%u@%08x ic_r=%u/%u:%08x "
            "master_ar=%u/%u@%08x master_r=%u/%u:%08x "
            "bus_ar=%u/%u@%08x bus_r=%u/%u:%08x "
            "data=%08x,%08x,%08x,%08x held=%08x,%08x,%08x,%08x\n",
            (unsigned long long)cycle, fetch_pc,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_bits_next_pc,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_is_flush,
            ifu_state, work, cache_addr, cache_state,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__count,
             rvalid,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid1,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid2,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid3,
             ic_arvalid, ic_arready, ic_araddr, ic_rvalid, ic_rready, ic_rdata,
             master_arvalid, master_arready, master_araddr,
             master_rvalid, master_rready, master_rdata,
             bus_arvalid, bus_arready, bus_araddr, bus_rvalid, bus_rready, bus_rdata,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___io_in_rdata_T_4,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___io_in_rdata1_T_5,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___io_in_rdata2_T_5,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT___io_in_rdata3_T_5,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__rdata,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__rdata1,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__rdata2,
             root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__icache__DOT__rdata3);
  }
  old_fetch_pc = fetch_pc;
  old_cache_addr = cache_addr;
  old_ifu_state = ifu_state;
  old_cache_state = cache_state;
}

static void trace_soc_control(uint64_t cycle) {
  static const bool enabled = read_env_u64("OOOD_SOC_TRACE_CONTROL") != 0;
  static const bool trace_ioe = read_env_u64("OOOD_SOC_TRACE_IOE") != 0;
  static uint32_t old_fetch_pc = UINT32_MAX;
  static uint32_t old_inst[4] = {UINT32_MAX, UINT32_MAX, UINT32_MAX, UINT32_MAX};
  static uint8_t old_valid_mask = UINT8_MAX;
  if (!enabled) return;

  const auto *root = top->rootp;
  const uint32_t fetch_pc =
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_bits_r_next_pc;
  const uint32_t ifu_next_pc =
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___ifu_io_pc_bits_next_pc;
  const uint32_t inst[4] = {
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rdata,
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rdata1,
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rdata2,
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rdata3};
  const uint8_t valid_mask =
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid |
      (root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid1 << 1) |
      (root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid2 << 2) |
      (root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___icache_io_in_rvalid3 << 3);
  const bool fetch_range = (fetch_pc >= 0xa00055c0U && fetch_pc <= 0xa00055dcU) ||
      (trace_ioe && fetch_pc >= 0xa0005660U && fetch_pc <= 0xa00056c0U);
  const bool fetch_changed = fetch_pc != old_fetch_pc || valid_mask != old_valid_mask ||
      inst[0] != old_inst[0] || inst[1] != old_inst[1] ||
      inst[2] != old_inst[2] || inst[3] != old_inst[3];
  if (fetch_range && fetch_changed) {
    fprintf(stderr,
            "[OOOD-SOC-FE] cycle=%llu pc=0x%08x next=0x%08x "
            "rv=%x inst=%08x/%08x/%08x/%08x bp=%u/%u target=%08x "
            "valid=%u flush=%u\n",
            (unsigned long long)cycle, fetch_pc, ifu_next_pc, valid_mask,
            inst[0], inst[1], inst[2], inst[3],
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT___bpu_io_bp_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT___bpu_io_bp_taken,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu__DOT___bpu_io_bp_target,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_in_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__ifu_io_is_flush);
  }
  old_fetch_pc = fetch_pc;
  old_valid_mask = valid_mask;
  for (int lane = 0; lane < 4; lane++) old_inst[lane] = inst[lane];

  const uint32_t bru_pc =
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_pc;
  const bool bru_range = (bru_pc >= 0xa00055c0U && bru_pc <= 0xa00055dcU) ||
      (trace_ioe && bru_pc >= 0xa0005660U && bru_pc <= 0xa00056c0U);
  if (bru_range &&
      root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_valid) {
    fprintf(stderr,
            "[OOOD-SOC-BRU] cycle=%llu pc=0x%08x inst=0x%08x jump=%u pc_src=%u "
            "pred=%u bp_valid=%u bp_target=0x%08x correct=0x%08x "
            "out_valid=%u issue=%u take=%u load=%u leave=%u flush=%u mis=%u "
            "dvalid=%u stop=%u now=%u bpwin=%u memwin=%u retwin=%u cm=%u\n",
            (unsigned long long)cycle, bru_pc,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_inst,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_signals_exu_jump,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_bru_io_pc_bits_pc_src,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_bp_taken,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_bp_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_bits_bp_target,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__correct_pc,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___exu_bru_io_out_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___brq_io_issue_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__take_bru_issue,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__can_load_bru,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__bru_leave,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__flush_bru,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__mis_predict_dbg,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__d_bru_valid,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__stop_issue,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__flush_now,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__branchRecoveryWins,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__memoryRecoveryWins,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__retirePathRecoveryWins,
            root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT___rob_io_commit_valid);
  }
}

static void execute(uint64_t n) {
  uint64_t hang_counter = 0;
  static word_t stuck_pc = 0;
  static int stuck_cnt = 0;
  static bool stuck_initialized = false;
  static uint64_t total_cycles = 0;
  static uint64_t total_commits = 0;
  static const uint64_t trace_commits = read_env_u64("OOOD_SOC_TRACE_COMMITS");
  static const uint64_t progress_interval =
      read_env_u64("OOOD_SOC_PROGRESS_INTERVAL");
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();
      total_cycles++;
      trace_soc_control(total_cycles);
      trace_frontend(total_cycles);
    #ifdef CONFIG_DEVICE
      device_update();
    #endif

      update_cpu_state();

      uint32_t commit_mask = commit_snapshot.mask;
      bool commit_valid = commit_mask != 0;
      bool reset = commit_snapshot.reset;

      if (commit_valid && !reset && rst_done) {
        hang_counter = 0;
        word_t commit_pc = 0;
        for (int lane = 3; lane >= 0; lane--) {
          if (commit_mask & (1u << lane)) {
            commit_pc = commit_snapshot.pc[lane];
            break;
          }
        }
#ifdef DIFFTEST
        word_t diff_commit_pcs[4] = {};
        word_t diff_commit_mem_addr[4] = {};
        bool diff_commit_is_load[4] = {};
        int diff_commit_count = 0;
#endif

        if (trace_commits >= 47 && total_commits >= 47 && total_commits <= 60) {
          const auto *root = top->rootp;
          fprintf(stderr, "[OOOD-SOC-ROB] idx=%u count=%u ",
                  root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__idx,
                  root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__count);
#define TRACE_ROB_SLOT(slot) fprintf(stderr, "s%d=%u/%u/%u/%08x/%x ", slot, \
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_valid, \
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_done, \
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_jump, \
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_pc, \
          root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__rob__DOT__impl__DOT__entries_##slot##_inst)
          TRACE_ROB_SLOT(0); TRACE_ROB_SLOT(1); TRACE_ROB_SLOT(2); TRACE_ROB_SLOT(3);
          TRACE_ROB_SLOT(4); TRACE_ROB_SLOT(5); TRACE_ROB_SLOT(6); TRACE_ROB_SLOT(7);
          TRACE_ROB_SLOT(8); TRACE_ROB_SLOT(9); TRACE_ROB_SLOT(10); TRACE_ROB_SLOT(11);
          TRACE_ROB_SLOT(12); TRACE_ROB_SLOT(13); TRACE_ROB_SLOT(14); TRACE_ROB_SLOT(15);
          TRACE_ROB_SLOT(16); TRACE_ROB_SLOT(17); TRACE_ROB_SLOT(18); TRACE_ROB_SLOT(19);
          TRACE_ROB_SLOT(20); TRACE_ROB_SLOT(21); TRACE_ROB_SLOT(22); TRACE_ROB_SLOT(23);
#undef TRACE_ROB_SLOT
          fputc('\n', stderr);
        }

        for (int lane = 0; lane < 4; lane++) {
          if ((commit_mask & (1u << lane)) == 0) continue;
          total_commits++;
          const word_t lane_pc = commit_snapshot.pc[lane];
#ifdef DIFFTEST
          diff_commit_pcs[diff_commit_count++] = lane_pc;
          diff_commit_mem_addr[diff_commit_count - 1] =
              commit_snapshot.mem_addr[lane];
          diff_commit_is_load[diff_commit_count - 1] =
              commit_snapshot.is_load[lane];
#endif
          if (total_commits <= trace_commits) {
            fprintf(stderr,
                    "[OOOD-SOC] commit=%llu lane=%d pc=0x%08x ra=0x%08x sp=0x%08x a0=0x%08x\n",
                    (unsigned long long)total_commits, lane,
                    lane_pc, read_gpr_from_top(1), read_gpr_from_top(2),
                    read_gpr_from_top(10));
          }
          // Some ysyxSoC compositions expose the commit probe but omit the
          // generated Ebreak DPI side effect. Recognize the architectural
          // ebreak at commit so the harness can terminate cleanly.
          if (pmem_read(lane_pc, 4) == 0x00100073U) {
            const word_t halt_ret = read_gpr_from_top(10);
            fprintf(stderr, "[OOOD-SOC] ebreak pc=0x%08x a0=0x%08x\n",
                    lane_pc, halt_ret);
            set_npc_state(NPC_END, lane_pc, halt_ret);
          }
        }

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

#if 0
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
#endif

#ifdef DIFFTEST
        if (!difftest_step_group(diff_commit_pcs, diff_commit_mem_addr,
                                 diff_commit_is_load, diff_commit_count,
                                 commit_snapshot.gpr_before)) {
          npc_state.state = NPC_ABORT;
          break;
        }
#endif
      }
    }

    if (progress_interval != 0 && total_cycles % progress_interval == 0) {
      fprintf(stderr,
              "[OOOD-SOC] cycles=%llu commits=%llu last_pc=0x%08x\n",
              (unsigned long long)total_cycles,
              (unsigned long long)total_commits, read_pc_from_top());
    }

    if (npc_state.state == NPC_END) {
      npc_state.halt_pc = read_pc_from_top();
    }

    if (npc_state.state != NPC_RUNNING) {
      break;
    }
    if (++hang_counter > HANG_TIMEOUT_CYCLES) {
      printf("[HANG] No commit for %d cycles, simulation stopped\n", HANG_TIMEOUT_CYCLES);
#if defined(DIFFTEST)
      {
        const auto *root = top->rootp;
#define CORE(name) root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__core__DOT__##name
        fprintf(stderr,
                "[OOOD-SOC-HANG] pc=0x%08x ifu_state=%u work=%u "
                "ic_state=%u count=%u in_addr=0x%08x "
                "ic_ar=%u/%u addr=0x%08x "
                "master_ar=%u/%u addr=0x%08x "
                "master_r=%u/%u data=0x%08x\n",
                read_pc_from_top(), CORE(ifu__DOT__state), CORE(ifu__DOT__work),
                CORE(icache__DOT__state), CORE(icache__DOT__count),
                CORE(icache__DOT__in_addr), CORE(_ifu_io_imem_arvalid),
                CORE(_icache_io_in_arready), CORE(_ifu_io_imem_araddr),
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_arvalid,
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_arready,
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_araddr,
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rvalid,
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rready,
                root->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__io_master_rdata);
#undef CORE
      }
#endif
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
