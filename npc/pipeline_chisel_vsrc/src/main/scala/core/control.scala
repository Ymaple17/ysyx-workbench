package core

import chisel3._
import chisel3.util._

object Instructions {
  // Loads
  def LB   =  BitPat("b?????????????????000?????0000011")
  def LH   =  BitPat("b?????????????????001?????0000011")
  def LW   =  BitPat("b?????????????????010?????0000011")
  def LBU  =  BitPat("b?????????????????100?????0000011")
  def LHU  =  BitPat("b?????????????????101?????0000011")
  
  // Stores
  def SB   =  BitPat("b?????????????????000?????0100011")
  def SH   =  BitPat("b?????????????????001?????0100011")
  def SW   =  BitPat("b?????????????????010?????0100011")
  
  // Shifts
  def SLL  =  BitPat("b0000000??????????001?????0110011")
  def SLLI =  BitPat("b0000000??????????001?????0010011")
  def SRL  =  BitPat("b0000000??????????101?????0110011")
  def SRLI =  BitPat("b0000000??????????101?????0010011")
  def SRA  =  BitPat("b0100000??????????101?????0110011")
  def SRAI =  BitPat("b0100000??????????101?????0010011")
  
  // Arithmetic
  def ADD  =  BitPat("b0000000??????????000?????0110011")
  def ADDI =  BitPat("b?????????????????000?????0010011")
  def SUB  =  BitPat("b0100000??????????000?????0110011")
  def LUI  =  BitPat("b?????????????????????????0110111")
  def AUIPC=  BitPat("b?????????????????????????0010111")
  
  // Logical
  def XOR  =  BitPat("b0000000??????????100?????0110011")
  def XORI =  BitPat("b?????????????????100?????0010011")
  def OR   =  BitPat("b0000000??????????110?????0110011")
  def ORI  =  BitPat("b?????????????????110?????0010011")
  def AND  =  BitPat("b0000000??????????111?????0110011")
  def ANDI =  BitPat("b?????????????????111?????0010011")
  
  // Compare
  def SLT  =  BitPat("b0000000??????????010?????0110011")
  def SLTI =  BitPat("b?????????????????010?????0010011")
  def SLTU =  BitPat("b0000000??????????011?????0110011")
  def SLTIU=  BitPat("b?????????????????011?????0010011")
  
  // Branch
  def BEQ  =  BitPat("b?????????????????000?????1100011")
  def BNE  =  BitPat("b?????????????????001?????1100011")
  def BLT  =  BitPat("b?????????????????100?????1100011")
  def BGE  =  BitPat("b?????????????????101?????1100011")
  def BLTU =  BitPat("b?????????????????110?????1100011")
  def BGEU =  BitPat("b?????????????????111?????1100011")
  
  // Jump
  def JAL  =  BitPat("b?????????????????????????1101111")
  def JALR =  BitPat("b?????????????????000?????1100111")
  
  // CSR Access
  def CSRRW = BitPat("b?????????????????001?????1110011")
  def CSRRS = BitPat("b?????????????????010?????1110011")
  def ECALL = BitPat("b00000000000000000000000001110011")
  def EBREAK= BitPat("b00000000000100000000000001110011")
  def MRET  = BitPat("b00110000001000000000000001110011")
  
  //fence.i
  def FENCEI = BitPat("b000000000000_00000_001_00000_0001111")

  def NOP   = BitPat.bitPatToUInt(BitPat("b00000000000000000000000000010011"))
}

object MEM_READ{
    val RNONE = 0.U(3.W)
    val RBYTE = 1.U(3.W)
    val RHALFW = 2.U(3.W)
    val RWORD = 3.U(3.W)
    val RBYTEU = 4.U(3.W)
    val RHALFWU = 5.U(3.W)
}

object MEM_WMASK{
    val WNONE = "b00000001".U(8.W)
    val WBYTE = "b00000001".U(8.W)
    val WHALFW = "b00000011".U(8.W)
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

object CSR_SEL{
    val CSR_NONE = 0.U(2.W)
    val CSR_RD1 = 1.U(2.W)
    val CSR_XOR = 2.U(2.W)  // =/=RD1
    val CSR_PC = 3.U(2.W)
}

object ImmType{
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

object JUMP{
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
}

object FENCEI_CTRL{
    val IS_FENCEI = 1.U(1.W)
    val NONE_FENCEI = 0.U(1.W)
}

object INST_Control{
    import core.ALU_OP._
    import Instructions._
    import MEM_READ._
    import MEM_WMASK._
    import MEM_WRITE_CTRL._
    import MEM_VALID_CTRL._
    import CSR_WRITE_CTRL._
    import REG_WRITE_CTRL._
    import REG1_READ_CTRL._
    import REG2_READ_CTRL._
    import REG_WRITE_SEL._
    import CSR_SEL._
    import ImmType._
    import ALU_SRCA._
    import ALU_SRCB._
    import JUMP._
    import PC_SEL._
    import IRQ_CTRL._
    import FENCEI_CTRL._

    val default: List[UInt] =
  //                                                    
  //              Jump           MemWrite        Regwrite          CSRWrite    Alu_SrcA  Alu_SrcB   immtype  AluCtrl  MemValid   MemRD   MemWmask Regwrite_SEL CSRWrite  IRQ      IRQ_NONE   FENCEI     RS1_READ          RS2_READ
  //               |               |                 |                |           |          |        |        |         |        |         |         |          |       |           |        |             |                |
        List(JUMP_NONE     , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ)

