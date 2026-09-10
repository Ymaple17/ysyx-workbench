#include <am.h>
#include <klib-macros.h>
#include <stdint.h>
#include <klib.h>
#include "../riscv.h"
#include "include/ysyxsoc.h"

extern char _heap_start;
extern char _psram_end;
int main(const char *args);
extern char _pmem_start;
extern char _heap_start, _heap_end;
Area heap = RANGE(&_heap_start, &_psram_end);
// Area heap = RANGE(&_heap_start, &_heap_end);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

#ifdef YSYXSOC_SIM_PRELOADED
void putch(char ch) {
  (void)ch;
}
#else
void putch(char ch) {
  while ((inb(UART_LSR) & 0x20) == 0);
  outb(UART_TX, ch);
}
#endif

#ifndef YSYXSOC_SIM_PRELOADED
static void uart_init(){
	outb(UART_LCR, inb(UART_LCR) | 0x80); //LCR寄存器最高位，使能分频系数寄存器
	outb(UART_LSB, 0x01);
	outb(UART_LCR, inb(UART_LCR) & 0x7f); //恢复LCR寄存器的值，关闭分频系数寄存器，可正常收发数据
	outb(UART_IER, 0xc7);
}
#endif

void halt(int code) {
  soc_trap(code);
  while (1);
}

// ============ 链接脚本导出的符号 ============
// extern char _text_lma[];        // Text 段在 FLASH 中的起始地址
// extern char _text_vma_start[];  // Text 段在 SRAM 中的起始地址
// extern char _text_vma_end[];    // Text 段在 SRAM 中的结束地址

// extern char _rodata_lma[];      // Rodata 段在 FLASH 中的起始地址
// extern char _rodata_vma_start[];// Rodata 段在 SRAM 中的起始地址
// extern char _rodata_vma_end[];  // Rodata 段在 SRAM 中的结束地址

// extern char _data_lma[];        // 数据段在 FLASH 中的起始地址
// extern char _data_vma_start[];  // 数据段在 SRAM 中的起始地址
// extern char _data_vma_end[];    // 数据段在 SRAM 中的结束地址
// extern char _bss_start[];       // BSS 段起始地址
// extern char _bss_end[];         // BSS 段结束地址

// void bootloader_load() {
//   size_t text_size = _text_vma_end - _text_vma_start;
//   if (text_size > 0) {
//     char *src = _text_lma;
//     char *dst = _text_vma_start;
//     while (text_size--) *dst++ = *src++;
//   }
  
//   size_t rodata_size = _rodata_vma_end - _rodata_vma_start;
//   if (rodata_size > 0) {
//     char *src = _rodata_lma;
//     char *dst = _rodata_vma_start;
//     while (rodata_size--) *dst++ = *src++;
//   }
  
//   size_t data_size = _data_vma_end - _data_vma_start;
//   if (data_size > 0) {
//     char *src = _data_lma;
//     char *dst = _data_vma_start;
//     while (data_size--) *dst++ = *src++;
//   }
  
//   size_t bss_size = _bss_end - _bss_start;
//   if (bss_size > 0) {
//     char *dst = _bss_start;
//     while (bss_size--) *dst++ = 0;
//   }
// }

void ysyx_show(){
  int i;
  uint32_t mvendorid;
  uint32_t marchid;
  asm volatile("csrr %0, mvendorid" : "=r"(mvendorid));
  asm volatile("csrr %0, marchid" : "=r"(marchid));
  for(i = 3;i >= 0;i--){
    putch((char)((mvendorid >> i*8) & 0xFF));
  }
  putch('_');
  for(i = 7; i >= 0; i--) {
    int digit = (marchid >> (i * 4)) & 0xF;
    putch(digit < 10 ? digit + '0' : digit - 10 + 'A');
  }
  putch('\n');
  *(volatile uint32_t *)REG_GPIO_SEG = marchid;
}

void _trm_init() {
#ifndef YSYXSOC_SIM_PRELOADED
  uart_init();
#endif
  
  // 执行 bootloader：将程序从 FLASH 加载到 SRAM
  //bootloader_load();
#ifndef YSYXSOC_SIM_PRELOADED
  ysyx_show();
#endif
  // 跳转到 SRAM 中执行 main 函数
  int ret = main(mainargs);
  halt(ret);
}
