package unit

import chisel3._
import chisel3.util._

import common.JUMP_TYPE._
import common.PC_SEL._
import core.CoreConfig

class PC_IO extends Bundle{
  val jump  = Input(UInt(4.W))
  val zero_flag = Input(UInt(1.W))
  val cmp_flag = Input(UInt(1.W))
  val pc_src = Output(UInt(3.W))
}

class PC(val conf: CoreConfig) extends Module{
    val io = IO(new PC_IO)
    io.pc_src := MuxCase(PC_NONE, Seq(
        (io.jump === JUMP_MERT)-> MEPC,
        (io.jump === JUMP_JAL) -> PC_IMM,
        (io.jump === JUMP_JALR)-> PC_RS2,
        (io.jump === JUMP_BEQ ) -> Mux(io.zero_flag.asBool, PC_IMM, PC_PLUS4),
        (io.jump === JUMP_BNE ) -> Mux(io.zero_flag.asBool, PC_PLUS4, PC_IMM),
        (io.jump === JUMP_BGE ) -> Mux(io.cmp_flag.asBool, PC_PLUS4, PC_IMM),
        (io.jump === JUMP_BGEU) -> Mux(io.cmp_flag.asBool, PC_PLUS4, PC_IMM),
        (io.jump === JUMP_BLT ) -> Mux(io.cmp_flag.asBool, PC_IMM, PC_PLUS4),
        (io.jump === JUMP_BLTU) -> Mux(io.cmp_flag.asBool, PC_IMM, PC_PLUS4)
    ))
}