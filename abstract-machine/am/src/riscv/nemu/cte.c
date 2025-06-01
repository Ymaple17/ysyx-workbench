#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    if (c->GPR1 == -1) {
      ev.event = EVENT_YIELD;
    } else if (c->GPR1 >= 0 && c->GPR1 <= 19) {
      ev.event = EVENT_SYSCALL;
    } else {
      ev.event = EVENT_ERROR;
    }

    c = user_handler(ev, c);

    switch (ev.event) {
      case EVENT_PAGEFAULT:
      case EVENT_ERROR:
        break;
      default: c->mepc += 4;
    }

    assert(c != NULL);
  }

  return c;
}


extern void __am_asm_trap(void);//中断异常入口

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  return NULL;
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
