package unit

import chisel3._
import chisel3.util._
import bus._
import core._
import core.PerfEvents._

class ICache_IO extends Bundle {
  val in = Flipped(new IFU_ICACHE_IO)
  val out = new AXI4Master
  val fencei = Flipped(Irrevocable(new IFU_signals))
}

class ICache(
    val set: Int,
    val way: Int,
    val block_size: Int,
    val conf: CoreConfig)
    extends Module {

  require(isPow2(set) && isPow2(way) && isPow2(block_size))
  require(block_size >= 4)

  val io = IO(new ICache_IO)

  private val offsetBits = log2Ceil(block_size)
  private val indexBits = log2Ceil(set)
  private val wayBits = log2Ceil(way).max(1)
  private val words = block_size / 4
  private val wordBits = log2Ceil(words).max(1)
  private val dataIndexBits = indexBits + wordBits
  private val tagBits = 32 - offsetBits - indexBits
  private val wordAddrBits = 30

  val dataMem = Seq.fill(way)(SyncReadMem(set * words, UInt(32.W)))
  // A 64x21 tag array is too shallow for a 7-series BRAM. Keep an explicit
  // registered address in front of asynchronous Mem so Vivado consistently
  // implements the four tags as distributed RAM instead of RAMB18 blocks.
  val tagMem = Seq.fill(way)(Mem(set, UInt(tagBits.W)))
  // Prefetch decides its way when the synchronous data read is launched. A
  // valid-tag shadow avoids putting tag comparison on the following cycle's
  // BRAM-data-to-buffer path.
  val prefetchTagMem = Seq.fill(way)(Mem(set, UInt((tagBits + 1).W)))
  val prefetchTagInit = RegInit(true.B)
  val prefetchTagInitIndex = RegInit(0.U(indexBits.W))
  val validBits = Mem(set, UInt(way.W))
  // Do not feed the refill-cycle valid-vector read directly into the RAM
  // write port.  Registering the install transaction removes that dependency
  // from the tag-read/valid-write critical path without changing hit lookup.
  val validInstallPending = RegInit(false.B)
  val validInstallIndex = RegInit(0.U(indexBits.W))
  val validInstallData = RegInit(0.U(way.W))
  val replacement = Mem(set, UInt(wayBits.W))

  val sIdle :: sLookup :: sMissAr :: sMissR :: sResp :: sFence :: Nil = Enum(6)
  val state = RegInit(sIdle)

  val reqAddr = Reg(UInt(32.W))
  val reqBurst = RegInit(false.B)
  val missWay = Reg(UInt(wayBits.W))
  val refillWord = RegInit(0.U(wordBits.W))
  val refillData = Reg(Vec(words, UInt(32.W)))
  val respData = Reg(UInt(32.W))
  val respCode = RegInit(0.U(2.W))
  val fenceIndex = RegInit(0.U(indexBits.W))

  // Each way is a narrow word RAM so an 8 KiB cache does not waste sixteen
  // BRAM36 blocks on four 256-bit-wide line reads. RAM responses always land
  // in a register before reaching IFU; two word buffers keep sequential fetch
  // at one instruction per cycle by reading the first unbuffered word ahead.
  val wordBufferValid = RegInit(false.B)
  val wordBufferAddr = Reg(UInt(wordAddrBits.W))
  val wordBufferData = Reg(UInt(32.W))
  val nextBufferValid = RegInit(false.B)
  val nextBufferAddr = Reg(UInt(wordAddrBits.W))
  val nextBufferData = Reg(UInt(32.W))
  val prefetchPending = RegInit(false.B)
  val prefetchAddr = RegInit(0.U(wordAddrBits.W))
  // Keep the asynchronous L0 within two 64-deep LUTRAM banks on 7-series.
  // A 256-entry table saves misses but adds another bank-select level to the
  // same-cycle fetch path and cannot close the 100 MHz board contract.
  val fetchWordCache = Module(new FetchWordCache(128))
  val l0FillValid = RegInit(false.B)
  val l0FillAddr = Reg(UInt(wordAddrBits.W))
  val l0FillData = Reg(UInt(32.W))
  val l1HitFillPending = RegInit(false.B)

  val requestWordAddr = io.in.araddr(31, 2)
  fetchWordCache.io.lookupAddr := requestWordAddr
  fetchWordCache.io.fillValid := l0FillValid
  fetchWordCache.io.fillAddr := l0FillAddr
  fetchWordCache.io.fillData := l0FillData
  fetchWordCache.io.invalidate := io.fencei.valid && io.fencei.bits.is_fencei
  l0FillValid := false.B
  val requestIndex = io.in.araddr(offsetBits + indexBits - 1, offsetBits)
  val requestWord = io.in.araddr(offsetBits - 1, 2)
  val requestDataIndex = Cat(requestIndex, requestWord)
  val requestFire = Wire(Bool())
  val prefetchFire = Wire(Bool())

  val prefetchBaseAddr = Mux(state === sLookup, reqAddr(31, 2), requestWordAddr)
  val immediateNextAddr = (prefetchBaseAddr + 1.U)(wordAddrBits - 1, 0)
  val lastRefillBeat = refillWord === (words - 1).U
  val currentRespCode = Mux(respCode.orR, respCode, io.out.rresp)
  // Always look one word ahead. Skipping over an already buffered word added
  // a second wide address-selection cone on the BRAM read-address path; an
  // occasional duplicate local read is cheaper and keeps sequential fetch at
  // one instruction per cycle through the existing buffers.
  val prefetchLaunchAddr = immediateNextAddr
  val prefetchLaunchIndex = prefetchLaunchAddr(dataIndexBits - 1, wordBits)
  val prefetchLaunchWord = prefetchLaunchAddr(wordBits - 1, 0)
  val prefetchLaunchTag = prefetchLaunchAddr(wordAddrBits - 1, dataIndexBits)
  val prefetchLaunchWayHits = Wire(Vec(way, Bool()))
  for (i <- 0 until way) {
    val entry = prefetchTagMem(i)(prefetchLaunchIndex)
    prefetchLaunchWayHits(i) := entry(tagBits) &&
      entry(tagBits - 1, 0) === prefetchLaunchTag
  }
  val prefetchWayHits = RegEnable(prefetchLaunchWayHits.asUInt, 0.U(way.W), prefetchFire)
  val prefetchDataIndex = Cat(prefetchLaunchIndex, prefetchLaunchWord)
  val memoryReadIndex = Mux(requestFire, requestDataIndex, prefetchDataIndex)
  val memoryReadSetIndex = Mux(requestFire, requestIndex, prefetchLaunchIndex)
  val memoryReadEnable = requestFire || prefetchFire
  val tagReadIndex = RegEnable(memoryReadSetIndex, 0.U(indexBits.W), memoryReadEnable)
  val lookupData = Wire(Vec(way, UInt(32.W)))
  val lookupTag = Wire(Vec(way, UInt(tagBits.W)))
  for (i <- 0 until way) {
    lookupData(i) := dataMem(i).read(memoryReadIndex, memoryReadEnable)
    lookupTag(i) := tagMem(i)(tagReadIndex)
  }

  val reqIndex = reqAddr(offsetBits + indexBits - 1, offsetBits)
  val reqTag = reqAddr(31, offsetBits + indexBits)
  val reqWord = reqAddr(offsetBits - 1, 2)
  val reqBase = reqAddr & (~(block_size - 1).U(32.W))
  val reqValid = validBits(reqIndex)
  val wayHits = Wire(Vec(way, Bool()))
  for (i <- 0 until way) {
    wayHits(i) := reqValid(i) && lookupTag(i) === reqTag
  }
  val hit = wayHits.asUInt.orR
  val hitData = Mux1H(wayHits, lookupData)

  val prefetchHit = prefetchPending && prefetchWayHits.orR
  val prefetchHitData = Mux1H(prefetchWayHits, lookupData)

  val wordBufferHit = wordBufferValid && requestWordAddr === wordBufferAddr
  val nextBufferHit = nextBufferValid && requestWordAddr === nextBufferAddr
  val fastHit = wordBufferHit || nextBufferHit || fetchWordCache.io.hit
  val fastData = Mux(wordBufferHit, wordBufferData,
    Mux(nextBufferHit, nextBufferData, fetchWordCache.io.data))
  val prefetchResponseBlocksDemand = prefetchPending &&
    requestWordAddr === prefetchAddr && !fastHit

  // A lookup launches the following word regardless of the current tag result.
  // The returned word is still accepted only when prefetchHit is true. Launch
  // validity intentionally does not re-check the two buffer addresses: those
  // wide comparisons created a tag-to-prefetchPending timing cone after logic
  // sharing, while an occasional duplicate local RAM read is harmless.
  // The BRAM result is still owned by the demand lookup while in sLookup.
  // Starting the next-word read there can replace that result before it is
  // captured, pairing the demand PC with the following instruction. Launch
  // lookahead only after the current response has been secured in respData.
  val prefetchOpportunity =
    (state === sIdle && io.in.arvalid && fastHit) ||
    (state === sResp && io.in.rready)
  prefetchFire := prefetchOpportunity &&
    !(io.fencei.valid && io.fencei.bits.is_fencei)
  requestFire := state === sIdle && io.in.arvalid && io.in.arready && !fastHit

  val invalidMask = ~reqValid
  val selectedWay = Mux(reqValid.andR, replacement(reqIndex), PriorityEncoder(invalidMask))

  val refillNext = Wire(Vec(words, UInt(32.W)))
  refillNext := refillData
  refillNext(refillWord) := io.out.rdata
  io.in.arready := false.B
  io.in.rvalid := false.B
  io.in.rdata := 0.U
  io.in.rresp := 0.U
  io.fencei.ready := state === sFence && fenceIndex === (set - 1).U

  io.out.araddr := 0.U
  io.out.arvalid := false.B
  io.out.arid := 0.U
  io.out.arlen := 0.U
  io.out.arsize := 2.U
  io.out.arburst := 1.U
  io.out.rready := false.B
  io.out.awaddr := 0.U
  io.out.awvalid := false.B
  io.out.awid := 0.U
  io.out.awlen := 0.U
  io.out.awsize := 0.U
  io.out.awburst := 0.U
  io.out.wdata := 0.U
  io.out.wstrb := 0.U
  io.out.wvalid := false.B
  io.out.wlast := false.B
  io.out.bready := false.B

  switch(state) {
    is(sIdle) {
      io.in.arready := !prefetchTagInit &&
        !(io.fencei.valid && io.fencei.bits.is_fencei) &&
        !prefetchResponseBlocksDemand
      when(io.fencei.valid && io.fencei.bits.is_fencei) {
        fenceIndex := 0.U
        wordBufferValid := false.B
        nextBufferValid := false.B
        prefetchPending := false.B
        l1HitFillPending := false.B
        state := sFence
      }.elsewhen(io.in.arvalid && fastHit) {
        io.in.rvalid := true.B
        io.in.rdata := fastData
        // A real L0 hit already owns this exact word. Rewriting it would create
        // a read/write timing loop with no benefit.
        l0FillValid := !fetchWordCache.io.hit
        l0FillAddr := requestWordAddr
        l0FillData := fastData
        when(nextBufferHit) {
          nextBufferValid := wordBufferValid
          nextBufferAddr := wordBufferAddr
          nextBufferData := wordBufferData
          wordBufferValid := true.B
          wordBufferAddr := requestWordAddr
          wordBufferData := nextBufferData
        }
        when(!io.in.rready) {
          reqAddr := io.in.araddr
          respData := fastData
          respCode := 0.U
          state := sResp
        }
      }.elsewhen(requestFire) {
        reqAddr := io.in.araddr
        reqBurst := io.in.araddr >= "ha000_0000".U && io.in.araddr <= "hbfff_ffff".U
        respCode := 0.U
        state := sLookup
      }
    }

    is(sLookup) {
      when(hit) {
        respData := hitData
        respCode := 0.U
        l1HitFillPending := true.B
        state := sResp
      }.otherwise {
        l1HitFillPending := false.B
        missWay := selectedWay
        refillWord := 0.U
        refillData := 0.U.asTypeOf(refillData)
        respCode := 0.U
        io.out.araddr := reqBase
        io.out.arvalid := true.B
        io.out.arlen := Mux(reqBurst, (words - 1).U, 0.U)
        when(io.out.arready) {
          state := sMissR
        }
      }
    }

    is(sMissAr) {
      io.out.araddr := reqBase + (refillWord << 2)
      io.out.arvalid := true.B
      when(io.out.arready) {
        state := sMissR
      }
    }

    is(sMissR) {
      io.out.rready := true.B
      io.in.rvalid := io.out.rvalid && lastRefillBeat
      io.in.rdata := refillNext(reqWord)
      io.in.rresp := currentRespCode

      when(io.out.rvalid) {
        refillData := refillNext
        respCode := currentRespCode
        for (i <- 0 until way) {
          when(missWay === i.U) {
            dataMem(i).write(Cat(reqIndex, refillWord), io.out.rdata)
          }
        }
        when(lastRefillBeat) {
          when(!currentRespCode.orR) {
            l0FillValid := true.B
            l0FillAddr := reqAddr(31, 2)
            l0FillData := refillNext(reqWord)
            for (i <- 0 until way) {
              when(missWay === i.U) {
                tagMem(i).write(reqIndex, reqTag)
              }
            }
            nextBufferValid := wordBufferValid
            nextBufferAddr := wordBufferAddr
            nextBufferData := wordBufferData
            wordBufferValid := true.B
            wordBufferAddr := reqAddr(31, 2)
            wordBufferData := refillNext(reqWord)
            when(reqWord =/= (words - 1).U) {
              nextBufferValid := true.B
              nextBufferAddr := reqAddr(31, 2) + 1.U
              nextBufferData := refillNext(reqWord + 1.U)
            }
          }
          when(io.in.rready) {
            state := sIdle
          }.otherwise {
            respData := refillNext(reqWord)
            state := sResp
          }
        }.otherwise {
          refillWord := refillWord + 1.U
          state := Mux(reqBurst, sMissR, sMissAr)
        }
      }
    }

    is(sResp) {
      io.in.rvalid := true.B
      io.in.rdata := respData
      io.in.rresp := respCode
      when(l1HitFillPending) {
        l0FillValid := true.B
        l0FillAddr := reqAddr(31, 2)
        l0FillData := respData
        nextBufferValid := wordBufferValid
        nextBufferAddr := wordBufferAddr
        nextBufferData := wordBufferData
        wordBufferValid := true.B
        wordBufferAddr := reqAddr(31, 2)
        wordBufferData := respData
        l1HitFillPending := false.B
      }
      when(io.in.rready) {
        state := sIdle
      }
    }

    is(sFence) {
      when(fenceIndex === (set - 1).U) {
        fenceIndex := 0.U
        state := sIdle
      }.otherwise {
        fenceIndex := fenceIndex + 1.U
      }
    }
  }

  // Keep this after the state machine so a returning lookahead word replaces
  // an obsolete buffer entry consumed in the same cycle.
  // The launch address is inexpensive to precompute every cycle. Its validity
  // remains owned by prefetchPending, so tag-hit control no longer drives the
  // clock enable of this 30-bit register bank.
  prefetchAddr := prefetchLaunchAddr
  prefetchPending := prefetchFire
  when(prefetchPending) {
    when(prefetchHit && !(io.fencei.valid && io.fencei.bits.is_fencei)) {
      nextBufferValid := true.B
      nextBufferAddr := prefetchAddr
      nextBufferData := prefetchHitData
    }
  }

  val prefetchTagWriteEnable = WireDefault(0.U(way.W))
  val prefetchTagWriteIndex = WireDefault(0.U(indexBits.W))
  val prefetchTagWriteData = WireDefault(0.U((tagBits + 1).W))
  val lineInstall = state === sMissR && io.out.rvalid && lastRefillBeat &&
    !currentRespCode.orR
  when(prefetchTagInit) {
    prefetchTagWriteEnable := Fill(way, true.B)
    prefetchTagWriteIndex := prefetchTagInitIndex
    when(prefetchTagInitIndex === (set - 1).U) {
      prefetchTagInitIndex := 0.U
      prefetchTagInit := false.B
    }.otherwise {
      prefetchTagInitIndex := prefetchTagInitIndex + 1.U
    }
  }.elsewhen(state === sFence) {
    prefetchTagWriteEnable := Fill(way, true.B)
    prefetchTagWriteIndex := fenceIndex
  }.elsewhen(lineInstall) {
    prefetchTagWriteEnable := UIntToOH(missWay, way)
    prefetchTagWriteIndex := reqIndex
    prefetchTagWriteData := Cat(true.B, reqTag)
  }
  for (i <- 0 until way) {
    when(prefetchTagWriteEnable(i)) {
      prefetchTagMem(i).write(prefetchTagWriteIndex, prefetchTagWriteData)
    }
  }

  val validWriteEnable = WireDefault(false.B)
  val validWriteIndex = WireDefault(0.U(indexBits.W))
  val validWriteData = WireDefault(0.U(way.W))
  when(prefetchTagInit) {
    validWriteEnable := true.B
    validWriteIndex := prefetchTagInitIndex
  }.elsewhen(state === sFence) {
    validWriteEnable := true.B
    validWriteIndex := fenceIndex
  }.elsewhen(validInstallPending) {
    validWriteEnable := true.B
    validWriteIndex := validInstallIndex
    validWriteData := validInstallData
  }
  when(validWriteEnable) {
    validBits.write(validWriteIndex, validWriteData)
  }
  when(prefetchTagInit || state === sFence) {
    validInstallPending := false.B
  }.elsewhen(validInstallPending) {
    validInstallPending := false.B
  }.elsewhen(lineInstall) {
    validInstallPending := true.B
    validInstallIndex := reqIndex
    validInstallData := reqValid | UIntToOH(missWay, way)
  }
  val replacementWriteEnable = prefetchTagInit || lineInstall
  val replacementWriteIndex = Mux(prefetchTagInit, prefetchTagInitIndex, reqIndex)
  val replacementWriteData = Mux(prefetchTagInit, 0.U,
    Mux(missWay === (way - 1).U, 0.U, missWay + 1.U))
  when(replacementWriteEnable) {
    replacement.write(replacementWriteIndex, replacementWriteData)
  }

  if (conf.statistics) {
    PM(conf, clock, EVENT_ICACHE_MISS, 1.U,
      state === sLookup && !hit && io.out.arvalid && io.out.arready)
  }
}
