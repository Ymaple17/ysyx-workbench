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
  override def desiredName = "ysyx_25020039_ALU"

  val io = IO(new ALU_IO(width))

  import ALU_OP._

  val is_sub = io.alu_control === ALU_SUB || io.alu_control === ALU_CMP || io.alu_control === ALU_CMPU
  val op2_inv = Mux(is_sub, ~io.B, io.B)
  val adder_result_ext = Wire(UInt((width + 1).W))
  adder_result_ext := io.A +& op2_inv + is_sub.asUInt

  val adder_result = adder_result_ext(width-1, 0)
  val sub_overflow_check = (io.A(width - 1) =/= io.B(width - 1)) && (io.A(width - 1) =/= adder_result(width - 1))
  val slt = adder_result(width-1) ^ sub_overflow_check
  val sltu = !adder_result_ext(width)

  val shamt = io.B(4, 0)
  val shin = Mux(io.alu_control === ALU_SLL, Reverse(io.A), io.A)
  val shift_right_logic = (Cat(Mux(io.alu_control === ALU_SRA, io.A(width-1), 0.U), shin).asSInt >> shamt).asUInt
  val shift_result = Mux(io.alu_control === ALU_SLL, Reverse(shift_right_logic(width-1, 0)), shift_right_logic(width-1, 0))

  io.result := MuxLookup(io.alu_control, 0.U)(Seq(
    ALU_ADD  -> adder_result,
    ALU_SUB  -> adder_result,
    ALU_SLL  -> shift_result,
    ALU_SRL  -> shift_result,
    ALU_SRA  -> shift_result,
    ALU_AND  -> (io.A & io.B),
    ALU_OR   -> (io.A | io.B),
    ALU_XOR  -> (io.A ^ io.B),
    ALU_CMP  -> slt.asUInt,
    ALU_CMPU -> sltu.asUInt
  ))

  io.zero_flag := (io.result === 0.U)
  io.negative_flag := io.result(width - 1)

  val add_overflow = (io.A(width - 1) === io.B(width - 1)) && (io.A(width - 1) =/= adder_result(width - 1))
  
  io.overflow_flag := Mux(io.alu_control === ALU_ADD, add_overflow, 
                      Mux(io.alu_control === ALU_SUB, sub_overflow_check, false.B))
  io.carry_flag := Mux(io.alu_control === ALU_ADD, adder_result_ext(width), false.B)
}