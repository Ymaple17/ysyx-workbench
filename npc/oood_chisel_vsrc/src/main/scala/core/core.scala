package core

import chisel3._
import chisel3.util._
import common.PC_SEL._
import common.REG_WRITE_SEL._
import common.JUMP_TYPE._
import common.CSR_SEL._
import common.MEM_WMASK._
import common.MEM_READ._
import common.IRQ_CTRL.IRQ_MEXT
import common.OoOParams
import sim._
import bus._
import unit._
import core.PerfEvents._

class Core_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val ebreak    = if (!conf.useDPIC) Some(Output(Bool())) else None
  val imem      = new AXI4Master
  val dmem      = new AXI4Master
  val commit_valid    = Output(Bool())
  val commit_pc       = Output(UInt(32.W))
  val commit_mem_addr = Output(UInt(32.W))
  val commit_is_load  = Output(Bool())
  val commit_valid1    = Output(Bool())
  val commit_pc1       = Output(UInt(32.W))
  val commit_mem_addr1 = Output(UInt(32.W))
  val commit_is_load1  = Output(Bool())
  val arch_rdata      = Output(Vec(32, UInt(32.W)))
}

class State extends Bundle {
  val state     = Bool()
  val state_num = UInt(8.W)
}

/**
 * 阶段 3d + 4a–4c + 5：
 * - 真乱序单发；CSR/异常 @ commit
 * - store 架构写仅 commit；load 查更老 store 转发/等待；无 mem@head
 */
class Core(val conf: CoreConfig) extends Module {
  val io = IO(new Core_IO(conf))

  val ifu    = Module(new IFU(conf))
  val idu    = Module(new IDU(conf))
  val idu1   = Module(new IDU(conf))
  val exu    = Module(new EXU(conf))
  val exu_div = Module(new EXU(conf))
  val exu_lsu = Module(new EXU(conf))
  val lsu    = Module(new LSU(conf))
  val wbu    = Module(new WBU(conf))
  val wbu1   = Module(new WBU(conf))
  val csr    = Module(new CSR(conf))
  val icache = Module(new ICache(set = 64, way = 4, block_size = 32, conf = conf))
  val dcache = Module(new DCache(
    set = OoOParams.DCACHE_SET,
    blockSize = OoOParams.DCACHE_BLOCK_SIZE,
    conf = conf))
  val prf    = Module(new PRF(conf))
  val busy   = Module(new BusyTable())
  val rename = Module(new Rename2())
  val rob    = Module(new ROB(conf.statistics))
  val rs     = Module(new RS())
  val fq     = Module(new FetchQueue())
  val sq     = Module(new StoreQueue())
  val stbuf  = Module(new StoreBuffer())

  val alu_wb = Wire(new LSU_WBU_IO)
  val div_wb = Wire(new LSU_WBU_IO)
  val lsu_wb = Wire(new LSU_WBU_IO)
  val alu_wb_valid = Wire(Bool())
  val div_wb_valid = Wire(Bool())
  val lsu_wb_valid = Wire(Bool())
  val d_valid = Wire(Bool())
  val d_bits  = Wire(new Bundle {
    val pc      = UInt(32.W)
    val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  })
  dontTouch(d_valid)
  dontTouch(d_bits)

  def StageConnect[T <: Data](prev: DecoupledIO[T], next: DecoupledIO[T], flush: Bool): Unit = {
    val valid = RegInit(false.B)
    prev.ready := next.ready
    when(flush) {
      valid := false.B
    }.elsewhen(prev.ready) {
      valid := prev.valid
    }
    next.valid := valid && !flush
    next.bits  := RegEnable(prev.bits, 0.U.asTypeOf(prev.bits), prev.valid && next.ready && !flush)
  }

  def robAge(idx: UInt, head: UInt): UInt =
    (idx - head)(OoOParams.ROB_PTR_W - 1, 0)

  def exuToWbu(in: EXU_LSU_IO): LSU_WBU_IO = {
    val b = Wire(new LSU_WBU_IO)
    b.signals.wbu := in.signals.wbu
    b.rd1 := in.rd1
    b.alu_result := in.alu_result
    b.pc := in.pc
    b.next_pc := in.next_pc
    b.imm_ext := in.imm_ext
    b.mem_read := 0.U
    b.waddr := in.waddr
    b.is_ebreak := in.is_ebreak
    b.csr_rd1 := in.csr_rd1
    b.csr_waddr := in.csr_waddr
    b.state := in.state
    b.rob_idx := in.rob_idx
    b.pdest := in.pdest
    b.old_phys := in.old_phys
    b.do_rename := in.do_rename
    b.br_taken := in.br_taken
    b.store_data := in.rd2
    b.fwd_valid := false.B
    b.fwd_data := 0.U
    b
  }

  val reset_pc =
    if (conf.ysyxsoc) "h3000_0000".U(32.W)
    else if (conf.npc) "h8000_0000".U(32.W)
    else "h0000_0000".U(32.W)

  ifu.io.pc.ready := ifu.io.in.ready
  ifu.io.slot1_enable :=
    (if (OoOParams.WIDE_FETCH_ENABLE) fq.io.space >= OoOParams.WIDE_FETCH_MIN_SPACE.U else false.B)
  // flush 时必须装入 correct_pc（即使上一拍 in.valid=0），否则 irq/mispred 后取指饿死
  val ifu_in_en = ifu.io.is_flush || (ifu.io.in.valid && ifu.io.pc.ready)
  ifu.io.in.bits  := RegEnable(ifu.io.pc.bits, reset_pc.asTypeOf(new IFU_PC_IO), ifu_in_en)
  ifu.io.in.valid := Mux(ifu.io.is_flush, true.B,
    RegEnable(ifu.io.pc.valid, false.B, ifu.io.in.ready))
  val is_bp_flush   = Wire(Bool())
  val is_irq_w      = Wire(Bool())
  val irq_commit    = Wire(Bool())
  val ext_irq_fire  = Wire(Bool()) // 4g：提交间隙采样
  val ext_irq_flush = Wire(Bool()) // 4g：下一拍结构冲刷
  val can_wb        = Wire(Bool())
  val can_wb1       = Wire(Bool())
  val wb_idx        = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb1_idx       = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb_pdest      = Wire(UInt(OoOParams.PHYS_W.W))
  val wb1_pdest     = Wire(UInt(OoOParams.PHYS_W.W))
  val wb_val        = Wire(UInt(32.W))
  val wb1_val       = Wire(UInt(32.W))
  val flush_now     = Wire(Bool())
  val flush_idx     = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val stop_issue    = Wire(Bool())
  val bp_commit_block = Wire(Bool())
  val bp_commit1_block = Wire(Bool())
  val fencei_flush         = Wire(Bool())
  val fencei_commit        = Wire(Bool())
  val fencei_commit_ready  = Wire(Bool())
  val mret_flush           = Wire(Bool())
  val mret_commit          = Wire(Bool())

  // ---------- Rename ----------
  val id_rs1 = idu.io.in.bits.inst(19, 15)
  val id_rs2 = idu.io.in.bits.inst(24, 20)
  val id_rd  = idu.io.out.bits.waddr
  val id1_rs1 = idu1.io.in.bits.inst(19, 15)
  val id1_rs2 = idu1.io.in.bits.inst(24, 20)
  val id1_rd  = idu1.io.out.bits.waddr

  val id_doren = idu.io.out.bits.signals.wbu.reg_write && (idu.io.out.bits.waddr =/= 0.U)
  val id1_doren = idu1.io.out.bits.signals.wbu.reg_write && (idu1.io.out.bits.waddr =/= 0.U)
  val id0_jump = idu.io.out.bits.signals.exu.jump
  val id1_jump = idu1.io.out.bits.signals.exu.jump
  val id0_is_ctrl = (id0_jump =/= JUMP_NONE) && (id0_jump =/= JUMP_MERT)
  val id1_is_ctrl = (id1_jump =/= JUMP_NONE) && (id1_jump =/= JUMP_MERT)
  val id_is_csr = idu.io.out.bits.signals.wbu.csr_write
  val id1_is_csr = idu1.io.out.bits.signals.wbu.csr_write
  val id0_special = id_is_csr || idu.io.is_fencei || (id0_jump === JUMP_MERT) ||
    idu.io.out.bits.is_ebreak || idu.io.out.bits.state.state
  val id1_special = id1_is_csr || idu1.io.is_fencei || (id1_jump === JUMP_MERT) ||
    idu1.io.out.bits.is_ebreak || idu1.io.out.bits.state.state
  val rob_full_stall = Wire(Bool())
  val fl_stall = Wire(Bool())
  val rs_full_stall = Wire(Bool())
  val fq_full_stall = ifu.io.out.valid && !fq.io.enq.ready

  val arch_rf = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  rename.io.rs1       := id_rs1
  rename.io.rs2       := id_rs2
  rename.io.rd        := id_rd
  rename.io.reg_write := idu.io.out.bits.signals.wbu.reg_write
  rename.io.rs1_0     := id_rs1
  rename.io.rs2_0     := id_rs2
  rename.io.rd0       := id_rd
  rename.io.reg_write0 := idu.io.out.bits.signals.wbu.reg_write
  rename.io.is_branch0 := id0_is_ctrl
  rename.io.cp_rob_idx0 := rob.io.enq_idx
  // lane1 fire is driven after the second decode lane is judged usable
  rename.io.rs1_1     := id1_rs1
  rename.io.rs2_1     := id1_rs2
  rename.io.rd1       := id1_rd
  rename.io.reg_write1 := idu1.io.out.bits.signals.wbu.reg_write
  rename.io.is_branch1 := id1_is_ctrl
  rename.io.cp_rob_idx1 := rob.io.enq1_idx
  rename.io.rob_head := rob.io.head
  rename.io.rb_fire     := false.B
  rename.io.rb_do_ren   := false.B
  rename.io.rb_arch_rd  := 0.U
  rename.io.rb_old_phys := 0.U
  rename.io.rb_new_phys := 0.U
  rename.io.free_mask := 0.U
  rename.io.free_vec  := VecInit(Seq.fill(3)(0.U(OoOParams.PHYS_W.W)))
  // checkpoint commit signals are wired after commit logic is known

  val id_pdest    = rename.io.dest_phys
  val id_oldphys  = rename.io.old_phys
  val id_psrc1    = Mux(id_rs1 === 0.U, 0.U, rename.io.rat_out(id_rs1))
  val id_psrc2    = Mux(id_rs2 === 0.U, 0.U, rename.io.rat_out(id_rs2))
  val id1_pdest   = rename.io.dest_phys1
  val id1_oldphys = rename.io.old_phys1
  val id1_psrc1   = Mux(id1_rs1 === 0.U, 0.U, Mux(id_doren && (id1_rs1 === id_rd), id_pdest, rename.io.rat_out(id1_rs1)))
  val id1_psrc2   = Mux(id1_rs2 === 0.U, 0.U, Mux(id_doren && (id1_rs2 === id_rd), id_pdest, rename.io.rat_out(id1_rs2)))

  prf.io.raddr1 := id_psrc1
  prf.io.raddr2 := id_psrc2
  prf.io.raddr3 := id1_psrc1
  prf.io.raddr4 := id1_psrc2
  idu.io.refile.rdata1 := prf.io.rdata1
  idu.io.refile.rdata2 := prf.io.rdata2
  idu1.io.refile.rdata1 := prf.io.rdata3
  idu1.io.refile.rdata2 := prf.io.rdata4

