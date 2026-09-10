package unit

import chisel3._
import chisel3.util._
import common.ALU_OP._
import common.OoOParams
import common.BPU_Config._
import common.JUMP_TYPE._
import core.State

class RSEntry extends Bundle {
  val valid      = Bool()
  val issued     = Bool()
  val rob_idx    = UInt(OoOParams.ROB_PTR_W.W)
  val cp_idx     = UInt(log2Ceil(OoOParams.CP_DEPTH).W)
  val src1_ready = Bool()
  val src2_ready = Bool()
  val src1_phys  = UInt(OoOParams.PHYS_W.W)
  val src2_phys  = UInt(OoOParams.PHYS_W.W)
  val src1_val   = UInt(32.W)
  val src2_val   = UInt(32.W)
  val pdest      = UInt(OoOParams.PHYS_W.W)
  val old_phys   = UInt(OoOParams.PHYS_W.W)
  val do_rename  = Bool()
  val pc         = UInt(32.W)
  val inst       = UInt(32.W)
  val imm_ext    = UInt(32.W)
  val waddr      = UInt(5.W)
  val is_ebreak  = Bool()
  val is_fencei  = Bool()
  val csr_rd1    = UInt(32.W)
  val csr_waddr  = UInt(12.W)
  val state      = new State
  val bp_valid   = Bool()
  val bp_taken   = Bool()
  val bp_target  = UInt(32.W)
  val bp_index   = UInt(BP_META_WIDTH.W)
  val ftq_idx    = UInt(OoOParams.FTQ_PTR_W.W)
  val ftq_generation = UInt(OoOParams.FTQ_GEN_W.W)
  val exu_alu_srcA    = UInt(2.W)
  val exu_alu_srcB    = UInt(2.W)
  val exu_alu_control = UInt(5.W)
  val exu_jump        = UInt(4.W)
  val lsu_mem_wmask   = UInt(8.W)
  val lsu_mem_rd      = UInt(3.W)
  val lsu_mem_write   = Bool()
  val lsu_mem_valid   = Bool()
  val wbu_reg_write     = Bool()
  val wbu_reg_write_sel = UInt(3.W)
  val wbu_csr_write     = Bool()
  val wbu_csr_sel       = UInt(2.W)
}

/**
 * 3d RS: ready 中 age 最小可发；访存仅 rob_head。
 * 8a: 按类型拆成 ALU / DIV / LSU 三个 issue 口。
 */
class RS(n: Int = OoOParams.RS_SIZE) extends Module {
  val io = IO(new Bundle {
    val enq_fire = Input(Bool())
    val enq_bits = Input(new RSEntry)
    val full     = Output(Bool())
    val count    = Output(UInt(log2Ceil(n + 1).W))

    val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
    // bit i: ROB[i] 是尚未 addr_ready 的 store（含已 leave 未 wb）
    val rob_st_pending = Input(UInt(OoOParams.ROB_SIZE.W))

    val issue_alu_valid = Output(Bool())
    val issue_alu_bits  = Output(new RSEntry)
    val issue_alu_idx   = Output(UInt(log2Ceil(n).W))
    val issue_alu_fire  = Input(Bool())
    val issue_alu1_valid = Output(Bool())
    val issue_alu1_bits  = Output(new RSEntry)
    val issue_alu1_idx   = Output(UInt(log2Ceil(n).W))
    val issue_alu1_fire  = Input(Bool())
    val issue_div_valid = Output(Bool())
    val issue_div_bits  = Output(new RSEntry)
    val issue_div_idx   = Output(UInt(log2Ceil(n).W))
    val issue_div_fire  = Input(Bool())
    val issue_lsu_valid = Output(Bool())
    val issue_lsu_bits  = Output(new RSEntry)
    val issue_lsu_idx   = Output(UInt(log2Ceil(n).W))
    val issue_lsu_fire  = Input(Bool())
    val control_ready = Output(Bool())
    val issue_valid = Output(Bool())
    val issue_bits  = Output(new RSEntry)
    val issue_idx   = Output(UInt(log2Ceil(n).W))
    val issue_fire  = Input(Bool())

    val enq1_fire = Input(Bool())
    val enq1_bits = Input(new RSEntry)
    val enq1_idx  = Output(UInt(log2Ceil(n).W))

    val free_rob_fire = Input(Bool())
    val free_rob_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free_rob1_fire = Input(Bool())
    val free_rob1_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free_rob2_fire = Input(Bool())
    val free_rob2_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free_ctrl_fire = Input(Bool())
    val free_ctrl_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free_store_fire = Input(Bool())
    val free_store_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))

