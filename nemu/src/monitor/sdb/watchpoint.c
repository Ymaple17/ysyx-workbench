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
WP* new_wp(){
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
}

bool check_all() {
    bool check = false;
    bool success = false;
    uint32_t temp = 0;

    for (int i = 0; i < NR_WP; i++) {
        if (wp_pool[i].expr[0] != '\0') {
            temp = expr(wp_pool[i].expr, &success);

            if (!success) {
                printf("Error evaluating expression for watchpoint %d: %s\n", wp_pool[i].NO, wp_pool[i].expr);
                continue;
            }

            if (temp != wp_pool[i].old_value) {
                printf("Watchpoint %d: %s\n", wp_pool[i].NO, wp_pool[i].expr);
                printf("old value: %d\n", wp_pool[i].old_value);
                printf("new value: %d\n\n", temp);
                wp_pool[i].old_value = temp; 
                check = true; 
            }
        }
    }

    return check;
}

void delete_watchpoint(int no){
    for(int i = 0 ; i < NR_WP ; i ++)
        if(wp_pool[i].NO == no){
            free_wp(&wp_pool[i]);
            return ;
        }
}
void create_watchpoint(char* args){
    WP* p =  new_wp();
    strcpy(p -> expr, args);
    bool success = false;
    int temp = expr(p -> expr,&success);
   if(success) p -> old_value = temp;
   else 
    printf("Create watchpoint No.%d success.\n", (p -> NO));
}

void sdb_watchpoint_display(){
    bool flag = true;
    for(int i = 0 ; i < NR_WP ; i ++){
        if(wp_pool[i].used){
            printf("Watchpoint.No: %d, expr = \"%s\", old_value = %d, new_value = %d\n",
                    wp_pool[i].NO, wp_pool[i].expr,wp_pool[i].old_value, wp_pool[i].new_value);
                flag = false;
        }
    }
    if(flag) printf("No watchpoint.");
    }
