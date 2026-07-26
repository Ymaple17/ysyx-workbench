package core

import chisel3._
import chisel3.util._
import unit._
import common.ALU_SRCA._
import common.ALU_SRCB._
import common.PC_SEL._


class EXU_PC_IO extends Bundle{
  val pc4_imm = Output(UInt(32.W))
  val pc4_rs2 = Output(UInt(32.W))
  val pc4 = Output(UInt(32.W))
  val pc_src = Output(UInt(3.W))
}

class EXU_LSU_IO extends Bundle{
  val signals = new Bundle{
    val lsu = new LSU_signals
    val wbu = new WBU_signals
  }
  val alu_result = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val next_pc = Output(UInt(32.W))
  val imm_ext = Output(UInt(32.W))
  val rd1 = Output(UInt(32.W))
  val rd2 = Output(UInt(32.W))
  val waddr = Output(UInt(5.W))
  val is_ebreak = Output(Bool())

  val csr_rd1 = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))
}

class EXU_IO extends Bundle{
  val in = Flipped(Decoupled(new IDU_EXU_IO))
  val out = Decoupled(new EXU_LSU_IO)
  val pc = Decoupled(new EXU_PC_IO)
}

class JUMP_PC_IO extends Bundle{
  val pc = Input(UInt(32.W))
  val imm = Input(UInt(32.W))
  val next_pc = Output(UInt(32.W))
}

class EXU(val conf: CoreConfig) extends Module{

    val io = IO(new EXU_IO)

    val alu = Module(new ALU(conf.xlen))
    val exu_pc = Module(new PC(conf))
    val jump_pc = Wire(new JUMP_PC_IO)
    jump_pc.next_pc := jump_pc.pc + jump_pc.imm & (~1.U(32.W))

    val alu_srcA = Wire(UInt(conf.xlen.W))
    val alu_srcB = Wire(UInt(conf.xlen.W))
    alu_srcA := MuxLookup(io.in.bits.signals.exu.alu_srcA, io.in.bits.rd1)(Seq(
        ALU_A_RD1 -> io.in.bits.rd1,
        ALU_A_PC -> io.in.bits.pc
    ))
    alu_srcB := MuxLookup(io.in.bits.signals.exu.alu_srcB, io.in.bits.rd2)(Seq(
        ALU_B_RD2 -> io.in.bits.rd2,
        ALU_B_IMM -> io.in.bits.imm_ext
    ))
    alu.io.A := alu_srcA
    alu.io.B := alu_srcB
    alu.io.alu_control := io.in.bits.signals.exu.alu_control

    exu_pc.io.jump := io.in.bits.signals.exu.jump
    exu_pc.io.zero_flag := alu.io.zero_flag
    exu_pc.io.cmp_flag := alu.io.result(0)

    jump_pc.pc := io.in.bits.pc
    jump_pc.imm := io.in.bits.imm_ext

    io.pc.bits.pc4 := io.in.bits.pc + 4.U
    io.pc.bits.pc4_imm := jump_pc.next_pc
    io.pc.bits.pc4_rs2 := alu.io.result & (~1.U(32.W))
    io.pc.bits.pc_src := exu_pc.io.pc_src

    //LSU
    io.out.bits.signals := io.in.bits.signals
    io.out.bits.alu_result := alu.io.result
    io.out.bits.pc := io.in.bits.pc

    io.out.bits.next_pc := MuxLookup(exu_pc.io.pc_src, io.in.bits.pc + 4.U)(Seq(
      PC_PLUS4 -> (io.in.bits.pc + 4.U),
      PC_IMM   -> jump_pc.next_pc,
      PC_RS2   -> (alu.io.result & (~1.U(32.W)))
    ))

    io.out.bits.imm_ext := io.in.bits.imm_ext
    io.out.bits.rd1 := io.in.bits.rd1
    io.out.bits.rd2 := io.in.bits.rd2
    io.out.bits.waddr := io.in.bits.waddr
    io.out.bits.is_ebreak := io.in.bits.is_ebreak

    io.out.bits.csr_rd1 := io.in.bits.csr_rd1
    io.out.bits.csr_waddr := io.in.bits.csr_waddr


    //mul control
    io.in.ready := io.out.ready
    io.out.valid := io.in.valid
    io.pc.valid := io.out.valid && io.in.ready
}