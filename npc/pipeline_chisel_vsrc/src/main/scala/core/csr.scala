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
    val irq_no = Input(UInt(8.W))
    val irq_pc = Input(UInt(32.W))
    val read  = new CSR_READ_IO(xlen)
    val write = new CSR_WRITE_IO(xlen)
}

class CSR(conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_CSR"
    import CSR_REG._
    val io = IO(new CSR_IO(conf.xlen))
    val xlen = conf.xlen

    val mstatus   = WireDefault(0x1800.U(xlen.W))
    val mvendorid = WireDefault("h79737978".U(xlen.W))
    val marchid   = WireDefault(0x25020039.U(xlen.W))
    val mtvec     = RegInit(0.U(xlen.W))
    val mepc      = RegInit(0.U(xlen.W))
    val mcause    = RegInit(0.U(xlen.W))

    val in_waddr = MuxLookup(io.write.waddr, csr_none)(Seq(
        MSTATUS   -> csr_mstatus,
        MTVEC     -> csr_mtvec,
        MEPC      -> csr_mepc,
        MCAUSE    -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID   -> csr_marchid
    ))
    val in_raddr = MuxLookup(io.read.raddr, csr_none)(Seq(
        MSTATUS   -> csr_mstatus,
        MTVEC     -> csr_mtvec,
        MEPC      -> csr_mepc,
        MCAUSE    -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID   -> csr_marchid
    ))

    io.read.rdata := MuxLookup(in_raddr, 0.U)(Seq(
        csr_mstatus   -> mstatus,
        csr_mtvec     -> mtvec,
        csr_mepc      -> mepc,
        csr_mcause    -> mcause,
        csr_mvendorid -> mvendorid,
        csr_marchid   -> marchid
    ))
    io.read.mtvec := mtvec
    io.read.mepc  := mepc

    when(io.write.wen && !io.irq) {
        when(in_waddr === csr_mtvec)  { mtvec  := io.write.wdata }
        when(in_waddr === csr_mepc)   { mepc   := io.write.wdata }
        when(in_waddr === csr_mcause) { mcause := io.write.wdata }
    }
    when(io.irq) {
        mcause := io.irq_no
        mepc   := io.irq_pc
    }
}