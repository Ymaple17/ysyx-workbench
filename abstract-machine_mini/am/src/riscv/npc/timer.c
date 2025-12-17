#include <am.h>
#include <riscv/riscv.h>

void __am_timer_init() {
}

// void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
//   uint32_t high, low,high2;
//   do {
//     high = inl(0xa0000048+4);
//     low = inl(0xa0000048);
//     high2 = inl(0xa0000048+4);
//   } while (high!= high2);
//   uptime->us = (uint64_t)low + (((uint64_t)high) << 32);
// }

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uint32_t high, low,high2;
   do {
    asm volatile ("csrr %0, mcycleh" : "=r" (high));
    asm volatile ("csrr %0, mcycle"  : "=r" (low));
    asm volatile ("csrr %0, mcycleh" : "=r" (high2));
  } while (high != high2);
  uint64_t total_cycles = ((uint64_t)high << 32) | low;
  uptime->us = total_cycles; 
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}