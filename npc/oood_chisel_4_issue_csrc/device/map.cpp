#include "../include/state.h"
#include "../include/common.h"
#include "../include/map.h"
#include <stdlib.h>
#include <assert.h>
#include "../../include/generated/autoconf.h"

#define IO_SPACE_MAX (2 * 1024 * 1024)
#define PAGE_SHIFT   12
#define PAGE_SIZE    (1ul << PAGE_SHIFT)
#define PAGE_MASK    (PAGE_SIZE - 1)

static uint8_t *io_space = NULL;
static uint8_t *p_space = NULL;

uint8_t* new_space(int size) {
  uint8_t *p = p_space;
  // page aligned;
  size = (size + (PAGE_SIZE - 1)) & ~PAGE_MASK;
  p_space += size;
  assert(p_space - io_space < IO_SPACE_MAX);
  return p;
}

static void check_bound(IOMap *map, uint32_t addr) {
  if (map == NULL) {
    assert(0);
  } else {
    assert(addr <= map->high && addr >= map->low);
  }
}

static void invoke_callback(io_callback_t c, uint32_t offset, int len, bool is_write) {
  if (c != NULL) { 
    c(offset, len, is_write); 
  }
}

void init_map() {
  io_space = (uint8_t *)malloc(IO_SPACE_MAX);
  assert(io_space);
  p_space = io_space;
}

inline uint64_t host_read(void *addr, int len) {
  switch (len) {
    case 1: return *(uint8_t  *)addr;
    case 2: return *(uint16_t *)addr;
    case 4: return *(uint32_t *)addr;
    case 8: return *(uint64_t *)addr;
    default: assert(0);
  }
}

inline void host_write(void *addr, int len, uint64_t data) {
  switch (len) {
    case 1: *(uint8_t  *)addr = data; return;
    case 2: *(uint16_t *)addr = data; return;
    case 4: *(uint32_t *)addr = data; return;
    case 8: *(uint64_t *)addr = data; return;
    default: assert(0);
  }
}

uint64_t map_read(uint32_t addr, int len, IOMap *map) {
  assert(len >= 1 && len <= 8);
  check_bound(map, addr);
  uint32_t offset = addr - map->low;
  invoke_callback(map->callback, offset, len, false);
  
  // 修复指针算术警告
  uint8_t *target = (uint8_t *)map->space + offset;
  uint64_t ret = host_read(target, len);
  return ret;
}

void map_write(uint32_t addr, int len, uint64_t data, IOMap *map) {
  assert(len >= 1 && len <= 8);
  check_bound(map, addr);
  uint32_t offset = addr - map->low;
  uint8_t *target = (uint8_t *)map->space + offset;
  host_write(target, len, data);
  invoke_callback(map->callback, offset, len, true);
}