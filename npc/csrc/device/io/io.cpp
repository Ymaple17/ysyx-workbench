#include <common.h>
#include <device/io.h>
#include <cpu/difftest.h>
#include <cpu/cpu.h>
#include <mem/host.h>

#define NR_DEVICE 16

static Device devices[NR_DEVICE] = {};
static int nr_device = 0;

#define IO_SPACE_MAX (2 * 1024 * 1024)

static uint8_t *io_space = NULL;
static uint8_t *p_space = NULL;

void init_io()
{
  io_space = (uint8_t *)malloc(IO_SPACE_MAX);
  assert(io_space);
  p_space = io_space;
}

uint8_t *new_space(int size)
{
  uint8_t *p = p_space;
  // page aligned;
  size = (size + (PAGE_SIZE - 1)) & ~PAGE_MASK;
  p_space += size;
  assert(p_space - io_space < IO_SPACE_MAX);
  return p;
}

static inline bool device_inside(Device *device, paddr_t addr)
{
  return (addr >= device->low && addr <= device->high);
}

static inline int find_deviceid_by_addr(Device *devices, int size, paddr_t addr)
{
  int i;
  for (i = 0; i < size; i++)
  {
    if (device_inside(devices + i, addr))
    {
      device_access_st++;
      return i;
    }
  }
  return -1;
}

static Device *fetch_device(paddr_t addr)
{
  int mapid = find_deviceid_by_addr(devices, nr_device, addr);
  return (mapid == -1 ? NULL : &devices[mapid]);
}

static void report_mmio_overlap(const char *name1, paddr_t l1, paddr_t r1,
                                const char *name2, paddr_t l2, paddr_t r2)
{
  Panic("MMIO region %s@[" FMT_PADDR ", " FMT_PADDR "] is overlapped "
        "with %s@[" FMT_PADDR ", " FMT_PADDR "]",
        name1, l1, r1, name2, l2, r2);
}

void add_device(const char *name, paddr_t addr, uint8_t *space, uint32_t len, io_callback_t callback)
{
  Assert(nr_device < NR_DEVICE, "Too many devices");
  paddr_t left = addr, right = addr + len - 1;
  if (in_pmem(left) || in_pmem(right))
  {
    report_mmio_overlap(name, left, right, "pmem", PMEM_LEFT, PMEM_RIGHT);
  }
  for (int i = 0; i < nr_device; i++)
  {
    if (left <= devices[i].high && right >= devices[i].low)
    {
      report_mmio_overlap(name, left, right, devices[i].name, devices[i].low, devices[i].high);
    }
  }

  devices[nr_device] = (Device){.name = name, .low = addr, .high = addr + len - 1, .space = space, .callback = callback};
  Log("Add mmio map '%s' at [" FMT_PADDR ", " FMT_PADDR "]",
      devices[nr_device].name, devices[nr_device].low, devices[nr_device].high);

  nr_device++;
}

static void check_bound(Device *device, paddr_t addr, bool is_read)
{
  if (device == NULL)
  {
    Assert(device != NULL, "%s at address (" FMT_PADDR ") is out of bound at pc = " FMT_WORD,
           is_read ? "read" : "write", addr, cpu.pc);
  }
  else
  {
    Assert(addr <= device->high && addr >= device->low,
           "%s at address (" FMT_PADDR ") is out of bound {%s} [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
           is_read ? "read" : "write", addr, device->name, device->low, device->high, cpu.pc);
  }
}

static void invoke_callback(io_callback_t c, paddr_t offset, int len, bool is_write)
{
  if (c != NULL)
  {
    c(offset, len, is_write);
  }
}

/* bus interface */
word_t device_read(paddr_t addr, int len)
{
  auto dv = fetch_device(addr);
  check_bound(dv, addr, true);
  paddr_t offset = addr - dv->low;
  invoke_callback(dv->callback, offset, len, false); // prepare data to read
  word_t ret = host_read(dv->space + offset, len);
  return ret;
}

void device_write(paddr_t addr, int len, word_t data)
{
  auto dv = fetch_device(addr);
  check_bound(dv, addr, false);
  paddr_t offset = addr - dv->low;
  host_write(dv->space + offset, len, data);
  invoke_callback(dv->callback, offset, len, true);
}
