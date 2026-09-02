package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class DCacheProtocolTest extends AnyFlatSpec {
  private def initialize(dut: DCache): Unit = {
    dut.io.cpu.araddr.poke(0.U)
    dut.io.cpu.arvalid.poke(false.B)
    dut.io.cpu.arid.poke(0.U)
    dut.io.cpu.arlen.poke(0.U)
    dut.io.cpu.arsize.poke(2.U)
    dut.io.cpu.arburst.poke(1.U)
    dut.io.cpu.rready.poke(true.B)
    dut.io.cpu.awaddr.poke(0.U)
    dut.io.cpu.awvalid.poke(false.B)
    dut.io.cpu.awid.poke(0.U)
    dut.io.cpu.awlen.poke(0.U)
    dut.io.cpu.awsize.poke(2.U)
    dut.io.cpu.awburst.poke(1.U)
    dut.io.cpu.wdata.poke(0.U)
    dut.io.cpu.wstrb.poke(0.U)
    dut.io.cpu.wvalid.poke(false.B)
    dut.io.cpu.wlast.poke(true.B)
    dut.io.cpu.bready.poke(true.B)

    dut.io.earlyLoad.valid.poke(false.B)
    dut.io.earlyLoad.bits.addr.poke(0.U)
    dut.io.earlyLoad.bits.id.poke(0.U)
    dut.io.earlyLoad.bits.size.poke(2.U)
    dut.io.earlyLoadResp.ready.poke(true.B)
    dut.io.earlyLoadCancel.poke(false.B)

    dut.io.mem.arready.poke(true.B)
    dut.io.mem.rdata.poke(0.U)
    dut.io.mem.rresp.poke(0.U)
    dut.io.mem.rvalid.poke(false.B)
    dut.io.mem.rlast.poke(true.B)
    dut.io.mem.rid.poke(0.U)
    dut.io.mem.awready.poke(true.B)
    dut.io.mem.wready.poke(true.B)
    dut.io.mem.bresp.poke(0.U)
    dut.io.mem.bvalid.poke(false.B)
    dut.io.mem.bid.poke(0.U)

    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    // DCache clears its LUTRAM metadata with one write per set after reset.
    dut.clock.step(65)
  }

  private def issueRead(dut: DCache, addr: BigInt): Unit = {
    dut.io.cpu.araddr.poke(addr.U)
    dut.io.cpu.arvalid.poke(true.B)
    dut.io.cpu.arready.expect(true.B)
    dut.clock.step()
    dut.io.cpu.arvalid.poke(false.B)
  }

  private def refillLine(dut: DCache, base: BigInt, data: Seq[BigInt]): Unit = {
    require(data.length == 8)
    data.zipWithIndex.foreach { case (word, index) =>
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base + index * 4).U)
      dut.io.mem.arlen.expect(0.U)
      dut.clock.step()

      dut.io.mem.rdata.poke(word.U)
      dut.io.mem.rresp.poke(0.U)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rlast.poke(true.B)
      dut.io.mem.rready.expect(true.B)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)
    }
  }

  behavior of "DCache"

  it should "refill a cacheable line and return the next access as a one-cycle hit" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000000", 16)
      val words = (0 until 8).map(i => BigInt("10000000", 16) + i)

      issueRead(dut, base + 4)
      dut.clock.step() // lookup miss -> refill address
      refillLine(dut, base, words)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(words(1).U)
      dut.io.cpu.rresp.expect(0.U)
      dut.clock.step()

      issueRead(dut, base + 4)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(words(1).U)
      dut.io.mem.arvalid.expect(false.B)
      dut.clock.step()
    }
  }

  it should "write a partial store through without allocating the missed line" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000100", 16)
      val addr = base + 5
      val words = (0 until 8).map(i => BigInt("11223340", 16) + i)

      dut.io.cpu.awaddr.poke(addr.U)
      dut.io.cpu.awvalid.poke(true.B)
      dut.io.cpu.wdata.poke(BigInt("0000aa00", 16).U)
      dut.io.cpu.wstrb.poke("b0010".U)
      dut.io.cpu.wvalid.poke(true.B)
      dut.io.cpu.awready.expect(true.B)
      dut.io.cpu.wready.expect(true.B)
      dut.io.mem.awvalid.expect(true.B)
      dut.io.mem.awaddr.expect(addr.U)
      dut.io.mem.wvalid.expect(true.B)
      dut.io.mem.wdata.expect(BigInt("0000aa00", 16).U)
      dut.io.mem.wstrb.expect("b0010".U)
      dut.io.cpu.bvalid.expect(true.B)
      dut.io.cpu.bresp.expect(0.U)
      dut.clock.step()
      dut.io.cpu.awvalid.poke(false.B)
      dut.io.cpu.wvalid.poke(false.B)

      // The CPU response was posted with the request. The real memory response
      // now only drains the cache's single pending-store slot.
      dut.io.mem.bvalid.poke(true.B)
      dut.io.cpu.bvalid.expect(false.B)
      dut.io.mem.bready.expect(true.B)
      dut.clock.step()
      dut.io.mem.bvalid.poke(false.B)

      issueRead(dut, addr)
      dut.io.cpu.rvalid.expect(false.B)
      dut.clock.step() // no store allocation: load lookup must miss
      val updatedWords = words.updated(1, BigInt("1122aa41", 16))
      refillLine(dut, base, updatedWords)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(BigInt("1122aa41", 16).U)
      dut.clock.step()
    }
  }

  it should "post a cacheable store before the downstream B response" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val addr = BigInt("80000120", 16)

      dut.io.cpu.awaddr.poke(addr.U)
      dut.io.cpu.awvalid.poke(true.B)
      dut.io.cpu.wdata.poke(BigInt("deadbeef", 16).U)
      dut.io.cpu.wstrb.poke("b1111".U)
      dut.io.cpu.wvalid.poke(true.B)
      dut.io.cpu.bvalid.expect(true.B)
      dut.io.cpu.bresp.expect(0.U)
      dut.clock.step()
      dut.io.cpu.awvalid.poke(false.B)
      dut.io.cpu.wvalid.poke(false.B)

      dut.io.mem.bvalid.poke(false.B)
      dut.io.cpu.bvalid.expect(false.B)
      dut.clock.step() // lookup completes; real B is now owned in the background

      // The store has left the CPU pipeline, but the DCache still owns the
      // write response. A same-line miss stays ordered, while an independent
      // cacheable line may use the lookup port before B arrives.
      dut.io.cpu.araddr.poke((addr + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(false.B)
      dut.io.cpu.araddr.poke((addr + 0x40).U)
      dut.io.cpu.arready.expect(true.B)
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.bvalid.poke(true.B)
      dut.io.mem.bready.expect(true.B)
      dut.clock.step()
      dut.io.mem.bvalid.poke(false.B)

      dut.io.cpu.araddr.poke((addr + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
    }
  }

  it should "register an early load hit before returning it to the LSU" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000200", 16)
      val words = (0 until 8).map(i => BigInt("22330000", 16) + i)

      issueRead(dut, base)
      dut.clock.step()
      refillLine(dut, base, words)
      dut.io.cpu.rvalid.expect(true.B)
      dut.clock.step()

      dut.io.earlyLoad.bits.addr.poke((base + 8).U)
      dut.io.earlyLoad.bits.id.poke(0.U)
      dut.io.earlyLoad.bits.size.poke(2.U)
      dut.io.earlyLoad.valid.poke(true.B)
      dut.io.earlyLoad.ready.expect(true.B)
      dut.clock.step()
      dut.io.earlyLoad.valid.poke(false.B)

      dut.io.cpu.rvalid.expect(false.B)
      dut.io.earlyLoadResp.valid.expect(false.B)
      dut.clock.step()
      dut.io.earlyLoadResp.valid.expect(true.B)
      dut.io.earlyLoadResp.bits.data.expect(words(2).U)
      dut.io.earlyLoadResp.bits.resp.expect(0.U)
      dut.io.earlyLoadResp.bits.replay.expect(false.B)
    }
  }

  it should "accept an uncacheable early load and request an ordered LSU replay" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val mmio = BigInt("44f02000", 16)

      dut.io.earlyLoad.bits.addr.poke(mmio.U)
      dut.io.earlyLoad.valid.poke(true.B)
      dut.io.earlyLoad.ready.expect(true.B)
      dut.clock.step()
      dut.io.earlyLoad.valid.poke(false.B)

      dut.io.mem.arvalid.expect(false.B)
      dut.io.earlyLoadResp.valid.expect(false.B)
      dut.clock.step()
      dut.io.earlyLoadResp.valid.expect(true.B)
      dut.io.earlyLoadResp.bits.replay.expect(true.B)
      dut.io.mem.arvalid.expect(false.B)
    }
  }

  it should "hold an early load response while its LSU consumer is stalled" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000280", 16)
      val words = (0 until 8).map(i => BigInt("77880000", 16) + i)

      issueRead(dut, base)
      dut.clock.step()
      refillLine(dut, base, words)
      dut.io.cpu.rvalid.expect(true.B)
      dut.clock.step()

      dut.io.earlyLoad.bits.addr.poke((base + 12).U)
      dut.io.earlyLoad.valid.poke(true.B)
      dut.clock.step()
      dut.io.earlyLoad.valid.poke(false.B)
      dut.io.earlyLoadResp.ready.poke(false.B)
      dut.io.earlyLoadResp.valid.expect(false.B)
      dut.clock.step()
      dut.io.earlyLoadResp.valid.expect(true.B)
      dut.io.earlyLoadResp.bits.data.expect(words(3).U)
      dut.clock.step()

      dut.io.earlyLoadResp.valid.expect(true.B)
      dut.io.earlyLoadResp.bits.data.expect(words(3).U)
      dut.io.earlyLoadResp.ready.poke(true.B)
      dut.clock.step()
      dut.io.earlyLoadResp.valid.expect(false.B)
    }
  }

  it should "replace a canceled early load with a replay completion" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000300", 16)
      val words = (0 until 8).map(i => BigInt("99aa0000", 16) + i)

      issueRead(dut, base)
      dut.clock.step()
      refillLine(dut, base, words)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(words.head.U)
      dut.clock.step()

      dut.io.earlyLoad.bits.addr.poke((base + 4).U)
      dut.io.earlyLoad.valid.poke(true.B)
      dut.io.earlyLoad.ready.expect(true.B)
      dut.clock.step()
      dut.io.earlyLoad.valid.poke(false.B)
      dut.io.earlyLoadCancel.poke(true.B)
      dut.clock.step()

      dut.io.earlyLoadCancel.poke(false.B)
      dut.io.earlyLoadResp.valid.expect(true.B)
      dut.io.earlyLoadResp.bits.replay.expect(true.B)
      dut.io.earlyLoadResp.bits.data.expect(0.U)
      dut.clock.step()
      dut.io.earlyLoadResp.valid.expect(false.B)
    }
  }

  it should "preserve write data when AW completes before a backpressured W" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val mmio = BigInt("44f02000", 16)

      dut.io.mem.wready.poke(false.B)
      dut.io.cpu.awaddr.poke(mmio.U)
      dut.io.cpu.awvalid.poke(true.B)
      dut.io.cpu.wdata.poke(BigInt("cafebabe", 16).U)
      dut.io.cpu.wstrb.poke("b1111".U)
      dut.io.cpu.wvalid.poke(true.B)
      dut.io.cpu.awready.expect(true.B)
      dut.io.cpu.wready.expect(true.B)
      dut.io.mem.awvalid.expect(true.B)
      dut.io.mem.wvalid.expect(true.B)
      dut.clock.step()

      dut.io.cpu.awvalid.poke(false.B)
      dut.io.cpu.wvalid.poke(false.B)
      dut.io.mem.wready.poke(true.B)
      dut.io.cpu.wready.expect(false.B)
      dut.io.mem.awvalid.expect(false.B)
      dut.io.mem.wvalid.expect(true.B)
      dut.io.mem.wdata.expect(BigInt("cafebabe", 16).U)
      dut.clock.step()

      dut.io.mem.bvalid.poke(true.B)
      dut.io.mem.bready.expect(true.B)
      dut.clock.step()
      dut.io.mem.bvalid.poke(false.B)
      dut.io.cpu.bvalid.expect(true.B)
      dut.io.cpu.bresp.expect(0.U)
      dut.clock.step()
    }
  }

  it should "invalidate a cached line after a posted store error" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val base = BigInt("80000300", 16)
      val words = (0 until 8).map(i => BigInt("33440000", 16) + i)

      issueRead(dut, base)
      dut.clock.step()
      refillLine(dut, base, words)
      dut.io.cpu.rvalid.expect(true.B)
      dut.clock.step()

      dut.io.cpu.awaddr.poke(base.U)
      dut.io.cpu.awvalid.poke(true.B)
      dut.io.cpu.wdata.poke(BigInt("deadbeef", 16).U)
      dut.io.cpu.wstrb.poke("b1111".U)
      dut.io.cpu.wvalid.poke(true.B)
      dut.io.cpu.bvalid.expect(true.B)
      dut.clock.step()
      dut.io.cpu.awvalid.poke(false.B)
      dut.io.cpu.wvalid.poke(false.B)
      dut.clock.step() // hit lookup transfers ownership of B to the pending slot

      dut.io.mem.bresp.poke(2.U)
      dut.io.mem.bvalid.poke(true.B)
      dut.io.mem.bready.expect(true.B)
      dut.clock.step() // capture the response at the AXI boundary
      dut.io.mem.bvalid.poke(false.B)
      dut.io.mem.bresp.poke(0.U)

      issueRead(dut, base)
      dut.io.cpu.rvalid.expect(false.B)
      dut.clock.step() // failed write-through invalidated the former hit
      val refreshed = words.updated(0, BigInt("55667788", 16))
      refillLine(dut, base, refreshed)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(refreshed.head.U)
    }
  }

  it should "bypass the planned peripheral window without allocating a cache line" in {
    simulate(new DCache) { dut =>
      initialize(dut)
      val mmio = BigInt("44f01000", 16)

      def bypassOnce(value: BigInt): Unit = {
        issueRead(dut, mmio)
        dut.io.mem.arvalid.expect(true.B)
        dut.io.mem.araddr.expect(mmio.U)
        dut.clock.step()

        dut.io.mem.rdata.poke(value.U)
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rlast.poke(false.B) // CLINT/UART single-beat slaves omit RLAST.
        dut.io.cpu.rvalid.expect(true.B)
        dut.io.cpu.rdata.expect(value.U)
        dut.io.cpu.rlast.expect(true.B)
        dut.clock.step()
        dut.io.mem.rvalid.poke(false.B)
        dut.io.mem.rlast.poke(true.B)
      }

      bypassOnce(BigInt("deadbeef", 16))
      bypassOnce(BigInt("12345678", 16))
    }
  }
}