    val map: Array[(BitPat, List[UInt])] = Array(
    LUI   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmU, ALU_NONE, NONE_VALID, RNONE  , WNONE , IMM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    AUIPC -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_PC  , ALU_B_IMM , ImmU, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    JAL   -> List(JUMP_JAL , NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_PC  , ALU_B_IMM , ImmJ, ALU_ADD , NONE_VALID, RNONE  , WNONE , PC4_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    JALR  -> List(JUMP_JALR, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , NONE_VALID, RNONE  , WNONE , PC4_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    BEQ   -> List(JUMP_BEQ , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_SUB , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BNE   -> List(JUMP_BNE , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_SUB , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BLT   -> List(JUMP_BLT , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMP , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BGE   -> List(JUMP_BGE , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMP , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BLTU  -> List(JUMP_BLTU, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMPU, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BGEU  -> List(JUMP_BGEU, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMPU, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    ADDI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLTI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_CMP , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLTIU -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_CMPU, NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    XORI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_XOR , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ORI   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_OR  , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ANDI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_AND , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLLI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SLL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SRLI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SRL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SRAI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SRA , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ADD   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SUB   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SUB , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SLL   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SLL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SLT   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_CMP , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SLTU  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_CMPU, NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    XOR   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_XOR , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SRL   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SRL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SRA   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SRA , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    OR    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_OR  , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    AND   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_AND , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SW    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WWORD , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SH    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WHALFW, NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SB    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WBYTE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    LW    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RWORD  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LH    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RHALFW , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LB    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RBYTE  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LBU   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RBYTEU , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LHU   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RHALFWU, WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ECALL -> List(JUMP_NONE, NONE_MEM_WRITE, NONE_REG_WRITE, CSR_WRITE     , ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_PC  , IRQ     , IRQECALL, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    EBREAK-> List(JUMP_NONE, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    MRET  -> List(JUMP_MERT, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    CSRRW -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , CSR_WRITE     , ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , CSR_DATA, CSR_RD1 , NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    CSRRS -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , CSR_WRITE     , ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , CSR_DATA, CSR_XOR , NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    FENCEI-> List(JUMP_NONE, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, IS_FENCEI  , NONE_REG1_READ, NONE_REG2_READ)
    )
}

class IFU_signals extends Bundle{
    val is_fencei = Output(Bool())
}

class IDU_signals extends Bundle{
    val imm_type = Output(UInt(3.W))
    val rs1_ren = Output(Bool())
    val rs2_ren = Output(Bool()) 
}

class EXU_signals extends Bundle{
    val alu_srcA = Output(UInt(2.W))
    val alu_srcB = Output(UInt(2.W))
    val alu_control = Output(UInt(4.W))
    val jump     = Output(UInt(4.W))
}

class LSU_signals extends Bundle{
    val mem_wmask = Output(UInt(8.W))
    val mem_rd    = Output(UInt(3.W))
    val mem_write = Output(Bool())
    val mem_valid = Output(Bool())
}

class WBU_signals extends Bundle{
    val reg_write = Output(Bool())
    val reg_write_sel = Output(UInt(3.W))
    val csr_write = Output(Bool())
    val csr_sel   = Output(UInt(2.W))
}

class Signals extends Bundle{
    val ifu = Irrevocable(new IFU_signals)
    val idu = new IDU_signals
    val exu = new EXU_signals
    val lsu = new LSU_signals
    val wbu = new WBU_signals
}

class Control_IO extends Bundle{
    val signals = new Signals
    val irq = Output(Bool())
    val irq_num = Output(UInt(8.W))
    val inst = Input(UInt(32.W))
}

class Control(val conf: CoreConfig) extends Module{
      override def desiredName = "ysyx_25020039_Control"
    val io = IO(new Control_IO)
    val default = INST_Control.default
    val map = INST_Control.map
    val control_signals = ListLookup(io.inst, default, map)

    // IFU signals
    io.signals.ifu.bits.is_fencei := control_signals(15).asBool

    // IDU signals
    io.signals.idu.imm_type := control_signals(6)
    io.signals.idu.rs1_ren := control_signals(16).asBool
    io.signals.idu.rs2_ren := control_signals(17).asBool

    // EXU signals
    io.signals.exu.alu_srcA := control_signals(4)
    io.signals.exu.alu_srcB := control_signals(5)
    io.signals.exu.alu_control := control_signals(7)
    io.signals.exu.jump     := control_signals(0)

    // LSU signals
    io.signals.lsu.mem_wmask := control_signals(10)
    io.signals.lsu.mem_rd    := control_signals(9)
    io.signals.lsu.mem_write := control_signals(1).asBool
    io.signals.lsu.mem_valid := control_signals(8).asBool

    // WBU signals
    io.signals.wbu.reg_write := control_signals(2).asBool
    io.signals.wbu.reg_write_sel := control_signals(11)
    io.signals.wbu.csr_write := control_signals(3).asBool
    io.signals.wbu.csr_sel   := control_signals(12)

    // IRQ signals
    io.irq := control_signals(13).asBool
    io.irq_num := control_signals(14)
    io.signals.ifu.valid := false.B // default
}




