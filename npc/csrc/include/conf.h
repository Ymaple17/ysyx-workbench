// #define CONFIG_LOG 1
// #define CONFIG_FTRACE 1
#define CONFIG_DIFFTEST 1
#define CONFIG_ITRACE 1
#define CONFIG_WTRACE 1
// #define CONFIG_WATCHPOINT 1
// #define CONFIG_MTRACE 1

#define CONFIG_MAX_WAVETRACE_CLK 32768
// 设置vcd的flush间隔，1为每次dump都flush一次，0为不flush，n为每n次dump则flush一次
// 该参数意义在于防止npc意外退出时部分波形数据未及时落盘
#define CONFIG_WAVETRACE_FLUSH 0

// 添加缺少的宏定义
#define CONFIG_MSIZE 0x8000000  // 128MB
#define CONFIG_MBASE 0x80000000 // Base address for physical memory
#define CONFIG_PC_RESET_OFFSET 0x0 // PC 复位偏移量
#define RESET_VECTOR (CONFIG_MBASE + CONFIG_PC_RESET_OFFSET) // Reset vector (same as in Top.v)
