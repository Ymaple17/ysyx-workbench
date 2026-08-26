package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.LSU_WBU_IO

class WritebackArbiter(inputCount: Int = 4, outputCount: Int = 2) extends Module {
  require(inputCount >= outputCount)
  require(outputCount == 2)

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

  val valid0 = io.in.map(_.valid)
  val grant0 = oldest(valid0)
  val valid1 = (0 until inputCount).map(i => io.in(i).valid && !grant0(i))
  val grant1 = oldest(valid1)

  io.out(0).valid := grant0.asUInt.orR
  io.out(1).valid := grant1.asUInt.orR
  io.grantIdx(0) := OHToUInt(grant0)
  io.grantIdx(1) := OHToUInt(grant1)
  io.out(0).bits := 0.U.asTypeOf(new LSU_WBU_IO)
  io.out(1).bits := 0.U.asTypeOf(new LSU_WBU_IO)
  when(io.out(0).valid) {
    io.out(0).bits := Mux1H(grant0, io.in.map(_.bits))
  }
  when(io.out(1).valid) {
    io.out(1).bits := Mux1H(grant1, io.in.map(_.bits))
  }

  for (i <- 0 until inputCount) {
    io.in(i).ready := (grant0(i) && io.out(0).ready) ||
      (grant1(i) && io.out(1).ready)
  }

  when(!reset.asBool) {
    assert((grant0.asUInt & grant1.asUInt) === 0.U,
      "writeback ports must not grant the same result")
    when(io.out(0).valid && io.out(1).valid) {
      assert(age(io.out(0).bits.rob_idx) <= age(io.out(1).bits.rob_idx),
        "writeback grants must be ordered by ROB age")
    }
  }
}
