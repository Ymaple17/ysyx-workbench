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

}

class WBU(val conf: CoreConfig) extends Module{
    val io = IO(new WBU_IO(conf))

    val ebreak = Module(new Ebreak)

    ebreak.io.is_ebreak := io.in.bits.is_ebreak & io.in.valid

    io.refile.wdata := MuxLookup(io.in.bits.signals.wbu.reg_write_sel, io.in.bits.alu_result)(Seq(
      ALU_SEL -> io.in.bits.alu_result,
      IMM_SEL -> io.in.bits.imm_ext,
      PC4_SEL -> (io.in.bits.pc + 4.U),
      MEM_SEL -> io.in.bits.mem_read,
      CSR_DATA -> io.in.bits.csr_rd1
    ))

    io.csr.wdata := MuxLookup(io.in.bits.signals.wbu.csr_sel, 0.U)(Seq(
      CSR_RD1 -> io.in.bits.rd1,
      CSR_XOR -> (io.in.bits.rd1 | io.in.bits.csr_rd1),
      CSR_PC  -> io.in.bits.pc
    ))

    io.refile.wen := io.in.bits.signals.wbu.reg_write & io.in.valid
    io.refile.waddr := io.in.bits.waddr

    io.csr.wen := io.in.bits.signals.wbu.csr_write & io.in.valid
    io.csr.waddr := io.in.bits.csr_waddr

    //mul control
    io.in.ready := true.B

  }