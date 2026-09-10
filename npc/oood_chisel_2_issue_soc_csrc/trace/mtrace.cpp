#include "../include/common.h"
#include <cstdio>

static FILE *mtrace_fp = NULL;

void init_mtrace() {
    mtrace_fp = fopen("mtrace.txt", "w");
    if (mtrace_fp == NULL) {
        perror("Failed to open mtrace.txt");
    }
}

void mtrace(char type, uint32_t addr, int len, uint32_t data) {
    if (mtrace_fp) {
        fprintf(mtrace_fp, "%c 0x%08x %d 0x%08x\n", type, addr, len, data);
        // fflush(mtrace_fp); // Uncomment if real-time update is needed, but slows down simulation
    }
}
