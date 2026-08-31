package unit

import chisel3._
import chisel3.util._
import common.BPU_Config._
import common.OoOParams

class TraceCommit extends Bundle {
  val pc = UInt(32.W)
  val inst = UInt(32.W)
  val bpIndex = UInt(BP_META_WIDTH.W)
  val actualNextPc = UInt(32.W)
}

class CommittedTraceFill extends Bundle {
  val startPc = UInt(32.W)
  val context = UInt(32.W)
  val pc = Vec(OoOParams.FETCH_WIDTH, UInt(32.W))
  val inst = Vec(OoOParams.FETCH_WIDTH, UInt(32.W))
  val nextPc = UInt(32.W)
}

class CommittedTraceData extends Bundle {
  val pcTag = UInt(32.W)
  val contextTag = UInt(32.W)
  val pc = Vec(OoOParams.FETCH_WIDTH, UInt(32.W))
  val inst = Vec(OoOParams.FETCH_WIDTH, UInt(32.W))
  val nextPc = UInt(32.W)
}

class CommittedTraceBuilder extends Module {
  private val width = OoOParams.COMMIT_WIDTH
  require(width == 4 && OoOParams.FETCH_WIDTH == 4)

  val io = IO(new Bundle {
    val commit = Input(Vec(width, Valid(new TraceCommit)))
    val clear = Input(Bool())
    val fill = Output(Vec(width, Valid(new CommittedTraceFill)))
  })

  val history = Reg(Vec(4, new TraceCommit))
  val count = RegInit(0.U(3.W))
  val historyStep = Wire(Vec(width + 1, Vec(4, new TraceCommit)))
  val countStep = Wire(Vec(width + 1, UInt(3.W)))
  historyStep(0) := history
  countStep(0) := count

  def context(index: UInt): UInt = {
    val ghr = index(GHR_LENGTH - 1, 0)
    val path = index(GHR_LENGTH + PATH_HISTORY_LENGTH - 1, GHR_LENGTH)
    ghr ^ path
  }

  def changesRas(inst: UInt): Bool = {
    val opcode = inst(6, 0)
    val rd = inst(11, 7)
    opcode === "b1100111".U || (opcode === "b1101111".U && rd =/= 0.U)
  }

  for (lane <- 0 until width) {
    io.fill(lane).valid := io.commit(lane).valid && countStep(lane) === 4.U &&
      !io.clear && !changesRas(historyStep(lane)(0).inst) &&
      !changesRas(historyStep(lane)(1).inst) &&
      !changesRas(historyStep(lane)(2).inst)
    io.fill(lane).bits.startPc := historyStep(lane)(0).pc
    io.fill(lane).bits.context := context(historyStep(lane)(0).bpIndex)
    for (slot <- 0 until 4) {
      io.fill(lane).bits.pc(slot) := historyStep(lane)(slot).pc
      io.fill(lane).bits.inst(slot) := historyStep(lane)(slot).inst
    }
    io.fill(lane).bits.nextPc := io.commit(lane).bits.pc

    historyStep(lane + 1) := historyStep(lane)
    countStep(lane + 1) := countStep(lane)
    when(io.commit(lane).valid) {
      when(countStep(lane) === 4.U) {
        for (slot <- 0 until 3) {
          historyStep(lane + 1)(slot) := historyStep(lane)(slot + 1)
        }
        historyStep(lane + 1)(3) := io.commit(lane).bits
      }.otherwise {
        historyStep(lane + 1)(countStep(lane)(1, 0)) := io.commit(lane).bits
        countStep(lane + 1) := countStep(lane) + 1.U
      }
    }
  }

  when(io.clear) {
    count := 0.U
  }.otherwise {
    history := historyStep(width)
    count := countStep(width)
  }
}

class CommittedTraceCache(entries: Int = 256, fillWidth: Int = 4) extends Module {
  private val banks = 4
  private val sets = entries / banks
  private val setWidth = log2Ceil(sets)
  require(entries >= banks && isPow2(entries) && isPow2(sets))
  require(fillWidth == OoOParams.COMMIT_WIDTH)

