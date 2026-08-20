package unit

import chisel3._
import chisel3.util._
import common.MEM_READ._
import common.OoOParams

class StoreQueueIO extends Bundle {
  val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
  val entries  = Input(Vec(OoOParams.ROB_SIZE, new ROBEntry))

  val ld_valid = Input(Bool())
  val ld_rob   = Input(UInt(OoOParams.ROB_PTR_W.W))
  val ld_addr  = Input(UInt(32.W))
  val ld_mem_rd = Input(UInt(3.W))

  val fwd_valid = Output(Bool())
  val fwd_data  = Output(UInt(32.W))
  val wait_load = Output(Bool())
}

class StoreQueue extends Module {
  val io = IO(new StoreQueueIO)

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

  val ldAge = age(io.ld_rob)
  val fwdHits = Wire(Vec(OoOParams.ROB_SIZE, Bool()))
  val waitHits = Wire(Vec(OoOParams.ROB_SIZE, Bool()))
  val fwdData = Wire(Vec(OoOParams.ROB_SIZE, UInt(32.W)))
  val fwdAge = Wire(Vec(OoOParams.ROB_SIZE, UInt(OoOParams.ROB_PTR_W.W)))

  for (i <- 0 until OoOParams.ROB_SIZE) {
    val e = io.entries(i)
    val eAge = age(i.U)
    val olderStore = e.valid && e.mem_valid && e.mem_write && (eAge < ldAge)
    val unresolved = olderStore && !e.addr_ready
    val sameWord = e.addr_ready && (e.mem_addr(31, 2) === io.ld_addr(31, 2))
    val storeMask = storeMaskBytes(e.mem_wmask, e.mem_addr)
    val loadMask = loadMaskBytes(io.ld_mem_rd, io.ld_addr)
    val fullCover = (loadMask & storeMask) === loadMask
    val overlap = (loadMask & storeMask) =/= 0.U

    waitHits(i) := io.ld_valid && (unresolved ||
      (olderStore && sameWord && overlap && !fullCover))
    fwdHits(i) := io.ld_valid && olderStore && sameWord && fullCover
    fwdData(i) := storeShiftData(e.mem_wdata, e.mem_addr) >> (io.ld_addr(1, 0) << 3)
    fwdAge(i) := eAge
  }

  val bestFwdOH = Wire(Vec(OoOParams.ROB_SIZE, Bool()))
  for (i <- 0 until OoOParams.ROB_SIZE) {
    val hasYoungerOlder = (0 until OoOParams.ROB_SIZE).map { j =>
      fwdHits(j) && (fwdAge(j) > fwdAge(i))
    }.foldLeft(false.B)(_ || _)
    bestFwdOH(i) := fwdHits(i) && !hasYoungerOlder
  }

  io.wait_load := waitHits.asUInt.orR
  io.fwd_valid := fwdHits.asUInt.orR && !io.wait_load
  io.fwd_data := Mux1H(bestFwdOH, fwdData)
}
