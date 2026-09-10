
module PcPerfMonitor(
    input clock,
    input [31:0] event_id,
    input [31:0] pc,
    input enable
);
`ifndef __ICARUS__
`ifndef YOSYS
  import "DPI-C" function void npc_pm_pc_event(input int event_id, input int pc);
  always @(posedge clock) begin
     if(enable) npc_pm_pc_event(event_id, pc);
  end
`endif
`endif
endmodule
    