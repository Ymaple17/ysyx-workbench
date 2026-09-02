package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.MEM_WMASK._
import common.IRQ_CTRL._
import unit.{DCacheEarlyLoadResp, WBU_signals}
import bus._
import core.PerfEvents._

class LSU_WBU_IO extends Bundle{
  val signals = new Bundle{
    val wbu = new WBU_signals
  }

  val rd1 = UInt(32.W)
  val alu_result = UInt(32.W)
  val pc = UInt(32.W)
  val next_pc = UInt(32.W)
  val imm_ext = UInt(32.W)
  val mem_read = UInt(32.W)
  val waddr = UInt(5.W)
  val is_ebreak = Bool()

  val csr_rd1 = UInt(32.W)
  val csr_waddr = UInt(12.W)

  val state = new State
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  val loadPreissued = Input(Bool())
  val earlyLoadResp = Flipped(Decoupled(new DCacheEarlyLoadResp))
  val loadResultValid = Output(Bool())
  
  val dmem = new AXI4Master

  val is_flush = Input(Bool())
}

class LSU(val conf: CoreConfig) extends Module{

  val io = IO(new LSU_IO(conf.xlen))

  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arburst := 1.U //INCR
  io.dmem.awid := 0.U
  io.dmem.awlen := 0.U
  io.dmem.awburst := 1.U //INCR
  io.dmem.wlast := true.B

  val is_load = !io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  val is_store = io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  val not_load_or_store = !is_load && !is_store

  val arsize = MuxLookup(io.in.bits.signals.lsu.mem_rd, RWORD)(Seq(
    RBYTE  -> 0.U,
    RHALF  -> 1.U,
    RWORD  -> 2.U,
    RBYTEU -> 0.U,
    RHALFU -> 1.U
  ))

  val awsize = MuxLookup(io.in.bits.signals.lsu.mem_wmask, WWORD)(Seq(
    WBYTE -> 0.U,
    WHALF -> 1.U,
    WWORD -> 2.U
  ))

  val idle = Wire(Bool())
  val work = Wire(Bool())
  val ready = Wire(Bool())

  val s_IDLE  :: s_WORK :: s_FLUSH :: Nil = Enum(3)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(state)
  val earlyReplay = RegInit(false.B)
  val replayArDone = RegInit(false.B)

  val ar_handshake_done = RegInit(false.B)
  val aw_handshake_done = RegInit(false.B)
  val w_handshake_done = RegInit(false.B)

  when(state =/= s_IDLE || io.is_flush) {
    ar_handshake_done := false.B
    aw_handshake_done := false.B
    w_handshake_done := false.B
  }

  val load_handshake = is_load && (io.loadPreissued || ar_handshake_done ||
    (io.dmem.arvalid && io.dmem.arready))
  val aw_done = is_store && (aw_handshake_done || (io.dmem.awvalid && io.dmem.awready))
  val w_done  = is_store && (w_handshake_done || (io.dmem.wvalid && io.dmem.wready))
  val store_handshake = aw_done && w_done

  val flushLoadDone = io.loadResultValid ||
    (io.loadPreissued && io.earlyLoadResp.valid) ||
    (io.loadPreissued && earlyReplay && !replayArDone)
  next_state := MuxLookup(state, s_IDLE)(Seq(
      s_IDLE  -> Mux(idle && (load_handshake || store_handshake), Mux(work, s_IDLE, s_WORK), s_IDLE),
      s_WORK  -> Mux(work, s_IDLE, Mux(io.is_flush, s_FLUSH, s_WORK)),
      s_FLUSH -> Mux(flushLoadDone || io.dmem.bvalid, s_IDLE, s_FLUSH)
  ))
  state := next_state

  when(io.earlyLoadResp.fire && io.earlyLoadResp.bits.replay && state =/= s_FLUSH) {
    earlyReplay := true.B
  }
  when(is_load && earlyReplay && io.dmem.arvalid && io.dmem.arready) {
    replayArDone := true.B
  }
  when(work || (state === s_FLUSH && (flushLoadDone || io.dmem.bvalid))) {
    earlyReplay := false.B
    replayArDone := false.B
  }

  when(state === s_IDLE && idle) {
      when(is_load && io.dmem.arvalid && io.dmem.arready) {
        ar_handshake_done := true.B
      }
      when(is_store) {
          when(io.dmem.awvalid && io.dmem.awready) {
            aw_handshake_done := true.B
          }
          when(io.dmem.wvalid && io.dmem.wready) {
            w_handshake_done := true.B
          }
      }
  }

  // 响应到达（含握手与响应同拍的 work 路径）或冲刷时清除握手标志
  when(io.is_flush || io.loadResultValid || io.dmem.bvalid) {
    ar_handshake_done := false.B
    aw_handshake_done := false.B
    w_handshake_done := false.B
  }

  val normalLoadAr = state === s_IDLE && !io.loadPreissued && !ar_handshake_done
  val replayLoadAr = state === s_WORK && io.loadPreissued && earlyReplay && !replayArDone
  io.dmem.arvalid := is_load && idle && (normalLoadAr || replayLoadAr)
  io.dmem.awvalid := is_store && (state === s_IDLE) && idle && !aw_handshake_done
  io.dmem.wvalid := is_store && (state === s_IDLE) && idle && !w_handshake_done

