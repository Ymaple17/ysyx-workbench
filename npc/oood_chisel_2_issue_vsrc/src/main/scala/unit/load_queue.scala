package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import common.MEM_READ._
import common.IRQ_CTRL._
import common.OoOParams
import core.{CoreConfig, LSU_WBU_IO, PM}
import core.PerfEvents._

class LoadQueueAlloc extends Bundle {
  val meta = new LSU_WBU_IO
  val addr = UInt(32.W)
  val memRd = UInt(3.W)
}

class LoadQueueEntry(indexWidth: Int) extends Bundle {
  val valid = Bool()
  val generation = UInt((4 - indexWidth).W)
  val meta = new LSU_WBU_IO
  val addr = UInt(32.W)
  val memRd = UInt(3.W)
  val state = UInt(2.W)
  val bypassedStores = UInt(OoOParams.ROB_SIZE.W)
}

class LoadQueueIO(depth: Int) extends Bundle {
  private val indexWidth = log2Ceil(depth)

  val alloc = Flipped(Decoupled(new LoadQueueAlloc))
  val wb = Decoupled(new LSU_WBU_IO)
  val dmem = new AXI4Master

  val robHead = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit0Valid = Input(Bool())
  val commit0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit1Valid = Input(Bool())
  val commit1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flush = Input(Bool())
  val flushIdx = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flushAll = Input(Bool())

  val queryValid = Output(Bool())
  val queryRob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val queryAddr = Output(UInt(32.W))
  val queryMemRd = Output(UInt(3.W))
  val fwdWait = Input(Bool())
  val fwdValid = Input(Bool())
  val fwdData = Input(UInt(32.W))
  val fwdUnknownOnly = Input(Bool())
  val fwdUnknownMask = Input(UInt(OoOParams.ROB_SIZE.W))
  val mmioReady = Input(Bool())

  val storeResolve0Valid = Input(Bool())
  val storeResolve0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve0Addr = Input(UInt(32.W))
  val storeResolve0Mask = Input(UInt(4.W))
  val storeResolve1Valid = Input(Bool())
  val storeResolve1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve1Addr = Input(UInt(32.W))
  val storeResolve1Mask = Input(UInt(4.W))

  val violationValid = Output(Bool())
  val violationRob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val violationPc = Output(UInt(32.W))
  val outstanding = Output(UInt(log2Ceil(depth + 1).W))
  val full = Output(Bool())
  val staleResp = Output(Bool())
  val debugHeadAllocPc = Output(UInt(32.W))
  val debugHeadRemoveReason = Output(UInt(2.W))
}

