#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>
#include <dlfcn.h>
#include "verilated_vcd_c.h" 
#include "VysyxSoCFull.h"
#include "svdpi.h"
#include "../include/common.h"
#include "../include/state.h"
#include "../include/difftest.h"
#include "../include/init.h"
#include "../include/trace.h"
#include "../../include/generated/autoconf.h"


#define memory_size 128*1024*1024
#define flash_size 0x01000000
extern const char* img_path;
word_t img_size = 0;

void init_map();
void init_serial();
void init_timer();
void init_vga();
void init_i8042();

void difftest_init(char* ref_so_file, word_t img_size);


uint32_t img[memory_size/4] = {};

uint32_t flash_img[flash_size/4] = {};

long long  getFileSize(FILE *fp){
  long long size;
  fpos_t pos;
  fgetpos(fp,&pos);
  fseek(fp,0,SEEK_END);
  size = ftell(fp);
  fsetpos(fp,&pos);
  return size;
}

void load_img(){
  FILE *p = fopen(img_path, "rb");
  if (p == NULL) {
    fprintf(stderr, "Error: cannot open image file %s\n", img_path);
    assert(0);
  }
  img_size = getFileSize(p);
  printf("img_size = %d\n", (int)img_size);

  if(fread(flash_img, img_size, 1, p) != 1){
    assert(0);
  }
  printf("Load image to flash_img (SOC Mode)\n");

  fclose(p);
}

void init_mem(){   
  load_img();
  extern uint8_t flash[flash_size];

  memcpy(flash, flash_img, img_size);
  printf("Copy image from flash_img to flash (SOC Mode)\n");
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();

#ifdef CONFIG_MTRACE
  init_mtrace();
#endif
}


void init_device() {
  init_map();
  init_serial();
  init_timer();
  init_vga();
  init_i8042();
}
