package core

import chisel3._
import chisel3.util._
import unit._

class IDU_EXU_IO extends Bundle{
  val signals = new Bundle{
    val exu = new EXU_signals
    val lsu = new LSU_signals
    val wbu = new WBU_signals
  }
  val rd1 = Output(UInt(32.W))
  val rd2 = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val imm_ext = Output(UInt(32.W))
  val waddr = Output(UInt(5.W))
  val is_ebreak = Output(Bool())

  val csr_rd1 = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))
}

class IDU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFU_IDU_IO))
  val out = Decoupled(new IDU_EXU_IO)
  val ifu_signals = Irrevocable(new IFU_signals)
  val refile = Flipped(new Refile_READ_IO(xlen))
  val csr = Flipped(new CSR_READ_IO(xlen))
  val rs1_ren = Output(Bool())
  val rs2_ren = Output(Bool())
}

class IDU(val conf: CoreConfig) extends Module{

    val io = IO(new IDU_IO(conf.xlen))

    val control = Module(new Control)
    val imm = Module(new IMM)

    control.io.inst := io.in.bits.inst
    imm.io.inst := io.in.bits.inst
    imm.io.imm_type := control.io.signals.idu.imm_type

    //refile
    io.refile.raddr1 := io.in.bits.inst(19,15)
    io.refile.raddr2 := io.in.bits.inst(24,20)
    io.rs1_ren := control.io.signals.idu.rs1_ren
    io.rs2_ren := control.io.signals.idu.rs2_ren

    //exu
    io.out.bits.signals := control.io.signals
    io.out.bits.rd1 := io.refile.rdata1
    io.out.bits.rd2 := io.refile.rdata2
    io.out.bits.pc := io.in.bits.pc
    io.out.bits.imm_ext := imm.io.imm_ext
    io.out.bits.waddr := io.in.bits.inst(11,7)
    io.out.bits.is_ebreak := (io.in.bits.inst === "h00100073".U(32.W))

    io.out.bits.csr_rd1 := io.csr.rdata
    io.out.bits.csr_waddr := io.in.bits.inst(31,20)
    io.csr.raddr := io.in.bits.inst(31,20)

    //ifu signals
    io.ifu_signals.valid := control.io.signals.ifu.valid
    io.ifu_signals.bits := control.io.signals.ifu.bits
    control.io.signals.ifu.ready := io.ifu_signals.ready

    //single control
    io.in.ready := true.B
    io.out.valid := true.B
}