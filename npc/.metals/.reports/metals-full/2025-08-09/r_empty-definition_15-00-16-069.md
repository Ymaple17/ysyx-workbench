error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/top.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/top.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/npc/io.
	 -chisel3/util/npc/io.
	 -common/Consts.npc.io.
	 -npc/io.
	 -scala/Predef.npc.io.
offset: 965
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/top.scala
text:
```scala
import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxResource
import common.Consts._
import _root_.circt.stage.ChiselStage


class Top extends RawModule {
  override def desiredName: String = "top"
  val clk = IO(Input(Clock()))
  val rst = IO(Input(Bool()))
  val instr = IO(Output(UInt(WORD_LEN.W)))
  val imem_pc = IO(Output(UInt(WORD_LEN.W)))
  val wen = IO(Output(Bool()))
  val mem_addr = IO(Output(UInt(WORD_LEN.W)))

  val npc = withClockAndReset(clk, rst) { Module(new NPC()) }
  
  val dpiMem = Module(new DpiMemory())

  dpiMem.io.clk := clk
  dpiMem.io.rst := rst
  dpiMem.io.exit := npc.io.exit

  dpiMem.io.imem_addr := npc.io.imem_addr
  npc.io.imem_data := dpiMem.io.imem_data

  dpiMem.io.dmem_addr := npc.io.dmem_addr
  dpiMem.io.dmem_wen := npc.io.dmem_wen
  dpiMem.io.dmem_ren := npc.io.dmem_ren
  dpiMem.io.dmem_en := npc.io.dmem_en
  dpiMem.io.dmem_wdata := npc.io.dmem_wdata
  dpiMem.io.dmem_wmask := npc.io.dmem_wmask
  npc.@@io.dmem_rdata := dpiMem.io.dmem_rdata


  instr := npc.io.imem_data
  imem_pc := npc.io.imem_addr
  wen := npc.io.dmem_wen
  mem_addr := npc.io.dmem_addr
}

object TopMain extends App {
  import _root_.circt.stage.ChiselStage
  ChiselStage.emitSystemVerilogFile(
    new Top,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.