package core

import chisel3._
import chisel3.util._
import common.PC_SEL._
import common.REG_WRITE_SEL._
import common.JUMP_TYPE._
import common.ALU_OP._
import common.ALU_SRCA._
import common.ALU_SRCB._
import sim._
import bus._
import unit._
import core.PerfEvents._

class SimCommitProbe extends Bundle {
  val valid = Bool()
  val pc = UInt(32.W)
  val aluResult = UInt(32.W)
  val regWriteSel = UInt(3.W)
}

class Core_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val ebreak    = if (!conf.useDPIC) Some(Output(Bool())) else None
  val simCommit = if (conf.npc) Some(Output(new SimCommitProbe)) else None
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
  val dcache = Module(new DCache(set = 64, way = 2, blockSize = 32))

  def StageConnect[T <: Data](prev: DecoupledIO[T], next: DecoupledIO[T], flush: Bool): Unit = {
    prev.ready := next.ready
    next.bits := RegEnable(prev.bits, 0.U.asTypeOf(prev.bits), prev.valid && next.ready)
    next.valid := RegEnable(Mux(flush, false.B, prev.valid), false.B, prev.ready || flush)
  }

  val reset_pc = conf.resetVector.U(32.W)

  ifu.io.pc.ready := ifu.io.in.ready
  ifu.io.in.bits := RegEnable(ifu.io.pc.bits, reset_pc.asTypeOf(new IFU_PC_IO), ifu.io.in.valid && ifu.io.pc.ready)
  ifu.io.in.valid := RegEnable(ifu.io.pc.valid, false.B, ifu.io.in.ready)

  //stage connection
  StageConnect(ifu.io.out, idu.io.in, idu.io.is_flush)
  val exuInputFlush = WireDefault(false.B)
  StageConnect(idu.io.out, exu.io.in, exuInputFlush)
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
    val load_use_hazard = lsu_hit && lsu_is_load && !lsu.io.loadResultValid
    load_use_hazard || exu_load_use_hazard
  }

  idu.io.is_stall := RsStall(idu.io.refile.raddr1, idu.io.rs1_ren) || RsStall(idu.io.refile.raddr2, idu.io.rs2_ren)

  val forwardedRs1 = RsForward(idu.io.refile.raddr1, idu.io.rs1_ren,
    idu.io.out.bits.rd1)
  val forwardedRs2 = RsForward(idu.io.refile.raddr2, idu.io.rs2_ren,
    idu.io.out.bits.rd2)
  val iduToExuFire = idu.io.out.valid && exu.io.in.ready

  // Launch the DCache lookup as the load crosses IDU->EXU. The registered
  // cache response is then ready when that same instruction reaches LSU,
  // preserving the one-cycle hit contract without a tag-to-global-control
  // combinational path. A rejected preissue falls back to LSU's AXI request.
  val iduOutIsLoad = idu.io.out.bits.signals.lsu.mem_valid &&
    !idu.io.out.bits.signals.lsu.mem_write
  // The preissue port bypasses LSU, so it must not cross an older store that is
  // still in EXU. The regular LSU path preserves ordering when this gate blocks.
  val olderExuStore = exu.io.in.valid &&
    exu.io.in.bits.signals.lsu.mem_valid && exu.io.in.bits.signals.lsu.mem_write
  val earlyBaseExuHit = exu_wen && idu.io.rs1_ren && idu.io.refile.raddr1 =/= 0.U &&
    idu.io.refile.raddr1 === exu.io.in.bits.waddr
  val earlyBaseLsuHit = lsu_wen && idu.io.rs1_ren && idu.io.refile.raddr1 =/= 0.U &&
    idu.io.refile.raddr1 === lsu.io.in.bits.waddr && !earlyBaseExuHit
  val earlyBaseWbuHit = wbu_wen && idu.io.rs1_ren && idu.io.refile.raddr1 =/= 0.U &&
    idu.io.refile.raddr1 === wbu.io.in.bits.waddr &&
    !earlyBaseExuHit && !earlyBaseLsuHit
  val earlyExuAdd = earlyBaseExuHit && !exu_is_load &&
    exu.io.in.bits.signals.wbu.reg_write_sel === ALU_SEL &&
    exu.io.in.bits.signals.exu.alu_control === ALU_ADD &&
    idu.io.out.bits.imm_ext === 0.U
  val earlyExuSrcA = Mux(exu.io.in.bits.signals.exu.alu_srcA === ALU_A_PC,
    exu.io.in.bits.pc, exu.io.in.bits.rd1)
  val earlyExuSrcB = Mux(exu.io.in.bits.signals.exu.alu_srcB === ALU_B_IMM,
    exu.io.in.bits.imm_ext, exu.io.in.bits.rd2)
  val earlyExuAddress = earlyExuSrcA + earlyExuSrcB
  val earlyLoadBase = Mux(earlyBaseWbuHit, wbu.io.refile.wdata,
    idu.io.out.bits.rd1)
  val earlyLoadAddress = Mux(earlyExuAdd, earlyExuAddress,
    earlyLoadBase + idu.io.out.bits.imm_ext)
  dcache.io.earlyLoad.valid := iduToExuFire && iduOutIsLoad &&
    !olderExuStore && !earlyBaseLsuHit &&
    (!earlyBaseExuHit || earlyExuAdd) && !exuInputFlush && !idu.io.is_flush
  // The EXU fast path is one dedicated ADD and no second address carry chain.
  // Other EXU/LSU dependencies use the ordered LSU path; WB forwarding remains.
  dcache.io.earlyLoad.bits.addr := earlyLoadAddress
  dcache.io.earlyLoad.bits.id := 0.U
  dcache.io.earlyLoad.bits.size := MuxLookup(idu.io.out.bits.signals.lsu.mem_rd, 2.U)(Seq(
    common.MEM_READ.RBYTE -> 0.U,
    common.MEM_READ.RHALF -> 1.U,
    common.MEM_READ.RWORD -> 2.U,
    common.MEM_READ.RBYTEU -> 0.U,
    common.MEM_READ.RHALFU -> 1.U
  ))
  dcache.io.earlyLoadCancel := idu.io.is_flush
  val exuLoadPreissued = RegEnable(dcache.io.earlyLoad.fire, false.B, iduToExuFire)
  val exuToLsuFire = exu.io.out.valid && lsu.io.in.ready
  val loadPreissued = RegEnable(exuLoadPreissued, false.B, exuToLsuFire)
  lsu.io.loadPreissued := loadPreissued
  lsu.io.earlyLoadResp <> dcache.io.earlyLoadResp

  // Keep the operand registers as the physical EXU stage boundary. Without
  // explicit preservation Vivado may duplicate/absorb these registers into a
  // DSP or recovery consumer and recreate a DCache-to-consumer timing path.
  val exuOperandRs1 = RegEnable(forwardedRs1, 0.U(32.W), iduToExuFire)
  val exuOperandRs2 = RegEnable(forwardedRs2, 0.U(32.W), iduToExuFire)
  dontTouch(exuOperandRs1)
  dontTouch(exuOperandRs2)
  exu.io.in.bits.rd1 := exuOperandRs1
  exu.io.in.bits.rd2 := exuOperandRs2

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
  val jump = exu.io.in.bits.signals.exu.jump
  val is_jump = jump =/= JUMP_NONE && exu.io.pc.valid
  val is_ch = is_jump && pc_src =/= PC_PLUS4
  val resolved_target = MuxCase(exu.io.pc.bits.pc4_imm, Seq(
    (jump === JUMP_JALR) -> exu.io.pc.bits.pc4_rs2,
    (jump === JUMP_MERT) -> csr.io.read.mepc
  ))
  val correct_pc = Mux(is_ch, resolved_target, exu.io.pc.bits.pc4)

  //BPU
  val predict_taken = exu.io.in.bits.bp_taken
  val predict_target = exu.io.in.bits.bp_target
  // Direction and target checks are independent. Comparing against a target
  // selected by pc_src would serialize branch resolution, a 32-bit target mux,
  // and a second 32-bit comparison on the recovery path.
  val target_mispredict = is_ch && predict_taken && (predict_target =/= resolved_target)

  val mis_predict = is_jump && ((is_ch =/= predict_taken) || target_mispredict)
  val is_bp_flush = mis_predict

  // fencei 完成下一拍：冲刷 IFU+IDU（丢弃失效前预取的后继），fencei 自身已进 EXU 继续提交
  val is_fencei = RegNext(icache.io.fencei.valid & icache.io.fencei.ready, false.B)
  val fencei_pc = RegEnable(idu.io.in.bits.pc, 0.U(32.W), idu.io.is_fencei)
  val fencei_redirect_pc = fencei_pc + 4.U

  // 重定向：IRQ > mispred > fencei
  ifu.io.correct_pc := Mux(is_irq, csr.io.read.mtvec,
    Mux(mis_predict, correct_pc,
      Mux(is_fencei, fencei_redirect_pc, 0.U)))

  // A current-cycle branch recovery invalidates the younger input slot, but
  // must not kill the branch that is resolving in EXU. EXU's internal flush
  // remains reserved for interrupts and long-operation cancellation.
  exuInputFlush := is_irq || is_bp_flush
  exu.io.pc.ready := true.B

  ifu.io.is_flush := is_irq || is_fencei || is_bp_flush
  idu.io.is_flush := is_irq || is_fencei || is_bp_flush
  exu.io.is_flush := is_irq
  lsu.io.is_flush := is_irq
  wbu.io.is_flush := is_irq

  val is_branch_update = is_jump && (jump === JUMP_BEQ || jump === JUMP_BNE || jump === JUMP_BLT || jump === JUMP_BGE || jump === JUMP_BLTU || jump === JUMP_BGEU)

  val exu_inst = exu.io.in.bits.inst
  val rd_is_ra  = exu_inst(11, 7) === 1.U
  val rs1_is_ra = exu_inst(19, 15) === 1.U
  // call：jal ra / jalr ra, rs1(≠ra)；ret：jalr x*, 0(ra) 优先于 call
  val is_call_update = is_jump && rd_is_ra && (
    (jump === JUMP_JAL) || ((jump === JUMP_JALR) && !rs1_is_ra)
  )
  val is_ret_update = is_jump && (jump === JUMP_JALR) && rs1_is_ra

  // Predictor training is delayed one cycle so downstream ready cannot feed
  // back into the current IFU prediction through the BHT/BTB collision bypass.
  // Recovery and flush still use the current EXU result above.
  val bpu_update_valid = RegNext(is_jump && jump =/= JUMP_MERT, false.B)
  val bpu_update_taken = RegNext(is_ch, false.B)
  val bpu_update_pc = RegNext(exu.io.in.bits.pc, 0.U)
  val bpu_update_target = RegNext(resolved_target, 0.U)
  val bpu_update_is_branch = RegNext(is_branch_update, false.B)
  val bpu_update_meta = RegNext(exu.io.in.bits.bp_meta, 0.U)
  val bpu_update_is_call = RegNext(is_call_update, false.B)
  val bpu_update_is_ret = RegNext(is_ret_update, false.B)

  // MRET depends on mutable CSR state and is intentionally not cached in BTB.
  ifu.io.bpu_update_valid := bpu_update_valid
  ifu.io.bpu_update_taken := bpu_update_taken
  ifu.io.bpu_update_pc := bpu_update_pc
  ifu.io.bpu_update_target := bpu_update_target
  ifu.io.bpu_update_is_branch := bpu_update_is_branch
  // Direction state captured at prediction time avoids a second table read.
  ifu.io.bpu_update_meta := bpu_update_meta
  ifu.io.bpu_update_is_call := bpu_update_is_call
  ifu.io.bpu_update_is_ret := bpu_update_is_ret

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
  lsu.io.dmem <> dcache.io.cpu
  dcache.io.mem <> io.dmem
  wbu.io.refile <> refile.io.write
  wbu.io.csr <> csr.io.write

  if (!conf.useDPIC) {
    io.ebreak.get := wbu.io.ebreak.get
  }

  if (conf.npc) {
    io.simCommit.get.valid := wbu.io.in.valid
    io.simCommit.get.pc := wbu.io.in.bits.pc
    io.simCommit.get.aluResult := wbu.io.in.bits.alu_result
    io.simCommit.get.regWriteSel := wbu.io.in.bits.signals.wbu.reg_write_sel
  }
  
}
