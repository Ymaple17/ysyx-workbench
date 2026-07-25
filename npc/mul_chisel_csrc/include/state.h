#ifndef __STATE_H__
#define __STATE_H__

#include <stdint.h>

typedef enum {
  NPC_RUNNING,
  NPC_STOP,
  NPC_END,
  NPC_ABORT,
  NPC_QUIT
} NPCStateEnum;

typedef struct {
  NPCStateEnum state;
  uint32_t halt_pc;
  int halt_ret;
} NPCState;

extern NPCState npc_state;

void set_npc_state(NPCStateEnum state, uint32_t pc, int halt_ret);


#endif