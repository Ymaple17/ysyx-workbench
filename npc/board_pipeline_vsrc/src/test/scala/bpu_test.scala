package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import core.CoreConfig
import org.scalatest.flatspec.AnyFlatSpec

class BPUAliasTest extends AnyFlatSpec {
  behavior of "BPU"

  it should "not bypass a BTB update to a different tag with the same index" in {
    simulate(new BPU(new CoreConfig(32, statistics = false))) { dut =>
      dut.io.predict_pc.poke(BigInt("80000210", 16).U)
      dut.io.update_pc.poke(0.U)
      dut.io.update_target.poke(0.U)
      dut.io.update_valid.poke(false.B)
      dut.io.update_taken.poke(false.B)
      dut.io.update_is_branch.poke(false.B)
      dut.io.update_meta.poke(1.U)
      dut.io.update_is_call.poke(false.B)
      dut.io.update_is_ret.poke(false.B)

      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      while (!dut.io.init_done.peek().litToBoolean) {
        dut.clock.step()
      }

      // 0x80000210 and 0x80002210 share BTB index PC[10:2], but not tag.
      dut.io.update_pc.poke(BigInt("80002210", 16).U)
      dut.io.update_target.poke(BigInt("80000208", 16).U)
      dut.io.update_valid.poke(true.B)
      dut.io.update_taken.poke(true.B)
      dut.io.bp_valid.expect(false.B)
      dut.clock.step()

      dut.io.update_valid.poke(false.B)
      dut.io.predict_pc.poke(BigInt("80002210", 16).U)
      dut.io.bp_valid.expect(true.B)
      dut.io.bp_taken.expect(true.B)
      dut.io.bp_target.expect(BigInt("80000208", 16).U)

      dut.io.predict_pc.poke(BigInt("80000210", 16).U)
      dut.io.bp_valid.expect(false.B)
    }
  }
}

class FixedPeriodPredictorTest extends AnyFlatSpec {
  behavior of "FixedPeriodPredictor"

  it should "override bimodal direction after two matching eight-outcome periods" in {
    simulate(new FixedPeriodPredictor) { dut =>
      val pcA = BigInt("80001000", 16)
      val pcB = BigInt("80001004", 16)

      dut.io.predictPc.poke(pcA.U)
      dut.io.updatePc.poke(0.U)
      dut.io.updateValid.poke(false.B)
      dut.io.updateTaken.poke(false.B)
      dut.io.updateMeta.poke(1.U)

      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      while (!dut.io.initDone.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.predictMeta.expect(1.U)
      dut.io.predictTaken.expect(false.B)

      val period = Seq(true, false, true, true, false, false, true, false)
      for (taken <- period ++ period) {
        val meta = dut.io.predictMeta.peek().litValue
        dut.io.updatePc.poke(pcA.U)
        dut.io.updateValid.poke(true.B)
        dut.io.updateTaken.poke(taken.B)
        dut.io.updateMeta.poke(meta.U)
        dut.clock.step()
      }

      dut.io.updateValid.poke(false.B)
      // The next element of the repeated period is taken, while the last two
      // outcomes leave the bimodal fallback predicting not-taken.
      dut.io.predictTaken.expect(true.B)
      dut.io.predictPc.poke(pcB.U)
      dut.io.predictMeta.expect(1.U)
      dut.io.predictTaken.expect(false.B)
    }
  }
}
