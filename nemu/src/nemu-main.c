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

#include <common.h>

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();
word_t expr(char *e, bool *success);
void test_cmd_p();
int main(int argc, char *argv[]) {
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif
  test_cmd_p();
  /* Start engine. */
  engine_start();
  
  return is_exit_status_bad();
}

void test_cmd_p() {
    bool success = false;
    char buf[2048];
    const char *file_path = "/home/qiu/ysyx-workbench/nemu/tools/gen-expr/input";
    FILE *fp = fopen(file_path, "r");
    if (!fp) {
        perror("Failed to open file");
        exit(1);
    }
    while (fgets(buf, sizeof(buf), fp) != NULL) {
        size_t len = strlen(buf);
        if (len > 0 && buf[len - 1] == '\n') {
            buf[len - 1] = '\0';
        }
        char *res_str = strtok(buf, " ");
        char *exp = strtok(NULL, "");

        if (res_str == NULL || exp == NULL) {
            fprintf(stderr, "Invalid input format: %s\n", buf);
            continue;
        }
        uint32_t res = 0;
        if (sscanf(res_str, "%u", &res) != 1) {
            fprintf(stderr, "Failed to parse result: %s\n", res_str);
            continue;
        }
        uint32_t expr_result = expr(exp, &success);
        if (!success) {
            fprintf(stderr, "Expression evaluation failed: %s\n", exp);
            continue;
        }
        if (res == expr_result) {
            printf("Match: %s -> %u\n", exp, res);
        } else {
            printf("Not match: %s -> expected %u, got %u\n", exp, res, expr_result);
        }
    }
    fclose(fp);
}
