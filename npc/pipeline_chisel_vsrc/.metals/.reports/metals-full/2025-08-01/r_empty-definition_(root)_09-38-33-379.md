file://<WORKSPACE>/src/main/scala/top.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:

offset: 612
uri: file://<WORKSPACE>/src/main/scala/top.scala
text:
```scala
import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class Top extends Module {
    val io = IO(new Bundle{
        val in0 = Input(UInt(4.W))
        val in1 = Input(UInt(4.W))
        val in2 = Input(UInt(4.W))
        val in3 = Input(UInt(4.W))
        val sel = Input(UInt(2.W))
        val out = Output(UInt(4.W))
    })

    io.out := MuxLookup(io.sel, 0.U, Seq(
        0.U -> io.in0,
        1.U -> io.in1,
        2.U -> io.in2,
        3.U -> io.in3
    ))
}

object TopMain extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top,
    firtoolOpts = Array("-disable-all-@@randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 