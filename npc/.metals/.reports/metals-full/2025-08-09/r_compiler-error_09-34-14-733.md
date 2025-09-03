file://<WORKSPACE>/chisel_vsrc/src/main/scala/exu.scala
### java.lang.OutOfMemoryError: Java heap space

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 3133
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/exu.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._
import common.Inst._

// Execution Unit
class EXU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new IDEXBundle))
    val out = Decoupled(new EXWBBundle)
    
    // 数据内存接口
    val dmem_addr = Output(UInt(WORD_LEN.W))
    val dmem_wen = Output(Bool())
    val dmem_ren = Output(Bool())
    val dmem_en = Output(Bool())
    val dmem_wdata = Output(UInt(WORD_LEN.W))
    val dmem_wmask = Output(UInt(WMASK_LEN))
    val dmem_rdata = Input(UInt(WORD_LEN.W))
  })

  io.out.valid := RegNext(io.in.valid && io.in.ready)
  io.in.ready := io.out.ready

  val in_data = io.in.bits

  // ALU 计算
  val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (in_data.exe_fun === ALU_ADD) -> (in_data.op1_data + in_data.op2_data),
    (in_data.exe_fun === ALU_SUB) -> (in_data.op1_data - in_data.op2_data),
    (in_data.exe_fun === ALU_AND) -> (in_data.op1_data & in_data.op2_data),
    (in_data.exe_fun === ALU_OR) -> (in_data.op1_data | in_data.op2_data),
    (in_data.exe_fun === ALU_XOR) -> (in_data.op1_data ^ in_data.op2_data),
    (in_data.exe_fun === ALU_SLT) -> Cat(Fill(WORD_LEN - 1, 0.U), (in_data.op1_data.asSInt < in_data.op2_data.asSInt).asUInt),
    (in_data.exe_fun === ALU_SLTU) -> Cat(Fill(WORD_LEN - 1, 0.U), (in_data.op1_data < in_data.op2_data).asUInt),
    (in_data.exe_fun === ALU_SLL) -> (in_data.op1_data << in_data.op2_data(4, 0))(WORD_LEN - 1, 0).asUInt,
    (in_data.exe_fun === ALU_SRL) -> (in_data.op1_data >> in_data.op2_data(4, 0)).asUInt,
    (in_data.exe_fun === ALU_SRA) -> (in_data.op1_data.asSInt >> in_data.op2_data(4, 0)).asUInt,
    (in_data.exe_fun === ALU_JALR) -> ((in_data.op1_data + in_data.op2_data) & (~1.U(WORD_LEN.W))),
    (in_data.exe_fun === ALU_RS1) -> in_data.op1_data
  ))

  // 分支逻辑
  val br_flag = MuxCase(false.B, Seq(
    (in_data.exe_fun === BR_BEQ) -> (in_data.op1_data === in_data.op2_data),
    (in_data.exe_fun === BR_BNE) -> (in_data.op1_data =/= in_data.op2_data),
    (in_data.exe_fun === BR_BLT) -> (in_data.op1_data.asSInt < in_data.op2_data.asSInt),
    (in_data.exe_fun === BR_BGE) -> (in_data.op1_data.asSInt >= in_data.op2_data.asSInt),
    (in_data.exe_fun === BR_BLTU) -> (in_data.op1_data < in_data.op2_data),
    (in_data.exe_fun === BR_BGEU) -> (in_data.op1_data >= in_data.op2_data)
  ))

  val br_target = (in_data.pc + in_data.imm_b_sext)(WORD_LEN - 1, 0)
  val jalr_flag = in_data.wb_sel === WB_PC

  // 内存接口
  io.dmem_addr := alu_out
  io.dmem_wen := in_data.mem_wen =/= 0.U
  io.dmem_ren := in_data.wb_sel === WB_MEM
  io.dmem_en := in_data.mem_en =/= 0.U

  // 写掩码生成
  io.dmem_wmask := MuxCase(0.U(WMASK_LEN), Seq(
    (in_data.mem_wen === MEM_BYTE) -> "b00000001".U(WMASK_LEN),
    (in_data.mem_wen === MEM_HALF) -> "b00000011".U(WMASK_LEN),
    (in_data.mem_wen === MEM_WORD) -> "b00001111".U(WMASK_LEN)
  ))

  // 写数据处理
  io.dmem_wdata := MuxCase(in_data.rs2_data, Seq(
    (in_data.mem_wen === MEM_BYTE) -> Cat(0.U(24.W), in_data.rs2_data(7, 0)),
    (in_data.mem_wen === MEM_HALF) -> Cat(0.U(16.W), in_data.rs2_data(15, 0))
  ))

  // 输出赋值
  io.out.bits.pc := in_data.pc
  io.@@out.bits.alu_out := alu_out
  io.out.bits.wb_sel := in_data.wb_sel
  io.out.bits.mem_wen := in_data.mem_wen
  io.out.bits.rf_wen := in_data.rf_wen
  io.out.bits.wb_addr := in_data.wb_addr
  io.out.bits.funct3 := in_data.funct3
  io.out.bits.rs2_data := in_data.rs2_data
  io.out.bits.csr_addr := in_data.csr_addr
  io.out.bits.csr_cmd := in_data.csr_cmd
  io.out.bits.op1_data := in_data.op1_data
  io.out.bits.br_flag := br_flag
  io.out.bits.br_target := br_target
  io.out.bits.jalr_flag := jalr_flag
}
```



#### Error stacktrace:

```

```
#### Short summary: 

java.lang.OutOfMemoryError: Java heap space