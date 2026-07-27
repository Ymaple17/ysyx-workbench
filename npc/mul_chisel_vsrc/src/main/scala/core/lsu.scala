package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.MEM_WMASK._
import unit.WBU_signals
import bus._

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
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  
  val dmem = new AXI4Master
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

  val mem_valid = io.in.valid && io.in.bits.signals.lsu.mem_valid
  val mem_write = io.in.bits.signals.lsu.mem_write
  val is_load = !mem_write & mem_valid
  val is_store = mem_write & mem_valid
  val no_mem = !mem_valid

  val arize = MuxLookup(io.in.bits.signals.lsu.mem_rd, RWORD)(Seq(
    RBYTE   -> 0.U,
    RHALF  -> 1.U,
    RWORD   -> 2.U,
    RBYTEU  -> 0.U,
    RHALFU -> 1.U
  ))

  val awize = MuxLookup(io.in.bits.signals.lsu.mem_wmask, WWORD)(Seq(
    WBYTE -> 0.U,
    WHALF -> 1.U,
    WWORD -> 2.U
  ))


  val temp_addr = Wire(UInt(conf.xlen.W))
  val data_offset = Wire(UInt(log2Ceil(conf.xlen/8).W))
  temp_addr := io.in.bits.alu_result & (~(conf.xlen/8-1).U(conf.xlen.W))
  data_offset := io.in.bits.alu_result - temp_addr

  val mem_wmask = Wire(UInt(4.W))
  mem_wmask := io.in.bits.signals.lsu.mem_wmask << data_offset

  val store_data_shifted = io.in.bits.rd2 << (data_offset << 3)
  val rd_offset = io.dmem.rdata >> (data_offset << 3)  

  io.dmem.araddr := io.in.bits.alu_result
  io.dmem.awaddr := io.in.bits.alu_result
  io.dmem.wdata := store_data_shifted
  io.dmem.wstrb := mem_wmask
  io.dmem.arsize := arize
  io.dmem.awsize := awize

  val s_IDLE :: s_WAIT :: Nil = Enum(2)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(s_IDLE)

  val ar_done = RegInit(false.B)
  val aw_done = RegInit(false.B)
  val w_done = RegInit(false.B)

  val ar_fire = io.dmem.arvalid && io.dmem.arready
  val aw_fire = io.dmem.awvalid && io.dmem.awready
  val w_fire = io.dmem.wvalid && io.dmem.wready
  val r_fire = io.dmem.rvalid && io.dmem.rready
  val b_fire = io.dmem.bvalid && io.dmem.bready

  val addr_done = Mux(is_load,ar_done || ar_fire,Mux(is_store,(aw_done && w_done) || (aw_fire && w_fire),true.B))

  next_state := MuxLookup(state, s_IDLE)(Seq(
    s_IDLE -> Mux(addr_done && mem_valid, s_WAIT, s_IDLE),
    s_WAIT -> Mux((is_load && r_fire) || (is_store && b_fire), s_IDLE, s_WAIT)
  ))
  state := next_state

  when(state === s_IDLE){
    when(ar_fire){ ar_done := true.B }
    when(aw_fire){ aw_done := true.B }
    when(w_fire) { w_done := true.B }
  }

  when(state === s_WAIT && next_state === s_IDLE){
    ar_done := false.B
    aw_done := false.B
    w_done := false.B
  }

  io.dmem.arvalid := !ar_done && is_load && (state === s_IDLE)
  io.dmem.awvalid := !aw_done && is_store && (state === s_IDLE)
  io.dmem.wvalid := !w_done && is_store && (state === s_IDLE)

  io.dmem.rready := is_load && (state === s_WAIT)
  io.dmem.bready := is_store && (state === s_WAIT)

  val rbyte   = rd_offset(7, 0).asSInt.pad(32).asUInt
  val rhalf  = rd_offset(15, 0).asSInt.pad(32).asUInt
  val rword   = rd_offset(31, 0)
  val rbyteu  = Cat(0.U(24.W), rd_offset(7, 0))
  val rhalfu = Cat(0.U(16.W), rd_offset(15, 0))

  io.out.bits.mem_read := MuxLookup(io.in.bits.signals.lsu.mem_rd, rbyte)(Seq(
      RBYTE   -> rbyte,
      RHALF  -> rhalf,
      RWORD   -> rword,
      RBYTEU  -> rbyteu,
      RHALFU -> rhalfu
  ))

  //WBU
  io.out.bits.signals := io.in.bits.signals
  io.out.bits.rd1 := io.in.bits.rd1
  io.out.bits.alu_result := io.in.bits.alu_result
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.next_pc := io.in.bits.next_pc
  io.out.bits.imm_ext := io.in.bits.imm_ext
  io.out.bits.waddr := io.in.bits.waddr
  io.out.bits.is_ebreak := io.in.bits.is_ebreak
  io.out.bits.csr_rd1 := io.in.bits.csr_rd1
  io.out.bits.csr_waddr := io.in.bits.csr_waddr

  val ready = (state === s_IDLE && no_mem) || (is_load && r_fire) || (is_store && b_fire)

  io.in.ready := io.out.ready && ready
  io.out.valid := io.in.valid && ready
}
