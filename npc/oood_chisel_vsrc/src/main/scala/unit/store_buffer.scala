package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import common.MEM_READ._
import common.OoOParams

class StoreBufferEntry extends Bundle {
  val addr = UInt(32.W)
  val data = UInt(32.W)
  val mask = UInt(4.W)
}

class StoreBufferIO(size: Int) extends Bundle {
  val enq = Flipped(Decoupled(new StoreBufferEntry))
  val enq1 = Flipped(Decoupled(new StoreBufferEntry))

  val ld_valid  = Input(Bool())
  val ld_addr   = Input(UInt(32.W))
  val ld_mem_rd = Input(UInt(3.W))
  val ld_wait   = Output(Bool())
  val ld_fwd_valid = Output(Bool())
  val ld_fwd_data  = Output(UInt(32.W))

  val bus_busy = Input(Bool())
  val dmem = new AXI4Master

  val deq_valid = Output(Bool())
  val deq_addr  = Output(UInt(32.W))
  val deq_count = Output(UInt(log2Ceil(size + 1).W))
  val drain_valid = Output(Bool())
  val drain_addr = Output(UInt(32.W))
  val drain_data = Output(UInt(32.W))
  val drain_mask = Output(UInt(4.W))
  val merged = Output(UInt(2.W))
  val write_burst = Output(Bool())
  val write_beats = Output(UInt(4.W))

  val empty = Output(Bool())
  val full  = Output(Bool())
  val busy  = Output(Bool())
  val count = Output(UInt(log2Ceil(size + 1).W))
  val free = Output(UInt(log2Ceil(size + 1).W))
}

