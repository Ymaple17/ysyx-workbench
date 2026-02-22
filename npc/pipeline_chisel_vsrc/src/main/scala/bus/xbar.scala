package bus

import chisel3._
import chisel3.util._
import core._
import bus.Slave_Master._

class Xbar_IO(coreConfig: CoreConfig) extends Bundle{
  val imem = new AXI4Slave
  val dmem = new AXI4Slave
  val soc = new AXI4Master
  val clint = new AXI4Master
  val uart = if(coreConfig.npc) Some(new AXI4Master) else None
}

class Xbar(coreConfig: CoreConfig) extends Module{
  override def desiredName = "ysyx_25020039_Xbar"

  val io = IO(new Xbar_IO(coreConfig))

  val s_SELECT :: s_IFUREAD :: s_LSUREAD :: s_CLINT :: s_UART :: Nil = Enum(5)
  
  val clint_read = Wire(Bool())
  val clint_write = Wire(Bool())
  val uart_read = Wire(Bool())
  val uart_write = Wire(Bool())

  if (coreConfig.npc) {
      clint_read := io.dmem.arvalid && io.dmem.araddr >= "ha0000048".U && io.dmem.araddr <= "ha000004f".U
      clint_write := io.dmem.awvalid && io.dmem.awaddr >= "ha0000048".U && io.dmem.awaddr <= "ha000004f".U
      
      uart_read := io.dmem.arvalid && io.dmem.araddr >= "ha00003f8".U && io.dmem.araddr <= "ha00003ff".U
      uart_write := io.dmem.awvalid && io.dmem.awaddr >= "ha00003f8".U && io.dmem.awaddr <= "ha00003ff".U
  } else {
      clint_read := io.dmem.araddr >= "h0200_0000".U && io.dmem.araddr <= "h0200_ffff".U
      clint_write := io.dmem.awaddr >= "h0200_0000".U && io.dmem.awaddr <= "h0200_ffff".U
      
      uart_read := false.B
      uart_write := false.B
  }

  val state = RegInit(s_SELECT)
  val next_state = WireDefault(s_SELECT)

  val is_dmem = Wire(Bool())
  val is_imem = Wire(Bool())
  is_dmem := Mux(io.dmem.arvalid | io.dmem.awvalid, true.B, false.B)
  
  val busy = state =/= s_SELECT
  is_imem := Mux(io.imem.arvalid, Mux(is_dmem || busy, false.B, true.B), false.B)

  next_state := MuxLookup(state, s_SELECT)(Seq(
    s_SELECT -> MuxCase(s_SELECT, Seq(
      (is_imem) -> s_IFUREAD,
      (is_dmem & (uart_read | uart_write)) -> s_UART,
      (is_dmem & (clint_read | clint_write)) -> s_CLINT,
      (is_dmem) -> s_LSUREAD
    )),
    s_IFUREAD -> Mux((io.soc.rvalid & io.soc.rlast), Mux(io.imem.arvalid, s_IFUREAD, s_SELECT), s_IFUREAD),
    s_LSUREAD -> Mux(io.soc.rvalid | io.soc.bvalid, s_SELECT, s_LSUREAD),
    s_CLINT -> Mux(io.clint.rvalid | io.clint.bvalid, s_SELECT, s_CLINT),
    s_UART -> (if(coreConfig.npc) Mux((io.uart.get.rvalid | io.uart.get.bvalid), s_SELECT, s_UART) else s_SELECT)
  ))
  state := next_state

  io.imem.setDefaults()
  io.dmem.setDefaults()
  io.clint.setDefaults()
  io.soc.setDefaults()
  if(coreConfig.npc) io.uart.get.setDefaults()

  import bus.Slave_Master._
  switch(state){
    is(s_SELECT){
      when(is_imem){
        Slave_Master.ALL_connect(io.imem, io.soc)
      }.elsewhen(is_dmem & (uart_read | uart_write)){
        if(coreConfig.npc) Slave_Master.ALL_connect(io.dmem, io.uart.get)
      }.elsewhen(is_dmem & (clint_read | clint_write)){
        Slave_Master.ALL_connect(io.dmem, io.clint)
      }.elsewhen(is_dmem){
         Slave_Master.ALL_connect(io.dmem, io.soc)
      }
    }
    is(s_IFUREAD){
      Slave_Master.ALL_connect(io.imem, io.soc)
    }
    is(s_LSUREAD){
      Slave_Master.ALL_connect(io.dmem, io.soc)
    }
    is(s_CLINT){
      Slave_Master.ALL_connect(io.dmem, io.clint)
    }
    is(s_UART){
      if(coreConfig.npc) Slave_Master.ALL_connect(io.dmem, io.uart.get)
    }
  }
}