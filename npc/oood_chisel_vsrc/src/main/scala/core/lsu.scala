package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.IRQ_CTRL._
import unit.{LoadQueue, WBU_signals}
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
  val in1 = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  val out1 = Decoupled(new LSU_WBU_IO)
  val storeComplete = Valid(new LSU_WBU_IO)

  val dmem = new AXI4Master
  val dmem1 = new AXI4Master

  // Store-to-load forwarding result from the core-side SQ/StoreBuffer path.
  val st_fwd_valid = Input(Bool())
  val st_fwd_data = Input(UInt(32.W))
  val st_fwd_wait = Input(Bool())
  val st_partial_valid = Input(Bool())
  val st_partial_data = Input(UInt(32.W))
  val st_partial_mask = Input(UInt(4.W))
  val st_unknown_valid = Input(Bool())
  val st_unknown_mask = Input(UInt(OoOParams.ROB_SIZE.W))
  val st_fwd1_valid = Input(Bool())
  val st_fwd1_data = Input(UInt(32.W))
  val st_fwd1_wait = Input(Bool())
  val st_partial1_valid = Input(Bool())
  val st_partial1_data = Input(UInt(32.W))
  val st_partial1_mask = Input(UInt(4.W))
  val st_unknown1_valid = Input(Bool())
  val st_unknown1_mask = Input(UInt(OoOParams.ROB_SIZE.W))
  val st_unresolved_mask = Input(UInt(OoOParams.ROB_SIZE.W))

  val ld_query_valid = Output(Bool())
  val ld_query_rob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val ld_query_addr = Output(UInt(32.W))
  val ld_query_mem_rd = Output(UInt(3.W))
  val ld_query1_valid = Output(Bool())
  val ld_query1_rob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val ld_query1_addr = Output(UInt(32.W))
  val ld_query1_mem_rd = Output(UInt(3.W))

  val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit0_valid = Input(Bool())
  val commit0_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit1_valid = Input(Bool())
  val commit1_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit2_valid = Input(Bool())
  val commit2_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit3_valid = Input(Bool())
  val commit3_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flush = Input(Bool())
  val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flush_all = Input(Bool())
  val mmio_ready = Input(Bool())
  val mmio_drain_req = Output(Bool())

  val store_resolve0_valid = Input(Bool())
  val store_resolve0_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val store_resolve0_addr = Input(UInt(32.W))
  val store_resolve0_mask = Input(UInt(4.W))
  val store_resolve1_valid = Input(Bool())
  val store_resolve1_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val store_resolve1_addr = Input(UInt(32.W))
  val store_resolve1_mask = Input(UInt(4.W))
  val store_resolve_head = Input(UInt(OoOParams.ROB_PTR_W.W))

  val mem_violation_valid = Output(Bool())
  val mem_violation_rob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val mem_violation_pc = Output(UInt(32.W))
  val load_commit_wait0 = Output(Bool())
  val load_commit_wait1 = Output(Bool())
  val load_commit_wait2 = Output(Bool())
  val load_commit_wait3 = Output(Bool())
  val lq_outstanding = Output(UInt(log2Ceil(OoOParams.LQ_SIZE + 1).W))
  val bus_busy = Output(Bool())
  val debug_lq_head_alloc_pc = Output(UInt(32.W))
  val debug_lq_head_remove_reason = Output(UInt(2.W))
}

class LSU(val conf: CoreConfig) extends Module {
  val io = IO(new LSU_IO(conf.xlen))

  val is_load = !io.in.bits.signals.lsu.mem_write && io.in.bits.signals.lsu.mem_valid
  val is_load1 = !io.in1.bits.signals.lsu.mem_write && io.in1.bits.signals.lsu.mem_valid

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

