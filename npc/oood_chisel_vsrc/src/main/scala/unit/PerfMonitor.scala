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
  val EVENT_BPU_PREDICT       = 15.U(32.W)
  val EVENT_BPU_MISPRED       = 16.U(32.W)
  val EVENT_MUL               = 17.U(32.W)
  val EVENT_DIV               = 18.U(32.W)
  val EVENT_COMMIT            = 19.U(32.W)
  val EVENT_ROB_FULL          = 20.U(32.W)
  val EVENT_FL_EMPTY          = 21.U(32.W)
  val EVENT_RS_FULL           = 22.U(32.W)
  val EVENT_FQ_FULL           = 23.U(32.W)
  val EVENT_FQ_EMPTY          = 24.U(32.W)
  val EVENT_BP_FLUSH          = 25.U(32.W)
  val EVENT_CDB_CONFLICT      = 26.U(32.W)
  val EVENT_CDB_BLOCKED       = 27.U(32.W)
  val EVENT_COMMIT_HEAD_WAIT  = 28.U(32.W)
  val EVENT_COMMIT_WAIT_STORE = 29.U(32.W)
  val EVENT_COMMIT_WAIT_FENCE = 30.U(32.W)
  val EVENT_COMMIT_WAIT_BP    = 31.U(32.W)
  val EVENT_COMMIT_WAIT_FLUSH = 32.U(32.W)
  val EVENT_LSU_SQ_WAIT       = 33.U(32.W)
  val EVENT_LSU_SQ_FORWARD    = 34.U(32.W)
  val EVENT_LSU_BUS_WAIT      = 35.U(32.W)
  val EVENT_BPU_DIR_MISPRED   = 36.U(32.W)
  val EVENT_BPU_TARGET_MISPRED = 37.U(32.W)
  val EVENT_BPU_UNPREDICTED   = 38.U(32.W)
  val EVENT_STORE_BUFFER_ENQ  = 39.U(32.W)
  val EVENT_STORE_BUFFER_DRAIN = 40.U(32.W)
  val EVENT_STORE_BUFFER_FULL = 41.U(32.W)
  val EVENT_STORE_BUFFER_FORWARD = 42.U(32.W)
  val EVENT_DCACHE_ACCESS     = 43.U(32.W)
  val EVENT_DCACHE_HIT        = 44.U(32.W)
  val EVENT_DCACHE_MISS       = 45.U(32.W)
  val EVENT_DCACHE_BYPASS     = 46.U(32.W)
}

class PerfMonitor extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock    = Input(Clock())
    val event_id = Input(UInt(32.W))
    val data     = Input(UInt(64.W))
    val enable   = Input(Bool())
  })
  setInline("PerfMonitor.v",
    """
      |module PerfMonitor(
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
