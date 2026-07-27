package core

import chisel3._
import chisel3.util._

class IFU_PC_IO extends Bundle{
  val next_pc = Output(UInt(32.W))
}

class IFU_IDU_IO extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
}

class IFU_ICACHE_IO extends Bundle{
  val araddr = Output(UInt(32.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rdata = Input(UInt(32.W))
  val rresp = Input(UInt(2.W))
  // extra AXI fields for BulkConnect compatibility
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val rlast   = Input(Bool())
  val rid     = Input(UInt(4.W))
  val awaddr  = Output(UInt(32.W))
  val awvalid = Output(Bool())
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))
  val awready = Input(Bool())
  val wdata   = Output(UInt(32.W))
  val wstrb   = Output(UInt(4.W))
  val wvalid  = Output(Bool())
  val wlast   = Output(Bool())
  val wready  = Input(Bool())
  val bresp   = Input(UInt(2.W))
  val bvalid  = Input(Bool())
  val bid     = Input(UInt(4.W))
  val bready  = Output(Bool())
}

class IFU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFU_PC_IO))
  val out = Decoupled(new IFU_IDU_IO)
  val pc = Decoupled(new IFU_PC_IO)
  
  val imem = new IFU_ICACHE_IO
}

class IFU(val conf: CoreConfig) extends Module{

  val io = IO(new IFU_IO(conf.xlen))

  val pc_init = "h8000_0000".U(conf.xlen.W)

  val pc_reg = RegInit(pc_init)
  val inst_reg = RegInit(0.U(conf.xlen.W))

  val s_IDLE :: s_WAIT :: s_DATA :: Nil = Enum(3)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(s_IDLE)

  next_state := MuxLookup(state, s_IDLE)(Seq(
    s_IDLE -> Mux(io.imem.arvalid && io.imem.arready, s_WAIT, s_IDLE),
    s_WAIT -> Mux(io.imem.rvalid, s_DATA, s_WAIT),
    s_DATA -> Mux(io.out.valid && io.out.ready, s_IDLE, s_DATA)
  ))
  state := next_state

  //AR
  io.imem.araddr := pc_reg
  io.imem.arvalid := (state === s_IDLE)
  io.imem.arid := 0.U
  io.imem.arlen := 0.U
  io.imem.arsize := 0.U
  io.imem.arburst := 1.U
  //R
  io.imem.rready := (state === s_WAIT)
  // AW (unused by IFU)
  io.imem.awaddr := 0.U
  io.imem.awvalid := false.B
  io.imem.awid := 0.U
  io.imem.awlen := 0.U
  io.imem.awsize := 0.U
  io.imem.awburst := 0.U
  // W (unused by IFU)
  io.imem.wdata := 0.U
  io.imem.wstrb := 0.U
  io.imem.wvalid := false.B
  io.imem.wlast := false.B
  // B (unused by IFU)
  io.imem.bready := false.B

  when(io.imem.rvalid && io.imem.rready){
    inst_reg := io.imem.rdata
  }

  //IDU
  io.out.bits.inst := inst_reg
  io.out.bits.pc := pc_reg
  io.out.valid := (state === s_DATA)

  val pc_plus4 = pc_reg + 4.U(conf.xlen.W)
  val first_fetched = RegInit(false.B)
  when(io.out.valid && io.out.ready){
    first_fetched := true.B
  }
  val next_pc = Mux(first_fetched && io.in.valid, io.in.bits.next_pc, pc_plus4)

  //PC
  io.pc.bits.next_pc := next_pc
  io.pc.valid := (state === s_DATA) && io.out.ready

  when(io.out.valid && io.out.ready){
    pc_reg := next_pc
  }

  io.in.ready := io.out.valid && io.out.ready

}