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

  val invCacheable = cacheable(io.invalidate_addr)
  val invIndex = indexOf(io.invalidate_addr)
  val invTag = tagOf(io.invalidate_addr)
  val inv2Cacheable = cacheable(io.invalidate2_addr)
  val inv2Index = indexOf(io.invalidate2_addr)
  val inv2Tag = tagOf(io.invalidate2_addr)
  val cpuInvalidatedNow =
    (io.invalidate_valid && invCacheable && invIndex === cpuIndex && invTag === cpuTag) ||
    (io.invalidate2_valid && inv2Cacheable && inv2Index === cpuIndex && inv2Tag === cpuTag)

  val cpuHit = cpuCacheable && cpuLine.valid && (cpuLine.tag === cpuTag) && !cpuInvalidatedNow
  val cpuHitData = cpuLine.data(cpuWord)

  val hitRespValid = RegInit(false.B)
  val hitRespData = RegInit(0.U(32.W))
  val hitRespResp = RegInit(0.U(2.W))
  val hitRespId = RegInit(0.U(4.W))
  val missRespValid = RegInit(false.B)
  val missRespData = RegInit(0.U(32.W))
  val missRespResp = RegInit(0.U(2.W))
  val missRespId = RegInit(0.U(4.W))

  val mshrValid = RegInit(false.B)
  val mshrCacheable = RegInit(false.B)
  val mshrAddr = RegInit(0.U(32.W))
  val mshrSize = RegInit(2.U(3.W))
  val mshrId = RegInit(0.U(4.W))
  val mshrFillIdx = RegInit(0.U(wordW.W))
  val mshrFillLine = RegInit(VecInit(Seq.fill(words)(0.U(32.W))))
  val mshrResp = RegInit(0.U(2.W))
  val mshrKilled = RegInit(false.B)
  val sMshrAr :: sMshrR :: Nil = Enum(2)
  val mshrState = RegInit(sMshrAr)

  val mshrIndex = indexOf(mshrAddr)
  val mshrTag = tagOf(mshrAddr)
  val mshrWord = wordOf(mshrAddr)
  val killMshrNow =
    mshrValid && mshrCacheable && (
      (io.invalidate_valid && invCacheable && invIndex === mshrIndex && invTag === mshrTag) ||
      (io.invalidate2_valid && inv2Cacheable && inv2Index === mshrIndex && inv2Tag === mshrTag)
    )

  val respValid = hitRespValid || missRespValid
  val respIsHit = hitRespValid
  io.cpu.rvalid := respValid
  io.cpu.rdata := Mux(respIsHit, hitRespData, missRespData)
  io.cpu.rresp := Mux(respIsHit, hitRespResp, missRespResp)
  io.cpu.rlast := true.B
  io.cpu.rid := Mux(respIsHit, hitRespId, missRespId)

  val canAcceptHit = cpuHit && !hitRespValid
  val canAcceptMissOrBypass = !mshrValid && !missRespValid
  io.cpu.arready := Mux(cpuHit, canAcceptHit, canAcceptMissOrBypass)
  val cpuArFire = io.cpu.arvalid && io.cpu.arready

  io.cpu.awready := false.B
  io.cpu.wready := false.B
  io.cpu.bvalid := false.B
  io.cpu.bresp := 0.U
  io.cpu.bid := 0.U

  io.mem.araddr := 0.U
  io.mem.arvalid := false.B
  io.mem.arid := mshrId
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

  io.busy := mshrValid || hitRespValid || missRespValid

  val cpuRespFire = io.cpu.rvalid && io.cpu.rready
  when(cpuRespFire) {
    when(hitRespValid) {
      hitRespValid := false.B
    }.otherwise {
      missRespValid := false.B
    }
  }

  when(cpuArFire) {
    when(cpuHit) {
      hitRespValid := true.B
      hitRespData := cpuHitData
      hitRespResp := 0.U
      hitRespId := io.cpu.arid
    }.otherwise {
      mshrValid := true.B
      mshrCacheable := cpuCacheable
      mshrAddr := io.cpu.araddr
      mshrSize := io.cpu.arsize
      mshrId := io.cpu.arid
      mshrFillIdx := 0.U
      mshrResp := 0.U
      mshrKilled := false.B
      mshrState := sMshrAr
      for (i <- 0 until words) {
        mshrFillLine(i) := 0.U
      }
    }
  }

  when(mshrValid) {
    when(mshrState === sMshrAr) {
      io.mem.araddr := Mux(mshrCacheable, lineBase(mshrAddr) + (mshrFillIdx << 2), mshrAddr)
      io.mem.arvalid := true.B
      io.mem.arid := mshrId
      io.mem.arlen := 0.U
      io.mem.arsize := Mux(mshrCacheable, 2.U, mshrSize)
      io.mem.arburst := 1.U
      when(io.mem.arready) {
        mshrState := sMshrR
      }
    }.otherwise {
      io.mem.rready := !missRespValid
      when(io.mem.rvalid && io.mem.rready) {
        when(mshrCacheable) {
          val nextLine = Wire(Vec(words, UInt(32.W)))
          nextLine := mshrFillLine
          nextLine(mshrFillIdx) := io.mem.rdata
          mshrFillLine := nextLine
          when(io.mem.rresp =/= 0.U) {
            mshrResp := io.mem.rresp
          }
          when(mshrFillIdx === (words - 1).U) {
            when(!mshrKilled && !killMshrNow) {
              lines(mshrIndex).valid := true.B
              lines(mshrIndex).tag := mshrTag
              lines(mshrIndex).data := nextLine
            }
            missRespValid := true.B
            missRespData := nextLine(mshrWord)
            missRespResp := Mux(io.mem.rresp =/= 0.U, io.mem.rresp, mshrResp)
            missRespId := mshrId
            mshrValid := false.B
            mshrState := sMshrAr
          }.otherwise {
            mshrFillIdx := mshrFillIdx + 1.U
            mshrState := sMshrAr
          }
        }.otherwise {
          missRespValid := true.B
          missRespData := io.mem.rdata
          missRespResp := io.mem.rresp
          missRespId := mshrId
          mshrValid := false.B
          mshrState := sMshrAr
        }
      }
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
  when(killMshrNow) {
    mshrKilled := true.B
  }

  if (conf.statistics) {
    PM(conf, clock, EVENT_DCACHE_ACCESS, 1.U, cpuArFire && cpuCacheable)
    PM(conf, clock, EVENT_DCACHE_HIT, 1.U, cpuArFire && cpuHit)
    PM(conf, clock, EVENT_DCACHE_MISS, 1.U, cpuArFire && cpuCacheable && !cpuHit)
    PM(conf, clock, EVENT_DCACHE_BYPASS, 1.U, cpuArFire && !cpuCacheable)
    PM(conf, clock, EVENT_DCACHE_MSHR_ALLOC, 1.U, cpuArFire && cpuCacheable && !cpuHit)
    PM(conf, clock, EVENT_DCACHE_HIT_UNDER_MISS, 1.U, cpuArFire && cpuHit && mshrValid)
    PM(conf, clock, EVENT_DCACHE_MSHR_REFILL, 1.U,
      mshrValid && mshrCacheable && mshrState === sMshrR &&
      io.mem.rvalid && io.mem.rready && mshrFillIdx === (words - 1).U)
  }
}
