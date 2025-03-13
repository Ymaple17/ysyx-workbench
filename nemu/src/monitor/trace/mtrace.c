#include <common.h>

void trace_read(paddr_t addr, int len) {
  mtrace_write("mtrace: read at " FMT_PADDR ", len=%d\n", addr, len);
}

void trace_write(paddr_t addr, int len, word_t data) {
  mtrace_write("mtrace: write at " FMT_PADDR ", len=%d, data=" FMT_WORD "\n", addr, len, data);
}

