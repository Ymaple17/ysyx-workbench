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
  val EVENT_FETCH_SLOT0_VALID = 47.U(32.W)
  val EVENT_FETCH_SLOT1_VALID = 48.U(32.W)
  val EVENT_FETCH_SLOT1_KILLED = 49.U(32.W)
  val EVENT_FQ_ENQ2           = 50.U(32.W)
  val EVENT_FQ_SPACE_ONE      = 51.U(32.W)
  val EVENT_FETCH_REDIRECT_BUBBLE = 52.U(32.W)
  val EVENT_BPU_TAGGED_HIT    = 53.U(32.W)
  val EVENT_BPU_INDIRECT_HIT  = 54.U(32.W)
  val EVENT_COMMIT_SLOT0      = 55.U(32.W)
  val EVENT_COMMIT_SLOT1      = 56.U(32.W)
  val EVENT_COMMIT2           = 57.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK = 58.U(32.W)
  val EVENT_COMMIT_SLOT1_NOT_READY = 59.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_SLOT0_EXCL = 60.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_MEM = 61.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_CTRL = 62.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_CSR = 63.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_SPECIAL = 64.U(32.W)
  val EVENT_COMMIT_SLOT1_BLOCK_BP = 65.U(32.W)
  val EVENT_DCACHE_MSHR_ALLOC = 66.U(32.W)
  val EVENT_DCACHE_HIT_UNDER_MISS = 67.U(32.W)
  val EVENT_DCACHE_MSHR_REFILL = 68.U(32.W)
  val EVENT_TAGE_USE_ALT       = 69.U(32.W)
  val EVENT_TAGE_ALLOC         = 70.U(32.W)
  val EVENT_ITAGE_HIT          = 71.U(32.W)
  val EVENT_ITAGE_ALLOC        = 72.U(32.W)
  val EVENT_COMMIT_SLOT1_LOAD  = 73.U(32.W)
  val EVENT_COMMIT_SLOT1_CTRL  = 74.U(32.W)
  val EVENT_COMMIT_SLOT1_STORE = 75.U(32.W)
  val EVENT_BPU_UPDATE_QUEUE_BLOCK = 76.U(32.W)
  val EVENT_STORE_BUFFER_ENQ2  = 77.U(32.W)
  val EVENT_LQ_ALLOC           = 78.U(32.W)
  val EVENT_LQ_FULL            = 79.U(32.W)
  val EVENT_LQ_HIGH_WATER      = 80.U(32.W)
  val EVENT_LOAD_REPLAY        = 81.U(32.W)
  val EVENT_REPLAY_STORE_WAIT  = 82.U(32.W)
  val EVENT_REPLAY_DCACHE_BUSY = 83.U(32.W)
  val EVENT_REPLAY_CDB_BUSY    = 84.U(32.W)
  val EVENT_STALE_LOAD_RESP    = 85.U(32.W)
  val EVENT_MEM_ORDER_VIOLATION = 86.U(32.W)
  val EVENT_FETCH_BLOCK        = 87.U(32.W)
  val EVENT_FETCH_VALID_INST   = 88.U(32.W)
  val EVENT_FETCH_BLOCK2       = 89.U(32.W)
  val EVENT_FETCH_BUFFER_FULL  = 90.U(32.W)
  val EVENT_FTQ_FULL           = 91.U(32.W)
  val EVENT_FETCH_LINE_TAIL    = 92.U(32.W)
  val EVENT_SPEC_GHR_ROLLBACK  = 93.U(32.W)
  val EVENT_RAS_ROLLBACK       = 94.U(32.W)
  val EVENT_FTQ_HIGH_WATER     = 95.U(32.W)
  val EVENT_FTQ_STALE_RECOVER  = 96.U(32.W)
  val EVENT_ALU1_ISSUE         = 97.U(32.W)
  val EVENT_DUAL_ALU_ISSUE     = 98.U(32.W)
  val EVENT_FU_REFILL          = 99.U(32.W)
  val EVENT_WB_AGE_REORDER     = 100.U(32.W)
  val EVENT_RS_CDB_WAKE_ISSUE  = 101.U(32.W)
  val EVENT_LSU_ADDR_REFILL    = 102.U(32.W)
  val EVENT_STORE_BUFFER_MERGE = 103.U(32.W)
  val EVENT_DCACHE_STORE_HIT   = 104.U(32.W)
  val EVENT_STORE_BUFFER_WRITE_BURST = 105.U(32.W)
  val EVENT_STORE_BUFFER_WRITE_BEAT = 106.U(32.W)
  val EVENT_DISPATCH_SLOT1 = 107.U(32.W)
  val EVENT_DISPATCH_BRANCH_SLOT1 = 108.U(32.W)
  val EVENT_DISPATCH_SLOT1_CTRL_BLOCK = 109.U(32.W)
  val EVENT_DISPATCH_SLOT1_BACKEND_BLOCK = 110.U(32.W)
  val EVENT_BIMODAL_SELECTED = 111.U(32.W)
  val EVENT_CONTROL_DIRECT_COMPLETE = 112.U(32.W)
  val EVENT_DCACHE_SECONDARY_ALLOC = 113.U(32.W)
  val EVENT_DCACHE_MSHR_MERGE = 114.U(32.W)
  val EVENT_HEAD_WAIT_ALU = 115.U(32.W)
  val EVENT_HEAD_WAIT_LOAD = 116.U(32.W)
  val EVENT_HEAD_WAIT_STORE = 117.U(32.W)
  val EVENT_HEAD_WAIT_CTRL = 118.U(32.W)
  val EVENT_HEAD_WAIT_OTHER = 119.U(32.W)
  val EVENT_FETCH_WAIT_RESOURCE = 120.U(32.W)
  val EVENT_FETCH_WAIT_PC = 121.U(32.W)
  val EVENT_FETCH_SLOT1_CREDIT_BLOCK = 122.U(32.W)
  val EVENT_BRANCH_ISSUE = 123.U(32.W)
  val EVENT_BRANCH_READY_WAIT = 124.U(32.W)
  val EVENT_STORE_DIRECT_COMPLETE = 125.U(32.W)
  val EVENT_LOOP_PREDICT_HIT = 126.U(32.W)
  val EVENT_RS_FRESH_ISSUE = 127.U(32.W)
  val EVENT_FETCH_SLOT2_VALID = 128.U(32.W)
  val EVENT_FETCH_SLOT3_VALID = 129.U(32.W)
  val EVENT_DISPATCH_SLOT2 = 130.U(32.W)
  val EVENT_DISPATCH_SLOT3 = 131.U(32.W)
  val EVENT_COMMIT_SLOT2 = 132.U(32.W)
  val EVENT_COMMIT_SLOT3 = 133.U(32.W)
  val EVENT_COMMIT4 = 134.U(32.W)
  val EVENT_QUAD_ALU_ISSUE = 135.U(32.W)
}

