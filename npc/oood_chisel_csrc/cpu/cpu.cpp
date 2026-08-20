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
  for (; n > 0; n--) {
    if (!Verilated::gotFinish()) {
      exec_once();
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
      {
        auto *r = top->rootp;
        unsigned rc = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__count;
        unsigned rh = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__id; // head
        unsigned rt = (unsigned)r->ysyx_25020039__DOT__core__DOT__rob__DOT__tail;
        unsigned long long fb = (unsigned long long)r->ysyx_25020039__DOT__core__DOT__rename__DOT__freeBits;
        printf("[HANG] last_commit_pc=0x%08x rob_count=%u head=%u tail=%u freeBits=0x%llx free_pop=%d\n",
               (unsigned)stuck_pc, rc, rh, rt, fb, __builtin_popcountll(fb));
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
        printf("[HANG] exu_lsu_out_v=%u lsu_in(v=%u rdy=%u pc=0x%08x rob=%u mem=%u wr=%u addr=0x%08x) lsu_out(v=%u rdy=%u) can_wb=%u wb_idx=%u\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT___exu_lsu_io_out_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_in_ready,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_pc,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_rob_idx,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_signals_lsu_mem_write,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_alu_result,
               (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_out_valid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_out_ready,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__can_wb,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__wb_cand_idx);
        printf("[HANG] lsu_ready=%u ar_done=%u ar(v=%u r=%u addr=0x%08x) r(v=%u r=%u)\n",
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__ready,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__ar_handshake_done,
               (unsigned)r->ysyx_25020039__DOT__core__DOT___lsu_io_dmem_arvalid,
               (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__lsu__io_dmem_arready,
               (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu_io_in_bits_r_alu_result,
               (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__lsu__io_dmem_rvalid,
               (unsigned)r->ysyx_25020039__DOT___core_io_dmem_rready);
        unsigned ifu_st = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu__DOT__state;
        unsigned mtvec = (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_1;
        unsigned mepc  = (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_2;
        unsigned mcause= (unsigned)r->ysyx_25020039__DOT__core__DOT__csr__DOT__rf_3;
        unsigned ifu_npc = (unsigned)r->ysyx_25020039__DOT__core__DOT___ifu_io_pc_bits_next_pc;
        unsigned ifu_inv = (unsigned)r->ysyx_25020039__DOT__core__DOT__ifu_io_in_valid;
        unsigned corr = (unsigned)r->ysyx_25020039__DOT__core__DOT____Vcellinp__ifu__io_correct_pc;
        printf("[HANG] ifu_state=%u mtvec=0x%08x mepc=0x%08x mcause=0x%x next_pc=0x%08x corr=0x%08x in_v=%u\n",
               ifu_st, mtvec, mepc, mcause, ifu_npc, corr, ifu_inv);
        unsigned cmst = (unsigned)r->ysyx_25020039__DOT__core__DOT__cm_st_state;
        unsigned lsust = (unsigned)r->ysyx_25020039__DOT__core__DOT__lsu__DOT__state;
        unsigned xb = (unsigned)r->ysyx_25020039__DOT__xbar__DOT__state;
        unsigned awv = (unsigned)r->ysyx_25020039__DOT___core_io_dmem_awvalid;
        unsigned wv  = (unsigned)r->ysyx_25020039__DOT___core_io_dmem_wvalid;
        unsigned swst = (unsigned)r->ysyx_25020039__DOT__sram__DOT__w_state;
        unsigned dw = (unsigned)r->ysyx_25020039__DOT__xbar__DOT__dmem_is_write;
        printf("[HANG] cm_st_state=%u lsu_state=%u xbar_state=%u dmem_wr=%u sram_w=%u\n",
               cmst, lsust, xb, dw, swst);
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
            printf("[HANG] rs[%d] rob=%u rdy=%u/%u pc=0x%08x\n", i, (unsigned)*rsr[i],
                   (unsigned)*rs1[i], (unsigned)*rs2[i], (unsigned)*rsp[i]);
        }
      }
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
