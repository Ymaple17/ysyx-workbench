package core

import chisel3._
import chisel3.util._
import bus._
import core._
import IRQ_CTRL._
import core.PM
import core.PerfEvents._

class IFUPC_IO extends Bundle{
  val next_pc = Output(UInt(32.W))
}

class IFU_IDU_IO extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  val state = Output(new State)
}

class IFU_ICACHE_IO extends Bundle{
  val araddr = Output(UInt(32.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rdata = Input(UInt(32.W))
  val rresp = Input(UInt(2.W))
}

class IFU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFUPC_IO))
  val out = Decoupled(new IFU_IDU_IO)

  val pc = Decoupled(new IFUPC_IO)

  val imem = new IFU_ICACHE_IO
  val is_flush = Input(Bool())
  val correct_pc = Input(UInt(32.W))
  
  val state = Input(new State)
}

class IFU(val conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_IFU"

    val io = IO(new IFU_IO(conf.xlen))

    val pc_init = if(conf.ysyxsoc){ "h3000_0000".U(32.W) }else if(conf.npc){ "h8000_0000".U(32.W)} else { "h0000_0000".U(32.W) }
    val pc_reg = RegInit(pc_init)
    when(io.in.valid && io.in.ready) {
      pc_reg := io.in.bits.next_pc
    }

    val first_cycle = RegNext(reset.asBool, true.B)
    val pc_plus4 = Wire(UInt(32.W))
    pc_plus4 := Mux(first_cycle,pc_init,Mux(io.in.valid, io.in.bits.next_pc + 4.U, pc_reg + 4.U))
    val idle = (Wire(Bool()))
    val work = (Wire(Bool()))
    val ready = (Wire(Bool()))

    val s_idle  :: s_work :: s_flush ::Nil = Enum(3)
    val state = RegInit(s_idle)
    val next_state = WireDefault(state)

    next_state := MuxLookup(state, s_idle)(Seq(
        s_idle   -> Mux(idle, Mux(work, s_idle, s_work), s_idle),
        s_work     -> Mux(work, s_idle, Mux(io.is_flush, s_flush, s_work)),
        s_flush   -> Mux(io.imem.rvalid, s_idle, s_flush)
    ))
    state := next_state

    io.imem.arvalid := idle && state === s_idle
    io.imem.rready := work || (state === s_flush)

    io.imem.araddr := Mux(io.in.valid, io.in.bits.next_pc, pc_reg)
    
    
    io.out.bits.pc := Mux(io.in.valid, io.in.bits.next_pc, pc_reg)
    
    
    io.out.bits.inst := io.imem.rdata

    io.pc.bits.next_pc := Mux(io.is_flush, io.correct_pc, pc_plus4)

    val has_irq = io.imem.rvalid && io.imem.rresp =/= 0.U
    io.out.bits.state.state := Mux(has_irq, true.B, false.B)
    io.out.bits.state.state_num := Mux(has_irq, IRQ_IAF, 0.U)

    ready := io.imem.rvalid && (state =/= s_flush)
    idle := io.in.valid && io.imem.arready && !io.is_flush
    work := ready && io.out.ready
    io.pc.valid := ~(reset.asBool) && io.pc.ready

    io.in.ready := (!io.in.valid || (ready && io.out.ready) || io.is_flush)
    io.out.valid := ready && io.in.valid

    if(conf.statistics){
      PM(conf, clock, EVENT_IFU_FETCH, 1.U, io.imem.arvalid && io.imem.arready)
      PM(conf, clock, EVENT_IFU_STALL_ICACHE, 1.U, state === s_work && !io.imem.rvalid) 
      PM(conf, clock, EVENT_IFU_STALL_IDU, 1.U, state === s_work && io.imem.rvalid && !io.out.ready)
    }
} 