object PcPerfEvents {
  val PC_EVENT_HEAD_WAIT = 0.U(32.W)
  val PC_EVENT_DIR_MISPRED = 1.U(32.W)
  val PC_EVENT_TARGET_MISPRED = 2.U(32.W)
  val PC_EVENT_UNPREDICTED = 3.U(32.W)
  val PC_EVENT_BP_FLUSH = 4.U(32.W)
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

class PcPerfMonitor extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val event_id = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
    val enable = Input(Bool())
  })
  setInline("PcPerfMonitor.v",
    """
      |module PcPerfMonitor(
      |    input clock,
      |    input [31:0] event_id,
      |    input [31:0] pc,
      |    input enable
      |);
      |`ifndef __ICARUS__
      |`ifndef YOSYS
      |  import "DPI-C" function void npc_pm_pc_event(input int event_id, input int pc);
      |  always @(posedge clock) begin
      |     if(enable) npc_pm_pc_event(event_id, pc);
      |  end
      |`endif
      |`endif
      |endmodule
    """.stripMargin)
}

object PMPC {
  def apply(config: CoreConfig, clock: Clock, event: UInt, pc: UInt, enable: Bool): Unit = {
    if (config.statistics) {
      val pm = Module(new PcPerfMonitor)
      pm.io.clock := clock
      pm.io.event_id := event
      pm.io.pc := pc
      pm.io.enable := enable
    }
  }
}
