package sim

import chisel3._
import chisel3.util._

class Pmem extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val wen = Input(Bool())
    val valid = Input(Bool())
    val raddr = Input(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wmask = Input(UInt(8.W))

    val rdata = Output(UInt(32.W))
  })
  setInline(
    "pmem_dpi.v",
    """
      |module Pmem(
      |  input        valid,
      |  input        wen,
      |  input [31:0] raddr,
      |  input [31:0] waddr,
      |  input [31:0] wdata,
      |  input [7:0]  wmask,
      |  output reg [31:0] rdata
      |);
      |
      |import "DPI-C" function int paddr_read(input int raddr);
      |import "DPI-C" function void paddr_write(input int waddr, input int wdata, input byte wmask);
      |
      |always @(*) begin
      |  if (valid) begin
      |    rdata = paddr_read(raddr);
      |    if (wen) begin
      |      paddr_write(waddr, wdata, wmask);
      |    end
      |  end
      |  else begin
      |    rdata = 0;
      |  end
      |end
      |
      |endmodule
    """.stripMargin
  )
}