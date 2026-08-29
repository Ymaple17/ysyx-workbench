#include "../include/perf.h"
#include <algorithm>
#include <stdio.h>
#include <unordered_map>
#include <utility>
#include <vector>

static long long counters[EVENT_MAX] = {0};
static std::unordered_map<uint32_t, long long> pc_counters[PC_EVENT_MAX];

extern "C" void npc_pm_event(int event_id, long long data) {
    if (event_id >= 0 && event_id < EVENT_MAX) {
        counters[event_id] += data;
    }
}

extern "C" void npc_pm_pc_event(int event_id, int pc) {
    if (event_id >= 0 && event_id < PC_EVENT_MAX) {
        pc_counters[event_id][static_cast<uint32_t>(pc)]++;
    }
}

static void print_top_pc(const char *label, int event_id) {
    std::vector<std::pair<uint32_t, long long>> rows;
    rows.reserve(pc_counters[event_id].size());
    for (const auto &entry : pc_counters[event_id]) {
        rows.push_back(entry);
    }
    std::sort(rows.begin(), rows.end(), [](
        const std::pair<uint32_t, long long> &a,
        const std::pair<uint32_t, long long> &b) {
        return a.second > b.second || (a.second == b.second && a.first < b.first);
    });
    printf("%s", label);
    const size_t limit = std::min<size_t>(12, rows.size());
    for (size_t i = 0; i < limit; ++i) {
        printf(" %08x:%lld", rows[i].first, rows[i].second);
    }
    printf("\n");
}

