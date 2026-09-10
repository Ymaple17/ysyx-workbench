package unit

import chisel3._
import chisel3.util._
import common.BPU_Config._

class BPUCommitUpdate extends Bundle {
  val pc = UInt(32.W)
  val target = UInt(32.W)
  val taken = Bool()
  val isBranch = Bool()
  val isJalr = Bool()
  val index = UInt(BP_META_WIDTH.W)
  val isCall = Bool()
  val isRet = Bool()
}

class BPUUpdateQueue(depth: Int = 8) extends Module {
  require(depth >= 2 && isPow2(depth))

  private val ptrW = log2Ceil(depth)
  private val countW = log2Ceil(depth + 1)

  val io = IO(new Bundle {
    val enq = Flipped(Vec(2, Valid(new BPUCommitUpdate)))
    val deq = Valid(new BPUCommitUpdate)
    val free = Output(UInt(countW.W))
  })

  val entries = Reg(Vec(depth, new BPUCommitUpdate))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(countW.W))

  val doDeq = count =/= 0.U
  val enqCount = PopCount(io.enq.map(_.valid))
  val available = depth.U - count + doDeq.asUInt

  io.deq.valid := doDeq
  io.deq.bits := entries(head)
  io.free := available

  assert(enqCount <= available, "BPU update queue overflow")

  val enq0 = io.enq(0).valid
  val enq1 = io.enq(1).valid
  val tail1 = (tail + enq0.asUInt)(ptrW - 1, 0)

  when(enq0) {
    entries(tail) := io.enq(0).bits
  }
  when(enq1) {
    entries(tail1) := io.enq(1).bits
  }
  when(enqCount =/= 0.U) {
    tail := (tail + enqCount)(ptrW - 1, 0)
  }
  when(doDeq) {
    head := head + 1.U
  }
  count := count + enqCount - doDeq.asUInt
}
