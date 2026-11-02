#include <regex.h>
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <assert.h>
#include "../include/regs.h"
#include "../include/memory.h"
#include "../../include/generated/autoconf.h"

#define ARRLEN(arr) (int)(sizeof(arr) / sizeof(arr[0]))

enum {
  TK_NOTYPE = 256, TK_EQ, TK_NUMBER, TK_HEX, TK_REG ,TK_NEQ,TK_AND, TK_POINT

  
};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  {" +", TK_NOTYPE},            // spaces
  {"\\+", '+'},                 // plus
  {"==", TK_EQ},                // equal
  {"-", '-'},                   // minus 
  {"\\*", '*'},                 // times or point
  {"\\/", '/'},                 // divided
  {"\\(", '('},                 // (
  {"\\)", ')'},                 // )
  {"\\b[0-9]+\\b", TK_NUMBER},  // decimal-number
  {"\\b0x[0-9A-Z]+\\b", TK_HEX},// hexadecimal-number
  {"\\$[a-z0-9]+\\b", TK_REG},  // reg
  {"!=", TK_NEQ},               // not equal
  {"&{2}", TK_AND},             // and

};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      printf("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
      assert(0);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
int nr_token __attribute__((used))  = 0;

bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        printf("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        switch (rules[i].token_type) {
          case TK_NOTYPE: break;
          case '+': tokens[nr_token].type = '+'; nr_token++; break;
          case TK_EQ: tokens[nr_token].type = TK_EQ; nr_token++; break;
          case '-': tokens[nr_token].type = '-'; nr_token++; break;
          case '*': tokens[nr_token].type = '*'; nr_token++; break;
          case '/': tokens[nr_token].type = '/'; nr_token++; break;
          case '(': tokens[nr_token].type = '('; nr_token++; break;
          case ')': tokens[nr_token].type = ')'; nr_token++; break;
          case TK_NUMBER: tokens[nr_token].type = TK_NUMBER; strncpy(tokens[nr_token].str, substr_start, substr_len);
                          tokens[nr_token].str[substr_len] = '\0'; nr_token++; break;   
          case TK_HEX: tokens[nr_token].type = TK_HEX; strncpy(tokens[nr_token].str, substr_start, substr_len);
                       tokens[nr_token].str[substr_len] = '\0'; nr_token++; break;
          case TK_REG: tokens[nr_token].type = TK_REG; strncpy(tokens[nr_token].str, substr_start, substr_len);
                       tokens[nr_token].str[substr_len] = '\0'; nr_token++; break;
          case TK_NEQ: tokens[nr_token].type = TK_NEQ; nr_token++; break;
          case TK_AND: tokens[nr_token].type = TK_AND; nr_token++; break;     
          default: printf("position = %d", position); assert(0);
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }
  return true;
}


uint64_t expr(char *e) {
  if (!make_token(e)) {
    return 0;
  }
  else{
    int i;
    for (i = 0; i < nr_token; i ++) {
      if (tokens[i].type == '*' && (i == 0 || tokens[i - 1].type == '(' || tokens[i - 1].type == '+' || tokens[i - 1].type == '-' || tokens[i - 1].type == '*' || tokens[i - 1].type == '/' || tokens[i - 1].type == TK_AND || tokens[i - 1].type == TK_EQ || tokens[i - 1].type == TK_NEQ ) ) {
        tokens[i].type = TK_POINT;
      }
    }
    return 0;
  }
}

struct OP{
  int po;
  int type;
}; //主运算符possision and type

int check_parentheses(int p, int q);
struct OP search_main_operator(int p, int q);

uint64_t eval(int p, int q) {
  if (p > q) {
    assert(0);
  }
  else if (p == q) {
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */
    int len = strlen(tokens[p].str);
    uint64_t num = 0;
    int i;

    if(tokens[p].type == TK_NUMBER){
      for(i = 0 ;i < len; i++){
      num *= 10;
      num += tokens[p].str[i] - '0';
      }
      return num; 
    }
    else if(tokens[p].type == TK_HEX){
      for(i = 2 ;i < len; i++){
          num *= 16;
        if(tokens[p].str[i] >= 'A')
          num += tokens[p].str[i] - 'A' + 10;
        else 
          num += tokens[p].str[i] - '0';
      }
      return num;
    }
    else if(tokens[p].type == TK_REG){
      bool a = false;
      bool *success = &a;
      num = isa_reg_str2val(tokens[p].str + 1, success);
      return num;
    }
    else{
      assert(0);
    } 
  }
  else if (check_parentheses(p, q) == true) {
    /* The expression is surrounded by a matched pair of parentheses.
     * If that is the case, just throw away the parentheses.
     */
    return eval(p + 1, q - 1);
  }
  else {
    struct OP op = search_main_operator(p, q);
    if(op.type == TK_POINT){
      return pmem_read(eval(op.po + 1, q), 1);
    }
    else{
      uint64_t val1 = eval(p, op.po - 1);
      uint64_t val2 = eval(op.po + 1, q);

      switch (op.type) {
        case '+': return (val1 + val2);
        case '-': return (val1 - val2);
        case '*': return (val1 * val2);
        case '/': return (val1 / val2);
        case TK_AND: return (val1 && val2);
        case TK_EQ: return (val1 == val2);
        case TK_NEQ: return (val1 != val2);
        default: assert(0);
      }
    }
  }
}

int check_parentheses(int p, int q){
  int i;
  int j = 0;
  int k = 0;
  if(tokens[p].type == '(' && tokens[q].type == ')'){
    for (i = p; i <= q ; i++){
      if(tokens[i].type == '(')
        j++;
      if(tokens[i].type == ')')
        k++;
      if(j < k){
        assert(0);
      }
      if(j == k && i != q )
        return false;
      if(j == k && i == q)
        return true;
    }
    return false;
  }
  else return false;
}

struct OP search_main_operator(int p, int q){
  struct OP op;
  int i;
  int j = 0;   //标志括号出现的次数，'('加一，')'减一
  int k = 0;   //标志括 && 出现的次数
  int s = 0;   //标志 == 和 != 出现的次数
  int m = 0;   //标志 + 和 - 出现的次数
  int n = 0;   //标志 * 和 / 出现的次数

  for(i = p; i <= q; i++){
    if(tokens[i].type == '(')
      j++;
    if(tokens[i].type == ')')
      j--;
    if(tokens[i].type == TK_AND && j == 0){
      op.po = i; 
      op.type = TK_AND;
      k++;
    }
    if(tokens[i].type == TK_EQ  && j == 0 && k == 0){
      op.po = i; 
      op.type = TK_EQ;
      s++;
    }
    if(tokens[i].type == TK_NEQ && j == 0 && k == 0){
      op.po = i; 
      op.type = TK_NEQ;
      s++;
    }
    if(tokens[i].type == '+' && j == 0 && k == 0 && s == 0){
      op.po = i; 
      op.type = '+';
      m++;
    }
    if(tokens[i].type == '-' && j == 0 && k == 0 && s == 0){
      op.po = i; 
      op.type = '-';
      m++;
    }
    if(tokens[i].type == '*' && j == 0 && k == 0 && s == 0 && m == 0){
      op.po = i; 
      op.type = '*';
      n++;
    }
    if(tokens[i].type == '/' && j == 0 && k == 0 && s == 0 && m == 0){
      op.po = i; 
      op.type = '/';
      n++;
    }
    if(tokens[i].type == TK_POINT && j == 0 && k == 0 && s == 0 && m == 0 && n == 0){
      op.po = i; 
      op.type = TK_POINT;
    }
  }
  return op;
}



