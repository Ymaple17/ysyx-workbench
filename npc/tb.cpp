#include <VTop.h>
#include <verilated_vcd_c.h>
#include <cstdio>
#include <VTop__Dpi.h>

static VTop dut;
VerilatedVcdC* vcd;

static void a_single_cycle() {
  static int sim_time = 0;
  dut.clock = 0; dut.eval(); vcd->dump(sim_time++);
  dut.clock = 1; dut.eval(); vcd->dump(sim_time++);
}

static void reset(int n) {
  dut.reset = 1;
  while (n -- > 0) a_single_cycle();
  dut.reset = 0;
}

static void finish() {
  vcd->close();
  delete vcd;
}

int main() {
  Verilated::traceEverOn(true);
  vcd = new VerilatedVcdC;
  dut.trace(vcd, 0);
  vcd->open("waveform.vcd");
  reset(10);
  
  for (int i = 0; i < 200; i++) {
    a_single_cycle();
  }

  finish();
}

void dpic_ebreak(unsigned int pc, unsigned int code) {
  printf("%#08x: ebreak with code(%d)\n", pc, code);
  finish();
  exit(0);
}

void dpic_commit(unsigned int pc, unsigned int inst, unsigned int npc, char dmAccess) {
  printf("0x%08x: commit inst(0x%08x)\n", pc, inst);
}