class LoadQueue(
    conf: CoreConfig,
    depth: Int = OoOParams.LQ_SIZE,
    speculateUnknownStores: Boolean = OoOParams.LQ_SPECULATE_UNKNOWN_STORES)
    extends Module {
  require(depth >= 2 && depth <= 8 && isPow2(depth))
  private val indexWidth = log2Ceil(depth)
  private val generationWidth = 4 - indexWidth
  require(generationWidth >= 1)

  val io = IO(new LoadQueueIO(depth))

  private val sWait :: sWaitResp :: sResult :: sComplete :: Nil = Enum(4)
  val entries = RegInit(VecInit(Seq.fill(depth)(0.U.asTypeOf(new LoadQueueEntry(indexWidth)))))
  val retryPtr = RegInit(0.U(indexWidth.W))
  val debugAllocPc = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(32.W))))
  val debugRemoveReason = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(2.W))))
  io.debugHeadAllocPc := debugAllocPc(io.robHead)
  io.debugHeadRemoveReason := debugRemoveReason(io.robHead)

  def age(idx: UInt): UInt =
    (idx - io.robHead)(OoOParams.ROB_PTR_W - 1, 0)

  def cacheable(addr: UInt): Bool =
    (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)

  def accessSize(memRd: UInt): UInt = MuxLookup(memRd, 2.U)(Seq(
    RBYTE -> 0.U,
    RHALF -> 1.U,
    RWORD -> 2.U,
    RBYTEU -> 0.U,
    RHALFU -> 1.U
  ))

  def extendLoad(raw: UInt, memRd: UInt): UInt = MuxLookup(memRd, raw)(Seq(
    RBYTE -> raw(7, 0).asSInt.pad(32).asUInt,
    RHALF -> raw(15, 0).asSInt.pad(32).asUInt,
    RWORD -> raw(31, 0),
    RBYTEU -> raw(7, 0).asUInt.pad(32),
    RHALFU -> raw(15, 0).asUInt.pad(32)
  ))

  def loadMask(memRd: UInt, addr: UInt): UInt = {
    val base = MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE -> "b0001".U(4.W),
      RHALF -> "b0011".U(4.W),
      RWORD -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << addr(1, 0))(3, 0)
  }

  def storeMask(mask: UInt, addr: UInt): UInt =
    (mask << addr(1, 0))(3, 0)

  def killedBySelectiveFlush(i: Int): Bool =
    io.flush && entries(i).valid && (age(entries(i).meta.rob_idx) > age(io.flushIdx))

  val alive = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    alive(i) := entries(i).valid && !io.flushAll && !killedBySelectiveFlush(i)
  }

  val validMask = VecInit(entries.map(e => !e.valid)).asUInt
  val hasFree = validMask.orR
  val allocIdx = PriorityEncoder(validMask)
  io.alloc.ready := hasFree && !io.flush && !io.flushAll
  io.full := !hasFree
  io.outstanding := PopCount(entries.map(_.valid))

  val schedEligible = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val mmioCanRun = cacheable(entries(i).addr) ||
      ((entries(i).meta.rob_idx === io.robHead) && io.mmioReady)
    schedEligible(i) := alive(i) && entries(i).state === sWait && mmioCanRun
  }
  val schedMask = schedEligible.asUInt
  val rotatedMask = (Cat(schedMask, schedMask) >> retryPtr)(depth - 1, 0)
  val entrySchedValid = schedMask.orR
  val schedOffset = PriorityEncoder(rotatedMask)
  val entrySchedIdx = (retryPtr + schedOffset)(indexWidth - 1, 0)
  val freshMmioCanRun = cacheable(io.alloc.bits.addr) ||
    ((io.alloc.bits.meta.rob_idx === io.robHead) && io.mmioReady)
  val freshSchedValid = !entrySchedValid && io.alloc.fire && freshMmioCanRun
  val freshEntry = WireDefault(entries(allocIdx))
  freshEntry.valid := true.B
  freshEntry.meta := io.alloc.bits.meta
  freshEntry.addr := io.alloc.bits.addr
  freshEntry.memRd := io.alloc.bits.memRd
  freshEntry.state := sWait
  freshEntry.bypassedStores := 0.U
  val schedValid = entrySchedValid || freshSchedValid
  val schedIdx = Mux(entrySchedValid, entrySchedIdx, allocIdx)
  val schedEntry = Mux(entrySchedValid, entries(entrySchedIdx), freshEntry)

  io.queryValid := schedValid
  io.queryRob := schedEntry.meta.rob_idx
  io.queryAddr := schedEntry.addr
  io.queryMemRd := schedEntry.memRd

  val resolvingUnknown =
    (io.storeResolve0Valid && io.fwdUnknownMask(io.storeResolve0Rob)) ||
      (io.storeResolve1Valid && io.fwdUnknownMask(io.storeResolve1Rob))
  val canBypassUnknown = speculateUnknownStores.B && io.fwdUnknownOnly && !resolvingUnknown
  val dependencyReady = !io.fwdWait || canBypassUnknown
  val schedulerForward = schedValid && dependencyReady && io.fwdValid
  val schedulerBus = schedValid && dependencyReady && !io.fwdValid

  io.dmem.arvalid := schedulerBus && !io.flush && !io.flushAll
  io.dmem.araddr := schedEntry.addr
  io.dmem.arsize := accessSize(schedEntry.memRd)
  io.dmem.arid := Cat(schedEntry.generation, schedIdx)
  io.dmem.arlen := 0.U
  io.dmem.arburst := 1.U
  val arFire = io.dmem.arvalid && io.dmem.arready

  io.dmem.awvalid := false.B
  io.dmem.awaddr := 0.U
  io.dmem.awsize := 0.U
  io.dmem.awid := 0.U
  io.dmem.awlen := 0.U
  io.dmem.awburst := 1.U
  io.dmem.wvalid := false.B
  io.dmem.wdata := 0.U
  io.dmem.wstrb := 0.U
  io.dmem.wlast := true.B
  io.dmem.bready := false.B

  val respIdx = io.dmem.rid(indexWidth - 1, 0)
  val respGeneration = io.dmem.rid(3, indexWidth)
  val respMatchesOutstanding = io.dmem.rvalid && alive(respIdx) &&
    entries(respIdx).state === sWaitResp &&
    entries(respIdx).generation === respGeneration
  val respMatchesCurrent = io.dmem.rvalid && arFire &&
    io.dmem.rid === io.dmem.arid && !respMatchesOutstanding
  val respMatches = respMatchesOutstanding || respMatchesCurrent
  io.staleResp := io.dmem.rvalid && !respMatches
  io.dmem.rready := true.B

  val responseFault = io.dmem.rresp =/= 0.U
  def responseMetaFor(entry: LoadQueueEntry): LSU_WBU_IO = {
    val meta = Wire(new LSU_WBU_IO)
    meta := entry.meta
    val offset = io.dmem.rdata >> (entry.addr(1, 0) << 3)
    meta.mem_read := extendLoad(offset, entry.memRd)
    meta.fwd_valid := false.B
    meta.fwd_data := 0.U
    meta.state.state := responseFault || entry.meta.state.state
    meta.state.state_num := Mux(responseFault, IRQ_LAF, entry.meta.state.state_num)
    meta
  }
  val outstandingResponseMeta = responseMetaFor(entries(respIdx))
  val currentResponseMeta = responseMetaFor(schedEntry)

  val forwardMeta = Wire(new LSU_WBU_IO)
  forwardMeta := schedEntry.meta
  forwardMeta.mem_read := extendLoad(io.fwdData, schedEntry.memRd)
  forwardMeta.fwd_valid := true.B
  forwardMeta.fwd_data := io.fwdData

  val resultCandidates = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    resultCandidates(i) := alive(i) && entries(i).state === sResult
  }
  val wbGrant = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val olderResult = (0 until depth).map { j =>
      resultCandidates(j) && (age(entries(j).meta.rob_idx) < age(entries(i).meta.rob_idx))
    }.foldLeft(false.B)(_ || _)
    wbGrant(i) := resultCandidates(i) && !olderResult
  }
  val wbValid = resultCandidates.asUInt.orR
  val wbIdx = OHToUInt(wbGrant)
  val responseIncoming = io.dmem.rvalid && io.dmem.rready && respMatches
  val responseIdx = Mux(respMatchesOutstanding, respIdx, schedIdx)
  val responseMeta = Mux(respMatchesOutstanding, outstandingResponseMeta, currentResponseMeta)
  val directResponse = !wbValid && responseIncoming
  val directForward = !wbValid && !responseIncoming && schedulerForward
  val directValid = directResponse || directForward
  val directIdx = Mux(directResponse, responseIdx, schedIdx)
  val directMeta = Mux(directResponse, responseMeta, forwardMeta)

  io.wb.valid := (wbValid || directValid) && !io.flushAll
  io.wb.bits := Mux(wbValid, entries(wbIdx).meta, directMeta)

  def violationForStore(
      entry: LoadQueueEntry,
      storeValid: Bool,
      storeRob: UInt,
      storeAddr: UInt,
      storeRawMask: UInt): Bool = {
    val active = entry.state === sWaitResp || entry.state === sResult || entry.state === sComplete
    val sameWord = entry.addr(31, 2) === storeAddr(31, 2)
    val overlap = (loadMask(entry.memRd, entry.addr) & storeMask(storeRawMask, storeAddr)) =/= 0.U
    storeValid && entry.valid && active && entry.bypassedStores(storeRob) && sameWord && overlap
  }

  val violationCandidates = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    violationCandidates(i) := entries(i).valid && (
      violationForStore(entries(i), io.storeResolve0Valid, io.storeResolve0Rob,
        io.storeResolve0Addr, io.storeResolve0Mask) ||
      violationForStore(entries(i), io.storeResolve1Valid, io.storeResolve1Rob,
        io.storeResolve1Addr, io.storeResolve1Mask))
  }
  val violationGrant = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val olderViolation = (0 until depth).map { j =>
      violationCandidates(j) && (age(entries(j).meta.rob_idx) < age(entries(i).meta.rob_idx))
    }.foldLeft(false.B)(_ || _)
    violationGrant(i) := violationCandidates(i) && !olderViolation
  }
  val violationIdx = OHToUInt(violationGrant)
  io.violationValid := violationCandidates.asUInt.orR
  io.violationRob := entries(violationIdx).meta.rob_idx
  io.violationPc := entries(violationIdx).meta.pc

  when(io.alloc.fire) {
    val duplicateAlloc = (0 until depth).map { i =>
      entries(i).valid &&
        (entries(i).meta.rob_idx === io.alloc.bits.meta.rob_idx) &&
        (entries(i).meta.pc === io.alloc.bits.meta.pc) &&
        (entries(i).meta.pdest === io.alloc.bits.meta.pdest)
    }.reduce(_ || _)
    assert(!duplicateAlloc, "duplicate dynamic load allocation")
    entries(allocIdx).valid := true.B
    entries(allocIdx).meta := io.alloc.bits.meta
    entries(allocIdx).addr := io.alloc.bits.addr
    entries(allocIdx).memRd := io.alloc.bits.memRd
    entries(allocIdx).state := sWait
    entries(allocIdx).bypassedStores := 0.U
    debugAllocPc(io.alloc.bits.meta.rob_idx) := io.alloc.bits.meta.pc
    debugRemoveReason(io.alloc.bits.meta.rob_idx) := 0.U
  }

  when(schedValid && (io.fwdWait || schedulerForward || schedulerBus)) {
    retryPtr := schedIdx + 1.U
  }
  when(schedulerForward) {
    entries(schedIdx).meta := forwardMeta
    when(!(directForward && io.wb.ready)) {
      entries(schedIdx).state := sResult
    }
  }
  when(arFire) {
    entries(schedIdx).state := sWaitResp
    entries(schedIdx).bypassedStores := Mux(canBypassUnknown, io.fwdUnknownMask, 0.U)
  }
  when(io.dmem.rvalid && io.dmem.rready && respMatchesOutstanding) {
    entries(respIdx).meta := outstandingResponseMeta
    when(!(directResponse && io.wb.ready)) {
      entries(respIdx).state := sResult
    }
  }
  when(io.dmem.rvalid && io.dmem.rready && respMatchesCurrent) {
    entries(schedIdx).meta := currentResponseMeta
    when(!(directResponse && io.wb.ready)) {
      entries(schedIdx).state := sResult
    }
  }
  when(io.wb.fire && wbValid) {
    debugRemoveReason(entries(wbIdx).meta.rob_idx) := 1.U
    if (speculateUnknownStores) {
      entries(wbIdx).state := sComplete
    } else {
      // Conservative loads never bypass an unresolved older store.  Once the
      // CDB accepts the result, the ROB owns completion and no violation state
      // remains in the LQ, so recycle the entry immediately.
      entries(wbIdx).valid := false.B
      entries(wbIdx).generation := entries(wbIdx).generation + 1.U
    }
  }
  when(io.wb.fire && directValid) {
    debugRemoveReason(directMeta.rob_idx) := 1.U
    if (speculateUnknownStores) {
      entries(directIdx).state := sComplete
    } else {
      entries(directIdx).valid := false.B
      entries(directIdx).generation := entries(directIdx).generation + 1.U
    }
  }

  when(io.commit0Valid) {
    for (i <- 0 until depth) {
      when(entries(i).valid && entries(i).meta.rob_idx === io.commit0Rob) {
        debugRemoveReason(io.commit0Rob) := 2.U
        entries(i).valid := false.B
        entries(i).generation := entries(i).generation + 1.U
      }
    }
  }
  when(io.commit1Valid) {
    for (i <- 0 until depth) {
      when(entries(i).valid && entries(i).meta.rob_idx === io.commit1Rob) {
        debugRemoveReason(io.commit1Rob) := 2.U
        entries(i).valid := false.B
        entries(i).generation := entries(i).generation + 1.U
      }
    }
  }

  when(io.flushAll) {
    for (i <- 0 until depth) {
      when(entries(i).valid) {
        entries(i).generation := entries(i).generation + 1.U
      }
      entries(i).valid := false.B
    }
  }.elsewhen(io.flush) {
    for (i <- 0 until depth) {
      when(killedBySelectiveFlush(i)) {
        debugRemoveReason(entries(i).meta.rob_idx) := 3.U
        entries(i).valid := false.B
        entries(i).generation := entries(i).generation + 1.U
      }
    }
  }

  val highWater = RegInit(0.U(log2Ceil(depth + 1).W))
  when(io.outstanding > highWater) {
    highWater := io.outstanding
  }

  if (conf.statistics) {
    PM(conf, clock, EVENT_LQ_ALLOC, 1.U, io.alloc.fire)
    PM(conf, clock, EVENT_LQ_FULL, 1.U, io.alloc.valid && !io.alloc.ready)
    PM(conf, clock, EVENT_LQ_HIGH_WATER, io.outstanding - highWater,
      io.outstanding > highWater)
    PM(conf, clock, EVENT_LOAD_REPLAY, 1.U,
      schedValid && ((io.fwdWait && !canBypassUnknown) || (schedulerBus && !io.dmem.arready)))
    PM(conf, clock, EVENT_REPLAY_STORE_WAIT, 1.U,
      schedValid && io.fwdWait && !canBypassUnknown)
    PM(conf, clock, EVENT_REPLAY_DCACHE_BUSY, 1.U,
      schedulerBus && !io.dmem.arready)
    PM(conf, clock, EVENT_REPLAY_CDB_BUSY, 1.U, io.wb.valid && !io.wb.ready)
    PM(conf, clock, EVENT_STALE_LOAD_RESP, 1.U, io.dmem.rvalid && io.dmem.rready && !respMatches)
    PM(conf, clock, EVENT_MEM_ORDER_VIOLATION, 1.U, io.violationValid)
  }
}
