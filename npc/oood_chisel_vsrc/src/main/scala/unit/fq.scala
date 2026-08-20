package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import common.BPU_Config._
import core.State

/** IFU→ID 指令包（与 IFU_IDU_IO 字段对齐，便于队列存储） */
class FQEntry extends Bundle {
  val inst      = UInt(32.W)
  val pc        = UInt(32.W)
  val state     = new State
  val bp_valid  = Bool()
  val bp_taken  = Bool()
  val bp_target = UInt(32.W)
  val bp_index  = UInt(log2Ceil(BHT_SIZE).W)
}

/**
 * Fetch Queue：解耦取指与后端 stall。
 * flush 必须真清（head=tail=count=0），不能只 mask deq.valid。
 */
class FetchQueue(n: Int = OoOParams.FQ_SIZE) extends Module {
  val ptrW = log2Ceil(n)
  require(n >= 2 && isPow2(n))

  val io = IO(new Bundle {
    val enq   = Flipped(Decoupled(new FQEntry))
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

  io.count := count
  io.empty := empty
  io.full  := full
  io.space := n.U - count

  io.enq.ready := !full && !io.flush
  io.deq.valid := !empty && !io.flush
  io.deq.bits  := entries(head)
  io.deq1.valid := (count >= 2.U) && !io.flush
  io.deq1.bits := entries((head + 1.U)(ptrW - 1, 0))

  when(io.flush) {
    head  := 0.U
    tail  := 0.U
    count := 0.U
  }.otherwise {
    val do_enq = io.enq.fire
    val do_deq0 = io.deq.fire
    val do_deq1 = io.deq1.fire && do_deq0
    val deqCount = PopCount(Seq(do_deq0, do_deq1))
    when(do_enq) {
      entries(tail) := io.enq.bits
      tail := tail + 1.U
    }
    when(deqCount =/= 0.U) {
      head := head + deqCount
    }
    count := count + do_enq.asUInt - deqCount
  }
}
