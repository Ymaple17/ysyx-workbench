file://<WORKSPACE>/chisel_vsrc/src/main/scala/idu.scala
### java.lang.OutOfMemoryError: Java heap space

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 5770
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/idu.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._
import common.Inst._

// Instruction Decode Unit
class IDU extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new IFIDBundle))
    val out = Decoupled(new IDEXBundle)
    
    // 寄存器文件读端口
    val rf_raddr1 = Output(UInt(5.W))
    val rf_raddr2 = Output(UInt(5.W))
    val rf_rdata1 = Input(UInt(WORD_LEN.W))
    val rf_rdata2 = Input(UInt(WORD_LEN.W))
  })

  // 修改：当输入和输出都就绪时才握手
  io.out.valid := RegNext(io.in.valid && io.in.ready)
  io.in.ready := io.out.ready

  val inst = io.in.bits.inst
  val pc = io.in.bits.pc

  // 寄存器地址提取
  val rs1_addr = inst(19, 15)
  val rs2_addr = inst(24, 20)
  val wb_addr = inst(11, 7)
  val funct3 = inst(14, 12)

  // 寄存器文件接口
  io.rf_raddr1 := rs1_addr
  io.rf_raddr2 := rs2_addr

  val rs1_data = Mux(rs1_addr === 0.U, 0.U, io.rf_rdata1)
  val rs2_data = Mux(rs2_addr === 0.U, 0.U, io.rf_rdata2)

  // 立即数生成
  val imm_i = inst(31, 20)
  val imm_i_sext = Cat(Fill(20, imm_i(11)), imm_i)
  val imm_s = Cat(inst(31, 25), inst(11, 7))
  val imm_s_sext = Cat(Fill(20, imm_s(11)), imm_s)
  val imm_b_sext = Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val imm_j = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21))
  val imm_j_sext = Cat(Fill(11, imm_j(19)), imm_j, 0.U(1.W))
  val imm_u_sext = Cat(inst(31, 12), 0.U(12.W))
  val imm_z_sext = Cat(Fill(27, 0.U), inst(19, 15))

  // 控制信号生成
  val List(exe_fun, op1_sel, op2_sel, wb_sel, mem_wen, rf_wen, csr_cmd,mem_en) = ListLookup(
    inst,
    List(ALU_NONE, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
    Array(
      LW -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_MEM, MEM_NONE, REN_SCALAR, CSR_NONE,EN_HAVE),
      LB -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_MEM, MEM_NONE, REN_SCALAR, CSR_NONE,EN_HAVE),
      LBU -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_MEM, MEM_NONE, REN_SCALAR, CSR_NONE,EN_HAVE),
      LH -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_MEM, MEM_NONE, REN_SCALAR, CSR_NONE,EN_HAVE),
      LHU -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_MEM, MEM_NONE, REN_SCALAR, CSR_NONE,EN_HAVE),
      SW -> List(ALU_ADD, OP1_RS1, OP2_IMS, WB_NONE, MEM_WORD, REN_NONE, CSR_NONE,EN_HAVE),
      SB -> List(ALU_ADD, OP1_RS1, OP2_IMS, WB_NONE, MEM_BYTE, REN_NONE, CSR_NONE,EN_HAVE),
      SH -> List(ALU_ADD, OP1_RS1, OP2_IMS, WB_NONE, MEM_HALF, REN_NONE, CSR_NONE,EN_HAVE),
      ADD -> List(ALU_ADD, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      ADDI -> List(ALU_ADD, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SUB -> List(ALU_SUB, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      AND -> List(ALU_AND, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      OR -> List(ALU_OR, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      XOR -> List(ALU_XOR, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      ANDI -> List(ALU_AND, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      ORI -> List(ALU_OR, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      XORI -> List(ALU_XOR, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLL -> List(ALU_SLL, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SRL -> List(ALU_SRL, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SRA -> List(ALU_SRA, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLLI -> List(ALU_SLL, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SRLI -> List(ALU_SRL, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SRAI -> List(ALU_SRA, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLT -> List(ALU_SLT, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLTU -> List(ALU_SLTU, OP1_RS1, OP2_RS2, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLTI -> List(ALU_SLT, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      SLTIU -> List(ALU_SLTU, OP1_RS1, OP2_IMI, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      LUI -> List(ALU_ADD, OP1_NONE, OP2_IMU, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      AUIPC -> List(ALU_ADD, OP1_PC, OP2_IMU, WB_ALU, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      BEQ -> List(BR_BEQ, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      BNE -> List(BR_BNE, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      BGE -> List(BR_BGE, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      BGEU -> List(BR_BGEU, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      BLT -> List(BR_BLT, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      BLTU -> List(BR_BLTU, OP1_RS1, OP2_RS2, WB_NONE, MEM_NONE, REN_NONE, CSR_NONE,EN_NONE),
      JAL -> List(ALU_ADD, OP1_PC, OP2_IMJ, WB_PC, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      JALR -> List(ALU_JALR, OP1_RS1, OP2_IMI, WB_PC, MEM_NONE, REN_SCALAR, CSR_NONE,EN_NONE),
      CSRRW -> List(ALU_RS1, OP1_RS1, OP2_NONE, WB_CSR, MEM_NONE, REN_SCALAR, CSR_W,EN_NONE),
      CSRRS -> List(ALU_RS1, OP1_RS1, OP2_NONE, WB_CSR, MEM_NONE, REN_SCALAR, CSR_S,EN_NONE),
      CSRRC -> List(ALU_RS1, OP1_RS1, OP2_NONE, WB_CSR, MEM_NONE, REN_SCALAR, CSR_C,EN_NONE),
      ECALL -> List(ALU_NONE, OP1_NONE, OP2_NONE, WB_NONE, MEM_NONE, REN_NONE, CSR_E,EN_NONE)
    )
  )

  // 操作数选择
  val op1_data = MuxCase(rs1_data, Seq(
    (op1_sel === OP1_PC) -> pc,
    (op1_sel === OP1_IMZ) -> imm_z_sext
  ))

  val op2_data = MuxCase(rs2_data, Seq(
    (op2_sel === OP2_IMI) -> imm_i_sext,
    (op2_sel === OP2_IMS) -> imm_s_sext,
    (op2_sel === @@OP2_IMB) -> imm_b_sext,
    (op2_sel === OP2_IMU) -> imm_u_sext,
    (op2_sel === OP2_IMJ) -> imm_j_sext
  ))

  // 输出赋值
  io.out.bits.pc := pc
  io.out.bits.inst := inst
  io.out.bits.rs1_data := rs1_data
  io.out.bits.rs2_data := rs2_data
  io.out.bits.op1_data := op1_data
  io.out.bits.op2_data := op2_data
  io.out.bits.exe_fun := exe_fun
  io.out.bits.wb_sel := wb_sel
  io.out.bits.mem_wen := mem_wen
  io.out.bits.mem_en := mem_en
  io.out.bits.rf_wen := rf_wen
  io.out.bits.wb_addr := wb_addr
  io.out.bits.funct3 := funct3
  io.out.bits.imm_b_sext := imm_b_sext
  io.out.bits.csr_addr := Mux(csr_cmd === CSR_E, 0x342.U, inst(31, 20))
  io.out.bits.csr_cmd := csr_cmd
}
```



#### Error stacktrace:

```

```
#### Short summary: 

java.lang.OutOfMemoryError: Java heap space