  val io = IO(new Bundle {
    val lookupPc = Input(UInt(32.W))
    val lookupContext = Input(UInt(32.W))
    val hit = Output(Bool())
    val pc = Output(Vec(4, UInt(32.W)))
    val inst = Output(Vec(4, UInt(32.W)))
    val nextPc = Output(UInt(32.W))
    val confidence = Output(UInt(2.W))
    val fill = Input(Vec(fillWidth, Valid(new CommittedTraceFill)))
    val invalidate = Input(Bool())
  })

  val valid = RegInit(VecInit(Seq.fill(banks)(VecInit(Seq.fill(sets)(false.B)))))
  val confidence = RegInit(VecInit(Seq.fill(banks)(VecInit(Seq.fill(sets)(0.U(2.W))))))
  val data = Seq.fill(banks)(Mem(sets, new CommittedTraceData))

  def foldedContext(value: UInt): UInt = {
    VecInit((0 until setWidth).map { bit =>
      (bit until 32 by setWidth).map(value(_)).reduce(_ ^ _)
    }).asUInt
  }

  def bank(pc: UInt): UInt = pc(3, 2)
  def set(pc: UInt, context: UInt): UInt =
    pc(setWidth + 3, 4) ^ foldedContext(context)

  val lookupBank = bank(io.lookupPc)
  val lookupSet = set(io.lookupPc, io.lookupContext)
  val lookupDataByBank = Wire(Vec(banks, new CommittedTraceData))
  for (b <- 0 until banks) {
    lookupDataByBank(b) := data(b).read(lookupSet)
  }
  val lookupData = lookupDataByBank(lookupBank)
  val lookupTagHit = valid(lookupBank)(lookupSet) &&
    lookupData.pcTag === io.lookupPc &&
    lookupData.contextTag === io.lookupContext
  io.confidence := Mux(lookupTagHit, confidence(lookupBank)(lookupSet), 0.U)
  io.hit := lookupTagHit && confidence(lookupBank)(lookupSet) >= 2.U
  io.pc := lookupData.pc
  io.inst := lookupData.inst
  io.nextPc := lookupData.nextPc

  when(io.invalidate) {
    for (b <- 0 until banks; s <- 0 until sets) {
      valid(b)(s) := false.B
      confidence(b)(s) := 0.U
    }
  }.otherwise {
    // Consecutive windows naturally spread over PC banks. If a control-flow
    // group sends several fills to one bank, the youngest architectural fill
    // owns that bank's single write port.
    for (b <- 0 until banks) {
      val candidates = VecInit((0 until fillWidth).map(lane =>
        io.fill(lane).valid && bank(io.fill(lane).bits.startPc) === b.U))
      val selected = Wire(new CommittedTraceFill)
      selected := io.fill(0).bits
      for (lane <- 0 until fillWidth) {
        when(candidates(lane)) {
          selected := io.fill(lane).bits
        }
      }
      val fillSet = set(selected.startPc, selected.context)
      val oldData = data(b).read(fillSet)
      val sameTag = valid(b)(fillSet) && oldData.pcTag === selected.startPc &&
        oldData.contextTag === selected.context
      val sameSequence = sameTag && oldData.nextPc === selected.nextPc &&
        VecInit((0 until 4).map(slot => oldData.pc(slot) === selected.pc(slot) &&
          oldData.inst(slot) === selected.inst(slot))).asUInt.andR
      val newData = Wire(new CommittedTraceData)
      newData.pcTag := selected.startPc
      newData.contextTag := selected.context
      newData.pc := selected.pc
      newData.inst := selected.inst
      newData.nextPc := selected.nextPc

      when(candidates.asUInt.orR) {
        data(b).write(fillSet, newData)
        valid(b)(fillSet) := true.B
        confidence(b)(fillSet) := Mux(!sameTag, 1.U,
          Mux(sameSequence, Mux(confidence(b)(fillSet) === 3.U,
            3.U, confidence(b)(fillSet) + 1.U), 0.U))
      }
    }
  }

  when(!reset.asBool && io.hit) {
    assert(io.pc(0) === io.lookupPc, "trace hit must start at the requested PC")
  }
}
