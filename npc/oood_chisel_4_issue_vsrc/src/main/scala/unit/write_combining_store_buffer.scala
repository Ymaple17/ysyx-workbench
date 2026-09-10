package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import common.MEM_READ._

class WriteCombiningStoreBufferIO(lines: Int) extends Bundle {
  val enq = Flipped(Decoupled(new StoreBufferEntry))
  val enq1 = Flipped(Decoupled(new StoreBufferEntry))
  val enq_cache_hit = Input(Bool())
  val enq1_cache_hit = Input(Bool())

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
  val cache_line = Decoupled(new StoreWritebackLine)
  val l1_writeback = Flipped(Decoupled(new StoreWritebackLine))
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

class WriteCombiningStoreBuffer(
    lines: Int = 16,
    retentionCycles: Int = 64,
    writebackDepth: Int = 4) extends Module {
  require(lines >= 4 && isPow2(lines))
  require(retentionCycles >= 1)
  require(writebackDepth >= 2 && isPow2(writebackDepth))

  val io = IO(new WriteCombiningStoreBufferIO(lines))
  private val ageW = log2Ceil(retentionCycles + 1).max(1)
  private val pressureThreshold = (lines - 2).max(1)

  val valid = RegInit(VecInit(Seq.fill(lines)(false.B)))
  val lineAddr = RegInit(VecInit(Seq.fill(lines)(0.U(32.W))))
  val data = RegInit(VecInit(Seq.fill(lines)(VecInit(Seq.fill(8)(0.U(32.W))))))
  val masks = RegInit(VecInit(Seq.fill(lines)(VecInit(Seq.fill(8)(0.U(4.W))))))
  val age = RegInit(VecInit(Seq.fill(lines)(0.U(ageW.W))))
  val count = RegInit(0.U(log2Ceil(lines + 1).W))

  val writeback = Module(new StoreWritebackQueue(writebackDepth))
  writeback.io.bus_busy := io.bus_busy
  writeback.io.ld_addr := io.ld_addr
  writeback.io.ld1_addr := io.ld1_addr

  def expandMask(mask: UInt): UInt =
    Cat((3 to 0 by -1).map(i => Fill(8, mask(i))))

  def normalizedData(raw: StoreBufferEntry): UInt =
    raw.data << (raw.addr(1, 0) << 3)

  def normalizedMask(raw: StoreBufferEntry): UInt =
    (raw.mask << raw.addr(1, 0))(3, 0)

  def mergedData(oldData: UInt, newData: UInt, newMask: UInt): UInt = {
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

  val in0Line = io.enq.bits.addr & "hffff_ffe0".U
  val in1Line = io.enq1.bits.addr & "hffff_ffe0".U
  val in0Word = io.enq.bits.addr(4, 2)
  val in1Word = io.enq1.bits.addr(4, 2)
  val in0Data = normalizedData(io.enq.bits)
  val in1Data = normalizedData(io.enq1.bits)
  val in0Mask = normalizedMask(io.enq.bits)
  val in1Mask = normalizedMask(io.enq1.bits)
  val residentMatch0 = VecInit((0 until lines).map(i =>
    valid(i) && lineAddr(i) === in0Line)).asUInt.orR
  val residentMatch1 = VecInit((0 until lines).map(i =>
    valid(i) && lineAddr(i) === in1Line)).asUInt.orR

  // Preserve useful same-cycle merges under pressure. An architectural drain
  // instead snapshots the oldest generation even when a younger same-line
  // store arrives; that store must allocate a distinct generation below.
  val unprotected = VecInit((0 until lines).map(i => valid(i) &&
    !(io.enq.valid && lineAddr(i) === in0Line) &&
    !(io.enq1.valid && lineAddr(i) === in1Line)))
  val oldestAnyOH = oldestOH(valid)
  val oldestUnprotectedOH = oldestOH(unprotected)
  val unprotectedValid = unprotected.asUInt.orR
  val incomingValid = io.enq.valid || io.enq1.valid
  val victimBits = Mux(io.drain_all, oldestAnyOH.asUInt,
    Mux(unprotectedValid, oldestUnprotectedOH.asUInt,
      Mux(!incomingValid, oldestAnyOH.asUInt, 0.U(lines.W))))
  val victimValid = victimBits.orR
  val victimIdx = PriorityEncoder(victimBits)
  val drainNeeded = io.drain_all || count >= pressureThreshold.U
  val migrateRequest = victimValid && drainNeeded
  io.cache_line.valid := migrateRequest
  io.cache_line.bits.lineAddr := lineAddr(victimIdx)
  for (w <- 0 until 8) {
    io.cache_line.bits.data(w) := data(victimIdx)(w)
    io.cache_line.bits.masks(w) := masks(victimIdx)(w)
  }
  val ownWritebackValid = migrateRequest && !io.cache_line.ready
  writeback.io.enq.valid := io.l1_writeback.valid || ownWritebackValid
  writeback.io.enq.bits.lineAddr := Mux(io.l1_writeback.valid,
    io.l1_writeback.bits.lineAddr, lineAddr(victimIdx))
  for (w <- 0 until 8) {
    writeback.io.enq.bits.data(w) := Mux(io.l1_writeback.valid,
      io.l1_writeback.bits.data(w), data(victimIdx)(w))
    writeback.io.enq.bits.masks(w) := Mux(io.l1_writeback.valid,
      io.l1_writeback.bits.masks(w), masks(victimIdx)(w))
  }
  io.l1_writeback.ready := writeback.io.enq.ready
  val ownWritebackFire = ownWritebackValid && !io.l1_writeback.valid &&
    writeback.io.enq.ready
  val migrateFire = io.cache_line.fire || ownWritebackFire

  val match0OH = VecInit((0 until lines).map(i =>
    valid(i) && lineAddr(i) === in0Line &&
      !(migrateFire && victimIdx === i.U)))
  val match1OH = VecInit((0 until lines).map(i =>
    valid(i) && lineAddr(i) === in1Line &&
      !(migrateFire && victimIdx === i.U)))
  val match0 = match0OH.asUInt.orR
  val match1 = match1OH.asUInt.orR
  val match0Idx = PriorityEncoder(match0OH.asUInt)
  val match1Idx = PriorityEncoder(match1OH.asUInt)
  val pairSameLine = io.enq.valid && io.enq1.valid && in0Line === in1Line
  writeback.io.probe_line := in0Line
  writeback.io.probe1_line := in1Line
  val migratingIn0Generation = migrateFire && lineAddr(victimIdx) === in0Line
  val migratingIn1Generation = migrateFire && lineAddr(victimIdx) === in1Line
  val pairCacheBypass = pairSameLine && io.enq_cache_hit && io.enq1_cache_hit &&
    !match0 && !writeback.io.probe_pending && !migratingIn0Generation
  val bypass0 = Mux(pairSameLine, pairCacheBypass,
    io.enq.valid && io.enq_cache_hit && !match0 &&
      !writeback.io.probe_pending && !migratingIn0Generation)
  val bypass1 = Mux(pairSameLine, pairCacheBypass,
    io.enq1.valid && io.enq1_cache_hit && !match1 &&
      !writeback.io.probe1_pending && !migratingIn1Generation)
  val slot0Needed = io.enq.valid && !bypass0 && !match0
  val slot1Needed = io.enq1.valid && !bypass1 && !pairSameLine && !match1
  val slotsNeeded = PopCount(Seq(slot0Needed, slot1Needed))

  val residentFreeMask = VecInit((0 until lines).map(i => !valid(i))).asUInt
  val freeMask = residentFreeMask
  val freeCount = PopCount(freeMask)
  // Cache-hit bypass never creates same-cycle capacity credit. Keeping ready
  // on registered resident state avoids a DCache install/poison feedback loop.
  val readySlot0Needed = io.enq.valid && !residentMatch0
  val readySlot1Needed = io.enq1.valid && !pairSameLine && !residentMatch1
  val batchReady = PopCount(Seq(readySlot0Needed, readySlot1Needed)) <= freeCount
  io.enq.ready := batchReady
  io.enq1.ready := batchReady
  val doEnq0 = io.enq.valid && batchReady && !bypass0
  val doEnq1 = io.enq1.valid && batchReady && !bypass1
  val alloc0Idx = PriorityEncoder(freeMask)
  val freeAfter0 = freeMask & ~Mux(doEnq0 && !match0,
    UIntToOH(alloc0Idx, lines), 0.U(lines.W))
  val alloc1Idx = PriorityEncoder(freeAfter0)
  val acceptedLines = Mux(batchReady, slotsNeeded, 0.U)

  io.empty := count === 0.U && writeback.io.empty
  io.full := count === lines.U
  io.busy := writeback.io.busy
  io.count := count
  // Core and the local atomic enqueue path consume registered credits only;
  // a migrated slot becomes visible on the following cycle. This keeps
  // commit, DCache poison, and dirty-writeback arbitration out of ready.
  io.free := PopCount(residentFreeMask)
  val bufferedStores = PopCount(Seq(
    io.enq.valid && !bypass0, io.enq1.valid && !bypass1))
  io.merged := Mux(batchReady, bufferedStores - slotsNeeded, 0.U)

  for (i <- 0 until lines) {
    when(valid(i) && age(i) =/= retentionCycles.U) {
      age(i) := age(i) + 1.U
    }
  }
  when(migrateFire) {
    valid(victimIdx) := false.B
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
      val after0 = mergedData(oldData, in0Data, in0Mask)
      val maskAfter0 = oldMask | in0Mask
      data(target)(in0Word) := mergedData(after0, in1Data, in1Mask)
      masks(target)(in0Word) := maskAfter0 | in1Mask
    }.otherwise {
      val oldData0 = Mux(match0, data(target)(in0Word), 0.U)
      val oldMask0 = Mux(match0, masks(target)(in0Word), 0.U)
      val oldData1 = Mux(match0, data(target)(in1Word), 0.U)
      val oldMask1 = Mux(match0, masks(target)(in1Word), 0.U)
      data(target)(in0Word) := mergedData(oldData0, in0Data, in0Mask)
      masks(target)(in0Word) := oldMask0 | in0Mask
      data(target)(in1Word) := mergedData(oldData1, in1Data, in1Mask)
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
      data(target)(in0Word) := mergedData(oldData, in0Data, in0Mask)
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
      data(target)(in1Word) := mergedData(oldData, in1Data, in1Mask)
      masks(target)(in1Word) := oldMask | in1Mask
      age(target) := 0.U
    }
  }

  count := count + acceptedLines - migrateFire.asUInt

  io.dmem.araddr := writeback.io.dmem.araddr
  io.dmem.arvalid := writeback.io.dmem.arvalid
  io.dmem.arid := writeback.io.dmem.arid
  io.dmem.arlen := writeback.io.dmem.arlen
  io.dmem.arsize := writeback.io.dmem.arsize
  io.dmem.arburst := writeback.io.dmem.arburst
  writeback.io.dmem.arready := io.dmem.arready
  writeback.io.dmem.rdata := io.dmem.rdata
  writeback.io.dmem.rresp := io.dmem.rresp
  writeback.io.dmem.rvalid := io.dmem.rvalid
  writeback.io.dmem.rlast := io.dmem.rlast
  writeback.io.dmem.rid := io.dmem.rid
  io.dmem.rready := writeback.io.dmem.rready
  io.dmem.awaddr := writeback.io.dmem.awaddr
  io.dmem.awvalid := writeback.io.dmem.awvalid
  io.dmem.awid := writeback.io.dmem.awid
  io.dmem.awlen := writeback.io.dmem.awlen
  io.dmem.awsize := writeback.io.dmem.awsize
  io.dmem.awburst := writeback.io.dmem.awburst
  writeback.io.dmem.awready := io.dmem.awready
  io.dmem.wdata := writeback.io.dmem.wdata
  io.dmem.wstrb := writeback.io.dmem.wstrb
  io.dmem.wvalid := writeback.io.dmem.wvalid
  io.dmem.wlast := writeback.io.dmem.wlast
  writeback.io.dmem.wready := io.dmem.wready
  writeback.io.dmem.bresp := io.dmem.bresp
  writeback.io.dmem.bvalid := io.dmem.bvalid
  writeback.io.dmem.bid := io.dmem.bid
  io.dmem.bready := writeback.io.dmem.bready

  io.write_burst := writeback.io.write_burst
  io.write_beats := writeback.io.write_beats
  io.write_chain := writeback.io.write_chain
  io.deq_valid := writeback.io.deq_valid
  io.deq_addr := writeback.io.deq_addr
  io.deq_count := writeback.io.deq_count
  io.drain_valid := writeback.io.drain_valid
  io.drain_addr := writeback.io.drain_addr
  io.drain_data := writeback.io.drain_data
  io.drain_mask := writeback.io.drain_mask

  def loadQuery(
      queryValid: Bool,
      addr: UInt,
      memRd: UInt,
      olderData: UInt,
      olderMask: UInt): (Bool, Bool, Bool, UInt, UInt, UInt) = {
    val queryLine = addr & "hffff_ffe0".U
    val queryWord = addr(4, 2)
    val mergedDataVec = Wire(Vec(lines + 1, UInt(32.W)))
    val mergedMaskVec = Wire(Vec(lines + 1, UInt(4.W)))
    mergedDataVec(0) := olderData
    mergedMaskVec(0) := olderMask
    for (i <- 0 until lines) {
      val hit = valid(i) && lineAddr(i) === queryLine
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

  val query0 = loadQuery(io.ld_valid, io.ld_addr, io.ld_mem_rd,
    writeback.io.ld_data, writeback.io.ld_mask)
  io.ld_wait := query0._1
  io.ld_fwd_valid := query0._2
  io.ld_fwd_data := query0._4
  io.ld_partial_valid := query0._3
  io.ld_partial_data := query0._5
  io.ld_partial_mask := query0._6
  val query1 = loadQuery(io.ld1_valid, io.ld1_addr, io.ld1_mem_rd,
    writeback.io.ld1_data, writeback.io.ld1_mask)
  io.ld1_wait := query1._1
  io.ld1_fwd_valid := query1._2
  io.ld1_fwd_data := query1._4
  io.ld1_partial_valid := query1._3
  io.ld1_partial_data := query1._5
  io.ld1_partial_mask := query1._6

  when(!reset.asBool) {
    assert(!(io.enq.fire ^ io.enq1.fire) || !(io.enq.valid && io.enq1.valid),
      "write-combining StoreBuffer must accept a two-store packet atomically")
    assert(count === PopCount(valid),
      "write-combining StoreBuffer count must match live combine owners")
    for (i <- 0 until lines; j <- i + 1 until lines) {
      assert(!(valid(i) && valid(j) && lineAddr(i) === lineAddr(j)),
        "each dirty line must have one mutable combine owner")
    }
  }
}
