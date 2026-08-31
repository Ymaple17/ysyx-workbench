package unit

import chisel3._
import chisel3.util._
import common.IMM_TYPE._
import common.BPU_Config._
import common.OoOParams
import core.CoreConfig

class BPU_IO(bhtSize: Int = BHT_SIZE, indirectSize: Int = INDIRECT_TARGET_SIZE) extends Bundle {
  val predict_pc = Input(UInt(32.W))
  val predict_inst = Input(UInt(32.W))
  val predict_pc1 = Input(UInt(32.W))
  val predict_inst1 = Input(UInt(32.W))
  val predict_pc2 = Input(UInt(32.W))
  val predict_inst2 = Input(UInt(32.W))
  val predict_pc3 = Input(UInt(32.W))
  val predict_inst3 = Input(UInt(32.W))

  val bp_valid = Output(Bool())
  val bp_taken = Output(Bool())
  val bp_target = Output(UInt(32.W))
  val bp_index = Output(UInt(BP_META_WIDTH.W))
  val bp_tagged_hit = Output(Bool())
  val bp_tage_use_alt = Output(Bool())
  val bp_bimodal_selected = Output(Bool())
  val bp_indirect_hit = Output(Bool())
  val bp_itage_hit = Output(Bool())
  val bp_loop_hit = Output(Bool())
  val bp_local_selected = Output(Bool())
  val bp_sc_selected = Output(Bool())

  val bp1_valid = Output(Bool())
  val bp1_taken = Output(Bool())
  val bp1_target = Output(UInt(32.W))
  val bp1_index = Output(UInt(BP_META_WIDTH.W))
  val bp1_tagged_hit = Output(Bool())
  val bp1_tage_use_alt = Output(Bool())
  val bp1_bimodal_selected = Output(Bool())
  val bp1_indirect_hit = Output(Bool())
  val bp1_itage_hit = Output(Bool())
  val bp1_loop_hit = Output(Bool())
  val bp1_local_selected = Output(Bool())
  val bp1_sc_selected = Output(Bool())

  val bp2_valid = Output(Bool())
  val bp2_taken = Output(Bool())
  val bp2_target = Output(UInt(32.W))
  val bp2_index = Output(UInt(BP_META_WIDTH.W))
  val bp2_tagged_hit = Output(Bool())
  val bp2_tage_use_alt = Output(Bool())
  val bp2_bimodal_selected = Output(Bool())
  val bp2_indirect_hit = Output(Bool())
  val bp2_itage_hit = Output(Bool())
  val bp2_loop_hit = Output(Bool())
  val bp2_local_selected = Output(Bool())
  val bp2_sc_selected = Output(Bool())

  val bp3_valid = Output(Bool())
  val bp3_taken = Output(Bool())
  val bp3_target = Output(UInt(32.W))
  val bp3_index = Output(UInt(BP_META_WIDTH.W))
  val bp3_tagged_hit = Output(Bool())
  val bp3_tage_use_alt = Output(Bool())
  val bp3_bimodal_selected = Output(Bool())
  val bp3_indirect_hit = Output(Bool())
  val bp3_itage_hit = Output(Bool())
  val bp3_loop_hit = Output(Bool())
  val bp3_local_selected = Output(Bool())
  val bp3_sc_selected = Output(Bool())

  val update_pc = Input(UInt(32.W))
  val update_target = Input(UInt(32.W))
  val update_valid = Input(Bool())
  val update_taken = Input(Bool())
  val update_is_branch = Input(Bool())
  val update_is_jalr = Input(Bool())
  val update_index = Input(UInt(BP_META_WIDTH.W))

  val update_is_call = Input(Bool())
  val update_is_ret = Input(Bool())

  val spec_advance_valid = Input(Bool())
  val spec_advance_mask = Input(UInt(OoOParams.CORE_WIDTH.W))
  val spec_override_valid = Input(Bool())
  val spec_override_taken = Input(UInt(OoOParams.CORE_WIDTH.W))
  val spec_override_target = Input(Vec(OoOParams.CORE_WIDTH, UInt(32.W)))
  val recover_valid = Input(Bool())
  val recover_pc = Input(UInt(32.W))
  val recover_index = Input(UInt(BP_META_WIDTH.W))
  val recover_target = Input(UInt(32.W))
  val recover_taken = Input(Bool())
  val recover_is_branch = Input(Bool())
  val recover_is_jalr = Input(Bool())
  val recover_is_call = Input(Bool())
  val recover_is_ret = Input(Bool())
  val recover_ras = Input(Vec(RAS_SIZE, UInt(32.W)))
  val recover_ras_ptr = Input(UInt(log2Ceil(RAS_SIZE).W))
  val recover_ras_count = Input(UInt(log2Ceil(RAS_SIZE + 1).W))
  val reset_spec = Input(Bool())

