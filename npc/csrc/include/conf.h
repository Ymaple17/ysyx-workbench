// #define CONFIG_LOG 1
// #define CONFIG_FTRACE 1
#define CONFIG_DIFFTEST 1
#define CONFIG_ITRACE 1
#define CONFIG_WTRACE 1
// #define CONFIG_WATCHPOINT 1
// #define CONFIG_MTRACE 1

#define CONFIG_MAX_WAVETRACE_CLK 32768
#define CONFIG_WAVETRACE_FLUSH 0

#define CONFIG_MSIZE 0x8000000 // 128MB
#define CONFIG_MBASE 0x80000000 // Base address for physical memory
#define CONFIG_PC_RESET_OFFSET 0x0 // PC 复位偏移量
#define RESET_VECTOR (CONFIG_MBASE + CONFIG_PC_RESET_OFFSET) // Reset vector (same as in Top.v)
