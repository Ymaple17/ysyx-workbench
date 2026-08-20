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
  val bp_index   = UInt(log2Ceil(BHT_SIZE).W)
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
    val issue_div_valid = Output(Bool())
    val issue_div_bits  = Output(new RSEntry)
    val issue_div_idx   = Output(UInt(log2Ceil(n).W))
    val issue_lsu_valid = Output(Bool())
    val issue_lsu_bits  = Output(new RSEntry)
    val issue_lsu_idx   = Output(UInt(log2Ceil(n).W))
    val issue_valid = Output(Bool())
    val issue_bits  = Output(new RSEntry)
    val issue_idx   = Output(UInt(log2Ceil(n).W))
    val issue_fire  = Input(Bool())

    val enq1_fire = Input(Bool())
    val enq1_bits = Input(new RSEntry)
    val enq1_idx  = Output(UInt(log2Ceil(n).W))

    val free_rob_fire = Input(Bool())
    val free_rob_idx  = Input(UInt(OoOParams.ROB_PTR_W.W))

    val cdb_valid = Input(Bool())
    val cdb_pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb_val   = Input(UInt(32.W))

    val flush     = Input(Bool())
    val flush_idx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val flush_all = Input(Bool())
    val space     = Output(UInt(log2Ceil(n + 1).W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new RSEntry))))
  io.count := PopCount(VecInit(entries.map(_.valid)).asUInt)
  io.space := n.U - io.count

  def age(idx: UInt): UInt = (idx - io.rob_head)(OoOParams.ROB_PTR_W - 1, 0)

  val freeByRob = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    freeByRob(i) := io.free_rob_fire && entries(i).valid && (entries(i).rob_idx === io.free_rob_idx)
  }

  val canIssue = Wire(Vec(n, Bool()))
  val isMulDiv = Wire(Vec(n, Bool()))
  val isLoad   = Wire(Vec(n, Bool()))
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

    val waitOlderStore = entries(i).valid && entries(i).lsu_mem_valid &&
      !entries(i).lsu_mem_write && hasOlderPendingStore(entries(i).rob_idx)
    val ready = entries(i).valid && !entries(i).issued && entries(i).src1_ready && entries(i).src2_ready && !freeByRob(i)
    canIssue(i) := ready && !waitOlderStore
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
  val divOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isMulDiv(i))))
  val lsuOH = oldestOH(VecInit((0 until n).map(i => canIssue(i) && isLoad(i))))

  val aluIdx = PriorityEncoder(aluOH.asUInt)
  val divIdx = PriorityEncoder(divOH.asUInt)
  val lsuIdx = PriorityEncoder(lsuOH.asUInt)
  val legacyOH = oldestOH(VecInit((0 until n).map(i => canIssue(i))))
  val legacyIdx = PriorityEncoder(legacyOH.asUInt)

  io.issue_alu_valid := aluOH.asUInt.orR && !io.flush
  io.issue_alu_bits  := entries(aluIdx)
  io.issue_alu_idx   := aluIdx
  io.issue_div_valid := divOH.asUInt.orR && !io.flush
  io.issue_div_bits  := entries(divIdx)
  io.issue_div_idx   := divIdx
  io.issue_lsu_valid := lsuOH.asUInt.orR && !io.flush
  io.issue_lsu_bits  := entries(lsuIdx)
  io.issue_lsu_idx   := lsuIdx
  io.issue_valid := legacyOH.asUInt.orR && !io.flush
  io.issue_bits  := entries(legacyIdx)
  io.issue_idx   := legacyIdx

  val willFreeRob = freeByRob.asUInt.orR
  val freeMask = VecInit(entries.map(e => !e.valid)).asUInt
  io.full := !freeMask.orR && !willFreeRob
  val freeOrIssue = freeMask | Mux(willFreeRob, freeByRob.asUInt, 0.U)
  val enqIdx = PriorityEncoder(freeOrIssue)
  val enqOH = PriorityEncoderOH(freeOrIssue)
  val freeMask1 = freeOrIssue & ~enqOH.asUInt
  val enq1Idx = PriorityEncoder(freeMask1)
  io.enq1_idx := enq1Idx

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
    when(io.issue_fire) {
      entries(io.issue_idx).issued := true.B
    }
    when(io.enq_fire && freeOrIssue.orR) {
      val e = WireDefault(io.enq_bits)
      e.valid := true.B
      e.issued := false.B
      val cdbHit1 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq_bits.src1_ready && (io.enq_bits.src1_phys === io.cdb_pdest)
      val cdbHit2 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq_bits.src2_ready && (io.enq_bits.src2_phys === io.cdb_pdest)
      when(cdbHit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb_val
      }
      when(cdbHit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb_val
      }
      entries(enqIdx) := e
    }
    when(io.enq1_fire && freeMask1.orR) {
      val e = WireDefault(io.enq1_bits)
      e.valid := true.B
      e.issued := false.B
      val cdbHit1 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq1_bits.src1_ready && (io.enq1_bits.src1_phys === io.cdb_pdest)
      val cdbHit2 = io.cdb_valid && (io.cdb_pdest =/= 0.U) &&
        !io.enq1_bits.src2_ready && (io.enq1_bits.src2_phys === io.cdb_pdest)
      when(cdbHit1) {
        e.src1_ready := true.B
        e.src1_val   := io.cdb_val
      }
      when(cdbHit2) {
        e.src2_ready := true.B
        e.src2_val   := io.cdb_val
      }
      entries(enq1Idx) := e
    }
  }
}
