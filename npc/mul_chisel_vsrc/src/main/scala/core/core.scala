package core

import chisel3._
import chisel3.util._
import common.PC_SEL._
import sim._
import bus._
import unit._

class Core_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val ebreak    = if (!conf.useDPIC) Some(Output(Bool())) else None
  val imem      = new AXI4Master
  val dmem      = new AXI4Master
}

class Core(val conf: CoreConfig) extends Module {
  val io = IO(new Core_IO(conf))

  val ifu = Module(new IFU(conf))
  val idu = Module(new IDU(conf))
  val exu = Module(new EXU(conf))
  val lsu = Module(new LSU(conf))
  val wbu = Module(new WBU(conf))

  val refile = Module(new Refile(conf))
  val csr = Module(new CSR(conf))

  val icache = Module(new ICache(set = 64, way = 4, block_size = 64, conf = conf))

  //stage connect
  ifu.io.out <> idu.io.in
  idu.io.out <> exu.io.in
  exu.io.out <> lsu.io.in
  lsu.io.out <> wbu.io.in

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

  ifu.io.imem <> icache.io.in
  icache.io.fencei <> idu.io.ifu_signals
  icache.io.out <> io.imem
  io.dmem <> lsu.io.dmem
  
  if (!conf.useDPIC) {
    io.ebreak.get := idu.io.out.bits.is_ebreak && idu.io.in.valid
  }
  
}
