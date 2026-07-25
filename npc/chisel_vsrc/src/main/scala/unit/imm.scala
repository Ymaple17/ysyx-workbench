package unit

import chisel3._
import chisel3.util._
import common.IMM_TYPE._

class IMM_IO extends Bundle{
    val inst = Input(UInt(32.W))
    val imm_type = Input(UInt(3.W))
    val imm_ext = Output(UInt(32.W))
}


class IMM extends Module{
    val io = IO(new IMM_IO)

    val imm_I = io.inst(31, 20).asSInt
    val imm_U = Cat(io.inst(31, 12), 0.U(12.W)).asSInt
    val imm_J = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt
    val imm_S = Cat(io.inst(31, 25), io.inst(11, 7)).asSInt
    val imm_B = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt

    io.imm_ext := MuxLookup(io.imm_type, imm_I)(Seq(
        ImmI -> imm_I,
        ImmU -> imm_U,
        ImmJ -> imm_J,
        ImmS -> imm_S,
        ImmB -> imm_B
    )).asUInt
}