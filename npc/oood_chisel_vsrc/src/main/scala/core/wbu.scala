package core

import chisel3._
import chisel3.util._

import sim.Ebreak
import common.REG_WRITE_SEL._

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

    // 4d：ebreak 改到 commit 触发；WBU 不再结束仿真
    val ebreak = Module(new Ebreak)
    ebreak.io.is_ebreak := false.B
    if(!conf.useDPIC) io.ebreak.get := false.B

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

    // 4b：CSR 架构写在 commit，WBU 不再 wen
    io.csr.wdata := 0.U
    io.csr.wen   := false.B
    io.csr.waddr := 0.U

    //state
    io.state_write := io.in.bits.state
    io.state_write_en := io.in.valid & !io.is_flush

    //pipeline control
    io.in.ready := true.B

  }