#include "utils.h"

NPCState npc_state = { .state = NPC_STOP };

int exit_code() {
  int good = npc_state.state == NPC_QUIT || 
    npc_state.state == NPC_END && npc_state.halt_code == 0;
  return !good;
}
