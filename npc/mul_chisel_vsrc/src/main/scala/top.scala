package core

import chisel3._
import _root_.circt.stage.ChiselStage

class top extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val ebreak = Output(Bool())
    val imem_pc = Output(UInt(32.W))
    val instr = Output(UInt(32.W))
    val wen = Output(Bool())
    val mem_addr = Output(UInt(32.W))
    val mem_valid = Output(Bool())
  })
  val core = Module(new Core(NPC_Config()))
  core.io.interrupt := io.interrupt
  io.ebreak := core.io.ebreak
  io.imem_pc := core.io.imem_pc
  io.instr := core.io.instr
  io.wen := core.io.wen
  io.mem_addr := core.io.mem_addr
  io.mem_valid := core.io.mem_valid
}

object TopMain extends App {
  import _root_.circt.stage.ChiselStage
  ChiselStage.emitSystemVerilogFile(
    new top,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
