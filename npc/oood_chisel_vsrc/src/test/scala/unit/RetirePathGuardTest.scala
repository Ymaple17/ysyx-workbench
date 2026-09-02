package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class RetirePathGuardTest extends AnyFlatSpec {
  behavior of "RetirePathGuard"

  private def clear(dut: RetirePathGuard): Unit = {
    for (lane <- 0 until 4) {
      dut.io.check(lane).valid.poke(false.B)
      dut.io.check(lane).isControl.poke(false.B)
      dut.io.check(lane).pc.poke(0.U)
      dut.io.check(lane).actualTaken.poke(false.B)
      dut.io.check(lane).actualTarget.poke(0.U)
      dut.io.check(lane).nextValid.poke(false.B)
      dut.io.check(lane).nextPc.poke(0.U)
      dut.io.check(lane).robIdx.poke(0.U)
    }
  }

  it should "accept matching sequential and taken successors" in {
    simulate(new RetirePathGuard()) { dut =>
      clear(dut)
      dut.io.check(0).valid.poke(true.B)
      dut.io.check(0).isControl.poke(true.B)
      dut.io.check(0).pc.poke("h80001000".U)
      dut.io.check(0).nextValid.poke(true.B)
      dut.io.check(0).nextPc.poke("h80001004".U)
      dut.io.check(1).valid.poke(true.B)
      dut.io.check(1).isControl.poke(true.B)
      dut.io.check(1).pc.poke("h80002000".U)
      dut.io.check(1).actualTaken.poke(true.B)
      dut.io.check(1).actualTarget.poke("h80002120".U)
      dut.io.check(1).nextValid.poke(true.B)
      dut.io.check(1).nextPc.poke("h80002120".U)
      dut.io.flush.expect(false.B)
      for (lane <- 0 until 4) dut.io.waitForNext(lane).expect(false.B)
    }
  }

  it should "redirect a final-lane control before the wrong next ROB head retires" in {
    simulate(new RetirePathGuard()) { dut =>
      clear(dut)
      dut.io.check(3).valid.poke(true.B)
      dut.io.check(3).isControl.poke(true.B)
      dut.io.check(3).pc.poke("h8000409c".U)
      dut.io.check(3).actualTaken.poke(true.B)
      dut.io.check(3).actualTarget.poke("h80004134".U)
      dut.io.check(3).nextValid.poke(true.B)
      dut.io.check(3).nextPc.poke(0.U)
      dut.io.check(3).robIdx.poke(11.U)
      dut.io.flush.expect(true.B)
      dut.io.waitForNext(3).expect(false.B)
      dut.io.flushRobIdx.expect(11.U)
      dut.io.correctPc.expect("h80004134".U)
    }
  }

  it should "select the oldest mismatch and ignore a missing successor" in {
    simulate(new RetirePathGuard()) { dut =>
      clear(dut)
      for (lane <- Seq(1, 2)) {
        dut.io.check(lane).valid.poke(true.B)
        dut.io.check(lane).isControl.poke(true.B)
        dut.io.check(lane).pc.poke((0x80003000L + lane * 4).U)
        dut.io.check(lane).actualTaken.poke(true.B)
        dut.io.check(lane).actualTarget.poke((0x80003100L + lane * 4).U)
        dut.io.check(lane).nextValid.poke(true.B)
        dut.io.check(lane).nextPc.poke(0.U)
        dut.io.check(lane).robIdx.poke((20 + lane).U)
      }
      dut.io.check(0).valid.poke(true.B)
      dut.io.check(0).isControl.poke(true.B)
      dut.io.check(0).nextValid.poke(false.B)
      dut.io.flush.expect(true.B)
      dut.io.waitForNext(0).expect(true.B)
      dut.io.flushRobIdx.expect(21.U)
      dut.io.correctPc.expect("h80003104".U)
    }
  }

  it should "hold a final control until its immediate successor is visible" in {
    simulate(new RetirePathGuard()) { dut =>
      clear(dut)
      dut.io.check(2).valid.poke(true.B)
      dut.io.check(2).isControl.poke(true.B)
      dut.io.check(2).pc.poke("h80005000".U)
      dut.io.check(2).actualTaken.poke(false.B)
      dut.io.check(2).nextValid.poke(false.B)
      dut.io.flush.expect(false.B)
      dut.io.waitForNext(2).expect(true.B)

      dut.io.check(2).nextValid.poke(true.B)
      dut.io.check(2).nextPc.poke("h80005004".U)
      dut.io.waitForNext(2).expect(false.B)
      dut.io.flush.expect(false.B)
    }
  }
}
