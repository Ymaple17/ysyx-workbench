package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.IRQ_CTRL._
import unit.WBU_signals
import common.OoOParams
import bus._
import core.PerfEvents._

class LSU_WBU_IO extends Bundle {
  val signals = new Bundle {
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
  val pdest = UInt(OoOParams.PHYS_W.W)
  val old_phys = UInt(OoOParams.PHYS_W.W)
  val do_rename = Bool()
  val br_taken = Bool()
  val store_data = UInt(32.W)
  val fwd_valid = Bool()
  val fwd_data = UInt(32.W)
}

class LSU_IO(xlen: Int) extends Bundle {
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)

  val dmem = new AXI4Master

  val is_flush = Input(Bool())

  // Store-to-load forwarding result from the core-side SQ/StoreBuffer path.
  val st_fwd_valid = Input(Bool())
  val st_fwd_data = Input(UInt(32.W))
  val st_fwd_wait = Input(Bool())
  val bus_busy = Output(Bool())
}

class LSU(val conf: CoreConfig) extends Module {
  val io = IO(new LSU_IO(conf.xlen))

  private val loadSlots = 2
  private val loadSlotW = log2Ceil(loadSlots)

  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arburst := 1.U
  io.dmem.awid := 0.U
  io.dmem.awlen := 0.U
  io.dmem.awburst := 1.U
  io.dmem.wlast := true.B
  // Stores are drained through the commit-side StoreBuffer/direct path.
  io.dmem.awvalid := false.B
  io.dmem.awaddr := 0.U
  io.dmem.awsize := 0.U
  io.dmem.wvalid := false.B
  io.dmem.wdata := 0.U
  io.dmem.wstrb := 0.U
  io.dmem.bready := false.B

  val is_load = !io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  val not_bus = !is_load

  val arsize = MuxLookup(io.in.bits.signals.lsu.mem_rd, RWORD)(Seq(
    RBYTE -> 0.U,
    RHALF -> 1.U,
    RWORD -> 2.U,
    RBYTEU -> 0.U,
    RHALFU -> 1.U
  ))

  val loadValid = RegInit(VecInit(Seq.fill(loadSlots)(false.B)))
  val loadMeta = Reg(Vec(loadSlots, new LSU_WBU_IO))
  val loadMemRd = Reg(Vec(loadSlots, UInt(3.W)))
  val loadAddr = Reg(Vec(loadSlots, UInt(32.W)))
  val freeMask = VecInit(loadValid.map(v => !v)).asUInt
  val hasFreeSlot = freeMask.orR
  val allocSlot = PriorityEncoder(freeMask)
  val respSlot = io.dmem.rid(loadSlotW - 1, 0)
  val respSlotValid = io.dmem.rvalid && loadValid(respSlot)
  val staleResp = io.dmem.rvalid && !loadValid(respSlot)

  def makeBase(in: EXU_LSU_IO): LSU_WBU_IO = {
    val b = Wire(new LSU_WBU_IO)
    b.signals.wbu := in.signals.wbu
    b.rd1 := in.rd1
    b.alu_result := in.alu_result
    b.pc := in.pc
    b.next_pc := in.next_pc
    b.imm_ext := in.imm_ext
    b.mem_read := 0.U
    b.waddr := in.waddr
    b.is_ebreak := in.is_ebreak
    b.csr_rd1 := in.csr_rd1
    b.csr_waddr := in.csr_waddr
    b.state := in.state
    b.rob_idx := in.rob_idx
    b.pdest := in.pdest
    b.old_phys := in.old_phys
    b.do_rename := in.do_rename
    b.br_taken := in.br_taken
    b.store_data := in.rd2
    b.fwd_valid := false.B
    b.fwd_data := 0.U
    b
  }

  def extendLoad(raw: UInt, memRd: UInt): UInt = {
    MuxLookup(memRd, raw)(Seq(
      RBYTE -> raw(7, 0).asSInt.pad(32).asUInt,
      RHALF -> raw(15, 0).asSInt.pad(32).asUInt,
      RWORD -> raw(31, 0),
      RBYTEU -> raw(7, 0).asUInt.pad(32),
      RHALFU -> raw(15, 0).asUInt.pad(32)
    ))
  }

  val inBase = makeBase(io.in.bits)
  val fwdDone = io.in.valid && is_load && io.st_fwd_valid && !io.st_fwd_wait && !io.is_flush
  val directDone = io.in.valid && !io.is_flush && (not_bus || fwdDone)
  val canIssueBusLoad =
    io.in.valid && is_load && !io.st_fwd_wait && !io.st_fwd_valid &&
    !io.is_flush && hasFreeSlot && !respSlotValid

  io.dmem.arvalid := canIssueBusLoad
  io.dmem.arsize := arsize
  io.dmem.araddr := io.in.bits.alu_result
  io.dmem.arid := allocSlot

  val arFire = io.dmem.arvalid && io.dmem.arready
  io.dmem.rready := (respSlotValid && io.out.ready) || staleResp

  val respByteOffset = loadAddr(respSlot)(1, 0)
  val respOffset = io.dmem.rdata >> (respByteOffset << 3)
  val respOut = Wire(new LSU_WBU_IO)
  respOut := loadMeta(respSlot)
  respOut.mem_read := extendLoad(respOffset, loadMemRd(respSlot))
  respOut.fwd_valid := false.B
  respOut.fwd_data := 0.U
  val hasLaf = io.dmem.rresp =/= 0.U
  respOut.state.state := hasLaf || loadMeta(respSlot).state.state
  respOut.state.state_num := Mux(hasLaf, IRQ_LAF, loadMeta(respSlot).state.state_num)

  val fwdExt = extendLoad(io.st_fwd_data, io.in.bits.signals.lsu.mem_rd)
  val directOut = Wire(new LSU_WBU_IO)
  directOut := inBase
  directOut.fwd_valid := fwdDone
  directOut.fwd_data := io.st_fwd_data
  directOut.mem_read := Mux(fwdDone, fwdExt, 0.U)

  io.out.valid := respSlotValid || directDone
  io.out.bits := Mux(respSlotValid, respOut, directOut)

  io.in.ready := MuxCase(false.B, Seq(
    (io.is_flush || !io.in.valid) -> true.B,
    respSlotValid -> false.B,
    (io.in.valid && is_load && io.st_fwd_wait) -> false.B,
    directDone -> io.out.ready,
    canIssueBusLoad -> io.dmem.arready
  ))

  when(arFire) {
    loadValid(allocSlot) := true.B
    loadMeta(allocSlot) := inBase
    loadMemRd(allocSlot) := io.in.bits.signals.lsu.mem_rd
    loadAddr(allocSlot) := io.in.bits.alu_result
  }

  when(io.dmem.rvalid && io.dmem.rready && loadValid(respSlot)) {
    loadValid(respSlot) := false.B
  }

  io.bus_busy := loadValid.asUInt.orR || io.dmem.arvalid

  if (conf.statistics) {
    PM(conf, clock, EVENT_LSU_READ, 1.U, arFire)
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, false.B)
    PM(conf, clock, EVENT_LSU_LATENCY, 1.U, loadValid.asUInt.orR && (!io.dmem.rvalid || io.out.ready))
    PM(conf, clock, EVENT_LSU_SQ_WAIT, 1.U, io.in.valid && is_load && io.st_fwd_wait && !io.is_flush)
    PM(conf, clock, EVENT_LSU_SQ_FORWARD, 1.U, directDone && io.out.ready && fwdDone)
    PM(conf, clock, EVENT_LSU_BUS_WAIT, 1.U, loadValid.asUInt.orR && !io.dmem.rvalid)
  }
}
