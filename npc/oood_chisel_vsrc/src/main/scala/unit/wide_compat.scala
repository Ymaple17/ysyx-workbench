package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.{CoreConfig, LSU_WBU_IO, State}

/** Legacy lane names around the vectorized Stage14 PRF. */
class WidePRFCompat(conf: CoreConfig) extends Module {
  private val physW = OoOParams.PHYS_W
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(physW.W)); val rdata1 = Output(UInt(conf.xlen.W))
    val raddr2 = Input(UInt(physW.W)); val rdata2 = Output(UInt(conf.xlen.W))
    val raddr3 = Input(UInt(physW.W)); val rdata3 = Output(UInt(conf.xlen.W))
    val raddr4 = Input(UInt(physW.W)); val rdata4 = Output(UInt(conf.xlen.W))
    val raddr5 = Input(UInt(physW.W)); val rdata5 = Output(UInt(conf.xlen.W))
    val raddr6 = Input(UInt(physW.W)); val rdata6 = Output(UInt(conf.xlen.W))
    val raddr7 = Input(UInt(physW.W)); val rdata7 = Output(UInt(conf.xlen.W))
    val raddr8 = Input(UInt(physW.W)); val rdata8 = Output(UInt(conf.xlen.W))
    val wen1 = Input(Bool()); val waddr1 = Input(UInt(physW.W)); val wdata1 = Input(UInt(conf.xlen.W))
    val wen2 = Input(Bool()); val waddr2 = Input(UInt(physW.W)); val wdata2 = Input(UInt(conf.xlen.W))
    val wen3 = Input(Bool()); val waddr3 = Input(UInt(physW.W)); val wdata3 = Input(UInt(conf.xlen.W))
    val wen4 = Input(Bool()); val waddr4 = Input(UInt(physW.W)); val wdata4 = Input(UInt(conf.xlen.W))
    val arch_raddr = Input(Vec(32, UInt(physW.W)))
    val arch_rdata = Output(Vec(32, UInt(conf.xlen.W)))
  })
  val impl = Module(new WidePRF(conf))
  val readAddr = Seq(io.raddr1, io.raddr2, io.raddr3, io.raddr4,
    io.raddr5, io.raddr6, io.raddr7, io.raddr8)
  val readData = Seq(io.rdata1, io.rdata2, io.rdata3, io.rdata4,
    io.rdata5, io.rdata6, io.rdata7, io.rdata8)
  for (i <- readAddr.indices) {
    impl.io.raddr(i) := readAddr(i)
    readData(i) := impl.io.rdata(i)
  }
  val wen = Seq(io.wen1, io.wen2, io.wen3, io.wen4)
  val waddr = Seq(io.waddr1, io.waddr2, io.waddr3, io.waddr4)
  val wdata = Seq(io.wdata1, io.wdata2, io.wdata3, io.wdata4)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.wen(i) := wen(i)
    impl.io.waddr(i) := waddr(i)
    impl.io.wdata(i) := wdata(i)
  }
  impl.io.arch_raddr := io.arch_raddr
  io.arch_rdata := impl.io.arch_rdata
}

