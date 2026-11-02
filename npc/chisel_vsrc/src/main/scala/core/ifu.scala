package core

import chisel3._
import chisel3.util._
import common.IRQ_CTRL._
import core.PerfEvents._
import unit.BPU
import common.BPU_Config._

class IFU_PC_IO extends Bundle{
  val next_pc = Output(UInt(32.W))
}

class IFU_IDU_IO extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  
  val state = Output(new State)

  //bpu
  val bp_valid = Output(Bool())
  val bp_taken = Output(Bool())
  val bp_target = Output(UInt(32.W))
  val bp_index = Output(UInt(log2Ceil(BHT_SIZE).W)) //预测时的 BHT 索引，随指令流传递
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
  val in = Flipped(Decoupled(new IFU_PC_IO))
  val out = Decoupled(new IFU_IDU_IO)
  val pc = Decoupled(new IFU_PC_IO)
  
  val imem = new IFU_ICACHE_IO

  val is_flush = Input(Bool())
  val correct_pc = Input(UInt(32.W))

  val state = Input(new State)

  //bpu
  val bpu_update_valid = Input(Bool())
  val bpu_update_taken = Input(Bool())
  val bpu_update_pc = Input(UInt(32.W))
  val bpu_update_is_branch = Input(Bool())
  val bpu_update_index = Input(UInt(log2Ceil(BHT_SIZE).W)) //预测时的索引（随指令流传回）
  val bpu_update_is_call = Input(Bool()) //被解析的指令是否为函数调用
  val bpu_update_is_ret = Input(Bool()) //被解析的指令是否为函数返回
}

class IFU(val conf: CoreConfig) extends Module{
    // override def desiredName = "ysyx_25020039_IFU"

    val io = IO(new IFU_IO(conf.xlen))

    val pc_init = if(conf.ysyxsoc){ "h3000_0000".U(32.W) }else if(conf.npc){ "h8000_0000".U(32.W)} else { "h0000_0000".U(32.W) }
    val pc_reg = RegInit(pc_init - 4.U)


    val pc_plus4 = Wire(UInt(32.W))
    pc_plus4 := Mux(io.in.valid, io.in.bits.next_pc + 4.U, pc_reg + 4.U)
    
    val idle = Wire(Bool())
    val work = Wire(Bool())
    val ready = Wire(Bool())

    val s_IDLE  :: s_WORK :: s_FLUSH ::Nil = Enum(3)
    val state = RegInit(s_IDLE)
    val next_state = WireDefault(state)

    next_state := MuxLookup(state, s_IDLE)(Seq(
        s_IDLE   -> Mux(idle, Mux(work, s_IDLE, s_WORK), s_IDLE),
        s_WORK   -> Mux(io.is_flush, s_FLUSH, Mux(work, s_IDLE, s_WORK)),
        s_FLUSH  -> Mux(io.imem.rvalid, s_IDLE, s_FLUSH)
    ))
    state := next_state

    when(io.in.valid && io.in.ready) {
      pc_reg := io.in.bits.next_pc
    }

    io.imem.arvalid := idle && state === s_IDLE
    io.imem.rready := work || (state === s_FLUSH)

    io.imem.araddr := io.in.bits.next_pc
    
    
    io.out.bits.pc := io.in.bits.next_pc
    
    
    io.out.bits.inst := io.imem.rdata

    //bpu（默认启用）
    val bpu = Module(new BPU(conf))
    bpu.io.predict_pc := io.in.bits.next_pc
    bpu.io.predict_inst := io.imem.rdata
    bpu.io.update_pc := io.bpu_update_pc
    bpu.io.update_valid := io.bpu_update_valid
    bpu.io.update_taken := io.bpu_update_taken
    bpu.io.update_is_branch := io.bpu_update_is_branch
    bpu.io.update_index := io.bpu_update_index
    bpu.io.update_is_call := io.bpu_update_is_call
    bpu.io.update_is_ret := io.bpu_update_is_ret

    //bpu -> idu
    io.out.bits.bp_valid := bpu.io.bp_valid
    // 关键：bp_taken 必须用 bp_valid 门控！
    // bpu.io.bp_taken 对非分支指令也输出 BHT 值（可能为 1），
    // 若不门控，JALR/普通指令会带着 bp_taken=1 进 EXU，误判"预测正确"而不冲刷
    io.out.bits.bp_taken := bpu.io.bp_valid && bpu.io.bp_taken
    io.out.bits.bp_target := bpu.io.bp_target
    io.out.bits.bp_index := bpu.io.bp_index

    //bpu
    val bp_bit = io.out.valid && io.out.ready && bpu.io.bp_valid && bpu.io.bp_taken
    io.pc.bits.next_pc := Mux(io.is_flush, io.correct_pc, Mux(bp_bit, bpu.io.bp_target, pc_plus4))


    val has_irq = io.imem.rvalid && io.imem.rresp =/= 0.U
    io.out.bits.state.state := Mux(has_irq, true.B, false.B)
    io.out.bits.state.state_num := Mux(has_irq, IRQ_IAF, 0.U)

    ready := io.imem.rvalid && (state =/= s_FLUSH)
    idle := io.in.valid && io.imem.arready && !io.is_flush
    work := ready && io.out.ready
    io.pc.valid := !reset.asBool && io.pc.ready

    //pipeline
    io.in.ready := (!io.in.valid || (ready && io.out.ready) || io.is_flush)
    io.out.valid := ready && io.in.valid

    if(conf.statistics){
      PM(conf, clock, EVENT_IFU_FETCH, 1.U, io.imem.arvalid && io.imem.arready)
      PM(conf, clock, EVENT_IFU_STALL_ICACHE, 1.U, state === s_WORK && !io.imem.rvalid) 
      PM(conf, clock, EVENT_IFU_STALL_IDU, 1.U, state === s_WORK && io.imem.rvalid && !io.out.ready)
    }
}

