#ifndef WATCHPOINT_H
#define WATCHPOINT_H

#include <stdbool.h>

// 定义最大观察点数
#define NR_WP 32

// 观察点结构体
typedef struct watchpoint {
    int NO;             // 观察点编号
    struct watchpoint *next; // 链表指针，指向下一个观察点
    bool flag;          // 标志位，表示该观察点是否被激活
    char expr[105];     // 表达式，用于存储观察点的条件表达式
    int new_value;      // 表达式的当前值
    int old_value;      // 表达式的旧值
} WP;

// 声明全局变量
extern WP wp_pool[NR_WP];   // 观察点池
extern WP *head;             // 激活的观察点链表头
extern WP *free_;            // 可用的观察点链表头

// 初始化观察点池
void init_wp_pool(void);

// 创建一个新的观察点
WP* new_wp(void);

// 释放一个观察点
void free_wp(WP *wp);

// 显示所有观察点的状态
void sdb_watchpoint_display(void);

// 删除指定编号的观察点
void delete_watchpoint(int no);

// 创建一个观察点，并初始化表达式
void create_watchpoint(char* args);

int expr(char *e, bool *success);
#endif  // WATCHPOINT_H

