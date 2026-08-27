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

class DCacheReadResp extends Bundle {
  val data = UInt(32.W)
  val resp = UInt(2.W)
  val id = UInt(4.W)
}

class DCacheIO extends Bundle {
  val cpu = Flipped(new AXI4Master)
  val mem = new AXI4Master

  val invalidate_valid = Input(Bool())
  val invalidate_addr  = Input(UInt(32.W))
  val invalidate2_valid = Input(Bool())
  val invalidate2_addr  = Input(UInt(32.W))
  val invalidate3_valid = Input(Bool())
  val invalidate3_addr  = Input(UInt(32.W))
  val store_valid = Input(Bool())
  val store_addr  = Input(UInt(32.W))
  val store_data  = Input(UInt(32.W))
  val store_mask  = Input(UInt(4.W))
  val store2_valid = Input(Bool())
  val store2_addr  = Input(UInt(32.W))
  val store2_data  = Input(UInt(32.W))
  val store2_mask  = Input(UInt(4.W))
  val store3_valid = Input(Bool())
  val store3_addr  = Input(UInt(32.W))
  val store3_data  = Input(UInt(32.W))
  val store3_mask  = Input(UInt(4.W))
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
  def expandMask(mask: UInt): UInt =
    Cat((3 to 0 by -1).map(i => Fill(8, mask(i))))
  def storeWord(old: UInt, addr: UInt, data: UInt, rawMask: UInt): UInt = {
    val off = addr(1, 0)
    val mask = (rawMask << off)(3, 0)
    val bits = expandMask(mask)
    val shifted = data << (off << 3)
    (old & ~bits) | (shifted & bits)
  }

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
  val inv3Cacheable = cacheable(io.invalidate3_addr)
  val inv3Index = indexOf(io.invalidate3_addr)
  val inv3Tag = tagOf(io.invalidate3_addr)
  val cpuInvalidatedNow =
    (io.invalidate_valid && invCacheable && invIndex === cpuIndex && invTag === cpuTag) ||
    (io.invalidate2_valid && inv2Cacheable && inv2Index === cpuIndex && inv2Tag === cpuTag) ||
    (io.invalidate3_valid && inv3Cacheable && inv3Index === cpuIndex && inv3Tag === cpuTag)

  val cpuHit = cpuCacheable && cpuLine.valid && (cpuLine.tag === cpuTag) && !cpuInvalidatedNow
  val storeHitCpu = io.store_valid && cacheable(io.store_addr) &&
    indexOf(io.store_addr) === cpuIndex && tagOf(io.store_addr) === cpuTag &&
    wordOf(io.store_addr) === cpuWord
  val store2HitCpu = io.store2_valid && cacheable(io.store2_addr) &&
    indexOf(io.store2_addr) === cpuIndex && tagOf(io.store2_addr) === cpuTag &&
    wordOf(io.store2_addr) === cpuWord
  val store3HitCpu = io.store3_valid && cacheable(io.store3_addr) &&
    indexOf(io.store3_addr) === cpuIndex && tagOf(io.store3_addr) === cpuTag &&
    wordOf(io.store3_addr) === cpuWord
  val cpuHitAfterStore3 = Mux(store3HitCpu,
    storeWord(cpuLine.data(cpuWord), io.store3_addr, io.store3_data, io.store3_mask),
    cpuLine.data(cpuWord))
  val cpuHitAfterStore0 = Mux(storeHitCpu,
    storeWord(cpuHitAfterStore3, io.store_addr, io.store_data, io.store_mask),
    cpuHitAfterStore3)
  val cpuHitData = Mux(store2HitCpu,
    storeWord(cpuHitAfterStore0, io.store2_addr, io.store2_data, io.store2_mask),
    cpuHitAfterStore0)

  val hitRespQ = Module(new Queue(new DCacheReadResp, 2, pipe = true, flow = true))
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
  val pendingValid = RegInit(false.B)
  val pendingCacheable = RegInit(false.B)
  val pendingAddr = RegInit(0.U(32.W))
  val pendingSize = RegInit(2.U(3.W))
  val pendingId = RegInit(0.U(4.W))
  val sMshrAr :: sMshrR :: sMshrLocal :: Nil = Enum(3)
  val mshrState = RegInit(sMshrAr)

  val mshrIndex = indexOf(mshrAddr)
  val mshrTag = tagOf(mshrAddr)
  val mshrWord = wordOf(mshrAddr)
  val killMshrNow =
    mshrValid && mshrCacheable && (
      (io.invalidate_valid && invCacheable && invIndex === mshrIndex && invTag === mshrTag) ||
      (io.invalidate2_valid && inv2Cacheable && inv2Index === mshrIndex && inv2Tag === mshrTag) ||
      (io.invalidate3_valid && inv3Cacheable && inv3Index === mshrIndex && inv3Tag === mshrTag) ||
      (io.store_valid && cacheable(io.store_addr) &&
        indexOf(io.store_addr) === mshrIndex && tagOf(io.store_addr) === mshrTag) ||
      (io.store2_valid && cacheable(io.store2_addr) &&
        indexOf(io.store2_addr) === mshrIndex && tagOf(io.store2_addr) === mshrTag) ||
      (io.store3_valid && cacheable(io.store3_addr) &&
        indexOf(io.store3_addr) === mshrIndex && tagOf(io.store3_addr) === mshrTag)
    )

