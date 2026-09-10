package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master

class StoreWritebackLine extends Bundle {
  val lineAddr = UInt(32.W)
  val data = Vec(8, UInt(32.W))
  val masks = Vec(8, UInt(4.W))
}

class StoreWritebackQueueIO(depth: Int) extends Bundle {
  val enq = Flipped(Decoupled(new StoreWritebackLine))

  val ld_addr = Input(UInt(32.W))
  val ld_data = Output(UInt(32.W))
  val ld_mask = Output(UInt(4.W))
  val ld1_addr = Input(UInt(32.W))
  val ld1_data = Output(UInt(32.W))
  val ld1_mask = Output(UInt(4.W))
  val probe_line = Input(UInt(32.W))
  val probe_pending = Output(Bool())
  val probe1_line = Input(UInt(32.W))
  val probe1_pending = Output(Bool())

  val bus_busy = Input(Bool())
  val dmem = new AXI4Master

  val deq_valid = Output(Bool())
  val deq_addr = Output(UInt(32.W))
  val deq_count = Output(UInt(4.W))
  val drain_valid = Output(Bool())
  val drain_addr = Output(UInt(32.W))
  val drain_data = Output(UInt(32.W))
  val drain_mask = Output(UInt(4.W))
  val write_burst = Output(Bool())
  val write_beats = Output(UInt(4.W))
  val write_chain = Output(Bool())

  val empty = Output(Bool())
  val full = Output(Bool())
  val busy = Output(Bool())
  val count = Output(UInt(log2Ceil(depth + 1).W))
}

/**
  * Ordered ownership boundary between dirty-line combining and AXI writeback.
  * Entries are immutable snapshots, so the producer may immediately reuse its
  * combine slot while this queue retains forwarding and memory-order ownership.
  */
class StoreWritebackQueue(depth: Int = 4) extends Module {
  require(depth >= 2 && isPow2(depth))

  val io = IO(new StoreWritebackQueueIO(depth))
  private val ptrW = log2Ceil(depth)
  private val countW = log2Ceil(depth + 1)