  val spec_ghr = Output(UInt(GHR_LENGTH.W))
  val spec_path_history = Output(UInt(PATH_HISTORY_LENGTH.W))
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
  private val loopW = log2Ceil(LOOP_TABLE_SIZE)
  private val localHistoryW = log2Ceil(LOCAL_HISTORY_TABLE_SIZE)
  private val localPhtW = log2Ceil(LOCAL_PHT_SIZE)
  private val scW = log2Ceil(SC_TABLE_SIZE)
  private val tageHistories = TAGE_HISTORY_LENGTHS.map(math.min(_, GHR_LENGTH)).distinct
  private val itageHistoryWidth = PATH_HISTORY_LENGTH
  private val itageHistories = ITAGE_HISTORY_LENGTHS.map(math.min(_, itageHistoryWidth)).distinct
  private val tageCount = tageHistories.length
  private val itageCount = itageHistories.length
  private val tageProviderW = log2Ceil(tageCount + 1)
  private val itageProviderW = log2Ceil(itageCount + 1)

  val bht = RegInit(VecInit(Seq.fill(bhtSize)(BHT_INIT.U(2.W))))
  val bht_valid = RegInit(VecInit(Seq.fill(bhtSize)(false.B)))
  val commitGhr = RegInit(0.U(GHR_LENGTH.W))
  val specGhr = RegInit(0.U(GHR_LENGTH.W))
  val commitPathHistory = RegInit(0.U(PATH_HISTORY_LENGTH.W))
  val specPathHistory = RegInit(0.U(PATH_HISTORY_LENGTH.W))