/** Legacy lane names around the vectorized Stage14 scoreboard. */
class WideBusyTableCompat extends Module {
  private val physW = OoOParams.PHYS_W
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(physW.W)); val ready1 = Output(Bool())
    val raddr2 = Input(UInt(physW.W)); val ready2 = Output(Bool())
    val raddr3 = Input(UInt(physW.W)); val ready3 = Output(Bool())
    val raddr4 = Input(UInt(physW.W)); val ready4 = Output(Bool())
    val raddr5 = Input(UInt(physW.W)); val ready5 = Output(Bool())
    val raddr6 = Input(UInt(physW.W)); val ready6 = Output(Bool())
    val raddr7 = Input(UInt(physW.W)); val ready7 = Output(Bool())
    val raddr8 = Input(UInt(physW.W)); val ready8 = Output(Bool())
    val set_en = Input(Bool()); val set_addr = Input(UInt(physW.W))
    val set_en2 = Input(Bool()); val set_addr2 = Input(UInt(physW.W))
    val set_en3 = Input(Bool()); val set_addr3 = Input(UInt(physW.W))
    val set_en4 = Input(Bool()); val set_addr4 = Input(UInt(physW.W))
    val clr_en = Input(Bool()); val clr_addr = Input(UInt(physW.W))
    val clr_en2 = Input(Bool()); val clr_addr2 = Input(UInt(physW.W))
    val clr_en3 = Input(Bool()); val clr_addr3 = Input(UInt(physW.W))
    val clr_en4 = Input(Bool()); val clr_addr4 = Input(UInt(physW.W))
    val clr_mask = Input(UInt(OoOParams.N_PHYS.W))
    val rebuild = Input(Bool())
    val rebuild_mask = Input(UInt(OoOParams.N_PHYS.W))
  })
  val impl = Module(new WideBusyTable())
  val readAddr = Seq(io.raddr1, io.raddr2, io.raddr3, io.raddr4,
    io.raddr5, io.raddr6, io.raddr7, io.raddr8)
  val ready = Seq(io.ready1, io.ready2, io.ready3, io.ready4,
    io.ready5, io.ready6, io.ready7, io.ready8)
  for (i <- readAddr.indices) {
    impl.io.raddr(i) := readAddr(i)
    ready(i) := impl.io.ready(i)
  }
  val setValid = Seq(io.set_en, io.set_en2, io.set_en3, io.set_en4)
  val setAddr = Seq(io.set_addr, io.set_addr2, io.set_addr3, io.set_addr4)
  val clrValid = Seq(io.clr_en, io.clr_en2, io.clr_en3, io.clr_en4)
  val clrAddr = Seq(io.clr_addr, io.clr_addr2, io.clr_addr3, io.clr_addr4)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.set_valid(i) := setValid(i)
    impl.io.set_addr(i) := setAddr(i)
    impl.io.clr_valid(i) := clrValid(i)
    impl.io.clr_addr(i) := clrAddr(i)
  }
  impl.io.clr_mask := io.clr_mask
  impl.io.rebuild := io.rebuild
  impl.io.rebuild_mask := io.rebuild_mask
}