  io.dmem.rready := (is_load && (!io.loadPreissued || earlyReplay) && state === s_WORK) ||
    state === s_FLUSH
  io.earlyLoadResp.ready := state === s_FLUSH ||
    (is_load && io.loadPreissued && (io.earlyLoadResp.bits.replay || io.out.ready))
  io.dmem.bready := (is_store && (state === s_WORK || state === s_IDLE)) ||
    state === s_FLUSH
  io.dmem.arsize := arsize
  io.dmem.awsize := awsize

  val byte_offset = io.in.bits.memory_address(1, 0)
  val useEarlyLoad = io.loadPreissued && !earlyReplay
  val loadResponseValid = Mux(useEarlyLoad,
    io.earlyLoadResp.valid && !io.earlyLoadResp.bits.replay, io.dmem.rvalid)
  val loadResponseData = Mux(useEarlyLoad,
    io.earlyLoadResp.bits.data, io.dmem.rdata)
  val loadResponseCode = Mux(useEarlyLoad,
    io.earlyLoadResp.bits.resp, io.dmem.rresp)
  io.loadResultValid := is_load && loadResponseValid

  val mem_wmask = Wire(UInt(4.W))
  mem_wmask := io.in.bits.signals.lsu.mem_wmask << byte_offset

  io.dmem.araddr := io.in.bits.memory_address
  io.dmem.awaddr := io.in.bits.memory_address
  io.dmem.wdata := io.in.bits.rd2 << (byte_offset << 3)
  io.dmem.wstrb := mem_wmask

  //WBU
  io.out.bits.signals := io.in.bits.signals
  io.out.bits.rd1 := io.in.bits.rd1
  io.out.bits.alu_result := io.in.bits.alu_result
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.next_pc := io.in.bits.next_pc
  io.out.bits.imm_ext := io.in.bits.imm_ext
  io.out.bits.waddr := io.in.bits.waddr
  io.out.bits.is_ebreak := io.in.bits.is_ebreak

  //csr
  io.out.bits.csr_rd1 := io.in.bits.csr_rd1
  io.out.bits.csr_waddr := io.in.bits.csr_waddr

  val selectedByte = MuxLookup(byte_offset, loadResponseData(7, 0))(Seq(
    0.U -> loadResponseData(7, 0),
    1.U -> loadResponseData(15, 8),
    2.U -> loadResponseData(23, 16),
    3.U -> loadResponseData(31, 24)
  ))
  val selectedHalf = MuxLookup(byte_offset, loadResponseData(15, 0))(Seq(
    0.U -> loadResponseData(15, 0),
    1.U -> loadResponseData(23, 8),
    2.U -> loadResponseData(31, 16),
    3.U -> Cat(0.U(8.W), loadResponseData(31, 24))
  ))
  val rbyte = selectedByte.asSInt.pad(32).asUInt
  val rhalf = selectedHalf.asSInt.pad(32).asUInt
  val rbyteu = selectedByte.pad(32)
  val rhalfu = selectedHalf.pad(32)

  // Keep the common architecturally aligned word load off the byte-lane
  // selection cone; byte and halfword operations still select their lane.
  io.out.bits.mem_read := MuxLookup(io.in.bits.signals.lsu.mem_rd, 0.U)(Seq(
    RBYTE  -> rbyte,
    RHALF  -> rhalf,
    RWORD  -> loadResponseData,
    RBYTEU -> rbyteu,
    RHALFU -> rhalfu
  ))

  //state（LAF/SAF）
  val has_laf = loadResponseValid && loadResponseCode =/= 0.U
  val has_saf = io.dmem.bvalid && io.dmem.bresp =/= 0.U
  io.out.bits.state := io.in.bits.state
  io.out.bits.state.state := has_laf || has_saf || io.in.bits.state.state
  io.out.bits.state.state_num := Mux(has_saf, IRQ_SAF, Mux(has_laf, IRQ_LAF, io.in.bits.state.state_num))

  //pipeline
  idle := io.in.valid && !io.is_flush
  ready := loadResponseValid || io.dmem.bvalid || not_load_or_store
  work := ready && io.out.ready
  io.in.ready := !io.in.valid || (ready && io.out.ready)
  io.out.valid := io.in.valid && ready

  if(conf.statistics){
    PM(conf, clock, EVENT_LSU_READ, 1.U,
      (io.dmem.arvalid && io.dmem.arready) ||
        (state === s_IDLE && io.in.valid && is_load && io.loadPreissued))
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, io.dmem.awvalid && io.dmem.awready)
    PM(conf, clock, EVENT_LSU_LATENCY, 1.U, state === s_WORK && (!ready || io.out.ready))
    PM(conf, clock, EVENT_LSU_LOAD_WAIT, 1.U, io.in.valid && is_load && !ready)
    PM(conf, clock, EVENT_LSU_STORE_WAIT, 1.U, io.in.valid && is_store && !ready)
  }
}
