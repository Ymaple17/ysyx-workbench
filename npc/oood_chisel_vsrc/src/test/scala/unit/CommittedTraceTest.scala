package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import common.BPU_Config.GHR_LENGTH
import common.OoOParams
import org.scalatest.flatspec.AnyFlatSpec

class CommittedTraceTest extends AnyFlatSpec {
  private def resetBuilder(dut: CommittedTraceBuilder): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    dut.clock.step()
  }

  private def idleBuilder(dut: CommittedTraceBuilder): Unit = {
    dut.io.clear.poke(false.B)
    for (lane <- 0 until OoOParams.COMMIT_WIDTH) {
      dut.io.commit(lane).valid.poke(false.B)
      dut.io.commit(lane).bits.pc.poke(0.U)
      dut.io.commit(lane).bits.inst.poke(0.U)
      dut.io.commit(lane).bits.bpIndex.poke(0.U)
      dut.io.commit(lane).bits.actualNextPc.poke(0.U)
    }
  }

  private def pokeCommit(
      dut: CommittedTraceBuilder,
      lane: Int,
      pc: BigInt,
      inst: BigInt = BigInt("00000013", 16),
      bpIndex: BigInt = 0): Unit = {
    dut.io.commit(lane).valid.poke(true.B)
    dut.io.commit(lane).bits.pc.poke(pc.U)
    dut.io.commit(lane).bits.inst.poke(inst.U)
    dut.io.commit(lane).bits.bpIndex.poke(bpIndex.U)
    dut.io.commit(lane).bits.actualNextPc.poke((pc + 4).U)
  }

  private def resetCache(dut: CommittedTraceCache): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    dut.clock.step()
  }

  private def idleCache(dut: CommittedTraceCache): Unit = {
    dut.io.lookupPc.poke(0.U)
    dut.io.lookupContext.poke(0.U)
    dut.io.invalidate.poke(false.B)
    for (lane <- 0 until OoOParams.COMMIT_WIDTH) {
      dut.io.fill(lane).valid.poke(false.B)
      dut.io.fill(lane).bits.startPc.poke(0.U)
      dut.io.fill(lane).bits.context.poke(0.U)
      dut.io.fill(lane).bits.nextPc.poke(0.U)
      for (slot <- 0 until OoOParams.FETCH_WIDTH) {
        dut.io.fill(lane).bits.pc(slot).poke(0.U)
        dut.io.fill(lane).bits.inst(slot).poke(0.U)
      }
    }
  }

  private def pokeFill(
      dut: CommittedTraceCache,
      startPc: BigInt,
      context: BigInt,
      nextPc: BigInt,
      instSeed: BigInt = BigInt("00100013", 16)): Unit = {
    dut.io.fill(0).valid.poke(true.B)
    dut.io.fill(0).bits.startPc.poke(startPc.U)
    dut.io.fill(0).bits.context.poke(context.U)
    dut.io.fill(0).bits.nextPc.poke(nextPc.U)
    for (slot <- 0 until OoOParams.FETCH_WIDTH) {
      dut.io.fill(0).bits.pc(slot).poke((startPc + slot * 8).U)
      dut.io.fill(0).bits.inst(slot).poke((instSeed + slot).U)
    }
  }

  behavior of "CommittedTraceBuilder"

  it should "form a cross-control four-uop window from architectural commits" in {
    simulate(new CommittedTraceBuilder) { dut =>
      resetBuilder(dut)
      idleBuilder(dut)
      val base = BigInt("80001000", 16)
      val ghr = BigInt("55aa", 16)
      val path = BigInt("1234", 16)
      val bpIndex = ghr | (path << GHR_LENGTH)
      val pcs = Seq(base, base + 4, base + 0x80, base + 0x84)
      for (lane <- pcs.indices) {
        pokeCommit(dut, lane, pcs(lane), bpIndex = if (lane == 0) bpIndex else 0)
      }
      dut.clock.step()

      idleBuilder(dut)
      pokeCommit(dut, 0, base + 0x88)
      dut.io.fill(0).valid.expect(true.B)
      dut.io.fill(0).bits.startPc.expect(base.U)
      dut.io.fill(0).bits.context.expect((ghr ^ path).U)
      dut.io.fill(0).bits.nextPc.expect((base + 0x88).U)
      for (slot <- pcs.indices) {
        dut.io.fill(0).bits.pc(slot).expect(pcs(slot).U)
      }
    }
  }

  it should "reject internal RAS-changing controls and clear its rolling state" in {
    simulate(new CommittedTraceBuilder) { dut =>
      resetBuilder(dut)
      idleBuilder(dut)
      val base = BigInt("80002000", 16)
      for (lane <- 0 until OoOParams.COMMIT_WIDTH) {
        val inst = if (lane == 1) BigInt("000080e7", 16) else BigInt("00000013", 16)
        pokeCommit(dut, lane, base + lane * 4, inst)
      }
      dut.clock.step()

      idleBuilder(dut)
      pokeCommit(dut, 0, base + 16)
      dut.io.fill(0).valid.expect(false.B)
      dut.io.clear.poke(true.B)
      dut.clock.step()

      idleBuilder(dut)
      pokeCommit(dut, 0, base + 20)
      dut.io.fill(0).valid.expect(false.B)
    }
  }

  behavior of "CommittedTraceCache"

  it should "require two matching observations before serving a trace" in {
    simulate(new CommittedTraceCache(entries = 16)) { dut =>
      resetCache(dut)
      idleCache(dut)
      val base = BigInt("80003000", 16)
      val context = BigInt("12345678", 16)
      val nextPc = base + 0x100

      pokeFill(dut, base, context, nextPc)
      dut.clock.step()
      idleCache(dut)
      dut.io.lookupPc.poke(base.U)
      dut.io.lookupContext.poke(context.U)
      dut.io.confidence.expect(1.U)
      dut.io.hit.expect(false.B)

      pokeFill(dut, base, context, nextPc)
      dut.clock.step()
      idleCache(dut)
      dut.io.lookupPc.poke(base.U)
      dut.io.lookupContext.poke(context.U)
      dut.io.confidence.expect(2.U)
      dut.io.hit.expect(true.B)
      dut.io.nextPc.expect(nextPc.U)

      pokeFill(dut, base, context, nextPc + 4)
      dut.clock.step()
      idleCache(dut)
      dut.io.lookupPc.poke(base.U)
      dut.io.lookupContext.poke(context.U)
      dut.io.confidence.expect(0.U)
      dut.io.hit.expect(false.B)
    }
  }

  it should "tag the full context and invalidate every trace on fence.i" in {
    simulate(new CommittedTraceCache(entries = 16)) { dut =>
      resetCache(dut)
      idleCache(dut)
      val base = BigInt("80004000", 16)
      val context = BigInt("11223344", 16)
      for (_ <- 0 until 2) {
        pokeFill(dut, base, context, base + 0x80)
        dut.clock.step()
        idleCache(dut)
      }

      dut.io.lookupPc.poke(base.U)
      // Bits 0 and 2 fold to the same set index for this four-set cache,
      // while the full context tag still differs.
      dut.io.lookupContext.poke((context ^ 5).U)
      dut.io.hit.expect(false.B)
      dut.io.lookupContext.poke(context.U)
      dut.io.hit.expect(true.B)

      dut.io.invalidate.poke(true.B)
      dut.clock.step()
      idleCache(dut)
      dut.io.lookupPc.poke(base.U)
      dut.io.lookupContext.poke(context.U)
      dut.io.hit.expect(false.B)
    }
  }
}
