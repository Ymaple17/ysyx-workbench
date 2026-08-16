package unit

import chisel3._
import chisel3.util._
import common.ALU_OP._

class DIV_IO extends Bundle {
    val req_valid = Input(Bool()) //请求除法
    val req_ready = Output(Bool()) //空间可接收

    val a = Input(UInt(32.W)) //被除数
    val b = Input(UInt(32.W)) //除数
    val op = Input(UInt(5.W)) //除法操作类型（★ 必须与 alu_control 同宽，3 位会截断导致符号判断错误）
    
    val result = Output(UInt(32.W)) //结果
    val result_valid = Output(Bool()) //结果有效
}

class DIV extends Module{
    val io = IO(new DIV_IO)

    val s_IDLE :: s_BUSY :: Nil = Enum(2)
    val state = RegInit(s_IDLE)
    // ★ cnt 需要计到 32（0..32 共 33 个值），必须 6 位！
    //   5 位时 cnt := 32.U（6 位字面量 0b100000）被截断成 0，
    //   导致请求后下一拍 result_valid=1，迭代从未执行，结果恒为 0
    val cnt = RegInit(0.U(6.W)) //32-1迭代 0迭代完成

    //工作寄存器
    val a_abs = Reg(UInt(32.W)) //被除数绝对值
    val b_abs = Reg(UInt(32.W)) //除数绝对值
    val rem = Reg(UInt(32.W)) //余数
    val quo = Reg(UInt(32.W)) //商
    val a_neg = RegInit(false.B) //被除数是否为负数
    val b_neg = RegInit(false.B) //除数是否为负数
    val is_rem = RegInit(false.B) //是否为取余操作
    val div_by_zero = RegInit(false.B) //除数是否为0

    //握手
    io.req_ready := (state === s_IDLE)
    io.result_valid := (state === s_BUSY) && (cnt === 0.U)

    io.result := Mux(div_by_zero, Mux(is_rem, io.a, "hFFFFFFFF".U(32.W)), Mux(is_rem, Mux(a_neg, (~rem) + 1.U, rem), Mux(a_neg ^ b_neg, (~quo) + 1.U, quo)))

    switch(state){
        is(s_IDLE){
            when(io.req_valid){
                val is_signed = io.op === ALU_DIV || io.op === ALU_REM
                a_abs := Mux(is_signed && io.a(31), (~io.a) + 1.U, io.a)
                b_abs := Mux(io.op === ALU_DIVU || io.op === ALU_REMU, io.b, Mux(io.b(31), (~io.b) + 1.U, io.b))
                a_neg := is_signed && io.a(31)
                b_neg := is_signed && io.b(31)
                is_rem := io.op === ALU_REM || io.op === ALU_REMU
                div_by_zero := io.b === 0.U
                rem := 0.U
                quo := 0.U
                cnt := 32.U

                state := s_BUSY
            }
        }
        is(s_BUSY){
            when(cnt =/= 0.U){
                // ★ restoring 除法：必须先移位得到 shifted，再与 b_abs 比较并相减。
                //   若直接用移位前的 rem 比较/相减（旧写法），结果完全错误
                val shifted = Cat(rem(30, 0), a_abs(cnt - 1.U)) //左移并入被除数下一位
                rem := shifted
                when(shifted >= b_abs){
                    rem := shifted - b_abs
                    quo := quo | (1.U << (cnt - 1.U))
                }
                cnt := cnt - 1.U
            }
            when(cnt === 0.U){
                state := s_IDLE
            }
        }
    }

}