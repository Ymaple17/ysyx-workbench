#include <common.h>

void etrace(){
    IFDEF(CONFIG_ETRACE,{printf("\n"ANSI_FMT("[ETRACE]",ANSI_FG_YELLOW)"ecall in mepc = "FMT_WORD",mcause = "FMT_WORD "\n",cpu.csrs.mepc,cpu.csrs.mcause);});
}