/** Existing lane0/1 contract plus lane2/3, backed by one prefix-aware rename unit. */
class WideRenameCompat extends Module {
  private val physW = OoOParams.PHYS_W
  private val cpW = log2Ceil(OoOParams.CP_DEPTH)
  val io = IO(new Bundle {
    val fire = Input(Bool()); val rs1 = Input(UInt(5.W)); val rs2 = Input(UInt(5.W))
    val rd = Input(UInt(5.W)); val reg_write = Input(Bool())
    val fire0 = Input(Bool()); val rs1_0 = Input(UInt(5.W)); val rs2_0 = Input(UInt(5.W))
    val rd0 = Input(UInt(5.W)); val reg_write0 = Input(Bool()); val is_branch0 = Input(Bool())
    val cp_rob_idx0 = Input(UInt(OoOParams.ROB_PTR_W.W))
    val fire1 = Input(Bool()); val rs1_1 = Input(UInt(5.W)); val rs2_1 = Input(UInt(5.W))
    val rd1 = Input(UInt(5.W)); val reg_write1 = Input(Bool()); val is_branch1 = Input(Bool())
    val cp_rob_idx1 = Input(UInt(OoOParams.ROB_PTR_W.W))
    val fire2 = Input(Bool()); val rs1_2 = Input(UInt(5.W)); val rs2_2 = Input(UInt(5.W))
    val rd2 = Input(UInt(5.W)); val reg_write2 = Input(Bool()); val is_branch2 = Input(Bool())
    val cp_rob_idx2 = Input(UInt(OoOParams.ROB_PTR_W.W))
    val fire3 = Input(Bool()); val rs1_3 = Input(UInt(5.W)); val rs2_3 = Input(UInt(5.W))
    val rd3 = Input(UInt(5.W)); val reg_write3 = Input(Bool()); val is_branch3 = Input(Bool())
    val cp_rob_idx3 = Input(UInt(OoOParams.ROB_PTR_W.W))

    val rs1_phys0 = Output(UInt(physW.W)); val rs2_phys0 = Output(UInt(physW.W))
    val dest_phys0 = Output(UInt(physW.W)); val old_phys0 = Output(UInt(physW.W))
    val do_rename0 = Output(Bool()); val cp_idx0 = Output(UInt(cpW.W))
    val rs1_phys = Output(UInt(physW.W)); val rs2_phys = Output(UInt(physW.W))
    val dest_phys = Output(UInt(physW.W)); val old_phys = Output(UInt(physW.W))
    val do_rename = Output(Bool())
    val rs1_phys1 = Output(UInt(physW.W)); val rs2_phys1 = Output(UInt(physW.W))
    val dest_phys1 = Output(UInt(physW.W)); val old_phys1 = Output(UInt(physW.W))
    val do_rename1 = Output(Bool()); val cp_idx1 = Output(UInt(cpW.W))
    val rs1_phys2 = Output(UInt(physW.W)); val rs2_phys2 = Output(UInt(physW.W))
    val dest_phys2 = Output(UInt(physW.W)); val old_phys2 = Output(UInt(physW.W))
    val do_rename2 = Output(Bool()); val cp_idx2 = Output(UInt(cpW.W))
    val rs1_phys3 = Output(UInt(physW.W)); val rs2_phys3 = Output(UInt(physW.W))
    val dest_phys3 = Output(UInt(physW.W)); val old_phys3 = Output(UInt(physW.W))
    val do_rename3 = Output(Bool()); val cp_idx3 = Output(UInt(cpW.W))

    val commit_fire = Input(Bool()); val cm_do_ren = Input(Bool())
    val cm_old_phys = Input(UInt(physW.W)); val cm_new_phys = Input(UInt(physW.W))
    val cm_arch_rd = Input(UInt(5.W)); val cm_cp_valid = Input(Bool()); val cm_cp_idx = Input(UInt(cpW.W))
    val commit1_fire = Input(Bool()); val cm1_do_ren = Input(Bool())
    val cm1_old_phys = Input(UInt(physW.W)); val cm1_new_phys = Input(UInt(physW.W))
    val cm1_arch_rd = Input(UInt(5.W)); val cm1_cp_valid = Input(Bool()); val cm1_cp_idx = Input(UInt(cpW.W))
    val commit2_fire = Input(Bool()); val cm2_do_ren = Input(Bool())
    val cm2_old_phys = Input(UInt(physW.W)); val cm2_new_phys = Input(UInt(physW.W))
    val cm2_arch_rd = Input(UInt(5.W)); val cm2_cp_valid = Input(Bool()); val cm2_cp_idx = Input(UInt(cpW.W))
    val commit3_fire = Input(Bool()); val cm3_do_ren = Input(Bool())
    val cm3_old_phys = Input(UInt(physW.W)); val cm3_new_phys = Input(UInt(physW.W))
    val cm3_arch_rd = Input(UInt(5.W)); val cm3_cp_valid = Input(Bool()); val cm3_cp_idx = Input(UInt(cpW.W))

    val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
    val restore_arch = Input(Bool()); val restore_cp = Input(Bool())
    val restore_cp_idx = Input(UInt(cpW.W)); val restore_rob_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val restore_do_rename = Input(Bool()); val restore_arch_rd = Input(UInt(5.W))
    val restore_new_phys = Input(UInt(physW.W))
    val rat_out = Output(Vec(32, UInt(physW.W)))
    val arch_rat_out = Output(Vec(32, UInt(physW.W)))
    val fl_empty = Output(Bool()); val free_cnt = Output(UInt(log2Ceil(OoOParams.N_PHYS + 1).W))
    val cp_full = Output(Bool())
    val free_mask = Input(UInt(3.W)); val free_vec = Input(Vec(3, UInt(physW.W)))
    val rebuild = Input(Bool()); val rebuild_rat = Input(Vec(32, UInt(physW.W)))
    val rebuild_free = Input(UInt(OoOParams.N_PHYS.W))
    val rb_fire = Input(Bool()); val rb_do_ren = Input(Bool()); val rb_arch_rd = Input(UInt(5.W))
    val rb_old_phys = Input(UInt(physW.W)); val rb_new_phys = Input(UInt(physW.W))
  })
  val impl = Module(new WideRename(cpDepth = OoOParams.CP_DEPTH))
  val fires = Seq(io.fire0 || io.fire, io.fire1, io.fire2, io.fire3)
  val rs1 = Seq(Mux(io.fire0, io.rs1_0, io.rs1), io.rs1_1, io.rs1_2, io.rs1_3)
  val rs2 = Seq(Mux(io.fire0, io.rs2_0, io.rs2), io.rs2_1, io.rs2_2, io.rs2_3)
  val rd = Seq(Mux(io.fire0, io.rd0, io.rd), io.rd1, io.rd2, io.rd3)
  val regWrite = Seq(Mux(io.fire0, io.reg_write0, io.reg_write), io.reg_write1, io.reg_write2, io.reg_write3)
  val branch = Seq(io.fire0 && io.is_branch0, io.fire1 && io.is_branch1,
    io.fire2 && io.is_branch2, io.fire3 && io.is_branch3)
  val robIdx = Seq(io.cp_rob_idx0, io.cp_rob_idx1, io.cp_rob_idx2, io.cp_rob_idx3)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.fire(i) := fires(i)
    impl.io.rs1(i) := rs1(i); impl.io.rs2(i) := rs2(i); impl.io.rd(i) := rd(i)
    impl.io.reg_write(i) := regWrite(i); impl.io.is_branch(i) := branch(i)
    impl.io.rob_idx(i) := robIdx(i)
  }
  val psrc1 = Seq(io.rs1_phys0, io.rs1_phys1, io.rs1_phys2, io.rs1_phys3)
  val psrc2 = Seq(io.rs2_phys0, io.rs2_phys1, io.rs2_phys2, io.rs2_phys3)
  val pdest = Seq(io.dest_phys0, io.dest_phys1, io.dest_phys2, io.dest_phys3)
  val old = Seq(io.old_phys0, io.old_phys1, io.old_phys2, io.old_phys3)
  val doRen = Seq(io.do_rename0, io.do_rename1, io.do_rename2, io.do_rename3)
  val cpIdx = Seq(io.cp_idx0, io.cp_idx1, io.cp_idx2, io.cp_idx3)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    psrc1(i) := impl.io.psrc1(i); psrc2(i) := impl.io.psrc2(i)
    pdest(i) := impl.io.pdest(i); old(i) := impl.io.old_phys(i)
    doRen(i) := impl.io.do_rename(i); cpIdx(i) := impl.io.cp_idx(i)
  }
  io.rs1_phys := impl.io.psrc1(0); io.rs2_phys := impl.io.psrc2(0)
  io.dest_phys := impl.io.pdest(0); io.old_phys := impl.io.old_phys(0)
  io.do_rename := impl.io.do_rename(0)

  val commitFire = Seq(io.commit_fire, io.commit1_fire, io.commit2_fire, io.commit3_fire)
  val commitDoRen = Seq(io.cm_do_ren, io.cm1_do_ren, io.cm2_do_ren, io.cm3_do_ren)
  val commitOld = Seq(io.cm_old_phys, io.cm1_old_phys, io.cm2_old_phys, io.cm3_old_phys)
  val commitNew = Seq(io.cm_new_phys, io.cm1_new_phys, io.cm2_new_phys, io.cm3_new_phys)
  val commitRd = Seq(io.cm_arch_rd, io.cm1_arch_rd, io.cm2_arch_rd, io.cm3_arch_rd)
  val commitCpValid = Seq(io.cm_cp_valid, io.cm1_cp_valid, io.cm2_cp_valid, io.cm3_cp_valid)
  val commitCpIdx = Seq(io.cm_cp_idx, io.cm1_cp_idx, io.cm2_cp_idx, io.cm3_cp_idx)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.commit_fire(i) := commitFire(i); impl.io.commit_do_rename(i) := commitDoRen(i)
    impl.io.commit_old_phys(i) := commitOld(i); impl.io.commit_new_phys(i) := commitNew(i)
    impl.io.commit_arch_rd(i) := commitRd(i); impl.io.commit_cp_valid(i) := commitCpValid(i)
    impl.io.commit_cp_idx(i) := commitCpIdx(i)
  }
  impl.io.rob_head := io.rob_head
  impl.io.restore_cp := io.restore_cp; impl.io.restore_cp_idx := io.restore_cp_idx
  impl.io.restore_do_rename := io.restore_do_rename
  impl.io.restore_arch_rd := io.restore_arch_rd
  impl.io.restore_new_phys := io.restore_new_phys
  impl.io.restore_arch := io.restore_arch
  impl.io.rebuild := io.rebuild; impl.io.rebuild_rat := io.rebuild_rat
  impl.io.rebuild_free := io.rebuild_free
  io.rat_out := impl.io.rat_out; io.arch_rat_out := impl.io.arch_rat_out
  io.free_cnt := impl.io.free_cnt; io.fl_empty := impl.io.free_cnt === 0.U
  io.cp_full := impl.io.cp_free === 0.U
}

