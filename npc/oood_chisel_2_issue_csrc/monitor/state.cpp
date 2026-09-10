#include "../include/state.h"
#include <stdint.h>
#include "../../include/generated/autoconf.h"

NPCState npc_state;

void set_npc_state(NPCStateEnum state, uint32_t pc, int halt_ret){
  npc_state.state = state;
  npc_state.halt_pc = pc;
  npc_state.halt_ret = halt_ret;
}

void check_exit_state(NPCState npc_state){
  if(npc_state.state == NPC_RUNNING){
    
  }
}

