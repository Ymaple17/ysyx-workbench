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
  val partialData = UInt(32.W)
  val partialMask = UInt(4.W)
}

class LoadQueueIO(depth: Int) extends Bundle {
  private val indexWidth = log2Ceil(depth)

  val alloc = Flipped(Decoupled(new LoadQueueAlloc))
  val alloc1 = Flipped(Decoupled(new LoadQueueAlloc))
  val wb = Decoupled(new LSU_WBU_IO)
  val wb1 = Decoupled(new LSU_WBU_IO)
  val dmem = new AXI4Master
  val dmem1 = new AXI4Master

  val robHead = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit0Valid = Input(Bool())
  val commit0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit1Valid = Input(Bool())
  val commit1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit2Valid = Input(Bool())
  val commit2Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit3Valid = Input(Bool())
  val commit3Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
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
  val partialValid = Input(Bool())
  val partialData = Input(UInt(32.W))
  val partialMask = Input(UInt(4.W))
  val unknownValid = Input(Bool())
  val unknownMask = Input(UInt(OoOParams.ROB_SIZE.W))
  val query1Valid = Output(Bool())
  val query1Rob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val query1Addr = Output(UInt(32.W))
  val query1MemRd = Output(UInt(3.W))
  val fwd1Wait = Input(Bool())
  val fwd1Valid = Input(Bool())
  val fwd1Data = Input(UInt(32.W))
  val partial1Valid = Input(Bool())
  val partial1Data = Input(UInt(32.W))
  val partial1Mask = Input(UInt(4.W))
  val unknown1Valid = Input(Bool())
  val unknown1Mask = Input(UInt(OoOParams.ROB_SIZE.W))
  val unresolvedStores = Input(UInt(OoOParams.ROB_SIZE.W))
  val mmioReady = Input(Bool())

  val storeResolve0Valid = Input(Bool())
  val storeResolve0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve0Addr = Input(UInt(32.W))
  val storeResolve0Mask = Input(UInt(4.W))
  val storeResolve1Valid = Input(Bool())
  val storeResolve1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve1Addr = Input(UInt(32.W))
  val storeResolve1Mask = Input(UInt(4.W))
  val storeResolveHead = Input(UInt(OoOParams.ROB_PTR_W.W))

