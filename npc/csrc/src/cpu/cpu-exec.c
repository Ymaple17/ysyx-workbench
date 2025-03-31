#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>
#include "isa.h"


#include "verilated.h"
#include "verilated_dpi.h"

#define MAX_INST_TO_PRINT 10

void display_inst();


extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
extern "C" void npc_run_once(Decode *s);

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0;
static bool g_print_step = false;

static void trace_and_difftest(Decode *_this, vaddr_t dnpc){
    #ifdef CONFIG_ITRACE_COND
    log_write("%s\n", _this->logbuf);
    #endif /* ifdef CONFIG_ITRACE_COND */
    if(g_print_step) {puts(_this->logbuf); }
    IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));
    //change_check();
}

static void exec_once(Decode *s, vaddr_t pc){
    s->pc = pc;
    s->snpc = pc;
    npc_run_once(s);   //对顶层模块赋值
    cpu.pc = s->dnpc;
#ifdef CONFIG_ITRACE
    char *p = s->logbuf;
    p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
    int ilen = s->snpc - s->pc;
    int i;
    uint8_t *inst = (uint8_t *)&s->isa.inst.val;
    for (i = ilen - 1; i >= 0; i--) {
        p += snprintf(p, 4, " %02x", inst[i]);
    }
    int ilen_max = 4;
    int space_len = ilen_max - ilen;
    if(space_len < 0) space_len = 0;
    space_len = space_len * 3 + 1;
    memset(p, ' ', space_len);
    p += space_len;

    disassemble(p, s->logbuf + sizeof(s->logbuf) - p, s->pc, (uint8_t *)&s->isa.inst.val, ilen);

#endif 
}

static void execute(uint64_t n){
    Decode s;
    for(; n > 0; n--){
        exec_once(&s, cpu.pc);
        g_nr_guest_inst++;
        trace_and_difftest(&s, cpu.pc);
        if (npc_state.state != NPC_RUNNING) break;
    }
}

static void statistic(){
    setlocale(LC_NUMERIC, "");
#define NUMBERIC_FMT "%'ld"
    Log("host time spent = " NUMBERIC_FMT " us", g_timer);
    Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
    if(g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT "inst/s", g_nr_guest_inst * 1000000 / g_timer);
    else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}


void assert_fail_msg(){
    IFDEF(CONFIG_IRINGBUF, display_inst()); //IRINGBUF
    isa_reg_display();
    statistic();
}

/* Simulate how the CPU works */
void cpu_exec(uint64_t n){
    g_print_step = (n < MAX_INST_TO_PRINT);
    switch(npc_state.state){
        case NPC_END: case NPC_ABORT:
            printf("Program ececution has ended. To restart the program, exit NPC and run again.\n");
            return;
        default: npc_state.state = NPC_RUNNING;
    }

    uint64_t timer_start = get_time();

    execute(n);

    uint64_t timer_end = get_time();
    g_timer += timer_end - timer_start;

    switch(npc_state.state){
        case NPC_RUNNING: npc_state.state = NPC_STOP; break;

        case NPC_END: case NPC_ABORT:
            Log("npc: %s at pc = " FMT_WORD,
                (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
                 (npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
                  ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
                npc_state.halt_pc);

        case NPC_QUIT: statistic();
    }
}
