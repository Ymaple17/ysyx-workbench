

module ysyx_25020039_Ebreak(
  input is_ebreak
);

`ifndef YOSYS
`ifdef __ICARUS__
  always @(*) begin
     if (is_ebreak) begin
        $display("Ebreak triggered. Finishing simulation.");
        $finish;
     end
  end
`else
`ifdef VERILATOR
import "DPI-C" function void sim_exit();
always @(*) begin
   if (is_ebreak) begin
      sim_exit();
   end
end
`endif
`endif
`endif

endmodule
    
