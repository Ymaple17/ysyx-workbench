
module Ebreak(
  input is_ebreak
);

import "DPI-C" function void sim_exit();

always @(*) begin
   if (is_ebreak) begin
      sim_exit();
  end
 end

endmodule
    