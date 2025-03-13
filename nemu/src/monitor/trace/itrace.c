#include <common.h>
#include <device/map.h>
#define MAX_ITRACE_SIZE 32

typedef struct {
    word_t pc;
    uint32_t inst;
} ITraceNode;

ITraceNode iringbuf[MAX_ITRACE_SIZE];
int p_cur = 0;
bool full = false;

void itrace_inst(word_t pc, uint32_t inst) {
    iringbuf[p_cur].pc = pc;
    iringbuf[p_cur].inst = inst;
    p_cur = (p_cur + 1) % MAX_ITRACE_SIZE;
    full = full || p_cur == 0;
}

void display_inst() {
#ifdef CONFIG_ITRACE
    if (!full && !p_cur) return;
    int end = p_cur;
    int i=full?p_cur:0;
    void disassemble(char *str,int size,uint64_t pc,uint8_t *code,int nbyte);
    char buf[128];
    char *p;
    Statement("Executed Instructions");
    do{
        p = buf;
        p+=sprintf(buf,"%s" FMT_WORD ": %08x ",(i+1)%MAX_ITRACE_SIZE == end ? "-->":"  ",iringbuf[i].pc, iringbuf[i].inst);
        disassemble(p,sizeof(buf)+buf-p,iringbuf[i].pc, (uint8_t *)&iringbuf[i].inst, 4);
        if((i+1)%MAX_ITRACE_SIZE == end) printf(ANSI_FG_RED);
        puts(buf);
    }while((i = (i+1)%MAX_ITRACE_SIZE)!= end);
    puts(ANSI_NONE);
#endif
}

