import chisel3._
import chisel3.util._
import bus._
import core._

object TopMain extends App {
  import _root_.circt.stage.ChiselStage
  import java.nio.file.{Paths, Files}
  import java.nio.charset.StandardCharsets

  val verilog = ChiselStage.emitSystemVerilog(
    new ysyx_25020039(NPC_Config()),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowLocalVariables,disallowPackedArrays")
  )
  
  val filePattern = """// ----- 8< ----- FILE "([^"]+)" ----- 8< -----""".r
  val splitVerilog = filePattern.split(verilog)
  val fileNames = filePattern.findAllIn(verilog).matchData.map(_.group(1)).toList
  
  // The first part is the main file
  Files.write(Paths.get("ysyx_25020039.sv"), splitVerilog.head.getBytes(StandardCharsets.UTF_8))
  
  if (splitVerilog.length > 1 && fileNames.length == splitVerilog.length - 1) {
      for ((name, content) <- fileNames.zip(splitVerilog.tail)) {
          val cleanContent = if (content.startsWith("\n")) content.substring(1) else content
          Files.write(Paths.get(name), cleanContent.getBytes(StandardCharsets.UTF_8))
      }
  } else if (fileNames.nonEmpty) {
       println(s"Warning: Split mismatch. Parts: ${splitVerilog.length}, Names: ${fileNames.length}")
  }

}

// object TopMain extends App {
//   import _root_.circt.stage.ChiselStage
//   ChiselStage.emitSystemVerilogFile(
//     new ysyx_25020039,
//     firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
//   )
// }