#include <common.h>

#define MAX_IRINGBUF 16

class InstTracer {
private:
  struct ItraceNode {
    word_t pc;
    uint32_t inst;
  } iringbuf[MAX_IRINGBUF];
  int p_cur = 0;
  bool full = false;

public:
  void trace(paddr_t pc, uint32_t inst, bool print);

  void dump();
};

extern InstTracer itracer;
