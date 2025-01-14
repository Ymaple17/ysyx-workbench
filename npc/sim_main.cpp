#include "Vtop.h"  
#include "verilated.h"
#include "verilated_vcd_c.h"  
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true); 

    Vtop* top = new Vtop;

    VerilatedVcdC* tfp = new VerilatedVcdC;
    top->trace(tfp, 99);  
    tfp->open("waveform.vcd");  

    for (int i = 0; i < 10; i++) {
        int a = rand() & 1;
        int b = rand() & 1;
        top->a = a;
        top->b = b;
        top->eval();
        tfp->dump(i);  
        printf("a = %d, b = %d, f = %d\n", a, b, top->f);
        assert(top->f == (a ^ b));
    }

    tfp->close();  
    delete top;
    delete tfp;

    return 0;
}
