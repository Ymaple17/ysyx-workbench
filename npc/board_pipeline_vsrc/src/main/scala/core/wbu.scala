package core

import chisel3._
import chisel3.util._

import sim.Ebreak
import common.REG_WRITE_SEL._
import common.CSR_SEL._

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
    val io = IO(new WBU_IO(conf))

    if(conf.useDPIC) {
      val ebreak = Module(new Ebreak)
      ebreak.io.is_ebreak := RegNext(io.in.valid && io.in.bits.is_ebreak && !io.is_flush, false.B)
    } else {
      io.ebreak.get := io.in.bits.is_ebreak && io.in.valid && !io.is_flush
    }

    //regfile
    io.refile.wdata := MuxLookup(io.in.bits.signals.wbu.reg_write_sel, io.in.bits.alu_result)(Seq(
      ALU_SEL -> io.in.bits.alu_result,
      IMM_SEL -> io.in.bits.imm_ext,
      PC4_SEL -> (io.in.bits.pc + 4.U),
      MEM_SEL -> io.in.bits.mem_read,
      CSR_DATA -> io.in.bits.csr_rd1
    ))

    io.refile.wen := io.in.bits.signals.wbu.reg_write & io.in.valid
    io.refile.waddr := io.in.bits.waddr

    //csr
    io.csr.wdata := MuxLookup(io.in.bits.signals.wbu.csr_sel, 0.U)(Seq(
      CSR_RD1 -> io.in.bits.rd1,
      CSR_XOR -> (io.in.bits.rd1 | io.in.bits.csr_rd1),
      CSR_PC  -> io.in.bits.pc
    ))

    io.csr.wen := io.in.bits.signals.wbu.csr_write & io.in.valid
    io.csr.waddr := io.in.bits.csr_waddr

    //state
    io.state_write := io.in.bits.state
    io.state_write_en := io.in.valid & !io.is_flush

    //pipeline control
    io.in.ready := true.B

  }
