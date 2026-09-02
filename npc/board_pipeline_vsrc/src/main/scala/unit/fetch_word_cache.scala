package unit

import chisel3._
import chisel3.util._

class FetchWordCache(entries: Int) extends Module {
  require(isPow2(entries))

  private val wordAddrBits = 30
  private val indexBits = log2Ceil(entries)
  private val tagBits = wordAddrBits - indexBits
  private val dataBits = 32
  private val payloadBits = tagBits + dataBits

  private val dataLo = 0
  private val dataHi = dataLo + dataBits - 1
  private val tagLo = dataHi + 1
  private val tagHi = payloadBits - 1

  val io = IO(new Bundle {
    val lookupAddr = Input(UInt(wordAddrBits.W))
    val hit = Output(Bool())
    val data = Output(UInt(32.W))

    val fillValid = Input(Bool())
    val fillAddr = Input(UInt(wordAddrBits.W))
    val fillData = Input(UInt(32.W))
    val invalidate = Input(Bool())
  })

  val valid = RegInit(VecInit(Seq.fill(entries)(false.B)))
  // An asynchronous Mem maps this small hot-word table to distributed RAM.
  // Keeping all payload fields together also avoids six parallel 32:1 mux trees.
  val payload = Mem(entries, UInt(payloadBits.W))

  val lookupIndex = io.lookupAddr(indexBits - 1, 0)
  val lookupTag = io.lookupAddr(wordAddrBits - 1, indexBits)
  val lookupPayload = payload(lookupIndex)
  io.hit := valid(lookupIndex) && lookupPayload(tagHi, tagLo) === lookupTag
  io.data := lookupPayload(dataHi, dataLo)

  when(io.invalidate) {
    valid := VecInit(Seq.fill(entries)(false.B))
  }.elsewhen(io.fillValid) {
    val fillIndex = io.fillAddr(indexBits - 1, 0)
    valid(fillIndex) := true.B
    payload.write(fillIndex, Cat(
      io.fillAddr(wordAddrBits - 1, indexBits),
      io.fillData))
  }
}
