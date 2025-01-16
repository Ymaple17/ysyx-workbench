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

#include <isa.h>
#include <memory/paddr.h>
/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256,TK_HEX_NUM,TK_REG,TK_EQ,TK_NUM,TK_NEQ, TK_MINUS,TK_AND,TK_DEREF

  /* TODO: Add more token types */
};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {"0x[0-9a-zA-Z]+", TK_HEX_NUM},
  {"\\$[0-0a-z]+", TK_REG},
  {" +", TK_NOTYPE},    
  {"\\+", '+'},         
  {"==", TK_EQ},   
  {"!=", TK_NEQ},
  {"[0-9]+", TK_NUM},	
  {"-", '-'},	
  {"\\*", '*'},	
  {"/", '/'}, 	
  {"[(]", '('},
  {"[)]", ')'},
  {"$$", '$'},
  {"&&", TK_AND}
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
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[24];
} Token;

static Token tokens[4096] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
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

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */
        switch (rules[i].token_type) {
          case TK_NOTYPE: break;
          default: 
          	strncpy(tokens[nr_token].str, substr_start, substr_len);
		tokens[nr_token].type = rules[i].token_type;
		nr_token++;
		break;
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

bool check_parentheses(int p,int q){
    if(tokens[p].type != '('||tokens[q].type!=')') return false;
    int num=1;
    for(int i=p+1;i<=q-1;i++){
	if(tokens[i].type=='(') num++;
	else if(tokens[i].type==')') num--;
	if(num<1) return false;
    }
    if(num==1) return true;
    else return false;
    }
    
int get_position(int p,int q){
	int num=0;
	int re=0;
	bool add_and_sub =false;
	bool mul_and_div =false;
	bool minus= false;
	bool pointer = false;
	bool neq=false;
	bool add=false;
	for(int i=p;i<=q;i++){
		if(tokens[i].type=='(') num++;
		if(tokens[i].type==')') num--;
		if(num!=0) continue;
		if(tokens[i].type==TK_EQ) return i;
		if(tokens[i].type=='+'||tokens[i].type=='-'){
			re=i;
			add_and_sub=true;
			continue;
		}
		if((tokens[i].type=='*'||tokens[i].type=='/')&&!add_and_sub){
			re=i;
			mul_and_div=true;
			continue;
		}
		if(tokens[i].type== TK_MINUS&&!add_and_sub&&!mul_and_div&&!minus){
			re=i;
			minus =true;
	}
		if(tokens[i].type==TK_DEREF&&!add_and_sub&&!mul_and_div&&!pointer){
			re=i;
			pointer=true;
	}
		if(tokens[i].type==TK_NEQ&&!add_and_sub&&!mul_and_div&&!neq){
			re=i;
			neq=true;
	}
		if(tokens[i].type==TK_AND&&!add_and_sub&&!mul_and_div&&!add){
			re=i;
			add=true;
	}
      }
	return re;
}		
			 
	
    
uint32_t eval(int p,int q){
	if(p>q) return 0;
	else if(p==q){
	uint32_t num = 0;
	if(tokens[p].type==TK_NUM) sscanf(tokens[p].str,"%d",&num);
	if(tokens[p].type==TK_HEX_NUM) sscanf(tokens[p].str,"%x",&num);
	if(tokens[p].type==TK_REG ){
	   bool success=false;
	   num = isa_reg_str2val(tokens[p].str, &success);
	   if(!success) {
		printf("The register name is incorrect.\n");
		return 0;
	   }
	}
	return num;
       }
       else if(check_parentheses(p,q) == true){
	  return eval(p + 1, q - 1);
       }
       else {
          int op = get_position(p,q);
          uint32_t val1=0;
          if(tokens[op].type != TK_DEREF&&tokens[op].type != TK_MINUS)
          	val1 = eval(p, op - 1);
          uint32_t val2 = eval(op + 1, q);

       switch (tokens[op].type) {
          case '+': return val1 + val2;
          case '-': return val1 - val2;
          case '*': return val1 * val2;
          case '/': return val1 / val2;
          case TK_EQ: return val1==val2 ?1 :0;
          case TK_AND: return val1&&val2 ?1 :0;
          case TK_NEQ: return val1!=val2 ?1 :0;
          case TK_MINUS: return -1*val2;
          case TK_DEREF: return paddr_read(val2,4);
          default: assert(0);
       }
     }
}	
	
word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  //TODO();
  for (int i = 0; i < nr_token; i ++) {
  if (tokens[i].type == '*' && (i == 0 || (tokens[i - 1].type !=TK_NUM&&tokens[i - 1].type !=')' ) )) {
    tokens[i].type = TK_DEREF;
  }
  if (tokens[i].type == '-' && (i == 0 ||( tokens[i - 1].type !=TK_NUM &&tokens[i - 1].type !=')')) ) {
    tokens[i].type = TK_MINUS;
  } 
}
  return eval(0,nr_token-1);
}
