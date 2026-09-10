package unit

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.OoOParams

class StoreQueueEntry extends Bundle {
  val valid = Bool()
  val addr_ready = Bool()
  val data_ready = Bool()
  val addr = UInt(32.W)
  val data = UInt(32.W)
  val mask = UInt(4.W)
}

class StoreQueueIO extends Bundle {
  val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))

  val alloc0_valid = Input(Bool())
  val alloc0_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val alloc0_mask  = Input(UInt(4.W))
  val alloc1_valid = Input(Bool())
  val alloc1_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val alloc1_mask  = Input(UInt(4.W))

  val wb_valid = Input(Bool())
  val wb_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val wb_addr  = Input(UInt(32.W))
  val wb_data  = Input(UInt(32.W))
  val wb_mask  = Input(UInt(4.W))
  val wb1_valid = Input(Bool())
  val wb1_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val wb1_addr  = Input(UInt(32.W))
  val wb1_data  = Input(UInt(32.W))
  val wb1_mask  = Input(UInt(4.W))

  val addr_wb_valid = Input(Bool())
  val addr_wb_rob = Input(UInt(OoOParams.ROB_PTR_W.W))
  val addr_wb_addr = Input(UInt(32.W))
  val commit_valid = Input(Bool())
  val commit_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val commit1_valid = Input(Bool())
  val commit1_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))

  val flush     = Input(Bool())
  val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flush_all = Input(Bool())

  val ld_valid = Input(Bool())
  val ld_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val ld_addr  = Input(UInt(32.W))
  val ld_mem_rd = Input(UInt(3.W))
  val ld1_valid = Input(Bool())
  val ld1_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val ld1_addr  = Input(UInt(32.W))
  val ld1_mem_rd = Input(UInt(3.W))

  val unresolved_mask = Output(UInt(OoOParams.ROB_SIZE.W))
  val fwd_valid = Output(Bool())
  val fwd_data  = Output(UInt(32.W))
  val partial_valid = Output(Bool())
  val partial_data = Output(UInt(32.W))
  val partial_mask = Output(UInt(4.W))
  val wait_load = Output(Bool())
  val wait_unknown = Output(Bool())
  val wait_partial = Output(Bool())
  val wait_data = Output(Bool())
  val has_fwd_candidate = Output(Bool())
  val older_unresolved_mask = Output(UInt(OoOParams.ROB_SIZE.W))
  val fwd1_valid = Output(Bool())
  val fwd1_data  = Output(UInt(32.W))
  val partial1_valid = Output(Bool())
  val partial1_data = Output(UInt(32.W))
  val partial1_mask = Output(UInt(4.W))
  val wait1_load = Output(Bool())
  val wait1_unknown = Output(Bool())
  val wait1_partial = Output(Bool())
  val wait1_data = Output(Bool())
  val has_fwd1_candidate = Output(Bool())
  val older_unresolved1_mask = Output(UInt(OoOParams.ROB_SIZE.W))
}

