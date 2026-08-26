package unit

import chisel3._
import chisel3.util._
import common.ALU_OP._

class DIV_IO extends Bundle {
    val req_valid = Input(Bool())
    val req_ready = Output(Bool())
    val kill = Input(Bool())

    val a = Input(UInt(32.W))
    val b = Input(UInt(32.W))
    val op = Input(UInt(5.W))

    val result = Output(UInt(32.W))
    val result_valid = Output(Bool())
    val busy = Output(Bool())
}

class DIV extends Module{
    val io = IO(new DIV_IO)

    val s_IDLE :: s_BUSY :: Nil = Enum(2)
    val state = RegInit(s_IDLE)
    // cnt 需计到 32（0..32 共 33 值）→ 6 位；5 位会把 32 截成 0
    val cnt = RegInit(0.U(6.W))

    val a_abs = Reg(UInt(32.W))
    val b_abs = Reg(UInt(32.W))
    val rem = Reg(UInt(32.W))
    val quo = Reg(UInt(32.W))
    val a_neg = RegInit(false.B)
    val b_neg = RegInit(false.B)
    val is_rem = RegInit(false.B)
    val div_by_zero = RegInit(false.B)
    val a_latched = Reg(UInt(32.W))

    io.req_ready := (state === s_IDLE)
    io.busy := (state === s_BUSY)
    io.result_valid := (state === s_BUSY) && (cnt === 0.U) && !io.kill

    // 除零余数用锁存的被除数，避免 io.a 在结果拍已变
    io.result := Mux(div_by_zero,
      Mux(is_rem, a_latched, "hFFFFFFFF".U(32.W)),
      Mux(is_rem,
        Mux(a_neg, (~rem) + 1.U, rem),
        Mux(a_neg ^ b_neg, (~quo) + 1.U, quo)))

    when(io.kill) {
        state := s_IDLE
        cnt := 0.U
    }.elsewhen(state === s_IDLE) {
        when(io.req_valid) {
            val is_signed = io.op === ALU_DIV || io.op === ALU_REM
            a_abs := Mux(is_signed && io.a(31), (~io.a) + 1.U, io.a)
            b_abs := Mux(io.op === ALU_DIVU || io.op === ALU_REMU, io.b, Mux(io.b(31), (~io.b) + 1.U, io.b))
            a_neg := is_signed && io.a(31)
            b_neg := is_signed && io.b(31)
            is_rem := io.op === ALU_REM || io.op === ALU_REMU
            div_by_zero := io.b === 0.U
            a_latched := io.a
            rem := 0.U
            quo := 0.U
            cnt := 32.U
            state := s_BUSY
        }
    }.elsewhen(state === s_BUSY) {
        when(cnt =/= 0.U) {
            // restoring：先移位再与 b_abs 比较
            val bit_idx = (cnt - 1.U)(4, 0)
            val shifted = Cat(rem(30, 0), a_abs(bit_idx))
            rem := shifted
            when(shifted >= b_abs) {
                rem := shifted - b_abs
                quo := quo | (1.U << bit_idx)
            }
            cnt := cnt - 1.U
        }
        when(cnt === 0.U) {
            state := s_IDLE
        }
    }
}
