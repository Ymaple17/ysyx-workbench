#include <bits/stdc++.h>
#include "Vtop.h"
#include "verilated_vcd_c.h"

int main()
{
	VerilatedContext* contextp = new VerilatedContext;
	Vtop* top=new Vtop(contextp);
	VerilatedVcdC* tfp = new VerilatedVcdC;
	contextp->traceEverOn(true);
	top->trace(tfp,0);
	tfp->open("wave.vcd");
	while(contextp->time()<100){
		int a=rand()&1;
		int b=rand()&1;
		top->a=a;
		top->b=b;
		top->eval();
		tfp->dump(contextp->time());
		contextp->timeInc(1);
		assert(top->f == (a^b));
	}
	tfp->close();
	delete top;
	delete contextp;
	return 0;
}

