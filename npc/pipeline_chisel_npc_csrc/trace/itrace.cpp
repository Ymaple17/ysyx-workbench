#include "../include/trace.h"
#include "../include/common.h"
#include "/home/qiu/ysyx-workbench/npc/include/generated/autoconf.h"
#include <cstdio>
#include <cassert>
#include <malloc.h>
#define MAX_IRINGBUF 16
extern const char *regs[];

typedef struct {
  uint32_t pc;
  uint32_t inst;
} ItraceNode;

ItraceNode iringbuf[MAX_IRINGBUF] = {0};
int p_cur = 0;
bool full = false;

void itrace_inst(uint32_t pc, uint32_t inst) {
    assert(p_cur >= 0 && p_cur < MAX_IRINGBUF);
    iringbuf[p_cur].pc = pc;
    iringbuf[p_cur].inst = inst;
    p_cur = (p_cur + 1) % MAX_IRINGBUF;
    full = full || (p_cur == 0);
}

void display_inst() {
    if (!full && p_cur == 0) return;

    int latest_idx = (p_cur - 1 + MAX_IRINGBUF) % MAX_IRINGBUF;

    void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
    char buf[128];
    char *p = buf;
    
    // Print just the latest instruction
    p += sprintf(buf, "0x%08x: %08x ", iringbuf[latest_idx].pc, iringbuf[latest_idx].inst);
        
    uint8_t *code = (uint8_t*)&iringbuf[latest_idx].inst;
    disassemble(p, buf + sizeof(buf) - p, iringbuf[latest_idx].pc, code, 4);

    puts(buf);
}
