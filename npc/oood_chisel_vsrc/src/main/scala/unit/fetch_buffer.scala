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
    val count = Output(UInt(log2Ceil(n + 1).W))
    val full = Output(Bool())
    val empty = Output(Bool())
  })

  val entries = Reg(Vec(n, new IFUPacket))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  io.out.valid := count =/= 0.U && !io.flush
  io.out.bits := entries(head)
  io.in.ready := !io.flush && ((count =/= n.U) || io.out.fire)
  io.count := count
  io.full := count === n.U
  io.empty := count === 0.U

  when(io.flush) {
    head := 0.U
    tail := 0.U
    count := 0.U
  }.otherwise {
    when(io.in.fire) {
      entries(tail) := io.in.bits
      tail := tail + 1.U
    }
    when(io.out.fire) {
      head := head + 1.U
    }
    count := count + io.in.fire.asUInt - io.out.fire.asUInt
  }
}
