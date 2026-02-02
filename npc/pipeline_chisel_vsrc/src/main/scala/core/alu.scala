package core

import chisel3._
import chisel3.util._

object ALU_OP{
  val ALU_ADD = 0.U(4.W)
  val ALU_SUB = 1.U(4.W)
  val ALU_AND = 2.U(4.W)
  val ALU_CMP = 3.U(4.W)
  val ALU_XOR = 4.U(4.W)
  val ALU_CMPU= 5.U(4.W)
  val ALU_OR  = 6.U(4.W)
  val ALU_SRA = 7.U(4.W)
  val ALU_SRL = 8.U(4.W)
  val ALU_SLL = 9.U(4.W)
  val ALU_NONE =10.U(4.W)
}

class ALU_IO(width: Int) extends Bundle {
  val A = Input(UInt(width.W))
  val B = Input(UInt(width.W))
  val alu_control = Input(UInt(4.W))
  val result = Output(UInt(width.W))
  val overflow_flag = Output(Bool())
  val zero_flag = Output(Bool())
  val negative_flag = Output(Bool())
  val carry_flag = Output(Bool())
}

class ALU(val width: Int) extends Module {
  val io = IO(new ALU_IO(width))

  val alu_add = io.A + io.B
  val alu_sub = io.A - io.B

  import ALU_OP._

  io.result := MuxLookup(io.alu_control, 0.U)(Seq(
    ALU_ADD  -> alu_add,
    ALU_SUB  -> alu_sub,
    ALU_SLL  -> (io.A << io.B(4, 0)),
    ALU_SRL  -> (io.A >> io.B(4, 0)),
    ALU_SRA  -> (io.A.asSInt >> io.B(4, 0)).asUInt,
    ALU_AND  -> (io.A & io.B),
    ALU_OR   -> (io.A | io.B),
    ALU_XOR  -> (io.A ^ io.B),
    ALU_CMP  -> (io.A.asSInt < io.B.asSInt).asUInt,
    ALU_CMPU -> (io.A < io.B).asUInt
  ))

  io.zero_flag := (io.result === 0.U)
  io.negative_flag := io.result(width - 1)

  val add_overflow = (io.A(width - 1) === io.B(width - 1)) && (io.A(width - 1) =/= alu_add(width - 1))
  val sub_overflow = (io.A(width - 1) =/= io.B(width - 1)) && (io.A(width - 1) =/= alu_sub(width - 1))
  
  io.overflow_flag := Mux(io.alu_control === ALU_ADD, add_overflow, 
                      Mux(io.alu_control === ALU_SUB, sub_overflow, false.B))
  
  io.carry_flag := Mux(io.alu_control === ALU_ADD, (io.A > (0.U(width.W) - 1.U - io.B)), false.B)
}