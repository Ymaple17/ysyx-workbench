package core

import chisel3._
import chisel3.util._
import common.PC_SEL._
import sim._

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    right.valid := left.valid
    right.bits := left.bits
    left.ready := right.ready
  }
}

class Core(val conf: CoreConfig) extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val ebreak = Output(Bool())
    val imem_pc = Output(UInt(32.W))
    val instr = Output(UInt(32.W))
    val wen = Output(Bool())
    val mem_addr = Output(UInt(32.W))
    val mem_valid = Output(Bool())
  })

  val ifu = Module(new IFU(conf))
  val idu = Module(new IDU(conf))
  val exu = Module(new EXU(conf))
  val lsu = Module(new LSU(conf))
  val wbu = Module(new WBU(conf))

  val refile = Module(new Refile(conf))
  val csr = Module(new CSR(conf))

  val imem = Module(new Pmem)
  val dmem = Module(new Pmem)

  StageConnect(ifu.io.out, idu.io.in)
  StageConnect(idu.io.out, exu.io.in)
  StageConnect(exu.io.out, lsu.io.in)
  StageConnect(lsu.io.out, wbu.io.in)

  ifu.io.in.bits.next_pc := MuxLookup(exu.io.pc.bits.pc_src, exu.io.pc.bits.pc4)(Seq(
    PC_PLUS4 -> exu.io.pc.bits.pc4,
    PC_IMM   -> exu.io.pc.bits.pc4_imm,
    PC_RS2   -> exu.io.pc.bits.pc4_rs2,
    MEPC     -> csr.io.read.mepc
  ))
  val is_irq = wbu.io.in.bits.signals.wbu.irq && wbu.io.in.valid
  when(is_irq) {
    ifu.io.in.bits.next_pc := csr.io.read.mtvec
  }

  csr.io.irq := is_irq
  csr.io.irq_no := wbu.io.in.bits.signals.wbu.irq_num
  csr.io.irq_pc := wbu.io.in.bits.pc

  ifu.io.in.valid := true.B
  ifu.io.pc.ready := true.B
  exu.io.pc.ready := true.B
  idu.io.ifu_signals.ready := true.B

  idu.io.refile <> refile.io.read
  wbu.io.refile <> refile.io.write

  idu.io.csr <> csr.io.read
  wbu.io.csr <> csr.io.write

  imem.io.valid := true.B
  imem.io.wen := false.B
  imem.io.raddr := ifu.io.imem_raddr
  imem.io.waddr := 0.U
  imem.io.wdata := 0.U
  imem.io.wmask := 0.U
  ifu.io.imem_rdata := imem.io.rdata

  dmem.io.valid := lsu.io.dmem_valid
  dmem.io.wen := lsu.io.dmem_wen
  dmem.io.raddr := lsu.io.dmem_waddr
  dmem.io.waddr := lsu.io.dmem_waddr
  dmem.io.wdata := lsu.io.dmem_wdata
  dmem.io.wmask := lsu.io.dmem_wmask
  lsu.io.dmem_raddr := dmem.io.rdata

  io.ebreak := wbu.io.in.bits.is_ebreak & wbu.io.in.valid
  io.imem_pc := ifu.io.imem_raddr
  io.instr := ifu.io.out.bits.inst
  io.wen := dmem.io.wen
  io.mem_addr := dmem.io.waddr
  io.mem_valid := dmem.io.valid
}