  val violationValid = Output(Bool())
  val violationRob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val violationPc = Output(UInt(32.W))
  val commitWait0 = Output(Bool())
  val commitWait1 = Output(Bool())
  val commitWait2 = Output(Bool())
  val commitWait3 = Output(Bool())
  val outstanding = Output(UInt(log2Ceil(depth + 1).W))
  val storeReplayCount = Output(UInt(2.W))
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
  val stalePending = RegInit(VecInit(Seq.fill(depth)(false.B)))
  val staleGeneration = Reg(Vec(depth, UInt(generationWidth.W)))
  val specTracker = Module(new SpecLoadTracker)
  val retryPtr = RegInit(0.U(indexWidth.W))
  val debugAllocPc = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(32.W))))
  val debugRemoveReason = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(2.W))))
  io.debugHeadAllocPc := debugAllocPc(io.robHead)
  io.debugHeadRemoveReason := debugRemoveReason(io.robHead)
  specTracker.io.robHead := io.robHead
  specTracker.io.storeResolve0Valid := io.storeResolve0Valid
  specTracker.io.storeResolve0Rob := io.storeResolve0Rob
  specTracker.io.storeResolve0Addr := io.storeResolve0Addr
  specTracker.io.storeResolve0Mask := io.storeResolve0Mask
  specTracker.io.storeResolve1Valid := io.storeResolve1Valid
  specTracker.io.storeResolve1Rob := io.storeResolve1Rob
  specTracker.io.storeResolve1Addr := io.storeResolve1Addr
  specTracker.io.storeResolve1Mask := io.storeResolve1Mask
  specTracker.io.storeResolveHead := io.storeResolveHead
  specTracker.io.unresolvedStores := io.unresolvedStores
  specTracker.io.commitValid := VecInit(Seq(io.commit0Valid, io.commit1Valid,
    io.commit2Valid, io.commit3Valid))
  specTracker.io.commitRob := VecInit(Seq(io.commit0Rob, io.commit1Rob,
    io.commit2Rob, io.commit3Rob))
  specTracker.io.flush := io.flush
  specTracker.io.flushIdx := io.flushIdx
  specTracker.io.flushAll := io.flushAll

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

  // A flushed request may still own a response in DCache/MSHR queues. Keep its
  // slot quarantined until that response drains so the finite AXI ID cannot
  // wrap through an ABA reuse and be mistaken for a new load.
  val validMask = VecInit((0 until depth).map(i =>
    !entries(i).valid && !stalePending(i))).asUInt
  val freeCount = PopCount(validMask)
  val hasFree = validMask.orR
  val canAllocate = !io.flush && !io.flushAll
  val bothAllocValid = io.alloc.valid && io.alloc1.valid
  val alloc0Older = age(io.alloc.bits.meta.rob_idx) <= age(io.alloc1.bits.meta.rob_idx)
  io.alloc.ready := hasFree && canAllocate &&
    (!bothAllocValid || freeCount >= 2.U || alloc0Older)
  io.alloc1.ready := hasFree && canAllocate &&
    (!bothAllocValid || freeCount >= 2.U || !alloc0Older)
  val allocIdx = PriorityEncoder(validMask)
  val freeAfter0 = validMask & ~Mux(io.alloc.fire,
    UIntToOH(allocIdx, depth), 0.U(depth.W))
  val alloc1Idx = PriorityEncoder(Mux(io.alloc.fire, freeAfter0, validMask))
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
  val sched1Eligible = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val mmioCanRun = cacheable(entries(i).addr) ||
      ((entries(i).meta.rob_idx === io.robHead) && io.mmioReady)
    sched1Eligible(i) := alive(i) && entries(i).state === sWait && mmioCanRun &&
      (!entrySchedValid || i.U =/= entrySchedIdx)
  }
  val sched1Mask = sched1Eligible.asUInt
  val rotated1Mask = (Cat(sched1Mask, sched1Mask) >> retryPtr)(depth - 1, 0)
  val entrySched1Valid = sched1Mask.orR
  val sched1Offset = PriorityEncoder(rotated1Mask)
  val entrySched1Idx = (retryPtr + sched1Offset)(indexWidth - 1, 0)
  val freshChoose1 = io.alloc1.fire && (!io.alloc.fire || !alloc0Older)
  val freshAllocValid = io.alloc.fire || io.alloc1.fire
  val freshAllocBits = Mux(freshChoose1, io.alloc1.bits, io.alloc.bits)
  val freshAllocIdx = Mux(freshChoose1, alloc1Idx, allocIdx)
  val freshMmioCanRun = cacheable(freshAllocBits.addr) ||
    ((freshAllocBits.meta.rob_idx === io.robHead) && io.mmioReady)
  val freshSchedValid = !entrySchedValid && freshAllocValid && freshMmioCanRun
  val freshEntry = WireDefault(entries(freshAllocIdx))
  freshEntry.valid := true.B
  freshEntry.meta := freshAllocBits.meta
  freshEntry.addr := freshAllocBits.addr
  freshEntry.memRd := freshAllocBits.memRd
  freshEntry.state := sWait
  freshEntry.bypassedStores := 0.U
  freshEntry.partialData := 0.U
  freshEntry.partialMask := 0.U
  val schedValid = entrySchedValid || freshSchedValid
  val schedIdx = Mux(entrySchedValid, entrySchedIdx, freshAllocIdx)
  val schedEntry = Mux(entrySchedValid, entries(entrySchedIdx), freshEntry)
  val fresh1Alloc0Valid = io.alloc.fire &&
    !(freshSchedValid && freshAllocIdx === allocIdx)
  val fresh1Alloc1Valid = io.alloc1.fire &&
    !(freshSchedValid && freshAllocIdx === alloc1Idx)
  val fresh1Choose1 = fresh1Alloc1Valid &&
    (!fresh1Alloc0Valid || !alloc0Older)
  val fresh1AllocValid = fresh1Alloc0Valid || fresh1Alloc1Valid
  val fresh1AllocBits = Mux(fresh1Choose1, io.alloc1.bits, io.alloc.bits)
  val fresh1AllocIdx = Mux(fresh1Choose1, alloc1Idx, allocIdx)
  val fresh1MmioCanRun = cacheable(fresh1AllocBits.addr) ||
    ((fresh1AllocBits.meta.rob_idx === io.robHead) && io.mmioReady)
  val freshSched1Valid = !entrySched1Valid && fresh1AllocValid && fresh1MmioCanRun
  val fresh1Entry = WireDefault(entries(fresh1AllocIdx))
  fresh1Entry.valid := true.B
  fresh1Entry.meta := fresh1AllocBits.meta
  fresh1Entry.addr := fresh1AllocBits.addr
  fresh1Entry.memRd := fresh1AllocBits.memRd
  fresh1Entry.state := sWait
  fresh1Entry.bypassedStores := 0.U
  fresh1Entry.partialData := 0.U
  fresh1Entry.partialMask := 0.U
  val sched1Valid = entrySched1Valid || freshSched1Valid
  val sched1Idx = Mux(entrySched1Valid, entrySched1Idx, fresh1AllocIdx)
  val sched1Entry = Mux(entrySched1Valid, entries(entrySched1Idx), fresh1Entry)

  io.queryValid := schedValid
  io.queryRob := schedEntry.meta.rob_idx
  io.queryAddr := schedEntry.addr
  io.queryMemRd := schedEntry.memRd
  io.query1Valid := sched1Valid
  io.query1Rob := sched1Entry.meta.rob_idx
  io.query1Addr := sched1Entry.addr
  io.query1MemRd := sched1Entry.memRd

  val resolvingUnknown =
    (io.storeResolve0Valid && io.unknownMask(io.storeResolve0Rob)) ||
      (io.storeResolve1Valid && io.unknownMask(io.storeResolve1Rob))
  val canBypassUnknown = speculateUnknownStores.B && io.unknownValid &&
    cacheable(schedEntry.addr) && !resolvingUnknown
  val dependencyReady = !io.fwdWait && (!io.unknownValid || canBypassUnknown)
  val schedulerForward = schedValid && dependencyReady && io.fwdValid
  val schedulerBus = schedValid && dependencyReady && !io.fwdValid
  val resolving1Unknown =
    (io.storeResolve0Valid && io.unknown1Mask(io.storeResolve0Rob)) ||
      (io.storeResolve1Valid && io.unknown1Mask(io.storeResolve1Rob))
  val canBypass1Unknown = speculateUnknownStores.B &&
    io.unknown1Valid && cacheable(sched1Entry.addr) && !resolving1Unknown
  val dependency1Ready = !io.fwd1Wait && (!io.unknown1Valid || canBypass1Unknown)
  val scheduler1Forward = sched1Valid && dependency1Ready && io.fwd1Valid
  val scheduler1Bus = sched1Valid && dependency1Ready && !io.fwd1Valid

  io.dmem.arvalid := schedulerBus && !io.flush && !io.flushAll
  io.dmem.araddr := schedEntry.addr
  io.dmem.arsize := accessSize(schedEntry.memRd)
  io.dmem.arid := Cat(schedEntry.generation, schedIdx)
  io.dmem.arlen := 0.U
  io.dmem.arburst := 1.U
  val arFire = io.dmem.arvalid && io.dmem.arready
  io.dmem1.arvalid := scheduler1Bus && !io.flush && !io.flushAll
  io.dmem1.araddr := sched1Entry.addr
  io.dmem1.arsize := accessSize(sched1Entry.memRd)
  io.dmem1.arid := Cat(sched1Entry.generation, sched1Idx)
  io.dmem1.arlen := 0.U
  io.dmem1.arburst := 1.U
  val arFire1 = io.dmem1.arvalid && io.dmem1.arready

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
  io.dmem1.awvalid := false.B
  io.dmem1.awaddr := 0.U
  io.dmem1.awsize := 0.U
  io.dmem1.awid := 0.U
  io.dmem1.awlen := 0.U
  io.dmem1.awburst := 1.U
  io.dmem1.wvalid := false.B
  io.dmem1.wdata := 0.U
  io.dmem1.wstrb := 0.U
  io.dmem1.wlast := true.B
  io.dmem1.bready := false.B

  val respIdx = io.dmem.rid(indexWidth - 1, 0)
  val respGeneration = io.dmem.rid(3, indexWidth)
  val respMatchesOutstanding = io.dmem.rvalid && alive(respIdx) &&
    entries(respIdx).state === sWaitResp &&
    entries(respIdx).generation === respGeneration
  val respMatchesCurrent = io.dmem.rvalid && arFire &&
    io.dmem.rid === io.dmem.arid && !respMatchesOutstanding
  val respMatches = respMatchesOutstanding || respMatchesCurrent
  val respMatchesStale = io.dmem.rvalid && stalePending(respIdx) &&
    staleGeneration(respIdx) === respGeneration
  io.dmem.rready := true.B

  val resp1Idx = io.dmem1.rid(indexWidth - 1, 0)
  val resp1Generation = io.dmem1.rid(3, indexWidth)
  val resp1MatchesOutstanding = io.dmem1.rvalid && alive(resp1Idx) &&
    entries(resp1Idx).state === sWaitResp &&
    entries(resp1Idx).generation === resp1Generation
  val resp1MatchesCurrent = io.dmem1.rvalid && arFire1 &&
    io.dmem1.rid === io.dmem1.arid && !resp1MatchesOutstanding
  val resp1Matches = resp1MatchesOutstanding || resp1MatchesCurrent
  val resp1MatchesStale = io.dmem1.rvalid && stalePending(resp1Idx) &&
    staleGeneration(resp1Idx) === resp1Generation
  io.staleResp := (io.dmem.rvalid && !respMatches) ||
    (io.dmem1.rvalid && !resp1Matches)
  io.dmem1.rready := true.B

  def responseMetaFor(
      entry: LoadQueueEntry,
      data: UInt,
      resp: UInt,
      partialData: UInt,
      partialMask: UInt): LSU_WBU_IO = {
    val meta = Wire(new LSU_WBU_IO)
    meta := entry.meta
    val partialBits = Cat((3 to 0 by -1).map(i => Fill(8, partialMask(i))))
    val merged = (data & ~partialBits) | (partialData & partialBits)
    val offset = merged >> (entry.addr(1, 0) << 3)
    meta.mem_read := extendLoad(offset, entry.memRd)
    meta.fwd_valid := false.B
    meta.fwd_data := 0.U
    val responseFault = resp =/= 0.U
    meta.state.state := responseFault || entry.meta.state.state
    meta.state.state_num := Mux(responseFault, IRQ_LAF, entry.meta.state.state_num)
    meta
  }
  val outstandingResponseMeta = responseMetaFor(entries(respIdx), io.dmem.rdata,
    io.dmem.rresp, entries(respIdx).partialData, entries(respIdx).partialMask)
  val currentResponseMeta = responseMetaFor(schedEntry, io.dmem.rdata, io.dmem.rresp,
    io.partialData, Mux(io.partialValid, io.partialMask, 0.U))
  val outstandingResponse1Meta = responseMetaFor(entries(resp1Idx),
    io.dmem1.rdata, io.dmem1.rresp, entries(resp1Idx).partialData,
    entries(resp1Idx).partialMask)
  val currentResponse1Meta = responseMetaFor(sched1Entry, io.dmem1.rdata,
    io.dmem1.rresp, io.partial1Data, Mux(io.partial1Valid, io.partial1Mask, 0.U))

  val forwardMeta = Wire(new LSU_WBU_IO)
  forwardMeta := schedEntry.meta
  forwardMeta.mem_read := extendLoad(io.fwdData, schedEntry.memRd)
  forwardMeta.fwd_valid := true.B
  forwardMeta.fwd_data := io.fwdData
  val forward1Meta = Wire(new LSU_WBU_IO)
  forward1Meta := sched1Entry.meta
  forward1Meta.mem_read := extendLoad(io.fwd1Data, sched1Entry.memRd)
  forward1Meta.fwd_valid := true.B
  forward1Meta.fwd_data := io.fwd1Data

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
  val wb1Grant = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val olderRemaining = (0 until depth).map { j =>
      resultCandidates(j) && !wbGrant(j) &&
        (age(entries(j).meta.rob_idx) < age(entries(i).meta.rob_idx))
    }.foldLeft(false.B)(_ || _)
    wb1Grant(i) := resultCandidates(i) && !wbGrant(i) && !olderRemaining
  }
  val wb1Valid = wb1Grant.asUInt.orR
  val wb1Idx = OHToUInt(wb1Grant)
  val responseIncoming = io.dmem.rvalid && io.dmem.rready && respMatches
  val responseIdx = Mux(respMatchesOutstanding, respIdx, schedIdx)
  val responseMeta = Mux(respMatchesOutstanding, outstandingResponseMeta, currentResponseMeta)
  val response1Incoming = io.dmem1.rvalid && io.dmem1.rready && resp1Matches
  val response1Idx = Mux(resp1MatchesOutstanding, resp1Idx, sched1Idx)
  val response1Meta = Mux(
    resp1MatchesOutstanding, outstandingResponse1Meta, currentResponse1Meta)
  val directResponse = !wbValid && responseIncoming
  val directForward = !wbValid && !responseIncoming && schedulerForward
  val directValid = directResponse || directForward
  val directIdx = Mux(directResponse, responseIdx, schedIdx)
  val directMeta = Mux(directResponse, responseMeta, forwardMeta)
  val direct1Response = !wb1Valid && response1Incoming
  val direct1Forward = !wb1Valid && !response1Incoming && scheduler1Forward
  val direct1Valid = direct1Response || direct1Forward
  val direct1Idx = Mux(direct1Response, response1Idx, sched1Idx)
  val direct1Meta = Mux(direct1Response, response1Meta, forward1Meta)
  // A zero-latency response can arrive in the same cycle as AR.  Preserve the
  // dependency snapshot captured by that AR before the direct result retires.
  val directBypassedStores = Mux(
    directResponse && respMatchesCurrent && arFire && canBypassUnknown,
    io.unknownMask,
    Mux(directForward && canBypassUnknown,
      io.unknownMask, entries(directIdx).bypassedStores))
  val direct1BypassedStores = Mux(
    direct1Response && resp1MatchesCurrent && arFire1 && canBypass1Unknown,
    io.unknown1Mask,
    Mux(direct1Forward && canBypass1Unknown,
      io.unknown1Mask, entries(direct1Idx).bypassedStores))
  val directEntry = Mux(
    directResponse && respMatchesOutstanding, entries(respIdx), schedEntry)
  val direct1Entry = Mux(
    direct1Response && resp1MatchesOutstanding, entries(resp1Idx), sched1Entry)
  val wbTrackEntry = Mux(wbValid, entries(wbIdx), directEntry)
  val wbTrackDependencies = Mux(
    wbValid, entries(wbIdx).bypassedStores, directBypassedStores)
  val wb1TrackEntry = Mux(wb1Valid, entries(wb1Idx), direct1Entry)
  val wb1TrackDependencies = Mux(
    wb1Valid, entries(wb1Idx).bypassedStores, direct1BypassedStores)

  specTracker.io.track0Valid := io.wb.fire && wbTrackDependencies.orR
  specTracker.io.track0.robIdx := io.wb.bits.rob_idx
  specTracker.io.track0.pc := io.wb.bits.pc
  specTracker.io.track0.addr := wbTrackEntry.addr
  specTracker.io.track0.memRd := wbTrackEntry.memRd
  specTracker.io.track0.dependencies := wbTrackDependencies
  specTracker.io.track1Valid := io.wb1.fire && wb1TrackDependencies.orR
  specTracker.io.track1.robIdx := io.wb1.bits.rob_idx
  specTracker.io.track1.pc := io.wb1.bits.pc
  specTracker.io.track1.addr := wb1TrackEntry.addr
  specTracker.io.track1.memRd := wb1TrackEntry.memRd
  specTracker.io.track1.dependencies := wb1TrackDependencies

  // SQ removes a store from unresolvedStores as soon as its address resolves,
  // while the LQ violation check observes that resolve through a delayed
  // sideband. Keep dependent loads from writing back during that one-cycle
  // gap, including a zero-latency response accepted in the same cycle.
  val resolvingStores =
    Mux(io.storeResolve0Valid,
      1.U(OoOParams.ROB_SIZE.W) << io.storeResolve0Rob,
      0.U(OoOParams.ROB_SIZE.W)) |
    Mux(io.storeResolve1Valid,
      1.U(OoOParams.ROB_SIZE.W) << io.storeResolve1Rob,
      0.U(OoOParams.ROB_SIZE.W))
  val dependencyStores = io.unresolvedStores | resolvingStores
  val wbSpecBlocked = wbValid &&
    (entries(wbIdx).bypassedStores & resolvingStores).orR
  val wb1SpecBlocked = wb1Valid &&
    (entries(wb1Idx).bypassedStores & resolvingStores).orR
  val directSpecBlocked = directValid &&
    (directBypassedStores & resolvingStores).orR
  val direct1SpecBlocked = direct1Valid &&
    (direct1BypassedStores & resolvingStores).orR

  io.wb.valid := (wbValid || directValid) && !io.flushAll &&
    !wbSpecBlocked && !directSpecBlocked
  io.wb.bits := Mux(wbValid, entries(wbIdx).meta, directMeta)
  io.wb1.valid := (wb1Valid || direct1Valid) && !io.flushAll &&
    !wb1SpecBlocked && !direct1SpecBlocked
  io.wb1.bits := Mux(wb1Valid, entries(wb1Idx).meta, direct1Meta)

  def violationForStore(
      entry: LoadQueueEntry,
      storeValid: Bool,
      storeRob: UInt,
      storeAddr: UInt,
      storeRawMask: UInt): Bool = {
    val active = entry.state === sWaitResp || entry.state === sResult
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
  val entryViolationValid = violationCandidates.asUInt.orR
  val entryViolationRob = entries(violationIdx).meta.rob_idx
  val trackerOlder = specTracker.io.violationValid &&
    (!entryViolationValid ||
      age(specTracker.io.violationRob) < age(entryViolationRob))
  io.violationValid := entryViolationValid || specTracker.io.violationValid
  io.violationRob := Mux(trackerOlder,
    specTracker.io.violationRob, entryViolationRob)
  io.violationPc := Mux(trackerOlder,
    specTracker.io.violationPc, entries(violationIdx).meta.pc)

  def commitWaitFor(rob: UInt): Bool =
    entries.map { entry =>
      entry.valid && entry.meta.rob_idx === rob &&
        (entry.bypassedStores & (io.unresolvedStores | resolvingStores)).orR
    }.reduce(_ || _)

  io.commitWait0 := commitWaitFor(io.commit0Rob) || specTracker.io.commitWait(0)
  io.commitWait1 := commitWaitFor(io.commit1Rob) || specTracker.io.commitWait(1)
  io.commitWait2 := commitWaitFor(io.commit2Rob) || specTracker.io.commitWait(2)
  io.commitWait3 := commitWaitFor(io.commit3Rob) || specTracker.io.commitWait(3)

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
    entries(allocIdx).partialData := 0.U
    entries(allocIdx).partialMask := 0.U
    debugAllocPc(io.alloc.bits.meta.rob_idx) := io.alloc.bits.meta.pc
    debugRemoveReason(io.alloc.bits.meta.rob_idx) := 0.U
  }
  when(io.alloc1.fire) {
    val duplicateAlloc = (0 until depth).map { i =>
      entries(i).valid &&
        (entries(i).meta.rob_idx === io.alloc1.bits.meta.rob_idx) &&
        (entries(i).meta.pc === io.alloc1.bits.meta.pc) &&
        (entries(i).meta.pdest === io.alloc1.bits.meta.pdest)
    }.reduce(_ || _) || (io.alloc.fire &&
      io.alloc.bits.meta.rob_idx === io.alloc1.bits.meta.rob_idx &&
      io.alloc.bits.meta.pc === io.alloc1.bits.meta.pc &&
      io.alloc.bits.meta.pdest === io.alloc1.bits.meta.pdest)
    assert(!duplicateAlloc, "duplicate dynamic load allocation on secondary port")
    entries(alloc1Idx).valid := true.B
    entries(alloc1Idx).meta := io.alloc1.bits.meta
    entries(alloc1Idx).addr := io.alloc1.bits.addr
    entries(alloc1Idx).memRd := io.alloc1.bits.memRd
    entries(alloc1Idx).state := sWait
    entries(alloc1Idx).bypassedStores := 0.U
    entries(alloc1Idx).partialData := 0.U
    entries(alloc1Idx).partialMask := 0.U
    debugAllocPc(io.alloc1.bits.meta.rob_idx) := io.alloc1.bits.meta.pc
    debugRemoveReason(io.alloc1.bits.meta.rob_idx) := 0.U
  }

  val schedAttempt = schedValid && (!dependencyReady || schedulerForward || schedulerBus)
  val sched1Attempt = sched1Valid && (!dependency1Ready || scheduler1Forward || scheduler1Bus)
  when(sched1Attempt) {
    retryPtr := sched1Idx + 1.U
  }.elsewhen(schedAttempt) {
    retryPtr := schedIdx + 1.U
  }
  when(schedulerForward) {
    entries(schedIdx).meta := forwardMeta
    entries(schedIdx).bypassedStores := Mux(canBypassUnknown, io.unknownMask, 0.U)
    when(!(directForward && io.wb.ready)) {
      entries(schedIdx).state := sResult
    }
  }
  when(arFire) {
    entries(schedIdx).state := sWaitResp
    entries(schedIdx).bypassedStores := Mux(canBypassUnknown, io.unknownMask, 0.U)
    entries(schedIdx).partialData := io.partialData
    entries(schedIdx).partialMask := Mux(io.partialValid, io.partialMask, 0.U)
  }
  when(scheduler1Forward) {
    entries(sched1Idx).meta := forward1Meta
    entries(sched1Idx).bypassedStores := Mux(canBypass1Unknown, io.unknown1Mask, 0.U)
    when(!(direct1Forward && io.wb1.ready)) {
      entries(sched1Idx).state := sResult
    }
  }
  when(arFire1) {
    entries(sched1Idx).state := sWaitResp
    entries(sched1Idx).bypassedStores := Mux(
      canBypass1Unknown, io.unknown1Mask, 0.U)
    entries(sched1Idx).partialData := io.partial1Data
    entries(sched1Idx).partialMask := Mux(io.partial1Valid, io.partial1Mask, 0.U)
  }
  when(io.dmem.rvalid && io.dmem.rready && respMatchesOutstanding) {
    entries(respIdx).meta := outstandingResponseMeta
    when(!(directResponse && io.wb.ready && !directSpecBlocked)) {
      entries(respIdx).state := sResult
    }
  }
  when(io.dmem.rvalid && io.dmem.rready && respMatchesCurrent) {
    entries(schedIdx).meta := currentResponseMeta
    when(!(directResponse && io.wb.ready && !directSpecBlocked)) {
      entries(schedIdx).state := sResult
    }
  }
  when(io.dmem1.rvalid && io.dmem1.rready && resp1MatchesOutstanding) {
    entries(resp1Idx).meta := outstandingResponse1Meta
    when(!(direct1Response && io.wb1.ready && !direct1SpecBlocked)) {
      entries(resp1Idx).state := sResult
    }
  }
  when(io.dmem1.rvalid && io.dmem1.rready && resp1MatchesCurrent) {
    entries(sched1Idx).meta := currentResponse1Meta
    when(!(direct1Response && io.wb1.ready && !direct1SpecBlocked)) {
      entries(sched1Idx).state := sResult
    }
  }
  when(io.wb.fire && wbValid) {
    debugRemoveReason(entries(wbIdx).meta.rob_idx) := 1.U
    entries(wbIdx).valid := false.B
    entries(wbIdx).generation := entries(wbIdx).generation + 1.U
  }
  when(io.wb1.fire && wb1Valid) {
    debugRemoveReason(entries(wb1Idx).meta.rob_idx) := 1.U
    entries(wb1Idx).valid := false.B
    entries(wb1Idx).generation := entries(wb1Idx).generation + 1.U
  }
  when(io.wb1.fire && direct1Valid) {
    debugRemoveReason(direct1Meta.rob_idx) := 1.U
    entries(direct1Idx).valid := false.B
    entries(direct1Idx).generation := entries(direct1Idx).generation + 1.U
  }
  when(io.wb.fire && directValid) {
    debugRemoveReason(directMeta.rob_idx) := 1.U
    entries(directIdx).valid := false.B
    entries(directIdx).generation := entries(directIdx).generation + 1.U
  }

  when(io.dmem.rvalid && io.dmem.rready && respMatchesStale) {
    stalePending(respIdx) := false.B
  }
  when(io.dmem1.rvalid && io.dmem1.rready && resp1MatchesStale) {
    stalePending(resp1Idx) := false.B
  }

  // Consume dependency bits after the resolve-side violation check above.
  // The ROB index is a slot, so leaving an old bit set would make a later
  // store reusing that slot look like the original dependency forever.
  when(resolvingStores.orR) {
    for (i <- 0 until depth) {
      when(entries(i).valid && (entries(i).bypassedStores & resolvingStores).orR) {
        entries(i).bypassedStores := entries(i).bypassedStores & ~resolvingStores
      }
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
  when(io.commit2Valid) {
    for (i <- 0 until depth) {
      when(entries(i).valid && entries(i).meta.rob_idx === io.commit2Rob) {
        debugRemoveReason(io.commit2Rob) := 2.U
        entries(i).valid := false.B
        entries(i).generation := entries(i).generation + 1.U
      }
    }
  }
  when(io.commit3Valid) {
    for (i <- 0 until depth) {
      when(entries(i).valid && entries(i).meta.rob_idx === io.commit3Rob) {
        debugRemoveReason(io.commit3Rob) := 2.U
        entries(i).valid := false.B
        entries(i).generation := entries(i).generation + 1.U
      }
    }
  }

  when(io.flushAll) {
    for (i <- 0 until depth) {
      when(entries(i).valid) {
        val responseArrives =
          (io.dmem.rvalid && respIdx === i.U &&
            respGeneration === entries(i).generation) ||
          (io.dmem1.rvalid && resp1Idx === i.U &&
            resp1Generation === entries(i).generation)
        when(entries(i).state === sWaitResp && !responseArrives) {
          stalePending(i) := true.B
          staleGeneration(i) := entries(i).generation
        }
        entries(i).generation := entries(i).generation + 1.U
      }
      entries(i).valid := false.B
    }
  }.elsewhen(io.flush) {
    for (i <- 0 until depth) {
      when(killedBySelectiveFlush(i)) {
        val responseArrives =
          (io.dmem.rvalid && respIdx === i.U &&
            respGeneration === entries(i).generation) ||
          (io.dmem1.rvalid && resp1Idx === i.U &&
            resp1Generation === entries(i).generation)
        when(entries(i).state === sWaitResp && !responseArrives) {
          stalePending(i) := true.B
          staleGeneration(i) := entries(i).generation
        }
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
    val allocCount = PopCount(Seq(io.alloc.fire, io.alloc1.fire))
    val allocStallCount = PopCount(Seq(
      io.alloc.valid && !io.alloc.ready,
      io.alloc1.valid && !io.alloc1.ready))
    PM(conf, clock, EVENT_LQ_ALLOC, allocCount, allocCount =/= 0.U)
    PM(conf, clock, EVENT_LQ_FULL, allocStallCount, allocStallCount =/= 0.U)
    PM(conf, clock, EVENT_LQ_HIGH_WATER, io.outstanding - highWater,
      io.outstanding > highWater)
    val replayStoreCount = io.storeReplayCount
    val replayDcacheCount = PopCount(Seq(
      schedulerBus && !io.dmem.arready,
      scheduler1Bus && !io.dmem1.arready))
    val replayCount = replayStoreCount +& replayDcacheCount
    val replayCdbCount = PopCount(Seq(
      io.wb.valid && !io.wb.ready,
      io.wb1.valid && !io.wb1.ready))
    val staleCount = PopCount(Seq(
      io.dmem.rvalid && io.dmem.rready && !respMatches,
      io.dmem1.rvalid && io.dmem1.rready && !resp1Matches))
    PM(conf, clock, EVENT_LOAD_REPLAY, replayCount, replayCount =/= 0.U)
    PM(conf, clock, EVENT_REPLAY_STORE_WAIT, replayStoreCount,
      replayStoreCount =/= 0.U)
    PM(conf, clock, EVENT_REPLAY_DCACHE_BUSY, replayDcacheCount,
      replayDcacheCount =/= 0.U)
    PM(conf, clock, EVENT_REPLAY_CDB_BUSY, replayCdbCount,
      replayCdbCount =/= 0.U)
    PM(conf, clock, EVENT_STALE_LOAD_RESP, staleCount, staleCount =/= 0.U)
    PM(conf, clock, EVENT_MEM_ORDER_VIOLATION, 1.U, io.violationValid)
  }

  io.storeReplayCount := PopCount(Seq(
    schedValid && !dependencyReady,
    sched1Valid && !dependency1Ready))
}
