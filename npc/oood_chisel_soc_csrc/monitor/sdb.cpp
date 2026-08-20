#include <readline/readline.h>
#include <readline/history.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <assert.h>
#include "../include/sdb.h"
#include "../include/cpu.h"
#include "../include/state.h"
#include "../include/regs.h"
#include "../include/memory.h"
#include "../../include/generated/autoconf.h"

// calculate the length of an array
#define ARRLEN(arr) (int)(sizeof(arr) / sizeof(arr[0]))

#ifdef CONFIG_BATCH_MODE
    static int is_batch_mode = true;
#else
    static int is_batch_mode = false;
#endif

static int wp_number = 0;
WP *HEAD = NULL;

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}

static int cmd_q(char *args) {
  npc_state.state = NPC_QUIT;
  return -1;
}

static int cmd_help(char *args);

static int cmd_si(char *args) {
  int steps;
  if(args == NULL){ steps = 1; }
  else sscanf(args,"%d",&steps);
  cpu_exec(steps);
  return 0;
}

static int cmd_info(char *args){
  if(strcmp("r", args) == 0){
    print_register_values();
  }
  else if(strcmp("w", args) == 0){
    int i;
    WP *b = HEAD;
    for(i = 0; i < NR_WP; i++){
      if(b == NULL){
        break;
      }
      else{
        printf("监视点%d\t%s\n", b->NO, b->experence);
        b = b->next;
      }
    }
  }
  else printf("Unknown option \"%s\"\n",args);
  return 0;
}

static int cmd_x(char *args){
  char *N = strtok(NULL," ");  
  char *EXPR = strtok(NULL," "); 
  int n;
  uint32_t address;
  sscanf(N,"%d",&n);
  sscanf(EXPR, "%x", &address);  
  int len = 4;
  int i;
  for(i = 0;i < n;i++){
    uint64_t data ;
    data = pmem_read(address, len);
    printf("0x%016lx\n", data);  
    address = address + 4;
  }
  return 0;  
}

static int cmd_p(char *args){
  if(expr(args) == 0 ){
    extern int nr_token;
    int num = eval(0, nr_token - 1);
    printf("%s = %d\n", args, num);
  }
  return 0;
}

static int cmd_w(char *args){
  HEAD = new_wp(args);
  HEAD->NO = wp_number + 1;
  wp_number++;
  return 0;
}

static int cmd_d(char *args){
  int N;
  int i;
  sscanf(args,"%d",&N);
  for(i = 0; i < NR_WP; i++){
    if(HEAD == NULL){
      assert(0);
    }
    else if(HEAD->NO == N){
      break;
    }
    else{
      HEAD = HEAD->next;
    }
  }
  HEAD = free_wp(HEAD);
  return 0;
}

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display informations about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Single step execution 单步执行", cmd_si },
  { "info", "Print program status 打印程序状态", cmd_info },
  { "x", "Scan memory 扫描内存", cmd_x},
  { "p", "表达式求值", cmd_p },
  { "w", "set watchpoint", cmd_w},
  { "d", "delte watchpoint", cmd_d},

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(npc) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }


    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}
