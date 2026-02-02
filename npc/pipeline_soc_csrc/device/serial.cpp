#include "../include/state.h"
#include "../include/map.h"
#include "../include/common.h"
#include "../../include/generated/autoconf.h"

#define CH_OFFSET 0
#define CONFIG_SERIAL_MMIO 0xa00003f8

static uint8_t *serial_base = NULL;

static void serial_putc(char ch) {
  putc(ch, stderr);
}

static void serial_io_handler(uint32_t offset, int len, bool is_write) {
  assert(len == 1);
  switch (offset) {
    /* We bind the serial port with the host stderr in NPCA. */
    case CH_OFFSET:
      if (is_write) serial_putc(serial_base[0]);
      else assert(0);
      break;
    default: assert(0);
  }
}

void init_serial() {
  serial_base = new_space(8);
  add_mmio_map("serial", CONFIG_SERIAL_MMIO, serial_base, 8, serial_io_handler);
}
