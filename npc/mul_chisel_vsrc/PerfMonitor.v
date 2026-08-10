
module PerfMonitor(
    input clock,
    input [31:0] event_id,
    input [63:0] data,
    input enable
);
`ifndef __ICARUS__
`ifndef YOSYS
  import "DPI-C" function void npc_pm_event(input int event_id, input longint data);
  always @(posedge clock) begin
     if(enable) npc_pm_event(event_id, data);
  end
`endif
`endif
endmodule
    