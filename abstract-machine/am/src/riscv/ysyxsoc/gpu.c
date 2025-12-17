#include <am.h>
#include "../riscv.h"
#include "include/ysyxsoc.h"

static uint32_t Width = 640;
static uint32_t Height = 480;

void __am_gpu_init() {
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  for (int i = 0; i < Width * Height; i ++){
    fb[i] = 0;
  } 
  outl(SYNC_ADDR, 1);
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, 
    .has_accel = false,
    .width = Width, 
    .height = Height,
    .vmemsz = Width * Height * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t x = ctl->x, y = ctl->y;
  uint32_t w = ctl->w, h = ctl->h;
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }else{
    outl(SYNC_ADDR, 0);
  }
  if(w==0 || h==0) return;
  uint32_t *pixels = (uint32_t *)ctl->pixels;
  uint32_t *fb = (uint32_t *)FB_ADDR;
  for (int i = 0; i < h; i ++){
    for(int j = 0; j < w; j++){
      fb[(i+y)*Width + x + j] = pixels[i*w + j];
    }
  }
 
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}