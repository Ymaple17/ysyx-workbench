package unit

import chisel3._
import chisel3.util._
import common.ALU_OP._
import common.JUMP_TYPE._
import common.OoOParams

class WideRS(n: Int = OoOParams.WIDE_RS_SIZE) extends Module {
  private val width = OoOParams.CORE_WIDTH
  private val idxW = log2Ceil(n)
  require(n >= width)

  val io = IO(new Bundle {
    val enq_fire = Input(Vec(width, Bool()))
    val enq_bits = Input(Vec(width, new RSEntry))
    val enq_idx = Output(Vec(width, UInt(idxW.W)))
    val space = Output(UInt(log2Ceil(n + 1).W))
    val count = Output(UInt(log2Ceil(n + 1).W))

    val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
    val rob_st_pending = Input(UInt(OoOParams.ROB_SIZE.W))

    val issue_alu_valid = Output(Vec(width, Bool()))
    val issue_alu_bits = Output(Vec(width, new RSEntry))
    val issue_alu_idx = Output(Vec(width, UInt(idxW.W)))
    val issue_alu_fire = Input(Vec(width, Bool()))
    val issue_div_valid = Output(Bool())
    val issue_div_bits = Output(new RSEntry)
    val issue_div_idx = Output(UInt(idxW.W))
    val issue_div_fire = Input(Bool())
    val issue_lsu_valid = Output(Bool())
    val issue_lsu_bits = Output(new RSEntry)
    val issue_lsu_idx = Output(UInt(idxW.W))
    val issue_lsu_fire = Input(Bool())
    val issue_lsu1_valid = Output(Bool())
    val issue_lsu1_bits = Output(new RSEntry)
    val issue_lsu1_idx = Output(UInt(idxW.W))
    val issue_lsu1_fire = Input(Bool())
    val issue_store_addr_valid = Output(Bool())
    val issue_store_addr_bits = Output(new RSEntry)
    val issue_store_addr_idx = Output(UInt(idxW.W))
    val issue_store_addr_fire = Input(Bool())
    val free_data_fire = Input(Vec(width, Bool()))
    val free_data_idx = Input(Vec(width, UInt(OoOParams.ROB_PTR_W.W)))
    val free_ctrl_fire = Input(Bool())
    val free_ctrl_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free_store_fire = Input(Bool())
    val free_store_idx = Input(UInt(OoOParams.ROB_PTR_W.W))

    val cdb_valid = Input(Vec(width, Bool()))
    val cdb_pdest = Input(Vec(width, UInt(OoOParams.PHYS_W.W)))
    val cdb_val = Input(Vec(width, UInt(32.W)))

    val flush = Input(Bool())
    val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val flush_all = Input(Bool())
    val fresh_issue_count = Output(UInt(3.W))
    val store_addr_candidate_count = Output(UInt(log2Ceil(n + 1).W))
    val store_addr_data_wait_count = Output(UInt(log2Ceil(n + 1).W))
    val store_data_addr_wait_count = Output(UInt(log2Ceil(n + 1).W))
    val store_ready_count = Output(UInt(log2Ceil(n + 1).W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new RSEntry))))
  val storeAddrIssued = RegInit(VecInit(Seq.fill(n)(false.B)))
  val validMask = VecInit(entries.map(_.valid)).asUInt
  io.count := PopCount(validMask)
  io.space := n.U - io.count

  def age(idx: UInt): UInt =
    (idx - io.rob_head)(OoOParams.ROB_PTR_W - 1, 0)
  def isMulDiv(e: RSEntry): Bool =
    e.exu_alu_control === ALU_MUL || e.exu_alu_control === ALU_MULH ||
      e.exu_alu_control === ALU_MULHSU || e.exu_alu_control === ALU_MULHU ||
      e.exu_alu_control === ALU_DIV || e.exu_alu_control === ALU_DIVU ||
      e.exu_alu_control === ALU_REM || e.exu_alu_control === ALU_REMU
  def auxAluCapable(e: RSEntry): Bool =
    e.exu_jump === JUMP_NONE && !e.wbu_csr_write && !e.is_ebreak &&
      !e.is_fencei && !e.state.state
  def hasOlderPendingStore(robIdx: UInt): Bool = {
    val myAge = age(robIdx)
    (0 until OoOParams.ROB_SIZE).map(i =>
      io.rob_st_pending(i) && age(i.U) < myAge).foldLeft(false.B)(_ || _)
  }

