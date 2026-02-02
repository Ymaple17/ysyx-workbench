package core

import chisel3._
import chisel3.util._
import bus._


class Simple_ICache(val conf: CoreConfig) extends Module{
  val io = IO(new ICache_IO)

  val ar_queue = Module(new Queue(new Bundle {
    val addr = UInt(32.W)
  }, entries = 1, pipe = true, flow = false))

  ar_queue.io.enq.valid := io.in.arvalid
  ar_queue.io.enq.bits.addr := io.in.araddr
  io.in.arready := ar_queue.io.enq.ready

  io.out.arvalid := ar_queue.io.deq.valid
  io.out.araddr  := ar_queue.io.deq.bits.addr
  ar_queue.io.deq.ready := io.out.arready

  io.out.arid    := 0.U
  io.out.arlen   := 0.U    // 单次传输
  io.out.arsize  := 2.U    // 4 bytes
  io.out.arburst := 1.U    // INCR

  io.in.rvalid  := io.out.rvalid
  io.in.rdata   := io.out.rdata
  io.in.rresp   := io.out.rresp
  io.out.rready := io.in.rready

  io.out.awvalid := false.B
  io.out.awaddr  := 0.U
  io.out.awid    := 0.U
  io.out.awlen   := 0.U
  io.out.awsize  := 0.U
  io.out.awburst := 0.U
  
  io.out.wvalid  := false.B
  io.out.wdata   := 0.U
  io.out.wstrb   := 0.U
  io.out.wlast   := false.B
  
  io.out.bready  := false.B

  io.fencei.ready := true.B
}