  val respValid = hitRespQ.io.deq.valid || missRespValid
  val respIsHit = hitRespQ.io.deq.valid
  io.cpu.rvalid := respValid
  io.cpu.rdata := Mux(respIsHit, hitRespQ.io.deq.bits.data, missRespData)
  io.cpu.rresp := Mux(respIsHit, hitRespQ.io.deq.bits.resp, missRespResp)
  io.cpu.rlast := true.B
  io.cpu.rid := Mux(respIsHit, hitRespQ.io.deq.bits.id, missRespId)
  hitRespQ.io.deq.ready := io.cpu.rready && respIsHit

  val cpuRespFire = io.cpu.rvalid && io.cpu.rready
  val missRespSlotFree = !missRespValid || (cpuRespFire && !respIsHit)

  val canAcceptHit = cpuHit && hitRespQ.io.enq.ready
  val mshrCompletingNow = mshrValid && (mshrState === sMshrR) &&
    io.mem.rvalid && !missRespValid &&
    Mux(mshrCacheable, mshrFillIdx === (words - 1).U, true.B)
  val localCompletingNow = mshrValid && (mshrState === sMshrLocal) &&
    missRespSlotFree && !mshrKilled && !killMshrNow
  val canAcceptMissOrBypass = (!mshrValid && !missRespValid) ||
    (mshrValid && !pendingValid && !mshrCompletingNow && !localCompletingNow)
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

  io.busy := mshrValid || pendingValid || hitRespQ.io.deq.valid || missRespValid

  when(cpuRespFire) {
    when(!respIsHit) {
      missRespValid := false.B
    }
  }

  hitRespQ.io.enq.valid := io.cpu.arvalid && cpuHit
  hitRespQ.io.enq.bits.data := cpuHitData
  hitRespQ.io.enq.bits.resp := 0.U
  hitRespQ.io.enq.bits.id := io.cpu.arid

  when(cpuArFire) {
    when(!cpuHit) {
      when(!mshrValid) {
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
      }.otherwise {
        pendingValid := true.B
        pendingCacheable := cpuCacheable
        pendingAddr := io.cpu.araddr
        pendingSize := io.cpu.arsize
        pendingId := io.cpu.arid
      }
    }
  }

