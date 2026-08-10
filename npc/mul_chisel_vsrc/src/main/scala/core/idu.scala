package core

import chisel3._
import chisel3.util._
import unit._
import common.JUMP_TYPE._
import core.PerfEvents._

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
    io.ifu_signals.valid := io.in.valid && control.io.signals.ifu.valid
    io.ifu_signals.bits := control.io.signals.ifu.bits
    control.io.signals.ifu.ready := io.ifu_signals.ready

    //mul control
    io.in.ready := io.out.ready
    io.out.valid := io.in.valid

  if(conf.statistics){
    val sigs = control.io.signals
    val fire = io.out.valid && io.out.ready

    val is_load = sigs.lsu.mem_valid && !sigs.lsu.mem_write
    val is_store = sigs.lsu.mem_valid && sigs.lsu.mem_write
    val is_csr = sigs.wbu.csr_write
    val jump = sigs.exu.jump

    val is_branch = (jump === JUMP_BEQ || jump === JUMP_BNE || jump === JUMP_BLT || jump === JUMP_BGE || jump === JUMP_BLTU || jump === JUMP_BGEU)
    val is_jump = (jump === JUMP_JAL || jump === JUMP_JALR)

    val is_other = (jump === JUMP_MERT) || (io.ifu_signals.valid && io.ifu_signals.bits.is_fencei) || (io.out.bits.is_ebreak)

    val is_compute = !is_load && !is_store && !is_csr && !is_branch && !is_jump && !is_other

    PM(conf, clock, EVENT_INST_TYPE_LOAD, 1.U, fire && is_load)
    PM(conf, clock, EVENT_INST_TYPE_STORE, 1.U, fire && is_store)
    PM(conf, clock, EVENT_INST_TYPE_CSR, 1.U, fire && is_csr)
    PM(conf, clock, EVENT_INST_TYPE_BRANCH, 1.U, fire && is_branch)
    PM(conf, clock, EVENT_INST_TYPE_JUMP, 1.U, fire && is_jump)
    PM(conf, clock, EVENT_INST_TYPE_OTHER, 1.U, fire && is_other)
    PM(conf, clock, EVENT_INST_TYPE_COMPUTE, 1.U, fire && is_compute)
  }
}