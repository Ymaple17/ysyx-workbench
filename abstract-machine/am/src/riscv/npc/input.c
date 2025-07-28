#include <am.h>
#include "../riscv.h"
#define KBD_ADDR 0xa0000060
#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t kbd_value = inl(KBD_ADDR);
  kbd->keydown = (kbd_value & KEYDOWN_MASK) ? true : false;
  kbd->keycode = kbd_value & ~KEYDOWN_MASK;
}
