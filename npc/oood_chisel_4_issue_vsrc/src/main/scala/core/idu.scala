package core

import chisel3._
import chisel3.util._
import unit._
import common.OoOParams
import common.BPU_Config._
import common.JUMP_TYPE._
import core.PerfEvents._

class IDU_EXU_IO extends Bundle{
  val signals = new Bundle{
    val exu = new EXU_signals
    val lsu = new LSU_signals
    val wbu = new WBU_signals
  }
  val rd1 = Output(UInt(32.W))
  val rd2 = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val imm_ext = Output(UInt(32.W))
  val waddr = Output(UInt(5.W))
  val is_ebreak = Output(Bool())

  val csr_rd1 = Output(UInt(32.W))
  val csr_waddr = Output(UInt(12.W))

  val state = Output(new State)

  val bp_valid = Output(Bool())
  val bp_taken = Output(Bool())
  val bp_target = Output(UInt(32.W))
  val bp_index = Output(UInt(BP_META_WIDTH.W))
  val ftq_idx = Output(UInt(OoOParams.FTQ_PTR_W.W))
  val ftq_generation = Output(UInt(OoOParams.FTQ_GEN_W.W))

  val inst = Output(UInt(32.W))
  val rob_idx = Output(UInt(OoOParams.ROB_PTR_W.W))
  val cp_idx  = Output(UInt(log2Ceil(OoOParams.CP_DEPTH).W))
  val pdest   = Output(UInt(OoOParams.PHYS_W.W))
  val old_phys = Output(UInt(OoOParams.PHYS_W.W))
  val do_rename = Output(Bool())
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

    val control = Module(new Control)
    val imm = Module(new IMM)

    control.io.inst := io.in.bits.inst
    imm.io.inst := io.in.bits.inst
    imm.io.imm_type := control.io.signals.idu.imm_type

    //refile
    io.refile.raddr1 := io.in.bits.inst(19,15)
    io.refile.raddr2 := io.in.bits.inst(24,20)
    io.rs1_ren := control.io.signals.idu.rs1_ren
    io.rs2_ren := control.io.signals.idu.rs2_ren

    //exu
    io.out.bits.signals := control.io.signals
    io.out.bits.rd1 := io.refile.rdata1
    io.out.bits.rd2 := io.refile.rdata2
    io.out.bits.pc := io.in.bits.pc
    io.out.bits.imm_ext := imm.io.imm_ext
    io.out.bits.waddr := io.in.bits.inst(11,7)
    io.out.bits.is_ebreak := (io.in.bits.inst === "h00100073".U(32.W))

    io.out.bits.csr_rd1 := io.csr.rdata
    io.out.bits.csr_waddr := io.in.bits.inst(31,20)
    io.csr.raddr := io.in.bits.inst(31,20)

    //ifu signals
    io.is_fencei := control.io.signals.ifu.bits.is_fencei && io.in.valid
    io.ifu_signals.valid := io.in.valid && control.io.signals.ifu.valid
    io.ifu_signals.bits := control.io.signals.ifu.bits
    control.io.signals.ifu.ready := io.ifu_signals.ready

    //state
    io.out.bits.state := io.state
    io.out.bits.state.state := control.io.irq || io.in.bits.state.state
    io.out.bits.state.state_num := Mux(control.io.irq, control.io.irq_num, io.in.bits.state.state_num)

    //pipeline control（4f：fencei 像普通指令进 ROB，不再 hold 等 icache）
    val send = !io.is_stall

    io.in.ready := !io.in.valid || (send && io.out.ready)
    io.out.valid := io.in.valid && send

    //bpu
    io.out.bits.bp_valid := io.in.bits.bp_valid
    io.out.bits.bp_taken := io.in.bits.bp_taken
    io.out.bits.bp_target := io.in.bits.bp_target
    io.out.bits.bp_index := io.in.bits.bp_index
    io.out.bits.ftq_idx := io.in.bits.ftq_idx
    io.out.bits.ftq_generation := io.in.bits.ftq_generation

    io.out.bits.inst := io.in.bits.inst
    io.out.bits.rob_idx := 0.U
    io.out.bits.cp_idx := 0.U
    io.out.bits.pdest := 0.U
    io.out.bits.old_phys := 0.U
    io.out.bits.do_rename := false.B

  if(conf.statistics){
    // Performance Counters
    val sigs = control.io.signals
    val fire = io.out.valid && io.out.ready 
      
    val is_load = sigs.lsu.mem_valid && !sigs.lsu.mem_write
    val is_store = sigs.lsu.mem_valid && sigs.lsu.mem_write
    val is_csr = sigs.wbu.csr_write
    val jump = sigs.exu.jump
    
    val is_branch = (jump === JUMP_BEQ || jump === JUMP_BNE || jump === JUMP_BLT || jump === JUMP_BGE || jump === JUMP_BLTU || jump === JUMP_BGEU)
    val is_jump = (jump === JUMP_JAL || jump === JUMP_JALR)
      
    val is_other = (jump === JUMP_MERT) || (io.is_fencei) || (io.out.bits.is_ebreak)

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
