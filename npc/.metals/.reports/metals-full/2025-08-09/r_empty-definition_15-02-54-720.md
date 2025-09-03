error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/wbu.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/wbu.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/in_data.
	 -chisel3/util/in_data.
	 -common/Consts.in_data.
	 -common/Inst.in_data.
	 -in_data.
	 -scala/Predef.in_data.
offset: 1433
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/wbu.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._
import common.Inst._

// Write Back Unit
class WBU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new EXWBBundle))
    
    // 寄存器文件写端口
    val rf_waddr = Output(UInt(5.W))
    val rf_wdata = Output(UInt(WORD_LEN.W))
    val rf_wen = Output(Bool())
    
    // CSR 接口
    val csr_rdata = Input(UInt(WORD_LEN.W))
    val csr_waddr = Output(UInt(CSR_ADDR_LEN.W))
    val csr_wdata = Output(UInt(WORD_LEN.W))
    val csr_wen = Output(Bool())
    
    // 内存读数据
    val dmem_rdata = Input(UInt(WORD_LEN.W))
    
    // 控制反馈信号
    val br_flag = Output(Bool())
    val br_target = Output(UInt(WORD_LEN.W))
    val jalr_flag = Output(Bool())
    val jalr_target = Output(UInt(WORD_LEN.W))
    val ecall_flag = Output(Bool())
    val ecall_target = Output(UInt(WORD_LEN.W))
  })

  // 握手信号（总是准备好接收）
  io.in.ready := true.B

  val in_data = io.in.bits

  // Load 数据处理
  val load_data = MuxCase(io.dmem_rdata, Seq(
    (in_data.funct3 === 0.U) -> Cat(Fill(24, io.dmem_rdata(7)), io.dmem_rdata(7, 0)),
    (in_data.funct3 === 1.U) -> Cat(Fill(16, io.dmem_rdata(15)), io.dmem_rdata(15, 0)),
    (in_data.funct3 === 4.U) -> Cat(0.U(24.W), io.dmem_rdata(7, 0)),
    (in_data.funct3 === 5.U) -> Cat(0.U(16.W), io.dmem_rdata(15, 0))
  ))

  // CSR 写数据生成
  val csr_wdata = MuxCase(0.U(WORD_LEN.W), Seq(
    (in_data.csr_cmd === CSR_W) -> in_data.op1_data,
    (@@in_data.csr_cmd === CSR_S) -> (io.csr_rdata | in_data.op1_data),
    (in_data.csr_cmd === CSR_C) -> (io.csr_rdata & ~in_data.op1_data),
    (in_data.csr_cmd === CSR_E) -> 11.U(WORD_LEN.W)
  ))

  // 写回数据选择
  val wb_data = MuxCase(in_data.alu_out, Seq(
    (in_data.wb_sel === WB_MEM) -> load_data,
    (in_data.wb_sel === WB_PC) -> (in_data.pc + 4.U(WORD_LEN.W)),
    (in_data.wb_sel === WB_CSR) -> io.csr_rdata
  ))

  // 寄存器文件写接口
  io.rf_waddr := in_data.wb_addr
  io.rf_wdata := wb_data
  io.rf_wen := io.in.valid && (in_data.rf_wen =/= REN_NONE) && (in_data.wb_addr =/= 0.U)

  // CSR 写接口
  io.csr_waddr := in_data.csr_addr
  io.csr_wdata := csr_wdata
  io.csr_wen := io.in.valid && (in_data.csr_cmd =/= CSR_NONE)

  // 控制反馈信号
  io.br_flag := io.in.valid && in_data.br_flag
  io.br_target := in_data.br_target
  io.jalr_flag := io.in.valid && in_data.jalr_flag
  io.jalr_target := in_data.alu_out
  io.ecall_flag := io.in.valid && (in_data.csr_cmd === CSR_E)
  io.ecall_target := 0x180.U
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.