class StoreQueue extends Module {
  val io = IO(new StoreQueueIO)
  private val n = OoOParams.ROB_SIZE

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new StoreQueueEntry))))

  def age(idx: UInt): UInt =
    (idx - io.rob_head)(OoOParams.ROB_PTR_W - 1, 0)

  def storeMaskBytes(raw: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    WireDefault((raw(3, 0) << off)(3, 0))
  }

  def storeShiftData(raw: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    WireDefault(raw << (off << 3))
  }

  def loadMaskBytes(memRd: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    val base = Wire(UInt(4.W))
    base := MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE  -> "b0001".U(4.W),
      RHALF  -> "b0011".U(4.W),
      RWORD  -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << off)(3, 0)
  }

  when(io.flush_all) {
    for (i <- 0 until n) {
      entries(i).valid := false.B
      entries(i).addr_ready := false.B
      entries(i).data_ready := false.B
    }
  }.otherwise {
    when(io.flush) {
      val flushAge = age(io.flush_idx)
      for (i <- 0 until n) {
        val idx = i.U(OoOParams.ROB_PTR_W.W)
        val idxAge = age(idx)
        when(entries(i).valid && (idxAge > flushAge)) {
          entries(i).valid := false.B
          entries(i).addr_ready := false.B
          entries(i).data_ready := false.B
        }
      }
    }

    when(io.commit_valid) {
      entries(io.commit_rob).valid := false.B
      entries(io.commit_rob).addr_ready := false.B
      entries(io.commit_rob).data_ready := false.B
    }
    when(io.commit1_valid) {
      entries(io.commit1_rob).valid := false.B
      entries(io.commit1_rob).addr_ready := false.B
      entries(io.commit1_rob).data_ready := false.B
    }

    when(io.alloc0_valid) {
      entries(io.alloc0_rob).valid := true.B
      entries(io.alloc0_rob).addr_ready := false.B
      entries(io.alloc0_rob).data_ready := false.B
      entries(io.alloc0_rob).addr := 0.U
      entries(io.alloc0_rob).data := 0.U
      entries(io.alloc0_rob).mask := io.alloc0_mask
    }

    when(io.alloc1_valid) {
      entries(io.alloc1_rob).valid := true.B
      entries(io.alloc1_rob).addr_ready := false.B
      entries(io.alloc1_rob).data_ready := false.B
      entries(io.alloc1_rob).addr := 0.U
      entries(io.alloc1_rob).data := 0.U
      entries(io.alloc1_rob).mask := io.alloc1_mask
    }

    when(io.addr_wb_valid && entries(io.addr_wb_rob).valid) {
      entries(io.addr_wb_rob).addr_ready := true.B
      entries(io.addr_wb_rob).addr := io.addr_wb_addr
    }
    when(io.wb_valid && entries(io.wb_rob).valid) {
      entries(io.wb_rob).addr_ready := true.B
      entries(io.wb_rob).data_ready := true.B
      entries(io.wb_rob).addr := io.wb_addr
      entries(io.wb_rob).data := io.wb_data
      entries(io.wb_rob).mask := io.wb_mask
    }
    when(io.wb1_valid && entries(io.wb1_rob).valid) {
      entries(io.wb1_rob).addr_ready := true.B
      entries(io.wb1_rob).data_ready := true.B
      entries(io.wb1_rob).addr := io.wb1_addr
      entries(io.wb1_rob).data := io.wb1_data
      entries(io.wb1_rob).mask := io.wb1_mask
    }
  }
  io.unresolved_mask := VecInit((0 until n).map { i =>
    entries(i).valid && !entries(i).addr_ready
  }).asUInt

  def loadQuery(valid: Bool, rob: UInt, addr: UInt, memRd: UInt):
      (Bool, Bool, Bool, Bool, UInt, Bool, UInt, Bool, UInt, UInt, Bool) = {
    val ldAge = age(rob)
    val unknownHits = Wire(Vec(n, Bool()))
    val knownByteHits = Wire(Vec(4, Vec(n, Bool())))
    val pendingDataByteHits = Wire(Vec(4, Vec(n, Bool())))
    val normalizedData = Wire(Vec(n, UInt(32.W)))
    val storeAges = Wire(Vec(n, UInt(OoOParams.ROB_PTR_W.W)))

    for (i <- 0 until n) {
      val e = entries(i)
      val eAge = age(i.U)
      val olderStore = e.valid && (eAge < ldAge)
      val unresolved = olderStore && !e.addr_ready
      val sameWord = e.addr_ready && (e.addr(31, 2) === addr(31, 2))
      val storeMask = storeMaskBytes(e.mask, e.addr)

      unknownHits(i) := valid && unresolved
      normalizedData(i) := storeShiftData(e.data, e.addr)
      storeAges(i) := eAge
      for (b <- 0 until 4) {
        knownByteHits(b)(i) := valid && olderStore && e.data_ready &&
          sameWord && storeMask(b)
        pendingDataByteHits(b)(i) := valid && olderStore && !e.data_ready &&
          sameWord && storeMask(b)
      }
    }

    val mergedBytes = Wire(Vec(4, UInt(8.W)))
    val mergedMask = Wire(Vec(4, Bool()))
    for (b <- 0 until 4) {
      val youngestOH = Wire(Vec(n, Bool()))
      for (i <- 0 until n) {
        val youngerKnown = (0 until n).map { j =>
          knownByteHits(b)(j) && (storeAges(j) > storeAges(i))
        }.foldLeft(false.B)(_ || _)
        youngestOH(i) := knownByteHits(b)(i) && !youngerKnown
      }
      mergedMask(b) := knownByteHits(b).asUInt.orR
      mergedBytes(b) := Mux1H(youngestOH, normalizedData.map(_(8 * b + 7, 8 * b)))
    }

    val mergedData = Cat(mergedBytes.reverse)
    val mergedMaskUInt = mergedMask.asUInt
    val loadMask = loadMaskBytes(memRd, addr)
    val waitUnknown = unknownHits.asUInt.orR
    val pendingDataMask = VecInit((0 until 4).map(b =>
      pendingDataByteHits(b).asUInt.orR)).asUInt
    val waitData = (pendingDataMask & loadMask).orR
    val overlap = (mergedMaskUInt & loadMask).orR
    val covered = (mergedMaskUInt & loadMask) === loadMask
    val waitPartial = overlap && !covered
    val waitLoad = waitUnknown || waitData || waitPartial
    val fwdValid = covered && !waitUnknown && !waitData
    val fwdData = mergedData >> (addr(1, 0) << 3)
    val partialValid = overlap && !covered
    (waitLoad, waitUnknown, waitPartial, overlap,
      unknownHits.asUInt, fwdValid, fwdData,
      partialValid, mergedData, mergedMaskUInt, waitData)
  }

  val query0 = loadQuery(io.ld_valid, io.ld_rob, io.ld_addr, io.ld_mem_rd)
  io.wait_load := query0._1
  io.wait_unknown := query0._2
  io.wait_partial := query0._3
  io.has_fwd_candidate := query0._4
  io.older_unresolved_mask := query0._5
  io.fwd_valid := query0._6
  io.fwd_data := query0._7
  io.partial_valid := query0._8
  io.partial_data := query0._9
  io.partial_mask := query0._10
  io.wait_data := query0._11

  val query1 = loadQuery(io.ld1_valid, io.ld1_rob, io.ld1_addr, io.ld1_mem_rd)
  io.wait1_load := query1._1
  io.wait1_unknown := query1._2
  io.wait1_partial := query1._3
  io.has_fwd1_candidate := query1._4
  io.older_unresolved1_mask := query1._5
  io.fwd1_valid := query1._6
  io.fwd1_data := query1._7
  io.partial1_valid := query1._8
  io.partial1_data := query1._9
  io.partial1_mask := query1._10
  io.wait1_data := query1._11
}