/** Four-wide ROB with the old named ports retained during the core migration. */
class WideROBCompat extends Module {
  private val ptrW = OoOParams.ROB_PTR_W
  val io = IO(new Bundle {
    val enq_fire = Input(Bool()); val enq_bits = Input(new ROBEntry); val enq_idx = Output(UInt(ptrW.W))
    val enq1_fire = Input(Bool()); val enq1_bits = Input(new ROBEntry); val enq1_idx = Output(UInt(ptrW.W))
    val enq2_fire = Input(Bool()); val enq2_bits = Input(new ROBEntry); val enq2_idx = Output(UInt(ptrW.W))
    val enq3_fire = Input(Bool()); val enq3_bits = Input(new ROBEntry); val enq3_idx = Output(UInt(ptrW.W))
    val space = Output(UInt(log2Ceil(OoOParams.ROB_SIZE + 1).W)); val full = Output(Bool())
    val issue_fire = Input(Bool())
    val wb_fire = Input(Bool()); val wb_idx = Input(UInt(ptrW.W)); val wb_val = Input(UInt(32.W))
    val wb_state = Input(new State); val wb_mem_addr = Input(UInt(32.W)); val wb_mem_wdata = Input(UInt(32.W))
    val wb_actual_taken = Input(Bool()); val wb_actual_target = Input(UInt(32.W))
    val wb1_fire = Input(Bool()); val wb1_idx = Input(UInt(ptrW.W)); val wb1_val = Input(UInt(32.W))
    val wb1_state = Input(new State); val wb1_mem_addr = Input(UInt(32.W)); val wb1_mem_wdata = Input(UInt(32.W))
    val wb1_actual_taken = Input(Bool()); val wb1_actual_target = Input(UInt(32.W))
    val wb2_fire = Input(Bool()); val wb2_idx = Input(UInt(ptrW.W)); val wb2_val = Input(UInt(32.W))
    val wb2_state = Input(new State); val wb2_mem_addr = Input(UInt(32.W)); val wb2_mem_wdata = Input(UInt(32.W))
    val wb2_actual_taken = Input(Bool()); val wb2_actual_target = Input(UInt(32.W))
    val wb3_fire = Input(Bool()); val wb3_idx = Input(UInt(ptrW.W)); val wb3_val = Input(UInt(32.W))
    val wb3_state = Input(new State); val wb3_mem_addr = Input(UInt(32.W)); val wb3_mem_wdata = Input(UInt(32.W))
    val wb3_actual_taken = Input(Bool()); val wb3_actual_target = Input(UInt(32.W))
    val ctrl_wb_fire = Input(Bool()); val ctrl_wb_idx = Input(UInt(ptrW.W)); val ctrl_wb_state = Input(new State)
    val ctrl_wb_actual_taken = Input(Bool()); val ctrl_wb_actual_target = Input(UInt(32.W))
    val store_wb_fire = Input(Bool()); val store_wb_idx = Input(UInt(ptrW.W)); val store_wb_state = Input(new State)
    val store_wb_mem_addr = Input(UInt(32.W)); val store_wb_mem_wdata = Input(UInt(32.W))
    val commit_valid = Output(Bool()); val commit_idx = Output(UInt(ptrW.W)); val commit_bits = Output(new ROBEntry)
    val commit_fire = Input(Bool())
    val commit1_valid = Output(Bool()); val commit1_idx = Output(UInt(ptrW.W)); val commit1_bits = Output(new ROBEntry)
    val commit1_fire = Input(Bool())
    val commit2_valid = Output(Bool()); val commit2_idx = Output(UInt(ptrW.W)); val commit2_bits = Output(new ROBEntry)
    val commit2_fire = Input(Bool())
    val commit3_valid = Output(Bool()); val commit3_idx = Output(UInt(ptrW.W)); val commit3_bits = Output(new ROBEntry)
    val commit3_fire = Input(Bool())
    val flush = Input(Bool()); val flush_idx = Input(UInt(ptrW.W)); val flush_all = Input(Bool())
    val entries = Output(Vec(OoOParams.ROB_SIZE, new ROBEntry)); val head = Output(UInt(ptrW.W))
    val tail = Output(UInt(ptrW.W)); val count = Output(UInt(log2Ceil(OoOParams.ROB_SIZE + 1).W))
  })
  val impl = Module(new WideROB())
  val enqFire = Seq(io.enq_fire, io.enq1_fire, io.enq2_fire, io.enq3_fire)
  val enqBits = Seq(io.enq_bits, io.enq1_bits, io.enq2_bits, io.enq3_bits)
  val enqIdx = Seq(io.enq_idx, io.enq1_idx, io.enq2_idx, io.enq3_idx)
  val wbFire = Seq(io.wb_fire, io.wb1_fire, io.wb2_fire, io.wb3_fire)
  val wbIdx = Seq(io.wb_idx, io.wb1_idx, io.wb2_idx, io.wb3_idx)
  val wbVal = Seq(io.wb_val, io.wb1_val, io.wb2_val, io.wb3_val)
  val wbState = Seq(io.wb_state, io.wb1_state, io.wb2_state, io.wb3_state)
  val wbAddr = Seq(io.wb_mem_addr, io.wb1_mem_addr, io.wb2_mem_addr, io.wb3_mem_addr)
  val wbData = Seq(io.wb_mem_wdata, io.wb1_mem_wdata, io.wb2_mem_wdata, io.wb3_mem_wdata)
  val wbTaken = Seq(io.wb_actual_taken, io.wb1_actual_taken, io.wb2_actual_taken, io.wb3_actual_taken)
  val wbTarget = Seq(io.wb_actual_target, io.wb1_actual_target, io.wb2_actual_target, io.wb3_actual_target)
  val cmValid = Seq(io.commit_valid, io.commit1_valid, io.commit2_valid, io.commit3_valid)
  val cmIdx = Seq(io.commit_idx, io.commit1_idx, io.commit2_idx, io.commit3_idx)
  val cmBits = Seq(io.commit_bits, io.commit1_bits, io.commit2_bits, io.commit3_bits)
  val cmFire = Seq(io.commit_fire, io.commit1_fire, io.commit2_fire, io.commit3_fire)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.enq_fire(i) := enqFire(i); impl.io.enq_bits(i) := enqBits(i); enqIdx(i) := impl.io.enq_idx(i)
    impl.io.wb_fire(i) := wbFire(i); impl.io.wb_idx(i) := wbIdx(i); impl.io.wb_val(i) := wbVal(i)
    impl.io.wb_state(i) := wbState(i); impl.io.wb_mem_addr(i) := wbAddr(i); impl.io.wb_mem_wdata(i) := wbData(i)
    impl.io.wb_actual_taken(i) := wbTaken(i); impl.io.wb_actual_target(i) := wbTarget(i)
    cmValid(i) := impl.io.commit_valid(i); cmIdx(i) := impl.io.commit_idx(i)
    cmBits(i) := impl.io.commit_bits(i); impl.io.commit_fire(i) := cmFire(i)
  }
  impl.io.ctrl_wb_fire := io.ctrl_wb_fire; impl.io.ctrl_wb_idx := io.ctrl_wb_idx
  impl.io.ctrl_wb_state := io.ctrl_wb_state; impl.io.ctrl_wb_actual_taken := io.ctrl_wb_actual_taken
  impl.io.ctrl_wb_actual_target := io.ctrl_wb_actual_target
  impl.io.store_wb_fire := io.store_wb_fire; impl.io.store_wb_idx := io.store_wb_idx
  impl.io.store_wb_state := io.store_wb_state; impl.io.store_wb_mem_addr := io.store_wb_mem_addr
  impl.io.store_wb_mem_wdata := io.store_wb_mem_wdata
  impl.io.flush := io.flush; impl.io.flush_idx := io.flush_idx; impl.io.flush_all := io.flush_all
  io.entries := impl.io.entries; io.head := impl.io.head; io.tail := impl.io.tail
  io.count := impl.io.count; io.space := impl.io.space; io.full := impl.io.space === 0.U
}

