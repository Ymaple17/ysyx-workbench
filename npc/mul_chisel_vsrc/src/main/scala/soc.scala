import chisel3._
import chisel3.util._
import core._
import bus._
import sim._

class NPC_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val master  = if (conf.ysyxsoc) Some(new AXI4Master) else None
  val slave   = if (conf.ysyxsoc) Some(new AXI4Slave)  else None
  val ebreak  = if (!conf.useDPIC) Some(Output(Bool())) else None
  val commit_valid = if (conf.npc) Some(Output(Bool())) else None
}

class ysyx_25020039(val coreConfig: CoreConfig) extends Module {
  val io = IO(new NPC_IO(coreConfig))
  if (coreConfig.ysyxsoc) {
    io.master.get.setDefaults()
    io.slave.get.setDefaults()
  }

  val core = Module(new Core(coreConfig))
  val xbar = Module(new Xbar(coreConfig))
  val clint = Module(new Clint(coreConfig))
  val uart = if (coreConfig.npc) Some(Module(new UART)) else None

  if (!coreConfig.useDPIC) io.ebreak.get := core.io.ebreak.get
  if (coreConfig.npc) io.commit_valid.get := core.io.commit_valid.get

  core.io.interrupt := io.interrupt

  //core <-> xbar
  core.io.imem <> xbar.io.imem
  core.io.dmem <> xbar.io.dmem

  //xbar <-> clint/uart
  xbar.io.clint <> clint.io
  uart.foreach(u => xbar.io.uart.get <> u.io)

  //xbar <-> soc/sram
  if (coreConfig.ysyxsoc) {
    xbar.io.soc <> io.master.get
  } else {
    val sram = Module(new SRAM)
    xbar.io.soc <> sram.io.sram
  }
}