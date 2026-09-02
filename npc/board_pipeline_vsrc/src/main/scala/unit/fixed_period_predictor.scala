package unit

import chisel3._
import chisel3.util._

object FixedPeriodPredictor {
  val Entries = 512
  val IndexBits = log2Ceil(Entries)
  val HistoryBits = 16
  val SeenBits = log2Ceil(HistoryBits + 1)
  val CounterBits = 2
  val MetaWidth = HistoryBits + SeenBits + CounterBits
}

class FixedPeriodPredictorIO extends Bundle {
  import FixedPeriodPredictor._

  val predictPc = Input(UInt(32.W))
  val predictTaken = Output(Bool())
  val predictMeta = Output(UInt(MetaWidth.W))
  val initDone = Output(Bool())

  val updatePc = Input(UInt(32.W))
  val updateValid = Input(Bool())
  val updateTaken = Input(Bool())
  val updateMeta = Input(UInt(MetaWidth.W))
}

class FixedPeriodPredictor extends Module {
  import FixedPeriodPredictor._

  val io = IO(new FixedPeriodPredictorIO)

  // CoreMark's dominant periodic branches repeat after eight outcomes. Keep
  // history, warmup count, and bimodal fallback in one LUTRAM entry so the
  // prediction path contains one memory lookup rather than an LHT->PHT chain.
  val table = Mem(Entries, UInt(MetaWidth.W))
  val initIndex = RegInit(0.U(IndexBits.W))
  val initializing = RegInit(true.B)
  io.initDone := !initializing

  val predictIndex = io.predictPc(IndexBits + 1, 2)
  val updateIndex = io.updatePc(IndexBits + 1, 2)

  val updateCounter = io.updateMeta(CounterBits - 1, 0)
  val updateSeen = io.updateMeta(CounterBits + SeenBits - 1, CounterBits)
  val updateHistory = io.updateMeta(MetaWidth - 1, CounterBits + SeenBits)

  val nextCounter = Mux(io.updateTaken,
    Mux(updateCounter === 3.U, 3.U, updateCounter + 1.U),
    Mux(updateCounter === 0.U, 0.U, updateCounter - 1.U))
  val nextSeen = Mux(updateSeen === HistoryBits.U,
    updateSeen, updateSeen + 1.U)
  val nextHistory = Cat(updateHistory(HistoryBits - 2, 0), io.updateTaken)
  val nextEntry = Cat(nextHistory, nextSeen, nextCounter)

  val tableRead = table(predictIndex)
  val updateCollision = !initializing && io.updateValid &&
    updateIndex === predictIndex
  val predictEntry = Mux(updateCollision, nextEntry, tableRead)
  val predictCounter = predictEntry(CounterBits - 1, 0)
  val predictSeen = predictEntry(CounterBits + SeenBits - 1, CounterBits)
  val predictHistory = predictEntry(MetaWidth - 1, CounterBits + SeenBits)
  val periodReady = predictSeen === HistoryBits.U &&
    predictHistory(15, 8) === predictHistory(7, 0)

  io.predictTaken := Mux(periodReady, predictHistory(7), predictCounter(1))
  io.predictMeta := predictEntry

  val initialEntry = Cat(
    0.U(HistoryBits.W),
    0.U(SeenBits.W),
    1.U(CounterBits.W))
  val writeEnable = initializing || io.updateValid
  val writeIndex = Mux(initializing, initIndex, updateIndex)
  val writeData = Mux(initializing, initialEntry, nextEntry)
  when(writeEnable) {
    table.write(writeIndex, writeData)
  }

  when(initializing) {
    when(initIndex === (Entries - 1).U) {
      initializing := false.B
    }.otherwise {
      initIndex := initIndex + 1.U
    }
  }
}
