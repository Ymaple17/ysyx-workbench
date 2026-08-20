package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.MEM_WMASK._
import common.IRQ_CTRL._
import unit.WBU_signals
import common.OoOParams
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
  val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  val pdest   = UInt(OoOParams.PHYS_W.W)
  val old_phys = UInt(OoOParams.PHYS_W.W)
  val do_rename = Bool()
  val br_taken  = Bool()
  // 5：load 前递结果（覆盖 mem_read）；store 透传 rd2 供 ROB.mem_wdata
  val store_data = UInt(32.W)
  val fwd_valid  = Bool()
  val fwd_data   = UInt(32.W)
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)

  val dmem = new AXI4Master

  val is_flush = Input(Bool())

  // 5：来自 core 的 store-to-load 前递（组合）
  val st_fwd_valid = Input(Bool())
  val st_fwd_data  = Input(UInt(32.W))
  val st_fwd_wait  = Input(Bool()) // 更老 store 地址未齐或部分重叠 → 不发 ar
  val bus_busy = Output(Bool())    // 有未完成 load 事务 → commit 写需等待
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
  // 5a：store 不在 LSU 写内存 → 写通道恒静默（commit SM 占用）
  io.dmem.awvalid := false.B
  io.dmem.awaddr  := 0.U
  io.dmem.awsize  := 0.U
  io.dmem.wvalid  := false.B
  io.dmem.wdata   := 0.U
  io.dmem.wstrb   := 0.U
  io.dmem.bready  := false.B

  val is_load = !io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  val is_store = io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  // store：只算地址/数据，不发 AXI；与 ALU 一样单拍完成
  val not_bus = !is_load

  val arsize = MuxLookup(io.in.bits.signals.lsu.mem_rd, RWORD)(Seq(
    RBYTE  -> 0.U,
    RHALF  -> 1.U,
    RWORD  -> 2.U,
    RBYTEU -> 0.U,
    RHALFU -> 1.U
  ))

  val idle = Wire(Bool())
  val work = Wire(Bool())
  val ready = Wire(Bool())

  val s_IDLE  :: s_WORK :: s_FLUSH :: Nil = Enum(3)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(state)

  val ar_handshake_done = RegInit(false.B)

  when(state =/= s_IDLE || io.is_flush) {
    ar_handshake_done := false.B
  }

  val can_issue_load = is_load && !io.st_fwd_wait && !io.st_fwd_valid
  val load_handshake = can_issue_load && (ar_handshake_done || (io.dmem.arvalid && io.dmem.arready))
  val fwd_done = is_load && io.st_fwd_valid && !io.st_fwd_wait

  next_state := MuxLookup(state, s_IDLE)(Seq(
      s_IDLE  -> Mux(idle && load_handshake, Mux(work, s_IDLE, s_WORK), s_IDLE),
      s_WORK  -> Mux(work, s_IDLE, Mux(io.is_flush, s_FLUSH, s_WORK)),
      s_FLUSH -> Mux(io.dmem.rvalid, s_IDLE, s_FLUSH)
  ))
  state := next_state

  when(state === s_IDLE && idle) {
      when(can_issue_load && io.dmem.arvalid && io.dmem.arready) {
        ar_handshake_done := true.B
      }
  }

  when(io.is_flush || io.dmem.rvalid) {
    ar_handshake_done := false.B
  }

  io.dmem.arvalid := can_issue_load && (state === s_IDLE) && idle && !ar_handshake_done
  io.dmem.rready := (is_load && (state === s_WORK) && io.out.ready) || state === s_FLUSH
  io.dmem.arsize := arsize
  io.dmem.araddr := io.in.bits.alu_result

  val byte_offset = io.in.bits.alu_result(1, 0)
  val rd_offset = io.dmem.rdata >> (byte_offset << 3)

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
  io.out.bits.rob_idx := io.in.bits.rob_idx
  io.out.bits.pdest   := io.in.bits.pdest
  io.out.bits.old_phys := io.in.bits.old_phys
  io.out.bits.do_rename := io.in.bits.do_rename
  io.out.bits.br_taken  := io.in.bits.br_taken
  io.out.bits.store_data := io.in.bits.rd2
  io.out.bits.fwd_valid  := fwd_done
  io.out.bits.fwd_data   := io.st_fwd_data

  val rbyte   = rd_offset(7, 0).asSInt.pad(32).asUInt
  val rhalf   = rd_offset(15, 0).asSInt.pad(32).asUInt
  val rword   = rd_offset(31, 0)
  val rbyteu  = rd_offset(7, 0)
  val rhalfu  = rd_offset(15, 0)

  val mem_from_bus = MuxLookup(io.in.bits.signals.lsu.mem_rd, rbyte)(Seq(
      RBYTE  -> rbyte,
      RHALF  -> rhalf,
      RWORD  -> rword,
      RBYTEU -> rbyteu,
      RHALFU -> rhalfu
  ))
  // 前递也必须按 load 类型做符号/零扩展（否则 lbu 会拿到 0xffffff80）
  val fwd_off = io.st_fwd_data
  val fwd_ext = MuxLookup(io.in.bits.signals.lsu.mem_rd, fwd_off)(Seq(
      RBYTE  -> fwd_off(7, 0).asSInt.pad(32).asUInt,
      RHALF  -> fwd_off(15, 0).asSInt.pad(32).asUInt,
      RWORD  -> fwd_off(31, 0),
      RBYTEU -> fwd_off(7, 0).asUInt.pad(32),
      RHALFU -> fwd_off(15, 0).asUInt.pad(32)
  ))
  io.out.bits.mem_read := Mux(fwd_done, fwd_ext, mem_from_bus)

  val has_laf = io.dmem.rvalid && io.dmem.rresp =/= 0.U
  io.out.bits.state := io.in.bits.state
  io.out.bits.state.state := has_laf || io.in.bits.state.state
  io.out.bits.state.state_num := Mux(has_laf, IRQ_LAF, io.in.bits.state.state_num)

  idle := io.in.valid && !io.is_flush
  // store / 非访存：单拍；load 前递：单拍；load 等 store：不 ready；load 总线：等 rvalid
  ready := not_bus || fwd_done || io.dmem.rvalid
  work := ready && io.out.ready
  io.in.ready := !io.in.valid || (ready && io.out.ready)
  io.out.valid := io.in.valid && ready
  // AR 已发或在 WORK 等 R → 总线忙（commit 写必须等）
  io.bus_busy := (state === s_WORK) || (state === s_FLUSH) ||
    (is_load && idle && (ar_handshake_done || io.dmem.arvalid))

  if(conf.statistics){
    PM(conf, clock, EVENT_LSU_READ, 1.U, io.dmem.arvalid && io.dmem.arready)
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, false.B)
    PM(conf, clock, EVENT_LSU_LATENCY, 1.U, state === s_WORK && (!ready || io.out.ready))
  }
}
