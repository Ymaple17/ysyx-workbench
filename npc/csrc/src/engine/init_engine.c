#include <cpu/cpu.h>

void sdb_mainloop();

extern "C" void engine_start(){
    sdb_mainloop();
}
