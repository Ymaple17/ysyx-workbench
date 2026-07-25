package core

import chisel3._
import chisel3.util._

class IFU_PC_IO extends Bundle{
  val next_pc = Output(UInt(32.W))
}

class IFU_IDU_IO extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
}

class IFU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFU_PC_IO))
  val out = Decoupled(new IFU_IDU_IO)
  val pc = Decoupled(new IFU_PC_IO)
  
  val imem_rdata = Input(UInt(xlen.W))
  val imem_raddr = Output(UInt(xlen.W))
}

class IFU(val conf: CoreConfig) extends Module{

    val io = IO(new IFU_IO(conf.xlen))

    val pc_init = "h8000_0000".U(conf.xlen.W)

    val pc_reg = RegInit(pc_init)
    when(io.in.fire) {
      pc_reg := io.in.bits.next_pc
    }

    val current_pc = pc_reg
    val pc_plus4 = current_pc + 4.U(conf.xlen.W)

    io.imem_raddr := current_pc

    io.out.bits.inst := io.imem_rdata
    io.out.bits.pc := current_pc
    io.out.valid := true.B

    io.pc.bits.next_pc := Mux(io.in.valid, io.in.bits.next_pc, pc_plus4)
    io.pc.valid := true.B

    io.in.ready := true.B
}