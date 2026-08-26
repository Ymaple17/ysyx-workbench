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
  val bp_index  = UInt(log2Ceil(BHT_SIZE).W)
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
  require(OoOParams.FETCH_WIDTH == 2)

  val io = IO(new Bundle {
    val enq   = Flipped(Decoupled(new FQPacket))
    val deq   = Decoupled(new FQEntry)
    val deq1  = Decoupled(new FQEntry)
    val flush = Input(Bool())
    val count = Output(UInt(log2Ceil(n + 1).W))
    val full  = Output(Bool())
    val empty = Output(Bool())
    val space = Output(UInt(log2Ceil(n + 1).W))
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

  val enqCount = PopCount(io.enq.bits.valid.asUInt)
  io.enq.ready := !io.flush && (enqCount === 0.U || space >= enqCount)
  io.deq.valid := !empty && !io.flush
  io.deq.bits  := entries(head)
  io.deq1.valid := (count >= 2.U) && !io.flush
  io.deq1.bits := entries((head + 1.U)(ptrW - 1, 0))

  when(io.flush) {
    head  := 0.U
    tail  := 0.U
    count := 0.U
  }.otherwise {
    val doEnq0 = io.enq.fire && io.enq.bits.valid(0)
    val doEnq1 = io.enq.fire && io.enq.bits.valid(1) && doEnq0
    val doDeq0 = io.deq.fire
    val doDeq1 = io.deq1.fire && doDeq0
    val deqCount = PopCount(Seq(doDeq0, doDeq1))
    val actualEnqCount = PopCount(Seq(doEnq0, doEnq1))
    val tail1 = tail + 1.U
    val tail2 = tail + 2.U

    when(doEnq0) {
      entries(tail) := io.enq.bits.bits(0)
    }
    when(doEnq1) {
      entries(tail1(ptrW - 1, 0)) := io.enq.bits.bits(1)
    }
    when(actualEnqCount =/= 0.U) {
      tail := Mux(doEnq1, tail2(ptrW - 1, 0), tail1(ptrW - 1, 0))
    }
    when(deqCount =/= 0.U) {
      head := head + deqCount
    }
    count := count + actualEnqCount - deqCount
  }
}
