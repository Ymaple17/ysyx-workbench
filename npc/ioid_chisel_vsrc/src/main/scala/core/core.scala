package core

import chisel3._
import chisel3.util._
import common.PC_SEL._
import common.REG_WRITE_SEL._
import common.JUMP_TYPE._
import sim._
import bus._
import unit._
import core.PerfEvents._

class Core_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val ebreak    = if (!conf.useDPIC) Some(Output(Bool())) else None
  val imem      = new AXI4Master
  val dmem      = new AXI4Master
}

class State extends Bundle{
  val state = Bool()
  val state_num = UInt(8.W)
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
  val icache = Module(new ICache(set = 64, way = 4, block_size = 32, conf = conf))

  def StageConnect[T <: Data](prev: DecoupledIO[T], next: DecoupledIO[T], flush: Bool): Unit = {
    prev.ready := next.ready
    next.bits := RegEnable(prev.bits,0.U.asTypeOf(prev.bits), prev.valid && next.ready)
    next.valid := RegEnable(prev.valid, false.B, prev.ready) && !flush
  }

  val reset_pc = if(conf.ysyxsoc){ "h3000_0000".U(32.W) }else if(conf.npc){ "h8000_0000".U(32.W)} else { "h0000_0000".U(32.W) }

  ifu.io.pc.ready := ifu.io.in.ready
  ifu.io.in.bits := RegEnable(ifu.io.pc.bits, reset_pc.asTypeOf(new IFU_PC_IO), ifu.io.in.valid && ifu.io.pc.ready)
  ifu.io.in.valid := RegEnable(ifu.io.pc.valid, false.B, ifu.io.in.ready)

  //stage connection
  StageConnect(ifu.io.out, idu.io.in, idu.io.is_flush)
  StageConnect(idu.io.out, exu.io.in, exu.io.is_flush)
  StageConnect(exu.io.out, lsu.io.in, lsu.io.is_flush)
  StageConnect(lsu.io.out, wbu.io.in, wbu.io.is_flush)

  //data raw :前递 + load-use hazard
  def DataForward(signal: WBU_signals,alu_result: UInt, imm: UInt, pc4: UInt,csr: UInt, mem_read: UInt): UInt = {
    MuxLookup(signal.reg_write_sel, 0.U)(Seq(
      ALU_SEL -> alu_result,
      IMM_SEL -> imm,
      PC4_SEL -> pc4,
      CSR_DATA -> csr,
      MEM_SEL -> mem_read
    ))
  }

  val exu_wen = exu.io.in.bits.signals.wbu.reg_write && exu.io.in.valid
  val lsu_wen = lsu.io.in.bits.signals.wbu.reg_write && lsu.io.in.valid
  val wbu_wen = wbu.io.in.bits.signals.wbu.reg_write && wbu.io.in.valid


  val exu_is_load = exu.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val lsu_is_load = lsu.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL

  val exu_wdata = DataForward(exu.io.in.bits.signals.wbu, exu.io.out.bits.alu_result, exu.io.out.bits.imm_ext, exu.io.out.bits.pc + 4.U, exu.io.out.bits.csr_rd1, 0.U)
  val lsu_wdata = DataForward(lsu.io.in.bits.signals.wbu, lsu.io.out.bits.alu_result, lsu.io.out.bits.imm_ext, lsu.io.out.bits.pc + 4.U, lsu.io.out.bits.csr_rd1, lsu.io.out.bits.mem_read)

  def RsForward(rs: UInt,ren: Bool, fallback: UInt): UInt = {
    val exu_hit = exu_wen && ren && rs =/= 0.U && rs === exu.io.in.bits.waddr && !exu_is_load
    val lsu_hit = lsu_wen && ren && rs =/= 0.U && rs === lsu.io.in.bits.waddr && !exu_hit
    val wbu_hit = wbu_wen && ren && rs =/= 0.U && rs === wbu.io.in.bits.waddr && !exu_hit && !lsu_hit

    MuxCase(fallback, Seq(
      exu_hit -> exu_wdata,
      lsu_hit -> lsu_wdata,
      wbu_hit -> wbu.io.refile.wdata
    ))
  }

  def RsStall(rs: UInt, ren: Bool): Bool = {
    val exu_hit = exu_wen && ren && rs =/= 0.U && rs === exu.io.in.bits.waddr
    val lsu_hit = lsu_wen && ren && rs =/= 0.U && rs === lsu.io.in.bits.waddr && !exu_hit
    val wbu_hit = wbu_wen && ren && rs =/= 0.U && rs === wbu.io.in.bits.waddr && !exu_hit && !lsu_hit

    val exu_load_use_hazard = exu_hit && exu_is_load
    val load_use_hazard = lsu_hit && lsu_is_load && !lsu.io.dmem.rvalid
    load_use_hazard || exu_load_use_hazard
  }

  idu.io.is_stall := RsStall(idu.io.refile.raddr1, idu.io.rs1_ren) || RsStall(idu.io.refile.raddr2, idu.io.rs2_ren)

