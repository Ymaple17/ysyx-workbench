error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/DpiMemory.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/DpiMemory.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/dmem_wdata.
	 -chisel3/dmem_wdata#
	 -chisel3/dmem_wdata().
	 -chisel3/util/dmem_wdata.
	 -chisel3/util/dmem_wdata#
	 -chisel3/util/dmem_wdata().
	 -common/Consts.dmem_wdata.
	 -common/Consts.dmem_wdata#
	 -common/Consts.dmem_wdata().
	 -dmem_wdata.
	 -dmem_wdata#
	 -dmem_wdata().
	 -scala/Predef.dmem_wdata.
	 -scala/Predef.dmem_wdata#
	 -scala/Predef.dmem_wdata().
offset: 459
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/DpiMemory.scala
text:
```scala
import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxResource
import common.Consts._


class DpiMemory extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())

    val imem_addr = Input(UInt(WORD_LEN.W))
    val imem_data = Output(UInt(WORD_LEN.W))

    val dmem_addr = Input(UInt(WORD_LEN.W))
    val dmem_wen  = Input(Bool())
    val dmem_en   = Input(Bool())
    val @@dmem_wdata = Input(UInt(WORD_LEN.W))
    val dmem_rdata = Output(UInt(WORD_LEN.W))
    val dmem_wmask = Input(UInt(WMASK_LEN))
    val dmem_ren  = Input(Bool())

    val exit = Input(Bool())
  })

  addResource("/DpiMemory.v")
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.