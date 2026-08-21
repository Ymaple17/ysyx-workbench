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
  val bp_indirect_hit = Output(Bool())

  val bp1_valid = Output(Bool())
  val bp1_taken = Output(Bool())
  val bp1_target = Output(UInt(32.W))
  val bp1_index = Output(UInt(log2Ceil(bhtSize).W))
  val bp1_tagged_hit = Output(Bool())
  val bp1_indirect_hit = Output(Bool())

  val update_pc = Input(UInt(32.W))
  val update_target = Input(UInt(32.W))
  val update_valid = Input(Bool())
  val update_taken = Input(Bool())
  val update_is_branch = Input(Bool())
  val update_is_jalr = Input(Bool())
  val update_index = Input(UInt(log2Ceil(bhtSize).W))

  val update_is_call = Input(Bool())
  val update_is_ret = Input(Bool())
}

class BPU(val conf: CoreConfig, bhtSize: Int = BHT_SIZE, indirectSize: Int = INDIRECT_TARGET_SIZE) extends Module {
  val io = IO(new BPU_IO(bhtSize, indirectSize))

  private val bhtW = log2Ceil(bhtSize)
  private val ittW = log2Ceil(indirectSize)
  private val rasW = log2Ceil(RAS_SIZE)

  val bht = RegInit(VecInit(Seq.fill(bhtSize)(BHT_INIT.U(2.W))))
  val bht_valid = RegInit(VecInit(Seq.fill(bhtSize)(false.B)))
  val ghr = RegInit(0.U(bhtW.W))

  val tagged_valid = RegInit(VecInit(Seq.fill(bhtSize)(false.B)))
  val tagged_tag = RegInit(VecInit(Seq.fill(bhtSize)(0.U(TAGGED_BHT_TAG_BITS.W))))
  val tagged_ctr = RegInit(VecInit(Seq.fill(bhtSize)(3.U(3.W))))

  val itt_valid = RegInit(VecInit(Seq.fill(indirectSize)(false.B)))
  val itt_tag = RegInit(VecInit(Seq.fill(indirectSize)(0.U(TAGGED_BHT_TAG_BITS.W))))
  val itt_target = RegInit(VecInit(Seq.fill(indirectSize)(0.U(32.W))))
  val itt_conf = RegInit(VecInit(Seq.fill(indirectSize)(0.U(2.W))))

  val ras = RegInit(VecInit(Seq.fill(RAS_SIZE)(0.U(32.W))))
  val ras_ptr = RegInit(0.U(rasW.W))
  val ras_cnt = RegInit(0.U(log2Ceil(RAS_SIZE + 1).W))
  val ras_empty = ras_cnt === 0.U
  val ras_top_idx = Wire(UInt(rasW.W))
  ras_top_idx := Mux(ras_ptr === 0.U, (RAS_SIZE - 1).U(rasW.W), (ras_ptr - 1.U)(rasW - 1, 0))
  val ras_target = Mux1H((0 until RAS_SIZE).map(i => (ras_top_idx === i.U) -> ras(i)))

  def satInc2(v: UInt): UInt = Mux(v === 3.U, 3.U, v + 1.U)
  def satDec2(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)
  def satInc3(v: UInt): UInt = Mux(v === 7.U, 7.U, v + 1.U)
  def satDec3(v: UInt): UInt = Mux(v === 0.U, 0.U, v - 1.U)
  def pcTag(pc: UInt): UInt = pc(TAGGED_BHT_TAG_BITS + 11, 12)
  def ittIndex(pc: UInt): UInt = pc(ittW + 1, 2)

  class PredResult extends Bundle {
    val valid = Bool()
    val taken = Bool()
    val target = UInt(32.W)
    val index = UInt(bhtW.W)
    val taggedHit = Bool()
    val indirectHit = Bool()
  }

