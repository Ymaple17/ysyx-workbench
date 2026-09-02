package unit

import chisel3._
import chisel3.util._
import common.OoOParams

class RetirePathCheck extends Bundle {
  val valid = Bool()
  val isControl = Bool()
  val pc = UInt(32.W)
  val actualTaken = Bool()
  val actualTarget = UInt(32.W)
  val nextValid = Bool()
  val nextPc = UInt(32.W)
  val robIdx = UInt(OoOParams.ROB_PTR_W.W)
}

/**
  * Last-line control-flow validation at the retirement boundary.
  *
  * Execute-time recovery remains the normal path. This guard catches a
  * frontend/metadata ownership failure that left a wrong younger PC in the
  * ROB despite a completed control-flow instruction.
  */
class RetirePathGuard(width: Int = OoOParams.COMMIT_WIDTH) extends Module {
  require(width >= 1)

  val io = IO(new Bundle {
    val check = Input(Vec(width, new RetirePathCheck))
    val flush = Output(Bool())
    val waitForNext = Output(Vec(width, Bool()))
    val flushRobIdx = Output(UInt(OoOParams.ROB_PTR_W.W))
    val correctPc = Output(UInt(32.W))
  })

  val expectedPc = Wire(Vec(width, UInt(32.W)))
  val mismatch = Wire(Vec(width, Bool()))
  for (lane <- 0 until width) {
    expectedPc(lane) := Mux(io.check(lane).actualTaken,
      io.check(lane).actualTarget, io.check(lane).pc + 4.U)
    mismatch(lane) := io.check(lane).valid && io.check(lane).isControl &&
      io.check(lane).nextValid && io.check(lane).nextPc =/= expectedPc(lane)
    io.waitForNext(lane) := io.check(lane).valid &&
      io.check(lane).isControl && !io.check(lane).nextValid
  }

  val oldest = PriorityEncoderOH(mismatch.asUInt)
  io.flush := mismatch.asUInt.orR
  io.flushRobIdx := Mux(io.flush, Mux1H(oldest, io.check.map(_.robIdx)), 0.U)
  io.correctPc := Mux(io.flush, Mux1H(oldest, expectedPc), 0.U)
}