  val tage_valid = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(false.B))))
  val tage_tag = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(0.U(TAGGED_BHT_TAG_BITS.W)))))
  val tage_ctr = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(3.U(3.W)))))
  val tage_useful = Seq.fill(tageCount)(RegInit(VecInit(Seq.fill(tageTableSize)(0.U(2.W)))))

  // PC-only bimodal complements the history-indexed TAGE/gshare path.  The
  // chooser learns per branch, so stable loop controls are not forced through
  // global-history aliasing while correlated branches still use TAGE.
  val bimodal = RegInit(VecInit(Seq.fill(bhtSize)(BHT_INIT.U(2.W))))
  val bimodalValid = RegInit(VecInit(Seq.fill(bhtSize)(false.B)))
  val tournamentChooser = RegInit(VecInit(Seq.fill(bhtSize)(1.U(2.W))))

  val localHistoryTable = RegInit(VecInit(Seq.fill(LOCAL_HISTORY_TABLE_SIZE)(0.U(LOCAL_HISTORY_BITS.W))))
  val localPht = RegInit(VecInit(Seq.fill(LOCAL_PHT_SIZE)(3.U(3.W))))
  val localPhtValid = RegInit(VecInit(Seq.fill(LOCAL_PHT_SIZE)(false.B)))
  val localChooser = RegInit(VecInit(Seq.fill(bhtSize)(1.U(2.W))))

  // Compact statistical corrector. The base direction predictors retain
  // ownership; this provider may override only after a per-PC chooser has
  // observed that its signed history sum beats the base prediction.
  val scCounters = Seq.fill(SC_HISTORY_LENGTHS.length)(
    RegInit(VecInit(Seq.fill(SC_TABLE_SIZE)(0.S(SC_COUNTER_BITS.W)))))
  val scChooser = RegInit(VecInit(Seq.fill(SC_TABLE_SIZE)(0.U(2.W))))

  val loop_valid = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(false.B)))
  val loop_tag = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(0.U(LOOP_TAG_BITS.W))))
  val loop_trip = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(0.U(LOOP_ITER_BITS.W))))
  val loop_conf = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(0.U(2.W))))
  val loop_commit_iter = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(0.U(LOOP_ITER_BITS.W))))
  val loop_spec_iter = RegInit(VecInit(Seq.fill(LOOP_TABLE_SIZE)(0.U(LOOP_ITER_BITS.W))))

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
  io.spec_path_history := specPathHistory
  io.spec_ras := specRas
  io.spec_ras_ptr := specRasPtr
  io.spec_ras_count := specRasCount

  def satInc2(v: UInt): UInt = Mux(v === 3.U, 3.U, v + 1.U)
  def satDec2(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)
  def satInc3(v: UInt): UInt = Mux(v === 7.U, 7.U, v + 1.U)
  def satDec3(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)

  private val pathLo = GHR_LENGTH
  private val loopIterLo = pathLo + PATH_HISTORY_LENGTH
  private val loopHitBit = loopIterLo + LOOP_ITER_BITS
  private val tageProviderLo = loopHitBit + 1
  private val itageProviderLo = tageProviderLo + TAGE_PROVIDER_BITS
  private val localHistoryLo = itageProviderLo + ITAGE_PROVIDER_BITS
  private val localPredBit = localHistoryLo + LOCAL_HISTORY_BITS
  private val primaryPredBit = localPredBit + 1
  private val basePredBit = primaryPredBit + 1
  private val scPredBit = basePredBit + 1

  def packMetadata(ghr: UInt, pathHistory: UInt, loopHit: Bool, loopIter: UInt,
                   tageProvider: UInt, itageProvider: UInt, localHistory: UInt,
                   localPred: Bool, primaryPred: Bool, basePred: Bool,
                   scPred: Bool): UInt =
    Cat(scPred, basePred, primaryPred, localPred, localHistory, itageProvider, tageProvider,
      loopHit, loopIter, pathHistory, ghr)

  def metadataGhr(meta: UInt): UInt = meta(GHR_LENGTH - 1, 0)
  def metadataPath(meta: UInt): UInt = meta(loopIterLo - 1, pathLo)
  def metadataLoopIter(meta: UInt): UInt = meta(loopHitBit - 1, loopIterLo)
  def metadataLoopHit(meta: UInt): Bool = meta(loopHitBit)
  def metadataTageProvider(meta: UInt): UInt =
    meta(itageProviderLo - 1, tageProviderLo)
  def metadataItageProvider(meta: UInt): UInt =
    meta(localHistoryLo - 1, itageProviderLo)
  def metadataLocalHistory(meta: UInt): UInt =
    meta(localPredBit - 1, localHistoryLo)
  def metadataLocalPred(meta: UInt): Bool = meta(localPredBit)
  def metadataPrimaryPred(meta: UInt): Bool = meta(primaryPredBit)
  def metadataBasePred(meta: UInt): Bool = meta(basePredBit)
  def metadataScPred(meta: UInt): Bool = meta(scPredBit)

  def pathStep(history: UInt, target: UInt): UInt = {
    val targetHash = target(17, 2) ^ target(31, 16)
    Cat(history(PATH_HISTORY_LENGTH - 9, 0), targetHash(7, 0))
  }

  def loopIndex(pc: UInt): UInt = pc(loopW + 1, 2)
  def loopTag(pc: UInt): UInt = pc(LOOP_TAG_BITS + loopW + 1, loopW + 2)
  def satIncLoop(v: UInt): UInt = Mux(v.andR, v, v + 1.U)

  def localHistoryIndex(pc: UInt): UInt = pc(localHistoryW + 1, 2)

  def localPhtIndex(pc: UInt, localHistory: UInt): UInt =
    pc(localPhtW + 1, 2) ^ foldHistory(localHistory, LOCAL_HISTORY_BITS, localPhtW)

  def foldHistory(history: UInt, historyLength: Int, width: Int): UInt = {
    VecInit((0 until width).map { bit =>
      val taps = (bit until historyLength by width).map(history(_))
      if (taps.isEmpty) false.B else taps.reduce(_ ^ _)
    }).asUInt
  }

  def historyIndex(pc: UInt, history: UInt, historyLength: Int, width: Int): UInt =
    pc(width + 1, 2) ^ foldHistory(history, historyLength, width)

  def historyTag(pc: UInt, history: UInt, historyLength: Int): UInt = {
    val historyWidth = history.getWidth
    val pcFold = pc(TAGGED_BHT_TAG_BITS + 1, 2) ^
      pc(2 * TAGGED_BHT_TAG_BITS + 1, TAGGED_BHT_TAG_BITS + 2)
    pcFold ^ foldHistory(history, historyLength, TAGGED_BHT_TAG_BITS) ^
      foldHistory(Cat(history(historyWidth - 2, 0), history(historyWidth - 1)),
        historyLength, TAGGED_BHT_TAG_BITS)
  }

  def scIndex(pc: UInt, history: UInt, historyLength: Int): UInt = {
    val pcIndex = pc(scW + 1, 2)
    if (historyLength == 0) pcIndex
    else pcIndex ^ foldHistory(history, historyLength, scW)
  }

  class StatisticalResult extends Bundle {
    val taken = Bool()
    val strong = Bool()
  }

  def statisticalPredict(pc: UInt, history: UInt): StatisticalResult = {
    val result = Wire(new StatisticalResult)
    val terms = SC_HISTORY_LENGTHS.zipWithIndex.map { case (length, table) =>
      scCounters(table)(scIndex(pc, history, length))
    }
    val sum = terms.reduce(_ +& _)
    result.taken := sum >= 0.S
    result.strong := sum >= SC_THRESHOLD.S || sum <= (-SC_THRESHOLD).S
    result
  }

  def ittIndex(pc: UInt): UInt = pc(ittW + 1, 2)
  def pcTag(pc: UInt): UInt = pc(TAGGED_BHT_TAG_BITS + ittW + 1, ittW + 2)

  class DirectionResult extends Bundle {
    val taken = Bool()
    val tageTaken = Bool()
    val bimodalPred = Bool()
    val useBimodal = Bool()
    val taggedHit = Bool()
    val useAlternate = Bool()
    val providerRank = UInt(tageProviderW.W)
    val providerPred = Bool()
    val alternatePred = Bool()
    val loopHit = Bool()
    val loopIter = UInt(LOOP_ITER_BITS.W)
    val localHistory = UInt(LOCAL_HISTORY_BITS.W)
    val localPred = Bool()
    val primaryPred = Bool()
    val localSelected = Bool()
    val basePred = Bool()
    val scPred = Bool()
    val scSelected = Bool()
  }

  def directionPredict(pc: UInt, history: UInt, baseIndex: UInt,
                       coldStaticTaken: Bool, speculativeLoopIter: UInt): DirectionResult = {
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
    val tageTaken = Mux(useAlternate, alternatePred, providerPred)
    val bimodalIndex = pc(bhtW + 1, 2)
    val bimodalPred = Mux(bimodalValid(bimodalIndex), bimodal(bimodalIndex)(1), coldStaticTaken)
    val useBimodal = bimodalValid(bimodalIndex) && tournamentChooser(bimodalIndex)(1)
    val lpIdx = loopIndex(pc)
    val loopHit = coldStaticTaken && loop_valid(lpIdx) &&
      loop_tag(lpIdx) === loopTag(pc) && loop_trip(lpIdx) =/= 0.U &&
      loop_conf(lpIdx) >= LOOP_CONFIDENCE_THRESHOLD.U
    val loopTaken = speculativeLoopIter < loop_trip(lpIdx)
    val primaryPred = Mux(loopHit, loopTaken, Mux(useBimodal, bimodalPred, tageTaken))
    val localHistory = localHistoryTable(localHistoryIndex(pc))
    val localIndex = localPhtIndex(pc, localHistory)
    val localPred = Mux(localPhtValid(localIndex), localPht(localIndex)(2), coldStaticTaken)
    val localSelected = !loopHit && localPhtValid(localIndex) && localChooser(bimodalIndex)(1)
    val basePred = Mux(localSelected, localPred, primaryPred)
    val statistical = statisticalPredict(pc, history)
    val scSelected = statistical.strong && scChooser(pc(scW + 1, 2))(1)
    res.taken := Mux(scSelected, statistical.taken, basePred)
    res.tageTaken := tageTaken
    res.bimodalPred := bimodalPred
    res.useBimodal := useBimodal
    res.taggedHit := providerRank =/= 0.U
    res.useAlternate := useAlternate
    res.providerRank := providerRank
    res.providerPred := providerPred
    res.alternatePred := alternatePred
    res.loopHit := loopHit
    res.loopIter := speculativeLoopIter
    res.localHistory := localHistory
    res.localPred := localPred
    res.primaryPred := primaryPred
    res.localSelected := localSelected
    res.basePred := basePred
    res.scPred := statistical.taken
    res.scSelected := scSelected
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

    val taggedReady = providerRank =/= 0.U && providerConf =/= 0.U
    // For an indirect jump, declining to predict already guarantees a
    // redirect at execute. A tagged PC match with a low-confidence latest
    // target is therefore still useful: a miss has the same recovery class,
    // while a hit avoids the redirect entirely.
    val baseReady = baseHit
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
    val index = UInt(BP_META_WIDTH.W)
    val taggedHit = Bool()
    val tageUseAlternate = Bool()
    val bimodalSelected = Bool()
    val indirectHit = Bool()
    val itageHit = Bool()
    val isBranch = Bool()
    val isCall = Bool()
    val isRet = Bool()
    val isIndirect = Bool()
    val loopHit = Bool()
    val loopIndex = UInt(loopW.W)
    val loopIter = UInt(LOOP_ITER_BITS.W)
    val localSelected = Bool()
    val scSelected = Bool()
  }

  def predict(pc: UInt, inst: UInt, history: UInt, pathHistory: UInt,
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
    val bhtIndex = pc(bhtW + 1, 2) ^ history(bhtW - 1, 0)
    val lpIdx = loopIndex(pc)
    val direction = directionPredict(pc, history, bhtIndex,
      imm.io.imm_ext(31), loop_spec_iter(lpIdx))
    val indirect = indirectPredict(pc, pathHistory)
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
    res.index := packMetadata(history, pathHistory, direction.loopHit,
      direction.loopIter, direction.providerRank, indirect.providerRank,
      direction.localHistory, direction.localPred, direction.primaryPred,
      direction.basePred, direction.scPred)
    res.taggedHit := isBranch && direction.taggedHit
    res.tageUseAlternate := isBranch && direction.useAlternate
    res.bimodalSelected := isBranch && direction.useBimodal
    res.indirectHit := isIndirect && indirect.valid
    res.itageHit := isIndirect && indirect.taggedHit
    res.isBranch := isBranch
    res.isCall := isCall
    res.isRet := isRet
    res.isIndirect := isIndirect
    res.loopHit := isBranch && direction.loopHit
    res.loopIndex := lpIdx
    res.loopIter := direction.loopIter
    res.localSelected := isBranch && direction.localSelected
    res.scSelected := isBranch && direction.scSelected
    res
  }

  // The last two fetch slots use medium-cost direction providers. Replicating
  // TAGE, local PHT, and ITAGE reads four times creates a very large
  // combinational cone, so lane2/3 add only the compact loop provider to the
  // PC-only direction/target tables while retaining exact training metadata.
  def predictLite(pc: UInt, inst: UInt, history: UInt, pathHistory: UInt,
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
    val bimodalIndex = pc(bhtW + 1, 2)
    val bimodalTaken = Mux(bimodalValid(bimodalIndex),
      bimodal(bimodalIndex)(1), imm.io.imm_ext(31))
    val lpIdx = loopIndex(pc)
    val loopIter = loop_spec_iter(lpIdx)
    val loopHit = imm.io.imm_ext(31) && loop_valid(lpIdx) &&
      loop_tag(lpIdx) === loopTag(pc) && loop_trip(lpIdx) =/= 0.U &&
      loop_conf(lpIdx) >= LOOP_CONFIDENCE_THRESHOLD.U
    val loopTaken = loopIter < loop_trip(lpIdx)
    val branchBaseTaken = Mux(loopHit, loopTaken, bimodalTaken)
    val statistical = statisticalPredict(pc, history)
    val scSelected = statistical.strong && scChooser(pc(scW + 1, 2))(1)
    val branchTaken = Mux(scSelected, statistical.taken, branchBaseTaken)
    val baseIdx = ittIndex(pc)
    val baseIndirectHit = itt_valid(baseIdx) && itt_tag(baseIdx) === pcTag(pc)
    val rasEmpty = rasCount === 0.U
    val rasTopIdx = Mux(rasPtr === 0.U, (RAS_SIZE - 1).U(rasW.W),
      (rasPtr - 1.U)(rasW - 1, 0))
    val rasTarget = Mux1H((0 until RAS_SIZE).map(i => (rasTopIdx === i.U) -> rasStack(i)))
    val predValid = isJal || isBranch || isRet || (isIndirect && baseIndirectHit)
    val predTaken = predValid && MuxCase(false.B, Seq(
      isJal -> true.B,
      isBranch -> branchTaken,
      isRet -> !rasEmpty,
      isIndirect -> baseIndirectHit))

    res.valid := predValid
    res.taken := predTaken
    res.target := MuxCase(pc + imm.io.imm_ext, Seq(
      isRet -> rasTarget,
      isIndirect -> itt_target(baseIdx)))
    res.index := packMetadata(history, pathHistory, loopHit, loopIter,
      0.U(TAGE_PROVIDER_BITS.W), 0.U(ITAGE_PROVIDER_BITS.W),
      localHistoryTable(localHistoryIndex(pc)), branchBaseTaken,
      branchBaseTaken, branchBaseTaken, statistical.taken)
    res.taggedHit := false.B
    res.tageUseAlternate := false.B
    res.bimodalSelected := isBranch && !loopHit
    res.indirectHit := isIndirect && baseIndirectHit
    res.itageHit := false.B
    res.isBranch := isBranch
    res.isCall := isCall
    res.isRet := isRet
    res.isIndirect := isIndirect
    res.loopHit := isBranch && loopHit
    res.loopIndex := lpIdx
    res.loopIter := loopIter
    res.localSelected := false.B
    res.scSelected := isBranch && scSelected
    res
  }

  val p0 = predict(io.predict_pc, io.predict_inst, specGhr, specPathHistory,
    specRas, specRasPtr, specRasCount)
  val packetTaken0 = Mux(io.spec_override_valid, io.spec_override_taken(0), p0.taken)
  val packetTarget0 = Mux(io.spec_override_valid, io.spec_override_target(0), p0.target)
  val historyAfterP0 = Mux(p0.isBranch,
    Cat(specGhr(GHR_LENGTH - 2, 0), packetTaken0), specGhr)
  val pathAfterP0 = Mux(p0.isIndirect && packetTaken0,
    pathStep(specPathHistory, packetTarget0), specPathHistory)
  val p1 = predict(io.predict_pc1, io.predict_inst1, historyAfterP0, pathAfterP0,
    specRas, specRasPtr, specRasCount)
  val packetTaken1 = Mux(io.spec_override_valid, io.spec_override_taken(1), p1.taken)
  val packetTarget1 = Mux(io.spec_override_valid, io.spec_override_target(1), p1.target)
  val historyAfterP1 = Mux(p1.isBranch,
    Cat(historyAfterP0(GHR_LENGTH - 2, 0), packetTaken1), historyAfterP0)
  val pathAfterP1 = Mux(p1.isIndirect && packetTaken1,
    pathStep(pathAfterP0, packetTarget1), pathAfterP0)
  val p2 = predictLite(io.predict_pc2, io.predict_inst2, historyAfterP1, pathAfterP1,
    specRas, specRasPtr, specRasCount)
  val packetTaken2 = Mux(io.spec_override_valid, io.spec_override_taken(2), p2.taken)
  val packetTarget2 = Mux(io.spec_override_valid, io.spec_override_target(2), p2.target)
  val historyAfterP2 = Mux(p2.isBranch,
    Cat(historyAfterP1(GHR_LENGTH - 2, 0), packetTaken2), historyAfterP1)
  val pathAfterP2 = Mux(p2.isIndirect && packetTaken2,
    pathStep(pathAfterP1, packetTarget2), pathAfterP1)
  val p3 = predictLite(io.predict_pc3, io.predict_inst3, historyAfterP2, pathAfterP2,
    specRas, specRasPtr, specRasCount)
  val packetTaken3 = Mux(io.spec_override_valid, io.spec_override_taken(3), p3.taken)
  val packetTarget3 = Mux(io.spec_override_valid, io.spec_override_target(3), p3.target)

  io.bp_valid := p0.valid
  io.bp_taken := p0.valid && p0.taken
  io.bp_target := p0.target
  io.bp_index := p0.index
  io.bp_tagged_hit := p0.taggedHit
  io.bp_tage_use_alt := p0.tageUseAlternate
  io.bp_bimodal_selected := p0.bimodalSelected
  io.bp_indirect_hit := p0.indirectHit
  io.bp_itage_hit := p0.itageHit
  io.bp_loop_hit := p0.loopHit
  io.bp_local_selected := p0.localSelected
  io.bp_sc_selected := p0.scSelected

  io.bp1_valid := p1.valid
  io.bp1_taken := p1.valid && p1.taken
  io.bp1_target := p1.target
  io.bp1_index := p1.index
  io.bp1_tagged_hit := p1.taggedHit
  io.bp1_tage_use_alt := p1.tageUseAlternate
  io.bp1_bimodal_selected := p1.bimodalSelected
  io.bp1_indirect_hit := p1.indirectHit
  io.bp1_itage_hit := p1.itageHit
  io.bp1_loop_hit := p1.loopHit
  io.bp1_local_selected := p1.localSelected
  io.bp1_sc_selected := p1.scSelected

  io.bp2_valid := p2.valid
  io.bp2_taken := p2.valid && p2.taken
  io.bp2_target := p2.target
  io.bp2_index := p2.index
  io.bp2_tagged_hit := p2.taggedHit
  io.bp2_tage_use_alt := p2.tageUseAlternate
  io.bp2_bimodal_selected := p2.bimodalSelected
  io.bp2_indirect_hit := p2.indirectHit
  io.bp2_itage_hit := p2.itageHit
  io.bp2_loop_hit := p2.loopHit
  io.bp2_local_selected := p2.localSelected
  io.bp2_sc_selected := p2.scSelected

  io.bp3_valid := p3.valid
  io.bp3_taken := p3.valid && p3.taken
  io.bp3_target := p3.target
  io.bp3_index := p3.index
  io.bp3_tagged_hit := p3.taggedHit
  io.bp3_tage_use_alt := p3.tageUseAlternate
  io.bp3_bimodal_selected := p3.bimodalSelected
  io.bp3_indirect_hit := p3.indirectHit
  io.bp3_itage_hit := p3.itageHit
  io.bp3_loop_hit := p3.loopHit
  io.bp3_local_selected := p3.localSelected
  io.bp3_sc_selected := p3.scSelected

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
    val updateHistory = metadataGhr(io.update_index)
    val updateBaseIndex = pcIndex ^ updateHistory(bhtW - 1, 0)
    val providerAtFetch = metadataTageProvider(io.update_index)
    val baseValue = bht(updateBaseIndex)
    val baseWasTrained = bht_valid(updateBaseIndex)
    val bimodalWasTrained = bimodalValid(pcIndex)
    val bimodalPredAtFetch = bimodal(pcIndex)(1)
    val fetchPrimaryPred = metadataPrimaryPred(io.update_index)
    val fetchBasePred = metadataBasePred(io.update_index)
    val fetchScPred = metadataScPred(io.update_index)
    bht(updateBaseIndex) := Mux(io.update_taken, satInc2(baseValue), satDec2(baseValue))
    bht_valid(updateBaseIndex) := true.B
    bimodal(pcIndex) := Mux(io.update_taken, satInc2(bimodal(pcIndex)), satDec2(bimodal(pcIndex)))
    bimodalValid(pcIndex) := true.B
    when(bimodalWasTrained && bimodalPredAtFetch =/= fetchPrimaryPred) {
      when(bimodalPredAtFetch === io.update_taken) {
        tournamentChooser(pcIndex) := satInc2(tournamentChooser(pcIndex))
      }.elsewhen(fetchPrimaryPred === io.update_taken) {
        tournamentChooser(pcIndex) := satDec2(tournamentChooser(pcIndex))
      }
    }

    val fetchLocalHistory = metadataLocalHistory(io.update_index)
    val fetchLocalPred = metadataLocalPred(io.update_index)
    val updateLocalIndex = localPhtIndex(io.update_pc, fetchLocalHistory)
    val localValue = localPht(updateLocalIndex)
    localPht(updateLocalIndex) := Mux(io.update_taken, satInc3(localValue), satDec3(localValue))
    localPhtValid(updateLocalIndex) := true.B
    when(fetchLocalPred =/= fetchPrimaryPred) {
      when(fetchLocalPred === io.update_taken) {
        localChooser(pcIndex) := satInc2(localChooser(pcIndex))
      }.elsewhen(fetchPrimaryPred === io.update_taken) {
        localChooser(pcIndex) := satDec2(localChooser(pcIndex))
      }
    }

    val scChooserIdx = io.update_pc(scW + 1, 2)
    when(fetchScPred =/= fetchBasePred) {
      when(fetchScPred === io.update_taken) {
        scChooser(scChooserIdx) := satInc2(scChooser(scChooserIdx))
      }.elsewhen(fetchBasePred === io.update_taken) {
        scChooser(scChooserIdx) := satDec2(scChooser(scChooserIdx))
      }
    }
    for ((length, table) <- SC_HISTORY_LENGTHS.zipWithIndex) {
      val idx = scIndex(io.update_pc, updateHistory, length)
      val counter = scCounters(table)(idx)
      val maxCounter = ((1 << (SC_COUNTER_BITS - 1)) - 1).S(SC_COUNTER_BITS.W)
      val minCounter = (-(1 << (SC_COUNTER_BITS - 1))).S(SC_COUNTER_BITS.W)
      when(io.update_taken && counter =/= maxCounter) {
        scCounters(table)(idx) := counter + 1.S
      }.elsewhen(!io.update_taken && counter =/= minCounter) {
        scCounters(table)(idx) := counter - 1.S
      }
    }

    val updateIndices = tageHistories.map(historyIndex(io.update_pc, updateHistory, _, tageW))
    val updateTags = tageHistories.map(historyTag(io.update_pc, updateHistory, _))
    for (i <- 0 until tageCount) {
      when(providerAtFetch === (i + 1).U && tage_valid(i)(updateIndices(i)) &&
           tage_tag(i)(updateIndices(i)) === updateTags(i)) {
        val ctr = tage_ctr(i)(updateIndices(i))
        tage_ctr(i)(updateIndices(i)) := Mux(io.update_taken, satInc3(ctr), satDec3(ctr))
        val useful = tage_useful(i)(updateIndices(i))
        tage_useful(i)(updateIndices(i)) :=
          Mux(ctr(2) === io.update_taken, satInc2(useful), satDec2(useful))
      }
    }

    val needAllocate = !baseWasTrained || fetchPrimaryPred =/= io.update_taken
    val candidates = Wire(Vec(tageCount, Bool()))
    for (i <- 0 until tageCount) {
      val longerThanProvider = (i + 1).U > providerAtFetch
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
          when((i + 1).U > providerAtFetch) {
            tage_useful(i)(updateIndices(i)) := satDec2(tage_useful(i)(updateIndices(i)))
          }
        }
      }
    }

  }

  when(io.update_valid && io.update_is_branch) {
    localHistoryTable(localHistoryIndex(io.update_pc)) :=
      Cat(localHistoryTable(localHistoryIndex(io.update_pc))(LOCAL_HISTORY_BITS - 2, 0), io.update_taken)

    val lpIdx = loopIndex(io.update_pc)
    val lpTagHit = loop_valid(lpIdx) && loop_tag(lpIdx) === loopTag(io.update_pc)
    when(lpTagHit) {
      when(io.update_taken) {
        loop_commit_iter(lpIdx) := satIncLoop(loop_commit_iter(lpIdx))
      }.otherwise {
        val observedTrip = loop_commit_iter(lpIdx)
        when(observedTrip =/= 0.U) {
          when(loop_trip(lpIdx) === observedTrip) {
            loop_conf(lpIdx) := satInc2(loop_conf(lpIdx))
          }.otherwise {
            loop_trip(lpIdx) := observedTrip
            loop_conf(lpIdx) := 0.U
          }
        }
        loop_commit_iter(lpIdx) := 0.U
      }
    }.elsewhen(io.update_taken && io.update_target < io.update_pc) {
      loop_valid(lpIdx) := true.B
      loop_tag(lpIdx) := loopTag(io.update_pc)
      loop_trip(lpIdx) := 0.U
      loop_conf(lpIdx) := 0.U
      loop_commit_iter(lpIdx) := 1.U
      loop_spec_iter(lpIdx) := 1.U
    }

    commitGhr := Cat(commitGhr(GHR_LENGTH - 2, 0), io.update_taken)
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

    val updateHistory = metadataPath(io.update_index)
    val providerAtFetch = metadataItageProvider(io.update_index)
    val updateIndices = itageHistories.map(historyIndex(io.update_pc, updateHistory, _, itageW))
    val updateTags = itageHistories.map(historyTag(io.update_pc, updateHistory, _))
    val providerTargetMatch = Wire(Bool())
    providerTargetMatch := baseTagHit && itt_conf(baseIdx) =/= 0.U &&
      itt_target(baseIdx) === io.update_target
    for (i <- 0 until itageCount) {
      when(providerAtFetch === (i + 1).U) {
        providerTargetMatch := itage_valid(i)(updateIndices(i)) &&
          itage_tag(i)(updateIndices(i)) === updateTags(i) &&
          itage_conf(i)(updateIndices(i)) =/= 0.U &&
          itage_target(i)(updateIndices(i)) === io.update_target
      }
      when(providerAtFetch === (i + 1).U && itage_valid(i)(updateIndices(i)) &&
           itage_tag(i)(updateIndices(i)) === updateTags(i)) {
        when(itage_target(i)(updateIndices(i)) === io.update_target) {
          itage_conf(i)(updateIndices(i)) := satInc2(itage_conf(i)(updateIndices(i)))
        }.otherwise {
          itage_target(i)(updateIndices(i)) := io.update_target
          itage_conf(i)(updateIndices(i)) := 0.U
        }
      }
    }

    val needAllocate = !providerTargetMatch
    val candidates = Wire(Vec(itageCount, Bool()))
    for (i <- 0 until itageCount) {
      candidates(i) := (i + 1).U > providerAtFetch &&
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

  when(io.update_valid && io.update_is_jalr && !io.update_is_ret) {
    commitPathHistory := pathStep(commitPathHistory, io.update_target)
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
    Cat(commitGhr(GHR_LENGTH - 2, 0), io.update_taken), commitGhr)
  val commitPathAfter = Mux(io.update_valid && io.update_is_jalr && !io.update_is_ret,
    pathStep(commitPathHistory, io.update_target), commitPathHistory)
  val specTaken = VecInit(Seq(packetTaken0, packetTaken1, packetTaken2, packetTaken3))
  val specTarget = VecInit(Seq(packetTarget0, packetTarget1, packetTarget2, packetTarget3))
  val specHistory0 = Mux(io.spec_advance_mask(0) && p0.isBranch,
    Cat(specGhr(GHR_LENGTH - 2, 0), specTaken(0)), specGhr)
  val specHistory1 = Mux(io.spec_advance_mask(1) && p1.isBranch,
    Cat(specHistory0(GHR_LENGTH - 2, 0), specTaken(1)), specHistory0)
  val specHistory2 = Mux(io.spec_advance_mask(2) && p2.isBranch,
    Cat(specHistory1(GHR_LENGTH - 2, 0), specTaken(2)), specHistory1)
  val specHistory3 = Mux(io.spec_advance_mask(3) && p3.isBranch,
    Cat(specHistory2(GHR_LENGTH - 2, 0), specTaken(3)), specHistory2)
  val specPath0 = Mux(io.spec_advance_mask(0) && p0.isIndirect && specTaken(0),
    pathStep(specPathHistory, specTarget(0)), specPathHistory)
  val specPath1 = Mux(io.spec_advance_mask(1) && p1.isIndirect && specTaken(1),
    pathStep(specPath0, specTarget(1)), specPath0)
  val specPath2 = Mux(io.spec_advance_mask(2) && p2.isIndirect && specTaken(2),
    pathStep(specPath1, specTarget(2)), specPath1)
  val specPath3 = Mux(io.spec_advance_mask(3) && p3.isIndirect && specTaken(3),
    pathStep(specPath2, specTarget(3)), specPath2)
  val (specRasAfter0, specRasPtrAfter0, specRasCountAfter0) =
    rasStep(specRas, specRasPtr, specRasCount, io.predict_pc,
      io.spec_advance_mask(0) && p0.isCall,
      io.spec_advance_mask(0) && p0.isRet)
  val (specRasAfter1, specRasPtrAfter1, specRasCountAfter1) =
    rasStep(specRasAfter0, specRasPtrAfter0, specRasCountAfter0, io.predict_pc1,
      io.spec_advance_mask(1) && p1.isCall,
      io.spec_advance_mask(1) && p1.isRet)
  val (specRasAfter2, specRasPtrAfter2, specRasCountAfter2) =
    rasStep(specRasAfter1, specRasPtrAfter1, specRasCountAfter1, io.predict_pc2,
      io.spec_advance_mask(2) && p2.isCall,
      io.spec_advance_mask(2) && p2.isRet)
  val (specRasAfter3, specRasPtrAfter3, specRasCountAfter3) =
    rasStep(specRasAfter2, specRasPtrAfter2, specRasCountAfter2, io.predict_pc3,
      io.spec_advance_mask(3) && p3.isCall,
      io.spec_advance_mask(3) && p3.isRet)

  val recoverHistory = metadataGhr(io.recover_index)
  val recoverGhr = Mux(io.recover_is_branch,
    Cat(recoverHistory(GHR_LENGTH - 2, 0), io.recover_taken), recoverHistory)
  val recoverPathBase = metadataPath(io.recover_index)
  val recoverPath = Mux(io.recover_is_jalr && !io.recover_is_ret,
    pathStep(recoverPathBase, io.recover_target), recoverPathBase)
  val (recoverRasAfter, recoverRasPtrAfter, recoverRasCountAfter) =
    rasStep(io.recover_ras, io.recover_ras_ptr, io.recover_ras_count,
      io.recover_pc, io.recover_is_call, io.recover_is_ret)

  when(io.recover_valid) {
    specGhr := recoverGhr
    specPathHistory := recoverPath
    specRas := recoverRasAfter
    specRasPtr := recoverRasPtrAfter
    specRasCount := recoverRasCountAfter
  }.elsewhen(io.reset_spec) {
    specGhr := commitGhrAfter
    specPathHistory := commitPathAfter
    specRas := commitRasAfter
    specRasPtr := commitRasPtrAfter
    specRasCount := commitRasCountAfter
  }.elsewhen(io.spec_advance_valid) {
    specGhr := specHistory3
    specPathHistory := specPath3
    specRas := specRasAfter3
    specRasPtr := specRasPtrAfter3
    specRasCount := specRasCountAfter3
  }

  val recoverLpIdx = loopIndex(io.recover_pc)
  val recoverLpMispredictedExit = metadataLoopIter(io.recover_index) >=
    loop_trip(recoverLpIdx)
  val recoverLpNext = Mux(io.recover_taken,
    Mux(recoverLpMispredictedExit, 0.U,
      satIncLoop(metadataLoopIter(io.recover_index))), 0.U)
  for (i <- 0 until LOOP_TABLE_SIZE) {
    when(io.recover_valid) {
      loop_spec_iter(i) := loop_commit_iter(i)
      when(io.recover_is_branch && metadataLoopHit(io.recover_index) &&
           recoverLpIdx === i.U) {
        loop_spec_iter(i) := recoverLpNext
      }
    }.elsewhen(io.reset_spec) {
      loop_spec_iter(i) := loop_commit_iter(i)
    }.elsewhen(io.spec_advance_valid) {
      when(io.spec_advance_mask(0) && p0.loopHit && p0.loopIndex === i.U) {
        loop_spec_iter(i) := Mux(p0.taken, satIncLoop(p0.loopIter), 0.U)
      }
      when(io.spec_advance_mask(1) && p1.loopHit && p1.loopIndex === i.U) {
        loop_spec_iter(i) := Mux(p1.taken, satIncLoop(p1.loopIter), 0.U)
      }
      when(io.spec_advance_mask(2) && p2.loopHit && p2.loopIndex === i.U) {
        loop_spec_iter(i) := Mux(p2.taken, satIncLoop(p2.loopIter), 0.U)
      }
      when(io.spec_advance_mask(3) && p3.loopHit && p3.loopIndex === i.U) {
        loop_spec_iter(i) := Mux(p3.taken, satIncLoop(p3.loopIter), 0.U)
      }
    }
  }
}
