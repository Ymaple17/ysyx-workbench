#include <am.h>
#include <klib-macros.h>
#include <riscv/riscv.h>

#define SERIAL_PORT 0xa00003f8

#define UART_BASE		0x10000000L
#define UART_TX			(UART_BASE + 0x00)
#define UART_RX			(UART_BASE + 0x00)
#define UART_LCR		(UART_BASE + 0x03)
#define UART_LSR		(UART_BASE + 0x05)
#define UART_LSB		(UART_BASE + 0x00)
#define UART_MSB		(UART_BASE + 0x01)
#define UART_IC     (UART_BASE + 0x02)

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS



void putch(char ch) {
  while ((inb(UART_LSR) & 0x20) == 0);
  outb(UART_TX, ch);
}

// void putch(char ch) {
//   outb(SERIAL_PORT, ch);
// }

void uart_init(){
	outb(UART_LCR, inb(UART_LCR) | 0x80);
	outb(UART_TX, 0x01);
	outb(UART_LCR, inb(UART_LCR) & 0x7f);
	outb(UART_IC, 0xc7);
}

void halt(int code) {
  register long a0 asm("a0") = code;
  asm volatile("ebreak" : : "r"(a0));
  while (1);
}

void ysyx_show(){
  int i;
  int index;
  char buf[10];
  uint32_t number;
  uint32_t mvendorid;
  uint32_t marchid;
  asm volatile("csrr %0, mvendorid" : "=r"(mvendorid));
  asm volatile("csrr %0, marchid" : "=r"(marchid));
  for(i = 3;i >= 0;i--){
    putch((char)((mvendorid >> i*8) & 0xFF));
  }
  number = marchid;
  index = 0;
  while (number > 0)
  {
    buf[index++] = (number % 10) + '0';
    number /= 10;
  }
  for(i = index - 1; i >= 0; i--) {
    putch(buf[i]);
  }
  putch('\n');
  
}

void _trm_init() {
  uart_init();
  // ysyx_show();
  int ret = main(mainargs);
  halt(ret);
}
