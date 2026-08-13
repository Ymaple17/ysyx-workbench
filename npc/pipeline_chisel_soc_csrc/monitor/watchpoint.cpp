#include <stdint.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "../include/sdb.h"
#include "../../include/generated/autoconf.h"

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
    wp_pool[i].num = 0;
  }

  head = NULL;
  free_ = wp_pool;
}

WP* new_wp(char *EXPR){
  if(free_ == NULL){
    assert(0);
  }
  else{
    WP *A = head;
    head = free_;
    free_ = free_->next;
    head->next = A;
    strcpy(head->experence, EXPR);
  }
  if(expr(EXPR) == 0 ){
    extern int nr_token;
    head->num = eval(0, nr_token - 1);
  }
  return head;
}

WP* free_wp(WP *wp){
  if(wp == head){
    head = head->next;
    wp->next = free_;
    free_ = wp;
    return head;
  }
  else{
    int i;
    WP *b = head;
    for(i = 0; i < NR_WP; i++){
      if(b->next == wp){
        b->next = wp->next;
        wp->next = free_;
        free_ = wp;
        break;
      }
      else b = b->next;
    }
    return head;
  }
}

