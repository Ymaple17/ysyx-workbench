#pragma once

#include <stdint.h> // For uint32_t, uint8_t
#include <mem/paddr.h> // For paddr_t and word_t
#define PAGE_SIZE 4096
#define PAGE_MASK (~(PAGE_SIZE - 1))

// ----------------------- Devices management -----------------------

typedef void(*io_callback_t)(uint32_t, int, bool);

// Allocate space for backing MMIO
uint8_t* new_space(int size);

typedef struct {
  const char *name;
  paddr_t low;
  paddr_t high;
  uint8_t *space;
  io_callback_t callback;
} Device;

void add_device(const char *name, paddr_t addr,
        uint8_t *space, uint32_t len, io_callback_t callback);

word_t device_read(paddr_t addr, int len);
void device_write(paddr_t addr, int len, word_t data);
