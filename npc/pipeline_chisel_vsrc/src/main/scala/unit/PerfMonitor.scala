package core

import chisel3._
import chisel3.util._

object PerfEvents {
  val EVENT_IFU_FETCH         = 0.U(32.W)
  val EVENT_IFU_STALL_ICACHE  = 1.U(32.W)
  val EVENT_IFU_STALL_IDU     = 2.U(32.W)
  val EVENT_LSU_READ          = 3.U(32.W)
  val EVENT_LSU_WRITE         = 4.U(32.W)
  val EVENT_LSU_LATENCY       = 5.U(32.W)
  val EVENT_EXU_COMP          = 6.U(32.W)
  val EVENT_INST_TYPE_COMPUTE = 7.U(32.W)
  val EVENT_INST_TYPE_LOAD    = 8.U(32.W)
  val EVENT_INST_TYPE_STORE   = 9.U(32.W)
  val EVENT_INST_TYPE_CSR     = 10.U(32.W)
  val EVENT_INST_TYPE_BRANCH  = 11.U(32.W)
  val EVENT_INST_TYPE_JUMP    = 12.U(32.W)
  val EVENT_INST_TYPE_OTHER   = 13.U(32.W)
  val EVENT_ICACHE_MISS       = 14.U(32.W)
}

class PerfMonitor extends BlackBox with HasBlackBoxInline {
    override def desiredName = "ysyx_25020039_PerfMonitor"
  val io = IO(new Bundle {
    val clock    = Input(Clock())
    val event_id = Input(UInt(32.W))
    val data     = Input(UInt(64.W))
    val enable   = Input(Bool())
  })
  setInline("ysyx_25020039_PerfMonitor.v",
    """
      |module ysyx_25020039_PerfMonitor(
      |    input clock,
      |    input [31:0] event_id,
      |    input [63:0] data,
      |    input enable
      |);
      |`ifndef __ICARUS__
      |`ifndef YOSYS
      |  import "DPI-C" function void npc_pm_event(input int event_id, input longint data);
      |  always @(posedge clock) begin
      |     if(enable) npc_pm_event(event_id, data);
      |  end
      |`endif
      |`endif
      |endmodule
    """.stripMargin)
}

object PM {
  def apply(config: CoreConfig, clock: Clock, event: UInt, data: UInt = 1.U(64.W), enable: Bool = true.B): Unit = {
    if (config.statistics) {
       val pm = Module(new PerfMonitor)
       pm.io.clock := clock
       pm.io.event_id := event
       pm.io.data := data
       pm.io.enable := enable
    }
  }
}
