package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import common.MEM_READ._

class WriteCombiningStoreBufferIO(lines: Int) extends Bundle {
  val enq = Flipped(Decoupled(new StoreBufferEntry))
  val enq1 = Flipped(Decoupled(new StoreBufferEntry))

  val ld_valid = Input(Bool())
  val ld_addr = Input(UInt(32.W))
  val ld_mem_rd = Input(UInt(3.W))
  val ld_wait = Output(Bool())
  val ld_fwd_valid = Output(Bool())
  val ld_fwd_data = Output(UInt(32.W))
  val ld_partial_valid = Output(Bool())
  val ld_partial_data = Output(UInt(32.W))
  val ld_partial_mask = Output(UInt(4.W))
  val ld1_valid = Input(Bool())
  val ld1_addr = Input(UInt(32.W))
  val ld1_mem_rd = Input(UInt(3.W))
  val ld1_wait = Output(Bool())
  val ld1_fwd_valid = Output(Bool())
  val ld1_fwd_data = Output(UInt(32.W))
  val ld1_partial_valid = Output(Bool())
  val ld1_partial_data = Output(UInt(32.W))
  val ld1_partial_mask = Output(UInt(4.W))

  val bus_busy = Input(Bool())
  val drain_all = Input(Bool())
  val dmem = new AXI4Master

  val deq_valid = Output(Bool())
  val deq_addr = Output(UInt(32.W))
  val deq_count = Output(UInt(4.W))
  val drain_valid = Output(Bool())
  val drain_addr = Output(UInt(32.W))
  val drain_data = Output(UInt(32.W))
  val drain_mask = Output(UInt(4.W))
  val merged = Output(UInt(2.W))
  val write_burst = Output(Bool())
  val write_beats = Output(UInt(4.W))
  val write_chain = Output(Bool())

  val empty = Output(Bool())
  val full = Output(Bool())
  val busy = Output(Bool())
  val count = Output(UInt(log2Ceil(lines + 1).W))
  val free = Output(UInt(log2Ceil(lines + 1).W))
}

