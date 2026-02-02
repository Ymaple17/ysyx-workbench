package core

import chisel3._
import chisel3.util._

class IDU_EXU_IO extends Bundle{
  val signals = new Bundle{
    val exu = new EXU_signals
    val lsu = new LSU_signals
    val wbu = new WBU_signals
  }
  val rd1 = Output(UInt(32.W))
  val rd2 = Output(UInt(32.W))
  val csr_rd1 = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val imm_ext = Output(UInt(32.W))
  val waddr = Output(UInt(5.W))
  val csr_waddr = Output(UInt(12.W))
  val is_ebreak = Output(Bool())
  val state = Output(new State)
}

class IDU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFU_IDU_IO))
  val out = Decoupled(new IDU_EXU_IO)
  val ifu_signals = Irrevocable(new IFU_signals)
  val refile = Flipped(new Refile_READ_IO(xlen))
  val csr = Flipped(new CSR_READ_IO(xlen))
  val rs1_ren = Output(Bool())
  val rs2_ren = Output(Bool())
  val is_stall = Input(Bool())
  val is_flush = Input(Bool())
  val is_fencei = Output(Bool())
  val state = Input(new State)
}

class IDU(val conf: CoreConfig) extends Module{
    val io = IO(new IDU_IO(conf.xlen))

    val controller = Module(new Control(conf))
    val imm = Module(new IMM)

    controller.io.inst := io.in.bits.inst
    imm.io.inst := io.in.bits.inst
    imm.io.imm_type := controller.io.signals.idu.imm_type

    //refile
    io.refile.raddr1 := io.in.bits.inst(19,15)
    io.refile.raddr2 := io.in.bits.inst(24,20)
    io.rs1_ren := controller.io.signals.idu.rs1_ren
    io.rs2_ren := controller.io.signals.idu.rs2_ren

    //csr
    io.csr.raddr := io.in.bits.inst(31,20)

    //fence.i
    io.is_fencei := controller.io.signals.ifu.bits.is_fencei && io.in.valid
    io.ifu_signals.valid := io.in.valid
    io.ifu_signals.bits := controller.io.signals.ifu.bits
    controller.io.signals.ifu.ready := io.ifu_signals.ready

    //exu
    io.out.bits.signals := controller.io.signals
    io.out.bits.rd1 := io.refile.rdata1
    io.out.bits.rd2 := io.refile.rdata2
    io.out.bits.csr_rd1 := io.csr.rdata
    io.out.bits.pc := io.in.bits.pc
    io.out.bits.imm_ext := imm.io.imm_ext
    io.out.bits.waddr := io.in.bits.inst(11,7)
    io.out.bits.csr_waddr := io.in.bits.inst(31,20)
    io.out.bits.is_ebreak := (io.in.bits.inst === "h00100073".U(32.W))


    //state
    io.out.bits.state.state := controller.io.irq
    io.out.bits.state.state_num := controller.io.irq_num

    //pipeline control
    io.in.ready := !io.in.valid || ((~io.is_stall) && (~io.is_fencei || io.ifu_signals.ready) && io.out.ready)
    io.out.valid := io.in.valid && (~io.is_stall) && (~io.is_fencei || io.ifu_signals.ready)
}