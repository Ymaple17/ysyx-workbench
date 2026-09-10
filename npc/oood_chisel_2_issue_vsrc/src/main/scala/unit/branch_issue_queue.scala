package unit

import chisel3._
import chisel3.util._
import common.OoOParams

/**
  * Small, independent scheduler for ordinary control-flow instructions.
  *
  * Keeping branches outside the integer RS lets ALU0 continue issuing while a
  * branch waits for operands or for its BRU dispatch register to drain.  The
  * queue still observes both CDB lanes and uses ROB-relative age for selection.
  */
class BranchIssueQueue(depth: Int = OoOParams.BRQ_SIZE) extends Module {
  require(depth >= 2 && isPow2(depth))

  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(new RSEntry))
    val issue = Decoupled(new RSEntry)

    val robHead = Input(UInt(OoOParams.ROB_PTR_W.W))
    val cdb0Valid = Input(Bool())
    val cdb0Pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb0Value = Input(UInt(32.W))
    val cdb1Valid = Input(Bool())
    val cdb1Pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb1Value = Input(UInt(32.W))
    val cdb2Valid = Input(Bool())
    val cdb2Pdest = Input(UInt(OoOParams.PHYS_W.W))
    val cdb2Value = Input(UInt(32.W))

    val free0Valid = Input(Bool())
    val free0Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free1Valid = Input(Bool())
    val free1Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
    val free2Valid = Input(Bool())
    val free2Rob = Input(UInt(OoOParams.ROB_PTR_W.W))
    val freeDirectValid = Input(Bool())
    val freeDirectRob = Input(UInt(OoOParams.ROB_PTR_W.W))

    val flush = Input(Bool())
    val flushIdx = Input(UInt(OoOParams.ROB_PTR_W.W))
    val flushAll = Input(Bool())

    val count = Output(UInt(log2Ceil(depth + 1).W))
    val space = Output(UInt(log2Ceil(depth + 1).W))
    val full = Output(Bool())
  })

  val entries = RegInit(VecInit(Seq.fill(depth)(0.U.asTypeOf(new RSEntry))))

  def age(rob: UInt): UInt =
    (rob - io.robHead)(OoOParams.ROB_PTR_W - 1, 0)

  val freeByCompletion = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    freeByCompletion(i) := entries(i).valid && (
      (io.free0Valid && entries(i).rob_idx === io.free0Rob) ||
      (io.free1Valid && entries(i).rob_idx === io.free1Rob) ||
      (io.free2Valid && entries(i).rob_idx === io.free2Rob) ||
      (io.freeDirectValid && entries(i).rob_idx === io.freeDirectRob))
  }

  val issueView = Wire(Vec(depth, new RSEntry))
  val readyMask = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val e = WireDefault(entries(i))
    val cdb0Hit1 = io.cdb0Valid && io.cdb0Pdest =/= 0.U &&
      !entries(i).src1_ready && entries(i).src1_phys === io.cdb0Pdest
    val cdb0Hit2 = io.cdb0Valid && io.cdb0Pdest =/= 0.U &&
      !entries(i).src2_ready && entries(i).src2_phys === io.cdb0Pdest
    val cdb1Hit1 = io.cdb1Valid && io.cdb1Pdest =/= 0.U &&
      !entries(i).src1_ready && entries(i).src1_phys === io.cdb1Pdest
    val cdb1Hit2 = io.cdb1Valid && io.cdb1Pdest =/= 0.U &&
      !entries(i).src2_ready && entries(i).src2_phys === io.cdb1Pdest
    val cdb2Hit1 = io.cdb2Valid && io.cdb2Pdest =/= 0.U &&
      !entries(i).src1_ready && entries(i).src1_phys === io.cdb2Pdest
    val cdb2Hit2 = io.cdb2Valid && io.cdb2Pdest =/= 0.U &&
      !entries(i).src2_ready && entries(i).src2_phys === io.cdb2Pdest
    when(cdb0Hit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb0Value
    }
    when(cdb0Hit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb0Value
    }
    when(cdb1Hit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb1Value
    }
    when(cdb1Hit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb1Value
    }
    when(cdb2Hit1) {
      e.src1_ready := true.B
      e.src1_val := io.cdb2Value
    }
    when(cdb2Hit2) {
      e.src2_ready := true.B
      e.src2_val := io.cdb2Value
    }
    issueView(i) := e
    readyMask(i) := entries(i).valid && !entries(i).issued &&
      e.src1_ready && e.src2_ready && !freeByCompletion(i)
  }

  val oldestReady = Wire(Vec(depth, Bool()))
  for (i <- 0 until depth) {
    val hasOlder = (0 until depth).map { j =>
      readyMask(j) && age(entries(j).rob_idx) < age(entries(i).rob_idx)
    }.foldLeft(false.B)(_ || _)
    oldestReady(i) := readyMask(i) && !hasOlder
  }
  val issueIdx = PriorityEncoder(oldestReady.asUInt)
  val residentIssueValid = oldestReady.asUInt.orR

  val validCount = PopCount(entries.map(_.valid))
  val invalidMask = VecInit(entries.map(e => !e.valid)).asUInt
  val reusableMask = invalidMask | freeByCompletion.asUInt
  val enqIdx = PriorityEncoder(reusableMask)
  io.enq.ready := reusableMask.orR && !io.flush
  io.count := validCount
  io.space := depth.U - validCount
  io.full := !reusableMask.orR

  val enqView = WireDefault(io.enq.bits)
  when(io.cdb0Valid && io.cdb0Pdest =/= 0.U &&
      !io.enq.bits.src1_ready && io.enq.bits.src1_phys === io.cdb0Pdest) {
    enqView.src1_ready := true.B
    enqView.src1_val := io.cdb0Value
  }
  when(io.cdb0Valid && io.cdb0Pdest =/= 0.U &&
      !io.enq.bits.src2_ready && io.enq.bits.src2_phys === io.cdb0Pdest) {
    enqView.src2_ready := true.B
    enqView.src2_val := io.cdb0Value
  }
  when(io.cdb1Valid && io.cdb1Pdest =/= 0.U &&
      !io.enq.bits.src1_ready && io.enq.bits.src1_phys === io.cdb1Pdest) {
    enqView.src1_ready := true.B
    enqView.src1_val := io.cdb1Value
  }
  when(io.cdb1Valid && io.cdb1Pdest =/= 0.U &&
      !io.enq.bits.src2_ready && io.enq.bits.src2_phys === io.cdb1Pdest) {
    enqView.src2_ready := true.B
    enqView.src2_val := io.cdb1Value
  }
  when(io.cdb2Valid && io.cdb2Pdest =/= 0.U &&
      !io.enq.bits.src1_ready && io.enq.bits.src1_phys === io.cdb2Pdest) {
    enqView.src1_ready := true.B
    enqView.src1_val := io.cdb2Value
  }
  when(io.cdb2Valid && io.cdb2Pdest =/= 0.U &&
      !io.enq.bits.src2_ready && io.enq.bits.src2_phys === io.cdb2Pdest) {
    enqView.src2_ready := true.B
    enqView.src2_val := io.cdb2Value
  }
  val freshIssueValid = io.enq.valid && io.enq.ready &&
    enqView.src1_ready && enqView.src2_ready && !residentIssueValid
  io.issue.valid := (residentIssueValid || freshIssueValid) && !io.flush
  io.issue.bits := Mux(residentIssueValid, issueView(issueIdx), enqView)

  // CDB wakeup is independent of selective cancellation.  An older branch
  // survives a younger redirect and must not lose an operand that completes
  // on the redirect cycle.
  for (i <- 0 until depth) {
    when(io.cdb0Valid && io.cdb0Pdest =/= 0.U && entries(i).valid) {
      when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb0Pdest) {
        entries(i).src1_ready := true.B
        entries(i).src1_val := io.cdb0Value
      }
      when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb0Pdest) {
        entries(i).src2_ready := true.B
        entries(i).src2_val := io.cdb0Value
      }
    }
    when(io.cdb1Valid && io.cdb1Pdest =/= 0.U && entries(i).valid) {
      when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb1Pdest) {
        entries(i).src1_ready := true.B
        entries(i).src1_val := io.cdb1Value
      }
      when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb1Pdest) {
        entries(i).src2_ready := true.B
        entries(i).src2_val := io.cdb1Value
      }
    }
    when(io.cdb2Valid && io.cdb2Pdest =/= 0.U && entries(i).valid) {
      when(!entries(i).src1_ready && entries(i).src1_phys === io.cdb2Pdest) {
        entries(i).src1_ready := true.B
        entries(i).src1_val := io.cdb2Value
      }
      when(!entries(i).src2_ready && entries(i).src2_phys === io.cdb2Pdest) {
        entries(i).src2_ready := true.B
        entries(i).src2_val := io.cdb2Value
      }
    }
  }

  when(io.flush) {
    for (i <- 0 until depth) {
      when(freeByCompletion(i) || io.flushAll ||
          (entries(i).valid && age(entries(i).rob_idx) > age(io.flushIdx))) {
        entries(i).valid := false.B
      }
    }
  }.otherwise {
    for (i <- 0 until depth) {
      when(freeByCompletion(i)) {
        entries(i).valid := false.B
      }
    }

    when(io.issue.fire && residentIssueValid) {
      entries(issueIdx).issued := true.B
    }
    when(io.enq.fire) {
      val e = WireDefault(enqView)
      e.valid := true.B
      e.issued := freshIssueValid && io.issue.ready
      entries(enqIdx) := e
    }
  }
}
