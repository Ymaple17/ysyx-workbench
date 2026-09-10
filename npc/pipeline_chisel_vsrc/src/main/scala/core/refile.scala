package core

import chisel3._
import chisel3.util._

class Refile_READ_IO(xlen: Int) extends Bundle{
  val raddr1 = Input(UInt(5.W))
  val raddr2 = Input(UInt(5.W))
  val rdata1 = Output(UInt(xlen.W))
  val rdata2 = Output(UInt(xlen.W))
}

class Refile_WRITE_IO(xlen: Int) extends Bundle{
  val wdata = Input(UInt(xlen.W))
  val waddr = Input(UInt(5.W))
  val wen = Input(Bool())
}

class Refile_IO(xlen: Int) extends Bundle{
  val read = new Refile_READ_IO(xlen)
  val write = new Refile_WRITE_IO(xlen)
}

class Refile(conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_Refile"

  val io = IO(new Refile_IO(conf.xlen))
  val rf = RegInit(VecInit(Seq.fill(16)(0.U(conf.xlen.W))))
  io.read.rdata1 := Mux(io.read.raddr1 === 0.U, 0.U, rf(io.read.raddr1(3, 0)))
  io.read.rdata2 := Mux(io.read.raddr2 === 0.U, 0.U, rf(io.read.raddr2(3, 0)))
  when(io.write.wen && io.write.waddr =/= 0.U) {
    rf(io.write.waddr(3, 0)) := io.write.wdata
  }
}