  exu.io.in.bits.rd1 := RegEnable(RsForward(idu.io.refile.raddr1, idu.io.rs1_ren, idu.io.out.bits.rd1), 0.U(32.W), idu.io.out.valid && exu.io.in.ready)
  exu.io.in.bits.rd2 := RegEnable(RsForward(idu.io.refile.raddr2, idu.io.rs2_ren, idu.io.out.bits.rd2), 0.U(32.W), idu.io.out.valid && exu.io.in.ready)

  //异常
  val is_irq = RegNext(wbu.io.state_write_en && wbu.io.state_write.state, false.B)
  csr.io.irq := is_irq
  csr.io.irq_no := RegNext(wbu.io.state_write.state_num, 0.U)
  csr.io.irq_pc := RegNext(wbu.io.in.bits.pc, 0.U)

  val state_reg = RegEnable(wbu.io.state_write, 0.U.asTypeOf(new State), wbu.io.state_write_en)
  ifu.io.state := state_reg
  idu.io.state := state_reg

  //分支/冲刷
  val pc_src = exu.io.pc.bits.pc_src
  val correct_pc = MuxLookup(pc_src, exu.io.pc.bits.pc4)(Seq(
      PC_PLUS4 -> exu.io.pc.bits.pc4,
      PC_IMM  -> exu.io.pc.bits.pc4_imm,
      PC_RS2  -> exu.io.pc.bits.pc4_rs2,
      MEPC -> csr.io.read.mepc
  ))

  val is_jump = exu.io.in.bits.signals.exu.jump =/= JUMP_NONE && exu.io.pc.valid
  val is_ch = is_jump && pc_src =/= PC_PLUS4

  //BPU
  val predict_taken = exu.io.in.bits.bp_taken
  val predict_target = exu.io.in.bits.bp_target
  val target_mispredict = is_ch && predict_taken && (predict_target =/= correct_pc)

  val mis_predict = is_jump && ((is_ch =/= predict_taken) || target_mispredict)
  val mis_predict_r = RegNext(mis_predict, false.B)
  val is_bp_flush = mis_predict_r

  val is_fencei = RegNext(icache.io.fencei.valid & icache.io.fencei.ready, false.B)
  // 重定向门控用 mis_predict_r：预测错时回正确目标
  ifu.io.correct_pc := Mux(is_irq, csr.io.read.mtvec, Mux(mis_predict_r, RegNext(correct_pc, 0.U), Mux(is_fencei, ifu.io.in.bits.next_pc, 0.U)))
  
  exu.io.pc.ready := true.B

  
  ifu.io.is_flush := is_irq || is_fencei || is_bp_flush
  idu.io.is_flush := is_irq || is_bp_flush
  exu.io.is_flush := is_irq || is_bp_flush
  lsu.io.is_flush := is_irq
  wbu.io.is_flush := is_irq

  val jump = exu.io.in.bits.signals.exu.jump
  val is_branch_update = is_jump && (jump === JUMP_BEQ || jump === JUMP_BNE || jump === JUMP_BLT || jump === JUMP_BGE || jump === JUMP_BLTU || jump === JUMP_BGEU)

  val exu_inst = exu.io.in.bits.inst
  val is_call_update = is_jump && (jump === JUMP_JALR) && (exu_inst(11,7) === 1.U) && (exu_inst(19,15) =/= 1.U)
  val is_ret_update = is_jump && (jump === JUMP_JALR) && (exu_inst(19,15) === 1.U)

  ifu.io.bpu_update_valid := is_jump
  ifu.io.bpu_update_taken := is_ch
  ifu.io.bpu_update_pc := exu.io.in.bits.pc
  ifu.io.bpu_update_is_branch := is_branch_update
  // GShare：更新用预测时的索引（随指令流传来），保证与预测严格一致
  ifu.io.bpu_update_index := exu.io.in.bits.bp_index
  ifu.io.bpu_update_is_call := is_call_update
  ifu.io.bpu_update_is_ret := is_ret_update

  if (conf.statistics) {
    // BPU 命中率统计：只统计真正做过预测的跳转/分支指令（bp_valid）
    // 命中率 = (PREDICT - MISPRED) / PREDICT
    PM(conf, clock, EVENT_BPU_PREDICT, 1.U, is_jump && exu.io.in.bits.bp_valid)
    PM(conf, clock, EVENT_BPU_MISPRED, 1.U, mis_predict && exu.io.in.bits.bp_valid)
  }

  //connect
  ifu.io.imem <> icache.io.in
  icache.io.out <> io.imem
  icache.io.fencei <> idu.io.ifu_signals
  idu.io.refile <> refile.io.read
  idu.io.csr <> csr.io.read
  lsu.io.dmem <> io.dmem
  wbu.io.refile <> refile.io.write
  wbu.io.csr <> csr.io.write

  if (!conf.useDPIC) {
    io.ebreak.get := idu.io.out.bits.is_ebreak && idu.io.in.valid
  }
  
}
