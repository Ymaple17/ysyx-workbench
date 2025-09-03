error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/ifu.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/ifu.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/io.
	 -chisel3/util/io.
	 -common/Consts.io.
	 -common/Inst.io.
	 -io.
	 -scala/Predef.io.
offset: 707
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/ifu.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._
import common.Inst._

// Instruction Fetch Unit
class IFU extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new IFIDBundle)
    val imem_addr = Output(UInt(WORD_LEN.W))
    val imem_data = Input(UInt(WORD_LEN.W))
    
    val br_flag = Input(Bool())
    val br_target = Input(UInt(WORD_LEN.W))
    val jalr_flag = Input(Bool())
    val jalr_target = Input(UInt(WORD_LEN.W))
    val ecall_flag = Input(Bool())
    val ecall_target = Input(UInt(WORD_LEN.W))
  })

  val pc = RegInit(START_PC)
  
  when(io.out.fire) {
    pc := MuxCase(pc + 4.U, Seq(
      io.br_flag -> io.br_target,
      io.jalr_flag -> io.jalr_target,
      @@io.ecall_flag -> io.ecall_target
    ))
  }

  io.imem_addr := pc
  
  io.out.valid := true.B
  io.out.bits.pc := pc
  io.out.bits.inst := io.imem_data
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.