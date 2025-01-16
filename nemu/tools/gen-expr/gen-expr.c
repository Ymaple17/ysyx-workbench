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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>


static char buf[65536];
int count;

uint32_t choose(uint32_t n) {
    return rand() % n;
}

bool is_previous_operator_division() {
    int len = strlen(buf);
    if (len == 0) {
        return false;
    }
    for (int i = len - 1; i >= 0; i--) {
        if (!isspace(buf[i])) {
            return buf[i] == '/';
        }
    }
    return false;
}

static inline void gen_num() {
    char s[4];
    uint32_t n;
    bool is_division = is_previous_operator_division();
    do {
        n = choose(100);
        sprintf(s, "%u", n);
    } while (is_division && n == 0);
    strcat(buf, s);
    count++;
}
static inline void gen(char str) {
    uint32_t left = choose(4);
    uint32_t right = choose(4); 
    char s[left + 1 + right + 1];
    uint32_t i;
    for (i = 0; i < left; i++) { s[i] = ' '; count++; }
    s[i++] = str;
    for (; i < left + 1 + right; i++) { s[i] = ' '; count++; }
    s[left + 1 + right] = '\0';
    strcat(buf, s);
}
static inline void gen_rand_op() {
    switch (choose(4)) {
        case 0: gen('+'); break;
        case 1: gen('-'); break;
        case 2: gen('*'); break;
        case 3: gen('/'); break;
    }
    count++;
}
static inline void gen_rand_expr() {
    int a = choose(3);
    if (count > 600) a = 0;
    switch (a) {
        case 0: gen_num(); break;
        case 1: gen('('); gen_rand_expr(); gen(')'); break;
        default: gen_rand_expr(); gen_rand_op(); gen_rand_expr(); break;
    }
}
static char code_buf[65536];
static char *code_format =
    "#include <stdio.h>\n"
    "int main() { "
    "_Bool flag;"
    "  unsigned result = %s; "
    "  printf(\"%%u\", result); "
    "  return 0; "
    "}";

int main(int argc, char *argv[]) {
    int seed = time(0);
    srand(seed);
    int loop = 1;
    if (argc > 1) {
        sscanf(argv[1], "%d", &loop);
    }
    int i;
    for (i = 0; i < loop; i++) {
        count = 0;
        memset(buf, '\0', sizeof(buf));
        gen_rand_expr();
        bool has_division_by_zero = false;
        char *div_pos = strstr(buf, "/ 0");
        if (div_pos != NULL) {
            has_division_by_zero = true;
        }
        if (has_division_by_zero) {
            i--; 
            continue;
        }
        sprintf(code_buf, code_format, buf);

        FILE *fp = fopen("/tmp/.code.c", "w");
        assert(fp != NULL);
        fputs(code_buf, fp);
        fclose(fp);
        int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
        if (ret != 0) continue;
        fp = popen("/tmp/.expr", "r");
        assert(fp != NULL);
        int result;
        int fsn = fscanf(fp, "%d", &result);
        pclose(fp);
        if (fsn == -1) {
            continue;
        }
        printf("%u\t %s\n", result, buf);
    }
    return 0;
}
