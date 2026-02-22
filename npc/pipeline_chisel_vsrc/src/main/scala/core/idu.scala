package core

import chisel3._
import chisel3.util._
import core.PM
import core.PerfEvents._

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
    override def desiredName = "ysyx_25020039_IDU"

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

  if(conf.statistics){
    // Performance Counters
    val sigs = controller.io.signals
    val fire = io.out.valid && io.out.ready 
    
    val is_load = sigs.lsu.mem_valid && !sigs.lsu.mem_write
    val is_store = sigs.lsu.mem_valid && sigs.lsu.mem_write
    val is_csr = sigs.wbu.csr_write
    val jump = sigs.exu.jump
    
    val is_branch = (jump === JUMP.JUMP_BEQ || jump === JUMP.JUMP_BNE || jump === JUMP.JUMP_BLT || jump === JUMP.JUMP_BGE || jump === JUMP.JUMP_BLTU || jump === JUMP.JUMP_BGEU)
    val is_jump = (jump === JUMP.JUMP_JAL || jump === JUMP.JUMP_JALR)
    
    val is_other = (jump === JUMP.JUMP_MERT) || (io.is_fencei) || (io.out.bits.is_ebreak)

    val is_compute = !is_load && !is_store && !is_csr && !is_branch && !is_jump && !is_other
    
      PM(conf, clock, EVENT_INST_TYPE_LOAD, 1.U, fire && is_load)
      PM(conf, clock, EVENT_INST_TYPE_STORE, 1.U, fire && is_store)
      PM(conf, clock, EVENT_INST_TYPE_CSR, 1.U, fire && is_csr)
      PM(conf, clock, EVENT_INST_TYPE_BRANCH, 1.U, fire && is_branch)
      PM(conf, clock, EVENT_INST_TYPE_JUMP, 1.U, fire && is_jump) 
      PM(conf, clock, EVENT_INST_TYPE_OTHER, 1.U, fire && is_other)
      PM(conf, clock, EVENT_INST_TYPE_COMPUTE, 1.U, fire && is_compute)
    }
}