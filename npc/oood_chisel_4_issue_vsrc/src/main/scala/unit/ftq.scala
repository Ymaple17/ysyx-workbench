package unit

import chisel3._
import chisel3.util._
import common.{BPU_Config, OoOParams}

class FTQAlloc extends Bundle {
  val basePc = UInt(32.W)
  val lanePc = Vec(OoOParams.FETCH_WIDTH, UInt(32.W))
  val validMask = UInt(OoOParams.FETCH_WIDTH.W)
  val predictedNextPc = UInt(32.W)
  val cfiSlot = UInt(log2Ceil(OoOParams.FETCH_WIDTH + 1).W)
  val ghr = UInt(BPU_Config.GHR_LENGTH.W)
  val pathHistory = UInt(BPU_Config.PATH_HISTORY_LENGTH.W)
  val ras = Vec(BPU_Config.RAS_SIZE, UInt(32.W))
  val rasPtr = UInt(log2Ceil(BPU_Config.RAS_SIZE).W)
  val rasCount = UInt(log2Ceil(BPU_Config.RAS_SIZE + 1).W)
}

class FTQEntry extends FTQAlloc {
  val valid = Bool()
  val generation = UInt(OoOParams.FTQ_GEN_W.W)
  val pending = UInt(log2Ceil(OoOParams.FETCH_WIDTH + 1).W)
}

class FTQ(n: Int = OoOParams.FTQ_SIZE) extends Module {
  require(n >= 2 && isPow2(n))
  require(n == OoOParams.FTQ_SIZE)