  val lineAddr = RegInit(VecInit(Seq.fill(depth)(0.U(32.W))))
  val data = RegInit(VecInit(Seq.fill(depth)(VecInit(Seq.fill(8)(0.U(32.W))))))
  val masks = RegInit(VecInit(Seq.fill(depth)(VecInit(Seq.fill(8)(0.U(4.W))))))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(countW.W))

  val sIdle :: sWrite :: sResp :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val activeFirstWord = RegInit(0.U(3.W))
  val activeBurstCount = RegInit(1.U(4.W))
  val writeBeat = RegInit(0.U(3.W))
  val awDone = RegInit(false.B)
  val wDone = RegInit(false.B)

  def nextPtr(ptr: UInt): UInt = (ptr + 1.U)(ptrW - 1, 0)

  def firstWord(maskWords: Vec[UInt]): UInt =
    PriorityEncoder(VecInit((0 until 8).map(w => maskWords(w).orR)).asUInt)

  def lastWord(maskWords: Vec[UInt]): UInt =
    (0 until 8).map(w => Mux(maskWords(w).orR, w.U(3.W), 0.U(3.W)))
      .reduce((a, b) => Mux(a > b, a, b))

  val bFire = io.dmem.bvalid && io.dmem.bready
  val deqFire = state === sResp && bFire
  // Keep producer credit independent of BVALID. A full queue advertises its
  // released slot on the cycle after dequeue, avoiding an AXI B -> AW loop.
  io.enq.ready := count =/= depth.U

  when(deqFire) {
    head := nextPtr(head)
  }
  when(io.enq.fire) {
    lineAddr(tail) := io.enq.bits.lineAddr
    for (w <- 0 until 8) {
      data(tail)(w) := io.enq.bits.data(w)
      masks(tail)(w) := io.enq.bits.masks(w)
    }
    tail := nextPtr(tail)
  }
  count := count + io.enq.fire.asUInt - deqFire.asUInt

  val queueFirst = firstWord(masks(head))
  val queueLast = lastWord(masks(head))
  val queueBurstCount = (queueLast - queueFirst) +& 1.U

  // AXI ownership starts only from a registered snapshot. Current commit/B
  // traffic may enqueue a victim, but cannot alter AW/W payload combinationally.
  val burstStart = state === sIdle && !io.bus_busy && count =/= 0.U
  val launchFirst = queueFirst
  val launchCount = queueBurstCount
  val launchLine = lineAddr(head)

  val nextHead = nextPtr(head)
  val storedChainFirst = firstWord(masks(nextHead))
  val storedChainLast = lastWord(masks(nextHead))
  val storedChainCount = (storedChainLast - storedChainFirst) +& 1.U
  val chainCandidate = state === sResp && !io.bus_busy && count > 1.U
  val chainFirst = storedChainFirst
  val chainCount = storedChainCount
  val chainLine = lineAddr(nextHead)
  val chainHandoff = chainCandidate && bFire

  when(state === sIdle) {
    awDone := false.B
    wDone := false.B
    writeBeat := 0.U
    when(burstStart) {
      activeFirstWord := launchFirst
      activeBurstCount := launchCount
      val launchAwFire = io.dmem.awvalid && io.dmem.awready
      val launchWFire = io.dmem.wvalid && io.dmem.wready
      val launchLastW = launchWFire && launchCount === 1.U
      awDone := launchAwFire
      wDone := launchLastW
      when(launchWFire && !launchLastW) {
        writeBeat := 1.U
      }
      state := Mux(launchAwFire && launchLastW, sResp, sWrite)
    }
  }.elsewhen(state === sWrite) {
    val awFire = io.dmem.awvalid && io.dmem.awready
    val wFire = io.dmem.wvalid && io.dmem.wready
    val lastWFire = wFire && writeBeat === activeBurstCount - 1.U
    when(awFire) { awDone := true.B }
    when(wFire) {
      when(lastWFire) { wDone := true.B }
        .otherwise { writeBeat := writeBeat + 1.U }
    }
    when((awDone || awFire) && (wDone || lastWFire)) {
      state := sResp
    }
  }.otherwise {
    when(bFire) {
      when(chainCandidate) {
        activeFirstWord := chainFirst
        activeBurstCount := chainCount
        awDone := io.dmem.awvalid && io.dmem.awready
        wDone := false.B
        writeBeat := 0.U
        state := sWrite
      }.otherwise {
        awDone := false.B
        wDone := false.B
        writeBeat := 0.U
        state := sIdle
      }
    }
  }

  val launching = state === sIdle && burstStart
  val chainLaunching = state === sResp && chainCandidate
  val busFirst = Mux(chainLaunching, chainFirst,
    Mux(launching, launchFirst, activeFirstWord))
  val busCount = Mux(chainLaunching, chainCount,
    Mux(launching, launchCount, activeBurstCount))
  val busBeat = Mux(launching, 0.U, writeBeat)
  val busWord = busFirst + busBeat
  val busLine = Mux(chainLaunching, chainLine,
    Mux(launching, launchLine, lineAddr(head)))
  val busData = data(head)(busWord)
  val busMask = masks(head)(busWord)

  io.dmem.araddr := 0.U
  io.dmem.arvalid := false.B
  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arsize := 0.U
  io.dmem.arburst := 0.U
  io.dmem.rready := false.B
  io.dmem.awaddr := busLine + (busFirst << 2)
  io.dmem.awvalid := (state === sWrite && !awDone) || launching || chainLaunching
  io.dmem.awid := 0.U
  io.dmem.awlen := busCount - 1.U
  io.dmem.awsize := 2.U
  io.dmem.awburst := 1.U
  io.dmem.wdata := busData
  io.dmem.wstrb := busMask
  io.dmem.wvalid := (state === sWrite && !wDone) || launching
  io.dmem.wlast := busBeat === busCount - 1.U
  io.dmem.bready := state === sResp

  io.write_burst := burstStart || chainHandoff
  io.write_beats := Mux(chainHandoff, chainCount,
    Mux(burstStart, launchCount, 0.U))
  io.write_chain := chainHandoff
  io.deq_valid := deqFire
  io.deq_addr := lineAddr(head)
  io.deq_count := PopCount(masks(head).map(_.orR))
  io.drain_valid := io.dmem.wvalid && io.dmem.wready && io.dmem.wstrb.orR
  io.drain_addr := busLine + (busWord << 2)
  io.drain_data := io.dmem.wdata
  io.drain_mask := io.dmem.wstrb

  def loadQuery(addr: UInt): (UInt, UInt) = {
    val queryLine = addr & "hffff_ffe0".U
    val queryWord = addr(4, 2)
    val mergedData = Wire(Vec(depth + 1, UInt(32.W)))
    val mergedMask = Wire(Vec(depth + 1, UInt(4.W)))
    mergedData(0) := 0.U
    mergedMask(0) := 0.U
    for (offset <- 0 until depth) {
      val idx = (head + offset.U)(ptrW - 1, 0)
      val hit = offset.U < count && lineAddr(idx) === queryLine
      val bits = Cat((3 to 0 by -1).map(i => Fill(8, masks(idx)(queryWord)(i))))
      mergedData(offset + 1) := Mux(hit,
        (mergedData(offset) & ~bits) | (data(idx)(queryWord) & bits),
        mergedData(offset))
      mergedMask(offset + 1) := Mux(hit,
        mergedMask(offset) | masks(idx)(queryWord), mergedMask(offset))
    }
    (mergedData(depth), mergedMask(depth))
  }

  val query0 = loadQuery(io.ld_addr)
  io.ld_data := query0._1
  io.ld_mask := query0._2
  val query1 = loadQuery(io.ld1_addr)
  io.ld1_data := query1._1
  io.ld1_mask := query1._2
  def linePending(queryLine: UInt): Bool =
    (0 until depth).map { offset =>
      val idx = (head + offset.U)(ptrW - 1, 0)
      offset.U < count && lineAddr(idx) === queryLine
    }.foldLeft(false.B)(_ || _)
  io.probe_pending := linePending(io.probe_line)
  io.probe1_pending := linePending(io.probe1_line)

  io.empty := count === 0.U
  io.full := count === depth.U
  io.busy := state =/= sIdle
  io.count := count

  when(!reset.asBool) {
    assert(count <= depth.U, "Store writeback queue count overflow")
    assert(!(state =/= sIdle && count === 0.U),
      "an active Store writeback must retain a queue owner")
  }
}
