package bus

import chisel3._
import chisel3.util._
import core._

class AXI4Master extends Bundle {
  // Read Address Channel (AR)
  val araddr   = Output(UInt(32.W))
  val arvalid  = Output(Bool())
  val arid     = Output(UInt(4.W)) //事务ID
  val arlen    = Output(UInt(8.W))
  val arsize   = Output(UInt(3.W))
  val arburst  = Output(UInt(2.W)) //突发类型
  val arready  = Input(Bool())

  // Read Data Channel (R)
  val rdata    = Input(UInt(32.W))
  val rresp    = Input(UInt(2.W)) //响应状态
  val rvalid   = Input(Bool())
  val rlast    = Input(Bool()) //最后一拍标记
  val rid      = Input(UInt(4.W)) //事务ID
  val rready   = Output(Bool())

  // Write Address Channel (AW)
  val awaddr   = Output(UInt(32.W))
  val awvalid  = Output(Bool())
  val awid     = Output(UInt(4.W))
  val awlen    = Output(UInt(8.W))
  val awsize   = Output(UInt(3.W))
  val awburst  = Output(UInt(2.W))
  val awready  = Input(Bool())

  // Write Data Channel (W)
  val wdata    = Output(UInt(32.W))
  val wstrb    = Output(UInt(4.W)) //字节使能掩码
  val wvalid   = Output(Bool())
  val wlast    = Output(Bool()) //最后一拍标记
  val wready   = Input(Bool())

  // Write Response Channel (B)
  val bresp    = Input(UInt(2.W))
  val bvalid   = Input(Bool())
  val bid      = Input(UInt(4.W))
  val bready   = Output(Bool())

  def setDefaults(): Unit = {
    araddr   := 0.U
    arvalid  := false.B
    arid     := 0.U
    arlen    := 0.U
    arsize   := 0.U
    arburst  := 0.U

    rready   := false.B

    awaddr   := 0.U
    awvalid  := false.B
    awid     := 0.U
    awlen    := 0.U
    awsize   := 0.U
    awburst  := 0.U

    wready   := false.B

    bready   := false.B
  }
}

class AXI4Slave extends Bundle {
  // Read Address Channel (AR)
  val araddr   = Input(UInt(32.W))
  val arvalid  = Input(Bool())
  val arid     = Input(UInt(4.W))
  val arlen    = Input(UInt(8.W))
  val arsize   = Input(UInt(3.W))
  val arburst  = Input(UInt(2.W))
  val arready  = Output(Bool())

  // Read Data Channel (R)
  val rdata    = Output(UInt(32.W))
  val rresp    = Output(UInt(2.W))
  val rvalid   = Output(Bool())
  val rlast    = Output(Bool())
  val rid      = Output(UInt(4.W))
  val rready   = Input(Bool())

  // Write Address Channel (AW)
  val awaddr   = Input(UInt(32.W))
  val awvalid  = Input(Bool())
  val awid     = Input(UInt(4.W))
  val awlen    = Input(UInt(8.W))
  val awsize   = Input(UInt(3.W))
  val awburst  = Input(UInt(2.W))
  val awready  = Output(Bool())

  // Write Data Channel (W)
  val wdata    = Input(UInt(32.W))
  val wstrb    = Input(UInt(4.W))
  val wvalid   = Input(Bool())
  val wlast    = Input(Bool())
  val wready   = Output(Bool())

  // Write Response Channel (B)
  val bresp    = Output(UInt(2.W))
  val bvalid   = Output(Bool())
  val bid      = Output(UInt(4.W))
  val bready   = Input(Bool())

  def setDefaults(): Unit = {
    arready  := false.B
    
    rdata    := 0.U
    rresp    := 0.U
    rvalid   := false.B
    rlast    := false.B
    rid      := 0.U

    awready  := false.B

    wready   := false.B

    bresp    := 0.U
    bvalid   := false.B
    bid      := 0.U
  }
}
