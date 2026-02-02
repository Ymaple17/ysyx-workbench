package core

import chisel3._
import chisel3.util._
import INST_Control._
import MEM_READ._
import MEM_WMASK._
import IRQ_CTRL._

class LSU_WBU_IO extends Bundle{
  val signals = new Bundle{
    val wbu = new WBU_signals
  }

  val rd1 = UInt(32.W)
  val alu_result = UInt(32.W)
  val csr_rd1 = UInt(32.W)
  val pc = UInt(32.W)
  val next_pc = UInt(32.W)
  val imm_ext = UInt(32.W)
  val mem_read = UInt(32.W)
  val waddr = UInt(5.W)
  val csr_waddr = UInt(12.W)
  val is_ebreak = Bool()
  val state = new State
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  val is_flush = Input(Bool())
  val dmem = new bus.AXI4Master
  val state = Input(new State)
}

class LSU(val conf: CoreConfig) extends Module{
  val io = IO(new LSU_IO(conf.xlen))

  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arburst := 1.U // INCR
  io.dmem.awid := 0.U
  io.dmem.awlen := 0.U
  io.dmem.awburst := 1.U // INCR
  io.dmem.wlast := true.B

  val is_load = Wire(Bool())
  val is_store = Wire(Bool())
  val not_load_or_store = Wire(Bool())
  is_load := (~io.in.bits.signals.lsu.mem_write & io.in.bits.signals.lsu.mem_valid)
  is_store := (io.in.bits.signals.lsu.mem_write & io.in.bits.signals.lsu.mem_valid)
  not_load_or_store := ~is_load & ~is_store

  val arsize = MuxLookup(io.in.bits.signals.lsu.mem_rd, 2.U)(Seq(
        RBYTE   -> 0.U,
        RHALFW  -> 1.U,
        RWORD   -> 2.U,
        RBYTEU  -> 0.U,
        RHALFWU -> 1.U
    ))

  val awsize = MuxLookup(io.in.bits.signals.lsu.mem_wmask, 2.U)(Seq(
        WBYTE   -> 0.U,
        WHALFW  -> 1.U,
        WWORD   -> 2.U
    ))

  val idle = (Wire(Bool()))
  val work = (Wire(Bool()))
  val ready = (Wire(Bool()))

  val s_idle  :: s_work :: s_flush ::Nil = Enum(3)
  val state = RegInit(s_idle)
  val next_state = WireDefault(state)

  val req_addr_sent = RegInit(false.B)
  val req_data_sent = RegInit(false.B)

  // Reset handshake flags
  when(state =/= s_idle || io.is_flush) {
    req_addr_sent := false.B
    req_data_sent := false.B
  }

  // Handshake logic
  val load_handshake = is_load && (io.dmem.arready || req_addr_sent)
  val aw_handshake = is_store && (io.dmem.awready || req_addr_sent)
  val w_handshake  = is_store && (io.dmem.wready  || req_data_sent)
  val store_handshake = aw_handshake && w_handshake

  next_state := MuxLookup(state, s_idle)(Seq(
      s_idle   -> Mux(idle && (load_handshake || store_handshake), Mux(work, s_idle, s_work), s_idle),
      s_work     -> Mux(work, s_idle, Mux(io.is_flush, s_flush, s_work)),
      s_flush   ->  Mux(io.dmem.rvalid || io.dmem.bvalid, s_idle, s_flush)
  ))
  state := next_state

  // Maintain handshake state
  when(state === s_idle && idle) {
      when(is_load && io.dmem.arready) { req_addr_sent := true.B }
      when(is_store) {
          when(io.dmem.awready) { req_addr_sent := true.B }
          when(io.dmem.wready)  { req_data_sent := true.B }
      }
  }
  
  // Reset handshake flags
  when(io.is_flush || (state === s_work && (io.dmem.rvalid || io.dmem.bvalid))) {
    req_addr_sent := false.B
    req_data_sent := false.B
  }

  io.dmem.arvalid := is_load && (state === s_idle) && idle && !req_addr_sent
  io.dmem.awvalid := is_store && (state === s_idle) && idle && !req_addr_sent
  io.dmem.wvalid := is_store && (state === s_idle) && idle && !req_data_sent
  
  // AXI Ready signals should not depend on Valid signals to avoid deadlock
  io.dmem.rready := (is_load && (state === s_work)) || state === s_flush
  io.dmem.bready := (is_store && (state === s_work)) || state === s_flush
  io.dmem.arsize := arsize
  io.dmem.awsize := awsize

  val temp_addr = Wire(UInt(32.W))
  val data_offset = Wire(UInt(32.W))
  temp_addr := io.in.bits.alu_result & (~3.U(32.W))
  data_offset := io.in.bits.alu_result - temp_addr

  val mem_wmask = Wire(UInt(4.W))
  val rd_offset = Wire(UInt(32.W))
  mem_wmask := io.in.bits.signals.lsu.mem_wmask << data_offset(2,0)
  rd_offset := io.dmem.rdata >> (data_offset(2,0) << 3)

  io.dmem.araddr := io.in.bits.alu_result
  io.dmem.awaddr := io.in.bits.alu_result
  io.dmem.wdata := io.in.bits.rd2 << (data_offset(2,0) << 3)
  io.dmem.wstrb := mem_wmask

  //LSU
  io.out.bits.signals := io.in.bits.signals
  io.out.bits.rd1 := io.in.bits.rd1
  io.out.bits.alu_result := io.in.bits.alu_result
  io.out.bits.csr_rd1 := io.in.bits.csr_rd1
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.next_pc := io.in.bits.next_pc
  io.out.bits.imm_ext := io.in.bits.imm_ext
  io.out.bits.waddr := io.in.bits.waddr
  io.out.bits.csr_waddr := io.in.bits.csr_waddr
  io.out.bits.is_ebreak := io.in.bits.is_ebreak

  val rbyte   = rd_offset(7, 0).asSInt.pad(32).asUInt
  val rhalfw  = rd_offset(15, 0).asSInt.pad(32).asUInt
  val rword   = rd_offset(31, 0)
  val rbyteu  = Cat(0.U(24.W), rd_offset(7, 0))
  val rhalfwu = Cat(0.U(16.W), rd_offset(15, 0))

  io.out.bits.mem_read := MuxLookup(io.in.bits.signals.lsu.mem_rd, rbyte)(Seq(
      RBYTE   -> rbyte,
      RHALFW  -> rhalfw,
      RWORD   -> rword,
      RBYTEU  -> rbyteu,
      RHALFWU -> rhalfwu
  ))

  //state
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
}
