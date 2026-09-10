package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import common.BPU_Config._
import core.State

class FQEntry extends Bundle {
  val inst      = UInt(32.W)
  val pc        = UInt(32.W)
  val state     = new State
  val bp_valid  = Bool()
  val bp_taken  = Bool()
  val bp_target = UInt(32.W)
  val bp_index  = UInt(BP_META_WIDTH.W)
  val ftq_idx = UInt(OoOParams.FTQ_PTR_W.W)
  val ftq_generation = UInt(OoOParams.FTQ_GEN_W.W)
}

class FQPacket extends Bundle {
  val valid = Vec(OoOParams.FETCH_WIDTH, Bool())
  val bits  = Vec(OoOParams.FETCH_WIDTH, new FQEntry)
}

class FetchQueue(n: Int = OoOParams.FQ_SIZE) extends Module {
  val ptrW = log2Ceil(n)
  require(n >= 2 && isPow2(n))
  require(OoOParams.FETCH_WIDTH == OoOParams.CORE_WIDTH)

  val io = IO(new Bundle {
    val enq   = Flipped(Decoupled(new FQPacket))
    val deq   = Decoupled(new FQEntry)
    val deq1  = Decoupled(new FQEntry)
    val deq2  = Decoupled(new FQEntry)
    val deq3  = Decoupled(new FQEntry)
    val flush = Input(Bool())
    val count = Output(UInt(log2Ceil(n + 1).W))
    val full  = Output(Bool())
    val empty = Output(Bool())
    val space = Output(UInt(log2Ceil(n + 1).W))
    val enqSpace = Output(UInt(log2Ceil(n + 1).W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new FQEntry))))
  val head  = RegInit(0.U(ptrW.W))
  val tail  = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  val empty = count === 0.U
  val full  = count === n.U
  val space = n.U - count

  io.count := count
  io.empty := empty
  io.full  := full
  io.space := space

  io.deq.valid := !empty && !io.flush
  io.deq.bits  := entries(head)
  io.deq1.valid := (count >= 2.U) && !io.flush
  io.deq1.bits := entries((head + 1.U)(ptrW - 1, 0))
  io.deq2.valid := (count >= 3.U) && !io.flush
  io.deq2.bits := entries((head + 2.U)(ptrW - 1, 0))
  io.deq3.valid := (count >= 4.U) && !io.flush
  io.deq3.bits := entries((head + 3.U)(ptrW - 1, 0))
  val deq0Fire = io.deq.valid && io.deq.ready
  val deq1Fire = io.deq1.valid && io.deq1.ready && deq0Fire
  val deq2Fire = io.deq2.valid && io.deq2.ready && deq1Fire
  val deq3Fire = io.deq3.valid && io.deq3.ready && deq2Fire
  val deqCountNow = PopCount(Seq(deq0Fire, deq1Fire, deq2Fire, deq3Fire))
  val enqSpace = space + deqCountNow
  val enqCount = PopCount(io.enq.bits.valid.asUInt)
  io.enqSpace := enqSpace
  io.enq.ready := !io.flush && (enqCount === 0.U || enqSpace >= enqCount)

  when(io.flush) {
    head  := 0.U
    tail  := 0.U
    count := 0.U
  }.otherwise {
    val doEnq = Wire(Vec(OoOParams.FETCH_WIDTH, Bool()))
    for (lane <- 0 until OoOParams.FETCH_WIDTH) {
      val prefix = (0 until lane).map(doEnq(_)).foldLeft(true.B)(_ && _)
      doEnq(lane) := io.enq.fire && io.enq.bits.valid(lane) && prefix
      when(doEnq(lane)) {
        entries((tail + lane.U)(ptrW - 1, 0)) := io.enq.bits.bits(lane)
      }
    }
    val deqCount = PopCount(Seq(deq0Fire, deq1Fire, deq2Fire, deq3Fire))
    val actualEnqCount = PopCount(doEnq)
    when(actualEnqCount =/= 0.U) {
      tail := tail + actualEnqCount
    }
    when(deqCount =/= 0.U) {
      head := head + deqCount
    }
    count := count + actualEnqCount - deqCount
  }

  when(!reset.asBool) {
    for (lane <- 1 until OoOParams.FETCH_WIDTH) {
      assert(!io.enq.bits.valid(lane) || io.enq.bits.valid(lane - 1),
        "fetch queue enqueue must be a valid prefix")
    }
  }
}
