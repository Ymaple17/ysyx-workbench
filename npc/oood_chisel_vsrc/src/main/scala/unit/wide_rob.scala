package unit

import chisel3._
import chisel3.util._
import common.OoOParams
import core.State

class WideROB(n: Int = OoOParams.ROB_SIZE) extends Module {
  private val width = OoOParams.CORE_WIDTH
  private val ptrW = log2Ceil(n)
  require(n >= width && isPow2(n))

  val io = IO(new Bundle {
    val enq_fire = Input(Vec(width, Bool()))
    val enq_bits = Input(Vec(width, new ROBEntry))
    val enq_idx = Output(Vec(width, UInt(ptrW.W)))
    val space = Output(UInt(log2Ceil(n + 1).W))

    val wb_fire = Input(Vec(width, Bool()))
    val wb_idx = Input(Vec(width, UInt(ptrW.W)))
    val wb_val = Input(Vec(width, UInt(32.W)))
    val wb_state = Input(Vec(width, new State))
    val wb_mem_addr = Input(Vec(width, UInt(32.W)))
    val wb_mem_wdata = Input(Vec(width, UInt(32.W)))
    val wb_actual_taken = Input(Vec(width, Bool()))
    val wb_actual_target = Input(Vec(width, UInt(32.W)))

    val ctrl_wb_fire = Input(Bool())
    val ctrl_wb_idx = Input(UInt(ptrW.W))
    val ctrl_wb_state = Input(new State)
    val ctrl_wb_actual_taken = Input(Bool())
    val ctrl_wb_actual_target = Input(UInt(32.W))
    val store_wb_fire = Input(Bool())
    val store_wb_idx = Input(UInt(ptrW.W))
    val store_wb_state = Input(new State)
    val store_wb_mem_addr = Input(UInt(32.W))
    val store_wb_mem_wdata = Input(UInt(32.W))

    val commit_valid = Output(Vec(width, Bool()))
    val commit_idx = Output(Vec(width, UInt(ptrW.W)))
    val commit_bits = Output(Vec(width, new ROBEntry))
    val commit_fire = Input(Vec(width, Bool()))

    val flush = Input(Bool())
    val flush_idx = Input(UInt(ptrW.W))
    val flush_all = Input(Bool())

    val entries = Output(Vec(n, new ROBEntry))
    val head = Output(UInt(ptrW.W))
    val tail = Output(UInt(ptrW.W))
    val count = Output(UInt(log2Ceil(n + 1).W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new ROBEntry))))
  val head = RegInit(0.U(ptrW.W))
  val tail = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  def ptr(base: UInt, offset: Int): UInt =
    (base + offset.U)(ptrW - 1, 0)
  def age(idx: UInt): UInt =
    (idx - head)(ptrW - 1, 0)
  def sameCycleDone(idx: UInt): Bool = {
    val data = (0 until width).map(i => io.wb_fire(i) && io.wb_idx(i) === idx)
      .foldLeft(false.B)(_ || _)
    data || (io.ctrl_wb_fire && io.ctrl_wb_idx === idx) ||
      (io.store_wb_fire && io.store_wb_idx === idx)
  }

  io.entries := entries
  io.head := head
  io.tail := tail
  io.count := count
  io.space := n.U - count

  for (lane <- 0 until width) {
    val idx = ptr(head, lane)
    io.enq_idx(lane) := ptr(tail, lane)
    io.commit_idx(lane) := idx
    io.commit_bits(lane) := entries(idx)
    io.commit_valid(lane) := count > lane.U && entries(idx).valid &&
      (entries(idx).done || sameCycleDone(idx))
  }

  for (lane <- 0 until width) {
    when(io.wb_fire(lane) && entries(io.wb_idx(lane)).valid) {
      val idx = io.wb_idx(lane)
      entries(idx).done := true.B
      entries(idx).dest_val := io.wb_val(lane)
      entries(idx).state := io.wb_state(lane)
      entries(idx).mem_addr := io.wb_mem_addr(lane)
      entries(idx).mem_wdata := io.wb_mem_wdata(lane)
      entries(idx).addr_ready := true.B
      entries(idx).actual_taken := io.wb_actual_taken(lane)
      entries(idx).actual_target := io.wb_actual_target(lane)
    }
  }
  when(io.ctrl_wb_fire && entries(io.ctrl_wb_idx).valid) {
    val idx = io.ctrl_wb_idx
    entries(idx).done := true.B
    entries(idx).state := io.ctrl_wb_state
    entries(idx).actual_taken := io.ctrl_wb_actual_taken
    entries(idx).actual_target := io.ctrl_wb_actual_target
  }
  when(io.store_wb_fire && entries(io.store_wb_idx).valid) {
    val idx = io.store_wb_idx
    entries(idx).done := true.B
    entries(idx).state := io.store_wb_state
    entries(idx).mem_addr := io.store_wb_mem_addr
    entries(idx).mem_wdata := io.store_wb_mem_wdata
    entries(idx).addr_ready := true.B
  }

  val commitCount = PopCount(io.commit_fire)
  val enqCount = PopCount(io.enq_fire)

  when(io.flush_all) {
    for (i <- 0 until n) {
      entries(i).valid := false.B
    }
    head := 0.U
    tail := 0.U
    count := 0.U
  }.elsewhen(io.flush) {
    val keepCount = age(io.flush_idx) +& 1.U
    for (i <- 0 until n) {
      val live = age(i.U) < count
      when(entries(i).valid && live && age(i.U) >= keepCount) {
        entries(i).valid := false.B
      }
    }
    tail := io.flush_idx + 1.U
    count := keepCount
  }.otherwise {
    for (lane <- 0 until width) {
      when(io.commit_fire(lane)) {
        entries(ptr(head, lane)).valid := false.B
      }
      when(io.enq_fire(lane)) {
        val entry = WireDefault(io.enq_bits(lane))
        entry.valid := true.B
        entry.done := false.B
        entries(ptr(tail, lane)) := entry
      }
    }
    when(commitCount =/= 0.U) {
      head := head + commitCount
    }
    when(enqCount =/= 0.U) {
      tail := tail + enqCount
    }
    count := count + enqCount - commitCount
  }

  when(!reset.asBool) {
    for (lane <- 1 until width) {
      assert(!io.enq_fire(lane) || io.enq_fire(lane - 1),
        "wide ROB enqueue must be a valid prefix")
      assert(!io.commit_fire(lane) || io.commit_fire(lane - 1),
        "wide ROB commit must be a valid prefix")
    }
    for (lane <- 0 until width) {
      assert(!io.commit_fire(lane) || io.commit_valid(lane),
        "wide ROB can only commit completed entries")
    }
    assert(!(io.flush || io.flush_all) || (!io.enq_fire.asUInt.orR && !io.commit_fire.asUInt.orR),
      "redirect cycles must not enqueue or commit")
    assert(PopCount(entries.map(_.valid)) === count,
      "wide ROB valid entries must match count")
  }
}