  when(mshrValid) {
    when(mshrState === sMshrAr) {
      io.mem.araddr := Mux(mshrCacheable, lineBase(mshrAddr), mshrAddr)
      io.mem.arvalid := true.B
      io.mem.arid := mshrId
      io.mem.arlen := Mux(mshrCacheable, (words - 1).U, 0.U)
      io.mem.arsize := Mux(mshrCacheable, 2.U, mshrSize)
      io.mem.arburst := 1.U
      when(io.mem.arready) {
        mshrState := sMshrR
        mshrKilled := false.B
      }
    }.elsewhen(mshrState === sMshrR) {
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
            val refillResp = Mux(io.mem.rresp =/= 0.U, io.mem.rresp, mshrResp)
            val mergePending = pendingValid && pendingCacheable &&
              (lineBase(pendingAddr) === lineBase(mshrAddr)) &&
              !mshrKilled && !killMshrNow && (refillResp === 0.U)
            when(!mshrKilled && !killMshrNow) {
              lines(mshrIndex).valid := true.B
              lines(mshrIndex).tag := mshrTag
              lines(mshrIndex).data := nextLine
            }
            missRespValid := true.B
            missRespData := nextLine(mshrWord)
            missRespResp := refillResp
            missRespId := mshrId
            when(pendingValid) {
              mshrValid := true.B
              mshrCacheable := pendingCacheable
              mshrAddr := pendingAddr
              mshrSize := pendingSize
              mshrId := pendingId
              mshrFillIdx := 0.U
              mshrResp := 0.U
              mshrKilled := false.B
              pendingValid := false.B
              when(mergePending) {
                mshrFillLine := nextLine
                mshrState := sMshrLocal
              }.otherwise {
                for (i <- 0 until words) {
                  mshrFillLine(i) := 0.U
                }
                mshrState := sMshrAr
              }
            }.otherwise {
              mshrValid := false.B
              mshrState := sMshrAr
            }
          }.otherwise {
            mshrFillIdx := mshrFillIdx + 1.U
          }
        }.otherwise {
          missRespValid := true.B
          missRespData := io.mem.rdata
          missRespResp := io.mem.rresp
          missRespId := mshrId
          when(pendingValid) {
            mshrValid := true.B
            mshrCacheable := pendingCacheable
            mshrAddr := pendingAddr
            mshrSize := pendingSize
            mshrId := pendingId
            mshrFillIdx := 0.U
            mshrResp := 0.U
            mshrKilled := false.B
            pendingValid := false.B
            for (i <- 0 until words) {
              mshrFillLine(i) := 0.U
            }
            mshrState := sMshrAr
          }.otherwise {
            mshrValid := false.B
            mshrState := sMshrAr
          }
        }
      }
    }.otherwise {
      val localInvalidated = mshrKilled || killMshrNow
      when(localInvalidated) {
        mshrFillIdx := 0.U
        mshrResp := 0.U
        mshrState := sMshrAr
        for (i <- 0 until words) {
          mshrFillLine(i) := 0.U
        }
      }.elsewhen(missRespSlotFree) {
        val mergePending = pendingValid && pendingCacheable &&
          (lineBase(pendingAddr) === lineBase(mshrAddr))
        missRespValid := true.B
        missRespData := mshrFillLine(mshrWord)
        missRespResp := 0.U
        missRespId := mshrId
        when(pendingValid) {
          mshrValid := true.B
          mshrCacheable := pendingCacheable
          mshrAddr := pendingAddr
          mshrSize := pendingSize
          mshrId := pendingId
          mshrFillIdx := 0.U
          mshrResp := 0.U
          mshrKilled := false.B
          pendingValid := false.B
          when(mergePending) {
            mshrState := sMshrLocal
          }.otherwise {
            for (i <- 0 until words) {
              mshrFillLine(i) := 0.U
            }
            mshrState := sMshrAr
          }
        }.otherwise {
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
  when(io.invalidate3_valid && inv3Cacheable &&
      lines(inv3Index).valid && (lines(inv3Index).tag === inv3Tag)) {
    lines(inv3Index).valid := false.B
  }
  val storeCacheable = cacheable(io.store_addr)
  val storeIndex = indexOf(io.store_addr)
  val storeTag = tagOf(io.store_addr)
  val storeWordIdx = wordOf(io.store_addr)
  val storeHit = io.store_valid && storeCacheable && lines(storeIndex).valid &&
    lines(storeIndex).tag === storeTag
  val store2Cacheable = cacheable(io.store2_addr)
  val store2Index = indexOf(io.store2_addr)
  val store2Tag = tagOf(io.store2_addr)
  val store2WordIdx = wordOf(io.store2_addr)
  val store2Hit = io.store2_valid && store2Cacheable && lines(store2Index).valid &&
    lines(store2Index).tag === store2Tag
  val store3Cacheable = cacheable(io.store3_addr)
  val store3Index = indexOf(io.store3_addr)
  val store3Tag = tagOf(io.store3_addr)
  val store3WordIdx = wordOf(io.store3_addr)
  val store3Hit = io.store3_valid && store3Cacheable && lines(store3Index).valid &&
    lines(store3Index).tag === store3Tag
  val store3Updated = storeWord(lines(store3Index).data(store3WordIdx),
    io.store3_addr, io.store3_data, io.store3_mask)
  val store0Base = Mux(store3Hit && store3Index === storeIndex &&
    store3WordIdx === storeWordIdx, store3Updated, lines(storeIndex).data(storeWordIdx))
  val store0Updated = storeWord(store0Base,
    io.store_addr, io.store_data, io.store_mask)
  when(store3Hit) {
    lines(store3Index).data(store3WordIdx) := store3Updated
  }
  when(storeHit) {
    lines(storeIndex).data(storeWordIdx) := store0Updated
  }
  when(store2Hit) {
    val afterStore3 = Mux(store3Hit && store3Index === store2Index &&
      store3WordIdx === store2WordIdx, store3Updated, lines(store2Index).data(store2WordIdx))
    val afterStore0 = Mux(storeHit && storeIndex === store2Index &&
      storeWordIdx === store2WordIdx, store0Updated, afterStore3)
    lines(store2Index).data(store2WordIdx) := storeWord(afterStore0,
      io.store2_addr, io.store2_data, io.store2_mask)
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
    PM(conf, clock, EVENT_DCACHE_STORE_HIT,
      PopCount(Seq(storeHit, store2Hit)), storeHit || store2Hit)
    PM(conf, clock, EVENT_DCACHE_SECONDARY_ALLOC, 1.U,
      cpuArFire && !cpuHit && mshrValid)
    val refillMerge = mshrCompletingNow && mshrCacheable && pendingValid &&
      pendingCacheable && (lineBase(pendingAddr) === lineBase(mshrAddr)) &&
      !mshrKilled && !killMshrNow &&
      (Mux(io.mem.rresp =/= 0.U, io.mem.rresp, mshrResp) === 0.U)
    val localMerge = mshrValid && (mshrState === sMshrLocal) &&
      missRespSlotFree && !mshrKilled && !killMshrNow && pendingValid &&
      pendingCacheable && (lineBase(pendingAddr) === lineBase(mshrAddr))
    PM(conf, clock, EVENT_DCACHE_MSHR_MERGE, 1.U, refillMerge || localMerge)
  }
}
