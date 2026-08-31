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
import core.PcPerfEvents._

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
  val commit_valid2    = Output(Bool())
  val commit_pc2       = Output(UInt(32.W))
  val commit_mem_addr2 = Output(UInt(32.W))
  val commit_is_load2  = Output(Bool())
  val commit_valid3    = Output(Bool())
  val commit_pc3       = Output(UInt(32.W))
  val commit_mem_addr3 = Output(UInt(32.W))
  val commit_is_load3  = Output(Bool())
  val arch_rdata      = Output(Vec(32, UInt(32.W)))
  val debug_lq_head_alloc_pc = Output(UInt(32.W))
  val debug_lq_head_remove_reason = Output(UInt(2.W))
  val debug_wb_head_reject_flags = Output(UInt(4.W))
  val debug_rob_head_pc = Output(UInt(32.W))
  val debug_rob_head_valid = Output(Bool())
  val debug_rob_head_done = Output(Bool())
  val debug_rob_head_mem = Output(Bool())
  val debug_rob_head_ctrl = Output(Bool())
  val debug_rob_count = Output(UInt(log2Ceil(OoOParams.ROB_SIZE + 1).W))
  val debug_rs_count = Output(UInt(log2Ceil(OoOParams.WIDE_RS_SIZE + 1).W))
  val debug_brq_count = Output(UInt(log2Ceil(OoOParams.BRQ_SIZE + 1).W))
  val debug_fq_count = Output(UInt(log2Ceil(OoOParams.FQ_SIZE + 1).W))
  val debug_brq_issue_valid = Output(Bool())
  val debug_bru_dispatch_valid = Output(Bool())
}

class State extends Bundle {
  val state     = Bool()
  val state_num = UInt(8.W)
}

/**
 * Stage12 reference core: 2-wide rename/dispatch/commit with distributed
 * ALU0/ALU1/LSU/DIV/BRU issue, two data CDBs, and precise ROB retirement.
 * Stores become architectural only at commit; LQ/SQ/StoreBuffer preserve
 * memory identity, forwarding, replay, and ordered visibility.
 */
class Core(val conf: CoreConfig) extends Module {
  val io = IO(new Core_IO(conf))

  val ifu    = Module(new IFU(conf))
  val idu    = Module(new IDU(conf))
  val idu1   = Module(new IDU(conf))
  val idu2   = Module(new IDU(conf))
  val idu3   = Module(new IDU(conf))
  val exu    = Module(new EXU(conf))
  val exu_alu1 = Module(new EXU(conf))
  val exu_alu2 = Module(new EXU(conf))
  val exu_alu3 = Module(new EXU(conf))
  val exu_bru = Module(new EXU(conf))
  val exu_div = Module(new EXU(conf))
  val exu_lsu = Module(new EXU(conf))
  val exu_lsu1 = Module(new EXU(conf))
  val lsu    = Module(new LSU(conf))
  val wbu    = Module(new WBU(conf))
  val wbu1   = Module(new WBU(conf))
  val wbu2   = Module(new WBU(conf))
  val wbu3   = Module(new WBU(conf))
  val csr    = Module(new CSR(conf))
  val icache = Module(new ICache(
    set = OoOParams.ICACHE_SET,
    way = 4,
    block_size = 32,
    conf = conf))
  val dcache = Module(new DCache(
    set = OoOParams.DCACHE_SET,
    blockSize = OoOParams.DCACHE_BLOCK_SIZE,
    conf = conf))
  val prf    = Module(new WidePRFCompat(conf))
  val busy   = Module(new WideBusyTableCompat())
  val rename = Module(new WideRenameCompat())
  val rob    = Module(new WideROBCompat())
  val rs     = Module(new WideRSCompat())
  val brq    = Module(new BranchIssueQueue())
  val fq     = Module(new FetchQueue())
  val sq     = Module(new StoreQueue())
  val storeAddrSidecar = Module(new StoreAddressSidecar())
  val stbuf = Module(new WriteCombiningStoreBuffer(
    lines = OoOParams.STORE_BUFFER_LINES,
    retentionCycles = OoOParams.STORE_BUFFER_RETENTION_CYCLES))

  val alu_wb = Wire(new LSU_WBU_IO)
  val alu1_wb = Wire(new LSU_WBU_IO)
  val alu2_wb = Wire(new LSU_WBU_IO)
  val alu3_wb = Wire(new LSU_WBU_IO)
  val bru_wb = Wire(new LSU_WBU_IO)
  val div_wb = Wire(new LSU_WBU_IO)
  val lsu_wb = Wire(new LSU_WBU_IO)
  val lsu1_wb = Wire(new LSU_WBU_IO)
  val lsu_store_wb = Wire(new LSU_WBU_IO)
  val alu_wb_valid = Wire(Bool())
  val alu1_wb_valid = Wire(Bool())
  val alu2_wb_valid = Wire(Bool())
  val alu3_wb_valid = Wire(Bool())
  val bru_wb_valid = Wire(Bool())
  val div_wb_valid = Wire(Bool())
  val lsu_wb_valid = Wire(Bool())
  val lsu1_wb_valid = Wire(Bool())
  val lsu_store_wb_valid = Wire(Bool())
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
  ifu.io.slot1_enable := OoOParams.WIDE_FETCH_ENABLE.B &&
    fq.io.enqSpace >= 2.U
  ifu.io.slot2_enable := OoOParams.WIDE_FETCH_ENABLE.B &&
    fq.io.enqSpace >= 3.U
  ifu.io.slot3_enable := OoOParams.WIDE_FETCH_ENABLE.B &&
    fq.io.enqSpace >= 4.U
  ifu.io.fetch_queue_count := fq.io.count
  ifu.io.fetch_buffer_replace_ready := fq.io.space >= OoOParams.FETCH_WIDTH.U
  // flush 时必须装入 correct_pc（即使上一拍 in.valid=0），否则 irq/mispred 后取指饿死
  val ifu_in_en = ifu.io.is_flush || (ifu.io.in.valid && ifu.io.pc.ready)
  ifu.io.in.bits  := RegEnable(ifu.io.pc.bits, reset_pc.asTypeOf(new IFU_PC_IO), ifu_in_en)
  ifu.io.in.valid := Mux(ifu.io.is_flush, true.B,
    RegEnable(ifu.io.pc.valid, false.B, ifu.io.in.ready))
  val is_bp_flush   = Wire(Bool())
  val is_mem_flush  = Wire(Bool())
  val mis_predict_w = Wire(Bool())
  val mis_rob_w     = Wire(UInt(OoOParams.ROB_PTR_W.W))
  // A memory-order violation at ROB head has no older live instruction to
  // preserve.  The normal flush_idx=violation-1 encoding wraps behind head,
  // so use the existing flush_all paths for this boundary case.
  val mem_flush_all = Wire(Bool())
  val mem_violation_w = Wire(Bool())
  val mem_violation_rob_w = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val mem_violation_pc_w = Wire(UInt(32.W))
  val lsu_mmio_ready = Wire(Bool())
  val is_irq_w      = Wire(Bool())
  val irq_commit    = Wire(Bool())
  val ext_irq_fire  = Wire(Bool()) // 4g：提交间隙采样
  val ext_irq_flush = Wire(Bool()) // 4g：下一拍结构冲刷
  val can_wb        = Wire(Bool())
  val can_wb1       = Wire(Bool())
  val can_wb2       = Wire(Bool())
  val can_wb3       = Wire(Bool())
  val wb_idx        = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb1_idx       = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb2_idx       = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb3_idx       = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val wb_pdest      = Wire(UInt(OoOParams.PHYS_W.W))
  val wb1_pdest     = Wire(UInt(OoOParams.PHYS_W.W))
  val wb2_pdest     = Wire(UInt(OoOParams.PHYS_W.W))
  val wb3_pdest     = Wire(UInt(OoOParams.PHYS_W.W))
  val wb_val        = Wire(UInt(32.W))
  val wb1_val       = Wire(UInt(32.W))
  val wb2_val       = Wire(UInt(32.W))
  val wb3_val       = Wire(UInt(32.W))
  val flush_now     = Wire(Bool())
  val flush_idx     = Wire(UInt(OoOParams.ROB_PTR_W.W))
  val stop_issue    = Wire(Bool())
  val bp_commit_block = Wire(Bool())
  val bp_commit1_block = Wire(Bool())
  val bp_commit2_block = Wire(Bool())
  val bp_commit3_block = Wire(Bool())
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
  val id2_rs1 = idu2.io.in.bits.inst(19, 15)
  val id2_rs2 = idu2.io.in.bits.inst(24, 20)
  val id2_rd  = idu2.io.out.bits.waddr
  val id3_rs1 = idu3.io.in.bits.inst(19, 15)
  val id3_rs2 = idu3.io.in.bits.inst(24, 20)
  val id3_rd  = idu3.io.out.bits.waddr

  val id_doren = idu.io.out.bits.signals.wbu.reg_write && (idu.io.out.bits.waddr =/= 0.U)
  val id1_doren = idu1.io.out.bits.signals.wbu.reg_write && (idu1.io.out.bits.waddr =/= 0.U)
  val id2_doren = idu2.io.out.bits.signals.wbu.reg_write && (idu2.io.out.bits.waddr =/= 0.U)
  val id3_doren = idu3.io.out.bits.signals.wbu.reg_write && (idu3.io.out.bits.waddr =/= 0.U)
  val id0_jump = idu.io.out.bits.signals.exu.jump
  val id1_jump = idu1.io.out.bits.signals.exu.jump
  val id2_jump = idu2.io.out.bits.signals.exu.jump
  val id3_jump = idu3.io.out.bits.signals.exu.jump
  val id0_is_ctrl = (id0_jump =/= JUMP_NONE) && (id0_jump =/= JUMP_MERT)
  val id1_is_ctrl = (id1_jump =/= JUMP_NONE) && (id1_jump =/= JUMP_MERT)
  val id2_is_ctrl = (id2_jump =/= JUMP_NONE) && (id2_jump =/= JUMP_MERT)
  val id3_is_ctrl = (id3_jump =/= JUMP_NONE) && (id3_jump =/= JUMP_MERT)
  val id0_is_cond_branch = id0_jump === JUMP_BEQ || id0_jump === JUMP_BNE ||
    id0_jump === JUMP_BLT || id0_jump === JUMP_BGE ||
    id0_jump === JUMP_BLTU || id0_jump === JUMP_BGEU
  val id_is_csr = idu.io.out.bits.signals.wbu.csr_write
  val id1_is_csr = idu1.io.out.bits.signals.wbu.csr_write
  val id2_is_csr = idu2.io.out.bits.signals.wbu.csr_write
  val id3_is_csr = idu3.io.out.bits.signals.wbu.csr_write
  val id0_special = id_is_csr || idu.io.is_fencei || (id0_jump === JUMP_MERT) ||
    idu.io.out.bits.is_ebreak || idu.io.out.bits.state.state
  val id1_special = id1_is_csr || idu1.io.is_fencei || (id1_jump === JUMP_MERT) ||
    idu1.io.out.bits.is_ebreak || idu1.io.out.bits.state.state
  val id2_special = id2_is_csr || idu2.io.is_fencei || (id2_jump === JUMP_MERT) ||
    idu2.io.out.bits.is_ebreak || idu2.io.out.bits.state.state
  val id3_special = id3_is_csr || idu3.io.is_fencei || (id3_jump === JUMP_MERT) ||
    idu3.io.out.bits.is_ebreak || idu3.io.out.bits.state.state
  val id0_is_mem = idu.io.out.bits.signals.lsu.mem_valid
  val id1_is_mem = idu1.io.out.bits.signals.lsu.mem_valid
  val id2_is_mem = idu2.io.out.bits.signals.lsu.mem_valid
  val id3_is_mem = idu3.io.out.bits.signals.lsu.mem_valid
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
  rename.io.rs1_2 := id2_rs1
  rename.io.rs2_2 := id2_rs2
  rename.io.rd2 := id2_rd
  rename.io.reg_write2 := idu2.io.out.bits.signals.wbu.reg_write
  rename.io.is_branch2 := id2_is_ctrl
  rename.io.cp_rob_idx2 := rob.io.enq2_idx
  rename.io.rs1_3 := id3_rs1
  rename.io.rs2_3 := id3_rs2
  rename.io.rd3 := id3_rd
  rename.io.reg_write3 := idu3.io.out.bits.signals.wbu.reg_write
  rename.io.is_branch3 := id3_is_ctrl
  rename.io.cp_rob_idx3 := rob.io.enq3_idx
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
  val id2_pdest   = rename.io.dest_phys2
  val id2_oldphys = rename.io.old_phys2
  val id2_psrc1   = rename.io.rs1_phys2
  val id2_psrc2   = rename.io.rs2_phys2
  val id3_pdest   = rename.io.dest_phys3
  val id3_oldphys = rename.io.old_phys3
  val id3_psrc1   = rename.io.rs1_phys3
  val id3_psrc2   = rename.io.rs2_phys3

  prf.io.raddr1 := id_psrc1
  prf.io.raddr2 := id_psrc2
  prf.io.raddr3 := id1_psrc1
  prf.io.raddr4 := id1_psrc2
  prf.io.raddr5 := id2_psrc1
  prf.io.raddr6 := id2_psrc2
  prf.io.raddr7 := id3_psrc1
  prf.io.raddr8 := id3_psrc2
  idu.io.refile.rdata1 := prf.io.rdata1
  idu.io.refile.rdata2 := prf.io.rdata2
  idu1.io.refile.rdata1 := prf.io.rdata3
  idu1.io.refile.rdata2 := prf.io.rdata4
  idu2.io.refile.rdata1 := prf.io.rdata5
  idu2.io.refile.rdata2 := prf.io.rdata6
  idu3.io.refile.rdata1 := prf.io.rdata7
  idu3.io.refile.rdata2 := prf.io.rdata8

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
  // LQ decouples allocation from completion, so the input and output may name
  // different dynamic loads.  Every completion bypass comparison must use the
  // output identity together with the output value.
  val lsu_wen = lsu.io.out.bits.signals.wbu.reg_write && lsu_res_ok &&
    (lsu.io.out.bits.pdest =/= 0.U)
  val wbu_wen = can_wb && wbu.io.in.bits.signals.wbu.reg_write && (wbu.io.in.bits.pdest =/= 0.U)
  val wbu1_wen = can_wb1 && wbu1.io.in.bits.signals.wbu.reg_write && (wbu1.io.in.bits.pdest =/= 0.U)
  val wbu2_wen = can_wb2 && wbu2.io.in.bits.signals.wbu.reg_write && (wbu2.io.in.bits.pdest =/= 0.U)
  val wbu3_wen = can_wb3 && wbu3.io.in.bits.signals.wbu.reg_write && (wbu3.io.in.bits.pdest =/= 0.U)
  val exu_is_load = exu.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val div_is_load = exu_div.io.in.bits.signals.wbu.reg_write_sel === MEM_SEL
  val lsu_is_load = lsu.io.out.bits.signals.wbu.reg_write_sel === MEM_SEL
  val exu_wdata = DataForward(exu.io.in.bits.signals.wbu, exu.io.out.bits.alu_result, exu.io.out.bits.imm_ext, exu.io.out.bits.pc + 4.U, exu.io.out.bits.csr_rd1, 0.U)
  val div_wdata = DataForward(exu_div.io.in.bits.signals.wbu, exu_div.io.out.bits.alu_result, exu_div.io.out.bits.imm_ext, exu_div.io.out.bits.pc + 4.U, exu_div.io.out.bits.csr_rd1, 0.U)
  val lsu_wdata = DataForward(lsu.io.out.bits.signals.wbu, lsu.io.out.bits.alu_result, lsu.io.out.bits.imm_ext, lsu.io.out.bits.pc + 4.U, lsu.io.out.bits.csr_rd1, lsu.io.out.bits.mem_read)
  val wbu_wdata = wbu.io.refile.wdata
  val wbu1_wdata = wbu1.io.refile.wdata
  val wbu2_wdata = wbu2.io.refile.wdata
  val wbu3_wdata = wbu3.io.refile.wdata

  // mispred 窗口内禁止从流水级前递（防 wrong-path 值入队）
  val fwd_ok = Wire(Bool())
  def PhysForward(psrc: UInt, ren: Bool, fallback: UInt): UInt = {
    val cdb_hit = fwd_ok && can_wb && ren && psrc =/= 0.U && psrc === wb_pdest
    val cdb1_hit = fwd_ok && can_wb1 && ren && psrc =/= 0.U && psrc === wb1_pdest && !cdb_hit
    val cdb2_hit = fwd_ok && can_wb2 && ren && psrc =/= 0.U && psrc === wb2_pdest && !cdb_hit && !cdb1_hit
    val cdb3_hit = fwd_ok && can_wb3 && ren && psrc =/= 0.U && psrc === wb3_pdest &&
      !cdb_hit && !cdb1_hit && !cdb2_hit
    val exu_hit = fwd_ok && exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest && !exu_is_load && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    val div_hit = fwd_ok && div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !div_is_load && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit && !exu_hit
    val lsu_hit = fwd_ok && lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.out.bits.pdest && !exu_hit && !div_hit && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    val wbu_hit = fwd_ok && wbu_wen && ren && psrc =/= 0.U && psrc === wbu.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    val wbu1_hit = fwd_ok && wbu1_wen && ren && psrc =/= 0.U && psrc === wbu1.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !wbu_hit && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    val wbu2_hit = fwd_ok && wbu2_wen && ren && psrc =/= 0.U && psrc === wbu2.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !wbu_hit && !wbu1_hit && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    val wbu3_hit = fwd_ok && wbu3_wen && ren && psrc =/= 0.U && psrc === wbu3.io.in.bits.pdest && !exu_hit && !div_hit && !lsu_hit && !wbu_hit && !wbu1_hit && !wbu2_hit && !cdb_hit && !cdb1_hit && !cdb2_hit && !cdb3_hit
    MuxCase(fallback, Seq(cdb_hit -> wb_val, cdb1_hit -> wb1_val, cdb2_hit -> wb2_val,
      cdb3_hit -> wb3_val,
      exu_hit -> exu_wdata, div_hit -> div_wdata, lsu_hit -> lsu_wdata,
      wbu_hit -> wbu_wdata, wbu1_hit -> wbu1_wdata, wbu2_hit -> wbu2_wdata,
      wbu3_hit -> wbu3_wdata))
  }
  def PhysBypassable(psrc: UInt, ren: Bool): Bool = {
    val cdb_hit = fwd_ok && can_wb && ren && psrc =/= 0.U && psrc === wb_pdest
    val cdb1_hit = fwd_ok && can_wb1 && ren && psrc =/= 0.U && psrc === wb1_pdest
    val cdb2_hit = fwd_ok && can_wb2 && ren && psrc =/= 0.U && psrc === wb2_pdest
    val cdb3_hit = fwd_ok && can_wb3 && ren && psrc =/= 0.U && psrc === wb3_pdest
    val exu_hit = fwd_ok && exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest && !exu_is_load
    val div_hit = fwd_ok && div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !div_is_load
    val lsu_hit = fwd_ok && lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.out.bits.pdest &&
      (!lsu_is_load || lsu.io.out.valid)
    val wbu_hit = fwd_ok && wbu_wen && ren && psrc =/= 0.U && psrc === wbu.io.in.bits.pdest
    val wbu1_hit = fwd_ok && wbu1_wen && ren && psrc =/= 0.U && psrc === wbu1.io.in.bits.pdest
    val wbu2_hit = fwd_ok && wbu2_wen && ren && psrc =/= 0.U && psrc === wbu2.io.in.bits.pdest
    val wbu3_hit = fwd_ok && wbu3_wen && ren && psrc =/= 0.U && psrc === wbu3.io.in.bits.pdest
    cdb_hit || cdb1_hit || cdb2_hit || cdb3_hit || exu_hit || div_hit || lsu_hit ||
      wbu_hit || wbu1_hit || wbu2_hit || wbu3_hit
  }

