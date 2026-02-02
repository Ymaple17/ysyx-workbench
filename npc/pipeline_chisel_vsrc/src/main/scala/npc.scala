import chisel3._
import chisel3.util._
import bus._
import bus.Master_Slave._
import core._

class NPC_IO extends Bundle{
  val interrupt = Input(Bool())
  val master = new AXI4Master
  val slave = new AXI4Slave
}

class ysyx_25020039(val coreConfig : CoreConfig) extends Module {
  val io = IO(new NPC_IO)
  io.master.setDefaults()
  io.slave.setDefaults()

  val core = Module(new Core(coreConfig))
  val xbar = Module(new Xbar)
  val clint = Module(new Clint(coreConfig))

  ALL_connect(core.io.imem ,xbar.io.imem)
  ALL_connect(core.io.dmem ,xbar.io.dmem)
  ALL_connect(xbar.io.clint ,clint.io)
  if(coreConfig.ysyxsoc){
    xbar.io.soc <> io.master
  }
  else {
    val sram = Module(new SRAM)
    ALL_connect(xbar.io.soc ,sram.io.axi)
  }
}