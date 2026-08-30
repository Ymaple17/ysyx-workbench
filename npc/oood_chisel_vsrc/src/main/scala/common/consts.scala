package common

import chisel3._
import chisel3.util._

object ALU_OP{
  val ALU_ADD = 0.U(5.W)
  val ALU_SUB = 1.U(5.W)
  val ALU_AND = 2.U(5.W)
  val ALU_CMP = 3.U(5.W)
  val ALU_XOR = 4.U(5.W)
  val ALU_CMPU= 5.U(5.W)
  val ALU_OR  = 6.U(5.W)
  val ALU_SRA = 7.U(5.W)
  val ALU_SRL = 8.U(5.W)
  val ALU_SLL = 9.U(5.W)

  val ALU_MUL = 10.U(5.W)
  val ALU_MULH = 11.U(5.W)
  val ALU_MULHSU = 12.U(5.W)
  val ALU_MULHU = 13.U(5.W)
  val ALU_DIV = 14.U(5.W)
  val ALU_DIVU = 15.U(5.W)
  val ALU_REM = 16.U(5.W)
  val ALU_REMU = 17.U(5.W)

  val ALU_NONE =18.U(5.W)
}

object MEM_READ{
    val RNONE = 0.U(3.W)
    val RBYTE = 1.U(3.W)
    val RHALF = 2.U(3.W)
    val RWORD = 3.U(3.W)
    val RBYTEU = 4.U(3.W)
    val RHALFU = 5.U(3.W)
}

object MEM_WMASK{
    val WNONE = "b00000001".U(8.W)
    val WBYTE = "b00000001".U(8.W)
    val WHALF = "b00000011".U(8.W)
    val WWORD = "b00001111".U(8.W)
}

object MEM_WRITE_CTRL{
    val MEM_WRITE = 1.U(1.W)
    val NONE_MEM_WRITE = 0.U(1.W)
}

object MEM_VALID_CTRL{
    val MEM_VALID = 1.U(1.W)
    val NONE_VALID = 0.U(1.W)
}

object CSR_WRITE_CTRL{
    val CSR_WRITE = 1.U(1.W)
    val NONE_CSR_WRITE = 0.U(1.W)
}

object REG_WRITE_CTRL{
    val REG_WRITE = 1.U(1.W)
    val NONE_REG_WRITE = 0.U(1.W)
}

object REG1_READ_CTRL{
    val REG1_READ = 1.U(1.W)
    val NONE_REG1_READ = 0.U(1.W)
}

object REG2_READ_CTRL{
    val REG2_READ = 1.U(1.W)
    val NONE_REG2_READ = 0.U(1.W)
}

object REG_WRITE_SEL{
    val NONE_SEL = 0.U(3.W)
    val ALU_SEL = 1.U(3.W)
    val IMM_SEL = 2.U(3.W)
    val PC4_SEL = 3.U(3.W)
    val MEM_SEL = 4.U(3.W)
    val CSR_DATA = 5.U(3.W)
}

object CSR_SEL {
    val CSR_NONE = 0.U(2.W)
    val CSR_RD1 = 1.U(2.W)
    val CSR_XOR = 2.U(2.W)
    val CSR_PC  = 3.U(2.W)
}

object IMM_TYPE{
    val ImmX = 0.U(3.W)
    val ImmI = 0.U(3.W)
    val ImmU = 1.U(3.W)
    val ImmJ = 2.U(3.W)
    val ImmS = 3.U(3.W)
    val ImmB = 4.U(3.W)
}

object ALU_SRCA{
    val ALU_A_NONE = 0.U(2.W)
    val ALU_A_RD1 = 0.U(2.W)
    val ALU_A_PC = 1.U(2.W)
}

object ALU_SRCB{
    val ALU_B_NONE = 0.U(2.W)
    val ALU_B_RD2 = 0.U(2.W)
    val ALU_B_IMM = 1.U(2.W)
}

object JUMP_TYPE{
    val JUMP_NONE = "b1111".U(4.W)
    val JUMP_BEQ = "b0000".U(4.W)
    val JUMP_BNE = "b0001".U(4.W)
    val JUMP_BLT = "b0100".U(4.W)
    val JUMP_BGE = "b0101".U(4.W)
    val JUMP_BLTU = "b0110".U(4.W)
    val JUMP_BGEU = "b0111".U(4.W)
    val JUMP_JAL = "b1000".U(4.W)
    val JUMP_JALR = "b1001".U(4.W)
    val JUMP_MERT = "b1010".U(4.W)
}

object PC_SEL{
    val PC_NONE = 0.U(3.W)
    val PC_PLUS4 = 0.U(3.W)
    val PC_IMM = 1.U(3.W)
    val PC_RS2 = 2.U(3.W)
    val MTVEC = 3.U(3.W)
    val MEPC = 4.U(3.W)
}

object IRQ_CTRL{
    val IRQ = 1.U(1.W)
    val NONE_IRQ = 0.U(1.W)

    val IRQ_NONE = 0.U(8.W)
    val IRQECALL = "b00001011".U(8.W)
    val IRQ_IAF = "b00000001".U(8.W)
    val IRQ_LAF = "b00000101".U(8.W)
    val IRQ_SAF = "b00000111".U(8.W)
    // 4g：Machine External Interrupt（interrupt bit | code=11）
    val IRQ_MEXT = "h8000000b".U(32.W)
}

object FENCEI_CTRL{
    val IS_FENCEI = 1.U(1.W)
    val NONE_FENCEI = 0.U(1.W)
}

object BPU_Config{
    val BHT_SIZE = 1024 // base gshare/bimodal entries; independent of history length
    val GHR_LENGTH = 32
    val PATH_HISTORY_LENGTH = 32
    val LOCAL_HISTORY_BITS = 12
    val LOCAL_HISTORY_TABLE_SIZE = 256
    val LOCAL_PHT_SIZE = 1024
    val SC_TABLE_SIZE = 256
    val SC_COUNTER_BITS = 5
    val SC_HISTORY_LENGTHS = Seq(0, 8, 32)
    val SC_THRESHOLD = 8
    val LOOP_ITER_BITS = 10
    val TAGE_PROVIDER_BITS = 3
    val ITAGE_PROVIDER_BITS = 3
    val BP_META_WIDTH = GHR_LENGTH + PATH_HISTORY_LENGTH + LOOP_ITER_BITS + 1 +
      TAGE_PROVIDER_BITS + ITAGE_PROVIDER_BITS + LOCAL_HISTORY_BITS + 4
    val RAS_SIZE = 16 //返回地址栈深度
    val BHT_INIT = 1 // weak not-taken; a taken branch becomes predicted-taken after one update
    val BHT_COLD_STATIC = true // cold entries use backward-taken / forward-not-taken
    val TAGGED_BHT_TAG_BITS = 8
    val TAGE_TABLE_SIZE = 256
    val TAGE_HISTORY_LENGTHS = Seq(3, 8, 16, 32)
    val LOOP_TABLE_SIZE = 64
    val LOOP_TAG_BITS = 10
    val LOOP_CONFIDENCE_THRESHOLD = 2
    val INDIRECT_TARGET_SIZE = 256
    val ITAGE_TABLE_SIZE = 128
    val ITAGE_HISTORY_LENGTHS = Seq(8, 16, 24, 32)
}
