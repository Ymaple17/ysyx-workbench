package core

import chisel3._
import chisel3.util._

object CSR_REG{
    val MSTATUS    = 0x300.U(12.W)
    val MTVEC      = 0x305.U(12.W)
    val MEPC       = 0x341.U(12.W)
    val MCAUSE     = 0x342.U(12.W)
    val MVENDORID  = 0xF11.U(12.W)
    val MARCHID    = 0xF12.U(12.W)

    val csr_none        = 0.U(3.W)
    val csr_mstatus     = 0.U(3.W)
    val csr_mtvec       = 1.U(3.W)
    val csr_mepc        = 2.U(3.W)
    val csr_mcause      = 3.U(3.W)
    val csr_mvendorid   = 4.U(3.W)
    val csr_marchid     = 5.U(3.W)
}

class CSR_READ_IO(xlen: Int) extends Bundle{
    val raddr = Input(UInt(12.W))
    val rdata = Output(UInt(xlen.W))
    val mtvec = Output(UInt(xlen.W))
    val mepc  = Output(UInt(xlen.W))
}

class CSR_WRITE_IO(xlen: Int) extends Bundle{
    val wdata = Input(UInt(xlen.W))
    val waddr = Input(UInt(12.W))
    val wen   = Input(Bool())
}

class CSR_IO(xlen: Int) extends Bundle{
    val irq = Input(Bool())
    val irq_no = Input(UInt(xlen.W)) // 4g：支持 0x8000000b 等中断 cause
    val irq_pc = Input(UInt(32.W))
    val read  = new CSR_READ_IO(xlen)
    val read1 = new CSR_READ_IO(xlen)
    val write = new CSR_WRITE_IO(xlen)
}

class CSR(conf: CoreConfig) extends Module{
    import CSR_REG._
    val io = IO(new CSR_IO(conf.xlen))
    val xlen = conf.xlen

    val rf = RegInit(VecInit(Seq(
        0x1800.U(xlen.W),       // mstatus 初始值 MIE=1
        0.U(xlen.W),            // mtvec
        0.U(xlen.W),            // mepc
        0.U(xlen.W),            // mcause
        "h79737978".U(xlen.W),  // mvendorid 只读
        0x25020039.U(xlen.W)    // marchid 只读
    )))

    val in_waddr = Wire(UInt(3.W))
    val in_raddr = Wire(UInt(3.W))
    val in_raddr1 = Wire(UInt(3.W))

    in_waddr := MuxLookup(io.write.waddr, csr_none)(Seq(
        MSTATUS   -> csr_mstatus,
        MTVEC     -> csr_mtvec,
        MEPC      -> csr_mepc,
        MCAUSE    -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID   -> csr_marchid
    ))

    in_raddr := MuxLookup(io.read.raddr, csr_none)(Seq(
        MSTATUS   -> csr_mstatus,
        MTVEC     -> csr_mtvec,
        MEPC      -> csr_mepc,
        MCAUSE    -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID   -> csr_marchid
    ))

    in_raddr1 := MuxLookup(io.read1.raddr, csr_none)(Seq(
        MSTATUS   -> csr_mstatus,
        MTVEC     -> csr_mtvec,
        MEPC      -> csr_mepc,
        MCAUSE    -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID   -> csr_marchid
    ))

    io.read.rdata := rf(in_raddr)
    io.read.mtvec := rf(csr_mtvec)
    io.read.mepc  := rf(csr_mepc)
    io.read1.rdata := rf(in_raddr1)
    io.read1.mtvec := rf(csr_mtvec)
    io.read1.mepc  := rf(csr_mepc)

    val is_ro_csr = (in_waddr === csr_mvendorid) || (in_waddr === csr_marchid)
    when(io.write.wen && !io.irq) {
        rf(in_waddr) := io.write.wdata
    }
    when(io.irq) {
        rf(csr_mcause) := io.irq_no
        rf(csr_mepc) := io.irq_pc
    }
}