void print_perf_stats(unsigned long long cycles) {
    printf("\n========== Performance Statistics ==========\n");
    printf("Total Cycles: %llu\n", cycles);
    
    long long commit_inst = counters[EVENT_COMMIT];
    long long exu_inst = counters[EVENT_EXU_COMP];
    if (cycles > 0 && commit_inst > 0) {
        printf("IPC: %.4f\n", (double)commit_inst / cycles);
        printf("CPI: %.4f\n", (double)cycles / commit_inst);
    }
    
    printf("Commit Instructions: %lld\n", commit_inst);
    printf("Instructions Executed (EXU): %lld\n", exu_inst);
    printf("Instructions Fetched (IFU):  %lld\n", counters[EVENT_IFU_FETCH]);
    
    printf("\n[Instruction Distribution]\n");
    long long total_decoded = counters[EVENT_INST_TYPE_COMPUTE] + counters[EVENT_INST_TYPE_LOAD] + 
                              counters[EVENT_INST_TYPE_STORE] + counters[EVENT_INST_TYPE_CSR] + 
                              counters[EVENT_INST_TYPE_BRANCH] + counters[EVENT_INST_TYPE_JUMP] + 
                              counters[EVENT_INST_TYPE_OTHER];
    
    if (total_decoded > 0) {
        printf("Compute: %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_COMPUTE], 100.0 * counters[EVENT_INST_TYPE_COMPUTE] / total_decoded);
        printf("Load:    %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_LOAD], 100.0 * counters[EVENT_INST_TYPE_LOAD] / total_decoded);
        printf("Store:   %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_STORE], 100.0 * counters[EVENT_INST_TYPE_STORE] / total_decoded);
        printf("CSR:     %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_CSR], 100.0 * counters[EVENT_INST_TYPE_CSR] / total_decoded);
        printf("Branch:  %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_BRANCH], 100.0 * counters[EVENT_INST_TYPE_BRANCH] / total_decoded);
        printf("Jump:    %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_JUMP], 100.0 * counters[EVENT_INST_TYPE_JUMP] / total_decoded);
        printf("Other:   %lld (%.2f%%)\n", counters[EVENT_INST_TYPE_OTHER], 100.0 * counters[EVENT_INST_TYPE_OTHER] / total_decoded);
    }

    printf("\n[IFU Stalls]\n");
    printf("ICache Stall: %lld\n", counters[EVENT_IFU_STALL_ICACHE]);
    printf("Pipeline Stall: %lld\n", counters[EVENT_IFU_STALL_IDU]);

    printf("\n[Frontend Width]\n");
    printf("Fetch Slot0 Valid: %lld\n", counters[EVENT_FETCH_SLOT0_VALID]);
    printf("Fetch Slot1 Valid: %lld\n", counters[EVENT_FETCH_SLOT1_VALID]);
    printf("Fetch Slot2 Valid: %lld\n", counters[EVENT_FETCH_SLOT2_VALID]);
    printf("Fetch Slot3 Valid: %lld\n", counters[EVENT_FETCH_SLOT3_VALID]);
    printf("Fetch Slot1 Killed: %lld\n", counters[EVENT_FETCH_SLOT1_KILLED]);
    printf("FQ Enq2: %lld\n", counters[EVENT_FQ_ENQ2]);
    printf("FQ Space One: %lld\n", counters[EVENT_FQ_SPACE_ONE]);
    printf("Fetch Redirect Bubble: %lld\n", counters[EVENT_FETCH_REDIRECT_BUBBLE]);
    printf("Fetch Blocks: %lld\n", counters[EVENT_FETCH_BLOCK]);
    printf("Fetch Valid Instructions: %lld\n", counters[EVENT_FETCH_VALID_INST]);
    printf("Fetch Two-Instruction Blocks: %lld\n", counters[EVENT_FETCH_BLOCK2]);
    printf("FetchBuffer Full Cycles: %lld\n", counters[EVENT_FETCH_BUFFER_FULL]);
    printf("FTQ Full Cycles: %lld\n", counters[EVENT_FTQ_FULL]);
    printf("Fetch Line Tails: %lld\n", counters[EVENT_FETCH_LINE_TAIL]);
    printf("Spec GHR Rollbacks: %lld\n", counters[EVENT_SPEC_GHR_ROLLBACK]);
    printf("RAS Rollbacks: %lld\n", counters[EVENT_RAS_ROLLBACK]);
    printf("FTQ High Water: %lld\n", counters[EVENT_FTQ_HIGH_WATER]);
    printf("FTQ Stale Recover: %lld\n", counters[EVENT_FTQ_STALE_RECOVER]);
    if (counters[EVENT_FETCH_BLOCK] > 0) {
        printf("Average Fetch Width: %.4f\n",
               (double)counters[EVENT_FETCH_VALID_INST] / counters[EVENT_FETCH_BLOCK]);
    }
    if (counters[EVENT_FETCH_SLOT0_VALID] > 0) {
        printf("Fetch Slot1 Util: %.2f%%\n",
               100.0 * counters[EVENT_FETCH_SLOT1_VALID] / counters[EVENT_FETCH_SLOT0_VALID]);
    }

    printf("\n[ICache Performance]\n");
    long long icache_access = counters[EVENT_IFU_FETCH];
    long long icache_miss = counters[EVENT_ICACHE_MISS];
    long long icache_hit = icache_access - icache_miss;
    long long icache_stall_cycles = counters[EVENT_IFU_STALL_ICACHE];
    
    if (icache_access > 0) {
        printf("Accesses: %lld\n", icache_access);
        printf("Hits:     %lld (%.2f%%)\n", icache_hit, 100.0 * icache_hit / icache_access);
        printf("Misses:   %lld (%.2f%%)\n", icache_miss, 100.0 * icache_miss / icache_access);
        double amat = (double)(icache_access + icache_stall_cycles) / icache_access;
        printf("AMAT:     %.4f cycles\n", amat);
    }

    printf("\n[BPU Performance]\n");
    long long bpu_predict = counters[EVENT_BPU_PREDICT];
    long long bpu_mispred = counters[EVENT_BPU_MISPRED];
    printf("Predictions:  %lld\n", bpu_predict);
    printf("Mispredicts:  %lld\n", bpu_mispred);
    printf("BP Flushes:   %lld\n", counters[EVENT_BP_FLUSH]);
    printf("Direction Miss:    %lld\n", counters[EVENT_BPU_DIR_MISPRED]);
    printf("Target Miss:       %lld\n", counters[EVENT_BPU_TARGET_MISPRED]);
    printf("Unpredicted JALR:  %lld\n", counters[EVENT_BPU_UNPREDICTED]);
    printf("Tagged Hits:       %lld\n", counters[EVENT_BPU_TAGGED_HIT]);
    printf("Indirect Hits:     %lld\n", counters[EVENT_BPU_INDIRECT_HIT]);
    printf("TAGE Alternate:    %lld\n", counters[EVENT_TAGE_USE_ALT]);
    printf("Bimodal Selections: %lld\n", counters[EVENT_BIMODAL_SELECTED]);
    printf("Control Direct Complete: %lld\n", counters[EVENT_CONTROL_DIRECT_COMPLETE]);
    printf("Store Direct Complete: %lld\n", counters[EVENT_STORE_DIRECT_COMPLETE]);
    printf("DCache Secondary Alloc: %lld\n", counters[EVENT_DCACHE_SECONDARY_ALLOC]);
    printf("DCache MSHR Merge: %lld\n", counters[EVENT_DCACHE_MSHR_MERGE]);
    printf("TAGE Allocations:  %lld\n", counters[EVENT_TAGE_ALLOC]);
    printf("ITAGE Hits:        %lld\n", counters[EVENT_ITAGE_HIT]);
    printf("ITAGE Allocations: %lld\n", counters[EVENT_ITAGE_ALLOC]);
    printf("Loop Predictor Hits: %lld\n", counters[EVENT_LOOP_PREDICT_HIT]);
    if (bpu_predict > 0) {
        printf("Hit Rate:     %.2f%%\n", 100.0 * (bpu_predict - bpu_mispred) / bpu_predict);
        printf("Mispredict Rate: %.2f%%\n", 100.0 * bpu_mispred / bpu_predict);
    }

    printf("\n[Rename / Frontend Stalls]\n");
    printf("ROB Full: %lld\n", counters[EVENT_ROB_FULL]);
    printf("FL Empty: %lld\n", counters[EVENT_FL_EMPTY]);
    printf("RS Full:  %lld\n", counters[EVENT_RS_FULL]);
    printf("FQ Full:  %lld\n", counters[EVENT_FQ_FULL]);
    printf("FQ Empty: %lld\n", counters[EVENT_FQ_EMPTY]);

    printf("\n[Writeback / Commit Bottlenecks]\n");
    printf("CDB Conflicts:       %lld\n", counters[EVENT_CDB_CONFLICT]);
    printf("CDB Blocked Results: %lld\n", counters[EVENT_CDB_BLOCKED]);
    printf("ALU1 Issues:         %lld\n", counters[EVENT_ALU1_ISSUE]);
    printf("Dual ALU Issues:     %lld\n", counters[EVENT_DUAL_ALU_ISSUE]);
    printf("FU Same-Cycle Refill: %lld\n", counters[EVENT_FU_REFILL]);
    printf("RS CDB Wake-Issue:    %lld\n", counters[EVENT_RS_CDB_WAKE_ISSUE]);
    printf("RS Fresh Issue:        %lld\n", counters[EVENT_RS_FRESH_ISSUE]);
    printf("LSU Address Refill:   %lld\n", counters[EVENT_LSU_ADDR_REFILL]);
    printf("WB Age Reorders:     %lld\n", counters[EVENT_WB_AGE_REORDER]);
    printf("Commit Slot0:        %lld\n", counters[EVENT_COMMIT_SLOT0]);
    printf("Commit Slot1:        %lld\n", counters[EVENT_COMMIT_SLOT1]);
    printf("Commit Slot2:        %lld\n", counters[EVENT_COMMIT_SLOT2]);
    printf("Commit Slot3:        %lld\n", counters[EVENT_COMMIT_SLOT3]);
    printf("Commit2 Cycles:      %lld\n", counters[EVENT_COMMIT2]);
    printf("Commit4 Cycles:      %lld\n", counters[EVENT_COMMIT4]);
    printf("Quad ALU Issues:     %lld\n", counters[EVENT_QUAD_ALU_ISSUE]);
    printf("Commit Slot1 Block:  %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK]);
    printf("Commit Slot1 NotReady: %lld\n", counters[EVENT_COMMIT_SLOT1_NOT_READY]);
    printf("Commit Slot1 Block Slot0Excl: %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_SLOT0_EXCL]);
    printf("Commit Slot1 Block Mem:       %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_MEM]);
    printf("Commit Slot1 Block Ctrl:      %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_CTRL]);
    printf("Commit Slot1 Block CSR:       %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_CSR]);
    printf("Commit Slot1 Block Special:   %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_SPECIAL]);
    printf("Commit Slot1 Block BP:        %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_BP]);
    printf("Commit Slot1 Load:            %lld\n", counters[EVENT_COMMIT_SLOT1_LOAD]);
    printf("Commit Slot1 Control:         %lld\n", counters[EVENT_COMMIT_SLOT1_CTRL]);
    printf("Commit Slot1 Store:           %lld\n", counters[EVENT_COMMIT_SLOT1_STORE]);
    printf("BPU Update Queue Block:       %lld\n", counters[EVENT_BPU_UPDATE_QUEUE_BLOCK]);
    if (counters[EVENT_COMMIT_SLOT0] > 0) {
        printf("Commit Slot1 Util:   %.2f%%\n",
               100.0 * counters[EVENT_COMMIT_SLOT1] / counters[EVENT_COMMIT_SLOT0]);
    }
    printf("Head Not Ready:      %lld\n", counters[EVENT_COMMIT_HEAD_WAIT]);
    printf("Wait Store Commit:   %lld\n", counters[EVENT_COMMIT_WAIT_STORE]);
    printf("Wait Fence/Icache:   %lld\n", counters[EVENT_COMMIT_WAIT_FENCE]);
    printf("Wait BP Recovery:    %lld\n", counters[EVENT_COMMIT_WAIT_BP]);
    printf("Wait Flush/IRQ:      %lld\n", counters[EVENT_COMMIT_WAIT_FLUSH]);
    printf("StoreBuffer Full:    %lld\n", counters[EVENT_STORE_BUFFER_FULL]);
    printf("Dispatch Slot1:      %lld\n", counters[EVENT_DISPATCH_SLOT1]);
    printf("Dispatch Slot2:      %lld\n", counters[EVENT_DISPATCH_SLOT2]);
    printf("Dispatch Slot3:      %lld\n", counters[EVENT_DISPATCH_SLOT3]);
    printf("Dispatch Branch+Slot1: %lld\n", counters[EVENT_DISPATCH_BRANCH_SLOT1]);
    printf("Dispatch Slot1 Ctrl Block: %lld\n", counters[EVENT_DISPATCH_SLOT1_CTRL_BLOCK]);
    printf("Dispatch Slot1 Backend Block: %lld\n", counters[EVENT_DISPATCH_SLOT1_BACKEND_BLOCK]);
    printf("Head Wait ALU:       %lld\n", counters[EVENT_HEAD_WAIT_ALU]);
    printf("Head Wait Load:      %lld\n", counters[EVENT_HEAD_WAIT_LOAD]);
    printf("Head Wait Store:     %lld\n", counters[EVENT_HEAD_WAIT_STORE]);
    printf("Head Wait Control:   %lld\n", counters[EVENT_HEAD_WAIT_CTRL]);
    printf("Head Wait Other:     %lld\n", counters[EVENT_HEAD_WAIT_OTHER]);
    printf("Fetch Wait Resource: %lld\n", counters[EVENT_FETCH_WAIT_RESOURCE]);
    printf("Fetch Wait PC:       %lld\n", counters[EVENT_FETCH_WAIT_PC]);
    printf("Fetch Slot1 Credit Block: %lld\n", counters[EVENT_FETCH_SLOT1_CREDIT_BLOCK]);
    printf("Branch Issues:       %lld\n", counters[EVENT_BRANCH_ISSUE]);
    printf("Branch Ready Wait:   %lld\n", counters[EVENT_BRANCH_READY_WAIT]);

    printf("\n[Stage12 PC Hotspots]\n");
    print_top_pc("Head Wait PCs:", PC_EVENT_HEAD_WAIT);
    print_top_pc("Direction Miss PCs:", PC_EVENT_DIR_MISPRED);
    print_top_pc("Target Miss PCs:", PC_EVENT_TARGET_MISPRED);
    print_top_pc("Unpredicted PCs:", PC_EVENT_UNPREDICTED);
    print_top_pc("Flush PCs:", PC_EVENT_BP_FLUSH);

    printf("\n[LSU Statistics]\n");
    long long total_access = counters[EVENT_LSU_READ] + counters[EVENT_LSU_WRITE];
    printf("Total Accesses: %lld\n", total_access);
    printf("Reads: %lld\n", counters[EVENT_LSU_READ]);
    printf("Writes: %lld\n", counters[EVENT_LSU_WRITE]);
    printf("StoreBuffer Enq:   %lld\n", counters[EVENT_STORE_BUFFER_ENQ]);
    printf("StoreBuffer Enq2:  %lld\n", counters[EVENT_STORE_BUFFER_ENQ2]);
    printf("StoreBuffer Merge: %lld\n", counters[EVENT_STORE_BUFFER_MERGE]);
    printf("StoreBuffer Write Bursts: %lld\n", counters[EVENT_STORE_BUFFER_WRITE_BURST]);
    printf("StoreBuffer Write Beats: %lld\n", counters[EVENT_STORE_BUFFER_WRITE_BEAT]);
    printf("StoreBuffer Drain: %lld\n", counters[EVENT_STORE_BUFFER_DRAIN]);
    printf("StoreBuffer Fwd:   %lld\n", counters[EVENT_STORE_BUFFER_FORWARD]);
    printf("SQ Wait Cycles: %lld\n", counters[EVENT_LSU_SQ_WAIT]);
    printf("SQ Forwards:    %lld\n", counters[EVENT_LSU_SQ_FORWARD]);
    printf("Bus Wait Cycles: %lld\n", counters[EVENT_LSU_BUS_WAIT]);
    printf("LQ Alloc:        %lld\n", counters[EVENT_LQ_ALLOC]);
    printf("LQ Full Cycles:  %lld\n", counters[EVENT_LQ_FULL]);
    printf("LQ High Water:   %lld\n", counters[EVENT_LQ_HIGH_WATER]);
    printf("Load Replays:    %lld\n", counters[EVENT_LOAD_REPLAY]);
    printf("Replay Store Wait: %lld\n", counters[EVENT_REPLAY_STORE_WAIT]);
    printf("Replay DCache Busy: %lld\n", counters[EVENT_REPLAY_DCACHE_BUSY]);
    printf("Replay CDB Busy: %lld\n", counters[EVENT_REPLAY_CDB_BUSY]);
    printf("Stale Load Resp: %lld\n", counters[EVENT_STALE_LOAD_RESP]);
    printf("Mem Order Violation: %lld\n", counters[EVENT_MEM_ORDER_VIOLATION]);
    if (total_access > 0) {
        printf("Average Latency: %.2f cycles\n", (double)counters[EVENT_LSU_LATENCY] / total_access);
    }

    printf("\n[DCache Performance]\n");
    long long dcache_access = counters[EVENT_DCACHE_ACCESS];
    long long dcache_hit = counters[EVENT_DCACHE_HIT];
    long long dcache_miss = counters[EVENT_DCACHE_MISS];
    printf("DCache Accesses: %lld\n", dcache_access);
    printf("DCache Hits:     %lld\n", dcache_hit);
    printf("DCache Misses:   %lld\n", dcache_miss);
    printf("DCache Bypass:   %lld\n", counters[EVENT_DCACHE_BYPASS]);
    printf("DCache MSHR Alloc: %lld\n", counters[EVENT_DCACHE_MSHR_ALLOC]);
    printf("DCache Hit Under Miss: %lld\n", counters[EVENT_DCACHE_HIT_UNDER_MISS]);
    printf("DCache MSHR Refill: %lld\n", counters[EVENT_DCACHE_MSHR_REFILL]);
    printf("DCache Store Hits: %lld\n", counters[EVENT_DCACHE_STORE_HIT]);
    if (dcache_access > 0) {
        printf("DCache Hit Rate: %.2f%%\n", 100.0 * dcache_hit / dcache_access);
    }

    printf("\n[MulDiv Statistics]\n");
    printf("Mul instructions: %lld\n", counters[EVENT_MUL]);
    printf("Div instructions: %lld\n", counters[EVENT_DIV]);
    
    printf("============================================\n");
}
