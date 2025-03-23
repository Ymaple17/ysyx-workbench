#include <verilated_vcd_c.h>
#include <VTop.h>
#include <common.h>

class WaveTracer {
private:
  static const uint32_t MAX_WAVE_CNT = CONFIG_MAX_WAVETRACE_CLK;
  
  VerilatedVcdC* vcd;
  VTop* dut;

public:
  void open(VTop* dut);

  void dump_single();

  void close();
};

extern WaveTracer wavetracer;
