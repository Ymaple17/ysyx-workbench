
module Sim_Uart(
    input clk,
    input wen,
    input [31:0] waddr,
    input [7:0]  wdata
);
    always @(posedge clk) begin
        if (wen && waddr >= 32'ha00003f8 && waddr <= 32'ha00003ff) begin
            $write("%c", wdata);
        end
    end
endmodule
        