    val cdb_valid = Input(Bool())
    val cdb_pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb_val   = Input(UInt(32.W))
    val cdb1_valid = Input(Bool())
    val cdb1_pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb1_val   = Input(UInt(32.W))
    val cdb2_valid = Input(Bool())
    val cdb2_pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb2_val   = Input(UInt(32.W))

    val flush     = Input(Bool())
    val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val flush_all = Input(Bool())
    val space     = Output(UInt(log2Ceil(n + 1).W))
    val fresh_issue_count = Output(UInt(3.W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new RSEntry))))
  io.count := PopCount(VecInit(entries.map(_.valid)).asUInt)
  io.space := n.U - io.count

  def age(idx: UInt): UInt = (idx - io.rob_head)(OoOParams.ROB_PTR_W - 1, 0)

  val freeByRob = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    freeByRob(i) := entries(i).valid && (
      (io.free_rob_fire && entries(i).rob_idx === io.free_rob_idx) ||
      (io.free_rob1_fire && entries(i).rob_idx === io.free_rob1_idx) ||
      (io.free_rob2_fire && entries(i).rob_idx === io.free_rob2_idx) ||
      (io.free_ctrl_fire && entries(i).rob_idx === io.free_ctrl_idx) ||
      (io.free_store_fire && entries(i).rob_idx === io.free_store_idx)
    )
  }

  val willFreeRob = freeByRob.asUInt.orR
  val freeMask = VecInit(entries.map(e => !e.valid)).asUInt
  io.full := !freeMask.orR && !willFreeRob
  val freeOrIssue = freeMask | Mux(willFreeRob, freeByRob.asUInt, 0.U)
  val enqIdx = PriorityEncoder(freeOrIssue)
  val enqOH = PriorityEncoderOH(freeOrIssue)
  // Lane1 may be the only RS enqueue when lane0 is routed to the branch queue.
  // Reserve enqIdx for lane0 only when lane0 actually fires.
  val freeMask1 = Mux(io.enq_fire, freeOrIssue & ~enqOH.asUInt, freeOrIssue)
  val enq1Idx = PriorityEncoder(freeMask1)
  io.enq1_idx := enq1Idx

  val canIssue = Wire(Vec(n, Bool()))
  val isMulDiv = Wire(Vec(n, Bool()))
  val isLoad   = Wire(Vec(n, Bool()))
  val issueEntries = Wire(Vec(n, new RSEntry))
  for (i <- 0 until n) {
    val e = WireDefault(entries(i))
    val cdbHit1 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
      !entries(i).src1_ready && (entries(i).src1_phys === io.cdb_pdest)
    val cdbHit2 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
      !entries(i).src2_ready && (entries(i).src2_phys === io.cdb_pdest)
    val cdb1Hit1 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
      !entries(i).src1_ready && (entries(i).src1_phys === io.cdb1_pdest)
    val cdb1Hit2 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
      !entries(i).src2_ready && (entries(i).src2_phys === io.cdb1_pdest)
    val cdb2Hit1 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
      !entries(i).src1_ready && (entries(i).src1_phys === io.cdb2_pdest)
    val cdb2Hit2 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
      !entries(i).src2_ready && (entries(i).src2_phys === io.cdb2_pdest)
    when(cdbHit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb_val
    }
    when(cdbHit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb_val
    }
    when(cdb1Hit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb1_val
    }
    when(cdb1Hit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb1_val
    }
    when(cdb2Hit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb2_val
    }
    when(cdb2Hit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb2_val
    }
    issueEntries(i) := e
  }

  def hasOlderPendingStore(robIdx: UInt): Bool = {
    val myAge = age(robIdx)
    (0 until OoOParams.ROB_SIZE).map { j =>
      io.rob_st_pending(j) && (age(j.U) < myAge)
    }.foldLeft(false.B)(_ || _)
  }
  for (i <- 0 until n) {
    isMulDiv(i) := entries(i).valid && (
      (entries(i).exu_alu_control === ALU_MUL)   ||
      (entries(i).exu_alu_control === ALU_MULH)  ||
      (entries(i).exu_alu_control === ALU_MULHSU) ||
      (entries(i).exu_alu_control === ALU_MULHU) ||
      (entries(i).exu_alu_control === ALU_DIV)   ||
      (entries(i).exu_alu_control === ALU_DIVU)  ||
      (entries(i).exu_alu_control === ALU_REM)   ||
      (entries(i).exu_alu_control === ALU_REMU)
    )
    isLoad(i) := entries(i).lsu_mem_valid

    val registeredStoreWait = if (OoOParams.LQ_SPECULATE_UNKNOWN_STORES) false.B
      else hasOlderPendingStore(entries(i).rob_idx)
    val waitOlderStore = entries(i).valid && entries(i).lsu_mem_valid &&
      !entries(i).lsu_mem_write && registeredStoreWait
    val ready = entries(i).valid && !entries(i).issued && issueEntries(i).src1_ready &&
      issueEntries(i).src2_ready && !freeByRob(i)
    canIssue(i) := ready && !waitOlderStore
  }

  def wakeEntry(in: RSEntry): RSEntry = {
    val e = WireDefault(in)
    when(!in.src1_ready) {
      when(io.cdb_valid && io.cdb_pdest =/= 0.U && in.src1_phys === io.cdb_pdest) {
        e.src1_ready := true.B
        e.src1_val := io.cdb_val
      }
      when(io.cdb1_valid && io.cdb1_pdest =/= 0.U && in.src1_phys === io.cdb1_pdest) {
        e.src1_ready := true.B
        e.src1_val := io.cdb1_val
      }
      when(io.cdb2_valid && io.cdb2_pdest =/= 0.U && in.src1_phys === io.cdb2_pdest) {
        e.src1_ready := true.B
        e.src1_val := io.cdb2_val
      }
    }
    when(!in.src2_ready) {
      when(io.cdb_valid && io.cdb_pdest =/= 0.U && in.src2_phys === io.cdb_pdest) {
        e.src2_ready := true.B
        e.src2_val := io.cdb_val
      }
      when(io.cdb1_valid && io.cdb1_pdest =/= 0.U && in.src2_phys === io.cdb1_pdest) {
        e.src2_ready := true.B
        e.src2_val := io.cdb1_val
      }
      when(io.cdb2_valid && io.cdb2_pdest =/= 0.U && in.src2_phys === io.cdb2_pdest) {
        e.src2_ready := true.B
        e.src2_val := io.cdb2_val
      }
    }
    e
  }

  val fresh = Wire(Vec(2, new RSEntry))
  fresh(0) := wakeEntry(io.enq_bits)
  fresh(1) := wakeEntry(io.enq1_bits)
  val freshAccept = Wire(Vec(2, Bool()))
  freshAccept(0) := io.enq_fire && freeOrIssue.orR
  freshAccept(1) := io.enq1_fire && freeMask1.orR
  val freshMulDiv = Wire(Vec(2, Bool()))
  val freshMem = Wire(Vec(2, Bool()))
  val freshCanIssue = Wire(Vec(2, Bool()))
  for (i <- 0 until 2) {
    freshMulDiv(i) := (fresh(i).exu_alu_control === ALU_MUL) ||
      (fresh(i).exu_alu_control === ALU_MULH) ||
      (fresh(i).exu_alu_control === ALU_MULHSU) ||
      (fresh(i).exu_alu_control === ALU_MULHU) ||
      (fresh(i).exu_alu_control === ALU_DIV) ||
      (fresh(i).exu_alu_control === ALU_DIVU) ||
      (fresh(i).exu_alu_control === ALU_REM) ||
      (fresh(i).exu_alu_control === ALU_REMU)
    freshMem(i) := fresh(i).lsu_mem_valid
    val olderFreshStore = if (i == 1) {
      freshAccept(0) && fresh(0).lsu_mem_valid && fresh(0).lsu_mem_write
    } else {
      false.B
    }
    val registeredStoreWait = if (OoOParams.LQ_SPECULATE_UNKNOWN_STORES) false.B
      else hasOlderPendingStore(fresh(i).rob_idx)
    val waitOlderStore = fresh(i).lsu_mem_valid && !fresh(i).lsu_mem_write &&
      (registeredStoreWait || olderFreshStore)
    freshCanIssue(i) := freshAccept(i) && fresh(i).src1_ready &&
      fresh(i).src2_ready && !waitOlderStore
  }

  def oldestOH(mask: Vec[Bool]): Vec[Bool] = {
    val oh = Wire(Vec(n, Bool()))
    for (i <- 0 until n) {
      val ai = age(entries(i).rob_idx)
      val hasOlder = (0 until n).map { j =>
        mask(j) && canIssue(j) && (age(entries(j).rob_idx) < ai)
      }.foldLeft(false.B)(_ || _)
      oh(i) := mask(i) && canIssue(i) && !hasOlder
    }
    oh
  }

  val aluOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && !isLoad(i) && !isMulDiv(i))))
  val alu1Eligible = VecInit((0 until n).map { i =>
    canIssue(i) && !isLoad(i) && !isMulDiv(i) && !aluOH(i) &&
      entries(i).exu_jump === JUMP_NONE && !entries(i).wbu_csr_write &&
      !entries(i).is_ebreak && !entries(i).is_fencei && !entries(i).state.state
  })
  val alu1OH = oldestOH(alu1Eligible)
  val divOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isMulDiv(i))))
  val lsuOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isLoad(i))))

  val aluIdx = PriorityEncoder(aluOH.asUInt)
  val alu1Idx = PriorityEncoder(alu1OH.asUInt)
  val divIdx = PriorityEncoder(divOH.asUInt)
  val lsuIdx = PriorityEncoder(lsuOH.asUInt)
  val legacyOH = oldestOH(VecInit((0 until n).map(i => canIssue(i))))
  val legacyIdx = PriorityEncoder(legacyOH.asUInt)

  val aluResident = aluOH.asUInt.orR
  val alu1Resident = alu1OH.asUInt.orR
  val divResident = divOH.asUInt.orR
  val lsuResident = lsuOH.asUInt.orR
  val freshAlu0 = freshCanIssue(0) && !freshMem(0) && !freshMulDiv(0)
  val freshAlu1 = freshCanIssue(1) && !freshMem(1) && !freshMulDiv(1)
  val aluFresh0 = !aluResident && freshAlu0
  val aluFresh1 = !aluResident && !aluFresh0 && freshAlu1
  def alu1Capable(e: RSEntry): Bool =
    e.exu_jump === JUMP_NONE && !e.wbu_csr_write && !e.is_ebreak &&
      !e.is_fencei && !e.state.state
  val alu1Fresh0 = !alu1Resident && !aluFresh0 && freshAlu0 && alu1Capable(fresh(0))
  val alu1Fresh1 = !alu1Resident && !aluFresh1 && !alu1Fresh0 &&
    freshAlu1 && alu1Capable(fresh(1))
  val divFresh0 = !divResident && freshCanIssue(0) && freshMulDiv(0)
  val divFresh1 = !divResident && !divFresh0 && freshCanIssue(1) && freshMulDiv(1)
  val lsuFresh0 = !lsuResident && freshCanIssue(0) && freshMem(0)
  val lsuFresh1 = !lsuResident && !lsuFresh0 && freshCanIssue(1) && freshMem(1)
  val aluFresh = aluFresh0 || aluFresh1
  val alu1Fresh = alu1Fresh0 || alu1Fresh1
  val divFresh = divFresh0 || divFresh1
  val lsuFresh = lsuFresh0 || lsuFresh1
  io.fresh_issue_count := PopCount(Seq(
    io.issue_alu_fire && aluFresh,
    io.issue_alu1_fire && alu1Fresh,
    io.issue_div_fire && divFresh,
    io.issue_lsu_fire && lsuFresh))

  io.issue_alu_valid := (aluResident || aluFresh) && !io.flush
  io.issue_alu_bits := Mux(aluResident, issueEntries(aluIdx), Mux(aluFresh0, fresh(0), fresh(1)))
  io.issue_alu_idx   := aluIdx
  io.issue_alu1_valid := (alu1Resident || alu1Fresh) && !io.flush
  io.issue_alu1_bits := Mux(alu1Resident, issueEntries(alu1Idx), Mux(alu1Fresh0, fresh(0), fresh(1)))
  io.issue_alu1_idx   := alu1Idx
  io.issue_div_valid := (divResident || divFresh) && !io.flush
  io.issue_div_bits := Mux(divResident, issueEntries(divIdx), Mux(divFresh0, fresh(0), fresh(1)))
  io.issue_div_idx   := divIdx
  io.issue_lsu_valid := (lsuResident || lsuFresh) && !io.flush
  io.issue_lsu_bits := Mux(lsuResident, issueEntries(lsuIdx), Mux(lsuFresh0, fresh(0), fresh(1)))
  io.issue_lsu_idx   := lsuIdx
  io.control_ready := VecInit((0 until n).map(i =>
    canIssue(i) && entries(i).exu_jump =/= JUMP_NONE &&
      entries(i).exu_jump =/= JUMP_MERT)).asUInt.orR && !io.flush
  io.issue_valid := legacyOH.asUInt.orR && !io.flush
  io.issue_bits := issueEntries(legacyIdx)
  io.issue_idx   := legacyIdx

  when(io.cdb_valid && (io.cdb_pdest =/= 0.U)) {
    for (i <- 0 until n) {
      when(entries(i).valid) {
        when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb_pdest) {
          entries(i).src1_ready := true.B
          entries(i).src1_val   := io.cdb_val
        }
        when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb_pdest) {
          entries(i).src2_ready := true.B
          entries(i).src2_val   := io.cdb_val
        }
      }
    }
  }
  when(io.cdb1_valid && (io.cdb1_pdest =/= 0.U)) {
    for (i <- 0 until n) {
      when(entries(i).valid) {
        when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb1_pdest) {
          entries(i).src1_ready := true.B
          entries(i).src1_val   := io.cdb1_val
        }
        when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb1_pdest) {
          entries(i).src2_ready := true.B
          entries(i).src2_val   := io.cdb1_val
        }
      }
    }
  }
  when(io.cdb2_valid && (io.cdb2_pdest =/= 0.U)) {
    for (i <- 0 until n) {
      when(entries(i).valid) {
        when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb2_pdest) {
          entries(i).src1_ready := true.B
          entries(i).src1_val   := io.cdb2_val
        }
        when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb2_pdest) {
          entries(i).src2_ready := true.B
          entries(i).src2_val   := io.cdb2_val
        }
      }
    }
  }

  when(io.flush) {
    when(io.flush_all) {
      for (i <- 0 until n) { entries(i).valid := false.B }
    }.otherwise {
      for (i <- 0 until n) {
        when(entries(i).valid && (freeByRob(i) || (age(entries(i).rob_idx) > age(io.flush_idx)))) {
          entries(i).valid := false.B
        }
      }
    }
  }.otherwise {
    for (i <- 0 until n) {
      when(freeByRob(i)) {
        entries(i).valid := false.B
      }
    }
    when(io.issue_lsu_fire && !lsuFresh) {
      entries(io.issue_lsu_idx).issued := true.B
    }
    when(io.issue_alu_fire && !aluFresh) {
      entries(io.issue_alu_idx).issued := true.B
    }
    when(io.issue_alu1_fire && !alu1Fresh) {
      entries(io.issue_alu1_idx).issued := true.B
    }
    when(io.issue_div_fire && !divFresh) {
      entries(io.issue_div_idx).issued := true.B
    }
    when(io.issue_fire) {
      entries(io.issue_idx).issued := true.B
    }
    when(io.enq_fire && freeOrIssue.orR) {
      val e = WireDefault(io.enq_bits)
      e.valid := true.B
      e.issued := (io.issue_alu_fire && aluFresh0) ||
        (io.issue_alu1_fire && alu1Fresh0) ||
        (io.issue_div_fire && divFresh0) ||
        (io.issue_lsu_fire && lsuFresh0)
      val cdbHit1 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq_bits.src1_ready && (io.enq_bits.src1_phys === io.cdb_pdest)
      val cdbHit2 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq_bits.src2_ready && (io.enq_bits.src2_phys === io.cdb_pdest)
      val cdb1Hit1 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
        !io.enq_bits.src1_ready && (io.enq_bits.src1_phys === io.cdb1_pdest)
      val cdb1Hit2 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
        !io.enq_bits.src2_ready && (io.enq_bits.src2_phys === io.cdb1_pdest)
      val cdb2Hit1 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
        !io.enq_bits.src1_ready && (io.enq_bits.src1_phys === io.cdb2_pdest)
      val cdb2Hit2 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
        !io.enq_bits.src2_ready && (io.enq_bits.src2_phys === io.cdb2_pdest)
      when(cdbHit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb_val
      }
      when(cdb1Hit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb1_val
      }
      when(cdbHit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb_val
      }
      when(cdb1Hit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb1_val
      }
      when(cdb2Hit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb2_val
      }
      when(cdb2Hit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb2_val
      }
      entries(enqIdx) := e
    }
    when(io.enq1_fire && freeMask1.orR) {
      val e = WireDefault(io.enq1_bits)
      e.valid := true.B
      e.issued := (io.issue_alu_fire && aluFresh1) ||
        (io.issue_alu1_fire && alu1Fresh1) ||
        (io.issue_div_fire && divFresh1) ||
        (io.issue_lsu_fire && lsuFresh1)
      val cdbHit1 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq1_bits.src1_ready && (io.enq1_bits.src1_phys === io.cdb_pdest)
      val cdbHit2 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq1_bits.src2_ready && (io.enq1_bits.src2_phys === io.cdb_pdest)
      val cdb1Hit1 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
        !io.enq1_bits.src1_ready && (io.enq1_bits.src1_phys === io.cdb1_pdest)
      val cdb1Hit2 = io.cdb1_valid && (io.cdb1_pdest =/= 0.U) &&
        !io.enq1_bits.src2_ready && (io.enq1_bits.src2_phys === io.cdb1_pdest)
      val cdb2Hit1 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
        !io.enq1_bits.src1_ready && (io.enq1_bits.src1_phys === io.cdb2_pdest)
      val cdb2Hit2 = io.cdb2_valid && (io.cdb2_pdest =/= 0.U) &&
        !io.enq1_bits.src2_ready && (io.enq1_bits.src2_phys === io.cdb2_pdest)
      when(cdbHit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb_val
      }
      when(cdb1Hit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb1_val
      }
      when(cdbHit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb_val
      }
      when(cdb1Hit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb1_val
      }
      when(cdb2Hit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb2_val
      }
      when(cdb2Hit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb2_val
      }
      entries(enq1Idx) := e
    }
  }
}
