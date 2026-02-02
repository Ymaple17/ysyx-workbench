import "DPI-C" function void sim_exit();
module Ebreak(
  input is_ebreak
);
always @(*) begin
   if (is_ebreak) begin
      sim_exit();
  end
 end
endmodule
    