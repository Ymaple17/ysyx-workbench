package core

import chisel3._
import chisel3.util._
import INST_Control._
import REG_WRITE_SEL._
import CSR_SEL._

class WBU_IO(conf: CoreConfig) extends Bundle{
  val in = Flipped(Decoupled(new LSU_WBU_IO))
  val refile = Flipped(new Refile_WRITE_IO(conf.xlen))
  val csr = Flipped(new CSR_WRITE_IO(conf.xlen))
  
  val state_read = Input(new State)
  val state_write = Output(new State)
  val state_write_en = Output(Bool())

  val is_flush = Input(Bool())
  val ebreak = if(!conf.useDPIC) Some(Output(Bool())) else None
}

class WBU(val conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_WBU"

    val io = IO(new WBU_IO(conf))

    val ebreak = if(conf.useDPIC) Some(Module(new Ebreak)) else None
    if(conf.useDPIC) ebreak.get.io.is_ebreak := io.in.bits.is_ebreak
    
    if(!conf.useDPIC) io.ebreak.get := io.in.bits.is_ebreak && io.in.valid && !io.is_flush

    io.refile.wdata := MuxLookup(io.in.bits.signals.wbu.reg_write_sel, io.in.bits.alu_result)(Seq(
      ALU_SEL -> io.in.bits.alu_result,
      PC4_SEL -> io.in.bits.pc_plus4,
      MEM_SEL -> io.in.bits.mem_read,
      CSR_DATA -> io.in.bits.csr_rd1
    ))
    
    io.csr.wdata := MuxLookup(io.in.bits.signals.wbu.csr_sel, 0.U)(Seq(
      CSR_RD1 -> io.in.bits.rd1,
      CSR_XOR -> (io.in.bits.rd1 | io.in.bits.csr_rd1),
      CSR_PC  -> io.in.bits.pc
    ))

    io.refile.wen := io.in.bits.signals.wbu.reg_write & io.in.valid & !io.is_flush
    io.refile.waddr := io.in.bits.waddr

    io.csr.wen := io.in.bits.signals.wbu.csr_write & io.in.valid & !io.is_flush
    io.csr.waddr := io.in.bits.csr_waddr

    io.state_write := io.in.bits.state
    io.state_write_en := io.in.valid & !io.is_flush

    io.in.ready := true.B

  }