class WriteCombiningStoreBuffer(lines: Int = 16, retentionCycles: Int = 64)
    extends Module {
  require(lines >= 4 && isPow2(lines))
  require(retentionCycles >= 1)

  val io = IO(new WriteCombiningStoreBufferIO(lines))
  private val idxW = log2Ceil(lines)
  private val ageW = log2Ceil(retentionCycles + 1).max(1)
  private val pressureThreshold = (lines - 2).max(1)

  val valid = RegInit(VecInit(Seq.fill(lines)(false.B)))
  val lineAddr = RegInit(VecInit(Seq.fill(lines)(0.U(32.W))))
  val data = RegInit(VecInit(Seq.fill(lines)(VecInit(Seq.fill(8)(0.U(32.W))))))
  val masks = RegInit(VecInit(Seq.fill(lines)(VecInit(Seq.fill(8)(0.U(4.W))))))
  val age = RegInit(VecInit(Seq.fill(lines)(0.U(ageW.W))))
  val count = RegInit(0.U(log2Ceil(lines + 1).W))

  val sIdle :: sWrite :: sResp :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val activeIdx = RegInit(0.U(idxW.W))
  val activeFirstWord = RegInit(0.U(3.W))
  val activeBurstCount = RegInit(1.U(4.W))
  val writeBeat = RegInit(0.U(3.W))
  val awDone = RegInit(false.B)
  val wDone = RegInit(false.B)

  def expandMask(mask: UInt): UInt =
    Cat((3 to 0 by -1).map(i => Fill(8, mask(i))))

  def normalizedData(raw: StoreBufferEntry): UInt =
    raw.data << (raw.addr(1, 0) << 3)

  def normalizedMask(raw: StoreBufferEntry): UInt =
    (raw.mask << raw.addr(1, 0))(3, 0)

  def mergedData(oldData: UInt, oldMask: UInt, newData: UInt, newMask: UInt): UInt = {
    val newBits = expandMask(newMask)
    (oldData & ~newBits) | (newData & newBits)
  }

  def loadMaskBytes(memRd: UInt, addr: UInt): UInt = {
    val base = MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE -> "b0001".U(4.W),
      RHALF -> "b0011".U(4.W),
      RWORD -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << addr(1, 0))(3, 0)
  }

  def oldestOH(eligible: Vec[Bool]): Vec[Bool] = {
    val selected = Wire(Vec(lines, Bool()))
    for (i <- 0 until lines) {
      val olderExists = (0 until lines).map { j =>
        eligible(j) && ((age(j) > age(i)) ||
          (age(j) === age(i) && j.U < i.U))
      }.foldLeft(false.B)(_ || _)
      selected(i) := eligible(i) && !olderExists
    }
    selected
  }

  def firstWord(idx: UInt): UInt =
    PriorityEncoder(VecInit((0 until 8).map(w => masks(idx)(w).orR)).asUInt)

  def lastWord(idx: UInt): UInt =
    (0 until 8).map(w => Mux(masks(idx)(w).orR, w.U(3.W), 0.U(3.W)))
      .reduce((a, b) => Mux(a > b, a, b))

  val bFire = io.dmem.bvalid && io.dmem.bready
  val deqFire = state === sResp && bFire
  val activeValid = state =/= sIdle && valid(activeIdx)

  val idleEligible = Wire(Vec(lines, Bool()))
  val chainEligible = Wire(Vec(lines, Bool()))
  for (i <- 0 until lines) {
    idleEligible(i) := valid(i)
    chainEligible(i) := valid(i) && !(activeValid && activeIdx === i.U)
  }
  val idleVictimOH = oldestOH(idleEligible)
  val idleVictimValid = idleVictimOH.asUInt.orR
  val idleVictimIdx = PriorityEncoder(idleVictimOH.asUInt)
  val chainVictimOH = oldestOH(chainEligible)
  val chainVictimValid = chainVictimOH.asUInt.orR
  val chainVictimIdx = PriorityEncoder(chainVictimOH.asUInt)
  val expired = VecInit((0 until lines).map(i => valid(i) && age(i) === retentionCycles.U))
  val chainExpired = VecInit((0 until lines).map(i =>
    chainEligible(i) && age(i) === retentionCycles.U))
  val drainNeeded = io.drain_all || count >= pressureThreshold.U || expired.asUInt.orR
  val countAfterDeq = Mux(count =/= 0.U, count - 1.U, 0.U)
  val chainDrainNeeded = io.drain_all || countAfterDeq >= pressureThreshold.U ||
    chainExpired.asUInt.orR
  val burstStart = state === sIdle && !io.bus_busy && idleVictimValid && drainNeeded
  val chainCandidate = state === sResp && !io.bus_busy && chainVictimValid &&
    chainDrainNeeded
  val chainHandoff = chainCandidate && bFire

  val immutable = Wire(Vec(lines, Bool()))
  for (i <- 0 until lines) {
    immutable(i) := (activeValid && activeIdx === i.U) ||
      (burstStart && idleVictimIdx === i.U) ||
      (chainCandidate && chainVictimIdx === i.U)
  }

  val in0Line = io.enq.bits.addr & "hffff_ffe0".U
  val in1Line = io.enq1.bits.addr & "hffff_ffe0".U
  val in0Word = io.enq.bits.addr(4, 2)
  val in1Word = io.enq1.bits.addr(4, 2)
  val in0Data = normalizedData(io.enq.bits)
  val in1Data = normalizedData(io.enq1.bits)
  val in0Mask = normalizedMask(io.enq.bits)
  val in1Mask = normalizedMask(io.enq1.bits)

  val match0OH = VecInit((0 until lines).map(i =>
    valid(i) && !immutable(i) && lineAddr(i) === in0Line))
  val match1OH = VecInit((0 until lines).map(i =>
    valid(i) && !immutable(i) && lineAddr(i) === in1Line))
  val match0 = match0OH.asUInt.orR
  val match1 = match1OH.asUInt.orR
  val match0Idx = PriorityEncoder(match0OH.asUInt)
  val match1Idx = PriorityEncoder(match1OH.asUInt)
  val pairSameLine = io.enq.valid && io.enq1.valid && in0Line === in1Line
  val slot0Needed = io.enq.valid && !match0
  val slot1Needed = io.enq1.valid && !pairSameLine && !match1
  val slotsNeeded = PopCount(Seq(slot0Needed, slot1Needed))

  val freeMask = VecInit((0 until lines).map(i => !valid(i))).asUInt |
    Mux(deqFire, UIntToOH(activeIdx, lines), 0.U(lines.W))
  val freeCount = PopCount(freeMask)
  val batchReady = slotsNeeded <= freeCount
  io.enq.ready := batchReady
  io.enq1.ready := batchReady
  val doEnq0 = io.enq.valid && batchReady
  val doEnq1 = io.enq1.valid && batchReady
  val alloc0Idx = PriorityEncoder(freeMask)
  val freeAfter0 = freeMask & ~Mux(doEnq0 && !match0,
    UIntToOH(alloc0Idx, lines), 0.U(lines.W))
  val alloc1Idx = PriorityEncoder(freeAfter0)
  val acceptedLines = Mux(batchReady, slotsNeeded, 0.U)

  io.empty := count === 0.U
  io.full := count === lines.U
  io.busy := state =/= sIdle
  io.count := count
  io.free := freeCount
  io.merged := Mux(batchReady,
    PopCount(Seq(io.enq.valid, io.enq1.valid)) - slotsNeeded, 0.U)

  for (i <- 0 until lines) {
    when(valid(i) && age(i) =/= retentionCycles.U) {
      age(i) := age(i) + 1.U
    }
  }
  when(deqFire) {
    valid(activeIdx) := false.B
  }

  when(doEnq0 && pairSameLine) {
    val target = Mux(match0, match0Idx, alloc0Idx)
    when(!match0) {
      valid(target) := true.B
      lineAddr(target) := in0Line
      for (w <- 0 until 8) {
        data(target)(w) := 0.U
        masks(target)(w) := 0.U
      }
    }
    when(in0Word === in1Word) {
      val oldData = Mux(match0, data(target)(in0Word), 0.U)
      val oldMask = Mux(match0, masks(target)(in0Word), 0.U)
      val after0 = mergedData(oldData, oldMask, in0Data, in0Mask)
      val maskAfter0 = oldMask | in0Mask
      data(target)(in0Word) := mergedData(after0, maskAfter0, in1Data, in1Mask)
      masks(target)(in0Word) := maskAfter0 | in1Mask
    }.otherwise {
      val oldData0 = Mux(match0, data(target)(in0Word), 0.U)
      val oldMask0 = Mux(match0, masks(target)(in0Word), 0.U)
      val oldData1 = Mux(match0, data(target)(in1Word), 0.U)
      val oldMask1 = Mux(match0, masks(target)(in1Word), 0.U)
      data(target)(in0Word) := mergedData(oldData0, oldMask0, in0Data, in0Mask)
      masks(target)(in0Word) := oldMask0 | in0Mask
      data(target)(in1Word) := mergedData(oldData1, oldMask1, in1Data, in1Mask)
      masks(target)(in1Word) := oldMask1 | in1Mask
    }
    age(target) := 0.U
  }.otherwise {
    when(doEnq0) {
      val target = Mux(match0, match0Idx, alloc0Idx)
      when(!match0) {
        valid(target) := true.B
        lineAddr(target) := in0Line
        for (w <- 0 until 8) {
          data(target)(w) := 0.U
          masks(target)(w) := 0.U
        }
      }
      val oldData = Mux(match0, data(target)(in0Word), 0.U)
      val oldMask = Mux(match0, masks(target)(in0Word), 0.U)
      data(target)(in0Word) := mergedData(oldData, oldMask, in0Data, in0Mask)
      masks(target)(in0Word) := oldMask | in0Mask
      age(target) := 0.U
    }
    when(doEnq1) {
      val target = Mux(match1, match1Idx, alloc1Idx)
      when(!match1) {
        valid(target) := true.B
        lineAddr(target) := in1Line
        for (w <- 0 until 8) {
          data(target)(w) := 0.U
          masks(target)(w) := 0.U
        }
      }
      val oldData = Mux(match1, data(target)(in1Word), 0.U)
      val oldMask = Mux(match1, masks(target)(in1Word), 0.U)
      data(target)(in1Word) := mergedData(oldData, oldMask, in1Data, in1Mask)
      masks(target)(in1Word) := oldMask | in1Mask
      age(target) := 0.U
    }
  }

  count := count + acceptedLines - deqFire

  val launchFirst = firstWord(idleVictimIdx)
  val launchLast = lastWord(idleVictimIdx)
  val launchCount = (launchLast - launchFirst) +& 1.U
  val chainFirst = firstWord(chainVictimIdx)
  val chainLast = lastWord(chainVictimIdx)
  val chainCount = (chainLast - chainFirst) +& 1.U

  when(state === sIdle) {
    awDone := false.B
    wDone := false.B
    writeBeat := 0.U
    when(burstStart) {
      activeIdx := idleVictimIdx
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
        activeIdx := chainVictimIdx
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
  val busIdx = Mux(chainLaunching, chainVictimIdx,
    Mux(launching, idleVictimIdx, activeIdx))
  val busFirst = Mux(chainLaunching, chainFirst,
    Mux(launching, launchFirst, activeFirstWord))
  val busCount = Mux(chainLaunching, chainCount,
    Mux(launching, launchCount, activeBurstCount))
  val busBeat = Mux(launching, 0.U, writeBeat)
  val busWord = busFirst + busBeat
  val busLine = lineAddr(busIdx)

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
  io.dmem.wdata := data(busIdx)(busWord)
  io.dmem.wstrb := masks(busIdx)(busWord)
  io.dmem.wvalid := (state === sWrite && !wDone) || launching
  io.dmem.wlast := busBeat === busCount - 1.U
  io.dmem.bready := state === sResp

  io.write_burst := burstStart || chainHandoff
  io.write_beats := Mux(chainHandoff, chainCount, Mux(burstStart, launchCount, 0.U))
  io.write_chain := chainHandoff
  io.deq_valid := deqFire
  io.deq_addr := lineAddr(activeIdx)
  io.deq_count := PopCount(masks(activeIdx).map(_.orR))
  io.drain_valid := io.dmem.wvalid && io.dmem.wready && io.dmem.wstrb.orR
  io.drain_addr := busLine + (busWord << 2)
  io.drain_data := io.dmem.wdata
  io.drain_mask := io.dmem.wstrb

  def loadQuery(
      queryValid: Bool,
      addr: UInt,
      memRd: UInt): (Bool, Bool, Bool, UInt, UInt, UInt) = {
    val queryLine = addr & "hffff_ffe0".U
    val queryWord = addr(4, 2)
    val activeHit = activeValid && lineAddr(activeIdx) === queryLine
    val mergedDataVec = Wire(Vec(lines + 1, UInt(32.W)))
    val mergedMaskVec = Wire(Vec(lines + 1, UInt(4.W)))
    mergedDataVec(0) := Mux(activeHit, data(activeIdx)(queryWord), 0.U)
    mergedMaskVec(0) := Mux(activeHit, masks(activeIdx)(queryWord), 0.U)
    for (i <- 0 until lines) {
      val hit = valid(i) && !(activeHit && activeIdx === i.U) &&
        lineAddr(i) === queryLine
      val bits = expandMask(masks(i)(queryWord))
      mergedDataVec(i + 1) := Mux(hit,
        (mergedDataVec(i) & ~bits) | (data(i)(queryWord) & bits), mergedDataVec(i))
      mergedMaskVec(i + 1) := Mux(hit,
        mergedMaskVec(i) | masks(i)(queryWord), mergedMaskVec(i))
    }
    val needed = loadMaskBytes(memRd, addr)
    val overlap = (mergedMaskVec(lines) & needed) =/= 0.U
    val covered = (mergedMaskVec(lines) & needed) === needed
    val partial = queryValid && overlap && !covered
    val cacheable = (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)
    (partial && !cacheable, queryValid && covered, partial && cacheable,
      mergedDataVec(lines) >> (addr(1, 0) << 3),
      mergedDataVec(lines), mergedMaskVec(lines))
  }

  val query0 = loadQuery(io.ld_valid, io.ld_addr, io.ld_mem_rd)
  io.ld_wait := query0._1
  io.ld_fwd_valid := query0._2
  io.ld_fwd_data := query0._4
  io.ld_partial_valid := query0._3
  io.ld_partial_data := query0._5
  io.ld_partial_mask := query0._6
  val query1 = loadQuery(io.ld1_valid, io.ld1_addr, io.ld1_mem_rd)
  io.ld1_wait := query1._1
  io.ld1_fwd_valid := query1._2
  io.ld1_fwd_data := query1._4
  io.ld1_partial_valid := query1._3
  io.ld1_partial_data := query1._5
  io.ld1_partial_mask := query1._6

  when(!reset.asBool) {
    assert(!(io.enq.fire ^ io.enq1.fire) || !(io.enq.valid && io.enq1.valid),
      "write-combining StoreBuffer must accept a two-store packet atomically")
    for (i <- 0 until lines; j <- i + 1 until lines) {
      assert(!(valid(i) && valid(j) && lineAddr(i) === lineAddr(j) &&
        !(activeValid && (activeIdx === i.U || activeIdx === j.U))),
        "only the immutable active line may have a younger line generation")
    }
  }
}