  // ---------- 前递（供 REN 入队时抓值；执行侧靠 RS 已抓值 + CDB）----------
  def DataForward(signal: WBU_signals, alu_result: UInt, imm: UInt, pc4: UInt, csrV: UInt, mem_read: UInt): UInt = {
    MuxLookup(signal.reg_write_sel, 0.U)(Seq(
      ALU_SEL -> alu_result, IMM_SEL -> imm, PC4_SEL -> pc4, CSR_DATA -> csrV, MEM_SEL -> mem_read
    ))
  }
  // EX 结果仅当 out.valid；DIV / LSU 同理
  val exu_res_ok = exu.io.out.valid
  val div_res_ok = exu_div.io.out.valid
  val lsu_res_ok = lsu.io.out.valid
  val exu_wen = exu.io.in.bits.signals.wbu.reg_write && exu.io.in.valid && exu_res_ok &&
    (exu.io.in.bits.pdest =/= 0.U)
  val div_wen = exu_div.io.in.bits.signals.wbu.reg_write && exu_div.io.in.valid && div_res_ok &&
    (exu_div.io.in.bits.pdest =/= 0.U)
  val lsu_wen = lsu.io.in.bits.signals.wbu.reg_write && lsu.io.in.valid && lsu_res_ok &&
    (lsu.io.in.bits.pdest =/= 0.U)
  val wbu_wen = can_wb && wbu.io.in.bits.signals.wbu.reg_write && (wbu.io.in.bits.pdest =/= 0.U)
  val wbu1_wen = can_wb1 && wbu1.io.in.bits.signals.wbu.reg_write && (wbu1.io.in.bits.pdest =/= 0.U)
  val exu_is_load = exu.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val div_is_load = exu_div.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val lsu_is_load = lsu.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val exu_wdata = DataForward(exu.io.in.bits.signals.wbu, exu.io.out.bits.alu_result, exu.io.out.bits.imm_ext, exu.io.out.bits.pc + 4.U, exu.io.out.bits.csr_rd1, 0.U)
  val div_wdata = DataForward(exu_div.io.in.bits.signals.wbu, exu_div.io.out.bits.alu_result, exu_div.io.out.bits.imm_ext, exu_div.io.out.bits.pc + 4.U, exu_div.io.out.bits.csr_rd1, 0.U)
  val lsu_wdata = DataForward(lsu.io.in.bits.signals.wbu, lsu.io.out.bits.alu_result, lsu.io.out.bits.imm_ext, lsu.io.out.bits.pc + 4.U, lsu.io.out.bits.csr_rd1, lsu.io.out.bits.mem_read)
  val wbu_wdata = wbu.io.refile.wdata
  val wbu1_wdata = wbu1.io.refile.wdata

  // mispred 窗口内禁止从流水级前递（防 wrong-path 值入队）
  val fwd_ok = Wire(Bool())
  def PhysForward(psrc: UInt, ren: Bool, fallback: UInt): UInt = {
    val cdb_hit = fwd_ok && can_wb && ren && psrc =/= 0.U && psrc === wb_pdest
    val cdb1_hit = fwd_ok && can_wb1 && ren && psrc =/= 0.U && psrc === wb1_pdest && !cdb_hit
    val exu_hit = fwd_ok && exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest && !exu_is_load && !cdb_hit && !cdb1_hit
    val div_hit = fwd_ok && div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !div_is_load && !cdb_hit && !cdb1_hit && !exu_hit
    val lsu_hit = fwd_ok && lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.in.bits.pdest && !exu_hit && !div_hit && !cdb_hit && !cdb1_hit
    val wbu_hit = fwd_ok && wbu_wen && ren && psrc =/= 0.U && psrc === wbu.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !cdb_hit && !cdb1_hit
    val wbu1_hit = fwd_ok && wbu1_wen && ren && psrc =/= 0.U && psrc === wbu1.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !wbu_hit && !cdb_hit && !cdb1_hit
    MuxCase(fallback, Seq(cdb_hit -> wb_val, cdb1_hit -> wb1_val, exu_hit -> exu_wdata, div_hit -> div_wdata, lsu_hit -> lsu_wdata, wbu_hit -> wbu_wdata, wbu1_hit -> wbu1_wdata))
  }
  def PhysBypassable(psrc: UInt, ren: Bool): Bool = {
    val cdb_hit = fwd_ok && can_wb && ren && psrc =/= 0.U && psrc === wb_pdest
    val cdb1_hit = fwd_ok && can_wb1 && ren && psrc =/= 0.U && psrc === wb1_pdest
    val exu_hit = fwd_ok && exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest && !exu_is_load
    val div_hit = fwd_ok && div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !div_is_load
    val lsu_hit = fwd_ok && lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.in.bits.pdest &&
      (!lsu_is_load || lsu.io.out.valid)
    val wbu_hit = fwd_ok && wbu_wen && ren && psrc =/= 0.U && psrc === wbu.io.in.bits.pdest
    val wbu1_hit = fwd_ok && wbu1_wen && ren && psrc =/= 0.U && psrc === wbu1.io.in.bits.pdest
    cdb_hit || cdb1_hit || exu_hit || div_hit || lsu_hit || wbu_hit || wbu1_hit
  }

  busy.io.raddr1 := id_psrc1
  busy.io.raddr2 := id_psrc2
  busy.io.raddr3 := id1_psrc1
  busy.io.raddr4 := id1_psrc2

  // 3d：未 ready 可 enq；load 结果未到仍 stall（无可抓值）
  def PhysLoadStall(psrc: UInt, ren: Bool): Bool = {
    val exu_hit = exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest
    val div_hit = div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !exu_hit
    val lsu_hit = lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.in.bits.pdest && !exu_hit && !div_hit
    (exu_hit && exu_is_load) || (div_hit && div_is_load) || (lsu_hit && lsu_is_load && !lsu.io.out.valid)
  }
  val id_src1 = PhysForward(id_psrc1, idu.io.rs1_ren, prf.io.rdata1)
  val id_src2 = PhysForward(id_psrc2, idu.io.rs2_ren, prf.io.rdata2)
  val id_s1_rdy = !idu.io.rs1_ren || busy.io.ready1 || PhysBypassable(id_psrc1, idu.io.rs1_ren)
  val id_s2_rdy = !idu.io.rs2_ren || busy.io.ready2 || PhysBypassable(id_psrc2, idu.io.rs2_ren)
  val id1_dep1 = id_doren && idu1.io.rs1_ren && (id1_psrc1 === id_pdest)
  val id1_dep2 = id_doren && idu1.io.rs2_ren && (id1_psrc2 === id_pdest)
  val id1_src1 = Mux(id1_dep1, 0.U, PhysForward(id1_psrc1, idu1.io.rs1_ren, prf.io.rdata3))
  val id1_src2 = Mux(id1_dep2, 0.U, PhysForward(id1_psrc2, idu1.io.rs2_ren, prf.io.rdata4))
  val id1_s1_rdy = !idu1.io.rs1_ren || (!id1_dep1 && (busy.io.ready3 || PhysBypassable(id1_psrc1, idu1.io.rs1_ren)))
  val id1_s2_rdy = !idu1.io.rs2_ren || (!id1_dep2 && (busy.io.ready4 || PhysBypassable(id1_psrc2, idu1.io.rs2_ren)))

  // 4b：在飞 CSR 未提交前禁止再入队；CSR 源必须就绪（rs1_val 只在 enq 抓一次）
  val csr_inflight = VecInit(rob.io.entries.map(e => e.valid && e.csr_write)).asUInt.orR
  val lane0_csr_stall = id_is_csr && csr_inflight
  val lane0_free_ok = !id_doren || (rename.io.free_cnt =/= 0.U)
  val lane0_cp_ok = !id0_is_ctrl || !rename.io.cp_full
  val lane0_space_ok = (rob.io.space =/= 0.U) && (rs.io.space =/= 0.U)
  val lane0_block = stop_issue || lane0_csr_stall || !lane0_free_ok || !lane0_cp_ok || !lane0_space_ok

  val lane1_try = fq.io.deq1.valid && !id0_special && !id0_is_ctrl && !id1_special
  val lane1_need_free = id_doren.asUInt +& id1_doren.asUInt
  val lane1_free_ok = !id1_doren || (rename.io.free_cnt >= lane1_need_free)
  val lane1_cp_ok = !id1_is_ctrl || !rename.io.cp_full
  val lane1_space_ok = (rob.io.space >= 2.U) && (rs.io.space >= 2.U)
  val lane1_csr_stall = id1_is_csr && (csr_inflight || !id1_s1_rdy)
  val lane1_block = lane0_block || !lane1_try || stop_issue || lane1_csr_stall || !lane1_free_ok ||
    !lane1_cp_ok || !lane1_space_ok

  rob_full_stall := idu.io.out.valid && !lane0_space_ok
  fl_stall := idu.io.out.valid && !lane0_free_ok
  rs_full_stall := idu.io.out.valid && (rs.io.space === 0.U)

  idu.io.is_stall := lane0_block
  idu.io.out.ready := !lane0_block
  idu1.io.is_stall := lane1_block
  idu1.io.out.ready := !lane1_block
  val en_ren = idu.io.out.valid && !lane0_block
  val en_ren1 = en_ren && idu1.io.out.valid && !lane1_block

  rename.io.fire := en_ren
  rename.io.fire0 := en_ren
  rename.io.fire1 := en_ren1

