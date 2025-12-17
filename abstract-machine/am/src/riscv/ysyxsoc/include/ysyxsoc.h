#ifndef YSYXSOC_H__
#define YSYXSOC_H__

#include <klib-macros.h>
#include <riscv/riscv.h>

#define soc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))



#define RTC_ADDR         0x02000000

#define GPIO_ADDR_BASE 0x10002000
#define REG_GPIO_LED (GPIO_ADDR_BASE + 0x0)
#define REG_GPIO_SW  (GPIO_ADDR_BASE + 0x4)
#define REG_GPIO_SEG (GPIO_ADDR_BASE + 0x8)

#define CSR_MARCHID 0xF12


#define UART_BASE		0x10000000L
#define UART_TX			(UART_BASE + 0x00)
#define UART_RX			(UART_BASE + 0x00)
#define UART_LCR		(UART_BASE + 0x03)
#define UART_LSR		(UART_BASE + 0x05)
#define UART_LSB		(UART_BASE + 0x00)
#define UART_MSB		(UART_BASE + 0x01)
#define UART_IER    (UART_BASE + 0x02)

#define PS2_KBD_ADDR         0x10011000
#define PS2_KBD_REG_SCANCODE 0x0

#define VGACTL_ADDR         0x211FFFF0
#define SYNC_ADDR           (VGACTL_ADDR + 4)
#define FB_ADDR             0x21000000

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)
#define YSYXSOC_PADDR_SPACE \
  RANGE(&_pmem_start, PMEM_END), \
  RANGE(FB_ADDR, FB_ADDR + 0x200000), \
  RANGE(MMIO_BASE, MMIO_BASE + 0x1000) /* serial, rtc, screen, keyboard */

typedef uintptr_t PTE;

#define PGSIZE		4096

#endif

