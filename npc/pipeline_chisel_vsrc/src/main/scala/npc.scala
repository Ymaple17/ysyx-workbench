import chisel3._
import chisel3.util._
import bus._
import bus.Master_Slave._
import core._

class NPC_IO(conf: CoreConfig) extends Bundle{
  val interrupt = Input(Bool())
  val master = if(conf.ysyxsoc) Some(new AXI4Master) else None
  val slave = if(conf.ysyxsoc) Some(new AXI4Slave) else None
  val ebreak = if(!conf.useDPIC) Some(Output(Bool())) else None
}

class ysyx_25020039(val coreConfig : CoreConfig) extends Module {
  val io = IO(new NPC_IO(coreConfig))
  if(coreConfig.ysyxsoc) {
    io.master.get.setDefaults()
    io.slave.get.setDefaults()
  }

  val core = Module(new Core(coreConfig))
  val xbar = Module(new Xbar(coreConfig))
  val clint = Module(new Clint(coreConfig))
  val uart = if(coreConfig.npc) Some(Module(new UartWrapper)) else None

  if(!coreConfig.useDPIC) io.ebreak.get := core.io.ebreak.get

  ALL_connect(core.io.imem ,xbar.io.imem)
  ALL_connect(core.io.dmem ,xbar.io.dmem)
  ALL_connect(xbar.io.clint ,clint.io)
  if(coreConfig.npc) {
    ALL_connect(xbar.io.uart.get, uart.get.io)
  }
  if(coreConfig.ysyxsoc){
    xbar.io.soc <> io.master.get
  }
  else {
    val sram = Module(new SRAM)
    ALL_connect(xbar.io.soc ,sram.io.axi)
  }
}