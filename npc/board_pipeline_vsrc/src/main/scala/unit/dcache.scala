package unit

import chisel3._
import chisel3.util._
import bus._

class DCacheIO extends Bundle {
  val cpu = new AXI4Slave
  val mem = new AXI4Master
  val earlyLoad = Flipped(Decoupled(new DCacheEarlyLoadReq))
  val earlyLoadResp = Decoupled(new DCacheEarlyLoadResp)
  val earlyLoadCancel = Input(Bool())
}

class DCacheEarlyLoadReq extends Bundle {
  val addr = UInt(32.W)
  val id = UInt(4.W)
  val size = UInt(3.W)
}

class DCacheEarlyLoadResp extends Bundle {
  val data = UInt(32.W)
  val resp = UInt(2.W)
  val replay = Bool()
}

class DCache(
    val set: Int = 64,
    val way: Int = 2,
    val blockSize: Int = 32,
    val cacheableBase: BigInt = 0x80000000L,
    val cacheableMask: BigInt = 0xf0000000L)
    extends Module {

  require(isPow2(set) && isPow2(way) && isPow2(blockSize))
  require(way == 2, "The compact replacement policy currently requires two ways")
  require(blockSize >= 4)

  val io = IO(new DCacheIO)

  private val offsetBits = log2Ceil(blockSize)
  private val indexBits = log2Ceil(set)
  private val words = blockSize / 4
  private val wordBits = log2Ceil(words)
  private val dataIndexBits = indexBits + wordBits
  private val tagBits = 32 - offsetBits - indexBits

  val dataMem = Seq.fill(way)(SyncReadMem(set * words, UInt(32.W)))
  // As with ICache, these shallow tags are explicitly asynchronous so Vivado
  // maps them to LUTRAM instead of consuming a RAMB18 per way.
  val tagMem = Seq.fill(way)(Mem(set, UInt(tagBits.W)))
  // Dynamic writes to a resettable Vec decode into every flip-flop D input and
  // dominate FPGA timing. Sweep these shallow tables after reset so both map
  // to compact single-write LUTRAMs with asynchronous lookup.
  val validBits = Mem(set, UInt(way.W))
  val replacement = Mem(set, UInt(1.W))
  val initActive = RegInit(true.B)
  val initIndex = RegInit(0.U(indexBits.W))
  val deferredValidWrite = RegInit(false.B)
  val deferredValidIndex = Reg(UInt(indexBits.W))
  val deferredValidData = Reg(UInt(way.W))

  val sIdle :: sWriteData :: sLookup :: sRefillAr :: sRefillR :: sReadResp :: sWriteSend :: sWriteResp :: sBypassReadAr :: sBypassReadR :: Nil = Enum(10)
  val state = RegInit(sIdle)

  val reqAddr = Reg(UInt(32.W))
  val reqId = Reg(UInt(4.W))
  val reqSize = Reg(UInt(3.W))
  val reqLen = Reg(UInt(8.W))
  val reqBurst = Reg(UInt(2.W))
  val reqIsWrite = RegInit(false.B)
  val reqCacheable = RegInit(false.B)
  val reqWdata = Reg(UInt(32.W))
  val reqWstrb = Reg(UInt(4.W))
  val reqWlast = Reg(Bool())
  val reqWasEarly = RegInit(false.B)

  val refillWord = RegInit(0.U(wordBits.W))
  val missWay = Reg(UInt(1.W))
  val respData = Reg(UInt(32.W))
  val respCode = RegInit(0.U(2.W))
  val writeAwDone = RegInit(false.B)
  val writeWDone = RegInit(false.B)
  val storeCached = RegInit(false.B)
  val storeWay = Reg(UInt(1.W))
  val postedCpuDone = RegInit(false.B)
  val postedMemDone = RegInit(false.B)
  val postedRespCode = RegInit(0.U(2.W))
  val pendingStoreValid = RegInit(false.B)
  val pendingStoreCached = RegInit(false.B)
  val pendingStoreLine = Reg(UInt((32 - offsetBits).W))
  val pendingStoreIndex = Reg(UInt(indexBits.W))
  val pendingStoreWay = Reg(UInt(1.W))
  // Register downstream write responses at the cache boundary. Besides being
  // a protocol skid buffer, this prevents an AXI slave's combinational B path
  // from feeding cache metadata and global pipeline control in the same cycle.
  val memBValid = RegInit(false.B)
  val memBResp = Reg(UInt(2.W))
  val memBId = Reg(UInt(4.W))
  // The private load response is a registered cache-boundary channel.  Cache
  // tags and hit logic only drive these registers; LSU and global pipeline
  // control consume them in the following cycle, keeping that control cone
  // out of the DCache lookup timing path.
  val earlyRespValid = RegInit(false.B)
  val earlyRespData = Reg(UInt(32.W))
  val earlyRespCode = Reg(UInt(2.W))
  val earlyRespReplay = RegInit(false.B)
  val earlyReqCanceled = RegInit(false.B)

  def isCacheable(addr: UInt): Bool =
    (addr & cacheableMask.U(32.W)) === (cacheableBase & cacheableMask).U(32.W)

  def mergeBytes(oldData: UInt, newData: UInt, strb: UInt): UInt =
    Cat((3 to 0 by -1).map { i =>
      Mux(strb(i), newData(8 * i + 7, 8 * i), oldData(8 * i + 7, 8 * i))
    })

  io.cpu.setDefaults()
  io.mem.setDefaults()
  io.earlyLoadResp.valid := earlyRespValid
  io.earlyLoadResp.bits.data := earlyRespData
  io.earlyLoadResp.bits.resp := earlyRespCode
  io.earlyLoadResp.bits.replay := earlyRespReplay
  when(io.earlyLoadResp.fire) {
    earlyRespValid := false.B
  }
  val memBConsume = WireDefault(false.B)
  io.mem.bready := !memBValid
  when(io.mem.bvalid && io.mem.bready) {
    memBValid := true.B
    memBResp := io.mem.bresp
    memBId := io.mem.bid
  }
  when(memBConsume) {
    memBValid := false.B
  }
  val cacheOperational = !initActive && !deferredValidWrite
  val pendingStoreResponse = pendingStoreValid && memBValid
  val pendingStoreBusy = pendingStoreValid && !pendingStoreResponse
  // Ready describes queue capacity only. Address-dependent cacheability and
  // ordering checks happen from the registered request in sLookup, avoiding
  // an address -> ready -> FSM control path in the issue cycle.
  io.earlyLoad.ready := cacheOperational &&
    state === sIdle && !io.cpu.arvalid && !io.cpu.awvalid &&
    !pendingStoreBusy &&
    (!earlyRespValid || io.earlyLoadResp.ready)

  // A posted store's real B response has an owner independent of the cache
  // lookup FSM. This lets unrelated cacheable loads proceed while preserving
  // same-line and MMIO ordering below.
  val pendingStoreError = pendingStoreResponse && memBResp.orR && pendingStoreCached
  when(pendingStoreResponse) {
    pendingStoreValid := false.B
    memBConsume := true.B
  }

  val cpuReadFire = io.cpu.arvalid && io.cpu.arready
  val cpuWriteAddrFire = io.cpu.awvalid && io.cpu.awready
  val cpuWriteDataFire = io.cpu.wvalid && io.cpu.wready
  val earlyLoadFire = io.earlyLoad.valid && io.earlyLoad.ready
  val pairedStoreRequest = cacheOperational && state === sIdle && !pendingStoreBusy &&
    !io.cpu.arvalid &&
    io.cpu.awvalid && io.cpu.wvalid
  val immediatePostedStore = pairedStoreRequest &&
    isCacheable(io.cpu.awaddr) && io.cpu.awlen === 0.U
  val immediateCacheableWrite = state === sIdle && cpuWriteAddrFire &&
    isCacheable(io.cpu.awaddr) && io.cpu.awlen === 0.U
  val cacheReadLaunch =
    (cpuReadFire && isCacheable(io.cpu.araddr) && io.cpu.arlen === 0.U) ||
      (cpuWriteDataFire && Mux(state === sIdle, immediateCacheableWrite, reqCacheable)) ||
      earlyLoadFire
  val cacheReadAddr = Mux(cpuReadFire, io.cpu.araddr,
    Mux(cpuWriteDataFire, Mux(state === sIdle, io.cpu.awaddr, reqAddr),
      io.earlyLoad.bits.addr))
  val cacheReadIndex = cacheReadAddr(offsetBits + indexBits - 1, offsetBits)
  val cacheReadWord = cacheReadAddr(offsetBits - 1, 2)
  val cacheDataIndex = Cat(cacheReadIndex, cacheReadWord)
  val tagReadIndex = RegEnable(cacheReadIndex, 0.U(indexBits.W), cacheReadLaunch)
  // Snapshot validity beside the synchronous data-array request. Reading the
  // 64-entry register vector again in sLookup built a dynamic mux directly on
  // the load-hit forwarding path. Fold a concurrent posted-store error
  // into the snapshot because it can independently invalidate one cached way.
  val launchStoreInvalidate = pendingStoreError && pendingStoreIndex === cacheReadIndex
  val launchValid = validBits(cacheReadIndex) & ~Mux(launchStoreInvalidate,
    UIntToOH(pendingStoreWay, way), 0.U(way.W))
  val lookupValid = RegEnable(launchValid, 0.U(way.W), cacheReadLaunch)

  val lookupData = Wire(Vec(way, UInt(32.W)))
  val lookupTag = Wire(Vec(way, UInt(tagBits.W)))
  for (i <- 0 until way) {
    lookupData(i) := dataMem(i).read(cacheDataIndex, cacheReadLaunch)
    lookupTag(i) := tagMem(i)(tagReadIndex)
  }

  val reqIndex = reqAddr(offsetBits + indexBits - 1, offsetBits)
  val reqTag = reqAddr(31, offsetBits + indexBits)
  val reqWord = reqAddr(offsetBits - 1, 2)
  val reqLineBase = reqAddr & (~(blockSize - 1).U(32.W))
  val wayHits = Wire(Vec(way, Bool()))
  for (i <- 0 until way) {
    wayHits(i) := lookupValid(i) && lookupTag(i) === reqTag
  }
  val hit = wayHits.asUInt.orR
  // This cache is fixed at two ways. A direct two-input select avoids the
  // one-hot OR tree on the load-hit-to-forward critical path.
  val hitWay = wayHits(1).asUInt
  val hitData = Mux(wayHits(1), lookupData(1), lookupData(0))
  val invalidMask = ~lookupValid
  val selectedWay = Mux(lookupValid.andR,
    replacement(reqIndex), PriorityEncoder(invalidMask))

  val dataWriteValid = WireDefault(false.B)
  val dataWriteWay = WireDefault(0.U(1.W))
  val dataWriteIndex = WireDefault(0.U(dataIndexBits.W))
  val dataWriteData = WireDefault(0.U(32.W))
  val tagWriteValid = WireDefault(false.B)
  val tagWriteWay = WireDefault(0.U(1.W))
  val tagWriteIndex = WireDefault(0.U(indexBits.W))
  val tagWriteData = WireDefault(0.U(tagBits.W))
  val validSet = WireDefault(false.B)
  val validSetIndex = WireDefault(0.U(indexBits.W))
  val validSetWay = WireDefault(0.U(1.W))
  val validClear = WireDefault(false.B)
  val validClearIndex = WireDefault(0.U(indexBits.W))
  val validClearWay = WireDefault(0.U(1.W))
  val replacementWrite = WireDefault(false.B)
  val replacementWriteIndex = WireDefault(0.U(indexBits.W))
  val replacementWriteData = WireDefault(0.U(1.W))

  switch(state) {
    is(sIdle) {
      val cpuPendingHazard = pendingStoreBusy &&
        (!isCacheable(io.cpu.araddr) ||
          (!pendingStoreCached &&
            io.cpu.araddr(31, offsetBits) === pendingStoreLine))
      // The core issues one operation at a time. Read wins only when both AXI
      // request classes are presented together by an external test master.
      io.cpu.arready := cacheOperational && !io.cpu.awvalid && !cpuPendingHazard
      io.cpu.awready := cacheOperational && !io.cpu.arvalid && !pendingStoreBusy
      io.cpu.wready := pairedStoreRequest

      // One cacheable write may occupy the cache while its write-through is
      // still completing. Once both CPU request channels are accepted, retire
      // the store immediately; the cache remains busy until the real memory B
      // response arrives, so younger memory operations cannot pass it.
      io.cpu.bvalid := immediatePostedStore
      io.cpu.bresp := 0.U
      io.cpu.bid := io.cpu.awid
      val immediateCpuResponseFire = io.cpu.bvalid && io.cpu.bready

      // The LSU presents AW and W together. Forward that common case directly
      // while the cache lookup runs for a possible cached-copy update.
      io.mem.awaddr := io.cpu.awaddr
      io.mem.awvalid := cacheOperational && !pendingStoreBusy &&
        !io.cpu.arvalid && io.cpu.awvalid
      io.mem.awid := io.cpu.awid
      io.mem.awlen := io.cpu.awlen
      io.mem.awsize := io.cpu.awsize
      io.mem.awburst := io.cpu.awburst
      io.mem.wdata := io.cpu.wdata
      io.mem.wstrb := io.cpu.wstrb
      io.mem.wvalid := pairedStoreRequest
      io.mem.wlast := io.cpu.wlast
      val awForwarded = io.mem.awvalid && io.mem.awready
      val wForwarded = io.mem.wvalid && io.mem.wready

      when(cpuReadFire) {
        reqAddr := io.cpu.araddr
        reqId := io.cpu.arid
        reqSize := io.cpu.arsize
        reqLen := io.cpu.arlen
        reqBurst := io.cpu.arburst
        reqIsWrite := false.B
        reqCacheable := isCacheable(io.cpu.araddr) && io.cpu.arlen === 0.U
        reqWasEarly := false.B
        respCode := 0.U
        state := Mux(isCacheable(io.cpu.araddr) && io.cpu.arlen === 0.U,
          sLookup, sBypassReadAr)
      }.elsewhen(io.cpu.awvalid && io.cpu.awready) {
        reqAddr := io.cpu.awaddr
        reqId := io.cpu.awid
        reqSize := io.cpu.awsize
        reqLen := io.cpu.awlen
        reqBurst := io.cpu.awburst
        reqIsWrite := true.B
        reqCacheable := isCacheable(io.cpu.awaddr) && io.cpu.awlen === 0.U
        reqWasEarly := false.B
        storeCached := false.B
        postedCpuDone := immediateCpuResponseFire
        postedMemDone := false.B
        postedRespCode := 0.U
        respCode := 0.U
        writeAwDone := awForwarded
        writeWDone := wForwarded
        when(cpuWriteDataFire) {
          reqWdata := io.cpu.wdata
          reqWstrb := io.cpu.wstrb
          reqWlast := io.cpu.wlast
          state := Mux(isCacheable(io.cpu.awaddr) && io.cpu.awlen === 0.U,
            sLookup, Mux(awForwarded && wForwarded, sWriteResp, sWriteSend))
        }.otherwise {
          state := sWriteData
        }
      }.elsewhen(earlyLoadFire) {
        reqAddr := io.earlyLoad.bits.addr
        reqId := io.earlyLoad.bits.id
        reqSize := io.earlyLoad.bits.size
        reqLen := 0.U
        reqBurst := 1.U
        reqIsWrite := false.B
        reqCacheable := isCacheable(io.earlyLoad.bits.addr)
        reqWasEarly := true.B
        earlyReqCanceled := false.B
        respCode := 0.U
        state := sLookup
      }
    }

    is(sWriteData) {
      io.cpu.wready := true.B
      io.mem.awaddr := reqAddr
      io.mem.awvalid := !writeAwDone
      io.mem.awid := reqId
      io.mem.awlen := reqLen
      io.mem.awsize := reqSize
      io.mem.awburst := reqBurst
      io.mem.wdata := io.cpu.wdata
      io.mem.wstrb := io.cpu.wstrb
      io.mem.wvalid := io.cpu.wvalid
      io.mem.wlast := io.cpu.wlast
      val awComplete = writeAwDone || (io.mem.awvalid && io.mem.awready)
      val wForwarded = io.mem.wvalid && io.mem.wready
      when(io.mem.awvalid && io.mem.awready) {
        writeAwDone := true.B
      }
      when(cpuWriteDataFire) {
        reqWdata := io.cpu.wdata
        reqWstrb := io.cpu.wstrb
        reqWlast := io.cpu.wlast
        respCode := 0.U
        writeWDone := wForwarded
        state := Mux(reqCacheable, sLookup,
          Mux(awComplete && wForwarded, sWriteResp, sWriteSend))
      }
    }

    is(sLookup) {
      // A store lookup and its write-through request proceed in parallel. The
      // lookup only decides whether the cached copy also needs a masked merge.
      io.mem.awaddr := reqAddr
      io.mem.awvalid := reqIsWrite && !writeAwDone
      io.mem.awid := reqId
      io.mem.awlen := reqLen
      io.mem.awsize := reqSize
      io.mem.awburst := reqBurst
      io.mem.wdata := reqWdata
      io.mem.wstrb := reqWstrb
      io.mem.wvalid := reqIsWrite && !writeWDone
      io.mem.wlast := reqWlast

      val awComplete = writeAwDone || (io.mem.awvalid && io.mem.awready)
      val wComplete = writeWDone || (io.mem.wvalid && io.mem.wready)
      val canPostWrite = reqIsWrite && reqCacheable && awComplete && wComplete
      io.cpu.bvalid := canPostWrite && !postedCpuDone
      io.cpu.bresp := Mux(postedMemDone, postedRespCode,
        Mux(memBValid, memBResp, 0.U))
      io.cpu.bid := reqId
      val cpuResponseFire = io.cpu.bvalid && io.cpu.bready
      val memResponseFire = canPostWrite && !postedMemDone && memBValid
      val cpuResponseDone = postedCpuDone || cpuResponseFire
      val memResponseDone = postedMemDone || memResponseFire
      when(io.mem.awvalid && io.mem.awready) {
        writeAwDone := true.B
      }
      when(io.mem.wvalid && io.mem.wready) {
        writeWDone := true.B
      }
      when(cpuResponseFire) {
        postedCpuDone := true.B
      }
      when(memResponseFire) {
        memBConsume := true.B
        postedMemDone := true.B
        postedRespCode := memBResp
      }

      when(reqWasEarly && !reqCacheable) {
        // Speculative MMIO is never sent downstream. Tell the LSU to replay
        // this load through its ordinary ordered AXI path.
        earlyRespValid := true.B
        earlyRespData := 0.U
        earlyRespCode := 0.U
        earlyRespReplay := true.B
        state := sIdle
      }.elsewhen(hit) {
        replacementWrite := true.B
        replacementWriteIndex := reqIndex
        replacementWriteData := ~hitWay(0)
        when(reqIsWrite) {
          dataWriteValid := true.B
          dataWriteWay := hitWay
          dataWriteIndex := Cat(reqIndex, reqWord)
          dataWriteData := mergeBytes(hitData, reqWdata, reqWstrb)
          storeCached := true.B
          storeWay := hitWay
          when(memResponseFire && memBResp.orR) {
            validClear := true.B
            validClearIndex := reqIndex
            validClearWay := hitWay
          }
          when(canPostWrite && cpuResponseDone && !memResponseDone) {
            pendingStoreValid := true.B
            pendingStoreCached := true.B
            pendingStoreLine := reqAddr(31, offsetBits)
            pendingStoreIndex := reqIndex
            pendingStoreWay := hitWay
            state := sIdle
          }.otherwise {
            state := Mux(canPostWrite,
              Mux(cpuResponseDone && memResponseDone, sIdle, sWriteResp), sWriteSend)
          }
        }.otherwise {
          // Register early hits before exposing them to the LSU.  The response
          // register retains Decoupled ownership until the LSU accepts it.
          when(reqWasEarly) {
            earlyRespValid := true.B
            earlyRespData := Mux(earlyReqCanceled || io.earlyLoadCancel, 0.U, hitData)
            earlyRespCode := 0.U
            earlyRespReplay := earlyReqCanceled || io.earlyLoadCancel
            state := sIdle
          }.otherwise {
            io.cpu.rvalid := true.B
            io.cpu.rdata := hitData
            io.cpu.rresp := 0.U
            io.cpu.rid := reqId
            io.cpu.rlast := true.B
            when(io.cpu.rready) {
              state := sIdle
            }.otherwise {
              respData := hitData
              respCode := 0.U
              state := sReadResp
            }
          }
        }
      }.otherwise {
        when(reqIsWrite) {
          // Write-through/no-write-allocate avoids fetching an entire line for
          // a store that may not be read again. Store hits still update the
          // cached copy above so later loads remain coherent.
          storeCached := false.B
          when(canPostWrite && cpuResponseDone && !memResponseDone) {
            pendingStoreValid := true.B
            pendingStoreCached := false.B
            pendingStoreLine := reqAddr(31, offsetBits)
            pendingStoreIndex := reqIndex
            pendingStoreWay := 0.U
            state := sIdle
          }.otherwise {
            state := Mux(canPostWrite,
              Mux(cpuResponseDone && memResponseDone, sIdle, sWriteResp), sWriteSend)
          }
        }.otherwise {
          missWay := selectedWay
          refillWord := 0.U
          respCode := 0.U
          state := sRefillAr
        }
      }
    }

    is(sRefillAr) {
      io.mem.araddr := reqLineBase + (refillWord << 2)
      io.mem.arvalid := true.B
      io.mem.arid := 0.U
      io.mem.arlen := 0.U
      io.mem.arsize := 2.U
      io.mem.arburst := 1.U
      when(io.mem.arready) {
        state := sRefillR
      }
    }

    is(sRefillR) {
      io.mem.rready := true.B
      when(io.mem.rvalid) {
        val refillResp = Mux(respCode.orR, respCode, io.mem.rresp)
        val isRequestedWord = refillWord === reqWord

        dataWriteValid := true.B
        dataWriteWay := missWay
        dataWriteIndex := Cat(reqIndex, refillWord)
        dataWriteData := io.mem.rdata
        respCode := refillResp
        when(isRequestedWord) {
          respData := io.mem.rdata
        }

        when(refillWord === (words - 1).U) {
          when(!refillResp.orR) {
            tagWriteValid := true.B
            tagWriteWay := missWay
            tagWriteIndex := reqIndex
            tagWriteData := reqTag
            validSet := true.B
            validSetIndex := reqIndex
            validSetWay := missWay
            replacementWrite := true.B
            replacementWriteIndex := reqIndex
            replacementWriteData := ~missWay(0)
          }
          when(reqWasEarly) {
            earlyRespValid := true.B
            earlyRespData := Mux(earlyReqCanceled || io.earlyLoadCancel, 0.U,
              Mux(isRequestedWord, io.mem.rdata, respData))
            earlyRespCode := Mux(earlyReqCanceled || io.earlyLoadCancel, 0.U, refillResp)
            earlyRespReplay := earlyReqCanceled || io.earlyLoadCancel
            state := sIdle
          }.otherwise {
            state := sReadResp
          }
        }.otherwise {
          refillWord := refillWord + 1.U
          state := sRefillAr
        }
      }
    }

    is(sReadResp) {
      when(reqWasEarly) {
        when(earlyReqCanceled || io.earlyLoadCancel) {
          when(!earlyRespValid || io.earlyLoadResp.ready) {
            earlyRespValid := true.B
            earlyRespData := 0.U
            earlyRespCode := 0.U
            earlyRespReplay := true.B
            state := sIdle
          }
        }.elsewhen(!earlyRespValid || io.earlyLoadResp.ready) {
          earlyRespValid := true.B
          earlyRespData := respData
          earlyRespCode := respCode
          earlyRespReplay := false.B
          state := sIdle
        }
      }.otherwise {
        io.cpu.rvalid := true.B
        io.cpu.rdata := respData
        io.cpu.rresp := respCode
        io.cpu.rid := reqId
        io.cpu.rlast := true.B
        when(io.cpu.rready) {
          state := sIdle
        }
      }
    }

    is(sWriteSend) {
      io.mem.awaddr := reqAddr
      io.mem.awvalid := !writeAwDone
      io.mem.awid := reqId
      io.mem.awlen := reqLen
      io.mem.awsize := reqSize
      io.mem.awburst := reqBurst
      io.mem.wdata := reqWdata
      io.mem.wstrb := reqWstrb
      io.mem.wvalid := !writeWDone
      io.mem.wlast := reqWlast

      val awComplete = writeAwDone || (io.mem.awvalid && io.mem.awready)
      val wComplete = writeWDone || (io.mem.wvalid && io.mem.wready)
      when(io.mem.awvalid && io.mem.awready) {
        writeAwDone := true.B
      }
      when(io.mem.wvalid && io.mem.wready) {
        writeWDone := true.B
      }
      when(awComplete && wComplete) {
        when(reqCacheable) {
          io.cpu.bvalid := !postedCpuDone
          io.cpu.bresp := Mux(postedMemDone, postedRespCode,
            Mux(memBValid, memBResp, 0.U))
          io.cpu.bid := reqId
          val cpuResponseFire = io.cpu.bvalid && io.cpu.bready
          val memResponseFire = !postedMemDone && memBValid
          val cpuResponseDone = postedCpuDone || cpuResponseFire
          val memResponseDone = postedMemDone || memResponseFire
          when(cpuResponseFire) {
            postedCpuDone := true.B
          }
          when(memResponseFire) {
            memBConsume := true.B
            postedMemDone := true.B
            postedRespCode := memBResp
            when(memBResp.orR && storeCached) {
              validClear := true.B
              validClearIndex := reqIndex
              validClearWay := storeWay
            }
          }
          when(cpuResponseDone && !memResponseDone) {
            pendingStoreValid := true.B
            pendingStoreCached := storeCached
            pendingStoreLine := reqAddr(31, offsetBits)
            pendingStoreIndex := reqIndex
            pendingStoreWay := storeWay
            state := sIdle
          }.otherwise {
            state := Mux(cpuResponseDone && memResponseDone, sIdle, sWriteResp)
          }
        }.otherwise {
          state := sWriteResp
        }
      }
    }

    is(sWriteResp) {
      when(reqCacheable) {
        io.cpu.bvalid := !postedCpuDone
        io.cpu.bresp := Mux(postedMemDone, postedRespCode,
          Mux(memBValid, memBResp, 0.U))
        io.cpu.bid := reqId
        val cpuResponseFire = io.cpu.bvalid && io.cpu.bready
        val memResponseFire = !postedMemDone && memBValid
        val cpuResponseDone = postedCpuDone || cpuResponseFire
        val memResponseDone = postedMemDone || memResponseFire
        when(cpuResponseFire) {
          postedCpuDone := true.B
        }
        when(memResponseFire) {
          memBConsume := true.B
          postedMemDone := true.B
          postedRespCode := memBResp
          when(memBResp.orR && storeCached) {
            validClear := true.B
            validClearIndex := reqIndex
            validClearWay := storeWay
          }
        }
        when(cpuResponseDone && !memResponseDone) {
          pendingStoreValid := true.B
          pendingStoreCached := storeCached
          pendingStoreLine := reqAddr(31, offsetBits)
          pendingStoreIndex := reqIndex
          pendingStoreWay := storeWay
          state := sIdle
        }.elsewhen(cpuResponseDone && memResponseDone) {
          state := sIdle
        }
      }.otherwise {
        io.cpu.bvalid := memBValid
        io.cpu.bresp := memBResp
        io.cpu.bid := memBId
        when(memBValid && io.cpu.bready) {
          memBConsume := true.B
          state := sIdle
        }
      }
    }

    is(sBypassReadAr) {
      io.mem.araddr := reqAddr
      io.mem.arvalid := true.B
      io.mem.arid := reqId
      io.mem.arlen := reqLen
      io.mem.arsize := reqSize
      io.mem.arburst := reqBurst
      when(io.mem.arready) {
        state := sBypassReadR
      }
    }

    is(sBypassReadR) {
      io.cpu.rvalid := io.mem.rvalid
      io.cpu.rdata := io.mem.rdata
      io.cpu.rresp := io.mem.rresp
      io.cpu.rid := io.mem.rid
      io.cpu.rlast := Mux(reqLen === 0.U, true.B, io.mem.rlast)
      io.mem.rready := io.cpu.rready
      when(io.mem.rvalid && io.cpu.rready && (reqLen === 0.U || io.mem.rlast)) {
        state := sIdle
      }
    }
  }

  // A killed speculative preissue may already own a refill transaction. Keep
  // draining that transaction, but suppress its private response so a later
  // load cannot consume data belonging to the flushed instruction.
  when(io.earlyLoadCancel && state === sIdle) {
    earlyRespValid := false.B
  }
  when(io.earlyLoadCancel && reqWasEarly && state =/= sIdle) {
    earlyReqCanceled := true.B
  }

  val validWriteEnable = WireDefault(false.B)
  val validWriteIndex = WireDefault(0.U(indexBits.W))
  val validWriteData = WireDefault(0.U(way.W))
  when(initActive) {
    validWriteEnable := true.B
    validWriteIndex := initIndex
    validWriteData := 0.U
    when(initIndex === (set - 1).U) {
      initIndex := 0.U
      initActive := false.B
    }.otherwise {
      initIndex := initIndex + 1.U
    }
  }.elsewhen(deferredValidWrite) {
    validWriteEnable := true.B
    validWriteIndex := deferredValidIndex
    validWriteData := deferredValidData
    deferredValidWrite := false.B
  }.elsewhen(pendingStoreError) {
    validWriteEnable := true.B
    validWriteIndex := pendingStoreIndex
    validWriteData := validBits(pendingStoreIndex) & ~UIntToOH(pendingStoreWay, way)
    // A posted-store error can return with the last beat of an unrelated
    // refill. Preserve both set updates while retaining one physical write
    // port; cache request acceptance pauses for the deferred install cycle.
    when(validSet) {
      val pendingMask = Mux(validSetIndex === pendingStoreIndex,
        UIntToOH(pendingStoreWay, way), 0.U(way.W))
      deferredValidWrite := true.B
      deferredValidIndex := validSetIndex
      deferredValidData := (validBits(validSetIndex) & ~pendingMask) |
        UIntToOH(validSetWay, way)
    }
  }.elsewhen(validClear) {
    validWriteEnable := true.B
    validWriteIndex := validClearIndex
    validWriteData := validBits(validClearIndex) & ~UIntToOH(validClearWay, way)
  }.elsewhen(validSet) {
    validWriteEnable := true.B
    validWriteIndex := validSetIndex
    validWriteData := validBits(validSetIndex) | UIntToOH(validSetWay, way)
  }

  when(validWriteEnable) {
    validBits.write(validWriteIndex, validWriteData)
  }
  val replacementWriteEnable = initActive || replacementWrite
  val replacementPhysicalWriteIndex = Mux(initActive, initIndex, replacementWriteIndex)
  val replacementPhysicalWriteData = Mux(initActive, 0.U, replacementWriteData)
  when(replacementWriteEnable) {
    replacement.write(replacementPhysicalWriteIndex, replacementPhysicalWriteData)
  }

  for (i <- 0 until way) {
    when(dataWriteValid && dataWriteWay === i.U) {
      dataMem(i).write(dataWriteIndex, dataWriteData)
    }
    when(tagWriteValid && tagWriteWay === i.U) {
      tagMem(i).write(tagWriteIndex, tagWriteData)
    }
  }
}