/** Four-wide distributed scheduler with old lane names at the integration boundary. */
class WideRSCompat extends Module {
  private val ptrW = OoOParams.ROB_PTR_W
  val io = IO(new Bundle {
    val enq_fire = Input(Bool()); val enq_bits = Input(new RSEntry)
    val enq1_fire = Input(Bool()); val enq1_bits = Input(new RSEntry)
    val enq2_fire = Input(Bool()); val enq2_bits = Input(new RSEntry)
    val enq3_fire = Input(Bool()); val enq3_bits = Input(new RSEntry)
    val space = Output(UInt(log2Ceil(OoOParams.WIDE_RS_SIZE + 1).W))
    val count = Output(UInt(log2Ceil(OoOParams.WIDE_RS_SIZE + 1).W))
    val rob_head = Input(UInt(ptrW.W)); val rob_st_pending = Input(UInt(OoOParams.ROB_SIZE.W))
    val issue_alu_valid = Output(Bool()); val issue_alu_bits = Output(new RSEntry); val issue_alu_fire = Input(Bool())
    val issue_alu1_valid = Output(Bool()); val issue_alu1_bits = Output(new RSEntry); val issue_alu1_fire = Input(Bool())
    val issue_alu2_valid = Output(Bool()); val issue_alu2_bits = Output(new RSEntry); val issue_alu2_fire = Input(Bool())
    val issue_alu3_valid = Output(Bool()); val issue_alu3_bits = Output(new RSEntry); val issue_alu3_fire = Input(Bool())
    val issue_div_valid = Output(Bool()); val issue_div_bits = Output(new RSEntry); val issue_div_fire = Input(Bool())
    val issue_lsu_valid = Output(Bool()); val issue_lsu_bits = Output(new RSEntry); val issue_lsu_fire = Input(Bool())
    val issue_fire = Input(Bool())
    val free_rob_fire = Input(Bool()); val free_rob_idx = Input(UInt(ptrW.W))
    val free_rob1_fire = Input(Bool()); val free_rob1_idx = Input(UInt(ptrW.W))
    val free_rob2_fire = Input(Bool()); val free_rob2_idx = Input(UInt(ptrW.W))
    val free_rob3_fire = Input(Bool()); val free_rob3_idx = Input(UInt(ptrW.W))
    val free_ctrl_fire = Input(Bool()); val free_ctrl_idx = Input(UInt(ptrW.W))
    val free_store_fire = Input(Bool()); val free_store_idx = Input(UInt(ptrW.W))
    val cdb_valid = Input(Bool()); val cdb_pdest = Input(UInt(OoOParams.PHYS_W.W)); val cdb_val = Input(UInt(32.W))
    val cdb1_valid = Input(Bool()); val cdb1_pdest = Input(UInt(OoOParams.PHYS_W.W)); val cdb1_val = Input(UInt(32.W))
    val cdb2_valid = Input(Bool()); val cdb2_pdest = Input(UInt(OoOParams.PHYS_W.W)); val cdb2_val = Input(UInt(32.W))
    val cdb3_valid = Input(Bool()); val cdb3_pdest = Input(UInt(OoOParams.PHYS_W.W)); val cdb3_val = Input(UInt(32.W))
    val flush = Input(Bool()); val flush_idx = Input(UInt(ptrW.W)); val flush_all = Input(Bool())
    val fresh_issue_count = Output(UInt(3.W))
  })
  val impl = Module(new WideRS())
  val enqFire = Seq(io.enq_fire, io.enq1_fire, io.enq2_fire, io.enq3_fire)
  val enqBits = Seq(io.enq_bits, io.enq1_bits, io.enq2_bits, io.enq3_bits)
  val issueValid = Seq(io.issue_alu_valid, io.issue_alu1_valid, io.issue_alu2_valid, io.issue_alu3_valid)
  val issueBits = Seq(io.issue_alu_bits, io.issue_alu1_bits, io.issue_alu2_bits, io.issue_alu3_bits)
  val issueFire = Seq(io.issue_alu_fire, io.issue_alu1_fire, io.issue_alu2_fire, io.issue_alu3_fire)
  val freeFire = Seq(io.free_rob_fire, io.free_rob1_fire, io.free_rob2_fire, io.free_rob3_fire)
  val freeIdx = Seq(io.free_rob_idx, io.free_rob1_idx, io.free_rob2_idx, io.free_rob3_idx)
  val cdbValid = Seq(io.cdb_valid, io.cdb1_valid, io.cdb2_valid, io.cdb3_valid)
  val cdbPdest = Seq(io.cdb_pdest, io.cdb1_pdest, io.cdb2_pdest, io.cdb3_pdest)
  val cdbVal = Seq(io.cdb_val, io.cdb1_val, io.cdb2_val, io.cdb3_val)
  for (i <- 0 until OoOParams.CORE_WIDTH) {
    impl.io.enq_fire(i) := enqFire(i); impl.io.enq_bits(i) := enqBits(i)
    issueValid(i) := impl.io.issue_alu_valid(i); issueBits(i) := impl.io.issue_alu_bits(i)
    impl.io.issue_alu_fire(i) := issueFire(i)
    impl.io.free_data_fire(i) := freeFire(i); impl.io.free_data_idx(i) := freeIdx(i)
    impl.io.cdb_valid(i) := cdbValid(i); impl.io.cdb_pdest(i) := cdbPdest(i); impl.io.cdb_val(i) := cdbVal(i)
  }
  impl.io.rob_head := io.rob_head; impl.io.rob_st_pending := io.rob_st_pending
  io.issue_div_valid := impl.io.issue_div_valid; io.issue_div_bits := impl.io.issue_div_bits
  impl.io.issue_div_fire := io.issue_div_fire
  io.issue_lsu_valid := impl.io.issue_lsu_valid; io.issue_lsu_bits := impl.io.issue_lsu_bits
  impl.io.issue_lsu_fire := io.issue_lsu_fire
  impl.io.free_ctrl_fire := io.free_ctrl_fire; impl.io.free_ctrl_idx := io.free_ctrl_idx
  impl.io.free_store_fire := io.free_store_fire; impl.io.free_store_idx := io.free_store_idx
  impl.io.flush := io.flush; impl.io.flush_idx := io.flush_idx; impl.io.flush_all := io.flush_all
  io.space := impl.io.space; io.count := impl.io.count
  io.fresh_issue_count := impl.io.fresh_issue_count
}
