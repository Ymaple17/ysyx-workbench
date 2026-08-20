package unit

import chisel3._
import chisel3.util._
import bus.AXI4Master
import core.CoreConfig
import core.PerfEvents._
import core.PM

class DCacheLine(tagWidth: Int, words: Int) extends Bundle {
  val valid = Bool()
  val tag = UInt(tagWidth.W)
  val data = Vec(words, UInt(32.W))
}

class DCacheIO extends Bundle {
  val cpu = Flipped(new AXI4Master)
  val mem = new AXI4Master

  val invalidate_valid = Input(Bool())
  val invalidate_addr  = Input(UInt(32.W))
  val invalidate2_valid = Input(Bool())
  val invalidate2_addr  = Input(UInt(32.W))
  val busy = Output(Bool())
}

class DCache(set: Int = 64, blockSize: Int = 32, conf: CoreConfig) extends Module {
  require(set >= 2 && isPow2(set))
  require(blockSize >= 4 && isPow2(blockSize))

  val io = IO(new DCacheIO)

  private val offsetW = log2Ceil(blockSize)
  private val indexW = log2Ceil(set)
  private val words = blockSize / 4
  private val wordW = log2Ceil(words).max(1)
  private val tagW = 32 - offsetW - indexW

  val lines = RegInit(VecInit(Seq.fill(set)(0.U.asTypeOf(new DCacheLine(tagW, words)))))
  val fillLine = RegInit(VecInit(Seq.fill(words)(0.U(32.W))))
  val fillIdx = RegInit(0.U(wordW.W))
  val reqAddr = RegInit(0.U(32.W))
  val reqSize = RegInit(2.U(3.W))
  val reqId = RegInit(0.U(4.W))
  val respData = RegInit(0.U(32.W))
  val respResp = RegInit(0.U(2.W))
  val missResp = RegInit(0.U(2.W))
  val fillKilled = RegInit(false.B)

  val sIdle :: sData :: sMemAr :: sMemR :: sBypassAr :: sBypassR :: Nil = Enum(6)
  val state = RegInit(sIdle)

  def cacheable(addr: UInt): Bool =
    (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)

  def tagOf(addr: UInt): UInt = addr(31, offsetW + indexW)
  def indexOf(addr: UInt): UInt = addr(offsetW + indexW - 1, offsetW)
  def wordOf(addr: UInt): UInt =
    if (words == 1) 0.U(wordW.W) else addr(offsetW - 1, 2)
  def lineBase(addr: UInt): UInt = addr & ~((blockSize - 1).U(32.W))

  val cpuCacheable = cacheable(io.cpu.araddr)
  val cpuIndex = indexOf(io.cpu.araddr)
  val cpuTag = tagOf(io.cpu.araddr)
  val cpuWord = wordOf(io.cpu.araddr)
  val cpuLine = lines(cpuIndex)
  val cpuHit = cpuCacheable && cpuLine.valid && (cpuLine.tag === cpuTag)
  val cpuHitData = cpuLine.data(cpuWord)

  val reqIndex = indexOf(reqAddr)
  val reqTag = tagOf(reqAddr)
  val reqWord = wordOf(reqAddr)
  val invCacheable = cacheable(io.invalidate_addr)
  val invIndex = indexOf(io.invalidate_addr)
  val invTag = tagOf(io.invalidate_addr)
  val inv2Cacheable = cacheable(io.invalidate2_addr)
  val inv2Index = indexOf(io.invalidate2_addr)
  val inv2Tag = tagOf(io.invalidate2_addr)
  val filling = (state === sMemAr) || (state === sMemR)
  val killFillNow =
    (io.invalidate_valid && invCacheable && filling &&
      (invIndex === reqIndex) && (invTag === reqTag)) ||
    (io.invalidate2_valid && inv2Cacheable && filling &&
      (inv2Index === reqIndex) && (inv2Tag === reqTag))

  io.cpu.arready := state === sIdle
  io.cpu.rvalid := state === sData
  io.cpu.rdata := respData
  io.cpu.rresp := respResp
  io.cpu.rlast := true.B
  io.cpu.rid := reqId

  io.cpu.awready := false.B
  io.cpu.wready := false.B
  io.cpu.bvalid := false.B
  io.cpu.bresp := 0.U
  io.cpu.bid := 0.U