  // ---------- ROB 入队 ----------
  rob.io.enq_fire := en_ren
  rob.io.enq_bits := 0.U.asTypeOf(new ROBEntry)
  rob.io.enq_bits.pc            := idu.io.out.bits.pc
  rob.io.enq_bits.inst          := idu.io.out.bits.inst
  rob.io.enq_bits.reg_write     := idu.io.out.bits.signals.wbu.reg_write
  rob.io.enq_bits.reg_write_sel := idu.io.out.bits.signals.wbu.reg_write_sel
  rob.io.enq_bits.csr_write     := idu.io.out.bits.signals.wbu.csr_write
  rob.io.enq_bits.csr_sel       := idu.io.out.bits.signals.wbu.csr_sel
  rob.io.enq_bits.csr_waddr     := idu.io.out.bits.csr_waddr
  rob.io.enq_bits.csr_rd1       := idu.io.out.bits.csr_rd1
  rob.io.enq_bits.rs1_val       := id_src1
  rob.io.enq_bits.state         := idu.io.out.bits.state
  rob.io.enq_bits.arch_rd       := idu.io.out.bits.waddr
  rob.io.enq_bits.new_phys      := id_pdest
  rob.io.enq_bits.old_phys      := id_oldphys
  rob.io.enq_bits.src1_phys     := id_psrc1
  rob.io.enq_bits.src2_phys     := id_psrc2
  rob.io.enq_bits.issued        := false.B
  rob.io.enq_bits.is_ebreak     := idu.io.out.bits.is_ebreak
  rob.io.enq_bits.is_fencei     := idu.io.is_fencei
  rob.io.enq_bits.mem_valid     := idu.io.out.bits.signals.lsu.mem_valid
  rob.io.enq_bits.mem_write     := idu.io.out.bits.signals.lsu.mem_write
  rob.io.enq_bits.mem_rd        := idu.io.out.bits.signals.lsu.mem_rd
  rob.io.enq_bits.mem_wmask     := idu.io.out.bits.signals.lsu.mem_wmask
  rob.io.enq_bits.rs2_val       := id_src2
  rob.io.enq_bits.addr_ready    := false.B
  rob.io.enq_bits.jump          := idu.io.out.bits.signals.exu.jump
  rob.io.enq_bits.bp_valid      := idu.io.out.bits.bp_valid
  rob.io.enq_bits.bp_taken      := idu.io.out.bits.bp_taken
  rob.io.enq_bits.bp_target     := idu.io.out.bits.bp_target
  rob.io.enq_bits.bp_index      := idu.io.out.bits.bp_index
  rob.io.enq_bits.cp_idx        := rename.io.cp_idx0
  rob.io.enq_bits.actual_taken  := false.B
  rob.io.enq_bits.actual_target := idu.io.out.bits.pc + 4.U
  rob.io.enq1_fire := en_ren1
  rob.io.enq1_bits := 0.U.asTypeOf(new ROBEntry)
  rob.io.enq1_bits.pc            := idu1.io.out.bits.pc
  rob.io.enq1_bits.inst          := idu1.io.out.bits.inst
  rob.io.enq1_bits.reg_write     := idu1.io.out.bits.signals.wbu.reg_write
  rob.io.enq1_bits.reg_write_sel := idu1.io.out.bits.signals.wbu.reg_write_sel
  rob.io.enq1_bits.csr_write     := idu1.io.out.bits.signals.wbu.csr_write
  rob.io.enq1_bits.csr_sel       := idu1.io.out.bits.signals.wbu.csr_sel
  rob.io.enq1_bits.csr_waddr     := idu1.io.out.bits.csr_waddr
  rob.io.enq1_bits.csr_rd1       := idu1.io.out.bits.csr_rd1
  rob.io.enq1_bits.rs1_val       := id1_src1
  rob.io.enq1_bits.state         := idu1.io.out.bits.state
  rob.io.enq1_bits.arch_rd       := idu1.io.out.bits.waddr
  rob.io.enq1_bits.new_phys      := id1_pdest
  rob.io.enq1_bits.old_phys      := id1_oldphys
  rob.io.enq1_bits.src1_phys     := id1_psrc1
  rob.io.enq1_bits.src2_phys     := id1_psrc2
  rob.io.enq1_bits.issued        := false.B
  rob.io.enq1_bits.is_ebreak     := idu1.io.out.bits.is_ebreak
  rob.io.enq1_bits.is_fencei     := idu1.io.is_fencei
  rob.io.enq1_bits.mem_valid     := idu1.io.out.bits.signals.lsu.mem_valid
  rob.io.enq1_bits.mem_write     := idu1.io.out.bits.signals.lsu.mem_write
  rob.io.enq1_bits.mem_rd        := idu1.io.out.bits.signals.lsu.mem_rd
  rob.io.enq1_bits.mem_wmask     := idu1.io.out.bits.signals.lsu.mem_wmask
  rob.io.enq1_bits.rs2_val       := id1_src2
  rob.io.enq1_bits.addr_ready    := false.B
  rob.io.enq1_bits.jump          := idu1.io.out.bits.signals.exu.jump
  rob.io.enq1_bits.bp_valid      := idu1.io.out.bits.bp_valid
  rob.io.enq1_bits.bp_taken      := idu1.io.out.bits.bp_taken
  rob.io.enq1_bits.bp_target     := idu1.io.out.bits.bp_target
  rob.io.enq1_bits.bp_index      := idu1.io.out.bits.bp_index
  rob.io.enq1_bits.cp_idx        := rename.io.cp_idx1
  rob.io.enq1_bits.actual_taken  := false.B
  rob.io.enq1_bits.actual_target := idu1.io.out.bits.pc + 4.U
  // 用 RS 的 issue 标 issued：ROB 内部 issue_idx 是最老未发，与 OoO 不一致 → 发射时按 rob_idx 写
  // 简化：issue_fire 仍走 ROB 端口但仅当 issue_idx 匹配时；否则 enq 后靠 RS，issued 仅调试
  rob.io.issue_fire := false.B

  // ---------- RS 入队 ----------
  rs.io.rob_head := rob.io.head
  rs.io.rob_st_pending := sq.io.unresolved_mask
  rs.io.enq_fire := en_ren
  rs.io.enq_bits := 0.U.asTypeOf(new RSEntry)
  rs.io.enq_bits.rob_idx    := rob.io.enq_idx
  rs.io.enq_bits.src1_ready := id_s1_rdy
  rs.io.enq_bits.src2_ready := id_s2_rdy
  rs.io.enq_bits.src1_phys  := id_psrc1
  rs.io.enq_bits.src2_phys  := id_psrc2
  rs.io.enq_bits.src1_val   := id_src1
  rs.io.enq_bits.src2_val   := id_src2
  rs.io.enq_bits.pdest      := id_pdest
  rs.io.enq_bits.old_phys   := id_oldphys
  rs.io.enq_bits.do_rename  := id_doren
  rs.io.enq_bits.pc         := idu.io.out.bits.pc
  rs.io.enq_bits.inst       := idu.io.out.bits.inst
  rs.io.enq_bits.imm_ext    := idu.io.out.bits.imm_ext
  rs.io.enq_bits.waddr      := idu.io.out.bits.waddr
  rs.io.enq_bits.is_ebreak  := idu.io.out.bits.is_ebreak
  rs.io.enq_bits.is_fencei  := idu.io.is_fencei
  rs.io.enq_bits.csr_rd1    := idu.io.out.bits.csr_rd1
  rs.io.enq_bits.csr_waddr  := idu.io.out.bits.csr_waddr
  rs.io.enq_bits.state      := idu.io.out.bits.state
  rs.io.enq_bits.bp_valid   := idu.io.out.bits.bp_valid
  rs.io.enq_bits.bp_taken   := idu.io.out.bits.bp_taken
  rs.io.enq_bits.bp_target  := idu.io.out.bits.bp_target
  rs.io.enq_bits.bp_index   := idu.io.out.bits.bp_index
  rs.io.enq_bits.exu_alu_srcA    := idu.io.out.bits.signals.exu.alu_srcA
  rs.io.enq_bits.exu_alu_srcB    := idu.io.out.bits.signals.exu.alu_srcB
  rs.io.enq_bits.exu_alu_control := idu.io.out.bits.signals.exu.alu_control
  rs.io.enq_bits.exu_jump        := idu.io.out.bits.signals.exu.jump
  rs.io.enq_bits.lsu_mem_wmask   := idu.io.out.bits.signals.lsu.mem_wmask
  rs.io.enq_bits.lsu_mem_rd      := idu.io.out.bits.signals.lsu.mem_rd
  rs.io.enq_bits.lsu_mem_write   := idu.io.out.bits.signals.lsu.mem_write
  rs.io.enq_bits.lsu_mem_valid   := idu.io.out.bits.signals.lsu.mem_valid
  rs.io.enq_bits.wbu_reg_write     := idu.io.out.bits.signals.wbu.reg_write
  rs.io.enq_bits.wbu_reg_write_sel := idu.io.out.bits.signals.wbu.reg_write_sel
  rs.io.enq_bits.wbu_csr_write     := idu.io.out.bits.signals.wbu.csr_write
  rs.io.enq_bits.wbu_csr_sel       := idu.io.out.bits.signals.wbu.csr_sel
  rs.io.enq_bits.cp_idx     := rename.io.cp_idx0
  rs.io.enq1_fire := en_ren1
  rs.io.enq1_bits := 0.U.asTypeOf(new RSEntry)
  rs.io.enq1_bits.rob_idx    := rob.io.enq1_idx
  rs.io.enq1_bits.src1_ready := id1_s1_rdy
  rs.io.enq1_bits.src2_ready := id1_s2_rdy
  rs.io.enq1_bits.src1_phys  := id1_psrc1
  rs.io.enq1_bits.src2_phys  := id1_psrc2
  rs.io.enq1_bits.src1_val   := id1_src1
  rs.io.enq1_bits.src2_val   := id1_src2
  rs.io.enq1_bits.pdest      := id1_pdest
  rs.io.enq1_bits.old_phys   := id1_oldphys
  rs.io.enq1_bits.do_rename  := id1_doren
  rs.io.enq1_bits.pc         := idu1.io.out.bits.pc
  rs.io.enq1_bits.inst       := idu1.io.out.bits.inst
  rs.io.enq1_bits.imm_ext    := idu1.io.out.bits.imm_ext
  rs.io.enq1_bits.waddr      := idu1.io.out.bits.waddr
  rs.io.enq1_bits.is_ebreak  := idu1.io.out.bits.is_ebreak
  rs.io.enq1_bits.is_fencei  := idu1.io.is_fencei
  rs.io.enq1_bits.csr_rd1    := idu1.io.out.bits.csr_rd1
  rs.io.enq1_bits.csr_waddr  := idu1.io.out.bits.csr_waddr
  rs.io.enq1_bits.state      := idu1.io.out.bits.state
  rs.io.enq1_bits.bp_valid   := idu1.io.out.bits.bp_valid
  rs.io.enq1_bits.bp_taken   := idu1.io.out.bits.bp_taken
  rs.io.enq1_bits.bp_target  := idu1.io.out.bits.bp_target
  rs.io.enq1_bits.bp_index   := idu1.io.out.bits.bp_index
  rs.io.enq1_bits.cp_idx     := rename.io.cp_idx1
  rs.io.enq1_bits.exu_alu_srcA    := idu1.io.out.bits.signals.exu.alu_srcA
  rs.io.enq1_bits.exu_alu_srcB    := idu1.io.out.bits.signals.exu.alu_srcB
  rs.io.enq1_bits.exu_alu_control := idu1.io.out.bits.signals.exu.alu_control
  rs.io.enq1_bits.exu_jump        := idu1.io.out.bits.signals.exu.jump
  rs.io.enq1_bits.lsu_mem_wmask   := idu1.io.out.bits.signals.lsu.mem_wmask
  rs.io.enq1_bits.lsu_mem_rd      := idu1.io.out.bits.signals.lsu.mem_rd
  rs.io.enq1_bits.lsu_mem_write   := idu1.io.out.bits.signals.lsu.mem_write
  rs.io.enq1_bits.lsu_mem_valid   := idu1.io.out.bits.signals.lsu.mem_valid
  rs.io.enq1_bits.wbu_reg_write     := idu1.io.out.bits.signals.wbu.reg_write
  rs.io.enq1_bits.wbu_reg_write_sel := idu1.io.out.bits.signals.wbu.reg_write_sel
  rs.io.enq1_bits.wbu_csr_write     := idu1.io.out.bits.signals.wbu.csr_write
  rs.io.enq1_bits.wbu_csr_sel       := idu1.io.out.bits.signals.wbu.csr_sel

  // CDB → RS 唤醒
  rs.io.cdb_valid := can_wb && (wb_pdest =/= 0.U)
  rs.io.cdb_pdest := wb_pdest
  rs.io.cdb_val   := wb_val
  rs.io.cdb1_valid := can_wb1 && (wb1_pdest =/= 0.U)
  rs.io.cdb1_pdest := wb1_pdest
  rs.io.cdb1_val   := wb1_val

  // ---------- 派遣寄存器 RS → EX（可 selective flush，无 StageConnect 组合环）----------
  // ---------- Dispatch registers RS -> EX / LSU ----------
  val d_alu_valid = RegInit(false.B)
  val d_alu_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_div_valid = RegInit(false.B)
  val d_div_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_lsu_valid = RegInit(false.B)
  val d_lsu_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_lsu_sent  = RegInit(false.B)
  d_valid := d_alu_valid || d_div_valid || d_lsu_valid
  d_bits.pc      := MuxCase(d_alu_bits.pc, Seq(
    d_div_valid -> d_div_bits.pc,
    d_lsu_valid -> d_lsu_bits.pc
  ))
  d_bits.rob_idx := MuxCase(d_alu_bits.rob_idx, Seq(
    d_div_valid -> d_div_bits.rob_idx,
    d_lsu_valid -> d_lsu_bits.rob_idx
  ))

