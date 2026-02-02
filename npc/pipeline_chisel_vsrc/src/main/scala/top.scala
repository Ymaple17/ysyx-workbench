import chisel3._
import chisel3.util._
import bus._
import core._

object TopMain extends App {
  import _root_.circt.stage.ChiselStage
  ChiselStage.emitSystemVerilogFile(
    new ysyx_25020039(NPC_Config()),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}