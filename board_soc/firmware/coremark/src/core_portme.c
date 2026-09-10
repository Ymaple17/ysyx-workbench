#include "coremark.h"

#if VALIDATION_RUN
volatile ee_s32 seed1_volatile = 0x3415;
volatile ee_s32 seed2_volatile = 0x3415;
volatile ee_s32 seed3_volatile = 0x66;
#elif PERFORMANCE_RUN
volatile ee_s32 seed1_volatile = 0x0;
volatile ee_s32 seed2_volatile = 0x0;
volatile ee_s32 seed3_volatile = 0x66;
#else
volatile ee_s32 seed1_volatile = 0x8;
volatile ee_s32 seed2_volatile = 0x8;
volatile ee_s32 seed3_volatile = 0x8;
#endif
volatile ee_s32 seed4_volatile = ITERATIONS;
volatile ee_s32 seed5_volatile = 0;

static volatile uint32_t *const board_cycle = (volatile uint32_t *)0x10000008u;
static volatile uint32_t *const board_clock_hz = (volatile uint32_t *)0x10000010u;
static uint32_t start_cycle;
static uint32_t stop_cycle;
static uint32_t board_ticks_per_ms = 50000u;

void ioe_init(void) {
  uint32_t clock_hz = *board_clock_hz;
  if (clock_hz >= 1000u)
    board_ticks_per_ms = clock_hz / 1000u;
}

void start_time(void) {
  start_cycle = *board_cycle;
}

void stop_time(void) {
  stop_cycle = *board_cycle;
}

CORE_TICKS get_time(void) {
  return stop_cycle - start_cycle;
}

ee_u32 time_in_secs(CORE_TICKS ticks) {
  return ticks / board_ticks_per_ms;
}

ee_u32 default_num_contexts = 1;

void portable_init(core_portable *p, int *argc, char *argv[]) {
  (void)argc;
  (void)argv;
  if (sizeof(ee_ptr_int) != sizeof(ee_u8 *))
    ee_printf("ERROR! pointer size mismatch\n");
  if (sizeof(ee_u32) != 4)
    ee_printf("ERROR! ee_u32 size mismatch\n");
  p->portable_id = 1;
}

void portable_fini(core_portable *p) {
  p->portable_id = 0;
}