  io.mem.araddr := 0.U
  io.mem.arvalid := false.B
  io.mem.arid := reqId
  io.mem.arlen := 0.U
  io.mem.arsize := 2.U
  io.mem.arburst := 1.U
  io.mem.rready := false.B
  io.mem.awaddr := 0.U
  io.mem.awvalid := false.B
  io.mem.awid := 0.U
  io.mem.awlen := 0.U
  io.mem.awsize := 0.U
  io.mem.awburst := 0.U
  io.mem.wdata := 0.U
  io.mem.wstrb := 0.U
  io.mem.wvalid := false.B
  io.mem.wlast := false.B
  io.mem.bready := false.B

  io.busy := state =/= sIdle

  val cpuArFire = io.cpu.arvalid && io.cpu.arready

  when(state === sIdle) {
    when(cpuArFire) {
      reqAddr := io.cpu.araddr
      reqSize := io.cpu.arsize
      reqId := io.cpu.arid
      respResp := 0.U
      missResp := 0.U
      when(cpuHit) {
        respData := cpuHitData
        state := sData
      }.elsewhen(cpuCacheable) {
        fillKilled := false.B
        fillIdx := 0.U
        for (i <- 0 until words) {
          fillLine(i) := 0.U
        }
        state := sMemAr
      }.otherwise {
        state := sBypassAr
      }
    }
  }.elsewhen(state === sData) {
    when(io.cpu.rready) {
      state := sIdle
    }
  }.elsewhen(state === sMemAr) {
    io.mem.araddr := lineBase(reqAddr) + (fillIdx << 2)
    io.mem.arvalid := true.B
    io.mem.arid := reqId
    io.mem.arlen := 0.U
    io.mem.arsize := 2.U
    io.mem.arburst := 1.U
    when(io.mem.arready) {
      state := sMemR
    }
  }.elsewhen(state === sMemR) {
    io.mem.rready := true.B
    when(io.mem.rvalid) {
      val nextLine = Wire(Vec(words, UInt(32.W)))
      nextLine := fillLine
      nextLine(fillIdx) := io.mem.rdata
      fillLine := nextLine
      when(io.mem.rresp =/= 0.U) {
        missResp := io.mem.rresp
      }
      when(fillIdx === (words - 1).U) {
        when(!fillKilled && !killFillNow) {
          lines(reqIndex).valid := true.B
          lines(reqIndex).tag := reqTag
          lines(reqIndex).data := nextLine
        }
        respData := nextLine(reqWord)
        respResp := Mux(io.mem.rresp =/= 0.U, io.mem.rresp, missResp)
        state := sData
      }.otherwise {
        fillIdx := fillIdx + 1.U
        state := sMemAr
      }
    }
  }.elsewhen(state === sBypassAr) {
    io.mem.araddr := reqAddr
    io.mem.arvalid := true.B
    io.mem.arid := reqId
    io.mem.arlen := 0.U
    io.mem.arsize := reqSize
    io.mem.arburst := 1.U
    when(io.mem.arready) {
      state := sBypassR
    }
  }.elsewhen(state === sBypassR) {
    io.mem.rready := io.cpu.rready
    io.cpu.rvalid := io.mem.rvalid
    io.cpu.rdata := io.mem.rdata
    io.cpu.rresp := io.mem.rresp
    io.cpu.rlast := io.mem.rlast
    io.cpu.rid := io.mem.rid
    when(io.mem.rvalid && io.cpu.rready) {
      state := sIdle
    }
  }

  when(io.invalidate_valid && invCacheable &&
      lines(invIndex).valid && (lines(invIndex).tag === invTag)) {
    lines(invIndex).valid := false.B
  }
  when(io.invalidate2_valid && inv2Cacheable &&
      lines(inv2Index).valid && (lines(inv2Index).tag === inv2Tag)) {
    lines(inv2Index).valid := false.B
  }
  when(killFillNow) {
    fillKilled := true.B
  }

  if (conf.statistics) {
    PM(conf, clock, EVENT_DCACHE_ACCESS, 1.U, cpuArFire && cpuCacheable)
    PM(conf, clock, EVENT_DCACHE_HIT, 1.U, cpuArFire && cpuHit)
    PM(conf, clock, EVENT_DCACHE_MISS, 1.U, cpuArFire && cpuCacheable && !cpuHit)
    PM(conf, clock, EVENT_DCACHE_BYPASS, 1.U, cpuArFire && !cpuCacheable)
  }
}
