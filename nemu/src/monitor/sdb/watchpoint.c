/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "sdb.h"
#define NR_WP 32
#include "watchpoint.h"
/*
typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  bool used;
  char expr[105];
  int new_value;
  int old_value;
  TODO: Add more members if necessary 

} WP;


static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;
*/
WP wp_pool[NR_WP] = {};
void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */
/*WP* new_wp(){
	for(WP* p =free_;p->next!=NULL;p = p->next){
		if(p->used ==false){
			p->used=true;
			if(head==NULL){
				head = p;
			}
		return p;
		}
	}
	printf("no watchpoint\n");
	assert(0);
	return NULL;
}

void free_wp(WP *wp){
	if(head->NO ==wp ->NO){
		head->used = false;
		head = NULL;
		printf("Delete watchpoint.\n");
		return ;
	}
	for(WP* p = head;p->next !=NULL; p =p->next){
		if(p->next->NO == wp->NO){
			p->next = p->next->next;
			p -> next ->used =false;
			return;
		}
	}
}*/


void add_watchpoint(char *str) {
    assert(free_ != NULL && "No free memory for new watchpoint");

    bool success = false;
    int value = expr(str, &success);
    if (!success) {
        printf("Error: Invalid expression '%s'\n", str);
        return;
    }

    WP *ptr = free_;
    free_ = free_->next;

    strncpy(ptr->str, str, 31);
    ptr->str[600] = '\0';
    ptr->old_value = value;

    ptr->next = head;
    head = ptr;

    printf("Watchpoint %d: %s\n", ptr->NO, ptr->str);
}

void delete_watchpoint(int no) {
    if (head == NULL) {
        printf("Error: No watchpoints to delete.\n");
        return;
    }

    if (head->NO == no) {
        WP *tmp = head;
        head = head->next;
        tmp->next = free_;
        free_ = tmp;
        printf("Deleted watchpoint %d.\n", no);
        return;
    }

    WP *ptr = head;
    while (ptr->next) {
        if (ptr->next->NO == no) {
            WP *tmp = ptr->next;
            ptr->next = tmp->next;
            tmp->next = free_;
            free_ = tmp;
            printf("Deleted watchpoint %d.\n", no);
            return;
        }
        ptr = ptr->next;
    }

    printf("Error: Watchpoint %d not found.\n", no);
}

void print_watchpoints() {
    if (head == NULL) {
        printf("No watchpoints.\n");
        return;
    }

    printf("Num\t\tWhat\n");
    WP *ptr = head;
    while (ptr) {
        printf("%d\t\t%s\n", ptr->NO, ptr->str);
        ptr = ptr->next;
    }
}

    
int update_watchpoint() {
    int n_changed = 0;
    WP *ptr = head;
    while (ptr != NULL) {
        bool success = false;
        word_t value = expr(ptr->str, &success);
        if (!success) {
            printf("Error: Invalid expression '%s' in watchpoint %d\n", ptr->str, ptr->NO);
            ptr = ptr->next;
            continue;
        }
        if (value != ptr->old_value) {
            n_changed += 1;
            printf("Watchpoint %d: %s\n", ptr->NO, ptr->str);
            printf("Old value = 0x%08x(%d)\n", ptr->old_value, ptr->old_value);
            printf("New value = 0x%08x(%d)\n", value, value);
            ptr->old_value = value;
        }
        ptr = ptr->next;
    }
    return n_changed;
}
