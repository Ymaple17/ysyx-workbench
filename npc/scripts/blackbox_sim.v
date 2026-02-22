(* blackbox *)
(* keep *)
module SimUART(
    input clk,
    input wen,
    input [31:0] waddr,
    input [7:0]  wdata
);
endmodule

(* blackbox *)
(* keep *)
module DPI_Mem(
  input         clk,
  input         rst,
  input         ren,
  input         wen,
  input  [31:0] raddr,
  input  [31:0] waddr,
  input  [31:0] wdata,
  input  [3:0]  wmask,
  output [31:0] rdata
);
endmodule
