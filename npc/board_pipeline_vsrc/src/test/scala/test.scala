package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import common.ALU_OP._
import org.scalatest.flatspec.AnyFlatSpec

class BoardPipelineUnitTest extends AnyFlatSpec {
  private def resetDut(clock: Clock, reset: Reset): Unit = {
    reset.poke(true.B)
    clock.step()
    reset.poke(false.B)
    clock.step()
  }

  behavior of "MUL"

  it should "return all RV32M multiply forms through the registered result stage" in {
    simulate(new MUL) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.kill.poke(false.B)
      dut.io.req_valid.poke(false.B)
      dut.io.result_ready.poke(true.B)

      def check(a: BigInt, b: BigInt, op: UInt, expected: BigInt): Unit = {
        dut.io.a.poke(a.U(32.W))
        dut.io.b.poke(b.U(32.W))
        dut.io.op.poke(op)
        dut.io.req_valid.poke(true.B)
        dut.io.req_ready.expect(true.B)
        dut.clock.step()
        dut.io.req_valid.poke(false.B)
        dut.io.result_valid.expect(true.B)
        dut.io.result.expect(expected.U(32.W))
        dut.clock.step()
        dut.io.busy.expect(false.B)
      }

      check(BigInt("fffffffe", 16), 3, ALU_MUL, BigInt("fffffffa", 16))
      check(BigInt("fffffffe", 16), 3, ALU_MULH, BigInt("ffffffff", 16))
      check(BigInt("fffffffe", 16), BigInt("ffffffff", 16), ALU_MULHSU, BigInt("fffffffe", 16))
      check(BigInt("ffffffff", 16), BigInt("ffffffff", 16), ALU_MULHU, BigInt("fffffffe", 16))
      check(BigInt("80000000", 16), BigInt("80000000", 16), ALU_MULH, BigInt("40000000", 16))
      check(BigInt("80000000", 16), BigInt("ffffffff", 16), ALU_MULHSU, BigInt("80000000", 16))
      check(BigInt("80000000", 16), 2, ALU_MULH, BigInt("ffffffff", 16))
    }
  }

  it should "discard an in-flight multiply on flush" in {
    simulate(new MUL) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.a.poke(7.U)
      dut.io.b.poke(9.U)
      dut.io.op.poke(ALU_MUL)
      dut.io.kill.poke(false.B)
      dut.io.req_valid.poke(true.B)
      dut.io.result_ready.poke(true.B)
      dut.clock.step()
      dut.io.req_valid.poke(false.B)
      dut.io.kill.poke(true.B)
      dut.io.result_valid.expect(false.B)
      dut.clock.step()
      dut.io.kill.poke(false.B)
      dut.io.busy.expect(false.B)
    }
  }

  it should "accept a new multiply while the previous result is consumed" in {
    simulate(new MUL) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.kill.poke(false.B)
      dut.io.result_ready.poke(true.B)

      dut.io.a.poke(6.U)
      dut.io.b.poke(7.U)
      dut.io.op.poke(ALU_MUL)
      dut.io.req_valid.poke(true.B)
      dut.clock.step()

      dut.io.result_valid.expect(true.B)
      dut.io.result.expect(42.U)
      dut.io.req_ready.expect(true.B)
      dut.io.a.poke(9.U)
      dut.io.b.poke(11.U)
      dut.clock.step()

      dut.io.req_valid.poke(false.B)
      dut.io.result_valid.expect(true.B)
      dut.io.result.expect(99.U)
      dut.clock.step()
      dut.io.result_valid.expect(false.B)
    }
  }

  behavior of "DIV"

  it should "return every RV32M divide form after the preparation stage" in {
    simulate(new DIV) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.kill.poke(false.B)
      dut.io.req_valid.poke(false.B)

      def check(a: BigInt, b: BigInt, op: UInt, expected: BigInt): Unit = {
        dut.io.a.poke(a.U(32.W))
        dut.io.b.poke(b.U(32.W))
        dut.io.op.poke(op)
        dut.io.req_valid.poke(true.B)
        dut.io.req_ready.expect(true.B)
        dut.clock.step()
        dut.io.req_valid.poke(false.B)
        dut.io.busy.expect(true.B)

        var cycles = 0
        while (!dut.io.result_valid.peek().litToBoolean && cycles < 40) {
          dut.clock.step()
          cycles += 1
        }
        assert(cycles < 40, "divider did not complete")
        dut.io.result.expect(expected.U(32.W))
        dut.clock.step()
        dut.io.busy.expect(false.B)
      }

      check(BigInt("ffffffec", 16), 3, ALU_DIV, BigInt("fffffffa", 16))
      check(BigInt("ffffffec", 16), 3, ALU_REM, BigInt("fffffffe", 16))
      check(BigInt("fffffffe", 16), 2, ALU_DIVU, BigInt("7fffffff", 16))
      check(BigInt("fffffffe", 16), 3, ALU_REMU, 2)
      check(7, 0, ALU_DIV, BigInt("ffffffff", 16))
      check(BigInt("ffffffec", 16), 0, ALU_REM, BigInt("ffffffec", 16))
    }
  }

  it should "discard a divide request during its preparation stage" in {
    simulate(new DIV) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.a.poke(100.U)
      dut.io.b.poke(7.U)
      dut.io.op.poke(ALU_DIVU)
      dut.io.kill.poke(false.B)
      dut.io.req_valid.poke(true.B)
      dut.clock.step()

      dut.io.req_valid.poke(false.B)
      dut.io.kill.poke(true.B)
      dut.io.result_valid.expect(false.B)
      dut.clock.step()
      dut.io.kill.poke(false.B)
      dut.io.busy.expect(false.B)
      dut.io.req_ready.expect(true.B)
    }
  }
}
