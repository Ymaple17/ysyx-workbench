// Board clock generator for the pure-PL shell.
//
// The 7-series MMCM VCO is kept at 1050 MHz. This makes common CPU clocks
// easy to select by changing CPU_FREQ_MHZ while keeping the input clock and
// feedback path stable. The requested output divider must be a legal MMCM
// value (0.125 step for the fractional range).
module board_clock #(
  parameter real CLKIN_FREQ_MHZ = 50.0,
  parameter real CPU_FREQ_MHZ = 84.0,
  parameter real VCO_FREQ_MHZ = 1050.0
)(
  input  clk_in,
  input  reset,
  output clk_out,
  output locked
);
  localparam real CLKIN_PERIOD_NS = 1000.0 / CLKIN_FREQ_MHZ;
  localparam real CLKFBOUT_MULT_F = VCO_FREQ_MHZ / CLKIN_FREQ_MHZ;
  localparam real CLKOUT0_DIVIDE_F = VCO_FREQ_MHZ / CPU_FREQ_MHZ;

`ifndef SYNTHESIS
  // Icarus has no Xilinx primitive model. The simulation clock remains the
  // 50 MHz source clock and the top-level UART uses its matching divider.
  assign clk_out = clk_in;
  assign locked = !reset;
`else
  wire mmcm_clkfb;
  wire mmcm_clkfb_buf;
  wire mmcm_clkout;
  wire mmcm_locked;

  MMCME2_BASE #(
    .BANDWIDTH("OPTIMIZED"),
    .CLKFBOUT_MULT_F(CLKFBOUT_MULT_F),
    .CLKIN1_PERIOD(CLKIN_PERIOD_NS),
    .CLKOUT0_DIVIDE_F(CLKOUT0_DIVIDE_F),
    .DIVCLK_DIVIDE(1),
    .STARTUP_WAIT("FALSE")
  ) mmcm_i (
    .CLKIN1(clk_in),
    .CLKFBIN(mmcm_clkfb_buf),
    .RST(reset),
    .PWRDWN(1'b0),
    .CLKFBOUT(mmcm_clkfb),
    .CLKOUT0(mmcm_clkout),
    .LOCKED(mmcm_locked)
  );

  BUFG mmcm_feedback_buf_i (
    .I(mmcm_clkfb),
    .O(mmcm_clkfb_buf)
  );

  BUFG mmcm_output_buf_i (
    .I(mmcm_clkout),
    .O(clk_out)
  );

  assign locked = mmcm_locked;
`endif
endmodule
