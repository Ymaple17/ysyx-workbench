#include <cpu/cpu.h>
#include <common.h>

word_t expr(char *e, bool *success);

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;

  char expr[256];
  word_t old;

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

static WP* new_wp() {
  assert(free_);
  WP* ret = free_;
  free_ = free_->next;
  ret->next = head;
  head = ret;
  return ret;
}

static void free_wp(WP *wp) {
  WP* h = head;
  if (h == wp) head = NULL;
  else {
    while (h && h->next != wp) h = h->next;
    assert(h);
    h->next = wp->next;
  }
  wp->next = free_;
  free_ = wp;
}

void wp_watch(char *expr, word_t res) {
  WP* wp = new_wp();
  strcpy(wp->expr, expr);
  wp->old = res;
  printf("Watchpoint %d: %s\n", wp->NO, expr);
}

void wp_remove(int no) {
  assert(no < NR_WP);
  WP* wp = &wp_pool[no];
  free_wp(wp);
  printf("Delete watchpoint %d: %s\n", wp->NO, wp->expr);
}

void wp_iterate() {
  WP* h = head;
  if (!h) {
    puts("No watchpoints.");
    return;
  }
  printf("%-8s%-8s\n", "Num", "What");
  while (h) {
    printf("%-8d%-8s\n", h->NO, h->expr);
    h = h->next;
  }
}

int wp_difftest() {
  WP* h = head;
  int ret = 0;
  while (h) {
    bool _;
    word_t new_val = expr(h->expr, &_);
    if (h->old != new_val) {
      printf("Watchpoint %d: %s\n"
        "Old value = %u\n"
        "New value = %u\n"
        , h->NO, h->expr, h->old, new_val);
      h->old = new_val;
      ret++;
    }
    h = h->next;
  }
  return ret;
}
