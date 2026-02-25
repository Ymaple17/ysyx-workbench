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

    import CSR_REG._

    val rf_mtvec = RegInit(0.U(conf.xlen.W))
    val rf_mepc  = RegInit(0.U(conf.xlen.W))

    val w_mstatus   = 0x1800.U(conf.xlen.W)
    val w_mcause    = 0xb.U(conf.xlen.W)
    val w_mvendorid = "h79737978".U(conf.xlen.W)
    val w_marchid   = 0x25020039.U(conf.xlen.W)

    when(io.irq) {
        rf_mepc := io.irq_pc
    }.elsewhen(io.write.wen) {
        switch(io.write.waddr) {
            is(MTVEC) { rf_mtvec := io.write.wdata }
            is(MEPC)  { rf_mepc  := io.write.wdata }
        }
    }
    io.read.rdata := MuxLookup(io.read.raddr, 0.U)(Seq(
        MSTATUS   -> w_mstatus,
        MTVEC     -> rf_mtvec,
        MEPC      -> rf_mepc,
        MCAUSE    -> w_mcause,
        MVENDORID -> w_mvendorid,
        MARCHID   -> w_marchid,
    ))

    io.read.mtvec := rf_mtvec
    io.read.mepc  := rf_mepc
}