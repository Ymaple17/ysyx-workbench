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
    
    long long total_inst = counters[EVENT_EXU_COMP]; 
    if (cycles > 0) {
        printf("IPC: %.4f\n", (double)total_inst / cycles);
        printf("CPI: %.4f\n", (double)cycles / total_inst);
    }
    
    printf("Instructions Executed (EXU): %lld\n", counters[EVENT_EXU_COMP]);
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
    long long total_ifu_cycles = counters[EVENT_IFU_FETCH] + counters[EVENT_IFU_STALL_ICACHE] + counters[EVENT_IFU_STALL_IDU]; 
    printf("ICache Stall: %lld\n", counters[EVENT_IFU_STALL_ICACHE]);
    printf("Pipeline Stall: %lld\n", counters[EVENT_IFU_STALL_IDU]);

    printf("\n[ICache Performance]\n");
    long long icache_access = counters[EVENT_IFU_FETCH]; // Total fetches completed
    long long icache_miss = counters[EVENT_ICACHE_MISS];
    long long icache_hit = icache_access - icache_miss;
    long long icache_stall_cycles = counters[EVENT_IFU_STALL_ICACHE];
    
    if (icache_access > 0) {
        printf("Accesses: %lld\n", icache_access);
        printf("Hits:     %lld (%.2f%%)\n", icache_hit, 100.0 * icache_hit / icache_access);
        printf("Misses:   %lld (%.2f%%)\n", icache_miss, 100.0 * icache_miss / icache_access);
        
        // AMAT = (Total Time spent in IFU for ICache) / Total Accesses
        // Total Time = Fetches (1 cycle each) + Stalls (wait cycles)
        double amat = (double)(icache_access + icache_stall_cycles) / icache_access;
        printf("AMAT:     %.4f cycles\n", amat);
    }

    printf("\n[BPU Performance]\n");
    long long bpu_predict = counters[EVENT_BPU_PREDICT];
    long long bpu_mispred = counters[EVENT_BPU_MISPRED];
    printf("Predictions:  %lld\n", bpu_predict);
    printf("Mispredicts:  %lld\n", bpu_mispred);
    if (bpu_predict > 0) {
        printf("Hit Rate:     %.2f%%\n", 100.0 * (bpu_predict - bpu_mispred) / bpu_predict);
    }

    printf("\n[LSU Statistics]\n");
    long long total_access = counters[EVENT_LSU_READ] + counters[EVENT_LSU_WRITE];
    printf("Total Accesses: %lld\n", total_access);
    printf("Reads: %lld\n", counters[EVENT_LSU_READ]);
    printf("Writes: %lld\n", counters[EVENT_LSU_WRITE]);
    if (total_access > 0) {
        printf("Average Latency: %.2f cycles\n", (double)counters[EVENT_LSU_LATENCY] / total_access);
    }

    printf("\n[MulDiv Statistics]\n");
    printf("Mul instructions: %lld\n", counters[EVENT_MUL]);
    printf("Div instructions: %lld\n", counters[EVENT_DIV]);
    
    printf("============================================\n");
}