  val inBase = makeBase(io.in.bits)
  val inBase1 = makeBase(io.in1.bits)
  val lq = Module(new LoadQueue(conf))
  lq.io.alloc.valid := io.in.valid && is_load
  lq.io.alloc.bits.meta := inBase
  lq.io.alloc.bits.addr := io.in.bits.alu_result
  lq.io.alloc.bits.memRd := io.in.bits.signals.lsu.mem_rd
  lq.io.alloc1.valid := io.in1.valid && is_load1
  lq.io.alloc1.bits.meta := inBase1
  lq.io.alloc1.bits.addr := io.in1.bits.alu_result
  lq.io.alloc1.bits.memRd := io.in1.bits.signals.lsu.mem_rd
  lq.io.robHead := io.rob_head
  lq.io.commit0Valid := io.commit0_valid
  lq.io.commit0Rob := io.commit0_rob
  lq.io.commit1Valid := io.commit1_valid
  lq.io.commit1Rob := io.commit1_rob
  lq.io.commit2Valid := io.commit2_valid
  lq.io.commit2Rob := io.commit2_rob
  lq.io.commit3Valid := io.commit3_valid
  lq.io.commit3Rob := io.commit3_rob
  lq.io.flush := io.flush
  lq.io.flushIdx := io.flush_idx
  lq.io.flushAll := io.flush_all
  lq.io.mmioReady := io.mmio_ready
  io.mmio_drain_req := lq.io.mmioDrainReq
  lq.io.fwdWait := io.st_fwd_wait
  lq.io.fwdValid := io.st_fwd_valid
  lq.io.fwdData := io.st_fwd_data
  lq.io.partialValid := io.st_partial_valid
  lq.io.partialData := io.st_partial_data
  lq.io.partialMask := io.st_partial_mask
  lq.io.unknownValid := io.st_unknown_valid
  lq.io.unknownMask := io.st_unknown_mask
  lq.io.fwd1Wait := io.st_fwd1_wait
  lq.io.fwd1Valid := io.st_fwd1_valid
  lq.io.fwd1Data := io.st_fwd1_data
  lq.io.partial1Valid := io.st_partial1_valid
  lq.io.partial1Data := io.st_partial1_data
  lq.io.partial1Mask := io.st_partial1_mask
  lq.io.unknown1Valid := io.st_unknown1_valid
  lq.io.unknown1Mask := io.st_unknown1_mask
  lq.io.unresolvedStores := io.st_unresolved_mask
  lq.io.storeResolve0Valid := io.store_resolve0_valid
  lq.io.storeResolve0Rob := io.store_resolve0_rob
  lq.io.storeResolve0Addr := io.store_resolve0_addr
  lq.io.storeResolve0Mask := io.store_resolve0_mask
  lq.io.storeResolve1Valid := io.store_resolve1_valid
  lq.io.storeResolve1Rob := io.store_resolve1_rob
  lq.io.storeResolve1Addr := io.store_resolve1_addr
  lq.io.storeResolve1Mask := io.store_resolve1_mask
  lq.io.storeResolveHead := io.store_resolve_head
  io.dmem <> lq.io.dmem
  io.dmem1 <> lq.io.dmem1

  io.ld_query_valid := lq.io.queryValid
  io.ld_query_rob := lq.io.queryRob
  io.ld_query_addr := lq.io.queryAddr
  io.ld_query_mem_rd := lq.io.queryMemRd
  io.ld_query1_valid := lq.io.query1Valid
  io.ld_query1_rob := lq.io.query1Rob
  io.ld_query1_addr := lq.io.query1Addr
  io.ld_query1_mem_rd := lq.io.query1MemRd
  io.mem_violation_valid := lq.io.violationValid
  io.mem_violation_rob := lq.io.violationRob
  io.mem_violation_pc := lq.io.violationPc
  io.load_commit_wait0 := lq.io.commitWait0
  io.load_commit_wait1 := lq.io.commitWait1
  io.load_commit_wait2 := lq.io.commitWait2
  io.load_commit_wait3 := lq.io.commitWait3
  io.lq_outstanding := lq.io.outstanding
  io.debug_lq_head_alloc_pc := lq.io.debugHeadAllocPc
  io.debug_lq_head_remove_reason := lq.io.debugHeadRemoveReason

  val directValid = io.in.valid && !is_load
  lq.io.wb.ready := io.out.ready
  lq.io.wb1.ready := io.out1.ready
  io.out.valid := lq.io.wb.valid
  io.out.bits := lq.io.wb.bits
  io.out1.valid := lq.io.wb1.valid
  io.out1.bits := lq.io.wb1.bits
  // Stores do not consume a data CDB.  A one-stage private completion pipe
  // keeps one-store-per-cycle throughput while cutting the recovery path from
  // the combinational LSU input back to issue/flush control.
  val storeCompleteValid = RegNext(directValid, false.B)
  val storeCompleteBits = RegEnable(inBase, directValid)
  io.storeComplete.valid := storeCompleteValid
  io.storeComplete.bits := storeCompleteBits
  io.in.ready := !io.in.valid || Mux(is_load,
    lq.io.alloc.ready, true.B)
  io.in1.ready := !io.in1.valid || (is_load1 && lq.io.alloc1.ready)

  when(!reset.asBool && io.in1.valid) {
    assert(is_load1, "the secondary LSU input is load-only")
  }

  io.bus_busy := lq.io.outstanding =/= 0.U || io.dmem.arvalid || io.dmem1.arvalid

  if (conf.statistics) {
    val readCount = PopCount(Seq(
      io.dmem.arvalid && io.dmem.arready,
      io.dmem1.arvalid && io.dmem1.arready))
    PM(conf, clock, EVENT_LSU_READ, readCount, readCount =/= 0.U)
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, false.B)
    PM(conf, clock, EVENT_LSU_LATENCY, lq.io.outstanding, lq.io.outstanding =/= 0.U)
    PM(conf, clock, EVENT_LSU_SQ_WAIT, lq.io.storeReplayCount,
      lq.io.storeReplayCount =/= 0.U)
    PM(conf, clock, EVENT_LSU_SQ_FORWARD, 1.U,
      lq.io.wb.valid && lq.io.wb.ready && lq.io.wb.bits.fwd_valid)
    PM(conf, clock, EVENT_LSU_BUS_WAIT, 1.U,
      lq.io.queryValid && !io.st_fwd_wait && !io.st_fwd_valid && !io.dmem.arready)
  }
}
