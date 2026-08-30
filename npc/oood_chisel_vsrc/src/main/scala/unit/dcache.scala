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
  val cpu1 = Flipped(new AXI4Master)
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
  val missQueue = Module(new DCacheMissQueue(blockSize))

  def cacheable(addr: UInt): Bool =
    (addr - "h8000_0000".U(32.W)) < "h0800_0000".U(32.W)
  def tagOf(addr: UInt): UInt = addr(31, offsetW + indexW)
  def indexOf(addr: UInt): UInt = addr(offsetW + indexW - 1, offsetW)
  def wordOf(addr: UInt): UInt =
    if (words == 1) 0.U(wordW.W) else addr(offsetW - 1, 2)
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
  val cpu1Cacheable = cacheable(io.cpu1.araddr)
  val cpu1Index = indexOf(io.cpu1.araddr)
  val cpu1Tag = tagOf(io.cpu1.araddr)
  val cpu1Word = wordOf(io.cpu1.araddr)
  val cpu1Line = lines(cpu1Index)

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
  val cpu1InvalidatedNow =
    (io.invalidate_valid && invCacheable && invIndex === cpu1Index && invTag === cpu1Tag) ||
      (io.invalidate2_valid && inv2Cacheable && inv2Index === cpu1Index && inv2Tag === cpu1Tag) ||
      (io.invalidate3_valid && inv3Cacheable && inv3Index === cpu1Index && inv3Tag === cpu1Tag)

  val cpuHit = cpuCacheable && cpuLine.valid && cpuLine.tag === cpuTag &&
    !cpuInvalidatedNow
  val cpu1Hit = cpu1Cacheable && cpu1Line.valid && cpu1Line.tag === cpu1Tag &&
    !cpu1InvalidatedNow
  val store3HitCpu = io.store3_valid && cacheable(io.store3_addr) &&
    indexOf(io.store3_addr) === cpuIndex && tagOf(io.store3_addr) === cpuTag &&
    wordOf(io.store3_addr) === cpuWord
  val store3HitCpu1 = io.store3_valid && cacheable(io.store3_addr) &&
    indexOf(io.store3_addr) === cpu1Index && tagOf(io.store3_addr) === cpu1Tag &&
    wordOf(io.store3_addr) === cpu1Word
  val cpuHitData = Mux(store3HitCpu,
    storeWord(cpuLine.data(cpuWord), io.store3_addr, io.store3_data, io.store3_mask),
    cpuLine.data(cpuWord))
  val cpu1HitData = Mux(store3HitCpu1,
    storeWord(cpu1Line.data(cpu1Word), io.store3_addr, io.store3_data, io.store3_mask),
    cpu1Line.data(cpu1Word))

  val hitRespQ = Module(new Queue(new DCacheReadResp, 2, pipe = true, flow = true))
  val hitRespQ1 = Module(new Queue(new DCacheReadResp, 2, pipe = true, flow = true))
  hitRespQ.io.enq.valid := io.cpu.arvalid && cpuHit
  hitRespQ.io.enq.bits.data := cpuHitData
  hitRespQ.io.enq.bits.resp := 0.U
  hitRespQ.io.enq.bits.id := io.cpu.arid
  hitRespQ1.io.enq.valid := io.cpu1.arvalid && cpu1Hit
  hitRespQ1.io.enq.bits.data := cpu1HitData
  hitRespQ1.io.enq.bits.resp := 0.U
  hitRespQ1.io.enq.bits.id := io.cpu1.arid

  val cpuMiss = io.cpu.arvalid && !cpuHit
  val cpu1Miss = io.cpu1.arvalid && !cpu1Hit && cpu1Cacheable
  val chooseCpu0Miss = cpuMiss
  missQueue.io.req.valid := chooseCpu0Miss || (!cpuMiss && cpu1Miss)
  missQueue.io.req.bits.addr := Mux(chooseCpu0Miss, io.cpu.araddr, io.cpu1.araddr)
  missQueue.io.req.bits.size := Mux(chooseCpu0Miss, io.cpu.arsize, io.cpu1.arsize)
  missQueue.io.req.bits.id := Mux(chooseCpu0Miss, io.cpu.arid, io.cpu1.arid)
  missQueue.io.req.bits.port := !chooseCpu0Miss

  io.cpu.arready := Mux(cpuHit, hitRespQ.io.enq.ready, missQueue.io.req.ready)
  io.cpu1.arready := Mux(cpu1Hit, hitRespQ1.io.enq.ready,
    cpu1Cacheable && !cpuMiss && missQueue.io.req.ready)
  val cpuArFire = io.cpu.arvalid && io.cpu.arready
  val cpu1ArFire = io.cpu1.arvalid && io.cpu1.arready

  val missResp0 = missQueue.io.resp0.valid
  val missResp1 = missQueue.io.resp1.valid
  io.cpu.rvalid := missResp0 || hitRespQ.io.deq.valid
  io.cpu.rdata := Mux(missResp0,
    missQueue.io.resp0.bits.data, hitRespQ.io.deq.bits.data)
  io.cpu.rresp := Mux(missResp0,
    missQueue.io.resp0.bits.resp, hitRespQ.io.deq.bits.resp)
  io.cpu.rid := Mux(missResp0,
    missQueue.io.resp0.bits.id, hitRespQ.io.deq.bits.id)
  io.cpu.rlast := true.B
  missQueue.io.resp0.ready := io.cpu.rready && missResp0
  hitRespQ.io.deq.ready := io.cpu.rready && !missResp0

  io.cpu1.rvalid := missResp1 || hitRespQ1.io.deq.valid
  io.cpu1.rdata := Mux(missResp1,
    missQueue.io.resp1.bits.data, hitRespQ1.io.deq.bits.data)
  io.cpu1.rresp := Mux(missResp1,
    missQueue.io.resp1.bits.resp, hitRespQ1.io.deq.bits.resp)
  io.cpu1.rid := Mux(missResp1,
    missQueue.io.resp1.bits.id, hitRespQ1.io.deq.bits.id)
  io.cpu1.rlast := true.B
  missQueue.io.resp1.ready := io.cpu1.rready && missResp1
  hitRespQ1.io.deq.ready := io.cpu1.rready && !missResp1

  io.cpu.awready := false.B
  io.cpu.wready := false.B
  io.cpu.bvalid := false.B
  io.cpu.bresp := 0.U
  io.cpu.bid := 0.U
  io.cpu1.awready := false.B
  io.cpu1.wready := false.B
  io.cpu1.bvalid := false.B
  io.cpu1.bresp := 0.U
  io.cpu1.bid := 0.U

  io.mem <> missQueue.io.mem
  missQueue.io.poisonValid := VecInit(Seq(
    io.invalidate_valid, io.invalidate2_valid, io.invalidate3_valid,
    io.store_valid, io.store2_valid, io.store3_valid))
  missQueue.io.poisonAddr := VecInit(Seq(
    io.invalidate_addr, io.invalidate2_addr, io.invalidate3_addr,
    io.store_addr, io.store2_addr, io.store3_addr))

  val installIndex = indexOf(missQueue.io.installAddr)
  val installTag = tagOf(missQueue.io.installAddr)
  when(missQueue.io.installValid) {
    lines(installIndex).valid := true.B
    lines(installIndex).tag := installTag
    lines(installIndex).data := missQueue.io.installData
  }

  when(io.invalidate_valid && invCacheable &&
      lines(invIndex).valid && lines(invIndex).tag === invTag) {
    lines(invIndex).valid := false.B
  }
  when(io.invalidate2_valid && inv2Cacheable &&
      lines(inv2Index).valid && lines(inv2Index).tag === inv2Tag) {
    lines(inv2Index).valid := false.B
  }
  when(io.invalidate3_valid && inv3Cacheable &&
      lines(inv3Index).valid && lines(inv3Index).tag === inv3Tag) {
    lines(inv3Index).valid := false.B
  }

  val storeCacheable = cacheable(io.store_addr)
  val storeIndex = indexOf(io.store_addr)
  val storeTag = tagOf(io.store_addr)
  val storeWordIdx = wordOf(io.store_addr)
  val store2Cacheable = cacheable(io.store2_addr)
  val store2Index = indexOf(io.store2_addr)
  val store2Tag = tagOf(io.store2_addr)
  val store2WordIdx = wordOf(io.store2_addr)
  val store3Cacheable = cacheable(io.store3_addr)
  val store3Index = indexOf(io.store3_addr)
  val store3Tag = tagOf(io.store3_addr)
  val store3WordIdx = wordOf(io.store3_addr)
  val installSameSet0 = missQueue.io.installValid && installIndex === storeIndex
  val installSameSet1 = missQueue.io.installValid && installIndex === store2Index
  val installSameSet2 = missQueue.io.installValid && installIndex === store3Index
  val storeHit = io.store_valid && storeCacheable && lines(storeIndex).valid &&
    lines(storeIndex).tag === storeTag && !installSameSet0
  val store2Hit = io.store2_valid && store2Cacheable && lines(store2Index).valid &&
    lines(store2Index).tag === store2Tag && !installSameSet1
  val store3Hit = io.store3_valid && store3Cacheable && lines(store3Index).valid &&
    lines(store3Index).tag === store3Tag && !installSameSet2
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

  io.busy := missQueue.io.busy || hitRespQ.io.deq.valid || hitRespQ1.io.deq.valid

  if (conf.statistics) {
    val accessCount = PopCount(Seq(
      cpuArFire && cpuCacheable,
      cpu1ArFire && cpu1Cacheable))
    val hitCount = PopCount(Seq(cpuArFire && cpuHit, cpu1ArFire && cpu1Hit))
    val missCount = PopCount(Seq(
      cpuArFire && cpuCacheable && !cpuHit,
      cpu1ArFire && cpu1Cacheable && !cpu1Hit))
    PM(conf, clock, EVENT_DCACHE_ACCESS, accessCount, accessCount =/= 0.U)
    PM(conf, clock, EVENT_DCACHE_HIT, hitCount, hitCount =/= 0.U)
    PM(conf, clock, EVENT_DCACHE_MISS, missCount, missCount =/= 0.U)
    PM(conf, clock, EVENT_DCACHE_BYPASS, 1.U,
      cpuArFire && !cpuCacheable)
    PM(conf, clock, EVENT_DCACHE_MSHR_ALLOC, 1.U, missQueue.io.allocPulse)
    val hitUnderMissCount = PopCount(Seq(
      cpuArFire && cpuHit && missQueue.io.busy,
      cpu1ArFire && cpu1Hit && missQueue.io.busy))
    PM(conf, clock, EVENT_DCACHE_HIT_UNDER_MISS, hitUnderMissCount,
      hitUnderMissCount =/= 0.U)
    PM(conf, clock, EVENT_DCACHE_MSHR_REFILL, 1.U, missQueue.io.refillPulse)
    PM(conf, clock, EVENT_DCACHE_STORE_HIT,
      PopCount(Seq(storeHit, store2Hit)), storeHit || store2Hit)
    PM(conf, clock, EVENT_DCACHE_SECONDARY_ALLOC, 1.U,
      missQueue.io.secondaryPulse)
    PM(conf, clock, EVENT_DCACHE_MSHR_MERGE, 1.U, missQueue.io.mergePulse)
  }
}