  def packIssue(e: RSEntry): IDU_EXU_IO = {
    val b = Wire(new IDU_EXU_IO)
    b.signals.exu.alu_srcA    := e.exu_alu_srcA
    b.signals.exu.alu_srcB    := e.exu_alu_srcB
    b.signals.exu.alu_control := e.exu_alu_control
    b.signals.exu.jump        := e.exu_jump
    b.signals.lsu.mem_wmask   := e.lsu_mem_wmask
    b.signals.lsu.mem_rd      := e.lsu_mem_rd
    b.signals.lsu.mem_write   := e.lsu_mem_write
    b.signals.lsu.mem_valid   := e.lsu_mem_valid
    b.signals.wbu.reg_write     := e.wbu_reg_write
    b.signals.wbu.reg_write_sel := e.wbu_reg_write_sel
    b.signals.wbu.csr_write     := e.wbu_csr_write
    b.signals.wbu.csr_sel       := e.wbu_csr_sel
    b.signals.wbu.irq           := false.B
    b.signals.wbu.irq_num       := 0.U
    b.rd1       := e.src1_val
    b.rd2       := e.src2_val
    b.pc        := e.pc
    b.imm_ext   := e.imm_ext
    b.waddr     := e.waddr
    b.is_ebreak := e.is_ebreak
    b.csr_rd1   := e.csr_rd1
    b.csr_waddr := e.csr_waddr
    b.state     := e.state
    b.bp_valid  := e.bp_valid
    b.bp_taken  := e.bp_taken
    b.bp_target := e.bp_target
    b.bp_index  := e.bp_index
    b.inst      := e.inst
    b.rob_idx   := e.rob_idx
    b.cp_idx    := e.cp_idx
    b.pdest     := e.pdest
    b.old_phys  := e.old_phys
    b.do_rename := e.do_rename
    b
  }

  def laneFlush(valid: Bool, bits: IDU_EXU_IO): Bool =
    valid && (robAge(bits.rob_idx, rob.io.head) > robAge(flush_idx, rob.io.head))

  val lsu_stage_valid = RegInit(false.B)
  val lsu_stage_bits = RegInit(0.U.asTypeOf(new EXU_LSU_IO))
  val flush_lsu_stage = is_irq_w || ((is_bp_flush || fencei_flush || mret_flush) &&
    lsu_stage_valid && (robAge(lsu_stage_bits.rob_idx, rob.io.head) > robAge(flush_idx, rob.io.head)))

  val flush_alu = is_irq_w || ((is_bp_flush || fencei_flush || mret_flush) && laneFlush(d_alu_valid, d_alu_bits))
  val flush_div = is_irq_w || ((is_bp_flush || fencei_flush || mret_flush) && laneFlush(d_div_valid, d_div_bits))
  val flush_lsu = flush_lsu_stage ||
    ((is_bp_flush || fencei_flush || mret_flush) && laneFlush(d_lsu_valid, d_lsu_bits))

  val hold_alu = d_alu_valid && !flush_alu
  val hold_div = d_div_valid && !flush_div
  val hold_lsu = d_lsu_valid && !flush_lsu

  exu.io.in.valid := hold_alu
  exu.io.in.bits  := d_alu_bits
  exu.io.is_flush := is_irq_w || flush_alu
  exu_div.io.in.valid := hold_div
  exu_div.io.in.bits  := d_div_bits
  exu_div.io.is_flush := is_irq_w || flush_div
  exu_lsu.io.in.valid := hold_lsu && !d_lsu_sent
  exu_lsu.io.in.bits  := d_lsu_bits
  exu_lsu.io.is_flush := is_irq_w || flush_lsu

  exu_lsu.io.out.ready := lsu.io.in.ready
  when(flush_lsu) {
    lsu_stage_valid := false.B
  }.elsewhen(exu_lsu.io.out.ready) {
    lsu_stage_valid := exu_lsu.io.out.valid
    when(exu_lsu.io.out.valid) {
      lsu_stage_bits := exu_lsu.io.out.bits
    }
  }
  lsu.io.in.valid := lsu_stage_valid && !flush_lsu
  lsu.io.in.bits  := lsu_stage_bits

  alu_wb := exuToWbu(exu.io.out.bits)
  alu_wb_valid := exu.io.out.valid
  div_wb := exuToWbu(exu_div.io.out.bits)
  div_wb_valid := exu_div.io.out.valid
  lsu_wb := lsu.io.out.bits
  lsu_wb_valid := lsu.io.out.valid

  val alu_leave = hold_alu && exu.io.out.valid && exu.io.out.ready
  val div_leave = hold_div && exu_div.io.out.valid && exu_div.io.out.ready
  val lsu_addr_leave = hold_lsu && !d_lsu_sent && exu_lsu.io.out.valid && exu_lsu.io.out.ready
  val lsu_leave = hold_lsu && lsu.io.out.valid && lsu.io.out.ready

  rs.io.issue_fire    := false.B
  rs.io.free_rob_fire := can_wb
  rs.io.free_rob_idx  := wb_idx
  rs.io.free_rob1_fire := can_wb1
  rs.io.free_rob1_idx  := wb1_idx

  val can_load_alu = !stop_issue && ((!d_alu_valid) || flush_alu)
  val can_load_div = !stop_issue && ((!d_div_valid) || flush_div)
  val can_load_lsu = !stop_issue && ((!d_lsu_valid) || flush_lsu)

  when(flush_alu) {
    when(can_load_alu && rs.io.issue_alu_valid) {
      d_alu_valid := true.B
      d_alu_bits  := packIssue(rs.io.issue_alu_bits)
    }.otherwise {
      d_alu_valid := false.B
    }
  }.elsewhen(alu_leave) {
    d_alu_valid := false.B
  }.elsewhen(!d_alu_valid && can_load_alu && rs.io.issue_alu_valid) {
    d_alu_valid := true.B
    d_alu_bits  := packIssue(rs.io.issue_alu_bits)
  }

  when(flush_div) {
    when(can_load_div && rs.io.issue_div_valid) {
      d_div_valid := true.B
      d_div_bits  := packIssue(rs.io.issue_div_bits)
    }.otherwise {
      d_div_valid := false.B
    }
  }.elsewhen(div_leave) {
    d_div_valid := false.B
  }.elsewhen(!d_div_valid && can_load_div && rs.io.issue_div_valid) {
    d_div_valid := true.B
    d_div_bits  := packIssue(rs.io.issue_div_bits)
  }

  when(flush_lsu) {
    when(can_load_lsu && rs.io.issue_lsu_valid) {
      d_lsu_valid := true.B
      d_lsu_bits  := packIssue(rs.io.issue_lsu_bits)
      d_lsu_sent  := false.B
    }.otherwise {
      d_lsu_valid := false.B
      d_lsu_sent  := false.B
    }
  }.elsewhen(lsu_leave) {
    d_lsu_valid := false.B
    d_lsu_sent  := false.B
  }.elsewhen(lsu_addr_leave) {
    d_lsu_sent  := true.B
  }.elsewhen(!d_lsu_valid && can_load_lsu && rs.io.issue_lsu_valid) {
    d_lsu_valid := true.B
    d_lsu_bits  := packIssue(rs.io.issue_lsu_bits)
    d_lsu_sent  := false.B
  }

  // ---------- 5: StoreQueue handles store -> load forward / wait ----------
  val id0_is_store = idu.io.out.bits.signals.lsu.mem_valid && idu.io.out.bits.signals.lsu.mem_write
  val id1_is_store = idu1.io.out.bits.signals.lsu.mem_valid && idu1.io.out.bits.signals.lsu.mem_write
  sq.io.rob_head := rob.io.head
  sq.io.alloc0_valid := en_ren && id0_is_store
  sq.io.alloc0_rob   := rob.io.enq_idx
  sq.io.alloc0_mask  := idu.io.out.bits.signals.lsu.mem_wmask(3, 0)
  sq.io.alloc1_valid := en_ren1 && id1_is_store
  sq.io.alloc1_rob   := rob.io.enq1_idx
  sq.io.alloc1_mask  := idu1.io.out.bits.signals.lsu.mem_wmask(3, 0)
  sq.io.ld_valid := lsu.io.in.valid && lsu.io.in.bits.signals.lsu.mem_valid &&
    !lsu.io.in.bits.signals.lsu.mem_write
  sq.io.ld_rob   := lsu.io.in.bits.rob_idx
  sq.io.ld_addr  := lsu.io.in.bits.alu_result
  sq.io.ld_mem_rd := lsu.io.in.bits.signals.lsu.mem_rd
  stbuf.io.ld_valid  := sq.io.ld_valid
  stbuf.io.ld_addr   := sq.io.ld_addr
  stbuf.io.ld_mem_rd := sq.io.ld_mem_rd

  lsu.io.st_fwd_wait  := sq.io.wait_load || (!sq.io.fwd_valid && stbuf.io.ld_wait)
  lsu.io.st_fwd_valid := sq.io.fwd_valid || (!sq.io.wait_load && stbuf.io.ld_fwd_valid)
  lsu.io.st_fwd_data  := Mux(sq.io.fwd_valid, sq.io.fwd_data, stbuf.io.ld_fwd_data)

  // ---------- 6：FQ 替换 IF→ID StageConnect ----------
  fq.io.flush := idu.io.is_flush
  fq.io.enq.valid := ifu.io.out.valid
  for (i <- 0 until OoOParams.FETCH_WIDTH) {
    fq.io.enq.bits.valid(i) := ifu.io.out.bits.valid(i)
    fq.io.enq.bits.bits(i).inst      := ifu.io.out.bits.bits(i).inst
    fq.io.enq.bits.bits(i).pc        := ifu.io.out.bits.bits(i).pc
    fq.io.enq.bits.bits(i).state     := ifu.io.out.bits.bits(i).state
    fq.io.enq.bits.bits(i).bp_valid  := ifu.io.out.bits.bits(i).bp_valid
    fq.io.enq.bits.bits(i).bp_taken  := ifu.io.out.bits.bits(i).bp_taken
    fq.io.enq.bits.bits(i).bp_target := ifu.io.out.bits.bits(i).bp_target
    fq.io.enq.bits.bits(i).bp_index  := ifu.io.out.bits.bits(i).bp_index
  }
  ifu.io.out.ready := fq.io.enq.ready

  idu.io.in.valid := fq.io.deq.valid
  idu.io.in.bits.inst      := fq.io.deq.bits.inst
  idu.io.in.bits.pc        := fq.io.deq.bits.pc
  idu.io.in.bits.state     := fq.io.deq.bits.state
  idu.io.in.bits.bp_valid  := fq.io.deq.bits.bp_valid
  idu.io.in.bits.bp_taken  := fq.io.deq.bits.bp_taken
  idu.io.in.bits.bp_target := fq.io.deq.bits.bp_target
  idu.io.in.bits.bp_index  := fq.io.deq.bits.bp_index
  idu1.io.in.valid := fq.io.deq1.valid
  idu1.io.in.bits.inst      := fq.io.deq1.bits.inst
  idu1.io.in.bits.pc        := fq.io.deq1.bits.pc
  idu1.io.in.bits.state     := fq.io.deq1.bits.state
  idu1.io.in.bits.bp_valid  := fq.io.deq1.bits.bp_valid
  idu1.io.in.bits.bp_taken  := fq.io.deq1.bits.bp_taken
  idu1.io.in.bits.bp_target := fq.io.deq1.bits.bp_target
  idu1.io.in.bits.bp_index  := fq.io.deq1.bits.bp_index
  fq.io.deq.ready := idu.io.in.ready
  fq.io.deq1.ready := idu1.io.in.ready

