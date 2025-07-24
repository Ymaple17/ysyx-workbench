#include <am.h>

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t kbd_value = inl(KBD_ADDR);
  kbd->keydown = (kbd_value & KEYDOWN_MASK) ? true : false;
  kbd->keycode = kbd_value & ~KEYDOWN_MASK;
}
