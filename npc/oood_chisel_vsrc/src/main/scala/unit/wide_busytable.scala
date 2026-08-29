package unit

import chisel3._
import chisel3.util._
import common.OoOParams

class WideBusyTable(nPhys: Int = OoOParams.N_PHYS) extends Module {
  private val width = OoOParams.CORE_WIDTH
  private val physW = log2Ceil(nPhys)

  val io = IO(new Bundle {
    val raddr = Input(Vec(width * 2, UInt(physW.W)))
    val ready = Output(Vec(width * 2, Bool()))
    val set_valid = Input(Vec(width, Bool()))
    val set_addr = Input(Vec(width, UInt(physW.W)))
    val clr_valid = Input(Vec(width, Bool()))
    val clr_addr = Input(Vec(width, UInt(physW.W)))
    val clr_mask = Input(UInt(nPhys.W))
    val rebuild = Input(Bool())
    val rebuild_mask = Input(UInt(nPhys.W))
  })

  val busy = RegInit(VecInit(Seq.fill(nPhys)(false.B)))
  for (i <- 0 until width * 2) {
    io.ready(i) := io.raddr(i) === 0.U || !busy(io.raddr(i))
  }

  when(io.rebuild) {
    for (i <- 0 until nPhys) {
      busy(i) := io.rebuild_mask(i) && i.U =/= 0.U
    }
  }.otherwise {
    for (p <- 1 until nPhys) {
      val set = (0 until width).map(i =>
        io.set_valid(i) && io.set_addr(i) === p.U).foldLeft(false.B)(_ || _)
      val clear = io.clr_mask(p) || (0 until width).map(i =>
        io.clr_valid(i) && io.clr_addr(i) === p.U).foldLeft(false.B)(_ || _)
      when(clear) {
        busy(p) := false.B
      }.elsewhen(set) {
        busy(p) := true.B
      }
    }
    busy(0) := false.B
  }
}
