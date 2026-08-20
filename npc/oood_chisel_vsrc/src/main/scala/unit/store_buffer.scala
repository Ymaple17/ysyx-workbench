package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import common.MEM_READ._
import common.MEM_WMASK._
import common.OoOParams

class StoreBufferEntry extends Bundle {
  val addr = UInt(32.W)
  val data = UInt(32.W)
  val mask = UInt(4.W)
}

class StoreBufferIO(size: Int) extends Bundle {
  val enq = Flipped(Decoupled(new StoreBufferEntry))

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

  val empty = Output(Bool())
  val full  = Output(Bool())
  val busy  = Output(Bool())
  val count = Output(UInt(log2Ceil(size + 1).W))
}

class StoreBuffer(size: Int = OoOParams.STORE_BUFFER_SIZE) extends Module {
  require(size >= 2 && isPow2(size))

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

  io.empty := count === 0.U
  io.full := count === size.U
  io.busy := state =/= sIdle
  io.count := count

  val bFire = io.dmem.bvalid && io.dmem.bready
  val deqFire = (state === sResp) && bFire
  io.enq.ready := !io.full || deqFire
  io.deq_valid := deqFire
  io.deq_addr := entries(head).addr

  when(io.enq.fire) {
    entries(tail) := io.enq.bits
    tail := tail + 1.U
  }

  when(deqFire) {
    head := head + 1.U
  }

  count := count + io.enq.fire.asUInt - deqFire.asUInt

  val headEntry = entries(head)
  val headOff = headEntry.addr(1, 0)
  val headMask = (headEntry.mask << headOff)(3, 0)
  val headData = headEntry.data << (headOff << 3)
  val headSize = MuxLookup(headEntry.mask, 2.U(3.W))(Seq(
    WBYTE(3, 0) -> 0.U(3.W),
    WHALF(3, 0) -> 1.U(3.W),
    WWORD(3, 0) -> 2.U(3.W)
  ))

  when(state === sIdle) {
    awDone := false.B
    wDone := false.B
    when(!io.empty && !io.bus_busy) {
      state := sWrite
    }
  }.elsewhen(state === sWrite) {
    val awFire = io.dmem.awvalid && io.dmem.awready
    val wFire = io.dmem.wvalid && io.dmem.wready
    when(awFire) { awDone := true.B }
    when(wFire) { wDone := true.B }
    when((awDone || awFire) && (wDone || wFire)) {
      state := sResp
    }
  }.elsewhen(state === sResp) {
    when(bFire) {
      state := sIdle
      awDone := false.B
      wDone := false.B
    }
  }

  io.dmem.araddr := 0.U
  io.dmem.arvalid := false.B
  io.dmem.arid := 0.U
  io.dmem.arlen := 0.U
  io.dmem.arsize := 0.U
  io.dmem.arburst := 0.U
  io.dmem.rready := false.B

  io.dmem.awaddr := headEntry.addr
  io.dmem.awvalid := (state === sWrite) && !awDone
  io.dmem.awid := 0.U
  io.dmem.awlen := 0.U
  io.dmem.awsize := headSize
  io.dmem.awburst := 1.U
  io.dmem.wdata := headData
  io.dmem.wstrb := headMask
  io.dmem.wvalid := (state === sWrite) && !wDone
  io.dmem.wlast := true.B
  io.dmem.bready := state === sResp

  def storeMaskBytes(raw: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    (raw(3, 0) << off)(3, 0)
  }

  def storeShiftData(raw: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    raw << (off << 3)
  }

  def loadMaskBytes(memRd: UInt, addr: UInt): UInt = {
    val off = addr(1, 0)
    val base = MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE  -> "b0001".U(4.W),
      RHALF  -> "b0011".U(4.W),
      RWORD  -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << off)(3, 0)
  }

  val fwdHits = Wire(Vec(size, Bool()))
  val waitHits = Wire(Vec(size, Bool()))
  val fwdData = Wire(Vec(size, UInt(32.W)))
  for (off <- 0 until size) {
    val idx = (head + off.U)(ptrW - 1, 0)
    val valid = off.U < count
    val e = entries(idx)
    val sameWord = e.addr(31, 2) === io.ld_addr(31, 2)
    val storeMask = storeMaskBytes(e.mask, e.addr)
    val loadMask = loadMaskBytes(io.ld_mem_rd, io.ld_addr)
    val fullCover = (loadMask & storeMask) === loadMask
    val overlap = (loadMask & storeMask) =/= 0.U

    waitHits(off) := io.ld_valid && valid && sameWord && overlap && !fullCover
    fwdHits(off) := io.ld_valid && valid && sameWord && fullCover
    fwdData(off) := storeShiftData(e.data, e.addr) >> (io.ld_addr(1, 0) << 3)
  }

  val bestFwdOH = Wire(Vec(size, Bool()))
  for (i <- 0 until size) {
    val hasYounger = (i + 1 until size).map(j => fwdHits(j)).foldLeft(false.B)(_ || _)
    bestFwdOH(i) := fwdHits(i) && !hasYounger
  }

  io.ld_wait := waitHits.asUInt.orR
  io.ld_fwd_valid := fwdHits.asUInt.orR && !io.ld_wait
  io.ld_fwd_data := Mux1H(bestFwdOH, fwdData)
}