  busy.io.raddr1 := id_psrc1
  busy.io.raddr2 := id_psrc2
  busy.io.raddr3 := id1_psrc1
  busy.io.raddr4 := id1_psrc2
  busy.io.raddr5 := id2_psrc1
  busy.io.raddr6 := id2_psrc2
  busy.io.raddr7 := id3_psrc1
  busy.io.raddr8 := id3_psrc2

  // 3d：未 ready 可 enq；load 结果未到仍 stall（无可抓值）
  def PhysLoadStall(psrc: UInt, ren: Bool): Bool = {
    val exu_hit = exu_wen && ren && psrc =/= 0.U && psrc === exu.io.in.bits.pdest
    val div_hit = div_wen && ren && psrc =/= 0.U && psrc === exu_div.io.in.bits.pdest && !exu_hit
    val lsu_hit = lsu_wen && ren && psrc =/= 0.U && psrc === lsu.io.out.bits.pdest && !exu_hit && !div_hit
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
  val id2_dep1 = id1_doren && idu2.io.rs1_ren && (id2_psrc1 === id1_pdest)
  val id2_dep0 = id_doren && idu2.io.rs1_ren && (id2_psrc1 === id_pdest)
  val id2_dep2_1 = id1_doren && idu2.io.rs2_ren && (id2_psrc2 === id1_pdest)
  val id2_dep2_0 = id_doren && idu2.io.rs2_ren && (id2_psrc2 === id_pdest)
  val id2_src1 = Mux(id2_dep1 || id2_dep0, 0.U,
    PhysForward(id2_psrc1, idu2.io.rs1_ren, prf.io.rdata5))
  val id2_src2 = Mux(id2_dep2_1 || id2_dep2_0, 0.U,
    PhysForward(id2_psrc2, idu2.io.rs2_ren, prf.io.rdata6))
  val id2_s1_rdy = !idu2.io.rs1_ren || (!(id2_dep1 || id2_dep0) &&
    (busy.io.ready5 || PhysBypassable(id2_psrc1, idu2.io.rs1_ren)))
  val id2_s2_rdy = !idu2.io.rs2_ren || (!(id2_dep2_1 || id2_dep2_0) &&
    (busy.io.ready6 || PhysBypassable(id2_psrc2, idu2.io.rs2_ren)))
  val id3_dep1 = (id2_doren && id3_psrc1 === id2_pdest) ||
    (id1_doren && id3_psrc1 === id1_pdest) || (id_doren && id3_psrc1 === id_pdest)
  val id3_dep2 = (id2_doren && id3_psrc2 === id2_pdest) ||
    (id1_doren && id3_psrc2 === id1_pdest) || (id_doren && id3_psrc2 === id_pdest)
  val id3_src1 = Mux(idu3.io.rs1_ren && id3_dep1, 0.U,
    PhysForward(id3_psrc1, idu3.io.rs1_ren, prf.io.rdata7))
  val id3_src2 = Mux(idu3.io.rs2_ren && id3_dep2, 0.U,
    PhysForward(id3_psrc2, idu3.io.rs2_ren, prf.io.rdata8))
  val id3_s1_rdy = !idu3.io.rs1_ren || (!id3_dep1 &&
    (busy.io.ready7 || PhysBypassable(id3_psrc1, idu3.io.rs1_ren)))
  val id3_s2_rdy = !idu3.io.rs2_ren || (!id3_dep2 &&
    (busy.io.ready8 || PhysBypassable(id3_psrc2, idu3.io.rs2_ren)))

  // 4b：在飞 CSR 未提交前禁止再入队；CSR 源必须就绪（rs1_val 只在 enq 抓一次）
  val csr_inflight = VecInit(rob.io.entries.map(e => e.valid && e.csr_write)).asUInt.orR
  val lane0_csr_stall = id_is_csr && csr_inflight
  val lane0_free_ok = !id_doren || (rename.io.free_cnt =/= 0.U)
  val lane0_cp_ok = !id0_is_ctrl || !rename.io.cp_full
  val lane0_issue_space = Mux(id0_is_ctrl, brq.io.enq.ready, rs.io.space =/= 0.U)
  val lane0_space_ok = (rob.io.space =/= 0.U) && lane0_issue_space
  val lane0_block = stop_issue || lane0_csr_stall || !lane0_free_ok || !lane0_cp_ok || !lane0_space_ok

  // Dispatch is a valid prefix.  Special instructions retry at lane0.  A
  // packet admits one control-flow and up to two memory operations; the
  // distributed RS serializes those memory operations onto the single LSU.
  val lane1_try = fq.io.deq1.valid && !id0_special && !id1_special &&
    !(id0_is_ctrl && id1_is_ctrl)
  val lane1_need_free = id_doren.asUInt +& id1_doren.asUInt
  val lane1_free_ok = !id1_doren || (rename.io.free_cnt >= lane1_need_free)
  val lane1_cp_ok = !id1_is_ctrl || !rename.io.cp_full
  // A dual-dispatch packet contains at most one ordinary control-flow
  // instruction: lane0 control flow only admits a non-control lane1. Account
  // for the integer RS and BRQ credits independently so a
  // lane1 branch does not consume or enter an integer-RS slot.
  val lane1_rs_need = (!id0_is_ctrl).asUInt +& (!id1_is_ctrl).asUInt
  val lane1_needs_brq = id0_is_ctrl || id1_is_ctrl
  val lane1_space_ok = (rob.io.space >= 2.U) && (rs.io.space >= lane1_rs_need) &&
    (!lane1_needs_brq || brq.io.enq.ready)
  val lane1_csr_stall = id1_is_csr && (csr_inflight || !id1_s1_rdy)
  val lane1_block = lane0_block || !lane1_try || stop_issue || lane1_csr_stall || !lane1_free_ok ||
    !lane1_cp_ok || !lane1_space_ok

  val lane2_try = lane1_try && fq.io.deq2.valid && !id2_special &&
    PopCount(Seq(id0_is_ctrl, id1_is_ctrl, id2_is_ctrl)) <= 1.U &&
    PopCount(Seq(id0_is_mem, id1_is_mem, id2_is_mem)) <= 2.U
  val lane2_need_free = PopCount(Seq(id_doren, id1_doren, id2_doren))
  val lane2_rs_need = PopCount(Seq(!id0_is_ctrl, !id1_is_ctrl, !id2_is_ctrl))
  val lane2_needs_brq = id0_is_ctrl || id1_is_ctrl || id2_is_ctrl
  val lane2_space_ok = rob.io.space >= 3.U && rs.io.space >= lane2_rs_need &&
    (!lane2_needs_brq || brq.io.enq.ready)
  val lane2_block = lane1_block || !lane2_try ||
    (id2_doren && rename.io.free_cnt < lane2_need_free) ||
    (id2_is_ctrl && rename.io.cp_full) || !lane2_space_ok

  val lane3_try = lane2_try && fq.io.deq3.valid && !id3_special &&
    PopCount(Seq(id0_is_ctrl, id1_is_ctrl, id2_is_ctrl, id3_is_ctrl)) <= 1.U &&
    PopCount(Seq(id0_is_mem, id1_is_mem, id2_is_mem, id3_is_mem)) <= 2.U
  val lane3_need_free = PopCount(Seq(id_doren, id1_doren, id2_doren, id3_doren))
  val lane3_rs_need = PopCount(Seq(!id0_is_ctrl, !id1_is_ctrl, !id2_is_ctrl, !id3_is_ctrl))
  val lane3_needs_brq = id0_is_ctrl || id1_is_ctrl || id2_is_ctrl || id3_is_ctrl
  val lane3_space_ok = rob.io.space >= 4.U && rs.io.space >= lane3_rs_need &&
    (!lane3_needs_brq || brq.io.enq.ready)
  val lane3_block = lane2_block || !lane3_try ||
    (id3_doren && rename.io.free_cnt < lane3_need_free) ||
    (id3_is_ctrl && rename.io.cp_full) || !lane3_space_ok

  rob_full_stall := idu.io.out.valid && !lane0_space_ok
  fl_stall := idu.io.out.valid && !lane0_free_ok
  rs_full_stall := idu.io.out.valid && !lane0_issue_space

  idu.io.is_stall := lane0_block
  idu.io.out.ready := !lane0_block
  idu1.io.is_stall := lane1_block
  idu1.io.out.ready := !lane1_block
  idu2.io.is_stall := lane2_block
  idu2.io.out.ready := !lane2_block
  idu3.io.is_stall := lane3_block
  idu3.io.out.ready := !lane3_block
  val en_ren = idu.io.out.valid && !lane0_block
  val en_ren1 = en_ren && idu1.io.out.valid && !lane1_block
  val en_ren2 = en_ren1 && idu2.io.out.valid && !lane2_block
  val en_ren3 = en_ren2 && idu3.io.out.valid && !lane3_block
  val lane1CtrlBlocked = idu.io.out.valid && fq.io.deq1.valid &&
    id0_is_ctrl && id1_is_ctrl
  val lane1BackendBlocked = en_ren && idu1.io.out.valid && lane1_try && lane1_block

  rename.io.fire := en_ren
  rename.io.fire0 := en_ren
  rename.io.fire1 := en_ren1
  rename.io.fire2 := en_ren2
  rename.io.fire3 := en_ren3

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
  rob.io.enq_bits.ftq_idx       := idu.io.out.bits.ftq_idx
  rob.io.enq_bits.ftq_generation := idu.io.out.bits.ftq_generation
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
  rob.io.enq1_bits.ftq_idx       := idu1.io.out.bits.ftq_idx
  rob.io.enq1_bits.ftq_generation := idu1.io.out.bits.ftq_generation
  rob.io.enq1_bits.cp_idx        := rename.io.cp_idx1
  rob.io.enq1_bits.actual_taken  := false.B
  rob.io.enq1_bits.actual_target := idu1.io.out.bits.pc + 4.U
  def makeRobEntry(id: IDU_EXU_IO, src1: UInt, src2: UInt,
                   psrc1: UInt, psrc2: UInt, pdest: UInt, oldPhys: UInt,
                   cpIdx: UInt, isFencei: Bool): ROBEntry = {
    val e = WireDefault(0.U.asTypeOf(new ROBEntry))
    e.pc := id.pc
    e.inst := id.inst
    e.reg_write := id.signals.wbu.reg_write
    e.reg_write_sel := id.signals.wbu.reg_write_sel
    e.csr_write := id.signals.wbu.csr_write
    e.csr_sel := id.signals.wbu.csr_sel
    e.csr_waddr := id.csr_waddr
    e.csr_rd1 := id.csr_rd1
    e.rs1_val := src1
    e.rs2_val := src2
    e.state := id.state
    e.arch_rd := id.waddr
    e.new_phys := pdest
    e.old_phys := oldPhys
    e.src1_phys := psrc1
    e.src2_phys := psrc2
    e.is_ebreak := id.is_ebreak
    e.is_fencei := isFencei
    e.mem_valid := id.signals.lsu.mem_valid
    e.mem_write := id.signals.lsu.mem_write
    e.mem_rd := id.signals.lsu.mem_rd
    e.mem_wmask := id.signals.lsu.mem_wmask
    e.jump := id.signals.exu.jump
    e.bp_valid := id.bp_valid
    e.bp_taken := id.bp_taken
    e.bp_target := id.bp_target
    e.bp_index := id.bp_index
    e.ftq_idx := id.ftq_idx
    e.ftq_generation := id.ftq_generation
    e.cp_idx := cpIdx
    e.actual_target := id.pc + 4.U
    e
  }
  rob.io.enq2_fire := en_ren2
  rob.io.enq2_bits := makeRobEntry(idu2.io.out.bits, id2_src1, id2_src2,
    id2_psrc1, id2_psrc2, id2_pdest, id2_oldphys, rename.io.cp_idx2, idu2.io.is_fencei)
  rob.io.enq3_fire := en_ren3
  rob.io.enq3_bits := makeRobEntry(idu3.io.out.bits, id3_src1, id3_src2,
    id3_psrc1, id3_psrc2, id3_pdest, id3_oldphys, rename.io.cp_idx3, idu3.io.is_fencei)
  // 用 RS 的 issue 标 issued：ROB 内部 issue_idx 是最老未发，与 OoO 不一致 → 发射时按 rob_idx 写
  // 简化：issue_fire 仍走 ROB 端口但仅当 issue_idx 匹配时；否则 enq 后靠 RS，issued 仅调试
  rob.io.issue_fire := false.B

  // ---------- RS 入队 ----------
  rs.io.rob_head := rob.io.head
  rs.io.rob_st_pending := sq.io.unresolved_mask
  rs.io.enq_fire := en_ren && !id0_is_ctrl
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
  rs.io.enq_bits.ftq_idx    := idu.io.out.bits.ftq_idx
  rs.io.enq_bits.ftq_generation := idu.io.out.bits.ftq_generation
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
  val brqEnqLane0 = en_ren && id0_is_ctrl
  val brqEnqLane1 = en_ren1 && id1_is_ctrl
  val brqEnqLane2 = en_ren2 && id2_is_ctrl
  val brqEnqLane3 = en_ren3 && id3_is_ctrl
  brq.io.enq.valid := brqEnqLane0 || brqEnqLane1 || brqEnqLane2 || brqEnqLane3
  brq.io.enq.bits := MuxCase(rs.io.enq_bits, Seq(
    brqEnqLane0 -> rs.io.enq_bits,
    brqEnqLane1 -> rs.io.enq1_bits,
    brqEnqLane2 -> rs.io.enq2_bits,
    brqEnqLane3 -> rs.io.enq3_bits))
  rs.io.enq1_fire := en_ren1 && !id1_is_ctrl
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
  rs.io.enq1_bits.ftq_idx    := idu1.io.out.bits.ftq_idx
  rs.io.enq1_bits.ftq_generation := idu1.io.out.bits.ftq_generation
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

  def makeRsEntry(id: IDU_EXU_IO, robIdx: UInt, src1Ready: Bool, src2Ready: Bool,
                  psrc1: UInt, psrc2: UInt, src1: UInt, src2: UInt,
                  pdest: UInt, oldPhys: UInt, doRename: Bool,
                  cpIdx: UInt, isFencei: Bool): RSEntry = {
    val e = WireDefault(0.U.asTypeOf(new RSEntry))
    e.rob_idx := robIdx
    e.src1_ready := src1Ready
    e.src2_ready := src2Ready
    e.src1_phys := psrc1
    e.src2_phys := psrc2
    e.src1_val := src1
    e.src2_val := src2
    e.pdest := pdest
    e.old_phys := oldPhys
    e.do_rename := doRename
    e.pc := id.pc
    e.inst := id.inst
    e.imm_ext := id.imm_ext
    e.waddr := id.waddr
    e.is_ebreak := id.is_ebreak
    e.is_fencei := isFencei
    e.csr_rd1 := id.csr_rd1
    e.csr_waddr := id.csr_waddr
    e.state := id.state
    e.bp_valid := id.bp_valid
    e.bp_taken := id.bp_taken
    e.bp_target := id.bp_target
    e.bp_index := id.bp_index
    e.ftq_idx := id.ftq_idx
    e.ftq_generation := id.ftq_generation
    e.cp_idx := cpIdx
    e.exu_alu_srcA := id.signals.exu.alu_srcA
    e.exu_alu_srcB := id.signals.exu.alu_srcB
    e.exu_alu_control := id.signals.exu.alu_control
    e.exu_jump := id.signals.exu.jump
    e.lsu_mem_wmask := id.signals.lsu.mem_wmask
    e.lsu_mem_rd := id.signals.lsu.mem_rd
    e.lsu_mem_write := id.signals.lsu.mem_write
    e.lsu_mem_valid := id.signals.lsu.mem_valid
    e.wbu_reg_write := id.signals.wbu.reg_write
    e.wbu_reg_write_sel := id.signals.wbu.reg_write_sel
    e.wbu_csr_write := id.signals.wbu.csr_write
    e.wbu_csr_sel := id.signals.wbu.csr_sel
    e
  }
  rs.io.enq2_fire := en_ren2 && !id2_is_ctrl
  rs.io.enq2_bits := makeRsEntry(idu2.io.out.bits, rob.io.enq2_idx,
    id2_s1_rdy, id2_s2_rdy, id2_psrc1, id2_psrc2, id2_src1, id2_src2,
    id2_pdest, id2_oldphys, id2_doren, rename.io.cp_idx2, idu2.io.is_fencei)
  rs.io.enq3_fire := en_ren3 && !id3_is_ctrl
  rs.io.enq3_bits := makeRsEntry(idu3.io.out.bits, rob.io.enq3_idx,
    id3_s1_rdy, id3_s2_rdy, id3_psrc1, id3_psrc2, id3_src1, id3_src2,
    id3_pdest, id3_oldphys, id3_doren, rename.io.cp_idx3, idu3.io.is_fencei)

  private def connectWakeupNetwork(): Unit = {
  // CDB → RS 唤醒
  rs.io.cdb_valid := can_wb && (wb_pdest =/= 0.U)
  rs.io.cdb_pdest := wb_pdest
  rs.io.cdb_val   := wb_val
  rs.io.cdb1_valid := can_wb1 && (wb1_pdest =/= 0.U)
  rs.io.cdb1_pdest := wb1_pdest
  rs.io.cdb1_val   := wb1_val
  rs.io.cdb2_valid := can_wb2 && (wb2_pdest =/= 0.U)
  rs.io.cdb2_pdest := wb2_pdest
  rs.io.cdb2_val   := wb2_val
  rs.io.cdb3_valid := can_wb3 && (wb3_pdest =/= 0.U)
  rs.io.cdb3_pdest := wb3_pdest
  rs.io.cdb3_val   := wb3_val
  brq.io.robHead := rob.io.head
  brq.io.cdb0Valid := rs.io.cdb_valid
  brq.io.cdb0Pdest := rs.io.cdb_pdest
  brq.io.cdb0Value := rs.io.cdb_val
  brq.io.cdb1Valid := rs.io.cdb1_valid
  brq.io.cdb1Pdest := rs.io.cdb1_pdest
  brq.io.cdb1Value := rs.io.cdb1_val
  brq.io.cdb2Valid := rs.io.cdb2_valid
  brq.io.cdb2Pdest := rs.io.cdb2_pdest
  brq.io.cdb2Value := rs.io.cdb2_val
  brq.io.cdb3Valid := rs.io.cdb3_valid
  brq.io.cdb3Pdest := rs.io.cdb3_pdest
  brq.io.cdb3Value := rs.io.cdb3_val
  }
  connectWakeupNetwork()

  // ---------- 派遣寄存器 RS → EX（可 selective flush，无 StageConnect 组合环）----------
  // ---------- Dispatch registers RS -> EX / LSU ----------
  val d_alu_valid = RegInit(false.B)
  val d_alu_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_alu1_valid = RegInit(false.B)
  val d_alu1_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_alu2_valid = RegInit(false.B)
  val d_alu2_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_alu3_valid = RegInit(false.B)
  val d_alu3_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_bru_valid = RegInit(false.B)
  val d_bru_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_bru_ctrl_resolved = RegInit(false.B)
  val d_div_valid = RegInit(false.B)
  val d_div_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_lsu_valid = RegInit(false.B)
  val d_lsu_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  val d_lsu1_valid = RegInit(false.B)
  val d_lsu1_bits  = RegInit(0.U.asTypeOf(new IDU_EXU_IO))
  d_valid := d_alu_valid || d_alu1_valid || d_alu2_valid || d_alu3_valid ||
    d_bru_valid || d_div_valid || d_lsu_valid || d_lsu1_valid
  d_bits.pc      := MuxCase(d_alu_bits.pc, Seq(
    d_bru_valid -> d_bru_bits.pc,
    d_div_valid -> d_div_bits.pc,
    d_lsu_valid -> d_lsu_bits.pc,
    d_lsu1_valid -> d_lsu1_bits.pc
  ))
  d_bits.rob_idx := MuxCase(d_alu_bits.rob_idx, Seq(
    d_bru_valid -> d_bru_bits.rob_idx,
    d_div_valid -> d_div_bits.rob_idx,
    d_lsu_valid -> d_lsu_bits.rob_idx,
    d_lsu1_valid -> d_lsu1_bits.rob_idx
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
    b.ftq_idx   := e.ftq_idx
    b.ftq_generation := e.ftq_generation
    b.inst      := e.inst
    b.rob_idx   := e.rob_idx
    b.cp_idx    := e.cp_idx
    b.pdest     := e.pdest
    b.old_phys  := e.old_phys
    b.do_rename := e.do_rename
    b
  }

  def laneFlush(valid: Bool, bits: IDU_EXU_IO): Bool =
    valid && (mem_flush_all ||
      (robAge(bits.rob_idx, rob.io.head) > robAge(flush_idx, rob.io.head)))

  val lsu_stage_valid = RegInit(false.B)
  val lsu_stage_bits = RegInit(0.U.asTypeOf(new EXU_LSU_IO))
  val lsu_stage1_valid = RegInit(false.B)
  val lsu_stage1_bits = RegInit(0.U.asTypeOf(new EXU_LSU_IO))
  val flush_lsu_stage = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) &&
    lsu_stage_valid && (robAge(lsu_stage_bits.rob_idx, rob.io.head) > robAge(flush_idx, rob.io.head)))
  val flush_lsu1_stage = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) &&
    lsu_stage1_valid && (robAge(lsu_stage1_bits.rob_idx, rob.io.head) > robAge(flush_idx, rob.io.head)))

  val flush_alu = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_alu_valid, d_alu_bits))
  val flush_alu1 = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_alu1_valid, d_alu1_bits))
  val flush_alu2 = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_alu2_valid, d_alu2_bits))
  val flush_alu3 = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_alu3_valid, d_alu3_bits))
  val flush_bru = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_bru_valid, d_bru_bits))
  val flush_div = is_irq_w || ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_div_valid, d_div_bits))
  val flush_lsu_d = is_irq_w ||
    ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_lsu_valid, d_lsu_bits))
  val flush_lsu1_d = is_irq_w ||
    ((is_bp_flush || is_mem_flush || fencei_flush || mret_flush) && laneFlush(d_lsu1_valid, d_lsu1_bits))

  val hold_alu = d_alu_valid && !flush_alu
  val hold_alu1 = d_alu1_valid && !flush_alu1
  val hold_alu2 = d_alu2_valid && !flush_alu2
  val hold_alu3 = d_alu3_valid && !flush_alu3
  val hold_bru = d_bru_valid && !flush_bru
  val hold_div = d_div_valid && !flush_div
  val hold_lsu = d_lsu_valid && !flush_lsu_d
  val hold_lsu1 = d_lsu1_valid && !flush_lsu1_d

  exu.io.in.valid := hold_alu
  exu.io.in.bits  := d_alu_bits
  exu.io.is_flush := is_irq_w || flush_alu
  exu_alu1.io.in.valid := hold_alu1
  exu_alu1.io.in.bits := d_alu1_bits
  exu_alu1.io.is_flush := is_irq_w || flush_alu1
  exu_alu2.io.in.valid := hold_alu2
  exu_alu2.io.in.bits := d_alu2_bits
  exu_alu2.io.is_flush := is_irq_w || flush_alu2
  exu_alu3.io.in.valid := hold_alu3
  exu_alu3.io.in.bits := d_alu3_bits
  exu_alu3.io.is_flush := is_irq_w || flush_alu3
  exu_bru.io.in.valid := hold_bru
  exu_bru.io.in.bits := d_bru_bits
  exu_bru.io.is_flush := is_irq_w || flush_bru
  exu_div.io.in.valid := hold_div
  exu_div.io.in.bits  := d_div_bits
  exu_div.io.is_flush := is_irq_w || flush_div
  exu_lsu.io.in.valid := hold_lsu
  exu_lsu.io.in.bits  := d_lsu_bits
  exu_lsu.io.is_flush := flush_lsu_d
  exu_lsu1.io.in.valid := hold_lsu1
  exu_lsu1.io.in.bits  := d_lsu1_bits
  exu_lsu1.io.is_flush := flush_lsu1_d

  val lsuStageRob = rob.io.entries(lsu_stage_bits.rob_idx)
  val lsu_stage_stale = lsu_stage_valid &&
    (!lsuStageRob.valid || (lsuStageRob.pc =/= lsu_stage_bits.pc))
  val lsu_stage_drop = flush_lsu_stage || lsu_stage_stale
  val lsu_stage_usable = lsu_stage_valid && !lsu_stage_drop
  val lsu_addr_usable = exu_lsu.io.out.valid && !flush_lsu_d

  // Empty skid slots flow directly into LSU. Backpressure captures the
  // address, and a consumed resident address may be replaced in the same
  // cycle. d_lsu is registered, so this does not feed LSU ready into RS select.
  lsu.io.in.valid := lsu_stage_usable || (!lsu_stage_usable && lsu_addr_usable)
  lsu.io.in.bits := Mux(lsu_stage_usable, lsu_stage_bits, exu_lsu.io.out.bits)
  val lsu_req_accept = lsu.io.in.fire
  val lsu_out_fire = lsu.io.out.valid && lsu.io.out.ready
  exu_lsu.io.out.ready := !flush_lsu_d &&
    (!lsu_stage_usable || (lsu_stage_usable && lsu.io.in.ready))
  val lsu_addr_accept = lsu_addr_usable && exu_lsu.io.out.ready
  val lsu_addr_flow = !lsu_stage_usable && lsu_addr_accept && lsu.io.in.ready
  val lsu_stage_push = lsu_addr_accept && !lsu_addr_flow
  val lsu_stage_pop = lsu_stage_usable && lsu_req_accept

  when(lsu_stage_push) {
    lsu_stage_valid := true.B
    lsu_stage_bits := exu_lsu.io.out.bits
  }.elsewhen(lsu_stage_pop || lsu_stage_drop) {
    lsu_stage_valid := false.B
  }

  when(!reset.asBool && lsu_addr_accept) {
    assert(!flush_lsu_d, "a flushed LSU dispatch entry must not enter LSU or its skid")
  }

  val lsuStage1Rob = rob.io.entries(lsu_stage1_bits.rob_idx)
  val lsu_stage1_stale = lsu_stage1_valid &&
    (!lsuStage1Rob.valid || (lsuStage1Rob.pc =/= lsu_stage1_bits.pc))
  val lsu_stage1_drop = flush_lsu1_stage || lsu_stage1_stale
  val lsu_stage1_usable = lsu_stage1_valid && !lsu_stage1_drop
  val lsu_addr1_usable = exu_lsu1.io.out.valid && !flush_lsu1_d

  lsu.io.in1.valid := lsu_stage1_usable || (!lsu_stage1_usable && lsu_addr1_usable)
  lsu.io.in1.bits := Mux(lsu_stage1_usable, lsu_stage1_bits, exu_lsu1.io.out.bits)
  val lsu1_req_accept = lsu.io.in1.fire
  exu_lsu1.io.out.ready := !flush_lsu1_d &&
    (!lsu_stage1_usable || (lsu_stage1_usable && lsu.io.in1.ready))
  val lsu_addr1_accept = lsu_addr1_usable && exu_lsu1.io.out.ready
  val lsu_addr1_flow = !lsu_stage1_usable && lsu_addr1_accept && lsu.io.in1.ready
  val lsu_stage1_push = lsu_addr1_accept && !lsu_addr1_flow
  val lsu_stage1_pop = lsu_stage1_usable && lsu1_req_accept

  when(lsu_stage1_push) {
    lsu_stage1_valid := true.B
    lsu_stage1_bits := exu_lsu1.io.out.bits
  }.elsewhen(lsu_stage1_pop || lsu_stage1_drop) {
    lsu_stage1_valid := false.B
  }

  when(!reset.asBool && lsu_addr1_accept) {
    assert(!flush_lsu1_d, "a flushed secondary LSU entry must not enter LSU or its skid")
    assert(!exu_lsu1.io.out.bits.signals.lsu.mem_write,
      "the secondary address-generation path is load-only")
  }

  alu_wb := exuToWbu(exu.io.out.bits)
  alu_wb_valid := exu.io.out.valid
  bru_wb := exuToWbu(exu_bru.io.out.bits)
  // Branches without a renamed destination complete through a private ROB
  // sideband, leaving both data CDB lanes available to value-producing FUs.
  val d_bru_ctrl_no_dest = d_bru_valid && !d_bru_bits.do_rename && !d_bru_bits.state.state
  val bru_resolve_valid = d_bru_valid
  val bru_resolve_bits = d_bru_bits
  val bru_resolve_result = exu_bru.io.out.bits
  val bru_resolve_out_valid = exu_bru.io.out.valid
  val bru_ctrl_no_dest = bru_resolve_valid &&
    !bru_resolve_bits.do_rename && !bru_resolve_bits.state.state
  val ctrl_wb_entry = rob.io.entries(bru_resolve_bits.rob_idx)
  val ctrl_direct_fire = bru_resolve_out_valid && bru_ctrl_no_dest &&
    (!d_bru_valid || !flush_bru) &&
    ctrl_wb_entry.valid && !ctrl_wb_entry.done &&
    (ctrl_wb_entry.pc === bru_resolve_bits.pc)
  bru_wb_valid := exu_bru.io.out.valid && !d_bru_ctrl_no_dest
  alu1_wb := exuToWbu(exu_alu1.io.out.bits)
  alu1_wb_valid := exu_alu1.io.out.valid
  alu2_wb := exuToWbu(exu_alu2.io.out.bits)
  alu2_wb_valid := exu_alu2.io.out.valid
  alu3_wb := exuToWbu(exu_alu3.io.out.bits)
  alu3_wb_valid := exu_alu3.io.out.valid
  div_wb := exuToWbu(exu_div.io.out.bits)
  div_wb_valid := exu_div.io.out.valid
  lsu_wb := lsu.io.out.bits
  lsu_wb_valid := lsu.io.out.valid
  lsu1_wb := lsu.io.out1.bits
  lsu1_wb_valid := lsu.io.out1.valid
  lsu_store_wb := lsu.io.storeComplete.bits
  lsu_store_wb_valid := lsu.io.storeComplete.valid

  val alu_leave = hold_alu && exu.io.out.valid && exu.io.out.ready
  val alu1_leave = hold_alu1 && exu_alu1.io.out.valid && exu_alu1.io.out.ready
  val alu2_leave = hold_alu2 && exu_alu2.io.out.valid && exu_alu2.io.out.ready
  val alu3_leave = hold_alu3 && exu_alu3.io.out.valid && exu_alu3.io.out.ready
  val bru_leave = hold_bru && exu_bru.io.out.valid && exu_bru.io.out.ready
  val div_leave = hold_div && exu_div.io.out.valid && exu_div.io.out.ready
  val lsu_addr_leave = hold_lsu && lsu_addr_accept
  val lsu_addr1_leave = hold_lsu1 && lsu_addr1_accept
  rs.io.issue_fire    := false.B
  rs.io.free_rob_fire := can_wb
  rs.io.free_rob_idx  := wb_idx
  rs.io.free_rob1_fire := can_wb1
  rs.io.free_rob1_idx  := wb1_idx
  rs.io.free_rob2_fire := can_wb2
  rs.io.free_rob2_idx  := wb2_idx
  rs.io.free_rob3_fire := can_wb3
  rs.io.free_rob3_idx  := wb3_idx
  rs.io.free_ctrl_fire := ctrl_direct_fire
  rs.io.free_ctrl_idx  := bru_resolve_bits.rob_idx
  brq.io.free0Valid := can_wb
  brq.io.free0Rob := wb_idx
  brq.io.free1Valid := can_wb1
  brq.io.free1Rob := wb1_idx
  brq.io.free2Valid := can_wb2
  brq.io.free2Rob := wb2_idx
  brq.io.free3Valid := can_wb3
  brq.io.free3Rob := wb3_idx
  brq.io.freeDirectValid := ctrl_direct_fire
  brq.io.freeDirectRob := bru_resolve_bits.rob_idx

  val can_load_alu = !stop_issue && ((!d_alu_valid) || alu_leave || flush_alu)
  val can_load_alu1 = !stop_issue && ((!d_alu1_valid) || alu1_leave || flush_alu1)
  val can_load_alu2 = !stop_issue && ((!d_alu2_valid) || alu2_leave || flush_alu2)
  val can_load_alu3 = !stop_issue && ((!d_alu3_valid) || alu3_leave || flush_alu3)
  val can_load_bru = !stop_issue && ((!d_bru_valid) || bru_leave || flush_bru)
  val can_load_div = !stop_issue && ((!d_div_valid) || div_leave || flush_div)
  val lsuCanOverlap = if (OoOParams.LSU_MLP_ENABLE) true.B else
    (!lsu_stage_valid && !lsu_stage1_valid && !lsu.io.bus_busy)
  val can_load_lsu = !stop_issue &&
    ((!d_lsu_valid) || lsu_addr_leave || flush_lsu_d) && lsuCanOverlap
  val can_load_lsu1 = !stop_issue &&
    ((!d_lsu1_valid) || lsu_addr1_leave || flush_lsu1_d) && lsuCanOverlap
  val take_alu_issue = can_load_alu && rs.io.issue_alu_valid
  val take_alu1_issue = can_load_alu1 && rs.io.issue_alu1_valid
  val take_alu2_issue = can_load_alu2 && rs.io.issue_alu2_valid
  val take_alu3_issue = can_load_alu3 && rs.io.issue_alu3_valid
  val take_bru_issue = can_load_bru && brq.io.issue.valid
  val take_div_issue = can_load_div && rs.io.issue_div_valid
  val take_lsu_issue = can_load_lsu && rs.io.issue_lsu_valid
  val take_lsu1_issue = can_load_lsu1 && rs.io.issue_lsu1_valid
  val take_store_addr_issue = !stop_issue && rs.io.issue_store_addr_valid

  rs.io.issue_alu_fire := take_alu_issue
  rs.io.issue_alu1_fire := take_alu1_issue
  rs.io.issue_alu2_fire := take_alu2_issue
  rs.io.issue_alu3_fire := take_alu3_issue
  rs.io.issue_div_fire := take_div_issue
  rs.io.issue_lsu_fire := take_lsu_issue
  rs.io.issue_lsu1_fire := take_lsu1_issue
  rs.io.issue_store_addr_fire := take_store_addr_issue
  storeAddrSidecar.io.issue.valid := take_store_addr_issue
  storeAddrSidecar.io.issue.bits := rs.io.issue_store_addr_bits
  brq.io.issue.ready := can_load_bru

  when(flush_alu) {
    when(can_load_alu && rs.io.issue_alu_valid) {
      d_alu_valid := true.B
      d_alu_bits  := packIssue(rs.io.issue_alu_bits)
    }.otherwise {
      d_alu_valid := false.B
    }
  }.elsewhen(alu_leave || !d_alu_valid) {
    d_alu_valid := take_alu_issue
    when(take_alu_issue) {
      d_alu_bits := packIssue(rs.io.issue_alu_bits)
    }
  }

  when(flush_bru) {
    d_bru_ctrl_resolved := false.B
    d_bru_valid := false.B
  }.elsewhen(bru_leave || !d_bru_valid) {
    d_bru_ctrl_resolved := false.B
    d_bru_valid := take_bru_issue
    when(take_bru_issue) {
      d_bru_bits := packIssue(brq.io.issue.bits)
    }
  }

  when(flush_div) {
    when(can_load_div && rs.io.issue_div_valid) {
      d_div_valid := true.B
      d_div_bits  := packIssue(rs.io.issue_div_bits)
    }.otherwise {
      d_div_valid := false.B
    }
  }.elsewhen(div_leave || !d_div_valid) {
    d_div_valid := take_div_issue
    when(take_div_issue) {
      d_div_bits := packIssue(rs.io.issue_div_bits)
    }
  }

  when(flush_lsu_d) {
    when(can_load_lsu && rs.io.issue_lsu_valid) {
      d_lsu_valid := true.B
      d_lsu_bits  := packIssue(rs.io.issue_lsu_bits)
    }.otherwise {
      d_lsu_valid := false.B
    }
  }.elsewhen(lsu_addr_leave || !d_lsu_valid) {
    d_lsu_valid := take_lsu_issue
    when(take_lsu_issue) {
      d_lsu_bits := packIssue(rs.io.issue_lsu_bits)
    }
  }

  when(flush_lsu1_d) {
    when(take_lsu1_issue) {
      d_lsu1_valid := true.B
      d_lsu1_bits := packIssue(rs.io.issue_lsu1_bits)
    }.otherwise {
      d_lsu1_valid := false.B
    }
  }.elsewhen(lsu_addr1_leave || !d_lsu1_valid) {
    d_lsu1_valid := take_lsu1_issue
    when(take_lsu1_issue) {
      d_lsu1_bits := packIssue(rs.io.issue_lsu1_bits)
    }
  }

  when(flush_alu1) {
    d_alu1_valid := false.B
  }.elsewhen(alu1_leave || !d_alu1_valid) {
    d_alu1_valid := take_alu1_issue
    when(take_alu1_issue) {
      d_alu1_bits := packIssue(rs.io.issue_alu1_bits)
    }
  }

  when(flush_alu2) {
    d_alu2_valid := false.B
  }.elsewhen(alu2_leave || !d_alu2_valid) {
    d_alu2_valid := take_alu2_issue
    when(take_alu2_issue) {
      d_alu2_bits := packIssue(rs.io.issue_alu2_bits)
    }
  }

  when(flush_alu3) {
    d_alu3_valid := false.B
  }.elsewhen(alu3_leave || !d_alu3_valid) {
    d_alu3_valid := take_alu3_issue
    when(take_alu3_issue) {
      d_alu3_bits := packIssue(rs.io.issue_alu3_bits)
    }
  }

  val storeAddrResultEntry = rob.io.entries(storeAddrSidecar.io.result.bits.rob_idx)
  val storeAddrResultKilled = is_irq_w || fencei_flush || mret_flush ||
    (mis_predict_w &&
      robAge(storeAddrSidecar.io.result.bits.rob_idx, rob.io.head) >
        robAge(bru_resolve_bits.rob_idx, rob.io.head))
  val storeAddrResultValid = storeAddrSidecar.io.result.valid &&
    !storeAddrResultKilled && storeAddrResultEntry.valid &&
    storeAddrResultEntry.pc === storeAddrSidecar.io.result.bits.pc &&
    storeAddrResultEntry.mem_valid && storeAddrResultEntry.mem_write &&
    !storeAddrResultEntry.addr_ready

  private def connectStoreQueue(): Unit = {
  // ---------- 5: StoreQueue handles store -> load forward / wait ----------
  val id0_is_store = idu.io.out.bits.signals.lsu.mem_valid && idu.io.out.bits.signals.lsu.mem_write
  val id1_is_store = idu1.io.out.bits.signals.lsu.mem_valid && idu1.io.out.bits.signals.lsu.mem_write
  val id2_is_store = idu2.io.out.bits.signals.lsu.mem_valid && idu2.io.out.bits.signals.lsu.mem_write
  val id3_is_store = idu3.io.out.bits.signals.lsu.mem_valid && idu3.io.out.bits.signals.lsu.mem_write
  val storeAllocMask = VecInit(Seq(en_ren && id0_is_store, en_ren1 && id1_is_store,
    en_ren2 && id2_is_store, en_ren3 && id3_is_store)).asUInt
  val storeAlloc0OH = PriorityEncoderOH(storeAllocMask)
  val storeAlloc1OH = PriorityEncoderOH(storeAllocMask & ~storeAlloc0OH)
  val storeRobIndices = Seq(rob.io.enq_idx, rob.io.enq1_idx, rob.io.enq2_idx, rob.io.enq3_idx)
  val storeMasks = Seq(idu.io.out.bits.signals.lsu.mem_wmask(3, 0),
    idu1.io.out.bits.signals.lsu.mem_wmask(3, 0),
    idu2.io.out.bits.signals.lsu.mem_wmask(3, 0),
    idu3.io.out.bits.signals.lsu.mem_wmask(3, 0))
  sq.io.rob_head := rob.io.head
  sq.io.alloc0_valid := storeAlloc0OH.orR
  sq.io.alloc0_rob := Mux1H(storeAlloc0OH, storeRobIndices)
  sq.io.alloc0_mask := Mux1H(storeAlloc0OH, storeMasks)
  sq.io.alloc1_valid := storeAlloc1OH.orR
  sq.io.alloc1_rob := Mux1H(storeAlloc1OH, storeRobIndices)
  sq.io.alloc1_mask := Mux1H(storeAlloc1OH, storeMasks)
  sq.io.addr_wb_valid := storeAddrResultValid
  sq.io.addr_wb_rob := storeAddrSidecar.io.result.bits.rob_idx
  sq.io.addr_wb_addr := storeAddrSidecar.io.result.bits.addr
  sq.io.ld_valid := lsu.io.ld_query_valid
  sq.io.ld_rob   := lsu.io.ld_query_rob
  sq.io.ld_addr  := lsu.io.ld_query_addr
  sq.io.ld_mem_rd := lsu.io.ld_query_mem_rd
  sq.io.ld1_valid := lsu.io.ld_query1_valid
  sq.io.ld1_rob := lsu.io.ld_query1_rob
  sq.io.ld1_addr := lsu.io.ld_query1_addr
  sq.io.ld1_mem_rd := lsu.io.ld_query1_mem_rd
  stbuf.io.ld_valid  := sq.io.ld_valid
  stbuf.io.ld_addr   := sq.io.ld_addr
  stbuf.io.ld_mem_rd := sq.io.ld_mem_rd
  stbuf.io.ld1_valid := sq.io.ld1_valid
  stbuf.io.ld1_addr := sq.io.ld1_addr
  stbuf.io.ld1_mem_rd := sq.io.ld1_mem_rd

  def loadMask(memRd: UInt, addr: UInt): UInt = {
    val base = MuxLookup(memRd, "b0001".U(4.W))(Seq(
      RBYTE -> "b0001".U(4.W),
      RHALF -> "b0011".U(4.W),
      RWORD -> "b1111".U(4.W),
      RBYTEU -> "b0001".U(4.W),
      RHALFU -> "b0011".U(4.W)
    ))
    (base << addr(1, 0))(3, 0)
  }
  def mergeStoreWords(older: UInt, younger: UInt, youngerMask: UInt): UInt = {
    val bits = Cat((3 to 0 by -1).map(i => Fill(8, youngerMask(i))))
    (older & ~bits) | (younger & bits)
  }
  def cacheable(addr: UInt): Bool =
    (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)

  val stbufKnown0 = stbuf.io.ld_fwd_valid || stbuf.io.ld_partial_valid || stbuf.io.ld_wait
  val stbufMask0 = Mux(stbufKnown0, stbuf.io.ld_partial_mask, 0.U)
  val sqKnown0 = sq.io.has_fwd_candidate
  val sqMask0 = Mux(sqKnown0, sq.io.partial_mask, 0.U)
  val knownMask0 = stbufMask0 | sqMask0
  val knownData0 = mergeStoreWords(stbuf.io.ld_partial_data,
    sq.io.partial_data, sqMask0)
  val neededMask0 = loadMask(sq.io.ld_mem_rd, sq.io.ld_addr)
  val knownOverlap0 = (knownMask0 & neededMask0).orR
  val knownCovered0 = (knownMask0 & neededMask0) === neededMask0
  val cacheable0 = cacheable(sq.io.ld_addr)
  val partialWait0 = knownOverlap0 && !knownCovered0 && !cacheable0
  lsu.io.st_fwd_wait := partialWait0 || sq.io.wait_data
  lsu.io.st_fwd_valid := knownCovered0
  lsu.io.st_fwd_data := knownData0 >> (sq.io.ld_addr(1, 0) << 3)
  lsu.io.st_partial_valid := cacheable0 &&
    knownOverlap0 && !knownCovered0
  lsu.io.st_partial_data := knownData0
  lsu.io.st_partial_mask := knownMask0
  lsu.io.st_unknown_valid := sq.io.wait_unknown
  lsu.io.st_unknown_mask := sq.io.older_unresolved_mask

  val stbufKnown1 = stbuf.io.ld1_fwd_valid || stbuf.io.ld1_partial_valid ||
    stbuf.io.ld1_wait
  val stbufMask1 = Mux(stbufKnown1, stbuf.io.ld1_partial_mask, 0.U)
  val sqKnown1 = sq.io.has_fwd1_candidate
  val sqMask1 = Mux(sqKnown1, sq.io.partial1_mask, 0.U)
  val knownMask1 = stbufMask1 | sqMask1
  val knownData1 = mergeStoreWords(stbuf.io.ld1_partial_data,
    sq.io.partial1_data, sqMask1)
  val neededMask1 = loadMask(sq.io.ld1_mem_rd, sq.io.ld1_addr)
  val knownOverlap1 = (knownMask1 & neededMask1).orR
  val knownCovered1 = (knownMask1 & neededMask1) === neededMask1
  val cacheable1 = cacheable(sq.io.ld1_addr)
  val partialWait1 = knownOverlap1 && !knownCovered1 && !cacheable1
  lsu.io.st_fwd1_wait := partialWait1 || sq.io.wait1_data
  lsu.io.st_fwd1_valid := knownCovered1
  lsu.io.st_fwd1_data := knownData1 >> (sq.io.ld1_addr(1, 0) << 3)
  lsu.io.st_partial1_valid := cacheable1 &&
    knownOverlap1 && !knownCovered1
  lsu.io.st_partial1_data := knownData1
  lsu.io.st_partial1_mask := knownMask1
  lsu.io.st_unknown1_valid := sq.io.wait1_unknown
  lsu.io.st_unknown1_mask := sq.io.older_unresolved1_mask
  lsu.io.st_unresolved_mask := sq.io.unresolved_mask
  lsu.io.rob_head := rob.io.head
  lsu.io.mmio_ready := lsu_mmio_ready
  }
  connectStoreQueue()

  private def connectFetchQueue(): Unit = {
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
    fq.io.enq.bits.bits(i).ftq_idx := ifu.io.out.bits.bits(i).ftq_idx
    fq.io.enq.bits.bits(i).ftq_generation := ifu.io.out.bits.bits(i).ftq_generation
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
  idu.io.in.bits.ftq_idx := fq.io.deq.bits.ftq_idx
  idu.io.in.bits.ftq_generation := fq.io.deq.bits.ftq_generation
  idu1.io.in.valid := fq.io.deq1.valid
  idu1.io.in.bits.inst      := fq.io.deq1.bits.inst
  idu1.io.in.bits.pc        := fq.io.deq1.bits.pc
  idu1.io.in.bits.state     := fq.io.deq1.bits.state
  idu1.io.in.bits.bp_valid  := fq.io.deq1.bits.bp_valid
  idu1.io.in.bits.bp_taken  := fq.io.deq1.bits.bp_taken
  idu1.io.in.bits.bp_target := fq.io.deq1.bits.bp_target
  idu1.io.in.bits.bp_index  := fq.io.deq1.bits.bp_index
  idu1.io.in.bits.ftq_idx := fq.io.deq1.bits.ftq_idx
  idu1.io.in.bits.ftq_generation := fq.io.deq1.bits.ftq_generation
  idu2.io.in.valid := fq.io.deq2.valid
  idu2.io.in.bits.inst := fq.io.deq2.bits.inst
  idu2.io.in.bits.pc := fq.io.deq2.bits.pc
  idu2.io.in.bits.state := fq.io.deq2.bits.state
  idu2.io.in.bits.bp_valid := fq.io.deq2.bits.bp_valid
  idu2.io.in.bits.bp_taken := fq.io.deq2.bits.bp_taken
  idu2.io.in.bits.bp_target := fq.io.deq2.bits.bp_target
  idu2.io.in.bits.bp_index := fq.io.deq2.bits.bp_index
  idu2.io.in.bits.ftq_idx := fq.io.deq2.bits.ftq_idx
  idu2.io.in.bits.ftq_generation := fq.io.deq2.bits.ftq_generation
  idu3.io.in.valid := fq.io.deq3.valid
  idu3.io.in.bits.inst := fq.io.deq3.bits.inst
  idu3.io.in.bits.pc := fq.io.deq3.bits.pc
  idu3.io.in.bits.state := fq.io.deq3.bits.state
  idu3.io.in.bits.bp_valid := fq.io.deq3.bits.bp_valid
  idu3.io.in.bits.bp_taken := fq.io.deq3.bits.bp_taken
  idu3.io.in.bits.bp_target := fq.io.deq3.bits.bp_target
  idu3.io.in.bits.bp_index := fq.io.deq3.bits.bp_index
  idu3.io.in.bits.ftq_idx := fq.io.deq3.bits.ftq_idx
  idu3.io.in.bits.ftq_generation := fq.io.deq3.bits.ftq_generation
  fq.io.deq.ready := idu.io.in.ready
  fq.io.deq1.ready := idu1.io.in.ready
  fq.io.deq2.ready := idu2.io.in.ready
  fq.io.deq3.ready := idu3.io.in.ready
  }
  connectFetchQueue()

  // ---------- WBU winner selection + ROB writeback ----------
  // Stores carry completion metadata but no PRF value.  Keep them off the
  // data CDBs and complete them through a private ROB/SQ sideband below.
  val lsu_data_wb_valid = lsu_wb_valid && lsu_is_load
  val lsu1_is_load = lsu1_wb.signals.wbu.reg_write_sel === MEM_SEL
  val lsu1_data_wb_valid = lsu1_wb_valid && lsu1_is_load
  val wb_cand_valid = VecInit(Seq(alu_wb_valid, lsu_data_wb_valid,
    lsu1_data_wb_valid, alu1_wb_valid, alu2_wb_valid,
    alu3_wb_valid, div_wb_valid, bru_wb_valid))
  val wb_cand_mask = wb_cand_valid.asUInt
  val wb_cand_count = PopCount(wb_cand_mask)
  val wb_conflict = wb_cand_count > OoOParams.CDB_NUM.U
  private def buildWritebackNetwork(): WritebackArbiter = {
    val arb = Module(new WritebackArbiter(inputCount = 8, outputCount = OoOParams.CDB_NUM))
    arb.io.robHead := rob.io.head
    val valid = Seq(alu_wb_valid, lsu_data_wb_valid, lsu1_data_wb_valid,
      alu1_wb_valid, alu2_wb_valid, alu3_wb_valid, div_wb_valid, bru_wb_valid)
    val bits = Seq(alu_wb, lsu_wb, lsu1_wb,
      alu1_wb, alu2_wb, alu3_wb, div_wb, bru_wb)
    for (i <- valid.indices) {
      arb.io.in(i).valid := valid(i)
      arb.io.in(i).bits := bits(i)
    }
    for (i <- 0 until OoOParams.CDB_NUM) {
      arb.io.out(i).ready := true.B
    }
    wbu.io.in.valid := arb.io.out(0).valid
    wbu.io.in.bits := arb.io.out(0).bits
    wbu.io.is_flush := false.B
    wbu1.io.in.valid := arb.io.out(1).valid
    wbu1.io.in.bits := arb.io.out(1).bits
    wbu1.io.is_flush := false.B
    wbu2.io.in.valid := arb.io.out(2).valid
    wbu2.io.in.bits := arb.io.out(2).bits
    wbu2.io.is_flush := false.B
    wbu3.io.in.valid := arb.io.out(3).valid
    wbu3.io.in.bits := arb.io.out(3).bits
    wbu3.io.is_flush := false.B
    exu.io.out.ready := arb.io.in(0).ready
    lsu.io.out.ready := arb.io.in(1).ready
    lsu.io.out1.ready := arb.io.in(2).ready
    exu_alu1.io.out.ready := arb.io.in(3).ready
    exu_alu2.io.out.ready := arb.io.in(4).ready
    exu_alu3.io.out.ready := arb.io.in(5).ready
    exu_div.io.out.ready := arb.io.in(6).ready
    exu_bru.io.out.ready := Mux(d_bru_ctrl_no_dest, true.B, arb.io.in(7).ready)
    exu.io.pc.ready := true.B
    exu_alu1.io.pc.ready := true.B
    exu_alu2.io.pc.ready := true.B
    exu_alu3.io.pc.ready := true.B
    exu_div.io.pc.ready := true.B
    exu_bru.io.pc.ready := true.B
    exu_lsu.io.pc.ready := true.B
    exu_lsu1.io.pc.ready := true.B
    arb
  }
  val wbArb = buildWritebackNetwork()
  val wb_fixed_first = PriorityEncoder(wb_cand_mask)
  val wb_age_reorder = wbArb.io.out(0).valid &&
    (wbArb.io.grantIdx(0) =/= wb_fixed_first)
  // The LQ reports a violation one cycle after the store resolves.  The
  // load may have been committed or flushed in that interval, and its ROB
  // index may already have been reused.  Never redirect from such a stale
  // LQ record: the selective ROB flush boundary is meaningful only for the
  // live dynamic instruction with the same PC.
  val mem_violation_entry = rob.io.entries(lsu.io.mem_violation_rob)
  val mem_violation_live = mem_violation_entry.valid &&
    mem_violation_entry.mem_valid && !mem_violation_entry.mem_write &&
    (mem_violation_entry.pc === lsu.io.mem_violation_pc)
  mem_violation_w := lsu.io.mem_violation_valid && mem_violation_live
  mem_violation_rob_w := lsu.io.mem_violation_rob
  mem_violation_pc_w := lsu.io.mem_violation_pc
  dontTouch(mem_violation_w)
  dontTouch(mem_violation_rob_w)
  dontTouch(mem_violation_pc_w)

  val wb_fire  = wbu.io.in.valid && !wbu.io.is_flush
  wb_idx   := wbu.io.in.bits.rob_idx
  val wb1_fire = wbu1.io.in.valid && !wbu1.io.is_flush
  wb1_idx  := wbu1.io.in.bits.rob_idx
  val wb2_fire = wbu2.io.in.valid && !wbu2.io.is_flush
  wb2_idx  := wbu2.io.in.bits.rob_idx
  val wb3_fire = wbu3.io.in.valid && !wbu3.io.is_flush
  wb3_idx  := wbu3.io.in.bits.rob_idx
  wb_pdest := wbu.io.in.bits.pdest
  wb_val   := wbu.io.refile.wdata
  wb1_pdest := wbu1.io.in.bits.pdest
  wb1_val   := wbu1.io.refile.wdata
  wb2_pdest := wbu2.io.in.bits.pdest
  wb2_val   := wbu2.io.refile.wdata
  wb3_pdest := wbu3.io.in.bits.pdest
  wb3_val   := wbu3.io.refile.wdata
  // A writeback winner is accepted only if its full ROB identity still matches.
  val head_e = rob.io.entries(rob.io.head)
  val wb_entry = rob.io.entries(wb_idx)
  val wb1_entry = rob.io.entries(wb1_idx)
  val wb2_entry = rob.io.entries(wb2_idx)
  val wb3_entry = rob.io.entries(wb3_idx)
  val store_entry = rob.io.entries(lsu_store_wb.rob_idx)
  val wb_matches_entry = wb_entry.valid &&
    (wb_entry.pc === wbu.io.in.bits.pc) &&
    (wb_entry.arch_rd === wbu.io.in.bits.waddr) &&
    (wb_entry.new_phys === wbu.io.in.bits.pdest)
  val wb1_matches_entry = wb1_entry.valid &&
    (wb1_entry.pc === wbu1.io.in.bits.pc) &&
    (wb1_entry.arch_rd === wbu1.io.in.bits.waddr) &&
    (wb1_entry.new_phys === wbu1.io.in.bits.pdest)
  val wb2_matches_entry = wb2_entry.valid &&
    (wb2_entry.pc === wbu2.io.in.bits.pc) &&
    (wb2_entry.arch_rd === wbu2.io.in.bits.waddr) &&
    (wb2_entry.new_phys === wbu2.io.in.bits.pdest)
  val wb3_matches_entry = wb3_entry.valid &&
    (wb3_entry.pc === wbu3.io.in.bits.pc) &&
    (wb3_entry.arch_rd === wbu3.io.in.bits.waddr) &&
    (wb3_entry.new_phys === wbu3.io.in.bits.pdest)
  val store_result_valid = lsu_store_wb_valid
  val store_matches_entry = store_entry.valid && store_entry.mem_valid && store_entry.mem_write &&
    (store_entry.pc === lsu_store_wb.pc) &&
    (store_entry.arch_rd === lsu_store_wb.waddr) &&
    (store_entry.new_phys === lsu_store_wb.pdest)
  val store_direct_raw = store_result_valid && store_matches_entry
  val head_wb0_raw = wb_fire && wb_matches_entry && (wb_idx === rob.io.head)
  val head_wb1_raw = wb1_fire && wb1_matches_entry && (wb1_idx === rob.io.head)
  val head_wb2_raw = wb2_fire && wb2_matches_entry && (wb2_idx === rob.io.head)
  val head_wb3_raw = wb3_fire && wb3_matches_entry && (wb3_idx === rob.io.head)
  val head_wb_raw_valid = head_wb0_raw || head_wb1_raw || head_wb2_raw || head_wb3_raw
  val head_wb_raw_bits = Wire(new LSU_WBU_IO)
  head_wb_raw_bits := Mux(head_wb0_raw, wbu.io.in.bits,
    Mux(head_wb1_raw, wbu1.io.in.bits,
      Mux(head_wb2_raw, wbu2.io.in.bits, wbu3.io.in.bits)))
  val head_store_raw = store_direct_raw && (lsu_store_wb.rob_idx === rob.io.head)
  val head_is_exc = head_e.valid && Mux(head_e.done, head_e.state.state,
    (head_wb_raw_valid && head_wb_raw_bits.state.state) ||
      (head_store_raw && lsu_store_wb.state.state))
  def wbKilled(idx: UInt): Bool = {
    val youngerMis = mis_predict_w &&
      (robAge(idx, rob.io.head) > robAge(mis_rob_w, rob.io.head))
    val youngerFlush = mem_flush_all || (flush_now &&
      (robAge(idx, rob.io.head) > robAge(flush_idx, rob.io.head)))
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
  can_wb2  := wb2_fire && wb2_matches_entry && !wb2_entry.done &&
    !(can_wb && (wb2_idx === wb_idx)) && !(can_wb1 && (wb2_idx === wb1_idx)) &&
    !wbKilled(wb2_idx)
  can_wb3  := wb3_fire && wb3_matches_entry && !wb3_entry.done &&
    !(can_wb && (wb3_idx === wb_idx)) && !(can_wb1 && (wb3_idx === wb1_idx)) &&
    !(can_wb2 && (wb3_idx === wb2_idx)) && !wbKilled(wb3_idx)
  val store_direct_fire = store_direct_raw && !store_entry.done &&
    !wbKilled(lsu_store_wb.rob_idx)

  rs.io.free_store_fire := store_direct_fire
  rs.io.free_store_idx := lsu_store_wb.rob_idx

  val wbRejectFlags = RegInit(VecInit(Seq.fill(OoOParams.ROB_SIZE)(0.U(4.W))))
  when(rob.io.enq_fire) {
    wbRejectFlags(rob.io.enq_idx) := 0.U
  }
  when(rob.io.enq1_fire) {
    wbRejectFlags(rob.io.enq1_idx) := 0.U
  }
  when(rob.io.enq2_fire) {
    wbRejectFlags(rob.io.enq2_idx) := 0.U
  }
  when(rob.io.enq3_fire) {
    wbRejectFlags(rob.io.enq3_idx) := 0.U
  }
  val wb0LiveIdentityReject = wb_fire && wb_entry.valid && !wb_entry.done &&
    !wbKilled(wb_idx) && !wb_matches_entry
  val wb1LiveIdentityReject = wb1_fire && wb1_entry.valid && !wb1_entry.done &&
    !wbKilled(wb1_idx) && !wb1_matches_entry
  val wb2LiveIdentityReject = wb2_fire && wb2_entry.valid && !wb2_entry.done &&
    !wbKilled(wb2_idx) && !wb2_matches_entry
  val wb3LiveIdentityReject = wb3_fire && wb3_entry.valid && !wb3_entry.done &&
    !wbKilled(wb3_idx) && !wb3_matches_entry
  when(wb0LiveIdentityReject) {
    wbRejectFlags(wb_idx) := Cat(
      wb_entry.pc === wbu.io.in.bits.pc,
      wb_entry.arch_rd === wbu.io.in.bits.waddr,
      wb_entry.new_phys === wbu.io.in.bits.pdest,
      true.B)
  }
  when(wb1LiveIdentityReject) {
    wbRejectFlags(wb1_idx) := Cat(
      wb1_entry.pc === wbu1.io.in.bits.pc,
      wb1_entry.arch_rd === wbu1.io.in.bits.waddr,
      wb1_entry.new_phys === wbu1.io.in.bits.pdest,
      true.B)
  }
  when(wb2LiveIdentityReject) {
    wbRejectFlags(wb2_idx) := Cat(
      wb2_entry.pc === wbu2.io.in.bits.pc,
      wb2_entry.arch_rd === wbu2.io.in.bits.waddr,
      wb2_entry.new_phys === wbu2.io.in.bits.pdest,
      true.B)
  }
  when(wb3LiveIdentityReject) {
    wbRejectFlags(wb3_idx) := Cat(
      wb3_entry.pc === wbu3.io.in.bits.pc,
      wb3_entry.arch_rd === wbu3.io.in.bits.waddr,
      wb3_entry.new_phys === wbu3.io.in.bits.pdest,
      true.B)
  }
  io.debug_lq_head_alloc_pc := lsu.io.debug_lq_head_alloc_pc
  io.debug_lq_head_remove_reason := lsu.io.debug_lq_head_remove_reason
  io.debug_wb_head_reject_flags := wbRejectFlags(rob.io.head)
  io.debug_rob_head_pc := head_e.pc
  io.debug_rob_head_valid := head_e.valid
  io.debug_rob_head_done := head_e.done
  io.debug_rob_head_mem := head_e.mem_valid
  io.debug_rob_head_ctrl := head_e.jump =/= JUMP_NONE
  io.debug_rob_count := rob.io.count
  io.debug_rs_count := rs.io.count
  io.debug_brq_count := brq.io.count
  io.debug_fq_count := fq.io.count
  io.debug_brq_issue_valid := brq.io.issue.valid
  io.debug_bru_dispatch_valid := d_bru_valid

  val wb_accept_count = PopCount(VecInit(Seq(can_wb, can_wb1, can_wb2, can_wb3)).asUInt)
  val wb_blocked = wb_cand_count =/= wb_accept_count
  val wb_wen = can_wb && wbu.io.refile.wen && (wb_pdest =/= 0.U) && (wbu.io.in.bits.waddr =/= 0.U)
  val wb1_wen = can_wb1 && wbu1.io.refile.wen && (wb1_pdest =/= 0.U) && (wbu1.io.in.bits.waddr =/= 0.U)
  val wb2_wen = can_wb2 && wbu2.io.refile.wen && (wb2_pdest =/= 0.U) && (wbu2.io.in.bits.waddr =/= 0.U)
  val wb3_wen = can_wb3 && wbu3.io.refile.wen && (wb3_pdest =/= 0.U) && (wbu3.io.in.bits.waddr =/= 0.U)
  val head_wb0 = can_wb && (wb_idx === rob.io.head)
  val head_wb1 = can_wb1 && (wb1_idx === rob.io.head)
  val head_wb2 = can_wb2 && (wb2_idx === rob.io.head)
  val head_wb3 = can_wb3 && (wb3_idx === rob.io.head)
  val head_wb_valid = head_wb0 || head_wb1 || head_wb2 || head_wb3
  val head_wb_bits = Wire(new LSU_WBU_IO)
  head_wb_bits := Mux(head_wb0, wbu.io.in.bits,
    Mux(head_wb1, wbu1.io.in.bits,
      Mux(head_wb2, wbu2.io.in.bits, wbu3.io.in.bits)))
  val head_wb_val = Mux(head_wb0, wb_val,
    Mux(head_wb1, wb1_val, Mux(head_wb2, wb2_val, wb3_val)))

  prf.io.wen1   := wb_wen
  prf.io.waddr1 := wb_pdest
  prf.io.wdata1 := wb_val
  prf.io.wen2   := wb1_wen
  prf.io.waddr2 := wb1_pdest
  prf.io.wdata2 := wb1_val
  prf.io.wen3   := wb2_wen
  prf.io.waddr3 := wb2_pdest
  prf.io.wdata3 := wb2_val
  prf.io.wen4   := wb3_wen
  prf.io.waddr4 := wb3_pdest
  prf.io.wdata4 := wb3_val

  busy.io.set_en   := en_ren && id_doren
  busy.io.set_addr := id_pdest
  busy.io.set_en2  := en_ren1 && id1_doren
  busy.io.set_addr2 := id1_pdest
  busy.io.set_en3  := en_ren2 && id2_doren
  busy.io.set_addr3 := id2_pdest
  busy.io.set_en4  := en_ren3 && id3_doren
  busy.io.set_addr4 := id3_pdest
  busy.io.clr_en   := wb_wen
  busy.io.clr_addr := wb_pdest
  busy.io.clr_en2  := wb1_wen
  busy.io.clr_addr2 := wb1_pdest
  busy.io.clr_en3  := wb2_wen
  busy.io.clr_addr3 := wb2_pdest
  busy.io.clr_en4  := wb3_wen
  busy.io.clr_addr4 := wb3_pdest

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
  rob.io.wb2_fire         := can_wb2
  rob.io.wb2_idx          := wb2_idx
  rob.io.wb2_val          := wb2_val
  rob.io.wb2_state        := wbu2.io.in.bits.state
  rob.io.wb2_mem_addr     := wbu2.io.in.bits.alu_result
  rob.io.wb2_mem_wdata    := wbu2.io.in.bits.store_data
  rob.io.wb2_actual_taken := wbu2.io.in.bits.br_taken
  rob.io.wb2_actual_target := wbu2.io.in.bits.next_pc
  rob.io.wb3_fire         := can_wb3
  rob.io.wb3_idx          := wb3_idx
  rob.io.wb3_val          := wb3_val
  rob.io.wb3_state        := wbu3.io.in.bits.state
  rob.io.wb3_mem_addr     := wbu3.io.in.bits.alu_result
  rob.io.wb3_mem_wdata    := wbu3.io.in.bits.store_data
  rob.io.wb3_actual_taken := wbu3.io.in.bits.br_taken
  rob.io.wb3_actual_target := wbu3.io.in.bits.next_pc
  rob.io.ctrl_wb_fire := ctrl_direct_fire
  rob.io.ctrl_wb_idx := bru_resolve_bits.rob_idx
  rob.io.ctrl_wb_state := bru_resolve_result.state
  rob.io.ctrl_wb_actual_taken := bru_resolve_result.br_taken
  rob.io.ctrl_wb_actual_target := bru_resolve_result.next_pc
  rob.io.store_wb_fire := store_direct_fire
  rob.io.store_wb_idx := lsu_store_wb.rob_idx
  rob.io.store_wb_state := lsu_store_wb.state
  rob.io.store_wb_mem_addr := lsu_store_wb.alu_result
  rob.io.store_wb_mem_wdata := lsu_store_wb.store_data

  val cm_bits = rob.io.commit_bits
  val cm1_bits = rob.io.commit1_bits
  val cm2_bits = rob.io.commit2_bits
  val cm3_bits = rob.io.commit3_bits
  val cm1_wb0 = can_wb && (wb_idx === rob.io.commit1_idx)
  val cm1_wb1 = can_wb1 && (wb1_idx === rob.io.commit1_idx)
  val cm1_wb2 = can_wb2 && (wb2_idx === rob.io.commit1_idx)
  val cm1_wb3 = can_wb3 && (wb3_idx === rob.io.commit1_idx)
  val cm1_wb_valid = cm1_wb0 || cm1_wb1 || cm1_wb2 || cm1_wb3
  val cm1_wb_bits = Wire(new LSU_WBU_IO)
  cm1_wb_bits := Mux(cm1_wb0, wbu.io.in.bits,
    Mux(cm1_wb1, wbu1.io.in.bits,
      Mux(cm1_wb2, wbu2.io.in.bits, wbu3.io.in.bits)))
  val cm1_wb_val = Mux(cm1_wb0, wb_val,
    Mux(cm1_wb1, wb1_val, Mux(cm1_wb2, wb2_val, wb3_val)))
  def sameCycleWb(idx: UInt): Bool =
    (can_wb && wb_idx === idx) || (can_wb1 && wb1_idx === idx) ||
      (can_wb2 && wb2_idx === idx) || (can_wb3 && wb3_idx === idx)
  def sameCycleWbValue(idx: UInt): UInt = Mux(can_wb && wb_idx === idx, wb_val,
    Mux(can_wb1 && wb1_idx === idx, wb1_val,
      Mux(can_wb2 && wb2_idx === idx, wb2_val, wb3_val)))
  val head_ctrl_wb = ctrl_direct_fire && (bru_resolve_bits.rob_idx === rob.io.head)
  val cm1_ctrl_wb = ctrl_direct_fire && (bru_resolve_bits.rob_idx === rob.io.commit1_idx)
  val cm2_ctrl_wb = ctrl_direct_fire && (bru_resolve_bits.rob_idx === rob.io.commit2_idx)
  val cm3_ctrl_wb = ctrl_direct_fire && (bru_resolve_bits.rob_idx === rob.io.commit3_idx)
  val head_store_wb = store_direct_fire && (lsu_store_wb.rob_idx === rob.io.head)
  val cm1_store_wb = store_direct_fire && (lsu_store_wb.rob_idx === rob.io.commit1_idx)
  val cm2_store_wb = store_direct_fire && (lsu_store_wb.rob_idx === rob.io.commit2_idx)
  val cm3_store_wb = store_direct_fire && (lsu_store_wb.rob_idx === rob.io.commit3_idx)
  val cm2_wb_valid = sameCycleWb(rob.io.commit2_idx)
  val cm3_wb_valid = sameCycleWb(rob.io.commit3_idx)
  val cm2_wb_bits = Mux(can_wb && wb_idx === rob.io.commit2_idx, wbu.io.in.bits,
    Mux(can_wb1 && wb1_idx === rob.io.commit2_idx, wbu1.io.in.bits,
      Mux(can_wb2 && wb2_idx === rob.io.commit2_idx, wbu2.io.in.bits, wbu3.io.in.bits)))
  val cm3_wb_bits = Mux(can_wb && wb_idx === rob.io.commit3_idx, wbu.io.in.bits,
    Mux(can_wb1 && wb1_idx === rob.io.commit3_idx, wbu1.io.in.bits,
      Mux(can_wb2 && wb2_idx === rob.io.commit3_idx, wbu2.io.in.bits, wbu3.io.in.bits)))

  def isPmem(addr: UInt): Bool =
    (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)

  // 4c：异常冲刷拍禁止提交后继；5a：store 写完才 commit_fire；4f：fencei icache ready 才 commit_fire
  // 4e：mret_flush 拍禁止提交后继（同 fencei）
  // 4g：ext_irq_flush 拍禁止提交（冲在飞）
  val is_irq_early = Wire(Bool())
  val store_commit_ready = Wire(Bool())
  val store_side_empty = Wire(Bool())
  val cm_is_store = cm_bits.mem_valid && cm_bits.mem_write
  val cm_is_load = cm_bits.mem_valid && !cm_bits.mem_write
  val cm_is_fencei = cm_bits.is_fencei
  val cm_is_mret = cm_bits.jump === JUMP_MERT
  val cm_state = Mux(head_ctrl_wb, bru_resolve_result.state,
    Mux(head_store_wb, lsu_store_wb.state,
      Mux(head_wb_valid, head_wb_bits.state, cm_bits.state)))
  val cm_needs_store_drain = cm_bits.is_ebreak || cm_is_mret || cm_state.state
  val cm_is_ctrl = cm_bits.jump =/= JUMP_NONE
  val cm1_is_store = cm1_bits.mem_valid && cm1_bits.mem_write
  val cm1_is_load = cm1_bits.mem_valid && !cm1_bits.mem_write
  val cm1_is_fencei = cm1_bits.is_fencei
  val cm1_is_mret = cm1_bits.jump === JUMP_MERT
  val cm1_state = Mux(cm1_ctrl_wb, bru_resolve_result.state,
    Mux(cm1_store_wb, lsu_store_wb.state,
      Mux(cm1_wb_valid, cm1_wb_bits.state, cm1_bits.state)))
  val cm1_is_ctrl = cm1_bits.jump =/= JUMP_NONE
  val cm1_is_ctrl_not_mret = cm1_is_ctrl && !cm1_is_mret
  val cm2_is_store = cm2_bits.mem_valid && cm2_bits.mem_write
  val cm2_is_load = cm2_bits.mem_valid && !cm2_bits.mem_write
  val cm2_is_mret = cm2_bits.jump === JUMP_MERT
  val cm2_is_ctrl = cm2_bits.jump =/= JUMP_NONE
  val cm2_is_ctrl_not_mret = cm2_is_ctrl && !cm2_is_mret
  val cm3_is_store = cm3_bits.mem_valid && cm3_bits.mem_write
  val cm3_is_load = cm3_bits.mem_valid && !cm3_bits.mem_write
  val cm3_is_mret = cm3_bits.jump === JUMP_MERT
  val cm3_is_ctrl = cm3_bits.jump =/= JUMP_NONE
  val cm3_is_ctrl_not_mret = cm3_is_ctrl && !cm3_is_mret
  val cm_mem_addr = Mux(head_store_wb, lsu_store_wb.alu_result,
    Mux(head_wb_valid, head_wb_bits.alu_result, cm_bits.mem_addr))
  val cm1_mem_addr = Mux(cm1_store_wb, lsu_store_wb.alu_result,
    Mux(cm1_wb_valid, cm1_wb_bits.alu_result, cm1_bits.mem_addr))
  val cm2_mem_addr = Mux(cm2_store_wb, lsu_store_wb.alu_result,
    Mux(cm2_wb_valid, cm2_wb_bits.alu_result, cm2_bits.mem_addr))
  val cm3_mem_addr = Mux(cm3_store_wb, lsu_store_wb.alu_result,
    Mux(cm3_wb_valid, cm3_wb_bits.alu_result, cm3_bits.mem_addr))
  val cm_actual_taken = Mux(head_ctrl_wb, bru_resolve_result.br_taken,
    Mux(head_wb_valid, head_wb_bits.br_taken, cm_bits.actual_taken))
  val cm_actual_target = Mux(head_ctrl_wb, bru_resolve_result.next_pc,
    Mux(head_wb_valid, head_wb_bits.next_pc, cm_bits.actual_target))
  val cm1_actual_taken = Mux(cm1_ctrl_wb, bru_resolve_result.br_taken,
    Mux(cm1_wb_valid, cm1_wb_bits.br_taken, cm1_bits.actual_taken))
  val cm1_actual_target = Mux(cm1_ctrl_wb, bru_resolve_result.next_pc,
    Mux(cm1_wb_valid, cm1_wb_bits.next_pc, cm1_bits.actual_target))
  val cm2_actual_taken = Mux(cm2_ctrl_wb, bru_resolve_result.br_taken,
    Mux(cm2_wb_valid, cm2_wb_bits.br_taken, cm2_bits.actual_taken))
  val cm2_actual_target = Mux(cm2_ctrl_wb, bru_resolve_result.next_pc,
    Mux(cm2_wb_valid, cm2_wb_bits.next_pc, cm2_bits.actual_target))
  val cm3_actual_taken = Mux(cm3_ctrl_wb, bru_resolve_result.br_taken,
    Mux(cm3_wb_valid, cm3_wb_bits.br_taken, cm3_bits.actual_taken))
  val cm3_actual_target = Mux(cm3_ctrl_wb, bru_resolve_result.next_pc,
    Mux(cm3_wb_valid, cm3_wb_bits.next_pc, cm3_bits.actual_target))
  val cm_mmio_mem = cm_bits.mem_valid && !isPmem(cm_mem_addr)
  val cm1_mmio_mem = cm1_bits.mem_valid && !isPmem(cm1_mem_addr)
  val cm2_mmio_mem = cm2_bits.mem_valid && !isPmem(cm2_mem_addr)
  val cm3_mmio_mem = cm3_bits.mem_valid && !isPmem(cm3_mem_addr)
  val cm_special = cm_bits.csr_write || cm_is_fencei || cm_is_mret ||
    cm_bits.is_ebreak || cm_state.state
  val cm1_special = cm1_bits.csr_write || cm1_is_fencei || cm1_is_mret ||
    cm1_bits.is_ebreak || cm1_state.state
  val cm2_state = Mux(cm2_ctrl_wb, bru_resolve_result.state,
    Mux(cm2_store_wb, lsu_store_wb.state, Mux(cm2_wb_valid, cm2_wb_bits.state, cm2_bits.state)))
  val cm3_state = Mux(cm3_ctrl_wb, bru_resolve_result.state,
    Mux(cm3_store_wb, lsu_store_wb.state, Mux(cm3_wb_valid, cm3_wb_bits.state, cm3_bits.state)))
  val cm2_special = cm2_bits.csr_write || cm2_bits.is_fencei || cm2_is_mret ||
    cm2_bits.is_ebreak || cm2_state.state
  val cm3_special = cm3_bits.csr_write || cm3_bits.is_fencei || cm3_is_mret ||
    cm3_bits.is_ebreak || cm3_state.state
  val cm_exclusive = cm_special || cm_mmio_mem
  val cm1_exclusive = cm1_special || cm1_mmio_mem
  val cm2_exclusive = cm2_special || cm2_mmio_mem
  val cm3_exclusive = cm3_special || cm3_mmio_mem
  val cm_bpu_update = cm_is_ctrl && !cm_is_mret
  val cm1_bpu_update = cm1_is_ctrl && !cm1_is_mret
  val cm2_bpu_update = cm2_is_ctrl && !cm2_is_mret
  val cm3_bpu_update = cm3_is_ctrl && !cm3_is_mret
  val cm_bpu_ready = !cm_bpu_update || (ifu.io.bpu_update_free >= 1.U)
  val pair_bpu_count = PopCount(Seq(cm_bpu_update, cm1_bpu_update))
  val pair_bpu_ready = ifu.io.bpu_update_free >= pair_bpu_count
  val pair_store_count = PopCount(Seq(cm_is_store && !cm_mmio_mem,
    cm1_is_store && !cm1_mmio_mem))
  val cm1_store_addr_ready = cm1_bits.addr_ready || cm1_store_wb ||
    (cm1_wb_valid && cm1_is_store)
  val pair_store_ready = (stbuf.io.free >= pair_store_count) &&
    (!cm1_is_store || cm1_store_addr_ready)
  val cm_arch_next_pc = Mux(cm_actual_taken, cm_actual_target, cm_bits.pc + 4.U)
  val pair_control_path_ok = !cm_bpu_update || (cm1_bits.pc === cm_arch_next_pc)
  val cm2_store_addr_ready = cm2_bits.addr_ready || cm2_store_wb ||
    (cm2_wb_valid && cm2_is_store)
  val cm3_store_addr_ready = cm3_bits.addr_ready || cm3_store_wb ||
    (cm3_wb_valid && cm3_is_store)
  val triple_bpu_count = PopCount(Seq(cm_bpu_update, cm1_bpu_update, cm2_bpu_update))
  val quad_bpu_count = PopCount(Seq(cm_bpu_update, cm1_bpu_update, cm2_bpu_update, cm3_bpu_update))
  val triple_bpu_ready = triple_bpu_count <= 2.U && ifu.io.bpu_update_free >= triple_bpu_count
  val quad_bpu_ready = quad_bpu_count <= 2.U && ifu.io.bpu_update_free >= quad_bpu_count
  val triple_store_count = PopCount(Seq(cm_is_store && !cm_mmio_mem,
    cm1_is_store && !cm1_mmio_mem, cm2_is_store && !cm2_mmio_mem))
  val quad_store_count = PopCount(Seq(cm_is_store && !cm_mmio_mem,
    cm1_is_store && !cm1_mmio_mem, cm2_is_store && !cm2_mmio_mem,
    cm3_is_store && !cm3_mmio_mem))
  val triple_store_ready = triple_store_count <= 2.U && stbuf.io.free >= triple_store_count &&
    (!cm1_is_store || cm1_store_addr_ready) && (!cm2_is_store || cm2_store_addr_ready)
  val quad_store_ready = quad_store_count <= 2.U && stbuf.io.free >= quad_store_count &&
    (!cm1_is_store || cm1_store_addr_ready) && (!cm2_is_store || cm2_store_addr_ready) &&
    (!cm3_is_store || cm3_store_addr_ready)
  // fencei/mret/ext_irq flush 拍禁止提交后继（否则会在冲刷前多提交 jal 等，difftest PC 错位）
  rob.io.commit_fire := rob.io.commit_valid && !is_irq_early && !fencei_flush && !mret_flush &&
    !ext_irq_flush && !is_bp_flush && !is_mem_flush && !bp_commit_block &&
    cm_bpu_ready &&
    (!cm_is_load || !lsu.io.load_commit_wait0) &&
    (!cm_is_store || store_commit_ready) &&
    (!cm_is_fencei || fencei_commit_ready) &&
    (!cm_needs_store_drain || store_side_empty)
  val cm_fire   = rob.io.commit_fire
  rob.io.commit1_fire := cm_fire && rob.io.commit1_valid && !cm_exclusive && !cm1_exclusive &&
    !bp_commit1_block && pair_bpu_ready && pair_store_ready && pair_control_path_ok &&
    (!cm1_is_load || !lsu.io.load_commit_wait1)
  val cm1_fire = rob.io.commit1_fire
  val cm1_arch_next_pc = Mux(cm1_actual_taken, cm1_actual_target, cm1_bits.pc + 4.U)
  val cm2_path_ok = !cm1_bpu_update || (cm2_bits.pc === cm1_arch_next_pc)
  rob.io.commit2_fire := cm1_fire && rob.io.commit2_valid && !cm2_exclusive &&
    !bp_commit2_block && cm2_path_ok && triple_bpu_ready && triple_store_ready &&
    (!cm2_is_load || !lsu.io.load_commit_wait2)
  val cm2_fire = rob.io.commit2_fire
  val cm2_arch_next_pc = Mux(cm2_actual_taken, cm2_actual_target, cm2_bits.pc + 4.U)
  val cm3_path_ok = !cm2_bpu_update || (cm3_bits.pc === cm2_arch_next_pc)
  rob.io.commit3_fire := cm2_fire && rob.io.commit3_valid && !cm3_exclusive &&
    !bp_commit3_block && cm3_path_ok && quad_bpu_ready && quad_store_ready &&
    (!cm3_is_load || !lsu.io.load_commit_wait3)
  val cm3_fire = rob.io.commit3_fire
  IFUCommitWiring.connect(ifu.io,
    cm_fire, cm_bits, cm1_fire, cm1_bits,
    cm2_fire, cm2_bits, cm3_fire, cm3_bits)
  val cm_do_ren = cm_fire && cm_bits.reg_write && (cm_bits.arch_rd =/= 0.U) && (cm_bits.new_phys =/= 0.U)
  val cm1_do_ren = cm1_fire && cm1_bits.reg_write && (cm1_bits.arch_rd =/= 0.U) && (cm1_bits.new_phys =/= 0.U)
  val cm2_do_ren = cm2_fire && cm2_bits.reg_write && (cm2_bits.arch_rd =/= 0.U) && (cm2_bits.new_phys =/= 0.U)
  val cm3_do_ren = cm3_fire && cm3_bits.reg_write && (cm3_bits.arch_rd =/= 0.U) && (cm3_bits.new_phys =/= 0.U)
  val cm1_slot_available = cm_fire && rob.io.commit1_valid
  val cm1_slot_not_ready = cm_fire && !rob.io.commit1_valid
  val cm1_slot_blocked = cm1_slot_available && !cm1_fire
  val cm1_block_slot0_excl = cm1_slot_blocked && cm_exclusive
  val cm1_block_mem = cm1_slot_blocked && !cm_exclusive &&
    (cm1_mmio_mem || (cm1_bits.mem_valid && !pair_store_ready))
  val cm1_block_ctrl = cm1_slot_blocked && !cm_exclusive &&
    ((!pair_control_path_ok) || (!cm1_bits.mem_valid && cm1_is_ctrl_not_mret && !pair_bpu_ready))
  val cm1_block_csr = cm1_slot_blocked && !cm_exclusive && !cm1_bits.mem_valid &&
    !cm1_is_ctrl_not_mret && cm1_bits.csr_write
  val cm1_block_special = cm1_slot_blocked && !cm_exclusive && !cm1_bits.mem_valid &&
    !cm1_is_ctrl_not_mret && !cm1_bits.csr_write &&
    (cm1_is_fencei || cm1_is_mret || cm1_bits.is_ebreak || cm1_bits.state.state)
  val cm1_block_bp = cm1_slot_blocked && !cm_exclusive && !cm1_exclusive &&
    (bp_commit1_block || !pair_bpu_ready)

  sq.io.wb_valid := store_direct_fire
  sq.io.wb_rob  := lsu_store_wb.rob_idx
  sq.io.wb_addr := lsu_store_wb.alu_result
  sq.io.wb_data := lsu_store_wb.store_data
  sq.io.wb_mask := store_entry.mem_wmask(3, 0)
  sq.io.wb1_valid := false.B
  sq.io.wb1_rob  := 0.U
  sq.io.wb1_addr := 0.U
  sq.io.wb1_data := 0.U
  sq.io.wb1_mask := 0.U
  val commitStoreMask = VecInit(Seq(cm_fire && cm_is_store, cm1_fire && cm1_is_store,
    cm2_fire && cm2_is_store, cm3_fire && cm3_is_store)).asUInt
  val commitStore0OH = PriorityEncoderOH(commitStoreMask)
  val commitStore1OH = PriorityEncoderOH(commitStoreMask & ~commitStore0OH)
  val commitRobIdx = Seq(rob.io.head, rob.io.commit1_idx, rob.io.commit2_idx, rob.io.commit3_idx)
  sq.io.commit_valid := commitStore0OH.orR
  sq.io.commit_rob := Mux1H(commitStore0OH, commitRobIdx)
  sq.io.commit1_valid := commitStore1OH.orR
  sq.io.commit1_rob := Mux1H(commitStore1OH, commitRobIdx)
  lsu.io.commit0_valid := cm_fire && cm_is_load
  lsu.io.commit0_rob := rob.io.head
  lsu.io.commit1_valid := cm1_fire && cm1_is_load
  lsu.io.commit1_rob := rob.io.commit1_idx
  lsu.io.commit2_valid := cm2_fire && cm2_is_load
  lsu.io.commit2_rob := rob.io.commit2_idx
  lsu.io.commit3_valid := cm3_fire && cm3_is_load
  lsu.io.commit3_rob := rob.io.commit3_idx
  // The sidecar resolves unknown addresses before Store data is ready. The
  // legacy full-Store path remains as a fallback for Stores that never used it.
  val lq_store_resolve0_valid = storeAddrResultValid
  val lq_store_resolve0_rob = storeAddrSidecar.io.result.bits.rob_idx
  val lq_store_resolve0_addr = storeAddrSidecar.io.result.bits.addr
  val lq_store_resolve0_mask = storeAddrSidecar.io.result.bits.mask
  val lq_store_resolve_head = rob.io.head
  val lq_store_resolve1_valid = store_direct_raw && !store_entry.done &&
    sq.io.unresolved_mask(lsu_store_wb.rob_idx)
  val lq_store_resolve1_rob = lsu_store_wb.rob_idx
  val lq_store_resolve1_addr = lsu_store_wb.alu_result
  val lq_store_resolve1_mask = store_entry.mem_wmask(3, 0)
  lsu.io.store_resolve0_valid := lq_store_resolve0_valid
  lsu.io.store_resolve0_rob := lq_store_resolve0_rob
  lsu.io.store_resolve0_addr := lq_store_resolve0_addr
  lsu.io.store_resolve0_mask := lq_store_resolve0_mask
  lsu.io.store_resolve1_valid := lq_store_resolve1_valid
  lsu.io.store_resolve1_rob := lq_store_resolve1_rob
  lsu.io.store_resolve1_addr := lq_store_resolve1_addr
  lsu.io.store_resolve1_mask := lq_store_resolve1_mask
  lsu.io.store_resolve_head := lq_store_resolve_head

  private def connectPerformanceCounters(): Unit = {
  if (conf.statistics) {
    val commitCount = PopCount(Seq(cm_fire, cm1_fire, cm2_fire, cm3_fire))
    PM(conf, clock, EVENT_COMMIT, commitCount, commitCount =/= 0.U)
    PM(conf, clock, EVENT_ROB_FULL, 1.U, rob_full_stall)
    PM(conf, clock, EVENT_FL_EMPTY, 1.U, fl_stall)
    PM(conf, clock, EVENT_RS_FULL, 1.U, rs_full_stall)
    PM(conf, clock, EVENT_FQ_FULL, 1.U, fq_full_stall)
    PM(conf, clock, EVENT_FQ_EMPTY, 1.U, !fq.io.deq.valid && !flush_now && !stop_issue)
    PM(conf, clock, EVENT_CDB_CONFLICT, 1.U, wb_conflict)
    PM(conf, clock, EVENT_CDB_BLOCKED, 1.U, wb_blocked)
    PM(conf, clock, EVENT_ALU1_ISSUE, 1.U, take_alu1_issue)
    PM(conf, clock, EVENT_DUAL_ALU_ISSUE, 1.U,
      take_alu_issue && take_alu1_issue)
    val fuRefillCount = PopCount(Seq(
      alu_leave && take_alu_issue,
      alu1_leave && take_alu1_issue,
      alu2_leave && take_alu2_issue,
      alu3_leave && take_alu3_issue,
      div_leave && take_div_issue))
    PM(conf, clock, EVENT_FU_REFILL, fuRefillCount, fuRefillCount =/= 0.U)
    def cdbWakes(e: RSEntry): Bool = {
      val wake0 = rs.io.cdb_valid && (rs.io.cdb_pdest =/= 0.U) &&
        ((e.src1_phys === rs.io.cdb_pdest) || (e.src2_phys === rs.io.cdb_pdest))
      val wake1 = rs.io.cdb1_valid && (rs.io.cdb1_pdest =/= 0.U) &&
        ((e.src1_phys === rs.io.cdb1_pdest) || (e.src2_phys === rs.io.cdb1_pdest))
      val wake2 = rs.io.cdb2_valid && (rs.io.cdb2_pdest =/= 0.U) &&
        ((e.src1_phys === rs.io.cdb2_pdest) || (e.src2_phys === rs.io.cdb2_pdest))
      val wake3 = rs.io.cdb3_valid && (rs.io.cdb3_pdest =/= 0.U) &&
        ((e.src1_phys === rs.io.cdb3_pdest) || (e.src2_phys === rs.io.cdb3_pdest))
      wake0 || wake1 || wake2 || wake3
    }
    val wakeIssueCount = PopCount(Seq(
      take_alu_issue && cdbWakes(rs.io.issue_alu_bits),
      take_alu1_issue && cdbWakes(rs.io.issue_alu1_bits),
      take_alu2_issue && cdbWakes(rs.io.issue_alu2_bits),
      take_alu3_issue && cdbWakes(rs.io.issue_alu3_bits),
      take_bru_issue && cdbWakes(brq.io.issue.bits),
      take_div_issue && cdbWakes(rs.io.issue_div_bits),
      take_lsu_issue && cdbWakes(rs.io.issue_lsu_bits),
      take_lsu1_issue && cdbWakes(rs.io.issue_lsu1_bits)))
    PM(conf, clock, EVENT_RS_CDB_WAKE_ISSUE, wakeIssueCount, wakeIssueCount =/= 0.U)
    PM(conf, clock, EVENT_RS_FRESH_ISSUE, rs.io.fresh_issue_count,
      rs.io.fresh_issue_count =/= 0.U)
    val lsuAddrRefillCount = PopCount(Seq(
      lsu_addr_leave && take_lsu_issue,
      lsu_addr1_leave && take_lsu1_issue))
    PM(conf, clock, EVENT_LSU_ADDR_REFILL, lsuAddrRefillCount,
      lsuAddrRefillCount =/= 0.U)
    PM(conf, clock, EVENT_DISPATCH_SLOT1, 1.U, en_ren1)
    PM(conf, clock, EVENT_DISPATCH_SLOT2, 1.U, en_ren2)
    PM(conf, clock, EVENT_DISPATCH_SLOT3, 1.U, en_ren3)
    PM(conf, clock, EVENT_DISPATCH_BRANCH_SLOT1, 1.U, en_ren1 && id0_is_cond_branch)
    PM(conf, clock, EVENT_DISPATCH_SLOT1_CTRL_BLOCK, 1.U, lane1CtrlBlocked)
    PM(conf, clock, EVENT_DISPATCH_SLOT1_BACKEND_BLOCK, 1.U, lane1BackendBlocked)
    PM(conf, clock, EVENT_WB_AGE_REORDER, 1.U, wb_age_reorder)
    PM(conf, clock, EVENT_COMMIT_HEAD_WAIT, 1.U, head_e.valid && !rob.io.commit_valid)
    val headWait = head_e.valid && !rob.io.commit_valid
    val headWaitLoad = headWait && head_e.mem_valid && !head_e.mem_write
    val headWaitStore = headWait && head_e.mem_valid && head_e.mem_write
    val headWaitCtrl = headWait && !head_e.mem_valid && head_e.jump =/= JUMP_NONE
    val headWaitAlu = headWait && !head_e.mem_valid && head_e.jump === JUMP_NONE &&
      !head_e.csr_write && !head_e.is_fencei && !head_e.is_ebreak && !head_e.state.state
    val headWaitOther = headWait && !(headWaitLoad || headWaitStore || headWaitCtrl || headWaitAlu)
    PM(conf, clock, EVENT_HEAD_WAIT_ALU, 1.U, headWaitAlu)
    PM(conf, clock, EVENT_HEAD_WAIT_LOAD, 1.U, headWaitLoad)
    PM(conf, clock, EVENT_HEAD_WAIT_STORE, 1.U, headWaitStore)
    PM(conf, clock, EVENT_HEAD_WAIT_CTRL, 1.U, headWaitCtrl)
    PM(conf, clock, EVENT_HEAD_WAIT_OTHER, 1.U, headWaitOther)
    PMPC(conf, clock, PC_EVENT_HEAD_WAIT, head_e.pc, headWait)
    val branchIssue = take_bru_issue
    PM(conf, clock, EVENT_BRANCH_ISSUE, 1.U, branchIssue)
    PM(conf, clock, EVENT_BRANCH_READY_WAIT, 1.U, brq.io.issue.valid && !branchIssue)
    PM(conf, clock, EVENT_COMMIT_WAIT_STORE, 1.U, rob.io.commit_valid && cm_is_store && !store_commit_ready)
    PM(conf, clock, EVENT_COMMIT_WAIT_FENCE, 1.U, rob.io.commit_valid && cm_is_fencei && !fencei_commit_ready)
    PM(conf, clock, EVENT_COMMIT_WAIT_BP, 1.U, rob.io.commit_valid && bp_commit_block)
    PM(conf, clock, EVENT_COMMIT_WAIT_FLUSH, 1.U, rob.io.commit_valid && (is_irq_early || fencei_flush || mret_flush || ext_irq_flush))
    PM(conf, clock, EVENT_COMMIT_SLOT0, 1.U, cm_fire)
    PM(conf, clock, EVENT_COMMIT_SLOT1, 1.U, cm1_fire)
    PM(conf, clock, EVENT_COMMIT_SLOT2, 1.U, cm2_fire)
    PM(conf, clock, EVENT_COMMIT_SLOT3, 1.U, cm3_fire)
    PM(conf, clock, EVENT_COMMIT2, 1.U, cm_fire && cm1_fire)
    PM(conf, clock, EVENT_COMMIT4, 1.U, cm3_fire)
    PM(conf, clock, EVENT_QUAD_ALU_ISSUE, 1.U,
      take_alu_issue && take_alu1_issue && take_alu2_issue && take_alu3_issue)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK, 1.U,
      cm1_slot_blocked)
    PM(conf, clock, EVENT_COMMIT_SLOT1_NOT_READY, 1.U, cm1_slot_not_ready)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_SLOT0_EXCL, 1.U, cm1_block_slot0_excl)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_MEM, 1.U, cm1_block_mem)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_CTRL, 1.U, cm1_block_ctrl)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_CSR, 1.U, cm1_block_csr)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_SPECIAL, 1.U, cm1_block_special)
    PM(conf, clock, EVENT_COMMIT_SLOT1_BLOCK_BP, 1.U, cm1_block_bp)
    PM(conf, clock, EVENT_COMMIT_SLOT1_LOAD, 1.U, cm1_fire && cm1_is_load)
    PM(conf, clock, EVENT_COMMIT_SLOT1_CTRL, 1.U, cm1_fire && cm1_is_ctrl_not_mret)
    PM(conf, clock, EVENT_COMMIT_SLOT1_STORE, 1.U, cm1_fire && cm1_is_store)
    PM(conf, clock, EVENT_BPU_UPDATE_QUEUE_BLOCK, 1.U,
      (rob.io.commit_valid && cm_bpu_update && !cm_bpu_ready) ||
        (cm1_slot_available && cm1_bpu_update && !pair_bpu_ready))
    PM(conf, clock, EVENT_FQ_ENQ2, 1.U,
      fq.io.enq.fire && fq.io.enq.bits.valid(0) && fq.io.enq.bits.valid(1))
    PM(conf, clock, EVENT_FQ_SPACE_ONE, 1.U,
      ifu.io.out.valid && ifu.io.out.bits.valid(0) && fq.io.space === 1.U)
    PM(conf, clock, EVENT_CONTROL_DIRECT_COMPLETE, 1.U, ctrl_direct_fire)
    PM(conf, clock, EVENT_STORE_DIRECT_COMPLETE, 1.U, store_direct_fire)
    val primaryStoreIssue = take_lsu_issue && rs.io.issue_lsu_bits.lsu_mem_write
    val storeAddrCandidate = rs.io.store_addr_candidate_count =/= 0.U
    PM(conf, clock, EVENT_STORE_ADDR_CANDIDATE_CYCLE, 1.U, storeAddrCandidate)
    PM(conf, clock, EVENT_STORE_ADDR_DATA_WAIT_SLOT,
      rs.io.store_addr_data_wait_count, rs.io.store_addr_data_wait_count =/= 0.U)
    PM(conf, clock, EVENT_STORE_DATA_ADDR_WAIT_SLOT,
      rs.io.store_data_addr_wait_count, rs.io.store_data_addr_wait_count =/= 0.U)
    PM(conf, clock, EVENT_STORE_READY_BLOCKED_CYCLE, 1.U,
      !stop_issue && rs.io.store_ready_count =/= 0.U && !primaryStoreIssue)
    PM(conf, clock, EVENT_STORE_ISSUE, 1.U, primaryStoreIssue)
    PM(conf, clock, EVENT_STORE_ADDR_DUAL_LOAD_OPPORTUNITY, 1.U,
      storeAddrCandidate && take_lsu_issue && !rs.io.issue_lsu_bits.lsu_mem_write &&
        take_lsu1_issue)
    PM(conf, clock, EVENT_STORE_ADDR_SIDECAR_ISSUE, 1.U, take_store_addr_issue)
    PM(conf, clock, EVENT_STORE_ADDR_SIDECAR_RESOLVE, 1.U, storeAddrResultValid)
  }
  }
  connectPerformanceCounters()

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
  rename.io.cm1_cp_valid := cm1_fire && cm1_is_ctrl_not_mret
  rename.io.cm1_cp_idx   := cm1_bits.cp_idx
  rename.io.commit2_fire := cm2_fire
  rename.io.cm2_do_ren   := cm2_do_ren
  rename.io.cm2_old_phys := cm2_bits.old_phys
  rename.io.cm2_new_phys := cm2_bits.new_phys
  rename.io.cm2_arch_rd  := cm2_bits.arch_rd
  rename.io.cm2_cp_valid := cm2_fire && cm2_is_ctrl_not_mret
  rename.io.cm2_cp_idx   := cm2_bits.cp_idx
  rename.io.commit3_fire := cm3_fire
  rename.io.cm3_do_ren   := cm3_do_ren
  rename.io.cm3_old_phys := cm3_bits.old_phys
  rename.io.cm3_new_phys := cm3_bits.new_phys
  rename.io.cm3_arch_rd  := cm3_bits.arch_rd
  rename.io.cm3_cp_valid := cm3_fire && cm3_is_ctrl_not_mret
  rename.io.cm3_cp_idx   := cm3_bits.cp_idx

  val cm_wb_same = cm_fire && head_wb_valid
  val cm1_wb_same = cm1_fire && cm1_wb_valid
  val cm2_wb_same = cm2_fire && sameCycleWb(rob.io.commit2_idx)
  val cm3_wb_same = cm3_fire && sameCycleWb(rob.io.commit3_idx)
  when(cm_do_ren) {
    arch_rf(cm_bits.arch_rd) := Mux(cm_wb_same, head_wb_val, cm_bits.dest_val)
  }
  when(cm1_do_ren) {
    arch_rf(cm1_bits.arch_rd) := Mux(cm1_wb_same, cm1_wb_val, cm1_bits.dest_val)
  }
  when(cm2_do_ren) {
    arch_rf(cm2_bits.arch_rd) := Mux(cm2_wb_same,
      sameCycleWbValue(rob.io.commit2_idx), cm2_bits.dest_val)
  }
  when(cm3_do_ren) {
    arch_rf(cm3_bits.arch_rd) := Mux(cm3_wb_same,
      sameCycleWbValue(rob.io.commit3_idx), cm3_bits.dest_val)
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
  val head_state = cm_state
  irq_commit := cm_fire && head_state.state
  val irq_commit_r = RegNext(irq_commit, false.B)
  val irq_rob_r    = RegEnable(rob.io.head, 0.U, irq_commit)
  // irq_mtvec / csr.irq / is_irq 在 4g 采样定稿后赋值（见下方）
  val is_irq = Wire(Bool())
  is_irq_early := is_irq
  is_irq_w := is_irq
  val wb_state_write = Mux(can_wb && wbu.io.in.bits.state.state,
    wbu.io.in.bits.state,
    Mux(can_wb1 && wbu1.io.in.bits.state.state, wbu1.io.in.bits.state,
      Mux(can_wb2 && wbu2.io.in.bits.state.state, wbu2.io.in.bits.state,
        Mux(can_wb3, wbu3.io.in.bits.state, wbu.io.in.bits.state))))
  val state_reg = RegEnable(wb_state_write, 0.U.asTypeOf(new State),
    can_wb || can_wb1 || can_wb2 || can_wb3)
  ifu.io.state := state_reg
  idu.io.state := state_reg
  idu1.io.state := state_reg
  idu2.io.state := state_reg
  idu3.io.state := state_reg

  // Mispredict detection is owned by the independent BRU path.
  val bru_resolve_pc = exu_bru.io.pc.bits
  val pc_src = bru_resolve_pc.pc_src
  val correct_pc = MuxLookup(pc_src, bru_resolve_pc.pc4)(Seq(
    PC_PLUS4 -> bru_resolve_pc.pc4,
    PC_IMM   -> bru_resolve_pc.pc4_imm,
    PC_RS2   -> bru_resolve_pc.pc4_rs2,
    MEPC     -> csr.io.read.mepc
  ))
  // 直接根据 EXU 当前结果判定 mispred；避免等 leave 一拍后再冲刷年轻指令
  val br_done = bru_resolve_valid
  val br_resolve = d_bru_valid && !d_bru_ctrl_resolved
  when(br_resolve && !bru_leave && !flush_bru) {
    d_bru_ctrl_resolved := true.B
  }
  val is_ch    = br_done && (pc_src =/= PC_PLUS4)
  val predict_taken = bru_resolve_bits.bp_taken
  val target_mispredict = is_ch && predict_taken && (bru_resolve_bits.bp_target =/= correct_pc)
  val mis_predict = br_resolve && ((is_ch =/= predict_taken) || target_mispredict)
  val mis_predict_dbg = Wire(Bool())
  mis_predict_dbg := mis_predict
  dontTouch(mis_predict_dbg)
  val mis_predict_r = RegNext(mis_predict, false.B)
  val mis_rob_r     = RegEnable(bru_resolve_bits.rob_idx, 0.U, mis_predict)
  val mis_cp_r      = RegEnable(bru_resolve_bits.cp_idx, 0.U, mis_predict)
  val correct_pc_r  = RegNext(correct_pc, 0.U)
  val mis_next_rob_r = Mux(mis_rob_r === (OoOParams.ROB_SIZE - 1).U, 0.U, mis_rob_r + 1.U)
  mis_predict_w := mis_predict
  mis_rob_w     := bru_resolve_bits.rob_idx

  val branchRecoveryWins = mis_predict_dbg &&
    (!mem_violation_w || (robAge(bru_resolve_bits.rob_idx, rob.io.head) < robAge(mem_violation_rob_w, rob.io.head)))
  val memoryRecoveryWins = mem_violation_w &&
    (!mis_predict_dbg || (robAge(mem_violation_rob_w, rob.io.head) <= robAge(bru_resolve_bits.rob_idx, rob.io.head)))
  is_bp_flush := branchRecoveryWins
  is_mem_flush := memoryRecoveryWins
  mem_flush_all := is_mem_flush && (rob.io.head === mem_violation_rob_w)
  val recoverJump = bru_resolve_bits.signals.exu.jump
  val recoverIsBranch = recoverJump === JUMP_BEQ || recoverJump === JUMP_BNE ||
    recoverJump === JUMP_BLT || recoverJump === JUMP_BGE ||
    recoverJump === JUMP_BLTU || recoverJump === JUMP_BGEU
  val recoverRdRa = bru_resolve_bits.inst(11, 7) === 1.U
  val recoverRs1Ra = bru_resolve_bits.inst(19, 15) === 1.U
  ifu.io.bp_recover_valid := is_bp_flush
  ifu.io.bp_recover_ftq_idx := bru_resolve_bits.ftq_idx
  ifu.io.bp_recover_ftq_generation := bru_resolve_bits.ftq_generation
  ifu.io.bp_recover_pc := bru_resolve_bits.pc
  ifu.io.bp_recover_index := bru_resolve_bits.bp_index
  ifu.io.bp_recover_target := correct_pc
  ifu.io.bp_recover_taken := is_ch
  ifu.io.bp_recover_is_branch := recoverIsBranch
  ifu.io.bp_recover_is_jalr := recoverJump === JUMP_JALR
  ifu.io.bp_recover_is_call := recoverRdRa &&
    ((recoverJump === JUMP_JAL) || (recoverJump === JUMP_JALR && !recoverRs1Ra))
  ifu.io.bp_recover_is_ret := (recoverJump === JUMP_JALR) && recoverRs1Ra
  bp_commit_block := mis_predict_r && (rob.io.commit_idx === mis_next_rob_r)
  bp_commit1_block := mis_predict_r && (rob.io.commit1_idx === mis_next_rob_r)
  bp_commit2_block := mis_predict_r && (rob.io.commit2_idx === mis_next_rob_r)
  bp_commit3_block := mis_predict_r && (rob.io.commit3_idx === mis_next_rob_r)
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
  val head_is_exc_gap = head_e.valid && cm_state.state
  val commit_gap = !head_would_cm && !head_is_exc_gap &&
    !mis_predict && !mis_predict_r && !mem_violation_w && !fencei_flush && !mret_flush && !cm_writing_gap &&
    !irq_commit_r && !ext_irq_flush
  ext_irq_fire := io.interrupt && commit_gap
  ext_irq_flush := RegNext(ext_irq_fire, false.B)
  val rob_nonempty = rob.io.count =/= 0.U
  // With an empty ROB, preserve the oldest instruction already buffered by the frontend.
  val frontend_mepc = Mux(fq.io.deq.valid, fq.io.deq.bits.pc,
    Mux(ifu.io.out.valid && ifu.io.out.bits.valid(0), ifu.io.out.bits.bits(0).pc,
      ifu.io.in.bits.next_pc))
  val ext_mepc = Mux(rob_nonempty, head_e.pc, frontend_mepc)
  is_irq := irq_commit_r || ext_irq_flush
  val irq_mtvec_r = RegEnable(csr.io.read.mtvec, 0.U, irq_commit || ext_irq_fire)
  // 优先级：项内 exception > 正常 commit > 外部中断间隙
  csr.io.irq    := irq_commit || ext_irq_fire
  csr.io.irq_no := Mux(irq_commit, head_state.state_num, IRQ_MEXT)
  csr.io.irq_pc := Mux(irq_commit, cm_bits.pc, ext_mepc)

  ifu.io.correct_pc := Mux(is_irq, irq_mtvec_r,
    Mux(is_mem_flush, mem_violation_pc_w,
      Mux(is_bp_flush, correct_pc,
      Mux(fencei_flush, fencei_pc_r + 4.U,
        Mux(mret_flush, mret_mepc_r, 0.U)))))
  // ROB/RS 注册拍结构冲刷；组合 mispred/irq/fencei/mret/ext_irq commit 停 issue
  flush_now := is_bp_flush || is_mem_flush || is_irq || fencei_flush || mret_flush
  stop_issue := flush_now || mis_predict || mem_violation_w || irq_commit || fencei_commit || mret_commit ||
    ext_irq_fire
  fwd_ok := !mis_predict && !mis_predict_r && !mem_violation_w && !is_irq && !irq_commit &&
    !fencei_commit && !fencei_flush && !mret_commit && !mret_flush &&
    !ext_irq_fire && !ext_irq_flush
  val mem_violation_keep_idx = (mem_violation_rob_w - 1.U)(OoOParams.ROB_PTR_W - 1, 0)
  flush_idx := Mux(is_irq, irq_rob_r,
    Mux(is_mem_flush, mem_violation_keep_idx,
      Mux(is_bp_flush, bru_resolve_bits.rob_idx, mis_rob_r)))

  ifu.io.is_flush := is_irq || fencei_flush || mret_flush || is_bp_flush || is_mem_flush
  idu.io.is_flush := is_irq || fencei_flush || mret_flush || is_bp_flush || is_mem_flush || mis_predict ||
    irq_commit || fencei_commit || mret_commit || ext_irq_fire
  idu1.io.is_flush := idu.io.is_flush
  idu2.io.is_flush := idu.io.is_flush
  idu3.io.is_flush := idu.io.is_flush

  // LSU/WBU 仅注册拍 irq（selective flush 会丢已 leave 指令）

  val selective_flush = flush_now && !is_irq && !fencei_flush && !mret_flush && !mem_flush_all
  rob.io.flush     := selective_flush
  rob.io.flush_idx := flush_idx
  rob.io.flush_all := is_irq || fencei_flush || mret_flush || mem_flush_all
  rs.io.flush      := flush_now && !fencei_flush && !mret_flush
  rs.io.flush_idx  := flush_idx
  rs.io.flush_all  := is_irq || fencei_flush || mret_flush || mem_flush_all
  brq.io.flush := flush_now && !fencei_flush && !mret_flush
  brq.io.flushIdx := flush_idx
  brq.io.flushAll := is_irq || fencei_flush || mret_flush || mem_flush_all
  sq.io.flush      := selective_flush
  sq.io.flush_idx  := flush_idx
  sq.io.flush_all  := is_irq || fencei_flush || mret_flush || mem_flush_all
  lsu.io.flush     := selective_flush
  lsu.io.flush_idx := flush_idx
  lsu.io.flush_all := is_irq || fencei_flush || mret_flush || mem_flush_all

  // flush 重建 RAT/free
  val cm_this = cm_fire && rob.io.entries(rob.io.head).valid
  val cm1_this = cm1_fire && rob.io.entries(rob.io.commit1_idx).valid
  val cm2_this = cm2_fire && rob.io.entries(rob.io.commit2_idx).valid
  val cm3_this = cm3_fire && rob.io.entries(rob.io.commit3_idx).valid
  val cm_count = PopCount(Seq(cm_this, cm1_this, cm2_this, cm3_this))
  val rb_head = (rob.io.head + cm_count)(OoOParams.ROB_PTR_W - 1, 0)
  val rb_empty = is_irq || fencei_flush || mret_flush || mem_flush_all ||
    (cm_this && (rob.io.head === flush_idx)) ||
    (cm1_this && (rob.io.commit1_idx === flush_idx)) ||
    (cm2_this && (rob.io.commit2_idx === flush_idx)) ||
    (cm3_this && (rob.io.commit3_idx === flush_idx))
  val kept_n = Mux(rb_empty, 0.U, Mux(flush_idx >= rb_head,
    (flush_idx - rb_head) + 1.U,
    (OoOParams.ROB_SIZE.U - rb_head) + flush_idx + 1.U))
  dontTouch(rb_head)
  dontTouch(kept_n)

  val rb_base = Wire(Vec(32, UInt(OoOParams.PHYS_W.W)))
  for (i <- 0 until 32) {
    val cmHit = cm_do_ren && (i.U === cm_bits.arch_rd) && (i.U =/= 0.U)
    val cm1Hit = cm1_do_ren && (i.U === cm1_bits.arch_rd) && (i.U =/= 0.U)
    val cm2Hit = cm2_do_ren && (i.U === cm2_bits.arch_rd) && (i.U =/= 0.U)
    val cm3Hit = cm3_do_ren && (i.U === cm3_bits.arch_rd) && (i.U =/= 0.U)
    rb_base(i) := Mux(cm3Hit, cm3_bits.new_phys,
      Mux(cm2Hit, cm2_bits.new_phys,
        Mux(cm1Hit, cm1_bits.new_phys,
          Mux(cmHit, cm_bits.new_phys, rename.io.arch_rat_out(i)))))
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
  dontTouch(rb_rat(1))

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

  val ratReserve = rename.io.rat_out.zipWithIndex.map { case (phys, arch) =>
    Mux(arch.U =/= 0.U && phys =/= 0.U,
      1.U(OoOParams.N_PHYS.W) << phys, 0.U(OoOParams.N_PHYS.W))
  }.reduce(_ | _)
  val robReserve = rob.io.entries.map { e =>
    val newPhys = Mux(e.valid && e.reg_write && e.new_phys =/= 0.U,
      1.U(OoOParams.N_PHYS.W) << e.new_phys, 0.U(OoOParams.N_PHYS.W))
    val oldPhys = Mux(e.valid && e.reg_write && e.old_phys =/= 0.U,
      1.U(OoOParams.N_PHYS.W) << e.old_phys, 0.U(OoOParams.N_PHYS.W))
    newPhys | oldPhys
  }.reduce(_ | _)
  rename.io.reserve_mask := ratReserve | robReserve

  // Branch recovery uses the same ROB-derived reconstruction as precise
  // exceptions.  A checkpoint can carry a younger physical mapping when a
  // four-wide packet crosses a redirect boundary; the live ROB is the source
  // of truth for the instructions that must survive the flush.
  rename.io.rebuild      := is_irq || fencei_flush || mret_flush || is_mem_flush || is_bp_flush
  rename.io.rebuild_rat  := rb_rat
  rename.io.rebuild_free := rb_free
  rename.io.restore_cp   := false.B
  rename.io.restore_cp_idx := Mux(is_bp_flush, bru_resolve_bits.cp_idx, mis_cp_r)
  rename.io.restore_do_rename := is_bp_flush && bru_resolve_bits.do_rename &&
    (bru_resolve_bits.waddr =/= 0.U) && (bru_resolve_bits.pdest =/= 0.U)
  rename.io.restore_arch_rd := bru_resolve_bits.waddr
  rename.io.restore_new_phys := bru_resolve_bits.pdest
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
    Mux(can_wb1 && (wb1_pdest =/= 0.U), 1.U(OoOParams.N_PHYS.W) << wb1_pdest, 0.U) |
    Mux(can_wb2 && (wb2_pdest =/= 0.U), 1.U(OoOParams.N_PHYS.W) << wb2_pdest, 0.U) |
    Mux(can_wb3 && (wb3_pdest =/= 0.U), 1.U(OoOParams.N_PHYS.W) << wb3_pdest, 0.U)
  val busyRebuild = busyKeep.reduce(_ | _) & ~wbClearMask
  val robEmpty = rob.io.count === 0.U
  val busySweep = robEmpty && !en_ren && !en_ren1 && !en_ren2 && !en_ren3
  // 4c：异常整表清空 → busy 也清空（勿按旧 flush_idx 选择性保留）
  busy.io.rebuild      := flush_now || busySweep
  busy.io.rebuild_mask := Mux(is_irq || fencei_flush || mret_flush || mem_flush_all, 0.U,
    Mux(flush_now, busyRebuild, 0.U))
  busy.io.clr_mask     := Mux(flush_now || busySweep, 0.U, killBusy)

  // Route the oldest two control-flow commits to the two-entry BPU update queue.
  val ctrlUpdateMask = VecInit(Seq(cm_fire && cm_bpu_update,
    cm1_fire && cm1_bpu_update, cm2_fire && cm2_bpu_update,
    cm3_fire && cm3_bpu_update)).asUInt
  val ctrlUpdate0OH = PriorityEncoderOH(ctrlUpdateMask)
  val ctrlUpdate1OH = PriorityEncoderOH(ctrlUpdateMask & ~ctrlUpdate0OH)
  val commitEntries = Seq(cm_bits, cm1_bits, cm2_bits, cm3_bits)
  val commitTaken = Seq(cm_actual_taken, cm1_actual_taken, cm2_actual_taken, cm3_actual_taken)
  val commitTargets = Seq(cm_actual_target, cm1_actual_target, cm2_actual_target, cm3_actual_target)
  val bpu0 = Mux1H(ctrlUpdate0OH, commitEntries)
  val bpu1 = Mux1H(ctrlUpdate1OH, commitEntries)
  val bpu0Taken = Mux1H(ctrlUpdate0OH, commitTaken)
  val bpu1Taken = Mux1H(ctrlUpdate1OH, commitTaken)
  val bpu0Target = Mux1H(ctrlUpdate0OH, commitTargets)
  val bpu1Target = Mux1H(ctrlUpdate1OH, commitTargets)
  def conditionalBranch(jump: UInt): Bool = jump === JUMP_BEQ || jump === JUMP_BNE ||
    jump === JUMP_BLT || jump === JUMP_BGE || jump === JUMP_BLTU || jump === JUMP_BGEU
  val bpu0RdRa = bpu0.inst(11, 7) === 1.U
  val bpu0Rs1Ra = bpu0.inst(19, 15) === 1.U
  val bpu1RdRa = bpu1.inst(11, 7) === 1.U
  val bpu1Rs1Ra = bpu1.inst(19, 15) === 1.U
  ifu.io.bpu_update_valid := ctrlUpdate0OH.orR
  ifu.io.bpu_update_taken := bpu0Taken
  ifu.io.bpu_update_pc := bpu0.pc
  ifu.io.bpu_update_target := bpu0Target
  ifu.io.bpu_update_is_branch := ctrlUpdate0OH.orR && conditionalBranch(bpu0.jump)
  ifu.io.bpu_update_is_jalr := ctrlUpdate0OH.orR && bpu0.jump === JUMP_JALR
  ifu.io.bpu_update_index := bpu0.bp_index
  ifu.io.bpu_update_is_call := ctrlUpdate0OH.orR && bpu0RdRa &&
    (bpu0.jump === JUMP_JAL || (bpu0.jump === JUMP_JALR && !bpu0Rs1Ra))
  ifu.io.bpu_update_is_ret := ctrlUpdate0OH.orR && bpu0.jump === JUMP_JALR && bpu0Rs1Ra
  ifu.io.bpu_update1_valid := ctrlUpdate1OH.orR
  ifu.io.bpu_update1_taken := bpu1Taken
  ifu.io.bpu_update1_pc := bpu1.pc
  ifu.io.bpu_update1_target := bpu1Target
  ifu.io.bpu_update1_is_branch := ctrlUpdate1OH.orR && conditionalBranch(bpu1.jump)
  ifu.io.bpu_update1_is_jalr := ctrlUpdate1OH.orR && bpu1.jump === JUMP_JALR
  ifu.io.bpu_update1_index := bpu1.bp_index
  ifu.io.bpu_update1_is_call := ctrlUpdate1OH.orR && bpu1RdRa &&
    (bpu1.jump === JUMP_JAL || (bpu1.jump === JUMP_JALR && !bpu1Rs1Ra))
  ifu.io.bpu_update1_is_ret := ctrlUpdate1OH.orR && bpu1.jump === JUMP_JALR && bpu1Rs1Ra
  if (conf.statistics) {
    PM(conf, clock, EVENT_BPU_PREDICT, 1.U, br_resolve && bru_resolve_bits.bp_valid)
    PM(conf, clock, EVENT_BPU_MISPRED, 1.U, mis_predict && bru_resolve_bits.bp_valid)
    PM(conf, clock, EVENT_BP_FLUSH, 1.U, mis_predict)
    PM(conf, clock, EVENT_BPU_DIR_MISPRED, 1.U,
      mis_predict && bru_resolve_bits.bp_valid && (is_ch =/= predict_taken))
    PM(conf, clock, EVENT_BPU_TARGET_MISPRED, 1.U,
      mis_predict && bru_resolve_bits.bp_valid && target_mispredict)
    PM(conf, clock, EVENT_BPU_UNPREDICTED, 1.U, mis_predict && !bru_resolve_bits.bp_valid)
    PMPC(conf, clock, PC_EVENT_BP_FLUSH, bru_resolve_bits.pc, mis_predict)
    PMPC(conf, clock, PC_EVENT_DIR_MISPRED, bru_resolve_bits.pc,
      mis_predict && bru_resolve_bits.bp_valid && (is_ch =/= predict_taken))
    PMPC(conf, clock, PC_EVENT_TARGET_MISPRED, bru_resolve_bits.pc,
      mis_predict && bru_resolve_bits.bp_valid && target_mispredict)
    PMPC(conf, clock, PC_EVENT_UNPREDICTED, bru_resolve_bits.pc,
      mis_predict && !bru_resolve_bits.bp_valid)
  }

  ifu.io.imem <> icache.io.in
  icache.io.out <> io.imem
  // 4f：fencei 改由 ROB head commit 驱动；IDU 不再直连 icache
  idu.io.ifu_signals.ready := true.B
  idu.io.csr <> csr.io.read
  idu1.io.ifu_signals.ready := true.B
  idu1.io.csr <> csr.io.read1
  idu2.io.ifu_signals.ready := true.B
  idu2.io.csr.rdata := 0.U
  idu2.io.csr.mtvec := csr.io.read.mtvec
  idu2.io.csr.mepc := csr.io.read.mepc
  idu3.io.ifu_signals.ready := true.B
  idu3.io.csr.rdata := 0.U
  idu3.io.csr.mtvec := csr.io.read.mtvec
  idu3.io.csr.mepc := csr.io.read.mepc
  // 4b：CSR 写口改由 commit 驱动；WBU.csr 闲置（wen=0）

  // ---------- 10a：StoreBuffer + dmem arbitration ----------
  val s_CM_IDLE :: s_CM_W :: s_CM_B :: s_CM_DONE :: Nil = Enum(4)
  val cm_st_state = RegInit(s_CM_IDLE)
  val cm_st_addr  = RegInit(0.U(32.W))
  val cm_st_wdata = RegInit(0.U(32.W))
  val cm_st_wstrb = RegInit(0.U(4.W))
  val cm_st_awsize = RegInit(0.U(3.W))
  val cm_st_aw_done = RegInit(false.B)
  val cm_st_w_done  = RegInit(false.B)

  val head_wb_store = head_wb_valid && cm_bits.mem_valid && cm_bits.mem_write
  val head_addr_rdy = cm_bits.addr_ready || head_store_wb || head_wb_store
  val head_st_addr  = Mux(head_store_wb, lsu_store_wb.alu_result,
    Mux(head_wb_store, head_wb_bits.alu_result, cm_bits.mem_addr))
  val head_st_data  = Mux(head_store_wb, lsu_store_wb.store_data,
    Mux(head_wb_store, head_wb_bits.store_data, cm_bits.mem_wdata))
  val cm1_wb_store = cm1_wb_valid && cm1_bits.mem_valid && cm1_bits.mem_write
  val cm1_addr_rdy = cm1_store_addr_ready
  val cm1_st_addr = Mux(cm1_store_wb, lsu_store_wb.alu_result,
    Mux(cm1_wb_store, cm1_wb_bits.alu_result, cm1_bits.mem_addr))
  val cm1_st_data = Mux(cm1_store_wb, lsu_store_wb.store_data,
    Mux(cm1_wb_store, cm1_wb_bits.store_data, cm1_bits.mem_wdata))
  val cm1_st_mask4 = cm1_bits.mem_wmask(3, 0)
  val cm1_st_is_pmem = isPmem(cm1_st_addr)
  val cm2_wb_store = cm2_wb_valid && cm2_is_store
  val cm2_st_addr = cm2_mem_addr
  val cm2_st_data = Mux(cm2_store_wb, lsu_store_wb.store_data,
    Mux(cm2_wb_store, cm2_wb_bits.store_data, cm2_bits.mem_wdata))
  val cm2_st_mask4 = cm2_bits.mem_wmask(3, 0)
  val cm2_st_is_pmem = isPmem(cm2_st_addr)
  val cm3_wb_store = cm3_wb_valid && cm3_is_store
  val cm3_st_addr = cm3_mem_addr
  val cm3_st_data = Mux(cm3_store_wb, lsu_store_wb.store_data,
    Mux(cm3_wb_store, cm3_wb_bits.store_data, cm3_bits.mem_wdata))
  val cm3_st_mask4 = cm3_bits.mem_wmask(3, 0)
  val cm3_st_is_pmem = isPmem(cm3_st_addr)
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
    // A completed younger cacheable load may remain in the LQ until commit;
    // waiting for the whole LSU to become empty would deadlock the head MMIO
    // store.  Only an active DCache transaction owns the external read bus.
    when(head_store_direct && stbuf.io.empty && !stbuf.io.busy &&
        !dcache.io.busy) {
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
      // AXI completion is not architectural retirement. Keep the captured
      // transaction owned until this exact ROB store is allowed to commit.
      cm_st_state := Mux(cm_fire, s_CM_IDLE, s_CM_DONE)
      when(cm_fire) {
        cm_st_aw_done := false.B
        cm_st_w_done := false.B
      }
    }
  }.elsewhen(cm_st_state === s_CM_DONE) {
    when(cm_fire) {
      cm_st_state := s_CM_IDLE
      cm_st_aw_done := false.B
      cm_st_w_done := false.B
    }
  }

  val direct_store_ready = (cm_st_state === s_CM_DONE) ||
    ((cm_st_state === s_CM_B) && io.dmem.bvalid && io.dmem.bready)

  val pmemStoreCommitMask = VecInit(Seq(
    cm_fire && cm_is_store && head_st_is_pmem,
    cm1_fire && cm1_is_store && cm1_st_is_pmem,
    cm2_fire && cm2_is_store && cm2_st_is_pmem,
    cm3_fire && cm3_is_store && cm3_st_is_pmem)).asUInt
  val pmemStore0OH = PriorityEncoderOH(pmemStoreCommitMask)
  val pmemStore1OH = PriorityEncoderOH(pmemStoreCommitMask & ~pmemStore0OH)
  val committedStoreAddrs = Seq(head_st_addr, cm1_st_addr, cm2_st_addr, cm3_st_addr)
  val committedStoreData = Seq(head_st_data, cm1_st_data, cm2_st_data, cm3_st_data)
  val committedStoreMasks = Seq(cm_st_mask4, cm1_st_mask4, cm2_st_mask4, cm3_st_mask4)
  stbuf.io.enq.valid := pmemStore0OH.orR
  stbuf.io.enq.bits.addr := Mux1H(pmemStore0OH, committedStoreAddrs)
  stbuf.io.enq.bits.data := Mux1H(pmemStore0OH, committedStoreData)
  stbuf.io.enq.bits.mask := Mux1H(pmemStore0OH, committedStoreMasks)
  stbuf.io.enq1.valid := pmemStore1OH.orR
  stbuf.io.enq1.bits.addr := Mux1H(pmemStore1OH, committedStoreAddrs)
  stbuf.io.enq1.bits.data := Mux1H(pmemStore1OH, committedStoreData)
  stbuf.io.enq1.bits.mask := Mux1H(pmemStore1OH, committedStoreMasks)

  store_commit_ready := head_addr_rdy &&
    Mux(head_st_is_pmem, stbuf.io.free >= 1.U, direct_store_ready)
  // MMIO must release older StoreBuffer ownership so the shared write channel
  // can make progress. Only explicit architectural boundaries also drain L1.
  val explicitStoreDrain = rob.io.commit_valid &&
    (cm_needs_store_drain || cm_is_fencei)
  val stbufDrainRequest = explicitStoreDrain || head_store_direct
  stbuf.io.drain_all := RegNext(stbufDrainRequest, false.B)
  dcache.io.drain_all := RegNext(explicitStoreDrain, false.B)

  // 4f：head 是 fencei 且可提交时刷 ICache；Irrevocable 保持 valid 直到 ready
  // fencei/mret/ebreak/exception drain committed stores before changing control state.
  val cm_writing = cm_st_state =/= s_CM_IDLE
  // Xbar tracks AXI read and write ownership independently. Cache refills and
  // committed PMEM stores may therefore overlap without weakening MMIO drain.
  stbuf.io.bus_busy := cm_writing
  val sb_writing = stbuf.io.busy
  val store_bus_busy = cm_writing || sb_writing
  store_side_empty := stbuf.io.empty && !stbuf.io.busy &&
    dcache.io.dirty_empty && !cm_writing
  // A normal MMIO read is ordered against the direct/uncached store path, but
  // it need not write back unrelated cacheable dirty lines. Explicit drain
  // boundaries (fence.i, trap/exit, or a direct store) still use drain_all.
  lsu_mmio_ready := !cm_writing
  cm_writing_gap := store_bus_busy || !stbuf.io.empty || !dcache.io.dirty_empty
  val head_fencei_pending = rob.io.commit_valid && !is_irq_early && !ext_irq_fire &&
    cm_is_fencei && store_side_empty
  icache.io.fencei.valid := head_fencei_pending
  icache.io.fencei.bits.is_fencei := true.B
  fencei_commit_ready := store_side_empty && icache.io.fencei.ready

  private def connectDCacheStorePath(): Unit = {
    // dmem read ownership belongs to LSU; committed stores update L1 or retain
    // ordered ownership in the StoreBuffer/writeback queue.
    lsu.io.dmem <> dcache.io.cpu
    lsu.io.dmem1 <> dcache.io.cpu1
    dcache.io.invalidate_valid := false.B
    dcache.io.invalidate_addr := 0.U
    dcache.io.invalidate2_valid := false.B
    dcache.io.invalidate2_addr := 0.U
    dcache.io.invalidate3_valid := false.B
    dcache.io.invalidate3_addr := 0.U
    dcache.io.store_valid := stbuf.io.enq.fire
    dcache.io.store_addr := stbuf.io.enq.bits.addr
    dcache.io.store_data := stbuf.io.enq.bits.data
    dcache.io.store_mask := stbuf.io.enq.bits.mask
    stbuf.io.enq_cache_hit := dcache.io.store_probe_hit
    dcache.io.store2_valid := stbuf.io.enq1.fire
    dcache.io.store2_addr := stbuf.io.enq1.bits.addr
    dcache.io.store2_data := stbuf.io.enq1.bits.data
    dcache.io.store2_mask := stbuf.io.enq1.bits.mask
    stbuf.io.enq1_cache_hit := dcache.io.store2_probe_hit
    dcache.io.store3_valid := stbuf.io.drain_valid
    dcache.io.store3_addr := stbuf.io.drain_addr
    dcache.io.store3_data := stbuf.io.drain_data
    dcache.io.store3_mask := stbuf.io.drain_mask
    dcache.io.store_line.valid := stbuf.io.cache_line.valid
    dcache.io.store_line.bits := stbuf.io.cache_line.bits
    stbuf.io.cache_line.ready := dcache.io.store_line.ready
    stbuf.io.l1_writeback.valid := dcache.io.dirty_writeback.valid
    stbuf.io.l1_writeback.bits := dcache.io.dirty_writeback.bits
    dcache.io.dirty_writeback.ready := stbuf.io.l1_writeback.ready
  }
  connectDCacheStorePath()

  dcache.io.mem.arready := io.dmem.arready
  dcache.io.mem.rdata   := io.dmem.rdata
  dcache.io.mem.rresp   := io.dmem.rresp
  dcache.io.mem.rvalid  := io.dmem.rvalid
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
  io.dmem.arvalid := dcache.io.mem.arvalid
  io.dmem.arid    := dcache.io.mem.arid
  io.dmem.arlen   := dcache.io.mem.arlen
  io.dmem.arsize  := dcache.io.mem.arsize
  io.dmem.arburst := dcache.io.mem.arburst
  io.dmem.rready  := dcache.io.mem.rready

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
    val sbEnqCount = PopCount(Seq(stbuf.io.enq.fire, stbuf.io.enq1.fire))
    PM(conf, clock, EVENT_LSU_WRITE, 1.U, io.dmem.awvalid && io.dmem.awready)
    PM(conf, clock, EVENT_STORE_BUFFER_ENQ, sbEnqCount, sbEnqCount =/= 0.U)
    PM(conf, clock, EVENT_STORE_BUFFER_ENQ2, 1.U,
      stbuf.io.enq.fire && stbuf.io.enq1.fire)
    PM(conf, clock, EVENT_STORE_BUFFER_MERGE, stbuf.io.merged, stbuf.io.merged =/= 0.U)
    PM(conf, clock, EVENT_STORE_BUFFER_WRITE_BURST, 1.U, stbuf.io.write_burst)
    PM(conf, clock, EVENT_STORE_BUFFER_WRITE_BEAT, stbuf.io.write_beats,
      stbuf.io.write_burst)
    PM(conf, clock, EVENT_STORE_BUFFER_CHAIN, 1.U, stbuf.io.write_chain)
    PM(conf, clock, EVENT_STORE_BUFFER_DRAIN, stbuf.io.deq_count,
      stbuf.io.deq_valid)
    PM(conf, clock, EVENT_STORE_BUFFER_FULL, 1.U,
      rob.io.commit_valid && cm_is_store && head_st_is_pmem && stbuf.io.free === 0.U)
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
  io.commit_mem_addr1 := cm1_mem_addr
  io.commit_is_load1  := cm1_bits.reg_write_sel === MEM_SEL
  io.commit_valid2    := cm2_fire
  io.commit_pc2       := cm2_bits.pc
  io.commit_mem_addr2 := cm2_mem_addr
  io.commit_is_load2  := cm2_is_load
  io.commit_valid3    := cm3_fire
  io.commit_pc3       := cm3_bits.pc
  io.commit_mem_addr3 := cm3_mem_addr
  io.commit_is_load3  := cm3_is_load
  for (i <- 0 until 32) {
    io.arch_rdata(i) := Mux(i.U === 0.U, 0.U, arch_rf(i.U))
  }
  prf.io.arch_raddr := rename.io.arch_rat_out
  dontTouch(io.commit_valid)
  dontTouch(io.commit_pc)
  dontTouch(io.commit_valid1)
  dontTouch(io.commit_pc1)
  dontTouch(io.commit_valid2)
  dontTouch(io.commit_pc2)
  dontTouch(io.commit_valid3)
  dontTouch(io.commit_pc3)
  dontTouch(io.arch_rdata)
  dontTouch(rob.io.count)
  dontTouch(rs.io.count)
  dontTouch(fq.io.count)
}
