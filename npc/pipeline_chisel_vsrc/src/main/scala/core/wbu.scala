package core

import chisel3._
import chisel3.util._

import sim.Ebreak

class WBU_IO(conf: CoreConfig) extends Bundle{
  val in = Flipped(Decoupled(new LSU_WBU_IO))
  val refile = Flipped(new Refile_WRITE_IO(conf.xlen))
  val csr = Flipped(new CSR_WRITE_IO(conf.xlen))

  val state_write = Output(new State)
  val state_write_en = Output(Bool())

  val is_flush = Input(Bool())

  val ebreak = if(!conf.useDPIC) Some(Output(Bool())) else None
}

class WBU(val conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_WBU"
    val io = IO(new WBU_IO(conf))

    val ebreak = if (conf.useDPIC) Some(Module(new Ebreak)) else None

    ebreak.foreach(_.io.is_ebreak := io.in.bits.is_ebreak)
    
    if(!conf.useDPIC) io.ebreak.get := io.in.bits.is_ebreak && io.in.valid && !io.is_flush

    //regfile
    io.refile.wdata := io.in.bits.wb_data

    io.refile.wen := io.in.bits.signals.wbu.reg_write & io.in.valid
    io.refile.waddr := io.in.bits.waddr

    //csr
    io.csr.wdata := io.in.bits.csr_wdata

    io.csr.wen := io.in.bits.signals.wbu.csr_write & io.in.valid
    io.csr.waddr := io.in.bits.csr_waddr

    //state
    io.state_write := io.in.bits.state
    io.state_write_en := io.in.valid & !io.is_flush

    //pipeline control
    io.in.ready := true.B

  }