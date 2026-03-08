// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Prototypes for DPI import and export functions.
//
// Verilator includes this file in all generated .cpp files that use DPI functions.
// Manually include this file where DPI .c import functions are declared to ensure
// the C functions match the expectations of the DPI imports.

#ifndef VERILATED_VYSYXSOCFULL__DPI_H_
#define VERILATED_VYSYXSOCFULL__DPI_H_  // guard

#include "svdpi.h"

#ifdef __cplusplus
extern "C" {
#endif


    // DPI IMPORTS
    // DPI import at ../ysyxSoC/perip/flash/flash.v:84:30
    extern void flash_read(int addr, int* data);
    // DPI import at ../ysyxSoC/build/ysyxSoCFull.v:7106:30
    extern void mrom_read(int raddr, int* rdata);
    // DPI import at ../ysyxSoC/perip/psram/psram.v:1:30
    extern void psram_read(int addr, int* data);
    // DPI import at ../ysyxSoC/perip/psram/psram.v:2:30
    extern void psram_write(int addr, int data, int wcount);
    // DPI import at vsrc/soc/ysyx_25020039_Ebreak.v:17:30
    extern void sim_exit();

#ifdef __cplusplus
}
#endif

#endif  // guard
