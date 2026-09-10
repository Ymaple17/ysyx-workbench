#ifndef __TRACE_DIFF_H__
#define __TRACE_DIFF_H__

#include <stdint.h>
#include <stdbool.h>

void trace_diff_init();
void trace_diff_load(const char *ref_so_file, long long img_size);
void trace_diff_register(const char *name, uint32_t *ptr);
void trace_diff_check_inst(uint64_t n);

#endif
