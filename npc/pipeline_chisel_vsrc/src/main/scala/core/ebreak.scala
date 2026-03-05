package core

import chisel3._
import chisel3.util._

class Ebreak extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val is_ebreak = Input(Bool())
  })
  override def desiredName = "ysyx_25020039_Ebreak"
  setInline(
    "ysyx_25020039_Ebreak.v",
    """
      |module ysyx_25020039_Ebreak(
      |  input is_ebreak
      |);
      |
      |`ifndef YOSYS
      |`ifdef __ICARUS__
      |  always @(*) begin
      |     if (is_ebreak) begin
      |        $display("Ebreak triggered. Finishing simulation.");
      |        $finish;
      |     end
      |  end
      |`else
      |`ifdef SIMULATION
      |import "DPI-C" function void sim_exit();
      |`endif
      |always @(*) begin
      |   if (is_ebreak) begin
      |`ifdef SIMULATION
      |      sim_exit();
      |`endif
      |  end
      | end
      |`endif
      |`endif
      |
      |endmodule
    """.stripMargin
  )
}
