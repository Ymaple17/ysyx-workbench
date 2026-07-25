package core

import chisel3._
import chisel3.util._
import common.MEM_READ._
import unit.WBU_signals

class LSU_WBU_IO extends Bundle{
  val signals = new Bundle{
    val wbu = new WBU_signals
  }

  val rd1 = UInt(32.W)
  val alu_result = UInt(32.W)
  val pc = UInt(32.W)
  val next_pc = UInt(32.W)
  val imm_ext = UInt(32.W)
  val mem_read = UInt(32.W)
  val waddr = UInt(5.W)
  val is_ebreak = Bool()

  val csr_rd1 = UInt(32.W)
  val csr_waddr = UInt(12.W)
}

class LSU_IO (xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new EXU_LSU_IO))
  val out = Decoupled(new LSU_WBU_IO)
  
  val dmem_valid = Output(Bool())
  val dmem_wen = Output(Bool())
  val dmem_waddr = Output(UInt(xlen.W))
  val dmem_wdata = Output(UInt(xlen.W))
  val dmem_wmask = Output(UInt((xlen/4).W))
  val dmem_raddr = Input(UInt(xlen.W))
}

class LSU(val conf: CoreConfig) extends Module{

  val io = IO(new LSU_IO(conf.xlen))

  val is_load = Wire(Bool())
  val is_store = Wire(Bool())
  val mem_valid = io.in.bits.signals.lsu.mem_valid
  val mem_write = io.in.bits.signals.lsu.mem_write
  is_load := ~mem_write & mem_valid
  is_store := mem_write & mem_valid

  val temp_addr = Wire(UInt(conf.xlen.W))
  val data_offset = Wire(UInt(log2Ceil(conf.xlen/8).W))
  temp_addr := io.in.bits.alu_result & (~(conf.xlen/8-1).U(conf.xlen.W))
  data_offset := io.in.bits.alu_result - temp_addr

  val mem_wmask_4b = Wire(UInt(4.W))
  mem_wmask_4b := io.in.bits.signals.lsu.mem_wmask << data_offset
  val mem_wmask = Cat(0.U(4.W), mem_wmask_4b)

  val store_data_shifted = io.in.bits.rd2 << (data_offset << 3)
  val rd_offset = io.dmem_raddr >> (data_offset << 3)  

  val rbyte   = rd_offset(7, 0).asSInt.pad(32).asUInt
  val rhalf  = rd_offset(15, 0).asSInt.pad(32).asUInt
  val rword   = rd_offset(31, 0)
  val rbyteu  = Cat(0.U(24.W), rd_offset(7, 0))
  val rhalfu = Cat(0.U(16.W), rd_offset(15, 0))

  io.out.bits.mem_read := MuxLookup(io.in.bits.signals.lsu.mem_rd, rbyte)(Seq(
      RBYTE   -> rbyte,
      RHALF  -> rhalf,
      RWORD   -> rword,
      RBYTEU  -> rbyteu,
      RHALFU -> rhalfu
  ))

  //WBU
  io.out.bits.signals := io.in.bits.signals
  io.out.bits.rd1 := io.in.bits.rd1
  io.out.bits.alu_result := io.in.bits.alu_result
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.next_pc := io.in.bits.next_pc
  io.out.bits.imm_ext := io.in.bits.imm_ext
  io.out.bits.waddr := io.in.bits.waddr
  io.out.bits.is_ebreak := io.in.bits.is_ebreak
  io.out.bits.csr_rd1 := io.in.bits.csr_rd1
  io.out.bits.csr_waddr := io.in.bits.csr_waddr


  //pmem
  io.dmem_valid := io.in.valid && mem_valid
  io.dmem_wen := is_store
  io.dmem_waddr := io.in.bits.alu_result
  io.dmem_wdata := store_data_shifted
  io.dmem_wmask := mem_wmask

  io.in.ready := true.B
  io.out.valid := true.B
}