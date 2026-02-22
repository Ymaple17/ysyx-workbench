package core

import chisel3._
import chisel3.util._

object CSR_REG{
    val MSTATUS = 0x300.U(12.W)
    val MTVEC = 0x305.U(12.W)
    val MEPC = 0x341.U(12.W)
    val MCAUSE = 0x342.U(12.W)
    val MVENDORID = 0xF11.U(12.W)
    val MARCHID = 0xF12.U(12.W)

    val csr_none = 0.U(3.W)
    val csr_mstatus = 0.U(3.W)
    val csr_mtvec = 1.U(3.W)
    val csr_mepc = 2.U(3.W)
    val csr_mcause = 3.U(3.W)
    val csr_mvendorid = 4.U(3.W)
    val csr_marchid = 5.U(3.W)
}

class CSR_READ_IO(xlen: Int) extends Bundle{
    val raddr = Input(UInt(12.W))
    val rdata = Output(UInt(xlen.W))
    val mtvec = Output(UInt(xlen.W))
    val mepc = Output(UInt(xlen.W))
}

class CSR_WRITE_IO(xlen: Int) extends Bundle{
    val wdata = Input(UInt(xlen.W))
    val waddr = Input(UInt(12.W))
    val wen = Input(Bool())
}

class CSR_IO(xlen: Int) extends Bundle{
    val irq = Input(Bool())
    val irq_no = Input(UInt(8.W))
    val irq_pc = Input(UInt(32.W))
    val read = new CSR_READ_IO(xlen)
    val write = new CSR_WRITE_IO(xlen)
}

class CSR(conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_CSR"

    val io = IO(new CSR_IO(conf.xlen))
    val rf = RegInit(VecInit(Seq.fill(6)(0.U(conf.xlen.W))))
    
    import CSR_REG._
    val in_waddr = Wire(UInt(3.W))
    val in_raddr = Wire(UInt(3.W))

    in_waddr := MuxLookup(io.write.waddr, csr_none)(Seq(
        MSTATUS -> csr_mstatus,
        MTVEC -> csr_mtvec,
        MEPC -> csr_mepc,
        MCAUSE -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID -> csr_marchid
    ))

    in_raddr := MuxLookup(io.read.raddr, csr_none)(Seq(
        MSTATUS -> csr_mstatus,
        MTVEC -> csr_mtvec,
        MEPC -> csr_mepc,
        MCAUSE -> csr_mcause,
        MVENDORID -> csr_mvendorid,
        MARCHID -> csr_marchid
    ))

    io.read.rdata := rf(in_raddr)
    io.read.mtvec := rf(csr_mtvec)
    io.read.mepc := rf(csr_mepc)
    rf(csr_mstatus) := 0x1800.U
    rf(csr_mvendorid) := "h79737978".U
    rf(csr_marchid) := 0x25020039.U

    when(io.write.wen & ~(io.irq)){
        rf(in_waddr) := io.write.wdata
    }
    when(io.irq){
        rf(csr_mcause) := io.irq_no.asUInt
        rf(csr_mepc) := io.irq_pc
    }
}