  val freeByRob = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    val dataFree = (0 until width).map(lane =>
      io.free_data_fire(lane) && entries(i).rob_idx === io.free_data_idx(lane))
      .foldLeft(false.B)(_ || _)
    freeByRob(i) := entries(i).valid && (dataFree ||
      (io.free_ctrl_fire && entries(i).rob_idx === io.free_ctrl_idx) ||
      (io.free_store_fire && entries(i).rob_idx === io.free_store_idx))
  }

  val allocMask = Wire(Vec(width + 1, UInt(n.W)))
  val allocAccept = Wire(Vec(width, Bool()))
  allocMask(0) := ~validMask | freeByRob.asUInt
  for (lane <- 0 until width) {
    io.enq_idx(lane) := PriorityEncoder(allocMask(lane))
    allocAccept(lane) := io.enq_fire(lane) && allocMask(lane).orR
    allocMask(lane + 1) := Mux(allocAccept(lane),
      allocMask(lane) & ~UIntToOH(io.enq_idx(lane), n), allocMask(lane))
  }

  def wake(in: RSEntry): RSEntry = {
    val out = WireDefault(in)
    val src1Hit = VecInit((0 until width).map(lane =>
      io.cdb_valid(lane) && io.cdb_pdest(lane) =/= 0.U &&
        !in.src1_ready && in.src1_phys === io.cdb_pdest(lane)))
    val src2Hit = VecInit((0 until width).map(lane =>
      io.cdb_valid(lane) && io.cdb_pdest(lane) =/= 0.U &&
        !in.src2_ready && in.src2_phys === io.cdb_pdest(lane)))
    out.src1_ready := in.src1_ready || src1Hit.asUInt.orR
    out.src2_ready := in.src2_ready || src2Hit.asUInt.orR
    out.src1_val := Mux(src1Hit.asUInt.orR, Mux1H(src1Hit, io.cdb_val), in.src1_val)
    out.src2_val := Mux(src2Hit.asUInt.orR, Mux1H(src2Hit, io.cdb_val), in.src2_val)
    out
  }

  val issueEntry = Wire(Vec(n, new RSEntry))
  val canIssue = Wire(Vec(n, Bool()))
  val residentMulDiv = Wire(Vec(n, Bool()))
  val residentMem = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    issueEntry(i) := wake(entries(i))
    residentMulDiv(i) := isMulDiv(entries(i))
    residentMem(i) := entries(i).lsu_mem_valid
    val registeredStoreWait = if (OoOParams.LQ_SPECULATE_UNKNOWN_STORES) false.B
      else hasOlderPendingStore(entries(i).rob_idx)
    val loadWait = entries(i).lsu_mem_valid && !entries(i).lsu_mem_write &&
      registeredStoreWait
    canIssue(i) := entries(i).valid && !entries(i).issued &&
      issueEntry(i).src1_ready && issueEntry(i).src2_ready &&
      !freeByRob(i) && !loadWait
  }

  val residentStore = VecInit((0 until n).map(i =>
    entries(i).valid && !entries(i).issued && !freeByRob(i) &&
      residentMem(i) && entries(i).lsu_mem_write))
  val storeAddrCandidate = VecInit((0 until n).map(i =>
    residentStore(i) && issueEntry(i).src1_ready))
  val storeAddrDataWait = VecInit((0 until n).map(i =>
    storeAddrCandidate(i) && !issueEntry(i).src2_ready))
  val storeDataAddrWait = VecInit((0 until n).map(i =>
    residentStore(i) && !issueEntry(i).src1_ready && issueEntry(i).src2_ready))
  val storeReady = VecInit((0 until n).map(i =>
    storeAddrCandidate(i) && issueEntry(i).src2_ready))
  io.store_addr_candidate_count := PopCount(storeAddrCandidate)
  io.store_addr_data_wait_count := PopCount(storeAddrDataWait)
  io.store_data_addr_wait_count := PopCount(storeDataAddrWait)
  io.store_ready_count := PopCount(storeReady)

  def oldest(mask: Vec[Bool]): Vec[Bool] = {
    val result = Wire(Vec(n, Bool()))
    for (i <- 0 until n) {
      val older = (0 until n).map(j =>
        mask(j) && age(entries(j).rob_idx) < age(entries(i).rob_idx))
        .foldLeft(false.B)(_ || _)
      result(i) := mask(i) && !older
    }
    result
  }

  val storeAddrSelect = oldest(VecInit((0 until n).map(i =>
    storeAddrDataWait(i) && !storeAddrIssued(i))))
  val residentStoreAddr = storeAddrSelect.asUInt.orR
  io.issue_store_addr_valid := residentStoreAddr && !io.flush
  io.issue_store_addr_bits := Mux1H(storeAddrSelect, issueEntry)
  io.issue_store_addr_idx := PriorityEncoder(storeAddrSelect.asUInt)

  val residentSelect = Wire(Vec(width, Vec(n, Bool())))
  val residentUsed = Wire(Vec(width + 1, UInt(n.W)))
  residentUsed(0) := 0.U
  for (port <- 0 until width) {
    val eligible = Wire(Vec(n, Bool()))
    for (i <- 0 until n) {
      val ordinary = canIssue(i) && !residentMem(i) && !residentMulDiv(i)
      eligible(i) := ordinary && !residentUsed(port)(i) &&
        (if (port == 0) true.B else auxAluCapable(entries(i)))
    }
    residentSelect(port) := oldest(eligible)
    residentUsed(port + 1) := residentUsed(port) | residentSelect(port).asUInt
  }

  val divSelect = oldest(VecInit((0 until n).map(i => canIssue(i) && residentMulDiv(i))))
  val lsuSelect = oldest(VecInit((0 until n).map(i => canIssue(i) && residentMem(i))))
  val lsuSelect1 = oldest(VecInit((0 until n).map(i =>
    canIssue(i) && residentMem(i) && !entries(i).lsu_mem_write && !lsuSelect(i))))

  val fresh = Wire(Vec(width, new RSEntry))
  val freshReady = Wire(Vec(width, Bool()))
  val freshMulDiv = Wire(Vec(width, Bool()))
  val freshMem = Wire(Vec(width, Bool()))
  for (lane <- 0 until width) {
    fresh(lane) := wake(io.enq_bits(lane))
    freshMulDiv(lane) := isMulDiv(fresh(lane))
    freshMem(lane) := fresh(lane).lsu_mem_valid
    val olderFreshStore = (0 until lane).map(i => allocAccept(i) &&
      fresh(i).lsu_mem_valid && fresh(i).lsu_mem_write).foldLeft(false.B)(_ || _)
    val registeredStoreWait = if (OoOParams.LQ_SPECULATE_UNKNOWN_STORES) false.B
      else hasOlderPendingStore(fresh(lane).rob_idx)
    val loadWait = fresh(lane).lsu_mem_valid && !fresh(lane).lsu_mem_write &&
      (registeredStoreWait || olderFreshStore)
    freshReady(lane) := allocAccept(lane) && fresh(lane).src1_ready &&
      fresh(lane).src2_ready && !loadWait
  }

  val freshAluGrant = Wire(Vec(width, UInt(width.W)))
  val freshUsed = Wire(Vec(width + 1, UInt(width.W)))
  freshUsed(0) := 0.U
  for (port <- 0 until width) {
    val candidates = VecInit((0 until width).map(lane =>
      freshReady(lane) && !freshMem(lane) && !freshMulDiv(lane) &&
        !freshUsed(port)(lane) && (if (port == 0) true.B else auxAluCapable(fresh(lane)))))
    val grant = PriorityEncoderOH(candidates.asUInt)
    freshAluGrant(port) := Mux(residentSelect(port).asUInt.orR, 0.U, grant)
    freshUsed(port + 1) := freshUsed(port) | freshAluGrant(port)
  }

  val freshDivCandidates = VecInit((0 until width).map(i => freshReady(i) && freshMulDiv(i)))
  val freshLsuCandidates = VecInit((0 until width).map(i => freshReady(i) && freshMem(i)))
  val freshLsu1Candidates = VecInit((0 until width).map(i =>
    freshReady(i) && freshMem(i) && !fresh(i).lsu_mem_write))
  val freshDivGrant = PriorityEncoderOH(freshDivCandidates.asUInt)
  val freshLsuGrant = PriorityEncoderOH(freshLsuCandidates.asUInt)
  val freshLsuGrant1 = PriorityEncoderOH(freshLsu1Candidates.asUInt & ~freshLsuGrant)
  val residentDiv = divSelect.asUInt.orR
  val residentLsu = lsuSelect.asUInt.orR
  val residentLsu1 = lsuSelect1.asUInt.orR
  val lsuFreshGrant = Mux(residentLsu, 0.U(width.W), freshLsuGrant)
  val lsu1FreshGrant = Mux(residentLsu1, 0.U(width.W),
    Mux(residentLsu, PriorityEncoderOH(freshLsu1Candidates.asUInt), freshLsuGrant1))

  for (port <- 0 until width) {
    val resident = residentSelect(port).asUInt.orR
    val freshValid = freshAluGrant(port).orR
    io.issue_alu_valid(port) := (resident || freshValid) && !io.flush
    io.issue_alu_bits(port) := Mux(resident,
      Mux1H(residentSelect(port), issueEntry), Mux1H(freshAluGrant(port), fresh))
    io.issue_alu_idx(port) := Mux(resident,
      PriorityEncoder(residentSelect(port).asUInt),
      Mux1H(freshAluGrant(port), io.enq_idx))
  }
  io.issue_div_valid := (residentDiv || freshDivCandidates.asUInt.orR) && !io.flush
  io.issue_div_bits := Mux(residentDiv, Mux1H(divSelect, issueEntry),
    Mux1H(freshDivGrant, fresh))
  io.issue_div_idx := Mux(residentDiv, PriorityEncoder(divSelect.asUInt),
    Mux1H(freshDivGrant, io.enq_idx))
  val lsuCandidate = residentLsu || lsuFreshGrant.orR
  val lsu1Candidate = residentLsu1 || lsu1FreshGrant.orR
  val lsuBits = Mux(residentLsu, Mux1H(lsuSelect, issueEntry),
    Mux1H(lsuFreshGrant, fresh))
  val lsu1Bits = Mux(residentLsu1, Mux1H(lsuSelect1, issueEntry),
    Mux1H(lsu1FreshGrant, fresh))
  io.issue_lsu_valid := lsuCandidate && !io.flush
  io.issue_lsu_bits := lsuBits
  io.issue_lsu_idx := Mux(residentLsu, PriorityEncoder(lsuSelect.asUInt),
    Mux1H(lsuFreshGrant, io.enq_idx))
  io.issue_lsu1_valid := lsu1Candidate && !io.flush
  io.issue_lsu1_bits := lsu1Bits
  io.issue_lsu1_idx := Mux(residentLsu1, PriorityEncoder(lsuSelect1.asUInt),
    Mux1H(lsu1FreshGrant, io.enq_idx))

  val freshIssued = Wire(Vec(width, Bool()))
  for (lane <- 0 until width) {
    val aluIssued = (0 until width).map(port =>
      io.issue_alu_fire(port) && freshAluGrant(port)(lane)).foldLeft(false.B)(_ || _)
    freshIssued(lane) := aluIssued ||
      (io.issue_div_fire && !residentDiv && freshDivGrant(lane)) ||
      (io.issue_lsu_fire && lsuFreshGrant(lane)) ||
      (io.issue_lsu1_fire && lsu1FreshGrant(lane))
  }
  io.fresh_issue_count := PopCount(freshIssued)

  for (lane <- 0 until width) {
    when(io.cdb_valid(lane) && io.cdb_pdest(lane) =/= 0.U) {
      for (i <- 0 until n) {
        when(entries(i).valid && !entries(i).src1_ready &&
          entries(i).src1_phys === io.cdb_pdest(lane)) {
          entries(i).src1_ready := true.B
          entries(i).src1_val := io.cdb_val(lane)
        }
        when(entries(i).valid && !entries(i).src2_ready &&
          entries(i).src2_phys === io.cdb_pdest(lane)) {
          entries(i).src2_ready := true.B
          entries(i).src2_val := io.cdb_val(lane)
        }
      }
    }
  }

  when(io.flush) {
    for (i <- 0 until n) {
      when(io.flush_all || (entries(i).valid && age(entries(i).rob_idx) > age(io.flush_idx))) {
        entries(i).valid := false.B
        storeAddrIssued(i) := false.B
      }
    }
  }.otherwise {
    for (i <- 0 until n) {
      when(freeByRob(i)) {
        entries(i).valid := false.B
        storeAddrIssued(i) := false.B
      }
    }
    for (port <- 0 until width) {
      when(io.issue_alu_fire(port) && residentSelect(port).asUInt.orR) {
        entries(PriorityEncoder(residentSelect(port).asUInt)).issued := true.B
      }
    }
    when(io.issue_div_fire && residentDiv) {
      entries(PriorityEncoder(divSelect.asUInt)).issued := true.B
    }
    when(io.issue_lsu_fire && residentLsu) {
      entries(PriorityEncoder(lsuSelect.asUInt)).issued := true.B
    }
    when(io.issue_lsu1_fire && residentLsu1) {
      entries(PriorityEncoder(lsuSelect1.asUInt)).issued := true.B
    }
    when(io.issue_store_addr_fire && residentStoreAddr) {
      storeAddrIssued(PriorityEncoder(storeAddrSelect.asUInt)) := true.B
    }
    for (lane <- 0 until width) {
      when(allocAccept(lane)) {
        val entry = WireDefault(fresh(lane))
        entry.valid := true.B
        entry.issued := freshIssued(lane)
        entries(io.enq_idx(lane)) := entry
        storeAddrIssued(io.enq_idx(lane)) := false.B
      }
    }
  }

  when(!reset.asBool) {
    for (lane <- 0 until width) {
      assert(!io.enq_fire(lane) || allocAccept(lane),
        "wide RS requires pre-checked enqueue credit")
    }
    for (a <- 0 until width; b <- a + 1 until width) {
      assert(!(io.issue_alu_fire(a) && io.issue_alu_fire(b) &&
        io.issue_alu_bits(a).rob_idx === io.issue_alu_bits(b).rob_idx),
        "wide RS must not issue one instruction to two ALUs")
    }
    assert(!io.issue_lsu1_fire || !io.issue_lsu1_bits.lsu_mem_write,
      "the secondary LSU issue path is load-only")
    assert(!(io.issue_lsu_fire && io.issue_lsu1_fire) ||
      (io.issue_lsu_idx =/= io.issue_lsu1_idx),
      "the two LSU ports must not issue the same RS entry")
    assert(!io.issue_store_addr_fire ||
      (io.issue_store_addr_bits.lsu_mem_write && io.issue_store_addr_bits.src1_ready &&
        !io.issue_store_addr_bits.src2_ready),
      "Store address issue must be an address-ready/data-wait Store")
  }
}