  // ---------- WBU winner selection + ROB writeback ----------
  val wb_cand_valid = VecInit(Seq(alu_wb_valid, lsu_wb_valid, div_wb_valid))
  val wb_cand_mask = wb_cand_valid.asUInt
  val wb_cand_count = PopCount(wb_cand_mask)
  val wb_has_cand = wb_cand_mask.orR
  val wb_conflict = wb_cand_count > OoOParams.CDB_NUM.U
  val wb_cand_idx = PriorityEncoder(wb_cand_mask)
  val wb_cand_mask1 = wb_cand_mask & ~UIntToOH(wb_cand_idx, 3).asUInt
  val wb_has_cand1 = wb_cand_mask1.orR
  val wb_cand_idx1 = PriorityEncoder(wb_cand_mask1)
  val wb_sel = Wire(new LSU_WBU_IO)
  wb_sel := MuxLookup(wb_cand_idx, alu_wb)(Seq(
    1.U -> lsu_wb,
    2.U -> div_wb
  ))
  val wb_sel1 = Wire(new LSU_WBU_IO)
  wb_sel1 := MuxLookup(wb_cand_idx1, alu_wb)(Seq(
    1.U -> lsu_wb,
    2.U -> div_wb
  ))
  wbu.io.in.valid := wb_has_cand
  wbu.io.in.bits  := wb_sel
  wbu.io.is_flush := false.B
  wbu1.io.in.valid := wb_has_cand1
  wbu1.io.in.bits  := wb_sel1
  wbu1.io.is_flush := false.B
  exu.io.out.ready := (wb_has_cand && (wb_cand_idx === 0.U)) ||
    (wb_has_cand1 && (wb_cand_idx1 === 0.U))
  lsu.io.out.ready := (wb_has_cand && (wb_cand_idx === 1.U)) ||
    (wb_has_cand1 && (wb_cand_idx1 === 1.U))
  exu_div.io.out.ready := (wb_has_cand && (wb_cand_idx === 2.U)) ||
    (wb_has_cand1 && (wb_cand_idx1 === 2.U))
  exu.io.pc.ready := true.B
  exu_div.io.pc.ready := true.B
  exu_lsu.io.pc.ready := true.B
  lsu.io.is_flush := is_irq_w || flush_lsu

  val mis_predict_w = Wire(Bool())
  val mis_rob_w     = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb_fire  = wbu.io.in.valid && !wbu.io.is_flush
  wb_idx   := wbu.io.in.bits.rob_idx
  val wb1_fire = wbu1.io.in.valid && !wbu1.io.is_flush
  wb1_idx  := wbu1.io.in.bits.rob_idx
  wb_pdest := wbu.io.in.bits.pdest
  wb_val   := wbu.io.refile.wdata
  wb1_pdest := wbu1.io.in.bits.pdest
  wb1_val   := wbu1.io.refile.wdata
  // ??????????? winner ? rob_idx ??
  val head_e = rob.io.entries(rob.io.head)
  val wb_entry = rob.io.entries(wb_idx)
  val wb1_entry = rob.io.entries(wb1_idx)
  val wb_matches_entry = wb_entry.valid &&
    (wb_entry.pc === wbu.io.in.bits.pc) &&
    (wb_entry.arch_rd === wbu.io.in.bits.waddr) &&
    (wb_entry.new_phys === wbu.io.in.bits.pdest)
  val wb1_matches_entry = wb1_entry.valid &&
    (wb1_entry.pc === wbu1.io.in.bits.pc) &&
    (wb1_entry.arch_rd === wbu1.io.in.bits.waddr) &&
    (wb1_entry.new_phys === wbu1.io.in.bits.pdest)
  val head_wb0_raw = wb_fire && wb_matches_entry && (wb_idx === rob.io.head)
  val head_wb1_raw = wb1_fire && wb1_matches_entry && (wb1_idx === rob.io.head)
  val head_wb_raw_valid = head_wb0_raw || head_wb1_raw
  val head_wb_raw_bits = Wire(new LSU_WBU_IO)
  head_wb_raw_bits := Mux(head_wb0_raw, wbu.io.in.bits, wbu1.io.in.bits)
  val head_is_exc = head_e.valid && Mux(head_e.done, head_e.state.state,
    head_wb_raw_valid && head_wb_raw_bits.state.state)
  def wbKilled(idx: UInt): Bool = {
    val youngerMis = mis_predict_w &&
      (robAge(idx, rob.io.head) > robAge(mis_rob_w, rob.io.head))
    val youngerFlush = flush_now &&
      (robAge(idx, rob.io.head) > robAge(flush_idx, rob.io.head))
    val youngerIrq = head_is_exc && (robAge(idx, rob.io.head) > 0.U)
    val youngerFencei = head_e.valid && head_e.is_fencei && head_e.done &&
      fencei_commit_ready && !fencei_flush && (robAge(idx, rob.io.head) > 0.U)
    val youngerMret = head_e.valid && (head_e.jump === JUMP_MERT) && head_e.done &&
      !mret_flush && (robAge(idx, rob.io.head) > 0.U)
    youngerMis || youngerFlush || youngerIrq || youngerFencei || youngerMret || ext_irq_flush
  }
  can_wb   := wb_fire && wb_matches_entry && !wb_entry.done &&
    !wbKilled(wb_idx)
  can_wb1  := wb1_fire && wb1_matches_entry && !wb1_entry.done &&
    !(can_wb && (wb1_idx === wb_idx)) && !wbKilled(wb1_idx)
  val wb_accept_count = PopCount(VecInit(Seq(can_wb, can_wb1)).asUInt)
  val wb_blocked = wb_cand_count =/= wb_accept_count
  val wb_wen = can_wb && wbu.io.refile.wen && (wb_pdest =/= 0.U) && (wbu.io.in.bits.waddr =/= 0.U)
  val wb1_wen = can_wb1 && wbu1.io.refile.wen && (wb1_pdest =/= 0.U) && (wbu1.io.in.bits.waddr =/= 0.U)
  val head_wb0 = can_wb && (wb_idx === rob.io.head)
  val head_wb1 = can_wb1 && (wb1_idx === rob.io.head)
  val head_wb_valid = head_wb0 || head_wb1
  val head_wb_bits = Wire(new LSU_WBU_IO)
  head_wb_bits := Mux(head_wb0, wbu.io.in.bits, wbu1.io.in.bits)
  val head_wb_val = Mux(head_wb0, wb_val, wb1_val)

  prf.io.wen1   := wb_wen
  prf.io.waddr1 := wb_pdest
  prf.io.wdata1 := wb_val
  prf.io.wen2   := wb1_wen
  prf.io.waddr2 := wb1_pdest
  prf.io.wdata2 := wb1_val

  busy.io.set_en   := en_ren && id_doren
  busy.io.set_addr := id_pdest
  busy.io.set_en2  := en_ren1 && id1_doren
  busy.io.set_addr2 := id1_pdest
  busy.io.clr_en   := wb_wen
  busy.io.clr_addr := wb_pdest
  busy.io.clr_en2  := wb1_wen
  busy.io.clr_addr2 := wb1_pdest

  rob.io.wb_fire         := can_wb
  rob.io.wb_idx          := wb_idx
  rob.io.wb_val          := wb_val
  rob.io.wb_state        := wbu.io.in.bits.state
  rob.io.wb_mem_addr     := wbu.io.in.bits.alu_result
  rob.io.wb_mem_wdata    := wbu.io.in.bits.store_data
  rob.io.wb_actual_taken := wbu.io.in.bits.br_taken
  rob.io.wb_actual_target := wbu.io.in.bits.next_pc
  rob.io.wb1_fire         := can_wb1
  rob.io.wb1_idx          := wb1_idx
  rob.io.wb1_val          := wb1_val
  rob.io.wb1_state        := wbu1.io.in.bits.state
  rob.io.wb1_mem_addr     := wbu1.io.in.bits.alu_result
  rob.io.wb1_mem_wdata    := wbu1.io.in.bits.store_data
  rob.io.wb1_actual_taken := wbu1.io.in.bits.br_taken
  rob.io.wb1_actual_target := wbu1.io.in.bits.next_pc

  val cm_bits = rob.io.commit_bits
  val cm1_bits = rob.io.commit1_bits
  // 4c：异常冲刷拍禁止提交后继；5a：store 写完才 commit_fire；4f：fencei icache ready 才 commit_fire
  // 4e：mret_flush 拍禁止提交后继（同 fencei）
  // 4g：ext_irq_flush 拍禁止提交（冲在飞）
  val is_irq_early = Wire(Bool())
  val store_commit_ready = Wire(Bool())
  val store_side_empty = Wire(Bool())
  val cm_is_store = cm_bits.mem_valid && cm_bits.mem_write
  val cm_is_fencei = cm_bits.is_fencei
  val cm_is_mret = cm_bits.jump === JUMP_MERT
  val cm_needs_store_drain = cm_bits.is_ebreak || cm_is_mret || cm_bits.state.state
  val cm_is_ctrl = cm_bits.jump =/= JUMP_NONE
  val cm1_is_store = cm1_bits.mem_valid && cm1_bits.mem_write
  val cm1_is_fencei = cm1_bits.is_fencei
  val cm1_is_mret = cm1_bits.jump === JUMP_MERT
  val cm1_is_ctrl = cm1_bits.jump =/= JUMP_NONE
  val cm1_is_ctrl_not_mret = cm1_is_ctrl && !cm1_is_mret
  val cm_exclusive = cm_bits.mem_valid || cm_bits.csr_write || cm_is_fencei || cm_is_mret ||
    cm_bits.is_ebreak || cm_bits.state.state || cm_is_ctrl
  val cm1_exclusive = cm1_bits.mem_valid || cm1_bits.csr_write || cm1_is_fencei || cm1_is_mret ||
    cm1_bits.is_ebreak || cm1_bits.state.state || cm1_is_ctrl
  // fencei/mret/ext_irq flush 拍禁止提交后继（否则会在冲刷前多提交 jal 等，difftest PC 错位）
  rob.io.commit_fire := rob.io.commit_valid && !is_irq_early && !fencei_flush && !mret_flush &&
    !ext_irq_flush && !bp_commit_block &&
    (!cm_is_store || store_commit_ready) &&
    (!cm_is_fencei || fencei_commit_ready) &&
    (!cm_needs_store_drain || store_side_empty)
  val cm_fire   = rob.io.commit_fire
  rob.io.commit1_fire := cm_fire && rob.io.commit1_valid && !cm_exclusive && !cm1_exclusive &&
    !bp_commit1_block
  val cm1_fire = rob.io.commit1_fire
  val cm_do_ren = cm_fire && cm_bits.reg_write && (cm_bits.arch_rd =/= 0.U) && (cm_bits.new_phys =/= 0.U)
  val cm1_do_ren = cm1_fire && cm1_bits.reg_write && (cm1_bits.arch_rd =/= 0.U) && (cm1_bits.new_phys =/= 0.U)
  val cm1_slot_available = cm_fire && rob.io.commit1_valid
  val cm1_slot_not_ready = cm_fire && !rob.io.commit1_valid
  val cm1_slot_blocked = cm1_slot_available && !cm1_fire
  val cm1_block_slot0_excl = cm1_slot_blocked && cm_exclusive
  val cm1_block_mem = cm1_slot_blocked && !cm_exclusive && cm1_bits.mem_valid
  val cm1_block_ctrl = cm1_slot_blocked && !cm_exclusive && !cm1_bits.mem_valid &&
    cm1_is_ctrl_not_mret
  val cm1_block_csr = cm1_slot_blocked && !cm_exclusive && !cm1_bits.mem_valid &&
    !cm1_is_ctrl_not_mret && cm1_bits.csr_write
  val cm1_block_special = cm1_slot_blocked && !cm_exclusive && !cm1_bits.mem_valid &&
    !cm1_is_ctrl_not_mret && !cm1_bits.csr_write &&
    (cm1_is_fencei || cm1_is_mret || cm1_bits.is_ebreak || cm1_bits.state.state)
  val cm1_block_bp = cm1_slot_blocked && !cm_exclusive && !cm1_exclusive && bp_commit1_block

