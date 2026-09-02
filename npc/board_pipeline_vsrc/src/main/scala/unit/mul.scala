package unit

import chisel3._
import common.ALU_OP._

class MUL_IO extends Bundle {
  val req_valid = Input(Bool())
  val req_ready = Output(Bool())
  val result_ready = Input(Bool())
  val kill = Input(Bool())
  val a = Input(UInt(32.W))
  val b = Input(UInt(32.W))
  val op = Input(UInt(5.W))
  val result = Output(UInt(32.W))
  val result_valid = Output(Bool())
  val busy = Output(Bool())
}

class MUL extends Module {
  val io = IO(new MUL_IO)

  val valid = RegInit(false.B)
  val product = Reg(UInt(64.W))
  val subtractA = Reg(UInt(32.W))
  val subtractB = Reg(UInt(32.W))
  val op = Reg(UInt(5.W))

  io.req_ready := !valid || io.result_ready
  io.busy := valid
  io.result_valid := valid && !io.kill
  val correctedHigh = product(63, 32) - subtractA - subtractB
  io.result := Mux(op === ALU_MUL, product(31, 0), correctedHigh)

  when(io.kill) {
    valid := false.B
  }.elsewhen(io.req_ready) {
    valid := io.req_valid
    when(io.req_valid) {
      // A single unsigned 32x32 product is sufficient for every RV32M form.
      // Signed high halves are corrected modulo 2^32 after the product
      // register, avoiding a wider dynamic-signed multiplier in the DSP path.
      product := io.a * io.b
      subtractA := Mux(
        (io.op === ALU_MULH || io.op === ALU_MULHSU) && io.a(31),
        io.b, 0.U)
      subtractB := Mux(io.op === ALU_MULH && io.b(31), io.a, 0.U)
      op := io.op
    }
  }
}
