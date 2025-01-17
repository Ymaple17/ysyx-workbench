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

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum {
  TK_NOTYPE = 256, TK_EQ,

  /* TODO: Add more token types */
  TK_UINT,
	TK_HEX,

	TK_NE,
	TK_AND,

	TK_REG,
	TK_DEREF,
	TK_NEG,
};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces
  {"\\+", '+'},         // plus
  {"==", TK_EQ},        // equal
												
	{"-", '-'},
	{"\\*", '*'},
	{"/", '/'},
	{"\\(", '('},
	{"\\)", ')'},
	{"0x[0-9AaBbCcDdEeFf]+", TK_HEX},
	{"[0-9]+", TK_UINT},

	{"!=", TK_NE},
	{"&&", TK_AND},
	{"\\$(\\$0|ra|sp|gp|tp|t[0-6]|s[0-9]|s10|s11|a[0-7])", TK_REG},
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
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
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
		case TK_HEX:
		case TK_UINT:
		case TK_REG:
			Assert(substr_len < 32, "token should less than 32 characters");
			strncpy(tokens[nr_token].str, substr_start, substr_len);
			tokens[nr_token].str[substr_len] = '\0';
		case '+':
		case '-':
		case '*':
		case '/':
		case '(':
		case ')':
		case TK_EQ:
		case TK_NE:
		case TK_AND:
			Assert(nr_token < 32, "token should less than 32");
			tokens[nr_token].type = rules[i].token_type;
			int current_token = rules[i].token_type;
			if (current_token  == '*' || current_token == '-') {
			int tk = nr_token == 0 ? -1 : tokens[nr_token - 1].type;
			if (nr_token == 0 || tk == '+' || tk == '-' || tk == '*' || tk == '/' || tk == '(' || tk == TK_EQ || tk == TK_NE || tk == TK_AND) {
			tokens[nr_token].type = current_token == '*' ? TK_DEREF : TK_NEG;
		}
	}
			nr_token++;
			break;
			default: Assert(false, "unknow token type %d", rules[i].token_type);
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


static bool is_paired(int p, int q) {
	if (tokens[p].type != '(' && tokens[q].type != ')') {
		return false;
	}
	int n_left = 0;
	for (int i = p+1; i <= q-1; i++) {
		if (tokens[i].type == '(') {
			n_left++;
		}
		else if (tokens[i].type == ')') {
			n_left--;
			if (n_left < 0) {
				return false;
			}
		}
	}
	return n_left == 0;
}
static int priority(int operator) {
	switch (operator) {
		case TK_AND:
			return 0;
		case TK_EQ:
		case TK_NE:
			return 1;
		case '+':
		case '-':
			return 2;
		case '*':
		case '/':
			return 3;
		case TK_DEREF:
		case TK_NEG:
			return 4;
		default:
			assert(0);
	}
}
static int find_main_operator_index(int p, int q) {
	int main_operator_index = -1;
	int main_operator = -1;
	int n_left = 0;
	for (int i = p; i <= q; i++) {
		int operator = tokens[i].type;
		switch (operator) {
			case '(': 
				n_left++;
				break;
			case ')':
				n_left--;
				break;
			case '+':
			case '-':
			case '*':
			case '/':
			case TK_EQ:
			case TK_NE:
			case TK_AND:
			case TK_DEREF:
			case TK_NEG:
				if (n_left == 0 && (main_operator_index == -1 || priority(operator) <= priority(main_operator))) {
					main_operator_index = i;
					main_operator = operator;
				}
				break;
			default:break;
		}
	}
	return main_operator_index;
}

word_t vaddr_read(vaddr_t, int);

word_t eval_expr(int p, int q, bool *success) {
	if (p > q) {
		*success = false;
		return 0;
	}
	else if (p == q) {
		*success = true;
		word_t result = 0;
		switch (tokens[p].type) {
			case TK_HEX:
				sscanf(tokens[p].str, "%x", &result);
				return result;
			case TK_UINT:
				sscanf(tokens[p].str, "%d", &result);
				return result;
			case TK_REG:
				return isa_reg_str2val(tokens[p].str + 1, success);
			default:
				Assert(false, "error token type %d", tokens[p].type);
		}
	}
	else if (is_paired(p, q)) {
		return eval_expr(p+1, q-1, success);
	}
	*success = true;
	int r = find_main_operator_index(p, q);
	if (r < 0) {
		printf("can't find main operator\n");
		*success = false;
		return 0;
	}

	word_t value_right = eval_expr(r+1, q, success);
	if (*success == false) {
		return 0;
	}

	if (tokens[r].type == TK_DEREF) {
		return vaddr_read(value_right, 4);
	}

	if (tokens[r].type == TK_NEG) {
		return -value_right;
	}

	word_t value_left = eval_expr(p, r-1, success);
	if (*success == false) {
		return 0;
	}

	switch (tokens[r].type) {
		case '+': return value_left + value_right;
		case '-': return value_left - value_right;
		case '*': return value_left * value_right;
		case '/': return value_left / value_right;
		case TK_EQ: return value_left == value_right;
		case TK_NE: return value_left != value_right;
		case TK_AND: return value_left && value_right;
		default: assert(0);
	}
}


word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  return eval_expr(0, nr_token-1, success);
}
