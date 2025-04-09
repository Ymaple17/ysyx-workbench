#include <svdpi.h>
#include <stdio.h>
#include <stdlib.h> // 添加这个头文件以声明 exit 函数

extern "C" void ebreak_handler() {
    printf("EBREAK encountered! Pausing simulation.\n");
    exit(0); // 现在应该可以正常编译
}