  def predict(pc: UInt, inst: UInt): PredResult = {
    val res = Wire(new PredResult)
    val opcode = inst(6, 0)
    val isJal = opcode === "b1101111".U
    val isJalr = opcode === "b1100111".U
    val isBranch = opcode === "b1100011".U

    val imm = Module(new IMM)
    imm.io.inst := inst
    imm.io.imm_type := Mux(isJal, ImmJ, ImmB)

    val rs1IsRa = inst(19, 15) === 1.U
    val isRet = isJalr && rs1IsRa
    val isIndirect = isJalr && !isRet

    val bhtIndex = pc(bhtW + 1, 2) ^ ghr
    val baseValue = bht(bhtIndex)
    val baseTrained = bht_valid(bhtIndex)
    val baseTaken = baseValue(1)
    val coldStaticTaken = imm.io.imm_ext(31)
    val basePredictTaken =
      if (BHT_COLD_STATIC) Mux(baseTrained, baseTaken, coldStaticTaken)
      else baseTaken

    val tagHit = tagged_valid(bhtIndex) && tagged_tag(bhtIndex) === pcTag(pc)
    val dirTaken = Mux(tagHit, tagged_ctr(bhtIndex)(2), basePredictTaken)

      val itIdx = ittIndex(pc)
      val itTagHit = itt_valid(itIdx) && itt_tag(itIdx) === pcTag(pc)
      val itHit = itTagHit && (itt_conf(itIdx) === 3.U)

    val predValid = isJal || isBranch || isRet || (isIndirect && itHit)
    val predTaken = predValid && MuxCase(false.B, Seq(
      isJal -> true.B,
      isBranch -> dirTaken,
      isRet -> !ras_empty,
      isIndirect -> itHit
    ))
    val target = MuxCase(pc + imm.io.imm_ext, Seq(
      isRet -> ras_target,
      isIndirect -> itt_target(itIdx)
    ))

    res.valid := predValid
    res.taken := predTaken
    res.target := target
    res.index := bhtIndex
    res.taggedHit := isBranch && tagHit
    res.indirectHit := isIndirect && itHit
    res
  }

  val p0 = predict(io.predict_pc, io.predict_inst)
  val p1 = predict(io.predict_pc1, io.predict_inst1)

  io.bp_valid := p0.valid
  io.bp_taken := p0.valid && p0.taken
  io.bp_target := p0.target
  io.bp_index := p0.index
  io.bp_tagged_hit := p0.taggedHit
  io.bp_indirect_hit := p0.indirectHit

  io.bp1_valid := p1.valid
  io.bp1_taken := p1.valid && p1.taken
  io.bp1_target := p1.target
  io.bp1_index := p1.index
  io.bp1_tagged_hit := p1.taggedHit
  io.bp1_indirect_hit := p1.indirectHit

  when(io.update_valid && io.update_is_branch) {
    val baseValue = bht(io.update_index)
    val basePred = baseValue(1)
    val baseWasTrained = bht_valid(io.update_index)
    bht(io.update_index) := Mux(io.update_taken, satInc2(baseValue), satDec2(baseValue))
    bht_valid(io.update_index) := true.B

    val tag = pcTag(io.update_pc)
    val tagHit = tagged_valid(io.update_index) && tagged_tag(io.update_index) === tag
    when(tagHit) {
      val tagValue = tagged_ctr(io.update_index)
      tagged_ctr(io.update_index) := Mux(io.update_taken, satInc3(tagValue), satDec3(tagValue))
    }.elsewhen(!baseWasTrained || (basePred =/= io.update_taken)) {
      tagged_valid(io.update_index) := true.B
      tagged_tag(io.update_index) := tag
      tagged_ctr(io.update_index) := Mux(io.update_taken, 4.U, 3.U)
    }

    ghr := Cat(ghr(bhtW - 2, 0), io.update_taken)
  }

  when(io.update_valid && io.update_is_jalr && !io.update_is_ret) {
    val idx = ittIndex(io.update_pc)
    val tag = pcTag(io.update_pc)
    val tagHit = itt_valid(idx) && itt_tag(idx) === tag
    when(tagHit && itt_target(idx) === io.update_target) {
      itt_conf(idx) := satInc2(itt_conf(idx))
    }.elsewhen(tagHit) {
      itt_target(idx) := io.update_target
      itt_conf(idx) := 0.U
    }.otherwise {
      itt_valid(idx) := true.B
      itt_tag(idx) := tag
      itt_target(idx) := io.update_target
      itt_conf(idx) := 1.U
    }
  }

  when(io.update_valid && io.update_is_call) {
    ras(ras_ptr) := io.update_pc + 4.U
    ras_ptr := Mux(ras_ptr === (RAS_SIZE - 1).U, 0.U, ras_ptr + 1.U)
    ras_cnt := Mux(ras_cnt === RAS_SIZE.U, ras_cnt, ras_cnt + 1.U)
  }
  when(io.update_valid && io.update_is_ret) {
    ras_ptr := Mux(ras_ptr === 0.U, (RAS_SIZE - 1).U, ras_ptr - 1.U)
    ras_cnt := Mux(ras_cnt === 0.U, 0.U, ras_cnt - 1.U)
  }
}
