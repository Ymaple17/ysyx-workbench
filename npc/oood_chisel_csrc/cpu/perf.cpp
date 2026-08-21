#include "../include/perf.h"
#include <stdio.h>

static long long counters[EVENT_MAX] = {0};

extern "C" void npc_pm_event(int event_id, long long data) {
    if (event_id >= 0 && event_id < EVENT_MAX) {
        counters[event_id] += data;
    }
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
    printf("Fetch Slot1 Killed: %lld\n", counters[EVENT_FETCH_SLOT1_KILLED]);
    printf("FQ Enq2: %lld\n", counters[EVENT_FQ_ENQ2]);
    printf("FQ Space One: %lld\n", counters[EVENT_FQ_SPACE_ONE]);
    printf("Fetch Redirect Bubble: %lld\n", counters[EVENT_FETCH_REDIRECT_BUBBLE]);
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
    printf("Commit Slot0:        %lld\n", counters[EVENT_COMMIT_SLOT0]);
    printf("Commit Slot1:        %lld\n", counters[EVENT_COMMIT_SLOT1]);
    printf("Commit2 Cycles:      %lld\n", counters[EVENT_COMMIT2]);
    printf("Commit Slot1 Block:  %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK]);
    printf("Commit Slot1 NotReady: %lld\n", counters[EVENT_COMMIT_SLOT1_NOT_READY]);
    printf("Commit Slot1 Block Slot0Excl: %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_SLOT0_EXCL]);
    printf("Commit Slot1 Block Mem:       %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_MEM]);
    printf("Commit Slot1 Block Ctrl:      %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_CTRL]);
    printf("Commit Slot1 Block CSR:       %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_CSR]);
    printf("Commit Slot1 Block Special:   %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_SPECIAL]);
    printf("Commit Slot1 Block BP:        %lld\n", counters[EVENT_COMMIT_SLOT1_BLOCK_BP]);
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

    printf("\n[LSU Statistics]\n");
    long long total_access = counters[EVENT_LSU_READ] + counters[EVENT_LSU_WRITE];
    printf("Total Accesses: %lld\n", total_access);
    printf("Reads: %lld\n", counters[EVENT_LSU_READ]);
    printf("Writes: %lld\n", counters[EVENT_LSU_WRITE]);
    printf("StoreBuffer Enq:   %lld\n", counters[EVENT_STORE_BUFFER_ENQ]);
    printf("StoreBuffer Drain: %lld\n", counters[EVENT_STORE_BUFFER_DRAIN]);
    printf("StoreBuffer Fwd:   %lld\n", counters[EVENT_STORE_BUFFER_FORWARD]);
    printf("SQ Wait Cycles: %lld\n", counters[EVENT_LSU_SQ_WAIT]);
    printf("SQ Forwards:    %lld\n", counters[EVENT_LSU_SQ_FORWARD]);
    printf("Bus Wait Cycles: %lld\n", counters[EVENT_LSU_BUS_WAIT]);
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
    if (dcache_access > 0) {
        printf("DCache Hit Rate: %.2f%%\n", 100.0 * dcache_hit / dcache_access);
    }

    printf("\n[MulDiv Statistics]\n");
    printf("Mul instructions: %lld\n", counters[EVENT_MUL]);
    printf("Div instructions: %lld\n", counters[EVENT_DIV]);
    
    printf("============================================\n");
}
