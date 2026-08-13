package unit

import chisel3._
import chisel3.util._

object INST_Control{
    import common.ALU_OP._
    import common.Instructions._
    import common.MEM_READ._
    import common.MEM_WMASK._
    import common.MEM_WRITE_CTRL._
    import common.MEM_VALID_CTRL._
    import common.CSR_WRITE_CTRL._
    import common.REG_WRITE_CTRL._
    import common.REG1_READ_CTRL._
    import common.REG2_READ_CTRL._
    import common.REG_WRITE_SEL._
    import common.CSR_SEL._
    import common.IMM_TYPE._
    import common.ALU_SRCA._
    import common.ALU_SRCB._
    import common.JUMP_TYPE._
    import common.PC_SEL._
    import common.IRQ_CTRL._
    import common.FENCEI_CTRL._

    val default: List[UInt] =
  //                                                    DIV
  //              Jump           MemWrite        Regwrite          CSRWrite    Alu_SrcA  Alu_SrcB   immtype  AluCtrl  MemValid   MemRD   MemWmask Regwrite_SEL CSRWrite  IRQ      IRQ_NONE   FENCEI     RS1_READ          RS2_READ
  //               |               |                 |                |           |          |        |        |         |        |         |         |          |       |           |        |             |                |
        List(JUMP_NONE     , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ)

    val map: Array[(BitPat, List[UInt])] = Array(
    // Arithmetic
    AUIPC -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_PC  , ALU_B_IMM , ImmU, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    ADDI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ADD   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_ADD , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SUB   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SUB , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    LUI   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmU, ALU_NONE, NONE_VALID, RNONE  , WNONE , IMM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    
    //Jump
    JAL   -> List(JUMP_JAL , NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_PC  , ALU_B_IMM , ImmJ, ALU_ADD , NONE_VALID, RNONE  , WNONE , PC4_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    JALR  -> List(JUMP_JALR, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , NONE_VALID, RNONE  , WNONE , PC4_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    
    //Stores    
    SW    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WWORD , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SB    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WBYTE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SH    -> List(JUMP_NONE, MEM_WRITE     , NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmS, ALU_ADD , MEM_VALID , RNONE  , WHALF , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    
    //Loads
    LW    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RWORD  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LH    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RHALF  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LHU   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RHALFU  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LB    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RBYTE  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    LBU   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_ADD , MEM_VALID , RBYTEU  , WWORD , MEM_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    
    //Branch
    BNE   -> List(JUMP_BNE , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_SUB , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BEQ   -> List(JUMP_BEQ , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_SUB , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BLT   -> List(JUMP_BLT , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMP , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BLTU  -> List(JUMP_BLTU, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMPU, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BGE   -> List(JUMP_BGE , NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMP , NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    BGEU  -> List(JUMP_BGEU, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmB, ALU_CMPU, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    
    //Compare
    SLTI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_CMP , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLTIU -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_CMPU, NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLT   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_CMP , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SLTU  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_CMPU, NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    
    // Logical
    AND   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_AND , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    ANDI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_AND , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    OR    -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_OR  , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    ORI   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_OR  , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    XOR   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_XOR , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    XORI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_XOR , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    
    // Shifts
    SLLI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SLL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SLL   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SLL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SRLI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SRL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SRL   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SRL , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),
    SRAI  -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_IMM , ImmI, ALU_SRA , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    SRA   -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , NONE_CSR_WRITE, ALU_A_RD1 , ALU_B_RD2 , ImmX, ALU_SRA , NONE_VALID, RNONE  , WNONE , ALU_SEL , CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , REG2_READ),

    //CSR
    CSRRW -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , CSR_WRITE     , ALU_A_RD1 , ALU_B_NONE, ImmI, ALU_NONE, NONE_VALID, RNONE  , WNONE , CSR_DATA, CSR_RD1 , NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    CSRRS -> List(JUMP_NONE, NONE_MEM_WRITE, REG_WRITE     , CSR_WRITE     , ALU_A_RD1 , ALU_B_NONE, ImmI, ALU_NONE, NONE_VALID, RNONE  , WNONE , CSR_DATA, CSR_XOR , NONE_IRQ, IRQ_NONE, NONE_FENCEI, REG1_READ     , NONE_REG2_READ),
    ECALL -> List(JUMP_NONE, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmI, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE,      IRQ, IRQECALL, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    MRET  -> List(JUMP_MERT, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmI, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, NONE_FENCEI, NONE_REG1_READ, NONE_REG2_READ),
    FENCEI-> List(JUMP_NONE, NONE_MEM_WRITE, NONE_REG_WRITE, NONE_CSR_WRITE, ALU_A_NONE, ALU_B_NONE, ImmX, ALU_NONE, NONE_VALID, RNONE  , WNONE , NONE_SEL, CSR_NONE, NONE_IRQ, IRQ_NONE, IS_FENCEI , NONE_REG1_READ, NONE_REG2_READ),
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
    val irq = Output(Bool())
    val irq_num = Output(UInt(8.W))
}

class Signals extends Bundle{
    val ifu = Irrevocable(new IFU_signals)
    val idu = new IDU_signals
    val exu = new EXU_signals
    val lsu = new LSU_signals
    val wbu = new WBU_signals
} 

class Control_IO extends Bundle{
    val inst = Input(UInt(32.W))
    val signals = new Signals

    val irq = Output(Bool())
    val irq_num = Output(UInt(8.W))
}

class Control extends Module{

    val io = IO(new Control_IO)

    val default = INST_Control.default
    val map = INST_Control.map
    val control = ListLookup(io.inst, default, map)

    //IFU signals
    io.signals.ifu.bits.is_fencei := control(15).asBool
    io.signals.ifu.valid := false.B

    //IDU signals
    io.signals.idu.imm_type := control(6).asUInt
    io.signals.idu.rs1_ren := control(16).asBool
    io.signals.idu.rs2_ren := control(17).asBool

    //EXU signals
    io.signals.exu.alu_srcA := control(4).asUInt
    io.signals.exu.alu_srcB := control(5).asUInt
    io.signals.exu.alu_control := control(7).asUInt
    io.signals.exu.jump := control(0).asUInt

    //LSU signals
    io.signals.lsu.mem_wmask := control(10).asUInt
    io.signals.lsu.mem_rd := control(9).asUInt
    io.signals.lsu.mem_write := control(1).asBool
    io.signals.lsu.mem_valid := control(8).asBool

    //WBU signals
    io.signals.wbu.reg_write := control(2).asBool
    io.signals.wbu.reg_write_sel := control(11).asUInt
    io.signals.wbu.csr_write := control(3).asBool
    io.signals.wbu.csr_sel := control(12).asUInt
    io.signals.wbu.irq := control(13).asBool
    io.signals.wbu.irq_num := control(14).asUInt

    //IRQ
    io.irq := control(13).asBool
    io.irq_num := control(14).asUInt
}