#include "Vdouble1.h"
#include "verilated.h"
#include "verilated_fst_c.h"
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

int main(int argc, char** argv)
{
	VerilatedContext* contextp = new VerilatedContext;
	contextp->commandArgs(argc, argv);
	Vdouble1* double1 = new Vdouble1{contextp};
	VerilatedFstC* tfp = new VerilatedFstC;
	contextp->traceEverOn(true);
	double1->trace(tfp, 0);
	tfp->open("wave.fst");
			while (!contextp->gotFinish()) {
        int a = rand() & 1;
        int b = rand() & 1;
        double1->a = a;
        double1->b = b;
        double1->eval();
        printf("a = %d, b = %d, f = %d\n", a, b, double1->f);
        assert(double1->f == (a ^ b));
  }
	delete double1;
	tfp->close();
	delete contextp;
	return 0;
}
