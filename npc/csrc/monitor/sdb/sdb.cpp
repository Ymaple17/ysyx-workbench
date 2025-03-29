#include <readline/readline.h>
#include <readline/history.h>
#include <utils.h>
#include <cpu/cpu.h>
#include <mem/paddr.h>

#define NR_CMD ARRLEN(cmd_table)

// expr
void init_regex();
word_t expr(char *e, bool *success);
// watchpoint
void init_wp_pool();
void wp_iterate();
void wp_watch(char*, word_t);
void wp_remove(int);


static int cmd_help(char*);
static int cmd_c(char*);
static int cmd_q(char*);
static int cmd_si(char*);
static int cmd_info(char*);
static int cmd_x(char*);
static int cmd_p(char*);
static int cmd_w(char*);
// static int cmd_d(char*);

static int is_batch_mode = false;

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Usage: si [N]. Continue the execution in N steps, default 1", cmd_si },
  { "info", "Usage: info (r [reg] | w). Display the info of registers & watchpoints", cmd_info },
  { "x", "Usage: x N ADDR. Scan the memory in ADDR by N 4bytes", cmd_x },
  { "p", "Usage: p EXPR. Calculate the expression, e.g. p $eax + 1", cmd_p },
  { "w", "Usage: w EXPR. Watch for the variation of the result of EXPR, pause at variation point", cmd_w },
  // { "d", "Usage: d N. Delete watchpoint of wp.NO=N", cmd_d },
};

static int cmd_help(char* args) {
  /* extract the first argument */
  char *arg = strtok(nullptr, " ");
  int i;

  if (arg == nullptr) {
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

static int cmd_si(char* args) {
  char *arg = strtok(nullptr, " ");
  int n;

  if (arg == nullptr) {
    n = 1;
  } else {
    n = strtol(arg, nullptr, 10);
  }
  cpu_exec(n);
  return 0;
}

static int cmd_c(char* args) {
  if (args == nullptr) {
    cpu_exec(-1);
    return 0;
  }
  char *arg = strtok(nullptr, " ");
  int n;

  if (arg == nullptr) {
    n = 1;
  } else {
    n = strtol(arg, nullptr, 10);
  }
  while (n-- > 0) {
    cpu_exec(-1);
  }
  
  return 0;
}

static int cmd_q(char* args) {
  return -1;
}

static int cmd_info(char*) {
  char *arg = strtok(nullptr, " ");
  if (arg == nullptr) {
    printf("Usage: info r (registers) or info w (watchpoints)\n");
  } else {
    if (strcmp(arg, "r") == 0) {
      dump_gpr();
    } else if (strcmp(arg, "w") == 0) {
      // wp_iterate();
    } else {
      printf("Usage: info r (registers) or info w (watchpoints)\n");
    }
  }
  
  return 0;
}

static int cmd_x(char*) {
  char *arg1 = strtok(nullptr, " ");
  if (arg1 == nullptr) {
    printf("Usage: x N ADDR\n");
    return 0;
  }
  char *arg2 = strtok(nullptr, " ");
  if (arg1 == nullptr) {
    printf("Usage: x N ADDR\n");
    return 0;
  }

  int n = strtol(arg1, nullptr, 10);
  paddr_t addr = strtol(arg2, nullptr, 16);

  int i, j;
  for (i = 0; i < n;) {
    printf(ANSI_FMT(FMT_PADDR ": ", ANSI_FG_CYAN), addr);
    
    for (j = 0; i < n && j < 4; i++, j++) {
      word_t w = paddr_read_c(addr, 4);
      addr += 4;
      printf(FMT_WORD " ", w);
    }
    puts("");
  }
  
  return 0;
}

static int cmd_p(char* args) {
  bool success;
  word_t res = expr(args, &success);
  if (!success) {
    puts("invalid expression");
  } else {
    printf("dec=%u hex="  FMT_WORD "\n", res, res);
  }
  return 0;
}

static int cmd_w(char* args) {
  if (!args) {
    printf("Usage: w EXPR\n");
    return 0;
  }
  bool success;
  word_t res = expr(args, &success);
  if (!success) {
    puts("invalid expression");
  } else {
    wp_watch(args, res);
  }
  return 0;
}

static char* rl_gets() {
  static char *line_read = nullptr;

  if (line_read) {
    free(line_read);
    line_read = nullptr;
  }

  line_read = readline("(npc) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  // reset cpu pc
  cpu_reset(2);

  if (is_batch_mode) {
    cmd_c(nullptr);
    return;
  }

  for (char *str; (str = rl_gets()) != nullptr; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == nullptr) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = nullptr;
    }

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        int ret = cmd_table[i].handler(args) < 0;
        if (ret != 0) {
          if (strcmp(cmd, "q") == 0) {
            npc_state.state = NPC_QUIT;
          }
          return;
        }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  init_regex();

  init_wp_pool();
}