class StoreBuffer(
    size: Int = OoOParams.STORE_BUFFER_SIZE,
    gatherCycles: Int = 0) extends Module {
  require(size >= 2 && isPow2(size))
  require(gatherCycles >= 0)

  val io = IO(new StoreBufferIO(size))
  private val ptrW = log2Ceil(size)

  val entries = RegInit(VecInit(Seq.fill(size)(0.U.asTypeOf(new StoreBufferEntry))))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(size + 1).W))

  val sIdle :: sWrite :: sResp :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val awDone = RegInit(false.B)
  val wDone = RegInit(false.B)
  private val maxBurst = 8.min(size)
  private val burstW = log2Ceil(maxBurst + 1)
  val burstCount = RegInit(1.U(burstW.W))
  val consumeCount = RegInit(1.U(burstW.W))
  val burstFirstWord = RegInit(0.U(3.W))
  val writeBeat = RegInit(0.U(log2Ceil(maxBurst).max(1).W))
  private val gatherW = log2Ceil(gatherCycles + 1).max(1)
  val gatherAge = RegInit(0.U(gatherW.W))

  def expandMask(mask: UInt): UInt =
    Cat((3 to 0 by -1).map(i => Fill(8, mask(i))))

  def normalize(raw: StoreBufferEntry): StoreBufferEntry = {
    val out = Wire(new StoreBufferEntry)
    val off = raw.addr(1, 0)
    out.addr := raw.addr & "hffff_fffc".U
    out.data := raw.data << (off << 3)
    out.mask := (raw.mask << off)(3, 0)
    out
  }

  def merge(older: StoreBufferEntry, younger: StoreBufferEntry): StoreBufferEntry = {
    val out = Wire(new StoreBufferEntry)
    val youngBits = expandMask(younger.mask)
    out.addr := older.addr
    out.data := (older.data & ~youngBits) | (younger.data & youngBits)
    out.mask := older.mask | younger.mask
    out
  }

  def loadMaskBytes(memRd: UInt, addr: UInt): UInt = {
    val base = MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE  -> "b0001".U(4.W),
      RHALF  -> "b0011".U(4.W),
      RWORD  -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << addr(1, 0))(3, 0)
  }

  io.empty := count === 0.U
  io.full := count === size.U
  io.busy := state =/= sIdle
  io.count := count

  val in0 = normalize(io.enq.bits)
  val in1 = normalize(io.enq1.bits)
  val headEntry = entries(head)
  // Consume the oldest same-line prefix, even when its words arrived in a
  // non-monotonic order. AXI emits the minimal word span and uses WSTRB=0 for
  // holes, preserving FIFO order between cache lines while amortizing AW/B.
  val linePrefix = Wire(Vec(maxBurst, Bool()))
  for (off <- 0 until maxBurst) {
    val idx = (head + off.U)(ptrW - 1, 0)
    val entryMatches = off.U < count &&
      entries(idx).addr(31, 5) === headEntry.addr(31, 5)
    linePrefix(off) := entryMatches && (if (off == 0) true.B else linePrefix(off - 1))
  }
  val nextConsumeCount = PopCount(linePrefix)
  val prefixWords = (0 until maxBurst).map { off =>
    val idx = (head + off.U)(ptrW - 1, 0)
    entries(idx).addr(4, 2)
  }
  val minWord = (0 until maxBurst).map(off =>
    Mux(linePrefix(off), prefixWords(off), 7.U(3.W))).reduce((a, b) => Mux(a < b, a, b))
  val maxWord = (0 until maxBurst).map(off =>
    Mux(linePrefix(off), prefixWords(off), 0.U(3.W))).reduce((a, b) => Mux(a > b, a, b))
  val nextBurstCount = (maxWord - minWord) +& 1.U

  val bFire = io.dmem.bvalid && io.dmem.bready
  val deqFire = (state === sResp) && bFire
  val deqCount = Mux(deqFire, consumeCount, 0.U)
  val available = size.U - count + deqCount

  // Committed stores may combine across unrelated addresses. Loads scan the
  // whole buffer in age order, and an active AXI burst is immutable until B.
  // Choosing the youngest mutable match preserves per-address store order.
  val merge0Idx = WireDefault(0.U(ptrW.W))
  val merge1Idx = WireDefault(0.U(ptrW.W))
  val merge0Found = WireDefault(false.B)
  val merge1Found = WireDefault(false.B)
  for (off <- 0 until size) {
    val idx = (head + off.U)(ptrW - 1, 0)
    val mutable = off.U < count && !(state =/= sIdle && off.U < consumeCount)
    when(mutable && entries(idx).addr === in0.addr) {
      merge0Idx := idx
      merge0Found := true.B
    }
    when(mutable && entries(idx).addr === in1.addr) {
      merge1Idx := idx
      merge1Found := true.B
    }
  }
  val merge0Existing = io.enq.valid && merge0Found
  val pairSameWord = io.enq.valid && io.enq1.valid && in0.addr === in1.addr
  val merge1Existing = io.enq1.valid && !pairSameWord && merge1Found
  val slot0Needed = io.enq.valid && !merge0Existing
  val slot1Needed = io.enq1.valid && !pairSameWord && !merge1Existing
  val slotsNeeded = PopCount(Seq(slot0Needed, slot1Needed))
  val batchReady = slotsNeeded <= available
  io.enq.ready := batchReady
  io.enq1.ready := batchReady
  io.free := available

  val doEnq0 = io.enq.valid && batchReady
  val doEnq1 = io.enq1.valid && batchReady
  val acceptedSlots = Mux(batchReady, slotsNeeded, 0.U)
  io.merged := PopCount(Seq(
    doEnq0 && merge0Existing,
    doEnq1 && (pairSameWord || merge1Existing)))

  when(doEnq0) {
    when(merge0Existing) {
      entries(merge0Idx) := merge(entries(merge0Idx), in0)
    }.otherwise {
      entries(tail) := in0
    }
  }
  when(doEnq1) {
    when(pairSameWord) {
      when(merge0Existing) {
        entries(merge0Idx) := merge(merge(entries(merge0Idx), in0), in1)
      }.otherwise {
        entries(tail) := merge(in0, in1)
      }
    }.elsewhen(merge1Existing) {
      entries(merge1Idx) := merge(entries(merge1Idx), in1)
    }.otherwise {
      val tailAfter0 = tail + slot0Needed
      entries(tailAfter0(ptrW - 1, 0)) := in1
    }
  }

  when(acceptedSlots =/= 0.U) {
    tail := tail + acceptedSlots
  }
  when(deqFire) {
    head := head + consumeCount
  }
  count := count + acceptedSlots - deqCount

  io.deq_valid := deqFire
  io.deq_addr := headEntry.addr
  io.deq_count := deqCount
  val prefixSealed = count > nextConsumeCount
  val fullBurstReady = nextConsumeCount === maxBurst.U
  val nearFull = count >= (size - 4).max(1).U
  val gatherExpired = if (gatherCycles == 0) true.B else gatherAge === gatherCycles.U
  val burstStart = state === sIdle && !io.empty && !io.bus_busy &&
    (prefixSealed || fullBurstReady || nearFull || gatherExpired)
  io.write_burst := burstStart
  io.write_beats := Mux(burstStart, nextBurstCount, 0.U)

  when(state =/= sIdle || io.empty || burstStart) {
    gatherAge := 0.U
  }.elsewhen(gatherAge =/= gatherCycles.U) {
    gatherAge := gatherAge + 1.U
  }

  when(state === sIdle) {
    awDone := false.B
    wDone := false.B
    writeBeat := 0.U
    when(burstStart) {
      assert(nextBurstCount =/= 0.U, "a non-empty StoreBuffer must form a write burst")
      burstCount := nextBurstCount
      consumeCount := nextConsumeCount
      burstFirstWord := minWord
      state := sWrite
    }
  }.elsewhen(state === sWrite) {
    val awFire = io.dmem.awvalid && io.dmem.awready
    val wFire = io.dmem.wvalid && io.dmem.wready
    val lastWFire = wFire && writeBeat === (burstCount - 1.U)
    when(awFire) { awDone := true.B }
    when(wFire) {
      when(lastWFire) {
        wDone := true.B
      }.otherwise {
        writeBeat := writeBeat + 1.U
      }
    }
    when((awDone || awFire) && (wDone || lastWFire)) {
      state := sResp
    }
  }.elsewhen(state === sResp) {
    when(bFire) {
      state := sIdle
      awDone := false.B
      wDone := false.B
      writeBeat := 0.U
    }
  }

  io.dmem.araddr := 0.U
  io.dmem.arvalid := false.B
  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arsize := 0.U
  io.dmem.arburst := 0.U
  io.dmem.rready := false.B

  val lineBase = headEntry.addr & "hffff_ffe0".U
  val writeWord = burstFirstWord + writeBeat
  val writeAddr = lineBase + (writeWord << 2)
  val writeMatches = Wire(Vec(maxBurst, Bool()))
  for (off <- 0 until maxBurst) {
    val idx = (head + off.U)(ptrW - 1, 0)
    writeMatches(off) := off.U < consumeCount && entries(idx).addr(4, 2) === writeWord
  }
  val writeEntries = (0 until maxBurst).map(off => entries((head + off.U)(ptrW - 1, 0)))

  io.dmem.awaddr := lineBase + (burstFirstWord << 2)
  io.dmem.awvalid := (state === sWrite) && !awDone
  io.dmem.awid := 0.U
  io.dmem.awlen := burstCount - 1.U
  io.dmem.awsize := 2.U
  io.dmem.awburst := 1.U
  io.dmem.wdata := Mux1H(writeMatches, writeEntries.map(_.data))
  io.dmem.wstrb := Mux1H(writeMatches, writeEntries.map(_.mask))
  io.dmem.wvalid := (state === sWrite) && !wDone
  io.dmem.wlast := writeBeat === (burstCount - 1.U)
  io.dmem.bready := state === sResp
  io.drain_valid := io.dmem.wvalid && io.dmem.wready && io.dmem.wstrb.orR
  io.drain_addr := writeAddr
  io.drain_data := io.dmem.wdata
  io.drain_mask := io.dmem.wstrb

  when(!reset.asBool && state === sWrite) {
    assert(PopCount(writeMatches) <= 1.U,
      "same-line StoreBuffer prefix must contain at most one entry per word")
  }

  val mergedData = Wire(Vec(size + 1, UInt(32.W)))
  val mergedMask = Wire(Vec(size + 1, UInt(4.W)))
  mergedData(0) := 0.U
  mergedMask(0) := 0.U
  for (off <- 0 until size) {
    val idx = (head + off.U)(ptrW - 1, 0)
    val e = entries(idx)
    val hit = off.U < count && e.addr(31, 2) === io.ld_addr(31, 2)
    val bits = expandMask(e.mask)
    mergedData(off + 1) := Mux(hit,
      (mergedData(off) & ~bits) | (e.data & bits), mergedData(off))
    mergedMask(off + 1) := Mux(hit, mergedMask(off) | e.mask, mergedMask(off))
  }

  val loadMask = loadMaskBytes(io.ld_mem_rd, io.ld_addr)
  val overlap = (mergedMask(size) & loadMask) =/= 0.U
  val fullCover = (mergedMask(size) & loadMask) === loadMask
  io.ld_wait := io.ld_valid && overlap && !fullCover
  io.ld_fwd_valid := io.ld_valid && fullCover
  io.ld_fwd_data := mergedData(size) >> (io.ld_addr(1, 0) << 3)
}
