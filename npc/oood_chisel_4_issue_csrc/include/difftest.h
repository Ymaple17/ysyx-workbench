#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "common.h"
#include <stdint.h>
typedef uint32_t word_t;
#define NR_GPRs 32
# define NR_CSRs 4

typedef struct {
  word_t gpr[NR_GPRs];  
  word_t pc;            
  word_t csr[NR_CSRs];
} CPU_State;

extern CPU_State cpu;
#define MEM_START 0x80000000
extern uint8_t pmem[];
#define guest_to_host(addr) (pmem + (addr - MEM_START))

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

void difftest_init(const char* ref_so_file, word_t img_size);
void difftest_one_exec();
bool difftest_check_reg();
void difftest_skip_ref();

extern void (*ref_difftest_memcpy)(word_t addr, void *buf, word_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(word_t n);
extern void (*ref_difftest_raise_intr)(word_t NO);

#endif /* __DIFFTEST_H__ */