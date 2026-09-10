package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.IFUPacket

class FetchBuffer(n: Int = OoOParams.FETCH_BUFFER_SIZE) extends Module {
  require(n >= 2 && isPow2(n))

  private val ptrW = log2Ceil(n)
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new IFUPacket))
    val out = Decoupled(new IFUPacket)
    val flush = Input(Bool())
    val replaceReady = Input(Bool())
    val count = Output(UInt(log2Ceil(n + 1).W))
    val full = Output(Bool())
    val empty = Output(Bool())
  })

  val entries = Reg(Vec(n, new IFUPacket))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  val storedValid = count =/= 0.U
  val flowThrough = !storedValid && io.in.valid && io.out.ready && !io.flush
  io.out.valid := (storedValid || io.in.valid) && !io.flush
  io.out.bits := Mux(storedValid, entries(head), io.in.bits)
  // replaceReady is derived from registered downstream space. It allows a
  // guaranteed full pop+push without feeding live backend ready into ICache.
  io.in.ready := !io.flush && (count =/= n.U || io.replaceReady)
  io.count := count
  io.full := count === n.U
  io.empty := count === 0.U

  when(io.flush) {
    head := 0.U
    tail := 0.U
    count := 0.U
  }.otherwise {
    val storeInput = io.in.fire && !flowThrough
    val removeStored = io.out.fire && storedValid
    when(storeInput) {
      entries(tail) := io.in.bits
      tail := tail + 1.U
    }
    when(removeStored) {
      head := head + 1.U
    }
    count := count + storeInput.asUInt - removeStored.asUInt
  }

  when(!reset.asBool && count === n.U && io.in.fire) {
    assert(io.out.fire, "full FetchBuffer replacement requires a guaranteed pop")
  }
}
