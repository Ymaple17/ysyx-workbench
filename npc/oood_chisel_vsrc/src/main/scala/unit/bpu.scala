package unit

import chisel3._
import chisel3.util._
import common.IMM_TYPE._
import common.BPU_Config._
import core.CoreConfig

class BPU_IO(bhtSize: Int = BHT_SIZE, indirectSize: Int = INDIRECT_TARGET_SIZE) extends Bundle {
  val predict_pc = Input(UInt(32.W))
  val predict_inst = Input(UInt(32.W))
  val predict_pc1 = Input(UInt(32.W))
  val predict_inst1 = Input(UInt(32.W))

  val bp_valid = Output(Bool())
  val bp_taken = Output(Bool())
  val bp_target = Output(UInt(32.W))
  val bp_index = Output(UInt(log2Ceil(bhtSize).W))
  val bp_tagged_hit = Output(Bool())
  val bp_tage_use_alt = Output(Bool())
  val bp_indirect_hit = Output(Bool())
  val bp_itage_hit = Output(Bool())

  val bp1_valid = Output(Bool())
  val bp1_taken = Output(Bool())
  val bp1_target = Output(UInt(32.W))
  val bp1_index = Output(UInt(log2Ceil(bhtSize).W))
  val bp1_tagged_hit = Output(Bool())
  val bp1_tage_use_alt = Output(Bool())
  val bp1_indirect_hit = Output(Bool())
  val bp1_itage_hit = Output(Bool())

  val update_pc = Input(UInt(32.W))
  val update_target = Input(UInt(32.W))
  val update_valid = Input(Bool())
  val update_taken = Input(Bool())
  val update_is_branch = Input(Bool())
  val update_is_jalr = Input(Bool())
  val update_index = Input(UInt(log2Ceil(bhtSize).W))

  val update_is_call = Input(Bool())
  val update_is_ret = Input(Bool())

  val spec_advance_valid = Input(Bool())
  val spec_advance_mask = Input(UInt(2.W))
  val recover_valid = Input(Bool())
  val recover_pc = Input(UInt(32.W))
  val recover_index = Input(UInt(log2Ceil(bhtSize).W))
  val recover_taken = Input(Bool())
  val recover_is_branch = Input(Bool())
  val recover_is_call = Input(Bool())
  val recover_is_ret = Input(Bool())
  val recover_ras = Input(Vec(RAS_SIZE, UInt(32.W)))
  val recover_ras_ptr = Input(UInt(log2Ceil(RAS_SIZE).W))
  val recover_ras_count = Input(UInt(log2Ceil(RAS_SIZE + 1).W))
  val reset_spec = Input(Bool())

  val spec_ghr = Output(UInt(log2Ceil(bhtSize).W))
  val spec_ras = Output(Vec(RAS_SIZE, UInt(32.W)))
  val spec_ras_ptr = Output(UInt(log2Ceil(RAS_SIZE).W))
  val spec_ras_count = Output(UInt(log2Ceil(RAS_SIZE + 1).W))
  val tage_alloc = Output(Bool())
  val itage_alloc = Output(Bool())
}

