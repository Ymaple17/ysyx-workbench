
module Ebreak(
  input is_ebreak
);

`ifdef __ICARUS__
  always @(*) begin
    if (is_ebreak) begin
      $display("Ebreak triggered. Finishing simulation.");
      $finish;
    end
  end
`else
`ifndef YOSYS
  import "DPI-C" function void sim_exit();
  always @(*) begin
    if (is_ebreak) begin
      sim_exit();
    end
  end
`endif
`endif

endmodule
    