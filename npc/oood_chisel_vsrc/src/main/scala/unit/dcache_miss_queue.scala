package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master

class DCacheMissReq extends Bundle {
  val addr = UInt(32.W)
  val size = UInt(3.W)
  val id = UInt(4.W)
  val port = Bool()
}

class DCacheMissQueueIO(words: Int, poisonPorts: Int) extends Bundle {
  val req = Flipped(Decoupled(new DCacheMissReq))
  val resp0 = Decoupled(new DCacheReadResp)
  val resp1 = Decoupled(new DCacheReadResp)
  val mem = new AXI4Master

  val poisonValid = Input(Vec(poisonPorts, Bool()))
  val poisonAddr = Input(Vec(poisonPorts, UInt(32.W)))

  val installValid = Output(Bool())
  val installAddr = Output(UInt(32.W))
  val installData = Output(Vec(words, UInt(32.W)))

  val busy = Output(Bool())
  val allocPulse = Output(Bool())
  val mergePulse = Output(Bool())
  val refillPulse = Output(Bool())
  val secondaryPulse = Output(Bool())
}

/**
  * Tagged, nonblocking DCache miss storage.
  *
  * Each entry owns one cache-line identity and several load waiters. The AXI
  * arbiter grants one burst at a time, but unstarted critical-word segments
  * take priority over refill tails. This preserves the simple downstream AXI
  * contract while allowing later misses to expose their critical word early.
  */
