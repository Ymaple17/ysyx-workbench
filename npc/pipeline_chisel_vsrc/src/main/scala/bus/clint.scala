package bus

import chisel3._
import chisel3.util._
import _root_.core._

class Clint(coreConfig: CoreConfig) extends Module{
  val io = IO(new AXI4Slave)
  io.setDefaults()
  val rdata = RegInit(0.U(32.W))
  io.rdata := rdata
  val ADDR = "h02000000".U
  
  val mtime = RegInit(0.U(64.W))
  mtime := mtime + 1.U
  
  val s_IDLE :: s_WAIT :: Nil = Enum(2)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(state)

  next_state := MuxLookup(state, s_IDLE)(Seq(
      s_IDLE   -> Mux(io.arvalid, s_WAIT, s_IDLE),
      s_WAIT   -> Mux(io.rready, s_IDLE, s_WAIT)
  ))
  state := next_state

  io.arready := false.B
  rdata := Mux(io.araddr === ADDR, mtime(31, 0), mtime(63, 32))

  switch(state){
    is(s_IDLE){
      io.arready := true.B
      io.rvalid := false.B
    }
    is(s_WAIT){
      io.arready := false.B
      io.rvalid := true.B
    }
  }
}
