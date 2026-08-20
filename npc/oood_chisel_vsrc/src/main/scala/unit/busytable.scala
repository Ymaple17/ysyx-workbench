package unit

import common.OoOParams
import chisel3._
import chisel3.util._

class BusyTable(nPhys: Int = OoOParams.N_PHYS) extends Module {
  val physW = log2Ceil(nPhys)
  val io = IO(new Bundle {
    val raddr1   = Input(UInt(physW.W))
    val ready1   = Output(Bool())
    val raddr2   = Input(UInt(physW.W))
    val ready2   = Output(Bool())
    val raddr3   = Input(UInt(physW.W))
    val ready3   = Output(Bool())
    val raddr4   = Input(UInt(physW.W))
    val ready4   = Output(Bool())
    val set_en   = Input(Bool())
    val set_addr = Input(UInt(physW.W))
    val set_en2  = Input(Bool())
    val set_addr2 = Input(UInt(physW.W))
    val clr_en   = Input(Bool())
    val clr_addr = Input(UInt(physW.W))
    val clr_en2  = Input(Bool())
    val clr_addr2 = Input(UInt(physW.W))
    // flush：一次清多个被杀 new_phys（bit=1 表示清）
    val clr_mask = Input(UInt(nPhys.W))
    // flush 整表重建：1=busy（优先于 set/clr）
    val rebuild      = Input(Bool())
    val rebuild_mask = Input(UInt(nPhys.W))
  })

  val busy = RegInit(VecInit(Seq.fill(nPhys)(false.B)))

  io.ready1 := (io.raddr1 === 0.U) || !busy(io.raddr1)
  io.ready2 := (io.raddr2 === 0.U) || !busy(io.raddr2)
  io.ready3 := (io.raddr3 === 0.U) || !busy(io.raddr3)
  io.ready4 := (io.raddr4 === 0.U) || !busy(io.raddr4)

  when(io.rebuild) {
    for (i <- 0 until nPhys) { busy(i) := io.rebuild_mask(i) && (i.U =/= 0.U) }
  }.otherwise {
    when(io.set_en && io.set_addr =/= 0.U) { busy(io.set_addr) := true.B }
    when(io.set_en2 && io.set_addr2 =/= 0.U) { busy(io.set_addr2) := true.B }
    when(io.clr_en && io.clr_addr =/= 0.U) { busy(io.clr_addr) := false.B }
    when(io.clr_en2 && io.clr_addr2 =/= 0.U) { busy(io.clr_addr2) := false.B }
    for (i <- 1 until nPhys) {
      when(io.clr_mask(i)) { busy(i) := false.B }
    }
  }
}
