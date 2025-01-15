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

  /* Start engine. */
  engine_start();
  test_cmd_p();
  return is_exit_status_bad();
}

void test_cmd_p(){
	bool success =false;
	char buf[2048];
	FILE* fp = fopen("./tools/gen-expr/input.txt","r");
	if(!fp){
		perror("read failed");
		exit(1);
	}
	while(fgets(buf,2048,fp)!=NULL){
		buf[strlen(buf)-1] ='\0';
		char *res_str = strtok(buf," ");
		char *exp = strtok(NULL," ");
		uint32_t res =0;
		sscanf(res_str,"%d",&res);
		if(res == expr(exp, &success)) printf("match");
		else printf("Not match");
		}
	fclose(fp);
}
		