  val wb_store = can_wb && rob.io.entries(wb_idx).valid &&
    rob.io.entries(wb_idx).mem_valid && rob.io.entries(wb_idx).mem_write
  val wb1_store = can_wb1 && rob.io.entries(wb1_idx).valid &&
    rob.io.entries(wb1_idx).mem_valid && rob.io.entries(wb1_idx).mem_write
  sq.io.wb_valid := wb_store || wb1_store
  sq.io.wb_rob  := Mux(wb_store, wb_idx, wb1_idx)
  sq.io.wb_addr := Mux(wb_store, wbu.io.in.bits.alu_result, wbu1.io.in.bits.alu_result)
  sq.io.wb_data := Mux(wb_store, wbu.io.in.bits.store_data, wbu1.io.in.bits.store_data)
  sq.io.wb_mask := Mux(wb_store, rob.io.entries(wb_idx).mem_wmask, rob.io.entries(wb1_idx).mem_wmask)(3, 0)
  sq.io.commit_valid := cm_fire && cm_is_store
  sq.io.commit_rob   := rob.io.head

  if (conf.statistics) {
    PM(conf, clock, EVENT_ROB_FULL, 1.U, rob_full_stall)
    PM(conf, clock, EVENT_FL_EMPTY, 1.U, fl_stall)
    PM(conf, clock, EVENT_RS_FULL, 1.U, rs_full_stall)
    PM(conf, clock, EVENT_FQ_FULL, 1.U, fq_full_stall)
    PM(conf, clock, EVENT_FQ_EMPTY, 1.U, !fq.io.deq.valid && !flush_now && !stop_issue)
    PM(conf, clock, EVENT_CDB_CONFLICT, 1.U, wb_conflict)
    PM(conf, clock, EVENT_CDB_BLOCKED, 1.U, wb_blocked)
    PM(conf, clock, EVENT_COMMIT_HEAD_WAIT, 1.U, head_e.valid && !rob.io.commit_valid)
    PM(conf, clock, EVENT_COMMIT_WAIT_STORE, 1.U, rob.io.commit_valid && cm_is_store && !store_commit_ready)
    PM(conf, clock, EVENT_COMMIT_WAIT_FENCE, 1.U, rob.io.commit_valid && cm_is_fencei && !fencei_commit_ready)
    PM(conf, clock, EVENT_COMMIT_WAIT_BP, 1.U, rob.io.commit_valid && bp_commit_block)
    PM(conf, clock, EVENT_COMMIT_WAIT_FLUSH, 1.U, rob.io.commit_valid && (is_irq_early || fencei_flush || mret_flush || ext_irq_flush))
    PM(conf, clock, EVENT_COMMIT_SLOT0, 1.U, cm_fire)
    PM(conf, clock, EVENT_COMMIT_SLOT1, 1.U, cm1_fire)
    PM(conf, clock, EVENT_COMMIT2, 1.U, cm_fire && cm1_fire)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK, 1.U,
      cm1_slot_blocked)
    PM(conf, clock, EVENT_COMMIT_SLOT1_NOT_READY, 1.U, cm1_slot_not_ready)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_SLOT0_EXCL, 1.U, cm1_block_slot0_excl)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_MEM, 1.U, cm1_block_mem)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_CTRL, 1.U, cm1_block_ctrl)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_CSR, 1.U, cm1_block_csr)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_SPECIAL, 1.U, cm1_block_special)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_BP, 1.U, cm1_block_bp)
    PM(conf, clock, EVENT_FQ_ENQ2, 1.U,
      fq.io.enq.fire && fq.io.enq.bits.valid(0) && fq.io.enq.bits.valid(1))
    PM(conf, clock, EVENT_FQ_SPACE_ONE, 1.U,
      ifu.io.out.valid && ifu.io.out.bits.valid(0) && fq.io.space === 1.U)
  }

  rename.io.commit_fire := cm_fire
  rename.io.cm_do_ren   := cm_do_ren
  rename.io.cm_old_phys := cm_bits.old_phys
  rename.io.cm_new_phys := cm_bits.new_phys
  rename.io.cm_arch_rd  := cm_bits.arch_rd
  rename.io.cm_cp_valid := cm_fire && (cm_bits.jump =/= JUMP_NONE) && (cm_bits.jump =/= JUMP_MERT)
  rename.io.cm_cp_idx   := cm_bits.cp_idx
  rename.io.commit1_fire := cm1_fire
  rename.io.cm1_do_ren   := cm1_do_ren
  rename.io.cm1_old_phys := cm1_bits.old_phys
  rename.io.cm1_new_phys := cm1_bits.new_phys
  rename.io.cm1_arch_rd  := cm1_bits.arch_rd
  rename.io.cm1_cp_valid := false.B
  rename.io.cm1_cp_idx   := cm1_bits.cp_idx

  val cm_wb_same = cm_fire && head_wb_valid
  val cm1_wb0 = can_wb && (wb_idx === rob.io.commit1_idx)
  val cm1_wb1 = can_wb1 && (wb1_idx === rob.io.commit1_idx)
  val cm1_wb_valid = cm1_wb0 || cm1_wb1
  val cm1_wb_bits = Wire(new LSU_WBU_IO)
  cm1_wb_bits := Mux(cm1_wb0, wbu.io.in.bits, wbu1.io.in.bits)
  val cm1_wb_val = Mux(cm1_wb0, wb_val, wb1_val)
  val cm1_wb_same = cm1_fire && cm1_wb_valid
  when(cm_do_ren) {
    arch_rf(cm_bits.arch_rd) := Mux(cm_wb_same, head_wb_val, cm_bits.dest_val)
  }
  when(cm1_do_ren) {
    arch_rf(cm1_bits.arch_rd) := Mux(cm1_wb_same, cm1_wb_val, cm1_bits.dest_val)
  }

  // 4b：CSR 架构写仅 commit（wdata 用入队时 rs1 / csr_rd1）
  val cm_csr_wdata = MuxLookup(cm_bits.csr_sel, 0.U)(Seq(
    CSR_RD1 -> cm_bits.rs1_val,
    CSR_XOR -> (cm_bits.rs1_val | cm_bits.csr_rd1),
    CSR_PC  -> cm_bits.pc
  ))
  csr.io.write.wen   := cm_fire && cm_bits.csr_write
  csr.io.write.waddr := cm_bits.csr_waddr
  csr.io.write.wdata := cm_csr_wdata

  // ---------- 异常 / 分支 / 外部中断 ----------
  // 4c：异常认 ROB head commit（非 WBU RegNext）
  // 提交拍写 mepc/mcause；下一拍结构冲刷（同 mispred_r）
  val head_state = Mux(head_wb_valid, head_wb_bits.state, cm_bits.state)
  irq_commit := cm_fire && head_state.state
  val irq_commit_r = RegNext(irq_commit, false.B)
  val irq_rob_r    = RegEnable(rob.io.head, 0.U, irq_commit)
  // irq_mtvec / csr.irq / is_irq 在 4g 采样定稿后赋值（见下方）
  val is_irq = Wire(Bool())
  is_irq_early := is_irq
  is_irq_w := is_irq
  val wb_state_write = Mux(can_wb && wbu.io.in.bits.state.state,
    wbu.io.in.bits.state,
    Mux(can_wb1, wbu1.io.in.bits.state, wbu.io.in.bits.state))
  val state_reg = RegEnable(wb_state_write, 0.U.asTypeOf(new State), can_wb || can_wb1)
  ifu.io.state := state_reg
  idu.io.state := state_reg
  idu1.io.state := state_reg

  // mispred 检测：用 d_reg 内容 + EX 组合 pc_src，不依赖 flush_d / in.valid（断环）
  val pc_src = exu.io.pc.bits.pc_src
  val correct_pc = MuxLookup(pc_src, exu.io.pc.bits.pc4)(Seq(
    PC_PLUS4 -> exu.io.pc.bits.pc4,
    PC_IMM   -> exu.io.pc.bits.pc4_imm,
    PC_RS2   -> exu.io.pc.bits.pc4_rs2,
    MEPC     -> csr.io.read.mepc
  ))
  // 直接根据 EXU 当前结果判定 mispred；避免等 leave 一拍后再冲刷年轻指令
  val br_done = d_alu_valid && (d_alu_bits.signals.exu.jump =/= JUMP_NONE) &&
    (d_alu_bits.signals.exu.jump =/= JUMP_MERT)
  val is_ch    = br_done && (pc_src =/= PC_PLUS4)
  val predict_taken = d_alu_bits.bp_taken
  val target_mispredict = is_ch && predict_taken && (d_alu_bits.bp_target =/= correct_pc)
  val mis_predict = br_done && ((is_ch =/= predict_taken) || target_mispredict)
  val mis_predict_dbg = Wire(Bool())
  mis_predict_dbg := mis_predict
  dontTouch(mis_predict_dbg)
  val mis_predict_r = RegNext(mis_predict, false.B)
  val mis_rob_r     = RegEnable(d_alu_bits.rob_idx, 0.U, mis_predict)
  val mis_cp_r      = RegEnable(d_alu_bits.cp_idx, 0.U, mis_predict)
  val correct_pc_r  = RegNext(correct_pc, 0.U)
  val mis_next_rob_r = Mux(mis_rob_r === (OoOParams.ROB_SIZE - 1).U, 0.U, mis_rob_r + 1.U)
  mis_predict_w := mis_predict
  mis_rob_w     := d_alu_bits.rob_idx

  is_bp_flush := mis_predict_dbg
  bp_commit_block := mis_predict_r && (rob.io.commit_idx === mis_next_rob_r)
  bp_commit1_block := mis_predict_r && (rob.io.commit1_idx === mis_next_rob_r)
  dontTouch(bp_commit_block)
  // 4f：fencei 提交拍组合停 issue；结构冲刷下一拍 flush_all（已提交，只剩 younger）
  fencei_commit := cm_fire && cm_is_fencei
  fencei_flush := RegNext(fencei_commit, false.B)
  val fencei_pc_r = RegEnable(cm_bits.pc, 0.U, fencei_commit)
  // 4e：mret 同 fencei 时序；PC←mepc（NEMU 不恢复 mstatus，本核也跳过）
  mret_commit := cm_fire && cm_is_mret
  mret_flush := RegNext(mret_commit, false.B)
  val mret_mepc_r = RegEnable(csr.io.read.mepc, 0.U, mret_commit)

  // 4g：外部中断仅提交间隙采样；写 CSR 后下一拍 flush_all（对齐 fencei/mret）
  // 间隙条件故意不依赖 cm_fire/can_wb（断组合环）；用 commit_valid+head 类型近似「本拍会提交」
  // mepc：ROB 非空 ← head.pc；空 ← IF 当前装入 PC（ifu.in.bits.next_pc）
  val cm_writing_gap = Wire(Bool())
  val head_would_cm = rob.io.commit_valid &&
    (!cm_is_store || store_commit_ready) &&
    (!cm_is_fencei || fencei_commit_ready) &&
    (!cm_needs_store_drain || store_side_empty)
  val head_is_exc_gap = head_e.valid && Mux(head_e.done, head_e.state.state,
    head_wb_raw_valid && head_wb_raw_bits.state.state)
  val commit_gap = !head_would_cm && !head_is_exc_gap &&
    !mis_predict && !mis_predict_r && !fencei_flush && !mret_flush && !cm_writing_gap &&
    !irq_commit_r && !ext_irq_flush
  ext_irq_fire := io.interrupt && commit_gap
  ext_irq_flush := RegNext(ext_irq_fire, false.B)
  val rob_nonempty = rob.io.count =/= 0.U
  val ext_mepc = Mux(rob_nonempty, head_e.pc, ifu.io.in.bits.next_pc)
  is_irq := irq_commit_r || ext_irq_flush
  val irq_mtvec_r = RegEnable(csr.io.read.mtvec, 0.U, irq_commit || ext_irq_fire)
  // 优先级：项内 exception > 正常 commit > 外部中断间隙
  csr.io.irq    := irq_commit || ext_irq_fire
  csr.io.irq_no := Mux(irq_commit, head_state.state_num, IRQ_MEXT)
  csr.io.irq_pc := Mux(irq_commit, cm_bits.pc, ext_mepc)

  ifu.io.correct_pc := Mux(is_irq, irq_mtvec_r,
    Mux(mis_predict_dbg, correct_pc,
      Mux(fencei_flush, fencei_pc_r + 4.U,
        Mux(mret_flush, mret_mepc_r, 0.U))))
  // ROB/RS 注册拍结构冲刷；组合 mispred/irq/fencei/mret/ext_irq commit 停 issue
  flush_now := is_bp_flush || is_irq || fencei_flush || mret_flush
  stop_issue := flush_now || mis_predict || irq_commit || fencei_commit || mret_commit ||
    ext_irq_fire
  fwd_ok := !mis_predict && !mis_predict_r && !is_irq && !irq_commit &&
    !fencei_commit && !fencei_flush && !mret_commit && !mret_flush &&
    !ext_irq_fire && !ext_irq_flush
  flush_idx := Mux(is_irq, irq_rob_r, Mux(mis_predict_dbg, d_alu_bits.rob_idx, mis_rob_r))

  ifu.io.is_flush := is_irq || fencei_flush || mret_flush || mis_predict_dbg
  idu.io.is_flush := is_irq || fencei_flush || mret_flush || mis_predict_dbg || mis_predict ||
    irq_commit || fencei_commit || mret_commit || ext_irq_fire
  idu1.io.is_flush := idu.io.is_flush

  // LSU/WBU 仅注册拍 irq（selective flush 会丢已 leave 指令）

  val selective_flush = flush_now && !is_irq && !fencei_flush && !mret_flush
  rob.io.flush     := selective_flush
  rob.io.flush_idx := flush_idx
  rob.io.flush_all := is_irq || fencei_flush || mret_flush
  rs.io.flush      := flush_now && !fencei_flush && !mret_flush
  rs.io.flush_idx  := flush_idx
  rs.io.flush_all  := is_irq || fencei_flush || mret_flush
  sq.io.flush      := selective_flush
  sq.io.flush_idx  := flush_idx
  sq.io.flush_all  := is_irq || fencei_flush || mret_flush

  // flush 重建 RAT/free
  val cm_this = cm_fire && rob.io.entries(rob.io.head).valid
  val cm1_this = cm1_fire && rob.io.entries(rob.io.commit1_idx).valid
  val cm_count = cm_this.asUInt +& cm1_this.asUInt
  val rb_head = (rob.io.head + cm_count)(OoOParams.ROB_PTR_W - 1, 0)
  val rb_empty = is_irq || fencei_flush || mret_flush ||
    (cm_this && (rob.io.head === flush_idx)) ||
    (cm1_this && (rob.io.commit1_idx === flush_idx))
  val kept_n = Mux(rb_empty, 0.U, Mux(flush_idx >= rb_head,
    (flush_idx - rb_head) + 1.U,
    (OoOParams.ROB_SIZE.U - rb_head) + flush_idx + 1.U))

  val rb_base = Wire(Vec(32, UInt(OoOParams.PHYS_W.W)))
  for (i <- 0 until 32) {
    val cmHit = cm_do_ren && (i.U === cm_bits.arch_rd) && (i.U =/= 0.U)
    val cm1Hit = cm1_do_ren && (i.U === cm1_bits.arch_rd) && (i.U =/= 0.U)
    rb_base(i) := Mux(cm1Hit, cm1_bits.new_phys,
      Mux(cmHit, cm_bits.new_phys, rename.io.arch_rat_out(i)))
  }

  val rb_steps = Wire(Vec(OoOParams.ROB_SIZE + 1, Vec(32, UInt(OoOParams.PHYS_W.W))))
  rb_steps(0) := rb_base
  for (off <- 0 until OoOParams.ROB_SIZE) {
    val idx  = (rb_head + off.U)(OoOParams.ROB_PTR_W - 1, 0)
    val take = (off.U < kept_n) && rob.io.entries(idx).valid &&
      rob.io.entries(idx).reg_write && (rob.io.entries(idx).arch_rd =/= 0.U)
    for (a <- 0 until 32) {
      rb_steps(off + 1)(a) := Mux(take && (rob.io.entries(idx).arch_rd === a.U),
        rob.io.entries(idx).new_phys, rb_steps(off)(a))
    }
  }
  val rb_rat = rb_steps(OoOParams.ROB_SIZE)

  val archUsed = (1 until 32).foldLeft(1.U(OoOParams.N_PHYS.W)) { (acc, a) =>
    acc | (1.U(OoOParams.N_PHYS.W) << rb_base(a))
  }
  val freeSteps = Wire(Vec(OoOParams.ROB_SIZE + 1, UInt(OoOParams.N_PHYS.W)))
  freeSteps(0) := ~archUsed
  for (off <- 0 until OoOParams.ROB_SIZE) {
    val idx  = (rb_head + off.U)(OoOParams.ROB_PTR_W - 1, 0)
    val take = (off.U < kept_n) && rob.io.entries(idx).valid &&
      rob.io.entries(idx).reg_write && (rob.io.entries(idx).arch_rd =/= 0.U)
    val np = rob.io.entries(idx).new_phys
    val op = rob.io.entries(idx).old_phys
    val f0 = freeSteps(off)
    val f1 = Mux(take && np =/= 0.U, f0 & ~(1.U(OoOParams.N_PHYS.W) << np), f0)
    freeSteps(off + 1) := Mux(take && op =/= 0.U, f1 & ~(1.U(OoOParams.N_PHYS.W) << op), f1)
  }
  val rb_free = freeSteps(OoOParams.ROB_SIZE) & ~1.U(OoOParams.N_PHYS.W)

  rename.io.rebuild      := is_irq || fencei_flush || mret_flush
  rename.io.rebuild_rat  := rb_rat
  rename.io.rebuild_free := rb_free
  rename.io.restore_cp   := is_bp_flush
  rename.io.restore_cp_idx := Mux(mis_predict_dbg, d_alu_bits.cp_idx, mis_cp_r)
  rename.io.restore_arch  := false.B
  rename.io.restore_rob_idx := mis_rob_r

  def isYoungerThanFlush(idx: UInt): Bool = {
    val idxAge = robAge(idx, rob.io.head)
    val flushAgeNow = robAge(flush_idx, rob.io.head)
    (idxAge < rob.io.count) && (idxAge > flushAgeNow)
  }

  val killBusy = Wire(UInt(OoOParams.N_PHYS.W))
  val killBits = Wire(Vec(OoOParams.ROB_SIZE, UInt(OoOParams.N_PHYS.W)))
  for (i <- 0 until OoOParams.ROB_SIZE) {
    val idx = i.U(OoOParams.ROB_PTR_W.W)
    val after = isYoungerThanFlush(idx)
    val e = rob.io.entries(idx)
    val kill = flush_now && after && e.valid && e.reg_write && (e.new_phys =/= 0.U)
    killBits(i) := Mux(kill, 1.U(OoOParams.N_PHYS.W) << e.new_phys, 0.U)
  }
  killBusy := killBits.reduce(_ | _)

  val busyKeep = Wire(Vec(OoOParams.ROB_SIZE, UInt(OoOParams.N_PHYS.W)))
  for (i <- 0 until OoOParams.ROB_SIZE) {
    val idx = i.U(OoOParams.ROB_PTR_W.W)
    val after = isYoungerThanFlush(idx)
    val e = rob.io.entries(idx)
    val keepBusy = flush_now && e.valid && !after && e.reg_write &&
      !e.done && (e.new_phys =/= 0.U)
    busyKeep(i) := Mux(keepBusy, 1.U(OoOParams.N_PHYS.W) << e.new_phys, 0.U)
  }
  val wbClearMask =
    Mux(can_wb && (wb_pdest =/= 0.U), 1.U(OoOParams.N_PHYS.W) << wb_pdest, 0.U) |
    Mux(can_wb1 && (wb1_pdest =/= 0.U), 1.U(OoOParams.N_PHYS.W) << wb1_pdest, 0.U)
  val busyRebuild = busyKeep.reduce(_ | _) & ~wbClearMask
  val robEmpty = rob.io.count === 0.U
  val busySweep = robEmpty && !en_ren && !en_ren1
  // 4c：异常整表清空 → busy 也清空（勿按旧 flush_idx 选择性保留）
  busy.io.rebuild      := flush_now || busySweep
  busy.io.rebuild_mask := Mux(is_irq || fencei_flush || mret_flush, 0.U, Mux(flush_now, busyRebuild, 0.U))
  busy.io.clr_mask     := Mux(flush_now || busySweep, 0.U, killBusy)

  // BPU@commit（4e：MRET 不更新 BPU）
  val cm_jump = cm_bits.jump
  val cm_is_jump = cm_fire && (cm_jump =/= JUMP_NONE) && (cm_jump =/= JUMP_MERT)
  val cm_is_branch = cm_is_jump && (cm_jump === JUMP_BEQ || cm_jump === JUMP_BNE ||
    cm_jump === JUMP_BLT || cm_jump === JUMP_BGE || cm_jump === JUMP_BLTU || cm_jump === JUMP_BGEU)
  val cm_inst = cm_bits.inst
  val cm_rd_ra  = cm_inst(11, 7) === 1.U
  val cm_rs1_ra = cm_inst(19, 15) === 1.U
  ifu.io.bpu_update_valid := cm_is_jump
  ifu.io.bpu_update_taken := cm_bits.actual_taken
  ifu.io.bpu_update_pc := cm_bits.pc
  ifu.io.bpu_update_target := cm_bits.actual_target
  ifu.io.bpu_update_is_branch := cm_is_branch
  ifu.io.bpu_update_is_jalr := cm_is_jump && (cm_jump === JUMP_JALR)
  ifu.io.bpu_update_index := cm_bits.bp_index
  ifu.io.bpu_update_is_call := cm_is_jump && cm_rd_ra &&
    ((cm_jump === JUMP_JAL) || (cm_jump === JUMP_JALR && !cm_rs1_ra))
  ifu.io.bpu_update_is_ret := cm_is_jump && (cm_jump === JUMP_JALR) && cm_rs1_ra

  if (conf.statistics) {
    PM(conf, clock, EVENT_BPU_PREDICT, 1.U, br_done && d_alu_bits.bp_valid)
    PM(conf, clock, EVENT_BPU_MISPRED, 1.U, mis_predict && d_alu_bits.bp_valid)
    PM(conf, clock, EVENT_BP_FLUSH, 1.U, mis_predict)
    PM(conf, clock, EVENT_BPU_DIR_MISPRED, 1.U, mis_predict && d_alu_bits.bp_valid && (is_ch =/= predict_taken))
    PM(conf, clock, EVENT_BPU_TARGET_MISPRED, 1.U, mis_predict && d_alu_bits.bp_valid && target_mispredict)
    PM(conf, clock, EVENT_BPU_UNPREDICTED, 1.U, mis_predict && !d_alu_bits.bp_valid)
  }

  ifu.io.imem <> icache.io.in
  icache.io.out <> io.imem
  // 4f：fencei 改由 ROB head commit 驱动；IDU 不再直连 icache
  idu.io.ifu_signals.ready := true.B
  idu.io.csr <> csr.io.read
  idu1.io.ifu_signals.ready := true.B
  idu1.io.csr <> csr.io.read1
  // 4b：CSR 写口改由 commit 驱动；WBU.csr 闲置（wen=0）

  // ---------- 10a：StoreBuffer + dmem arbitration ----------
  val s_CM_IDLE :: s_CM_W :: s_CM_B :: Nil = Enum(3)
  val cm_st_state = RegInit(s_CM_IDLE)
  val cm_st_addr  = RegInit(0.U(32.W))
  val cm_st_wdata = RegInit(0.U(32.W))
  val cm_st_wstrb = RegInit(0.U(4.W))
  val cm_st_awsize = RegInit(0.U(3.W))
  val cm_st_aw_done = RegInit(false.B)
  val cm_st_w_done  = RegInit(false.B)

  val head_wb_store = head_wb_valid && cm_bits.mem_valid && cm_bits.mem_write
  val head_addr_rdy = cm_bits.addr_ready || head_wb_store
  val head_st_addr  = Mux(head_wb_store, head_wb_bits.alu_result, cm_bits.mem_addr)
  val head_st_data  = Mux(head_wb_store, head_wb_bits.store_data, cm_bits.mem_wdata)
  // Normal memory stores enter StoreBuffer at commit. MMIO stays direct and waits B.
  val head_store_pending = rob.io.commit_valid && !is_irq_early && !ext_irq_fire &&
    cm_bits.mem_valid && cm_bits.mem_write && head_addr_rdy && !cm_is_fencei
  val head_st_is_pmem = (head_st_addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)
  val head_store_direct = head_store_pending && !head_st_is_pmem

  val cm_st_off = head_st_addr(1, 0)
  val cm_st_mask4 = cm_bits.mem_wmask(3, 0)
  val cm_st_size = MuxLookup(cm_st_mask4, 2.U(3.W))(Seq(
    WBYTE(3, 0) -> 0.U(3.W),
    WHALF(3, 0) -> 1.U(3.W),
    WWORD(3, 0) -> 2.U(3.W)
  ))

  // Direct path is only for non-bufferable stores. It starts before commit and
  // the ROB entry retires only when B returns, preserving old MMIO semantics.
  when(is_irq) {
    cm_st_state := s_CM_IDLE
    cm_st_aw_done := false.B
    cm_st_w_done := false.B
  }.elsewhen(cm_st_state === s_CM_IDLE) {
    when(head_store_direct && stbuf.io.empty && !stbuf.io.busy && !lsu.io.bus_busy) {
      cm_st_addr   := head_st_addr
      cm_st_wdata  := head_st_data << (cm_st_off << 3)
      cm_st_wstrb  := (cm_st_mask4 << cm_st_off)(3, 0)
      cm_st_awsize := cm_st_size
      cm_st_aw_done := false.B
      cm_st_w_done  := false.B
      cm_st_state  := s_CM_W
    }
  }.elsewhen(cm_st_state === s_CM_W) {
    val aw_fire = io.dmem.awvalid && io.dmem.awready
    val w_fire  = io.dmem.wvalid && io.dmem.wready
    when(aw_fire) { cm_st_aw_done := true.B }
    when(w_fire)  { cm_st_w_done  := true.B }
    when((cm_st_aw_done || aw_fire) && (cm_st_w_done || w_fire)) {
      cm_st_state := s_CM_B
    }
  }.elsewhen(cm_st_state === s_CM_B) {
    when(io.dmem.bvalid && io.dmem.bready) {
      cm_st_state := s_CM_IDLE
      cm_st_aw_done := false.B
      cm_st_w_done := false.B
    }
  }

  val direct_store_ready = (cm_st_state === s_CM_B) && io.dmem.bvalid && io.dmem.bready

  stbuf.io.enq.valid := cm_fire && cm_is_store && head_st_is_pmem
  stbuf.io.enq.bits.addr := head_st_addr
  stbuf.io.enq.bits.data := head_st_data
  stbuf.io.enq.bits.mask := cm_st_mask4

  store_commit_ready := head_addr_rdy && Mux(head_st_is_pmem, stbuf.io.enq.ready, direct_store_ready)

  // 4f：head 是 fencei 且可提交时刷 ICache；Irrevocable 保持 valid 直到 ready
  // fencei/mret/ebreak/exception drain committed stores before changing control state.
  val cm_writing = cm_st_state =/= s_CM_IDLE
  stbuf.io.bus_busy := lsu.io.bus_busy || cm_writing
  val sb_writing = stbuf.io.busy
  val store_bus_busy = cm_writing || sb_writing
  store_side_empty := stbuf.io.empty && !stbuf.io.busy && !cm_writing
  cm_writing_gap := store_bus_busy || !stbuf.io.empty
  val head_fencei_pending = rob.io.commit_valid && !is_irq_early && !ext_irq_fire &&
    cm_is_fencei && store_side_empty
  icache.io.fencei.valid := head_fencei_pending
  icache.io.fencei.bits.is_fencei := true.B
  fencei_commit_ready := store_side_empty && icache.io.fencei.ready

  // dmem：read <- LSU; write <- direct MMIO path or StoreBuffer.
  lsu.io.dmem <> dcache.io.cpu
  dcache.io.invalidate_valid := stbuf.io.enq.fire
  dcache.io.invalidate_addr  := stbuf.io.enq.bits.addr
  dcache.io.invalidate2_valid := stbuf.io.deq_valid
  dcache.io.invalidate2_addr  := stbuf.io.deq_addr

  dcache.io.mem.arready := io.dmem.arready && !store_bus_busy
  dcache.io.mem.rdata   := io.dmem.rdata
  dcache.io.mem.rresp   := io.dmem.rresp
  dcache.io.mem.rvalid  := io.dmem.rvalid && !store_bus_busy
  dcache.io.mem.rlast   := io.dmem.rlast
  dcache.io.mem.rid     := io.dmem.rid
  dcache.io.mem.awready := false.B
  dcache.io.mem.wready  := false.B
  dcache.io.mem.bvalid  := false.B
  dcache.io.mem.bresp   := 0.U
  dcache.io.mem.bid     := 0.U

  stbuf.io.dmem.arready := false.B
  stbuf.io.dmem.rdata   := 0.U
  stbuf.io.dmem.rresp   := 0.U
  stbuf.io.dmem.rvalid  := false.B
  stbuf.io.dmem.rlast   := false.B
  stbuf.io.dmem.rid     := 0.U
  stbuf.io.dmem.awready := io.dmem.awready && !cm_writing
  stbuf.io.dmem.wready  := io.dmem.wready && !cm_writing
  stbuf.io.dmem.bvalid  := io.dmem.bvalid && !cm_writing
  stbuf.io.dmem.bresp   := io.dmem.bresp
  stbuf.io.dmem.bid     := io.dmem.bid

  io.dmem.araddr  := dcache.io.mem.araddr
  io.dmem.arvalid := dcache.io.mem.arvalid && !store_bus_busy
  io.dmem.arid    := dcache.io.mem.arid
  io.dmem.arlen   := dcache.io.mem.arlen
  io.dmem.arsize  := dcache.io.mem.arsize
  io.dmem.arburst := dcache.io.mem.arburst
  io.dmem.rready  := dcache.io.mem.rready && !store_bus_busy

  io.dmem.awaddr  := Mux(cm_writing, cm_st_addr, stbuf.io.dmem.awaddr)
  io.dmem.awvalid := Mux(cm_writing, (cm_st_state === s_CM_W) && !cm_st_aw_done, stbuf.io.dmem.awvalid)
  io.dmem.awid    := Mux(cm_writing, 0.U, stbuf.io.dmem.awid)
  io.dmem.awlen   := Mux(cm_writing, 0.U, stbuf.io.dmem.awlen)
  io.dmem.awsize  := Mux(cm_writing, cm_st_awsize, stbuf.io.dmem.awsize)
  io.dmem.awburst := Mux(cm_writing, 1.U, stbuf.io.dmem.awburst)
  io.dmem.wdata   := Mux(cm_writing, cm_st_wdata, stbuf.io.dmem.wdata)
  io.dmem.wstrb   := Mux(cm_writing, cm_st_wstrb, stbuf.io.dmem.wstrb)
  io.dmem.wvalid  := Mux(cm_writing, (cm_st_state === s_CM_W) && !cm_st_w_done, stbuf.io.dmem.wvalid)
  io.dmem.wlast   := Mux(cm_writing, true.B, stbuf.io.dmem.wlast)
  io.dmem.bready  := Mux(cm_writing, cm_st_state === s_CM_B, stbuf.io.dmem.bready)

  if (conf.statistics) {
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, io.dmem.awvalid && io.dmem.awready)
    PM(conf, clock, EVENT_STORE_BUFFER_ENQ, 1.U, stbuf.io.enq.fire)
    PM(conf, clock, EVENT_STORE_BUFFER_DRAIN, 1.U,
      !cm_writing && stbuf.io.dmem.bvalid && stbuf.io.dmem.bready)
    PM(conf, clock, EVENT_STORE_BUFFER_FULL, 1.U,
      rob.io.commit_valid && cm_is_store && head_st_is_pmem && !stbuf.io.enq.ready)
    PM(conf, clock, EVENT_STORE_BUFFER_FORWARD, 1.U,
      lsu.io.in.valid && !sq.io.wait_load && !sq.io.fwd_valid && stbuf.io.ld_fwd_valid)
  }

  // 4d：ebreak 仅在 commit 触发（DPIC / 非 DPIC）
  val ebreak_cm = Module(new Ebreak)
  ebreak_cm.io.is_ebreak := cm_fire && cm_bits.is_ebreak
  if (!conf.useDPIC) {
    io.ebreak.get := cm_fire && cm_bits.is_ebreak
  }

  io.commit_valid    := cm_fire
  io.commit_pc       := cm_bits.pc
  io.commit_mem_addr := Mux(cm_is_store, head_st_addr,
    Mux(cm_wb_same, head_wb_bits.alu_result, cm_bits.mem_addr))
  io.commit_is_load  := cm_bits.reg_write_sel === MEM_SEL
  io.commit_valid1    := cm1_fire
  io.commit_pc1       := cm1_bits.pc
  io.commit_mem_addr1 := Mux(cm1_wb_same, cm1_wb_bits.alu_result, cm1_bits.mem_addr)
  io.commit_is_load1  := cm1_bits.reg_write_sel === MEM_SEL
  for (i <- 0 until 32) {
    io.arch_rdata(i) := Mux(i.U === 0.U, 0.U, arch_rf(i.U))
  }
  prf.io.arch_raddr := rename.io.arch_rat_out
  dontTouch(io.commit_valid)
  dontTouch(io.commit_pc)
  dontTouch(io.commit_valid1)
  dontTouch(io.commit_pc1)
  dontTouch(io.arch_rdata)
  dontTouch(rob.io.count)
  dontTouch(rs.io.count)
  dontTouch(fq.io.count)
}
