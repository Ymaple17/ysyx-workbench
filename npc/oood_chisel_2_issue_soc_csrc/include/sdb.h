#ifndef __SDB_H__
#define __SDB_H__

#include <stdint.h>

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  char experence[40];
  uint64_t num;
  struct watchpoint *next;
} WP;

WP* new_wp(char *EXPR);
WP* free_wp(WP *wp);

uint64_t expr(char *e);
uint64_t eval(int p, int q);

void init_regex();
void init_wp_pool();

void sdb_mainloop();

#endif
