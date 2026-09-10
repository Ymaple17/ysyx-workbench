package sim

import chisel3._
import chisel3.util._

class Ebreak extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val is_ebreak = Input(Bool())
  })
  setInline(
    "Ebreak.v",
    """
      |module Ebreak(
      |  input is_ebreak
      |);
      |
      |import "DPI-C" function void sim_exit();
      |
      |always @(*) begin
      |   if (is_ebreak) begin
      |      sim_exit();
      |  end
      | end
      |
      |endmodule
    """.stripMargin
  )
}
