package unit

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.OoOParams

class SpecLoadTrackReq extends Bundle {
  val robIdx = UInt(OoOParams.ROB_PTR_W.W)
  val pc = UInt(32.W)
  val addr = UInt(32.W)
  val memRd = UInt(3.W)
  val dependencies = UInt(OoOParams.ROB_SIZE.W)
}

class SpecLoadTrackerIO extends Bundle {
  val robHead = Input(UInt(OoOParams.ROB_PTR_W.W))

  val track0Valid = Input(Bool())
  val track0 = Input(new SpecLoadTrackReq)
  val track1Valid = Input(Bool())
  val track1 = Input(new SpecLoadTrackReq)

  val storeResolve0Valid = Input(Bool())
  val storeResolve0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve0Addr = Input(UInt(32.W))
  val storeResolve0Mask = Input(UInt(4.W))
  val storeResolve1Valid = Input(Bool())
  val storeResolve1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val storeResolve1Addr = Input(UInt(32.W))
  val storeResolve1Mask = Input(UInt(4.W))
  val storeResolveHead = Input(UInt(OoOParams.ROB_PTR_W.W))
  val unresolvedStores = Input(UInt(OoOParams.ROB_SIZE.W))

  val commitValid = Input(Vec(4, Bool()))
  val commitRob = Input(Vec(4, UInt(OoOParams.ROB_PTR_W.W)))
  val flush = Input(Bool())
  val flushIdx = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flushAll = Input(Bool())

  val violationValid = Output(Bool())
  val violationRob = Output(UInt(OoOParams.ROB_PTR_W.W))
  val violationPc = Output(UInt(32.W))
  val commitWait = Output(Vec(4, Bool()))
  val count = Output(UInt(log2Ceil(OoOParams.ROB_SIZE + 1).W))
}

class SpecLoadTracker extends Module {
  val io = IO(new SpecLoadTrackerIO)

