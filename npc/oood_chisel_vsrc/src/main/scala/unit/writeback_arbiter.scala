package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.LSU_WBU_IO

class WritebackArbiter(inputCount: Int = 4, outputCount: Int = 2) extends Module {
  require(inputCount >= outputCount)
  require(outputCount >= 1)

  val io = IO(new Bundle {
    val in = Flipped(Vec(inputCount, Decoupled(new LSU_WBU_IO)))
    val out = Vec(outputCount, Decoupled(new LSU_WBU_IO))
    val robHead = Input(UInt(OoOParams.ROB_PTR_W.W))
    val grantIdx = Output(Vec(outputCount, UInt(log2Ceil(inputCount).W)))
  })

  def age(robIdx: UInt): UInt =
    (robIdx - io.robHead)(OoOParams.ROB_PTR_W - 1, 0)

  def oldest(valid: Seq[Bool]): Vec[Bool] = {
    val grant = Wire(Vec(inputCount, Bool()))
    for (i <- 0 until inputCount) {
      val olderExists = (0 until inputCount).map { j =>
        val ageJ = age(io.in(j).bits.rob_idx)
        val ageI = age(io.in(i).bits.rob_idx)
        valid(j) && ((ageJ < ageI) || ((ageJ === ageI) && (j < i).B))
      }.foldLeft(false.B)(_ || _)
      grant(i) := valid(i) && !olderExists
    }
    grant
  }

  val remaining = Wire(Vec(outputCount + 1, Vec(inputCount, Bool())))
  val grants = Wire(Vec(outputCount, Vec(inputCount, Bool())))
  for (i <- 0 until inputCount) {
    remaining(0)(i) := io.in(i).valid
  }
  for (o <- 0 until outputCount) {
    grants(o) := oldest(remaining(o))
    for (i <- 0 until inputCount) {
      remaining(o + 1)(i) := remaining(o)(i) && !grants(o)(i)
    }
    io.out(o).valid := grants(o).asUInt.orR
    io.grantIdx(o) := OHToUInt(grants(o))
    io.out(o).bits := 0.U.asTypeOf(new LSU_WBU_IO)
    when(io.out(o).valid) {
      io.out(o).bits := Mux1H(grants(o), io.in.map(_.bits))
    }
  }

  for (i <- 0 until inputCount) {
    io.in(i).ready := (0 until outputCount).map { o =>
      grants(o)(i) && io.out(o).ready
    }.reduce(_ || _)
  }

  when(!reset.asBool) {
    for (a <- 0 until outputCount; b <- a + 1 until outputCount) {
      assert((grants(a).asUInt & grants(b).asUInt) === 0.U,
        "writeback ports must not grant the same result")
    }
    for (o <- 0 until outputCount - 1) {
      when(io.out(o).valid && io.out(o + 1).valid) {
        assert(age(io.out(o).bits.rob_idx) <= age(io.out(o + 1).bits.rob_idx),
          "writeback grants must be ordered by ROB age")
      }
    }
  }
}
