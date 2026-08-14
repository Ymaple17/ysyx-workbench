package sim

import chisel3._
import chisel3.util._

class Ebreak extends BlackBox with HasBlackBoxInline {
  override def desiredName = "ysyx_25020039_Ebreak"
  val io = IO(new Bundle {
    val is_ebreak = Input(Bool())
  })
  setInline(
    "ysyx_25020039_Ebreak.v",
    """
      |module ysyx_25020039_Ebreak(
      |  input is_ebreak
      |);
      |
      |`ifdef __ICARUS__
      |  always @(*) begin
      |    if (is_ebreak) begin
      |      $display("Ebreak triggered. Finishing simulation.");
      |      $finish;
      |    end
      |  end
      |`else
      |`ifndef YOSYS
      |  import "DPI-C" function void sim_exit();
      |  always @(*) begin
      |    if (is_ebreak) begin
      |      sim_exit();
      |    end
      |  end
      |`endif
      |`endif
      |
      |endmodule
    """.stripMargin
  )
}
