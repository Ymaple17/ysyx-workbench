package core

import chisel3._
import chisel3.util._
import unit._
import common.ALU_OP._
import common.ALU_SRCA._
import common.ALU_SRCB._
import common.JUMP_TYPE._
import common.PC_SEL._
import core.PerfEvents._


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
  val memory_address = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val next_pc = Output(UInt(32.W))
  val imm_ext = Output(UInt(32.W))
  val rd1 = Output(UInt(32.W))
  val rd2 = Output(UInt(32.W))
  val waddr = Output(UInt(5.W))
  val is_ebreak = Output(Bool())

  val csr_rd1 = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))

  val state = Output(new State)
}

class EXU_IO extends Bundle{
  val in = Flipped(Decoupled(new IDU_EXU_IO))
  val out = Decoupled(new EXU_LSU_IO)
  val pc = Decoupled(new EXU_PC_IO)

  val is_flush = Input(Bool())
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
    alu_srcA := Mux(io.in.bits.signals.exu.alu_srcA === ALU_A_PC,
      io.in.bits.pc, io.in.bits.rd1)
    alu_srcB := Mux(io.in.bits.signals.exu.alu_srcB === ALU_B_IMM,
      io.in.bits.imm_ext, io.in.bits.rd2)
    alu.io.A := alu_srcA
    alu.io.B := alu_srcB
    alu.io.alu_control := io.in.bits.signals.exu.alu_control
    // Every RV32 load/store effective address is rs1 + immediate. A dedicated
    // adder keeps DCache preissue off the generic ALU source/result mux cone.
    val memoryAddress = io.in.bits.rd1 + io.in.bits.imm_ext

    // Resolve branches directly from the source operands. Routing the generic
    // ALU result mux into PC selection puts the full arithmetic datapath on the
    // branch-recovery path even though only equality or less-than is needed.
    val branchOperandsEqual = io.in.bits.rd1 === io.in.bits.rd2
    val branchSignedLess = io.in.bits.rd1.asSInt < io.in.bits.rd2.asSInt
    val branchUnsignedLess = io.in.bits.rd1 < io.in.bits.rd2
    val branchUsesUnsigned = io.in.bits.signals.exu.jump === JUMP_BLTU ||
      io.in.bits.signals.exu.jump === JUMP_BGEU

    exu_pc.io.jump := io.in.bits.signals.exu.jump
    exu_pc.io.zero_flag := branchOperandsEqual
    exu_pc.io.cmp_flag := Mux(branchUsesUnsigned, branchUnsignedLess, branchSignedLess)

    jump_pc.pc := io.in.bits.pc
    jump_pc.imm := io.in.bits.imm_ext
    val jalrTarget = (io.in.bits.rd1 + io.in.bits.imm_ext) & (~1.U(32.W))

    io.pc.bits.pc4 := io.in.bits.pc + 4.U
    io.pc.bits.pc4_imm := jump_pc.next_pc
    io.pc.bits.pc4_rs2 := jalrTarget
    io.pc.bits.pc_src := exu_pc.io.pc_src

    //LSU
    io.out.bits.signals := io.in.bits.signals
    
    io.out.bits.pc := io.in.bits.pc

    io.out.bits.next_pc := MuxLookup(exu_pc.io.pc_src, io.in.bits.pc + 4.U)(Seq(
      PC_PLUS4 -> (io.in.bits.pc + 4.U),
      PC_IMM   -> jump_pc.next_pc,
      PC_RS2   -> jalrTarget
    ))

    io.out.bits.imm_ext := io.in.bits.imm_ext
    io.out.bits.rd1 := io.in.bits.rd1
    io.out.bits.rd2 := io.in.bits.rd2
    io.out.bits.waddr := io.in.bits.waddr
    io.out.bits.is_ebreak := io.in.bits.is_ebreak

    //csr 
    io.out.bits.csr_rd1 := io.in.bits.csr_rd1
    io.out.bits.csr_waddr := io.in.bits.csr_waddr

    //state
    io.out.bits.state := io.in.bits.state

    //DIV
    val is_div = io.in.valid && (io.in.bits.signals.exu.alu_control === ALU_DIV || io.in.bits.signals.exu.alu_control === ALU_DIVU || io.in.bits.signals.exu.alu_control === ALU_REM || io.in.bits.signals.exu.alu_control === ALU_REMU)
    val is_mul = io.in.valid && (io.in.bits.signals.exu.alu_control === ALU_MUL || io.in.bits.signals.exu.alu_control === ALU_MULH || io.in.bits.signals.exu.alu_control === ALU_MULHSU || io.in.bits.signals.exu.alu_control === ALU_MULHU)

    val mul = Module(new MUL)
    // The multiplier consumes only the registered EXU payload. Feeding it
    // directly from IDU forwarding made the DSP input select depend on LSU
    // backpressure and created a DCache-to-DSP timing path across the core.
    // io.in remains valid through the result-consume cycle. Do not present
    // that same instruction as a refill request or the stale product would
    // remain valid for the next multiply entering EXU.
    mul.io.req_valid := is_mul && !mul.io.result_valid && !io.is_flush
    mul.io.a := io.in.bits.rd1
    mul.io.b := io.in.bits.rd2
    mul.io.op := io.in.bits.signals.exu.alu_control
    mul.io.kill := io.is_flush

    val div = Module(new DIV)
    div.io.req_valid := io.in.valid && is_div && div.io.req_ready && !io.is_flush
    div.io.a := io.in.bits.rd1
    div.io.b := io.in.bits.rd2
    div.io.op := io.in.bits.signals.exu.alu_control
    div.io.kill := io.is_flush

    // busy 用 DIV 内部状态：冲刷后 is_div 变 0 时仍挡住新指令，直到 kill 生效
    // 结果拍 result_valid=1 释放，允许下一条同拍进入
    val div_done = div.io.result_valid
    val div_busy = (is_div && !div_done) || (div.io.busy && !div_done)
    val mul_done = mul.io.result_valid
    mul.io.result_ready := mul_done && io.out.ready && io.in.valid && is_mul &&
      !io.is_flush
    val mul_busy = (is_mul && !mul_done) || (mul.io.busy && !mul_done)

    //pipeline control
    io.in.ready := (!io.in.valid || io.out.ready) && !div_busy && !mul_busy
    io.out.valid := io.in.valid && (!is_div || div_done) && (!is_mul || mul_done) && !io.is_flush
    io.pc.valid := io.out.valid && io.in.ready
    //ALU / DIV
    val executeResult = Mux(is_div, div.io.result,
      Mux(is_mul, mul.io.result, alu.io.result))
    io.out.bits.memory_address := memoryAddress
    io.out.bits.alu_result := Mux(io.in.bits.signals.lsu.mem_valid,
      memoryAddress, executeResult)

    if(conf.statistics){
      PM(conf, clock, EVENT_EXU_COMP, 1.U, io.out.valid && io.out.ready)
      PM(conf, clock, EVENT_MUL, 1.U, io.out.valid && io.out.ready && is_mul)
      PM(conf, clock, EVENT_DIV, 1.U, io.out.valid && io.out.ready && is_div)
      PM(conf, clock, EVENT_EXU_MUL_WAIT, 1.U, io.in.valid && is_mul && !mul_done)
      PM(conf, clock, EVENT_EXU_DIV_WAIT, 1.U, io.in.valid && is_div && !div_done)
    }
}
