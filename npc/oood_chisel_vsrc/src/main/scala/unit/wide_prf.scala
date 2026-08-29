package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.CoreConfig

class WidePRF(conf: CoreConfig, nPhys: Int = OoOParams.N_PHYS) extends Module {
  private val width = OoOParams.CORE_WIDTH
  private val physW = log2Ceil(nPhys)

  val io = IO(new Bundle {
    val raddr = Input(Vec(width * 2, UInt(physW.W)))
    val rdata = Output(Vec(width * 2, UInt(conf.xlen.W)))
    val wen = Input(Vec(width, Bool()))
    val waddr = Input(Vec(width, UInt(physW.W)))
    val wdata = Input(Vec(width, UInt(conf.xlen.W)))
    val arch_raddr = Input(Vec(32, UInt(physW.W)))
    val arch_rdata = Output(Vec(32, UInt(conf.xlen.W)))
  })

  val rf = RegInit(VecInit(Seq.fill(nPhys)(0.U(conf.xlen.W))))

  for (i <- 0 until width * 2) {
    io.rdata(i) := Mux(io.raddr(i) === 0.U, 0.U, rf(io.raddr(i)))
  }
  for (i <- 0 until width) {
    when(io.wen(i) && io.waddr(i) =/= 0.U) {
      rf(io.waddr(i)) := io.wdata(i)
    }
  }
  for (i <- 0 until 32) {
    io.arch_rdata(i) := Mux(i.U === 0.U, 0.U, rf(io.arch_raddr(i)))
  }

  when(!reset.asBool) {
    for (i <- 0 until width; j <- i + 1 until width) {
      assert(!(io.wen(i) && io.wen(j) && io.waddr(i) =/= 0.U &&
        io.waddr(i) === io.waddr(j)), "wide PRF writes must have unique destinations")
    }
  }
}
