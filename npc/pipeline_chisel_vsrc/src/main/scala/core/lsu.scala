package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.MEM_WMASK._
import common.REG_WRITE_SEL._
import common.IRQ_CTRL._
import unit.WBU_signals
import bus._
import core.PerfEvents._

class LSU_WBU_IO extends Bundle{
  val signals = new Bundle{
    val wbu = new WBU_signals
  }

  val pc = UInt(32.W)
  val waddr = UInt(5.W)
  val is_ebreak = Bool()

  val wb_data   = UInt(32.W)
  val csr_wdata = UInt(32.W)
  val csr_waddr = UInt(12.W)

  val state = new State
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  
  val dmem = new AXI4Master

  val is_flush = Input(Bool())
}

class LSU(val conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_LSU"

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

  val s_IDLE  :: s_WOEK :: s_FLUSH :: Nil = Enum(3)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(state)

  val ar_handshake_done = RegInit(false.B)
  val aw_handshake_done = RegInit(false.B)
  val w_handshake_done = RegInit(false.B)

  when(state =/= s_IDLE || io.is_flush) {
    ar_handshake_done := false.B
    aw_handshake_done := false.B
    w_handshake_done := false.B
  }

  val load_handshake = is_load && (ar_handshake_done || (io.dmem.arvalid && io.dmem.arready))
  val aw_done = is_store && (aw_handshake_done || (io.dmem.awvalid && io.dmem.awready))
  val w_done  = is_store && (w_handshake_done || (io.dmem.wvalid && io.dmem.wready))
  val store_handshake = aw_done && w_done

  next_state := MuxLookup(state, s_IDLE)(Seq(
      s_IDLE  -> Mux(idle && (load_handshake || store_handshake), Mux(work, s_IDLE, s_WOEK), s_IDLE),
      s_WOEK  -> Mux(work, s_IDLE, Mux(io.is_flush, s_FLUSH, s_WOEK)),
      s_FLUSH -> Mux(io.dmem.rvalid || io.dmem.bvalid, s_IDLE, s_FLUSH)
  ))
  state := next_state

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
  when(io.is_flush || io.dmem.rvalid || io.dmem.bvalid) {
    ar_handshake_done := false.B
    aw_handshake_done := false.B
    w_handshake_done := false.B
  }

  io.dmem.arvalid := is_load && (state === s_IDLE) && idle && !ar_handshake_done
  io.dmem.awvalid := is_store && (state === s_IDLE) && idle && !aw_handshake_done
  io.dmem.wvalid := is_store && (state === s_IDLE) && idle && !w_handshake_done
  
  io.dmem.rready := (is_load && (state === s_WOEK)) || state === s_FLUSH
  io.dmem.bready := (is_store && (state === s_WOEK)) || state === s_FLUSH
  io.dmem.arsize := arsize
  io.dmem.awsize := awsize

  val byte_offset = io.in.bits.alu_result(1, 0)

  val mem_wmask = Wire(UInt(4.W))
  mem_wmask := io.in.bits.signals.lsu.mem_wmask << byte_offset

  val rd_offset = io.dmem.rdata >> (byte_offset << 3)

  io.dmem.araddr := io.in.bits.alu_result
  io.dmem.awaddr := io.in.bits.alu_result
  io.dmem.wdata := io.in.bits.rd2 << (byte_offset << 3)
  io.dmem.wstrb := mem_wmask

  //WBU
  io.out.bits.signals := io.in.bits.signals
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.waddr := io.in.bits.waddr
  io.out.bits.is_ebreak := io.in.bits.is_ebreak

  val rbyte   = rd_offset(7, 0).asSInt.pad(32).asUInt
  val rhalf   = rd_offset(15, 0).asSInt.pad(32).asUInt
  val rword   = rd_offset(31, 0)
  val rbyteu  = rd_offset(7, 0)
  val rhalfu  = rd_offset(15, 0)

  val mem_read_data = MuxLookup(io.in.bits.signals.lsu.mem_rd, rbyte)(Seq(
      RBYTE  -> rbyte,
      RHALF  -> rhalf,
      RWORD  -> rword,
      RBYTEU -> rbyteu,
      RHALFU -> rhalfu
  ))

  //load 结果在 LSU 折进写回数据
  io.out.bits.wb_data := Mux(io.in.bits.signals.wbu.reg_write_sel === MEM_SEL, mem_read_data, io.in.bits.wb_data)
  io.out.bits.csr_wdata := io.in.bits.csr_wdata
  io.out.bits.csr_waddr := io.in.bits.csr_waddr

  //state（LAF/SAF）
  val has_laf = io.dmem.rvalid && io.dmem.rresp =/= 0.U
  val has_saf = io.dmem.bvalid && io.dmem.bresp =/= 0.U
  io.out.bits.state := io.in.bits.state
  io.out.bits.state.state := has_laf || has_saf || io.in.bits.state.state
  io.out.bits.state.state_num := Mux(has_saf, IRQ_SAF, Mux(has_laf, IRQ_LAF, io.in.bits.state.state_num))

  //pipeline
  idle := io.in.valid && !io.is_flush
  ready := io.dmem.rvalid || io.dmem.bvalid || not_load_or_store
  work := ready && io.out.ready
  io.in.ready := !io.in.valid || (ready && io.out.ready)
  io.out.valid := io.in.valid && ready

  if(conf.statistics){
    PM(conf, clock, EVENT_LSU_READ, 1.U, io.dmem.arvalid && io.dmem.arready)
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, io.dmem.awvalid && io.dmem.awready)
    PM(conf, clock, EVENT_LSU_LATENCY, 1.U, state === s_WOEK && (!ready || io.out.ready))
  }
}

