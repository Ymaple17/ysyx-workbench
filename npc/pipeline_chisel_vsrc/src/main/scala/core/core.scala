package core

import chisel3._
import chisel3.util._
import bus._
import Instructions._
import MEM_READ._
import MEM_WMASK._
import MEM_WRITE_CTRL._
import MEM_VALID_CTRL._
import CSR_WRITE_CTRL._
import REG_WRITE_CTRL._
import REG1_READ_CTRL._
import REG2_READ_CTRL._
import REG_WRITE_SEL._
import CSR_SEL._
import ImmType._
import ALU_SRCA._
import ALU_SRCB._
import JUMP._
import PC_SEL._
import IRQ_CTRL._
import FENCEI_CTRL._

class Core_IO(val conf :CoreConfig) extends Bundle{
  val imem = new AXI4Master
  val dmem = new AXI4Master
  val ebreak = if(!conf.useDPIC) Some(Output(Bool())) else None
}

class State extends Bundle{
  val state = Bool()
  val state_num = UInt(8.W)
}

class Core(val conf :CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_Core"

    val io = IO(new Core_IO(conf))
    val ifu = Module(new IFU(conf))
    val idu = Module(new IDU(conf))
    val exu = Module(new EXU(conf))
    val lsu = Module(new LSU(conf))
    val wbu = Module(new WBU(conf))

    if (!conf.useDPIC) {
      io.ebreak.get := wbu.io.ebreak.get
    }

    val icache = Module(new ICache(1,1,4,conf))
    val refile = Module(new Refile(conf))
    val csr = Module(new CSR(conf))

    val state = RegEnable(wbu.io.state_write, 0.U.asTypeOf(new State), wbu.io.state_write_en)
    ifu.io.state := state
    idu.io.state := state
    exu.io.state := state
    lsu.io.state := state
    wbu.io.state_read := state

    val is_irq = RegNext(wbu.io.state_write_en && (wbu.io.state_write.state), false.B)
    csr.io.irq    := is_irq
    csr.io.irq_no := RegNext(wbu.io.state_write.state_num, 0.U)
    csr.io.irq_pc := RegNext(wbu.io.in.bits.pc, 0.U)

    // pipeline connect
    def PipelineConnect[T <: Data, T2 <: Data](prevOut: DecoupledIO[T], thisIn: DecoupledIO[T], is_flush: Bool) = {
      prevOut.ready := thisIn.ready
      thisIn.bits   := RegEnable(prevOut.bits, 0.U.asTypeOf(prevOut.bits), prevOut.valid && thisIn.ready)
      thisIn.valid  := RegEnable(prevOut.valid, false.B, thisIn.ready) && !is_flush
    }

    def IFU_Connect[T <: Data, T2 <: Data](prevOut: DecoupledIO[T], thisIn: DecoupledIO[T]) = {
      prevOut.ready := thisIn.ready
      val resetPC = if(conf.ysyxsoc){ "h3000_0000".U(32.W) }
                    else { "h8000_0000".U(32.W) }
      thisIn.bits  := RegEnable(prevOut.bits, resetPC.asTypeOf(new IFUPC_IO), prevOut.valid && thisIn.ready)
      thisIn.valid := RegEnable(prevOut.valid, false.B, thisIn.ready)
    }

    IFU_Connect(ifu.io.pc, ifu.io.in)
    PipelineConnect(ifu.io.out,  idu.io.in,  idu.io.is_flush)
    PipelineConnect(idu.io.out,  exu.io.in,  exu.io.is_flush)
    PipelineConnect(exu.io.out,  lsu.io.in,  lsu.io.is_flush)
    PipelineConnect(lsu.io.out,  wbu.io.in,  wbu.io.is_flush)

    // data conflict helpers
    def dataConflict(rs: UInt, rd: UInt) = (rs === rd)

    def dataConflictWithStage(stage: IDU_IO, rd: UInt, is_write: Bool, stage_work: Bool) = {
      val rs1          = stage.refile.raddr1
      val rs2          = stage.refile.raddr2
      val stage_working = stage.in.valid & stage_work
      val rs1_ren      = stage.rs1_ren
      val rs2_ren      = stage.rs2_ren
      ((rs1_ren & (rs1 =/= 0.U) & dataConflict(rs1, rd)) ||
       (rs2_ren & (rs2 =/= 0.U) & dataConflict(rs2, rd))) && is_write && stage_working
    }

    // RAW detection per stage
    val exu_raw = Wire(Bool())
    val exu_raw_rs1 = Wire(Bool())
    val exu_raw_rs2 = Wire(Bool())
    exu_raw     := dataConflictWithStage(idu.io, exu.io.in.bits.waddr, exu.io.in.bits.signals.wbu.reg_write, exu.io.in.valid)
    exu_raw_rs1 := exu_raw && dataConflict(exu.io.in.bits.waddr, idu.io.refile.raddr1)
    exu_raw_rs2 := exu_raw && dataConflict(exu.io.in.bits.waddr, idu.io.refile.raddr2)

    val lsu_raw = Wire(Bool())
    val lsu_raw_rs1 = Wire(Bool())
    val lsu_raw_rs2 = Wire(Bool())
    lsu_raw     := dataConflictWithStage(idu.io, lsu.io.in.bits.waddr, lsu.io.in.bits.signals.wbu.reg_write, lsu.io.in.valid)
    lsu_raw_rs1 := lsu_raw && dataConflict(lsu.io.in.bits.waddr, idu.io.refile.raddr1)
    lsu_raw_rs2 := lsu_raw && dataConflict(lsu.io.in.bits.waddr, idu.io.refile.raddr2)

    val wbu_raw = Wire(Bool())
    val wbu_raw_rs1 = Wire(Bool())
    val wbu_raw_rs2 = Wire(Bool())
    wbu_raw     := dataConflictWithStage(idu.io, wbu.io.in.bits.waddr, wbu.io.in.bits.signals.wbu.reg_write, wbu.io.in.valid)
    wbu_raw_rs1 := wbu_raw && dataConflict(wbu.io.in.bits.waddr, idu.io.refile.raddr1)
    wbu_raw_rs2 := wbu_raw && dataConflict(wbu.io.in.bits.waddr, idu.io.refile.raddr2)

    val is_raw_rs1 = exu_raw_rs1 || lsu_raw_rs1 || wbu_raw_rs1
    val is_raw_rs2 = exu_raw_rs2 || lsu_raw_rs2 || wbu_raw_rs2

    val exu_forward_rd1 = Wire(Bool())
    val exu_forward_rd2 = Wire(Bool())
    val lsu_forward_rd1 = Wire(Bool())
    val lsu_forward_rd2 = Wire(Bool())
    val wbu_forward_rd1 = Wire(Bool())
    val wbu_forward_rd2 = Wire(Bool())

    exu_forward_rd1 := exu_raw_rs1 && (exu.io.in.bits.signals.wbu.reg_write_sel =/= MEM_SEL)
    exu_forward_rd2 := exu_raw_rs2 && (exu.io.in.bits.signals.wbu.reg_write_sel =/= MEM_SEL)

    lsu_forward_rd1 := lsu_raw_rs1 && (lsu.io.in.bits.signals.wbu.reg_write_sel =/= MEM_SEL || lsu.io.dmem.rvalid)
    lsu_forward_rd2 := lsu_raw_rs2 && (lsu.io.in.bits.signals.wbu.reg_write_sel =/= MEM_SEL || lsu.io.dmem.rvalid)

    wbu_forward_rd1 := wbu_raw_rs1
    wbu_forward_rd2 := wbu_raw_rs2

    // stall
    val rs1_stall = is_raw_rs1 && !exu_forward_rd1 && !lsu_forward_rd1 && !wbu_forward_rd1
    val rs2_stall = is_raw_rs2 && !exu_forward_rd2 && !lsu_forward_rd2 && !wbu_forward_rd2
    idu.io.is_stall := rs1_stall || rs2_stall

    // forward data per stage
    val exu_forward_data = Wire(UInt(32.W))
    val lsu_forward_data = Wire(UInt(32.W))
    val wbu_forward_data = Wire(UInt(32.W))

    exu_forward_data := MuxLookup(exu.io.in.bits.signals.wbu.reg_write_sel, 0.U)(Seq(
      ALU_SEL  -> exu.io.out.bits.alu_result,
      IMM_SEL  -> exu.io.out.bits.imm_ext,
      PC4_SEL  -> (exu.io.out.bits.pc + 4.U),
      CSR_DATA -> exu.io.out.bits.csr_rd1
    ))

    lsu_forward_data := MuxLookup(lsu.io.in.bits.signals.wbu.reg_write_sel, 0.U)(Seq(
      ALU_SEL  -> lsu.io.out.bits.alu_result,
      IMM_SEL  -> lsu.io.out.bits.imm_ext,
      PC4_SEL  -> (lsu.io.out.bits.pc + 4.U),
      CSR_DATA -> lsu.io.out.bits.csr_rd1,
      MEM_SEL  -> lsu.io.out.bits.mem_read
    ))

    wbu_forward_data := wbu.io.refile.wdata

    // select forward data with priority: exu > lsu > wbu
    val rd1_forward_data = Wire(UInt(32.W))
    val rd2_forward_data = Wire(UInt(32.W))

    rd1_forward_data := MuxCase(idu.io.out.bits.rd1, Seq(
      exu_forward_rd1 -> exu_forward_data,
      lsu_forward_rd1 -> lsu_forward_data,
      wbu_forward_rd1 -> wbu_forward_data
    ))

    rd2_forward_data := MuxCase(idu.io.out.bits.rd2, Seq(
      exu_forward_rd2 -> exu_forward_data,
      lsu_forward_rd2 -> lsu_forward_data,
      wbu_forward_rd2 -> wbu_forward_data
    ))

    exu.io.in.bits.rd1 := RegEnable(rd1_forward_data, 0.U(32.W), idu.io.out.valid && exu.io.in.ready)
    exu.io.in.bits.rd2 := RegEnable(rd2_forward_data, 0.U(32.W), idu.io.out.valid && exu.io.in.ready)

    // hazard / branch
    val correct_pc = Wire(UInt(32.W))
    val pc_src = exu.io.pc.bits.pc_src
    correct_pc := MuxLookup(pc_src, exu.io.pc.bits.pc4)(Seq(
      PC_IMM -> exu.io.pc.bits.pc4_imm,
      PC_RS2 -> exu.io.pc.bits.pc4_rs2,
      MEPC   -> csr.io.read.mepc
    ))

    val is_jump = exu.io.in.bits.signals.exu.jump =/= JUMP_NONE & exu.io.out.valid
    exu.io.pc.ready := true.B
    val is_ch = Wire(Bool())
    is_ch := is_jump & pc_src =/= PC_PLUS4

    val is_fencei = RegNext(icache.io.fencei.valid & icache.io.fencei.ready, false.B)
    ifu.io.correct_pc := Mux(is_irq,    csr.io.read.mtvec,
                         Mux(is_ch,     correct_pc,
                         Mux(is_fencei, ifu.io.in.bits.next_pc, 0.U)))

    ifu.io.is_flush := is_ch || is_irq || is_fencei
    idu.io.is_flush := is_ch || is_irq
    exu.io.is_flush := is_irq
    lsu.io.is_flush := is_irq
    wbu.io.is_flush := is_irq

    ifu.io.imem      <> icache.io.in
    icache.io.out    <> io.imem
    icache.io.fencei <> idu.io.ifu_signals
    idu.io.refile    :<>= refile.io.read
    idu.io.csr       :<>= csr.io.read

    lsu.io.dmem   <> io.dmem
    wbu.io.refile :<>= refile.io.write
    wbu.io.csr    :<>= csr.io.write
}