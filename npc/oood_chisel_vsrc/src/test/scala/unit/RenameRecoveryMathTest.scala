package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import common.OoOParams
import org.scalatest.flatspec.AnyFlatSpec

class RenameRecoveryCountHarness extends Module {
  private val ptrW = log2Ceil(OoOParams.ROB_SIZE)
  val io = IO(new Bundle {
    val head = Input(UInt(ptrW.W))
    val lastKept = Input(UInt(ptrW.W))
    val empty = Input(Bool())
    val keptCount = Output(UInt((ptrW + 1).W))
  })

  io.keptCount := RenameRecoveryMath.keptCount(io.head, io.lastKept, io.empty)
}

class RenameRecoveryMathTest extends AnyFlatSpec {
  behavior of "RenameRecoveryMath"

  it should "preserve the full ROB and wrapped recovery ranges" in {
    simulate(new RenameRecoveryCountHarness) { dut =>
      dut.io.empty.poke(false.B)

      dut.io.head.poke(0.U)
      dut.io.lastKept.poke(31.U)
      dut.io.keptCount.expect(32.U)

      dut.io.head.poke(28.U)
      dut.io.lastKept.poke(3.U)
      dut.io.keptCount.expect(8.U)

      dut.io.head.poke(7.U)
      dut.io.lastKept.poke(7.U)
      dut.io.keptCount.expect(1.U)

      dut.io.empty.poke(true.B)
      dut.io.keptCount.expect(0.U)
    }
  }
}
