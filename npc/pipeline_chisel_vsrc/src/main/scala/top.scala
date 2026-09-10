import chisel3._
import core._
import _root_.circt.stage.ChiselStage
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object GenUtil {
  // firtool's string output may contain split markers (FILE "..." lines,
  // bare file names and verification includes). Strip them to produce a
  // single clean SystemVerilog file.
  def cleanup(verilog: String): String = {
    verilog.linesIterator
      .filterNot(_.contains("8< ----- FILE"))
      .filterNot(_.trim.startsWith("`include \"verification/"))
      .filterNot(l => l.trim.matches("^[./][\\w./\\-]+\\.(v|f|sv)$"))
      .mkString("\n")
      .replace("always_comb", "always @(*)")
  }
}

object TopMain extends App {
  val verilog = ChiselStage.emitSystemVerilog(
    new ysyx_25020039(NPC_Config()),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays"
    )
  )
  val fixed = GenUtil.cleanup(verilog).replaceAll("\\brf_32x32\\b", "ysyx_25020039_rf_32x32")
  Files.write(Paths.get("ysyx_25020039.sv"), fixed.getBytes(StandardCharsets.UTF_8))
  println(s"[TopMain] Generated ysyx_25020039.sv (${fixed.linesIterator.length} lines)")
}

object TopMainSoC extends App {
  val verilog = ChiselStage.emitSystemVerilog(
    new ysyx_25020039(SoC_Config()),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays"
    )
  )
  val fixed = GenUtil.cleanup(verilog).replaceAll("\\brf_32x32\\b", "ysyx_25020039_rf_32x32")
  Files.write(Paths.get("ysyx_25020039.sv"), fixed.getBytes(StandardCharsets.UTF_8))
  println(s"[TopMainSoC] Generated ysyx_25020039.sv (${fixed.linesIterator.length} lines)")
}
