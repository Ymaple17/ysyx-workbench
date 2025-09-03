#include "../include/state.h"
#include "../include/common.h"
#include <SDL2/SDL.h>
#include "../../include/generated/autoconf.h"

#define TIMER_HZ 60
uint64_t get_time();
void send_key(uint8_t, bool);
uint64_t get_time();
void vga_update_screen();
extern NPCState npc_state;

void device_update() {
  static uint64_t last = 0;
  uint64_t now = get_time();
  if (now - last < 1000000 / TIMER_HZ) {
    return;
  }
  last = now;
  vga_update_screen();
  SDL_Event event;
  while (SDL_PollEvent(&event)) {
    switch (event.type) {
      case SDL_QUIT:
        npc_state.state = NPC_QUIT;
        break;

      // If a key was pressed
      case SDL_KEYDOWN:
      case SDL_KEYUP: {
        uint8_t k = event.key.keysym.scancode;
        bool is_keydown = (event.key.type == SDL_KEYDOWN);
        send_key(k, is_keydown);
        break;
      }

      default: break;
    }
  }
}

void sdl_clear_event_queue() {
  SDL_Event event;
  while (SDL_PollEvent(&event));
}

