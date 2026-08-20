package unit

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.OoOParams

class StoreQueueEntry extends Bundle {
  val valid = Bool()
  val addr_ready = Bool()
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

  val commit_valid = Input(Bool())
  val commit_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))

  val flush     = Input(Bool())
  val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
  val flush_all = Input(Bool())

  val ld_valid = Input(Bool())
  val ld_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val ld_addr  = Input(UInt(32.W))
  val ld_mem_rd = Input(UInt(3.W))

  val unresolved_mask = Output(UInt(OoOParams.ROB_SIZE.W))
  val fwd_valid = Output(Bool())
  val fwd_data  = Output(UInt(32.W))
  val wait_load = Output(Bool())
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
        }
      }
    }

    when(io.commit_valid) {
      entries(io.commit_rob).valid := false.B
      entries(io.commit_rob).addr_ready := false.B
    }

    when(io.alloc0_valid) {
      entries(io.alloc0_rob).valid := true.B
      entries(io.alloc0_rob).addr_ready := false.B
      entries(io.alloc0_rob).addr := 0.U
      entries(io.alloc0_rob).data := 0.U
      entries(io.alloc0_rob).mask := io.alloc0_mask
    }

    when(io.alloc1_valid) {
      entries(io.alloc1_rob).valid := true.B
      entries(io.alloc1_rob).addr_ready := false.B
      entries(io.alloc1_rob).addr := 0.U
      entries(io.alloc1_rob).data := 0.U
      entries(io.alloc1_rob).mask := io.alloc1_mask
    }

    when(io.wb_valid && entries(io.wb_rob).valid) {
      entries(io.wb_rob).addr_ready := true.B
      entries(io.wb_rob).addr := io.wb_addr
      entries(io.wb_rob).data := io.wb_data
      entries(io.wb_rob).mask := io.wb_mask
    }
  }
  io.unresolved_mask := VecInit((0 until n).map { i =>
    entries(i).valid && !entries(i).addr_ready
  }).asUInt

  val ldAge = age(io.ld_rob)
  val fwdHits = Wire(Vec(n, Bool()))
  val waitHits = Wire(Vec(n, Bool()))
  val fwdData = Wire(Vec(n, UInt(32.W)))
  val fwdAge = Wire(Vec(n, UInt(OoOParams.ROB_PTR_W.W)))

  for (i <- 0 until n) {
    val e = entries(i)
    val eAge = age(i.U)
    val olderStore = e.valid && (eAge < ldAge)
    val unresolved = olderStore && !e.addr_ready
    val sameWord = e.addr_ready && (e.addr(31, 2) === io.ld_addr(31, 2))
    val storeMask = storeMaskBytes(e.mask, e.addr)
    val loadMask = loadMaskBytes(io.ld_mem_rd, io.ld_addr)
    val fullCover = (loadMask & storeMask) === loadMask
    val overlap = (loadMask & storeMask) =/= 0.U

    waitHits(i) := io.ld_valid && (unresolved ||
      (olderStore && sameWord && overlap && !fullCover))
    fwdHits(i) := io.ld_valid && olderStore && sameWord && fullCover
    fwdData(i) := storeShiftData(e.data, e.addr) >> (io.ld_addr(1, 0) << 3)
    fwdAge(i) := eAge
  }

  val bestFwdOH = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    val hasYoungerOlder = (0 until n).map { j =>
      fwdHits(j) && (fwdAge(j) > fwdAge(i))
    }.foldLeft(false.B)(_ || _)
    bestFwdOH(i) := fwdHits(i) && !hasYoungerOlder
  }

  io.wait_load := waitHits.asUInt.orR
  io.fwd_valid := fwdHits.asUInt.orR && !io.wait_load
  io.fwd_data := Mux1H(bestFwdOH, fwdData)
}
