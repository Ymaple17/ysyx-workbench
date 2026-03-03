#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>
//user_handler 注册事件处理函数
static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {//instr
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 11: ev.event = EVENT_YIELD;c->mepc+=4;break;
      default: ev.event = EVENT_ERROR; break;
    }
    c = user_handler(ev, c);
    assert(c != NULL);
  }
  return c;
}


extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));//mtvec(_am_asm_trap)->_am_irq_handler
  // register event handler
  user_handler = handler;

  return true;
}
/*
bool cte_init(Context*(*handler)(Event, Context*)) {
  putch('C');
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));
  
  uintptr_t mtvec_val;
  asm volatile("csrr %0, mtvec" : "=r"(mtvec_val));
  
  uintptr_t trap_addr = (uintptr_t)__am_asm_trap;
  
  // 打印两个地址
  for(int i = 7; i >= 0; i--) { int d=(mtvec_val>>(i*4))&0xF; putch(d<10?d+'0':d-10+'A'); }
  putch(' ');
  for(int i = 7; i >= 0; i--) { int d=(trap_addr>>(i*4))&0xF; putch(d<10?d+'0':d-10+'A'); }
  putch('\n');
  
  user_handler = handler;
  putch('c');
  return true;
}
*/
Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *cp = (Context *)(kstack.end - sizeof(Context));
  cp->mstatus = 0x1800;
  cp->mepc = (uintptr_t)entry;
  cp->gpr[10] = (uintptr_t)(arg);
  return cp;
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
