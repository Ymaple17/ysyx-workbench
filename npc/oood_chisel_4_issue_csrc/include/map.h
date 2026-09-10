#ifndef __MAP_H__
#define __MAP_H__

#include "common.h"
#include <stdint.h>
#include <stdbool.h>

// 先定义回调类型
typedef void (*io_callback_t)(uint32_t addr, int len, bool is_write);
uint8_t* new_space(int size);

typedef struct {
  const char *name;
  uint32_t low;
  uint32_t high;
  void *space;
  io_callback_t callback;
} IOMap;

// 内联函数定义
static inline bool map_inside(IOMap *map, uint32_t addr) {
  return (addr >= map->low && addr <= map->high);
}

static inline int find_mapid_by_addr(IOMap *maps, int size, uint32_t addr) {
  for (int i = 0; i < size; i ++) {
    if (map_inside(&maps[i], addr)) {
      return i;
    }
  }
  return -1;
}

// 函数声明保持一致性
void add_mmio_map(const char *name, uint32_t addr, void *space, uint32_t len, io_callback_t callback);
uint64_t map_read(uint32_t addr, int len, IOMap *map);
void map_write(uint32_t addr, int len, uint64_t data, IOMap *map);


#endif