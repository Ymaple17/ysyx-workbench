package bus

import chisel3._
import chisel3.util._
import core._
import bus.Slave_Master._

class Xbar_IO extends Bundle{
  val imem = new AXI4Slave
  val dmem = new AXI4Slave
  val soc = new AXI4Master
  val clint = new AXI4Master
}

class Xbar extends Module{
  val io = IO(new Xbar_IO)

  val s_SELECT :: s_IFUREAD :: s_LSUREAD :: s_CLINTWRITE :: Nil = Enum(4)
  
  val clint_read = Wire(Bool())
  val clint_write = Wire(Bool())
  clint_read := io.dmem.araddr >= "h0200_0000".U(32.W) && io.dmem.araddr <= "h0200_ffff".U(32.W)
  clint_write := io.dmem.awaddr >= "h0200_0000".U(32.W) && io.dmem.awaddr <= "h0200_ffff".U(32.W)

  val state = RegInit(s_SELECT)
  val next_state = WireDefault(s_SELECT)

  val is_dmem = Wire(Bool())
  val is_imem = Wire(Bool())
  is_dmem := Mux(io.dmem.arvalid | io.dmem.awvalid, true.B, false.B)
  is_imem := Mux(io.imem.arvalid, Mux(is_dmem || state === s_LSUREAD || state === s_CLINTWRITE, false.B, true.B), false.B)

  next_state := MuxLookup(state, s_SELECT)(Seq(
    s_SELECT -> MuxCase(s_SELECT, Seq(
      (is_imem) -> s_IFUREAD,
      (is_dmem & ~clint_read & ~clint_write) -> s_LSUREAD,
      (is_dmem & (clint_read | clint_write)) -> s_CLINTWRITE
    )),
    s_IFUREAD -> Mux((io.soc.rvalid & io.soc.rlast), Mux(io.imem.arvalid, s_IFUREAD, s_SELECT), s_IFUREAD),
    s_LSUREAD -> Mux(io.soc.rvalid | io.soc.bvalid, s_SELECT, s_LSUREAD),
    s_CLINTWRITE -> Mux(io.clint.rvalid | io.soc.bvalid, s_SELECT, s_CLINTWRITE)
  ))
  state := next_state

  io.imem.setDefaults()
  io.dmem.setDefaults()
  io.clint.setDefaults()
  io.soc.setDefaults()

  import bus.Slave_Master._
  switch(state){
    is(s_SELECT){
      when(is_imem){
        Slave_Master.ALL_connect(io.imem, io.soc)
      }.elsewhen(is_dmem & ~clint_read & ~clint_write){
        Slave_Master.ALL_connect(io.dmem, io.soc)
      }.elsewhen(is_dmem & (clint_read | clint_write)){
        Slave_Master.ALL_connect(io.dmem, io.clint)
      }
    }
    is(s_IFUREAD){
      Slave_Master.ALL_connect(io.imem, io.soc)
    }
    is(s_LSUREAD){
      Slave_Master.ALL_connect(io.dmem, io.soc)
    }
    is(s_CLINTWRITE){
      Slave_Master.ALL_connect(io.dmem, io.clint)
    }
  }
}