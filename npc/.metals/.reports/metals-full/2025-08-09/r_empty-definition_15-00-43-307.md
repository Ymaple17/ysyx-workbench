error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/databundle.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/databundle.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/when.
	 -chisel3/when#
	 -chisel3/when().
	 -chisel3/util/when.
	 -chisel3/util/when#
	 -chisel3/util/when().
	 -common/Consts.when.
	 -common/Consts.when#
	 -common/Consts.when().
	 -when.
	 -when#
	 -when().
	 -scala/Predef.when.
	 -scala/Predef.when#
	 -scala/Predef.when().
offset: 1654
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/databundle.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._

// IF -> ID stage data
class IFIDBundle extends Bundle {
  val pc = UInt(WORD_LEN.W)
  val inst = UInt(WORD_LEN.W)
}

// ID -> EX stage data
class IDEXBundle extends Bundle {
  val pc = UInt(WORD_LEN.W)
  val inst = UInt(WORD_LEN.W)
  val rs1_data = UInt(WORD_LEN.W)
  val rs2_data = UInt(WORD_LEN.W)
  val op1_data = UInt(WORD_LEN.W)
  val op2_data = UInt(WORD_LEN.W)
  val exe_fun = UInt(WORD_LEN.W)
  val wb_sel = UInt(WORD_LEN.W)
  val mem_wen = UInt(WORD_LEN.W)
  val rf_wen = UInt(WORD_LEN.W)
  val mem_en = UInt(WORD_LEN.W)
  val wb_addr = UInt(5.W)
  val funct3 = UInt(3.W)
  val imm_b_sext = UInt(WORD_LEN.W)
  val csr_addr = UInt(CSR_ADDR_LEN.W)
  val csr_cmd = UInt(WORD_LEN.W)
}

// EX -> WB stage data
class EXWBBundle extends Bundle {
  val pc = UInt(WORD_LEN.W)
  val alu_out = UInt(WORD_LEN.W)
  val wb_sel = UInt(WORD_LEN.W)
  val mem_wen = UInt(WORD_LEN.W)
  val rf_wen = UInt(WORD_LEN.W)
  val wb_addr = UInt(5.W)
  val funct3 = UInt(3.W)
  val rs2_data = UInt(WORD_LEN.W)
  val csr_addr = UInt(CSR_ADDR_LEN.W)
  val csr_cmd = UInt(WORD_LEN.W)
  val op1_data = UInt(WORD_LEN.W)
  val br_flag = Bool()
  val br_target = UInt(WORD_LEN.W)
  val jalr_flag = Bool()
}

// Stage connection utility
object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T], arch: String = "single") = {
    if (arch == "single") {
      right.bits := left.bits
      right.valid := left.valid
      left.ready := right.ready
    }
    else if (arch == "multi") {
      val reg_bits = RegInit(0.U.asTypeOf(left.bits))
      val reg_valid = RegInit(false.B)


      @@when(left.fire) {
        reg_bits := left.bits
        reg_valid := true.B
      }.elsewhen(right.ready) {
        reg_valid := false.B
      }

      right.bits := reg_bits
      right.valid := reg_valid
      left.ready := !reg_valid || right.ready
    }
  }
}

class PipeReg[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(gen))
    val out = Decoupled(gen)
  })

  val reg_bits = Reg(chiselTypeOf(io.in.bits))
  val reg_valid = RegInit(false.B)

  when(io.in.fire) {
    reg_bits := io.in.bits
    reg_valid := true.B
  }.elsewhen(io.out.ready) {
    reg_valid := false.B
  }

  io.out.bits := reg_bits
  io.out.valid := reg_valid
  io.in.ready := !reg_valid || io.out.ready
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.