class DCacheMissQueue(
    blockSize: Int,
    entries: Int = 4,
    waiters: Int = 8,
    poisonPorts: Int = 6) extends Module {
  require(blockSize >= 4 && isPow2(blockSize))
  require(entries >= 2 && isPow2(entries))
  require(waiters >= 2 && isPow2(waiters))

  private val words = blockSize / 4
  private val wordW = log2Ceil(words).max(1)
  private val entryW = log2Ceil(entries)
  private val waiterW = log2Ceil(waiters)
  private val flatW = log2Ceil(entries * waiters)
  private val generationW = 4 - entryW
  require(generationW >= 1)

  val io = IO(new DCacheMissQueueIO(words, poisonPorts))

  private val sNeedAr :: sWaitR :: sComplete :: Nil = Enum(3)
  val valid = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val cacheable = Reg(Vec(entries, Bool()))
  val addr = Reg(Vec(entries, UInt(32.W)))
  val size = Reg(Vec(entries, UInt(3.W)))
  val generation = RegInit(VecInit(Seq.fill(entries)(0.U(generationW.W))))
  val state = RegInit(VecInit(Seq.fill(entries)(sNeedAr)))
  val secondSegment = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val fillIdx = Reg(Vec(entries, UInt(wordW.W)))
  val fillMask = RegInit(VecInit(Seq.fill(entries)(0.U(words.W))))
  val fillData = Reg(Vec(entries, Vec(words, UInt(32.W))))
  val refillResp = RegInit(VecInit(Seq.fill(entries)(0.U(2.W))))
  val killed = RegInit(VecInit(Seq.fill(entries)(false.B)))

  val waiterValid = RegInit(VecInit(Seq.fill(entries)(
    VecInit(Seq.fill(waiters)(false.B)))))
  val waiterPort = Reg(Vec(entries, Vec(waiters, Bool())))
  val waiterId = Reg(Vec(entries, Vec(waiters, UInt(4.W))))
  val waiterWord = Reg(Vec(entries, Vec(waiters, UInt(wordW.W))))

  def isCacheable(a: UInt): Bool =
    (a - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)
  def lineBase(a: UInt): UInt = a & ~((blockSize - 1).U(32.W))
  def wordOf(a: UInt): UInt =
    if (words == 1) 0.U(wordW.W) else a(log2Ceil(blockSize) - 1, 2)

  val poisonNow = Wire(Vec(entries, Bool()))
  for (i <- 0 until entries) {
    poisonNow(i) := valid(i) && cacheable(i) &&
      (0 until poisonPorts).map { p =>
        io.poisonValid(p) && isCacheable(io.poisonAddr(p)) &&
          lineBase(io.poisonAddr(p)) === lineBase(addr(i))
      }.foldLeft(false.B)(_ || _)
  }

  val reqCacheable = isCacheable(io.req.bits.addr)
  val matchVec = Wire(Vec(entries, Bool()))
  for (i <- 0 until entries) {
    matchVec(i) := valid(i) && !killed(i) && reqCacheable && cacheable(i) &&
      lineBase(addr(i)) === lineBase(io.req.bits.addr)
  }
  val matchValid = matchVec.asUInt.orR
  val matchIdx = OHToUInt(matchVec)
  val freeMask = VecInit(valid.map(!_)).asUInt
  val freeValid = freeMask.orR
  val freeIdx = PriorityEncoder(freeMask)
  val reqEntryIdx = Mux(matchValid, matchIdx, freeIdx)
  val waiterFreeMask = VecInit(waiterValid(reqEntryIdx).map(!_)).asUInt
  val waiterFree = waiterFreeMask.orR
  val waiterFreeIdx = PriorityEncoder(waiterFreeMask)
  io.req.ready := Mux(matchValid, waiterFree, freeValid)
  val reqFire = io.req.valid && io.req.ready

  io.allocPulse := reqFire && !matchValid
  io.mergePulse := reqFire && matchValid
  io.secondaryPulse := reqFire && io.req.bits.port

  when(reqFire) {
    when(!matchValid) {
      valid(reqEntryIdx) := true.B
      cacheable(reqEntryIdx) := reqCacheable
      addr(reqEntryIdx) := io.req.bits.addr
      size(reqEntryIdx) := io.req.bits.size
      state(reqEntryIdx) := sNeedAr
      secondSegment(reqEntryIdx) := false.B
      fillIdx(reqEntryIdx) := Mux(reqCacheable, wordOf(io.req.bits.addr), 0.U)
      fillMask(reqEntryIdx) := 0.U
      refillResp(reqEntryIdx) := 0.U
      killed(reqEntryIdx) := false.B
      for (w <- 0 until words) {
        fillData(reqEntryIdx)(w) := 0.U
      }
      for (w <- 0 until waiters) {
        waiterValid(reqEntryIdx)(w) := false.B
      }
      waiterValid(reqEntryIdx)(0) := true.B
      waiterPort(reqEntryIdx)(0) := io.req.bits.port
      waiterId(reqEntryIdx)(0) := io.req.bits.id
      waiterWord(reqEntryIdx)(0) := Mux(reqCacheable,
        wordOf(io.req.bits.addr), 0.U)
    }.otherwise {
      waiterValid(reqEntryIdx)(waiterFreeIdx) := true.B
      waiterPort(reqEntryIdx)(waiterFreeIdx) := io.req.bits.port
      waiterId(reqEntryIdx)(waiterFreeIdx) := io.req.bits.id
      waiterWord(reqEntryIdx)(waiterFreeIdx) := wordOf(io.req.bits.addr)
    }
  }

  val firstSegmentReady = Wire(Vec(entries, Bool()))
  val tailSegmentReady = Wire(Vec(entries, Bool()))
  for (i <- 0 until entries) {
    firstSegmentReady(i) := valid(i) && state(i) === sNeedAr && !secondSegment(i)
    tailSegmentReady(i) := valid(i) && state(i) === sNeedAr && secondSegment(i)
  }
  val arCandidates = Mux(firstSegmentReady.asUInt.orR,
    firstSegmentReady.asUInt, tailSegmentReady.asUInt)
  val activeValid = RegInit(false.B)
  val activeIdx = RegInit(0.U(entryW.W))
  val arGrantValid = arCandidates.orR && !activeValid
  val arGrantIdx = PriorityEncoder(arCandidates)
  val grantStartWord = Mux(secondSegment(arGrantIdx), 0.U, wordOf(addr(arGrantIdx)))
  val grantLen = if (words == 1) 0.U else Mux(cacheable(arGrantIdx),
    Mux(secondSegment(arGrantIdx), wordOf(addr(arGrantIdx)) - 1.U,
      (words - 1).U - wordOf(addr(arGrantIdx))), 0.U)
  val grantId = Cat(generation(arGrantIdx), arGrantIdx)

  val arFire = arGrantValid && io.mem.arready
  val rFire = io.mem.rvalid
  // Xbar permits only one downstream read burst at a time, and local MMIO
  // slaves such as Clint do not echo AXI RID. The unique active owner is
  // therefore authoritative; waiter IDs still route results back to the CPU.
  val rMatchesActive = activeValid
  // The simulation MMIO model may return R in the same cycle that AR fires.
  // Bind that response directly to the launching MSHR instead of waiting for
  // activeValid to become visible on the following cycle.
  val rMatchesLaunch = !activeValid && arFire
  val rMatches = rMatchesActive || rMatchesLaunch
  val responseEntry = Mux(rMatchesActive, activeIdx, arGrantIdx)

  val respQ0 = Module(new Queue(new DCacheReadResp, 4, pipe = true, flow = true))
  val respQ1 = Module(new Queue(new DCacheReadResp, 4, pipe = true, flow = true))
  io.resp0 <> respQ0.io.deq
  io.resp1 <> respQ1.io.deq

  val ready0 = Wire(Vec(entries * waiters, Bool()))
  val ready1 = Wire(Vec(entries * waiters, Bool()))
  for (i <- 0 until entries; w <- 0 until waiters) {
    val flat = i * waiters + w
    val currentBeatReady = rFire && rMatches && responseEntry === i.U &&
      fillIdx(i) === waiterWord(i)(w)
    val wordReady = fillMask(i)(waiterWord(i)(w)) || currentBeatReady
    // Poison prevents line installation. The load result itself remains valid:
    // older-store bytes were captured by SQ/StoreBuffer forwarding, while an
    // unknown-address conflict is recovered by SpecLoadTracker.
    val canRespond = valid(i) && waiterValid(i)(w) && wordReady
    ready0(flat) := canRespond && !waiterPort(i)(w)
    ready1(flat) := canRespond && waiterPort(i)(w)
  }
  val ready0Mask = ready0.asUInt
  val ready1Mask = ready1.asUInt
  val ready0Idx = PriorityEncoder(ready0Mask)
  val ready1Idx = PriorityEncoder(ready1Mask)
  val ready0Entry = ready0Idx(flatW - 1, waiterW)
  val ready1Entry = ready1Idx(flatW - 1, waiterW)
  val ready0Waiter = ready0Idx(waiterW - 1, 0)
  val ready1Waiter = ready1Idx(waiterW - 1, 0)

  respQ0.io.enq.valid := ready0Mask.orR
  val ready0CurrentBeat = rFire && rMatches && responseEntry === ready0Entry &&
    fillIdx(ready0Entry) === waiterWord(ready0Entry)(ready0Waiter)
  val ready1CurrentBeat = rFire && rMatches && responseEntry === ready1Entry &&
    fillIdx(ready1Entry) === waiterWord(ready1Entry)(ready1Waiter)
  respQ0.io.enq.bits.data := Mux(ready0CurrentBeat, io.mem.rdata,
    fillData(ready0Entry)(waiterWord(ready0Entry)(ready0Waiter)))
  respQ0.io.enq.bits.resp := Mux(ready0CurrentBeat && io.mem.rresp =/= 0.U,
    io.mem.rresp, refillResp(ready0Entry))
  respQ0.io.enq.bits.id := waiterId(ready0Entry)(ready0Waiter)
  respQ1.io.enq.valid := ready1Mask.orR
  respQ1.io.enq.bits.data := Mux(ready1CurrentBeat, io.mem.rdata,
    fillData(ready1Entry)(waiterWord(ready1Entry)(ready1Waiter)))
  respQ1.io.enq.bits.resp := Mux(ready1CurrentBeat && io.mem.rresp =/= 0.U,
    io.mem.rresp, refillResp(ready1Entry))
  respQ1.io.enq.bits.id := waiterId(ready1Entry)(ready1Waiter)

  when(respQ0.io.enq.fire) {
    waiterValid(ready0Entry)(ready0Waiter) := false.B
  }
  when(respQ1.io.enq.fire) {
    waiterValid(ready1Entry)(ready1Waiter) := false.B
  }

  io.mem.araddr := Mux(cacheable(arGrantIdx),
    lineBase(addr(arGrantIdx)) + (grantStartWord << 2), addr(arGrantIdx))
  io.mem.arvalid := arGrantValid
  io.mem.arid := grantId
  io.mem.arlen := grantLen
  io.mem.arsize := Mux(cacheable(arGrantIdx), 2.U, size(arGrantIdx))
  io.mem.arburst := 1.U
  io.mem.rready := true.B
  io.mem.awaddr := 0.U
  io.mem.awvalid := false.B
  io.mem.awid := 0.U
  io.mem.awlen := 0.U
  io.mem.awsize := 0.U
  io.mem.awburst := 0.U
  io.mem.wdata := 0.U
  io.mem.wstrb := 0.U
  io.mem.wvalid := false.B
  io.mem.wlast := false.B
  io.mem.bready := false.B

  when(io.mem.arvalid && io.mem.arready) {
    activeValid := true.B
    activeIdx := arGrantIdx
    state(arGrantIdx) := sWaitR
  }

  io.installValid := false.B
  io.installAddr := 0.U
  io.installData := VecInit(Seq.fill(words)(0.U(32.W)))
  io.refillPulse := false.B

  when(rFire && (activeValid || arFire)) {
    assert(rMatches, "DCache refill response must match the live MSHR generation")
  }
  for (i <- 0 until entries) {
    val hasWaiters = waiterValid(i).asUInt.orR
    val incomingWaiter = reqFire && reqEntryIdx === i.U
    when(valid(i) && poisonNow(i)) {
      killed(i) := true.B
    }
    when(valid(i) && state(i) === sComplete && !hasWaiters &&
        !incomingWaiter && !poisonNow(i)) {
      valid(i) := false.B
      generation(i) := generation(i) + 1.U
      fillMask(i) := 0.U
      killed(i) := false.B
    }
  }

  when(rFire && rMatches) {
    val nextLine = Wire(Vec(words, UInt(32.W)))
    nextLine := fillData(responseEntry)
    nextLine(fillIdx(responseEntry)) := io.mem.rdata
    fillData(responseEntry) := nextLine
    val nextMask = fillMask(responseEntry) |
      (1.U(words.W) << fillIdx(responseEntry))
    fillMask(responseEntry) := nextMask
    when(io.mem.rresp =/= 0.U) {
      refillResp(responseEntry) := io.mem.rresp
    }

    val firstDone = !secondSegment(responseEntry) &&
      Mux(cacheable(responseEntry), fillIdx(responseEntry) === (words - 1).U, true.B)
    val tailDone = secondSegment(responseEntry) &&
      fillIdx(responseEntry) === (wordOf(addr(responseEntry)) - 1.U)
    val lineDone = Mux(cacheable(responseEntry),
      Mux(wordOf(addr(responseEntry)) === 0.U, firstDone, tailDone), firstDone)
    val segmentDone = firstDone || tailDone

    when(segmentDone) {
      activeValid := false.B
      when(firstDone && cacheable(responseEntry) && wordOf(addr(responseEntry)) =/= 0.U) {
        secondSegment(responseEntry) := true.B
        fillIdx(responseEntry) := 0.U
        state(responseEntry) := sNeedAr
      }.elsewhen(lineDone) {
        io.refillPulse := cacheable(responseEntry)
        val poisoned = killed(responseEntry) || poisonNow(responseEntry)
        when(cacheable(responseEntry) && !poisoned &&
            Mux(io.mem.rresp =/= 0.U, io.mem.rresp, refillResp(responseEntry)) === 0.U) {
          io.installValid := true.B
          io.installAddr := addr(responseEntry)
          io.installData := nextLine
        }
        state(responseEntry) := sComplete
      }
    }.otherwise {
      fillIdx(responseEntry) := fillIdx(responseEntry) + 1.U
    }
  }

  io.busy := valid.asUInt.orR || activeValid ||
    respQ0.io.deq.valid || respQ1.io.deq.valid
}
