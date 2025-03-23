import riscv._
import chisel3._
import chisel3.stage.ChiselStage

object Elaborate extends App {
  (new chisel3.stage.ChiselStage).execute(args, Seq(chisel3.stage.ChiselGeneratorAnnotation(() => new Main())))
}

object Elaborate {
  def main(args: Array[String]): Unit = {
    val targetDir = args.dropWhile(_ != "--target-dir").drop(1).headOption.getOrElse("generated")
    (new ChiselStage).emitVerilog(new Main, Array("--target-dir", targetDir))
  }
}
