#ifndef __PERF_H__
#define __PERF_H__

#include <stdint.h>

// Event IDs
enum {
    EVENT_IFU_FETCH = 0,
    EVENT_IFU_STALL_ICACHE,
    EVENT_IFU_STALL_IDU,
    EVENT_LSU_READ,
    EVENT_LSU_WRITE,
    EVENT_LSU_LATENCY,
    EVENT_EXU_COMP,
    EVENT_INST_TYPE_COMPUTE,
    EVENT_INST_TYPE_LOAD,
    EVENT_INST_TYPE_STORE,
    EVENT_INST_TYPE_CSR,
    EVENT_INST_TYPE_BRANCH,
    EVENT_INST_TYPE_JUMP,
    EVENT_INST_TYPE_OTHER,
    EVENT_ICACHE_MISS,
    EVENT_BPU_PREDICT,
    EVENT_BPU_MISPRED,
    EVENT_MUL,
    EVENT_DIV,
    EVENT_IFU_NO_PC,
    EVENT_IFU_AR_BLOCK,
    EVENT_IFU_FAST_BACKPRESSURE,
    EVENT_IFU_FLUSH_WAIT,
    EVENT_IDU_RAW_STALL,
    EVENT_LSU_LOAD_WAIT,
    EVENT_LSU_STORE_WAIT,
    EVENT_EXU_MUL_WAIT,
    EVENT_EXU_DIV_WAIT,
    EVENT_MAX
};

#ifdef __cplusplus
extern "C" {
#endif

void npc_pm_event(int event_id, long long data);
void print_perf_stats(unsigned long long cycles);

#ifdef __cplusplus
}
#endif

#endif
