import chisel3._
import core._
import _root_.circt.stage.ChiselStage

object TopMain extends App {
  ChiselStage.emitSystemVerilogFile(
    new ysyx_25020039(NPC_Config()),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays"
    )
  )
}

object TopMainSoC extends App {
  ChiselStage.emitSystemVerilogFile(
    new ysyx_25020039(SoC_Config()),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays"
    )
  )
}

object TopMainFPGA extends App {
  ChiselStage.emitSystemVerilogFile(
    new ysyx_25020039(FPGA_Config()),
    args = Array("--target-dir", "generated/fpga"),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays"
    )
  )
}