  private val ptrW = log2Ceil(n)
  val io = IO(new Bundle {
    val alloc = Flipped(Decoupled(new FTQAlloc))
    val allocIdx = Output(UInt(ptrW.W))
    val allocGeneration = Output(UInt(OoOParams.FTQ_GEN_W.W))

    val commit0Valid = Input(Bool())
    val commit0Idx = Input(UInt(ptrW.W))
    val commit0Generation = Input(UInt(OoOParams.FTQ_GEN_W.W))
    val commit1Valid = Input(Bool())
    val commit1Idx = Input(UInt(ptrW.W))
    val commit1Generation = Input(UInt(OoOParams.FTQ_GEN_W.W))
    val commit2Valid = Input(Bool())
    val commit2Idx = Input(UInt(ptrW.W))
    val commit2Generation = Input(UInt(OoOParams.FTQ_GEN_W.W))
    val commit3Valid = Input(Bool())
    val commit3Idx = Input(UInt(ptrW.W))
    val commit3Generation = Input(UInt(OoOParams.FTQ_GEN_W.W))

    val recoverIdx = Input(UInt(ptrW.W))
    val recoverGeneration = Input(UInt(OoOParams.FTQ_GEN_W.W))
    val recoverValid = Output(Bool())
    val recover = Output(new FTQAlloc)
    val recoverFlush = Input(Bool())
    val recoverSlot = Input(UInt(log2Ceil(OoOParams.FETCH_WIDTH).W))

    val flush = Input(Bool())
    val count = Output(UInt(log2Ceil(n + 1).W))
    val full = Output(Bool())
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new FTQEntry))))
  val generations = RegInit(VecInit(Seq.fill(n)(0.U(OoOParams.FTQ_GEN_W.W))))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  io.alloc.ready := !io.flush && !io.recoverFlush && count =/= n.U
  io.allocIdx := tail
  io.allocGeneration := generations(tail)
  io.count := count
  io.full := count === n.U

  val recoverEntry = entries(io.recoverIdx)
  val recoverAge = (io.recoverIdx - head)(ptrW - 1, 0)
  val recoverInWindow = recoverAge < count
  io.recoverValid := recoverEntry.valid &&
    recoverEntry.generation === io.recoverGeneration && recoverInWindow
  io.recover.basePc := recoverEntry.basePc
  io.recover.lanePc := recoverEntry.lanePc
  io.recover.validMask := recoverEntry.validMask
  io.recover.predictedNextPc := recoverEntry.predictedNextPc
  io.recover.cfiSlot := recoverEntry.cfiSlot
  io.recover.ghr := recoverEntry.ghr
  io.recover.pathHistory := recoverEntry.pathHistory
  io.recover.ras := recoverEntry.ras
  io.recover.rasPtr := recoverEntry.rasPtr
  io.recover.rasCount := recoverEntry.rasCount

  val commitHits = Wire(Vec(n, UInt(log2Ceil(OoOParams.CORE_WIDTH + 1).W)))
  val willFree = Wire(Vec(n, Bool()))
  for (i <- 0 until n) {
    val hit0 = io.commit0Valid && entries(i).valid &&
      io.commit0Idx === i.U && entries(i).generation === io.commit0Generation
    val hit1 = io.commit1Valid && entries(i).valid &&
      io.commit1Idx === i.U && entries(i).generation === io.commit1Generation
    val hit2 = io.commit2Valid && entries(i).valid &&
      io.commit2Idx === i.U && entries(i).generation === io.commit2Generation
    val hit3 = io.commit3Valid && entries(i).valid &&
      io.commit3Idx === i.U && entries(i).generation === io.commit3Generation
    commitHits(i) := PopCount(Seq(hit0, hit1, hit2, hit3))
    willFree(i) := entries(i).valid && commitHits(i) =/= 0.U &&
      entries(i).pending <= commitHits(i)
  }

  val freeHead = Wire(Vec(OoOParams.CORE_WIDTH, Bool()))
  for (lane <- 0 until OoOParams.CORE_WIDTH) {
    val prefix = (0 until lane).map(freeHead(_)).foldLeft(true.B)(_ && _)
    freeHead(lane) := prefix && willFree((head + lane.U)(ptrW - 1, 0))
  }
  val freeCount = PopCount(freeHead)
  val recoverKeepCount = recoverAge +& 1.U

  when(io.flush || (io.recoverFlush && !io.recoverValid)) {
    for (i <- 0 until n) {
      entries(i).valid := false.B
      generations(i) := generations(i) + 1.U
    }
    head := 0.U
    tail := 0.U
    count := 0.U
  }.elsewhen(io.recoverFlush) {
    for (i <- 0 until n) {
      val entryAge = (i.U(ptrW.W) - head)(ptrW - 1, 0)
      when(entries(i).valid && entryAge > recoverAge) {
        entries(i).valid := false.B
        entries(i).pending := 0.U
        generations(i) := generations(i) + 1.U
      }
    }
    val recoverSlots = io.recoverSlot +& 1.U
    val keepMask = ((1.U((OoOParams.FETCH_WIDTH + 1).W) <<
      recoverSlots) - 1.U)(OoOParams.FETCH_WIDTH - 1, 0)
    val recoveredMask = recoverEntry.validMask & keepMask
    val removed = PopCount(recoverEntry.validMask & ~keepMask)
    val recoveredPending = recoverEntry.pending - removed
    when(recoveredMask.orR && recoveredPending =/= 0.U) {
      entries(io.recoverIdx).validMask := recoveredMask
      entries(io.recoverIdx).pending := recoveredPending
      tail := io.recoverIdx + 1.U
      count := recoverKeepCount
    }.otherwise {
      entries(io.recoverIdx).valid := false.B
      entries(io.recoverIdx).pending := 0.U
      generations(io.recoverIdx) := generations(io.recoverIdx) + 1.U
      tail := io.recoverIdx
      count := recoverAge
    }
  }.otherwise {
    for (i <- 0 until n) {
      when(commitHits(i) =/= 0.U) {
        when(willFree(i)) {
          entries(i).valid := false.B
          entries(i).pending := 0.U
          generations(i) := generations(i) + 1.U
        }.otherwise {
          entries(i).pending := entries(i).pending - commitHits(i)
        }
      }
    }
    when(io.alloc.fire) {
      val entry = Wire(new FTQEntry)
      entry := 0.U.asTypeOf(new FTQEntry)
      entry.valid := true.B
      entry.generation := generations(tail)
      entry.pending := PopCount(io.alloc.bits.validMask)
      entry.basePc := io.alloc.bits.basePc
      entry.lanePc := io.alloc.bits.lanePc
      entry.validMask := io.alloc.bits.validMask
      entry.predictedNextPc := io.alloc.bits.predictedNextPc
      entry.cfiSlot := io.alloc.bits.cfiSlot
      entry.ghr := io.alloc.bits.ghr
      entry.pathHistory := io.alloc.bits.pathHistory
      entry.ras := io.alloc.bits.ras
      entry.rasPtr := io.alloc.bits.rasPtr
      entry.rasCount := io.alloc.bits.rasCount
      entries(tail) := entry
      tail := tail + 1.U
    }
    when(freeCount =/= 0.U) {
      head := head + freeCount
    }
    count := count + io.alloc.fire.asUInt - freeCount
  }

  when(!reset.asBool && !io.flush) {
    assert(PopCount(entries.map(_.valid)) === count,
      "FTQ valid entries must match the queue count")
    assert(!willFree.asUInt.orR || freeHead(0),
      "FTQ commits must free blocks in fetch order")
    assert(!io.recoverFlush || !(io.commit0Valid || io.commit1Valid ||
      io.commit2Valid || io.commit3Valid),
      "FTQ redirect must not race architectural commit")
    assert(!io.recoverFlush || !io.recoverValid || recoverEntry.pending >=
      PopCount(recoverEntry.validMask & ~((1.U((OoOParams.FETCH_WIDTH + 1).W) <<
        (io.recoverSlot +& 1.U)) - 1.U)(OoOParams.FETCH_WIDTH - 1, 0)),
      "FTQ recovery cannot remove more slots than remain pending")
  }
}
