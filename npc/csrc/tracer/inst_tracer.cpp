#include <common.h>
#include <tracer/inst_tracer.h>

void InstTracer::trace(paddr_t pc, uint32_t inst, bool print) {
  // print step
  if (print) {
    char buf[128];
    char *p = buf;
    p += snprintf(p, sizeof(buf), FMT_PADDR ": %08x\t", pc, inst);
    void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
    disassemble(p, buf + sizeof(buf) - p,
        pc, (uint8_t *)&inst, 4);
    puts(buf);
  }

  // trace
  iringbuf[p_cur].pc = pc;
  iringbuf[p_cur].inst = inst;
  p_cur = (p_cur + 1) % MAX_IRINGBUF;
  full = full || p_cur == 0;
}

void InstTracer::dump() {
  if (!full && !p_cur) return;

  int end = p_cur;
  int i = full?p_cur:0;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  char buf[128]; // 128 should be enough!?
  char *p;
  Statement("Most recently executed instructions");
  do {
    p = buf;
    p += sprintf(buf, "%s" FMT_WORD ": %08x ", (i+1)%MAX_IRINGBUF==end?" --> ":"     ", iringbuf[i].pc, iringbuf[i].inst);
    disassemble(p, buf+sizeof(buf)-p, iringbuf[i].pc, (uint8_t *)&iringbuf[i].inst, 4);

    if ((i+1)%MAX_IRINGBUF==end) printf(ANSI_FG_RED);
    puts(buf);
  } while ((i = (i+1)%MAX_IRINGBUF) != end);
  puts(ANSI_NONE);
}

InstTracer itracer;
