package unit

import common.OoOParams
import chisel3._
import core.CoreConfig
import chisel3.util._

class PRF(conf: CoreConfig, nPhys: Int = OoOParams.N_PHYS) extends Module {
  val physW = log2Ceil(nPhys)
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(physW.W))
    val rdata1 = Output(UInt(conf.xlen.W))
    val raddr2 = Input(UInt(physW.W))
    val rdata2 = Output(UInt(conf.xlen.W))
    val raddr3 = Input(UInt(physW.W))
    val rdata3 = Output(UInt(conf.xlen.W))
    val raddr4 = Input(UInt(physW.W))
    val rdata4 = Output(UInt(conf.xlen.W))
    val wen1   = Input(Bool())
    val waddr1 = Input(UInt(physW.W))
    val wdata1 = Input(UInt(conf.xlen.W))
    val wen2   = Input(Bool())
    val waddr2 = Input(UInt(physW.W))
    val wdata2 = Input(UInt(conf.xlen.W))
    val wen3   = Input(Bool())
    val waddr3 = Input(UInt(physW.W))
    val wdata3 = Input(UInt(conf.xlen.W))
    // 架构寄存器读：arch_raddr(i) = rat(i)
    val arch_raddr = Input(Vec(32, UInt(physW.W)))
    val arch_rdata = Output(Vec(32, UInt(conf.xlen.W)))
  })

  val rf = RegInit(VecInit(Seq.fill(nPhys)(0.U(conf.xlen.W))))

  io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, rf(io.raddr1))
  io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, rf(io.raddr2))
  io.rdata3 := Mux(io.raddr3 === 0.U, 0.U, rf(io.raddr3))
  io.rdata4 := Mux(io.raddr4 === 0.U, 0.U, rf(io.raddr4))

  when(io.wen1 && io.waddr1 =/= 0.U) { rf(io.waddr1) := io.wdata1 }
  when(io.wen2 && io.waddr2 =/= 0.U) { rf(io.waddr2) := io.wdata2 }
  when(io.wen3 && io.waddr3 =/= 0.U) { rf(io.waddr3) := io.wdata3 }

  for (i <- 0 until 32) {
    io.arch_rdata(i) := Mux(i.U === 0.U, 0.U, rf(io.arch_raddr(i)))
  }
}
