#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include "../include/common.h"
#include "Vysyx_25020039.h"
#include "../../obj_dir/Vysyx_25020039___024root.h"
#include "../../include/generated/autoconf.h"

#ifdef CONFIG_AXI_MONITOR

extern Vysyx_25020039* top;
extern vluint64_t main_time;

#define AXI_STUCK_THRESHOLD 500
#define CHISEL_STALL_THRESHOLD 200

#define CHK(mod, sig, threshold, msg) do { \
    uint8_t v = sig; \
    if (v >= (threshold)) { \
        printf("[axi_monitor] " ANSI_FG_YELLOW "%s." ANSI_FG_RED "%s" ANSI_RESET \
               " : %s (cnt=%u)\n", mod, #sig, msg, v); \
    } \
} while(0)

static uint64_t last_commit_cycle = 0;
static uint32_t r_stall_cnt = 0;
static uint32_t b_stall_cnt = 0;

static void check_chisel_counters(void) {
    auto& r = *top->rootp;
    bool ar_done = r.ysyx_25020039__DOT__core__DOT__lsu__DOT__ar_handshake_done;
    bool aw_done = r.ysyx_25020039__DOT__core__DOT__lsu__DOT__aw_handshake_done;
    bool w_done  = r.ysyx_25020039__DOT__core__DOT__lsu__DOT__w_handshake_done;
    r_stall_cnt = ar_done ? r_stall_cnt + 1 : 0;
    b_stall_cnt = (aw_done && w_done) ? b_stall_cnt + 1 : 0;
    CHK("LSU.DMEM", r_stall_cnt, CHISEL_STALL_THRESHOLD, "rvalid stuck after AR handshake!");
    CHK("LSU.DMEM", b_stall_cnt, CHISEL_STALL_THRESHOLD, "bvalid stuck after AW+W handshake!");
}

static void check_cpu_stall(void) {
    extern bool rst_done;
    if (!rst_done) return;
    bool wb_valid = top->rootp->ysyx_25020039__DOT__core__DOT___lsu_io_out_valid;
    if (wb_valid) {
        last_commit_cycle = main_time;
    } else if (main_time - last_commit_cycle > AXI_STUCK_THRESHOLD && last_commit_cycle > 0) {
        printf(ANSI_FG_RED "\n[axi_monitor] ========== CPU STUCK ==========" ANSI_RESET "\n");
        printf(ANSI_FG_RED "[axi_monitor] No instruction committed for %lu cycles!" ANSI_RESET "\n",
               (unsigned long)(main_time - last_commit_cycle));
        printf(ANSI_FG_RED "[axi_monitor] Check Chisel stall counters: r_stall=%u b_stall=%u" ANSI_RESET "\n",
               r_stall_cnt, b_stall_cnt);
        panic("CPU stuck: AXI bus stalled");
    }
}

void axi_monitor_init(void) {
    printf(ANSI_FG_CYAN "[axi_monitor] Initializing AXI protocol monitor..." ANSI_RESET "\n");
    printf(ANSI_FG_GREEN "[axi_monitor] Monitoring Chisel stall counters + CPU stall detection" ANSI_RESET "\n");
}

void axi_monitor_check(void) {
    if (main_time < 20) return;
    check_chisel_counters();
    check_cpu_stall();
}

#endif