  val valid = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(false.B)))
  val pc = Reg(Vec(OoOParams.ROB_SIZE, UInt(32.W)))
  val addr = Reg(Vec(OoOParams.ROB_SIZE, UInt(32.W)))
  val memRd = Reg(Vec(OoOParams.ROB_SIZE, UInt(3.W)))
  val dependencies = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(OoOParams.ROB_SIZE.W))))

  def age(idx: UInt): UInt =
    (idx - io.robHead)(OoOParams.ROB_PTR_W - 1, 0)

  def resolveAge(idx: UInt): UInt =
    (idx - io.storeResolveHead)(OoOParams.ROB_PTR_W - 1, 0)

  def olderStoreMask(loadRob: UInt, stores: UInt): UInt =
    VecInit((0 until OoOParams.ROB_SIZE).map(storeRob =>
      stores(storeRob) && age(storeRob.U) < age(loadRob))).asUInt

  def loadMask(kind: UInt, loadAddr: UInt): UInt = {
    val base = MuxLookup(kind, "b0001".U(4.W))(Seq(
      RBYTE -> "b0001".U(4.W),
      RHALF -> "b0011".U(4.W),
      RWORD -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)))
    (base << loadAddr(1, 0))(3, 0)
  }

  def overlapsStore(i: Int, storeValid: Bool, storeRob: UInt,
      storeAddr: UInt, rawMask: UInt): Bool = {
    val sameWord = addr(i)(31, 2) === storeAddr(31, 2)
    val storeBytes = (rawMask << storeAddr(1, 0))(3, 0)
    storeValid && valid(i) && dependencies(i)(storeRob) &&
      resolveAge(storeRob) < resolveAge(i.U) && sameWord &&
      (loadMask(memRd(i), addr(i)) & storeBytes).orR
  }

  val violation = Wire(Vec(OoOParams.ROB_SIZE, Bool()))
  for (i <- 0 until OoOParams.ROB_SIZE) {
    violation(i) := overlapsStore(i, io.storeResolve0Valid,
      io.storeResolve0Rob, io.storeResolve0Addr, io.storeResolve0Mask) ||
      overlapsStore(i, io.storeResolve1Valid,
        io.storeResolve1Rob, io.storeResolve1Addr, io.storeResolve1Mask)
  }
  val violationOldest = Wire(Vec(OoOParams.ROB_SIZE, Bool()))
  for (i <- 0 until OoOParams.ROB_SIZE) {
    val hasOlder = (0 until OoOParams.ROB_SIZE).map(j =>
      violation(j) && age(j.U) < age(i.U)).foldLeft(false.B)(_ || _)
    violationOldest(i) := violation(i) && !hasOlder
  }
  val violationIdx = OHToUInt(violationOldest)
  io.violationValid := violation.asUInt.orR
  io.violationRob := violationIdx
  io.violationPc := pc(violationIdx)

  val resolvingMask =
    Mux(io.storeResolve0Valid,
      1.U(OoOParams.ROB_SIZE.W) << io.storeResolve0Rob, 0.U) |
    Mux(io.storeResolve1Valid,
      1.U(OoOParams.ROB_SIZE.W) << io.storeResolve1Rob, 0.U)

  val liveStoreMask = io.unresolvedStores | resolvingMask
  for (lane <- 0 until 4) {
    val loadRob = io.commitRob(lane)
    io.commitWait(lane) := valid(io.commitRob(lane)) &&
      (dependencies(loadRob) & olderStoreMask(loadRob, liveStoreMask)).orR
  }
  io.count := PopCount(valid)

  for (i <- 0 until OoOParams.ROB_SIZE) {
    val remaining = dependencies(i) & olderStoreMask(i.U, io.unresolvedStores) &
      ~resolvingMask
    when(valid(i) && dependencies(i) =/= remaining) {
      dependencies(i) := remaining
      when(!remaining.orR) {
        valid(i) := false.B
      }
    }
  }

  def trackAllowed(req: SpecLoadTrackReq): Bool =
    !io.flushAll && (!io.flush || age(req.robIdx) <= age(io.flushIdx))

  val track0Dependencies = io.track0.dependencies &
    olderStoreMask(io.track0.robIdx, liveStoreMask)
  val track1Dependencies = io.track1.dependencies &
    olderStoreMask(io.track1.robIdx, liveStoreMask)
  when(io.track0Valid && track0Dependencies.orR && trackAllowed(io.track0)) {
    assert(!valid(io.track0.robIdx), "speculative load tracker slot reused while live")
    valid(io.track0.robIdx) := true.B
    pc(io.track0.robIdx) := io.track0.pc
    addr(io.track0.robIdx) := io.track0.addr
    memRd(io.track0.robIdx) := io.track0.memRd
    dependencies(io.track0.robIdx) := track0Dependencies
  }
  when(io.track1Valid && track1Dependencies.orR && trackAllowed(io.track1)) {
    assert(!valid(io.track1.robIdx), "secondary speculative load tracker slot reused while live")
    assert(!io.track0Valid || io.track0.robIdx =/= io.track1.robIdx,
      "two speculative load completions cannot own one ROB slot")
    valid(io.track1.robIdx) := true.B
    pc(io.track1.robIdx) := io.track1.pc
    addr(io.track1.robIdx) := io.track1.addr
    memRd(io.track1.robIdx) := io.track1.memRd
    dependencies(io.track1.robIdx) := track1Dependencies
  }

  for (lane <- 0 until 4) {
    when(io.commitValid(lane)) {
      valid(io.commitRob(lane)) := false.B
      dependencies(io.commitRob(lane)) := 0.U
    }
  }

  when(io.flushAll) {
    for (i <- 0 until OoOParams.ROB_SIZE) {
      valid(i) := false.B
      dependencies(i) := 0.U
    }
  }.elsewhen(io.flush) {
    for (i <- 0 until OoOParams.ROB_SIZE) {
      when(valid(i) && age(i.U) > age(io.flushIdx)) {
        valid(i) := false.B
        dependencies(i) := 0.U
      }
    }
  }
}