class BPU(
  val conf: CoreConfig,
  bhtSize: Int = BHT_SIZE,
  indirectSize: Int = INDIRECT_TARGET_SIZE,
  tageTableSize: Int = TAGE_TABLE_SIZE,
  itageTableSize: Int = ITAGE_TABLE_SIZE
) extends Module {
  val io = IO(new BPU_IO(bhtSize, indirectSize))

  require(isPow2(bhtSize) && isPow2(indirectSize))
  require(isPow2(tageTableSize) && isPow2(itageTableSize))

  private val bhtW = log2Ceil(bhtSize)
  private val ittW = log2Ceil(indirectSize)
  private val tageW = log2Ceil(tageTableSize)
  private val itageW = log2Ceil(itageTableSize)
  private val rasW = log2Ceil(RAS_SIZE)
  private val tageHistories = TAGE_HISTORY_LENGTHS.map(math.min(_, bhtW)).distinct
  private val itageHistories = ITAGE_HISTORY_LENGTHS.map(math.min(_, bhtW)).distinct
  private val tageCount = tageHistories.length
  private val itageCount = itageHistories.length
  private val tageProviderW = log2Ceil(tageCount + 1)
  private val itageProviderW = log2Ceil(itageCount + 1)

  val bht = RegInit(VecInit(Seq.fill(bhtSize)(BHT_INIT.U(2.W))))
  val bht_valid = RegInit(VecInit(Seq.fill(bhtSize)(false.B)))
  val commitGhr = RegInit(0.U(bhtW.W))
  val specGhr = RegInit(0.U(bhtW.W))

  val tage_valid = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(false.B))))
  val tage_tag = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(0.U(TAGGED_BHT_TAG_BITS.W)))))
  val tage_ctr = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(3.U(3.W)))))
  val tage_useful = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(0.U(2.W)))))

  // A PC-only target table is the alternate provider for history-tagged ITAGE tables.
  val itt_valid = RegInit(VecInit(Seq.fill(indirectSize)(false.B)))
  val itt_tag = RegInit(VecInit(Seq.fill(indirectSize)(0.U(TAGGED_BHT_TAG_BITS.W))))
  val itt_target = RegInit(VecInit(Seq.fill(indirectSize)(0.U(32.W))))
  val itt_conf = RegInit(VecInit(Seq.fill(indirectSize)(0.U(2.W))))
  val itage_valid = Seq.fill(itageCount)(RegInit(VecInit(Seq.fill(itageTableSize)(false.B))))
  val itage_tag = Seq.fill(itageCount)(RegInit(VecInit(Seq.fill(itageTableSize)(0.U(TAGGED_BHT_TAG_BITS.W)))))
  val itage_target = Seq.fill(itageCount)(RegInit(VecInit(Seq.fill(itageTableSize)(0.U(32.W)))))
  val itage_conf = Seq.fill(itageCount)(RegInit(VecInit(Seq.fill(itageTableSize)(0.U(2.W)))))

  val commitRas = RegInit(VecInit(Seq.fill(RAS_SIZE)(0.U(32.W))))
  val commitRasPtr = RegInit(0.U(rasW.W))
  val commitRasCount = RegInit(0.U(log2Ceil(RAS_SIZE + 1).W))
  val specRas = RegInit(VecInit(Seq.fill(RAS_SIZE)(0.U(32.W))))
  val specRasPtr = RegInit(0.U(rasW.W))
  val specRasCount = RegInit(0.U(log2Ceil(RAS_SIZE + 1).W))

  io.spec_ghr := specGhr
  io.spec_ras := specRas
  io.spec_ras_ptr := specRasPtr
  io.spec_ras_count := specRasCount

  def satInc2(v: UInt): UInt = Mux(v === 3.U, 3.U, v + 1.U)
  def satDec2(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)
  def satInc3(v: UInt): UInt = Mux(v === 7.U, 7.U, v + 1.U)
  def satDec3(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)

  def foldHistory(history: UInt, historyLength: Int, width: Int): UInt = {
    VecInit((0 until width).map { bit =>
      val taps = (bit until historyLength by width).map(history(_))
      if (taps.isEmpty) false.B else taps.reduce(_ ^ _)
    }).asUInt
  }

  def historyIndex(pc: UInt, history: UInt, historyLength: Int, width: Int): UInt =
    pc(width + 1, 2) ^ foldHistory(history, historyLength, width)

  def historyTag(pc: UInt, history: UInt, historyLength: Int): UInt = {
    val pcFold = pc(TAGGED_BHT_TAG_BITS + 1, 2) ^
      pc(2 * TAGGED_BHT_TAG_BITS + 1, TAGGED_BHT_TAG_BITS + 2)
    pcFold ^ foldHistory(history, historyLength, TAGGED_BHT_TAG_BITS) ^
      foldHistory(Cat(history(bhtW - 2, 0), history(bhtW - 1)), historyLength, TAGGED_BHT_TAG_BITS)
  }

  def ittIndex(pc: UInt): UInt = pc(ittW + 1, 2)
  def pcTag(pc: UInt): UInt = pc(TAGGED_BHT_TAG_BITS + ittW + 1, ittW + 2)

  class DirectionResult extends Bundle {
    val taken = Bool()
    val taggedHit = Bool()
    val useAlternate = Bool()
    val providerRank = UInt(tageProviderW.W)
    val providerPred = Bool()
    val alternatePred = Bool()
  }

  def directionPredict(pc: UInt, history: UInt, baseIndex: UInt, coldStaticTaken: Bool): DirectionResult = {
    val res = Wire(new DirectionResult)
    val baseValue = bht(baseIndex)
    val basePredict = if (BHT_COLD_STATIC) Mux(bht_valid(baseIndex), baseValue(1), coldStaticTaken) else baseValue(1)
    val indices = tageHistories.map(historyIndex(pc, history, _, tageW))
    val tags = tageHistories.map(historyTag(pc, history, _))
    val hits = (0 until tageCount).map(i => tage_valid(i)(indices(i)) && tage_tag(i)(indices(i)) === tags(i))

    val providerRank = Wire(UInt(tageProviderW.W))
    val providerPred = Wire(Bool())
    val providerCtr = Wire(UInt(3.W))
    val providerUseful = Wire(UInt(2.W))
    providerRank := 0.U
    providerPred := basePredict
    providerCtr := Cat(baseValue, 0.U(1.W))
    providerUseful := 0.U
    for (i <- 0 until tageCount) {
      when(hits(i)) {
        providerRank := (i + 1).U
        providerPred := tage_ctr(i)(indices(i))(2)
        providerCtr := tage_ctr(i)(indices(i))
        providerUseful := tage_useful(i)(indices(i))
      }
    }

    val alternatePred = Wire(Bool())
    alternatePred := basePredict
    for (i <- 0 until tageCount) {
      when(hits(i) && (i + 1).U < providerRank) {
        alternatePred := tage_ctr(i)(indices(i))(2)
      }
    }

    val providerWeak = providerCtr === 3.U || providerCtr === 4.U
    val useAlternate = providerRank =/= 0.U && providerUseful === 0.U && providerWeak
    res.taken := Mux(useAlternate, alternatePred, providerPred)
    res.taggedHit := providerRank =/= 0.U
    res.useAlternate := useAlternate
    res.providerRank := providerRank
    res.providerPred := providerPred
    res.alternatePred := alternatePred
    res
  }

  class IndirectResult extends Bundle {
    val valid = Bool()
    val target = UInt(32.W)
    val taggedHit = Bool()
    val providerRank = UInt(itageProviderW.W)
  }

  def indirectPredict(pc: UInt, history: UInt): IndirectResult = {
    val res = Wire(new IndirectResult)
    val baseIdx = ittIndex(pc)
    val baseHit = itt_valid(baseIdx) && itt_tag(baseIdx) === pcTag(pc)
    val indices = itageHistories.map(historyIndex(pc, history, _, itageW))
    val tags = itageHistories.map(historyTag(pc, history, _))
    val hits = (0 until itageCount).map(i => itage_valid(i)(indices(i)) && itage_tag(i)(indices(i)) === tags(i))

    val providerRank = Wire(UInt(itageProviderW.W))
    val providerTarget = Wire(UInt(32.W))
    val providerConf = Wire(UInt(2.W))
    providerRank := 0.U
    providerTarget := itt_target(baseIdx)
    providerConf := Mux(baseHit, itt_conf(baseIdx), 0.U)
    for (i <- 0 until itageCount) {
      when(hits(i)) {
        providerRank := (i + 1).U
        providerTarget := itage_target(i)(indices(i))
        providerConf := itage_conf(i)(indices(i))
      }
    }

    val taggedReady = providerRank =/= 0.U && providerConf === 3.U
    val baseReady = baseHit && itt_conf(baseIdx) === 3.U
    res.valid := taggedReady || baseReady
    res.target := Mux(taggedReady, providerTarget, itt_target(baseIdx))
    res.taggedHit := taggedReady
    res.providerRank := providerRank
    res
  }

  class PredResult extends Bundle {
    val valid = Bool()
    val taken = Bool()
    val target = UInt(32.W)
    val index = UInt(bhtW.W)
    val taggedHit = Bool()
    val tageUseAlternate = Bool()
    val indirectHit = Bool()
    val itageHit = Bool()
    val isBranch = Bool()
    val isCall = Bool()
    val isRet = Bool()
  }

  def predict(pc: UInt, inst: UInt, history: UInt,
              rasStack: Vec[UInt], rasPtr: UInt, rasCount: UInt): PredResult = {
    val res = Wire(new PredResult)
    val opcode = inst(6, 0)
    val isJal = opcode === "b1101111".U
    val isJalr = opcode === "b1100111".U
    val isBranch = opcode === "b1100011".U

    val imm = Module(new IMM)
    imm.io.inst := inst
    imm.io.imm_type := Mux(isJal, ImmJ, ImmB)

    val rs1IsRa = inst(19, 15) === 1.U
    val rdIsRa = inst(11, 7) === 1.U
    val isRet = isJalr && rs1IsRa
    val isCall = rdIsRa && (isJal || (isJalr && !rs1IsRa))
    val isIndirect = isJalr && !isRet
    val bhtIndex = pc(bhtW + 1, 2) ^ history
    val direction = directionPredict(pc, history, bhtIndex, imm.io.imm_ext(31))
    val indirect = indirectPredict(pc, history)
    val rasEmpty = rasCount === 0.U
    val rasTopIdx = Mux(rasPtr === 0.U, (RAS_SIZE - 1).U(rasW.W),
      (rasPtr - 1.U)(rasW - 1, 0))
    val rasTarget = Mux1H((0 until RAS_SIZE).map(i => (rasTopIdx === i.U) -> rasStack(i)))

    val predValid = isJal || isBranch || isRet || (isIndirect && indirect.valid)
    val predTaken = predValid && MuxCase(false.B, Seq(
      isJal -> true.B,
      isBranch -> direction.taken,
      isRet -> !rasEmpty,
      isIndirect -> indirect.valid
    ))
    val target = MuxCase(pc + imm.io.imm_ext, Seq(
      isRet -> rasTarget,
      isIndirect -> indirect.target
    ))

    res.valid := predValid
    res.taken := predTaken
    res.target := target
    res.index := bhtIndex
    res.taggedHit := isBranch && direction.taggedHit
    res.tageUseAlternate := isBranch && direction.useAlternate
    res.indirectHit := isIndirect && indirect.valid
    res.itageHit := isIndirect && indirect.taggedHit
    res.isBranch := isBranch
    res.isCall := isCall
    res.isRet := isRet
    res
  }

  val p0 = predict(io.predict_pc, io.predict_inst, specGhr,
    specRas, specRasPtr, specRasCount)
  val historyAfterP0 = Mux(p0.isBranch,
    Cat(specGhr(bhtW - 2, 0), p0.taken), specGhr)
  val p1 = predict(io.predict_pc1, io.predict_inst1, historyAfterP0,
    specRas, specRasPtr, specRasCount)

  io.bp_valid := p0.valid
  io.bp_taken := p0.valid && p0.taken
  io.bp_target := p0.target
  io.bp_index := p0.index
  io.bp_tagged_hit := p0.taggedHit
  io.bp_tage_use_alt := p0.tageUseAlternate
  io.bp_indirect_hit := p0.indirectHit
  io.bp_itage_hit := p0.itageHit

  io.bp1_valid := p1.valid
  io.bp1_taken := p1.valid && p1.taken
  io.bp1_target := p1.target
  io.bp1_index := p1.index
  io.bp1_tagged_hit := p1.taggedHit
  io.bp1_tage_use_alt := p1.tageUseAlternate
  io.bp1_indirect_hit := p1.indirectHit
  io.bp1_itage_hit := p1.itageHit

  io.tage_alloc := false.B
  io.itage_alloc := false.B

  def rasStep(stack: Vec[UInt], ptr: UInt, count: UInt,
              pc: UInt, isCall: Bool, isRet: Bool): (Vec[UInt], UInt, UInt) = {
    val nextStack = Wire(Vec(RAS_SIZE, UInt(32.W)))
    val nextPtr = Wire(UInt(rasW.W))
    val nextCount = Wire(UInt(log2Ceil(RAS_SIZE + 1).W))
    nextStack := stack
    nextPtr := ptr
    nextCount := count
    when(isCall) {
      nextStack(ptr) := pc + 4.U
      nextPtr := Mux(ptr === (RAS_SIZE - 1).U, 0.U, ptr + 1.U)
      nextCount := Mux(count === RAS_SIZE.U, count, count + 1.U)
    }.elsewhen(isRet) {
      nextPtr := Mux(ptr === 0.U, (RAS_SIZE - 1).U, ptr - 1.U)
      nextCount := Mux(count === 0.U, 0.U, count - 1.U)
    }
    (nextStack, nextPtr, nextCount)
  }

  when(io.update_valid && io.update_is_branch) {
    val pcIndex = io.update_pc(bhtW + 1, 2)
    val updateHistory = io.update_index ^ pcIndex
    val updateDirection = directionPredict(io.update_pc, updateHistory, io.update_index, false.B)
    val baseValue = bht(io.update_index)
    val baseWasTrained = bht_valid(io.update_index)
    bht(io.update_index) := Mux(io.update_taken, satInc2(baseValue), satDec2(baseValue))
    bht_valid(io.update_index) := true.B

    val updateIndices = tageHistories.map(historyIndex(io.update_pc, updateHistory, _, tageW))
    val updateTags = tageHistories.map(historyTag(io.update_pc, updateHistory, _))
    for (i <- 0 until tageCount) {
      when(updateDirection.providerRank === (i + 1).U) {
        val ctr = tage_ctr(i)(updateIndices(i))
        tage_ctr(i)(updateIndices(i)) := Mux(io.update_taken, satInc3(ctr), satDec3(ctr))
        when(updateDirection.providerPred =/= updateDirection.alternatePred) {
          val useful = tage_useful(i)(updateIndices(i))
          tage_useful(i)(updateIndices(i)) :=
            Mux(updateDirection.providerPred === io.update_taken, satInc2(useful), satDec2(useful))
        }
      }
    }

    val needAllocate = !baseWasTrained || updateDirection.taken =/= io.update_taken
    val candidates = Wire(Vec(tageCount, Bool()))
    for (i <- 0 until tageCount) {
      val longerThanProvider = (i + 1).U > updateDirection.providerRank
      candidates(i) := longerThanProvider &&
        (!tage_valid(i)(updateIndices(i)) || tage_useful(i)(updateIndices(i)) === 0.U)
    }
    val allocOH = PriorityEncoderOH(candidates.asUInt)
    io.tage_alloc := needAllocate && candidates.asUInt.orR
    when(needAllocate) {
      when(candidates.asUInt.orR) {
        for (i <- 0 until tageCount) {
          when(allocOH(i)) {
            tage_valid(i)(updateIndices(i)) := true.B
            tage_tag(i)(updateIndices(i)) := updateTags(i)
            tage_ctr(i)(updateIndices(i)) := Mux(io.update_taken, 4.U, 3.U)
            tage_useful(i)(updateIndices(i)) := 0.U
          }
        }
      }.otherwise {
        for (i <- 0 until tageCount) {
          when((i + 1).U > updateDirection.providerRank) {
            tage_useful(i)(updateIndices(i)) := satDec2(tage_useful(i)(updateIndices(i)))
          }
        }
      }
    }

    commitGhr := Cat(commitGhr(bhtW - 2, 0), io.update_taken)
  }

  when(io.update_valid && io.update_is_jalr && !io.update_is_ret) {
    val baseIdx = ittIndex(io.update_pc)
    val baseTag = pcTag(io.update_pc)
    val baseTagHit = itt_valid(baseIdx) && itt_tag(baseIdx) === baseTag
    when(baseTagHit && itt_target(baseIdx) === io.update_target) {
      itt_conf(baseIdx) := satInc2(itt_conf(baseIdx))
    }.elsewhen(baseTagHit) {
      itt_target(baseIdx) := io.update_target
      itt_conf(baseIdx) := 0.U
    }.otherwise {
      itt_valid(baseIdx) := true.B
      itt_tag(baseIdx) := baseTag
      itt_target(baseIdx) := io.update_target
      itt_conf(baseIdx) := 1.U
    }

    val pcIndex = io.update_pc(bhtW + 1, 2)
    val updateHistory = io.update_index ^ pcIndex
    val updateIndirect = indirectPredict(io.update_pc, updateHistory)
    val updateIndices = itageHistories.map(historyIndex(io.update_pc, updateHistory, _, itageW))
    val updateTags = itageHistories.map(historyTag(io.update_pc, updateHistory, _))
    for (i <- 0 until itageCount) {
      when(updateIndirect.providerRank === (i + 1).U) {
        when(itage_target(i)(updateIndices(i)) === io.update_target) {
          itage_conf(i)(updateIndices(i)) := satInc2(itage_conf(i)(updateIndices(i)))
        }.otherwise {
          itage_target(i)(updateIndices(i)) := io.update_target
          itage_conf(i)(updateIndices(i)) := 0.U
        }
      }
    }

    val needAllocate = !updateIndirect.valid || updateIndirect.target =/= io.update_target
    val candidates = Wire(Vec(itageCount, Bool()))
    for (i <- 0 until itageCount) {
      candidates(i) := (i + 1).U > updateIndirect.providerRank &&
        (!itage_valid(i)(updateIndices(i)) || itage_conf(i)(updateIndices(i)) === 0.U)
    }
    val allocOH = PriorityEncoderOH(candidates.asUInt)
    io.itage_alloc := needAllocate && candidates.asUInt.orR
    when(needAllocate && candidates.asUInt.orR) {
      for (i <- 0 until itageCount) {
        when(allocOH(i)) {
          itage_valid(i)(updateIndices(i)) := true.B
          itage_tag(i)(updateIndices(i)) := updateTags(i)
          itage_target(i)(updateIndices(i)) := io.update_target
          itage_conf(i)(updateIndices(i)) := 1.U
        }
      }
    }
  }

  val commitCall = io.update_valid && io.update_is_call
  val commitRet = io.update_valid && io.update_is_ret
  val (commitRasAfter, commitRasPtrAfter, commitRasCountAfter) =
    rasStep(commitRas, commitRasPtr, commitRasCount,
      io.update_pc, commitCall, commitRet)
  when(commitCall || commitRet) {
    commitRas := commitRasAfter
    commitRasPtr := commitRasPtrAfter
    commitRasCount := commitRasCountAfter
  }

  val commitGhrAfter = Mux(io.update_valid && io.update_is_branch,
    Cat(commitGhr(bhtW - 2, 0), io.update_taken), commitGhr)
  val specHistory0 = Mux(io.spec_advance_mask(0) && p0.isBranch,
    Cat(specGhr(bhtW - 2, 0), p0.taken), specGhr)
  val specHistory1 = Mux(io.spec_advance_mask(1) && p1.isBranch,
    Cat(specHistory0(bhtW - 2, 0), p1.taken), specHistory0)
  val (specRasAfter0, specRasPtrAfter0, specRasCountAfter0) =
    rasStep(specRas, specRasPtr, specRasCount, io.predict_pc,
      io.spec_advance_mask(0) && p0.isCall,
      io.spec_advance_mask(0) && p0.isRet)
  val (specRasAfter1, specRasPtrAfter1, specRasCountAfter1) =
    rasStep(specRasAfter0, specRasPtrAfter0, specRasCountAfter0, io.predict_pc1,
      io.spec_advance_mask(1) && p1.isCall,
      io.spec_advance_mask(1) && p1.isRet)

  val recoverHistory = io.recover_index ^ io.recover_pc(bhtW + 1, 2)
  val recoverGhr = Mux(io.recover_is_branch,
    Cat(recoverHistory(bhtW - 2, 0), io.recover_taken), recoverHistory)
  val (recoverRasAfter, recoverRasPtrAfter, recoverRasCountAfter) =
    rasStep(io.recover_ras, io.recover_ras_ptr, io.recover_ras_count,
      io.recover_pc, io.recover_is_call, io.recover_is_ret)

  when(io.recover_valid) {
    specGhr := recoverGhr
    specRas := recoverRasAfter
    specRasPtr := recoverRasPtrAfter
    specRasCount := recoverRasCountAfter
  }.elsewhen(io.reset_spec) {
    specGhr := commitGhrAfter
    specRas := commitRasAfter
    specRasPtr := commitRasPtrAfter
    specRasCount := commitRasCountAfter
  }.elsewhen(io.spec_advance_valid) {
    specGhr := specHistory1
    specRas := specRasAfter1
    specRasPtr := specRasPtrAfter1
    specRasCount := specRasCountAfter1
  }
}
