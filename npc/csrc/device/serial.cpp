#include <utils.h>
#include <device/io.h>

#define SERIAL_ADDR 0xa00003f8
#define CH_OFFSET 0

static uint8_t *serial_base = NULL;

static void serial_putc(char ch) {
  putc(ch, stderr);
}

static void serial_io_handler(uint32_t offset, int len, bool is_write) {
  assert(len == 1);
  switch (offset) {
    /* We bind the serial port with the host stderr in NEMU. */
    case CH_OFFSET:
      if (is_write) serial_putc(serial_base[0]);
      else Panic("do not support read");
      break;
    default: Panic("do not support offset = %d", offset);
  }
}

void init_serial() {
  serial_base = new_space(8);
  add_device("serial", SERIAL_ADDR, serial_base, 8, serial_io_handler);
}
