package core

import chisel3._
import chisel3.util._
import common.IRQ_CTRL._
import common.OoOParams
import core.PerfEvents._
import unit.BPU
import common.BPU_Config._

class IFU_PC_IO extends Bundle{
  val next_pc = Output(UInt(32.W))
}

class IFU_IDU_IO extends Bundle{
  val inst = UInt(32.W)
  val pc = UInt(32.W)
  val state = new State

  val bp_valid = Bool()
  val bp_taken = Bool()
  val bp_target = UInt(32.W)
  val bp_index = UInt(log2Ceil(BHT_SIZE).W)
}

class IFUPacket extends Bundle {
  val valid = Vec(OoOParams.FETCH_WIDTH, Bool())
  val bits  = Vec(OoOParams.FETCH_WIDTH, new IFU_IDU_IO)
}

class IFU_ICACHE_IO extends Bundle{
  val araddr = Output(UInt(32.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())

  val rvalid = Input(Bool())
  val rvalid1 = Input(Bool())
  val rready = Output(Bool())
  val rdata = Input(UInt(32.W))
  val rdata1 = Input(UInt(32.W))
  val rresp = Input(UInt(2.W))
  val rresp1 = Input(UInt(2.W))
}

class IFU_IO(xlen: Int) extends Bundle{
  val in = Flipped(Decoupled(new IFU_PC_IO))
  val out = Decoupled(new IFUPacket)
  val pc = Decoupled(new IFU_PC_IO)

  val imem = new IFU_ICACHE_IO

  val is_flush = Input(Bool())
  val correct_pc = Input(UInt(32.W))

  val state = Input(new State)
  val slot1_enable = Input(Bool())

  val bpu_update_valid = Input(Bool())
  val bpu_update_taken = Input(Bool())
  val bpu_update_pc = Input(UInt(32.W))
  val bpu_update_target = Input(UInt(32.W))
  val bpu_update_is_branch = Input(Bool())
  val bpu_update_is_jalr = Input(Bool())
  val bpu_update_index = Input(UInt(log2Ceil(BHT_SIZE).W))
  val bpu_update_is_call = Input(Bool())
  val bpu_update_is_ret = Input(Bool())
}

class IFU(val conf: CoreConfig) extends Module{
  val io = IO(new IFU_IO(conf.xlen))

  val pc_init =
    if(conf.ysyxsoc){ "h3000_0000".U(32.W) }
    else if(conf.npc){ "h8000_0000".U(32.W)}
    else { "h0000_0000".U(32.W) }
  val pc_reg = RegInit(pc_init - 4.U)

  val idle = Wire(Bool())
  val work = Wire(Bool())
  val ready = Wire(Bool())

  val s_IDLE  :: s_WORK :: s_FLUSH :: Nil = Enum(3)
  val state = RegInit(s_IDLE)
  val next_state = WireDefault(state)

  next_state := MuxLookup(state, s_IDLE)(Seq(
    s_IDLE   -> Mux(io.is_flush, s_IDLE, Mux(idle, Mux(work, s_IDLE, s_WORK), s_IDLE)),
    s_WORK   -> Mux(io.is_flush, Mux(io.imem.rvalid, s_IDLE, s_FLUSH), Mux(work, s_IDLE, s_WORK)),
    s_FLUSH  -> Mux(io.imem.rvalid, s_IDLE, s_FLUSH)
  ))
  state := next_state

  when(io.is_flush) {
    pc_reg := io.correct_pc
  }.elsewhen(io.in.valid && io.in.ready) {
    pc_reg := io.in.bits.next_pc
  }

  val fetchPc = io.in.bits.next_pc
  val fetchPc1 = fetchPc + 4.U

  io.imem.arvalid := idle && state === s_IDLE
  io.imem.rready := work || (state === s_FLUSH) || (state === s_WORK && io.is_flush)
  io.imem.araddr := fetchPc

  val bpu = Module(new BPU(conf))
  bpu.io.predict_pc := fetchPc
  bpu.io.predict_inst := io.imem.rdata
  bpu.io.predict_pc1 := fetchPc1
  bpu.io.predict_inst1 := io.imem.rdata1
  bpu.io.update_pc := io.bpu_update_pc
  bpu.io.update_target := io.bpu_update_target
  bpu.io.update_valid := io.bpu_update_valid
  bpu.io.update_taken := io.bpu_update_taken
  bpu.io.update_is_branch := io.bpu_update_is_branch
  bpu.io.update_is_jalr := io.bpu_update_is_jalr
  bpu.io.update_index := io.bpu_update_index
  bpu.io.update_is_call := io.bpu_update_is_call
  bpu.io.update_is_ret := io.bpu_update_is_ret

  val slot0Taken = bpu.io.bp_valid && bpu.io.bp_taken
  val slot0Fault = io.imem.rresp =/= 0.U
  val slot1RawUse = io.imem.rvalid1 && !slot0Taken && !slot0Fault
  val slot1CanUse = io.slot1_enable && slot1RawUse
  val slot1Taken = slot1CanUse && bpu.io.bp1_valid && bpu.io.bp1_taken
  val seqNext = Mux(slot1CanUse, fetchPc + 8.U, fetchPc + 4.U)

  for (i <- 0 until OoOParams.FETCH_WIDTH) {
    io.out.bits.valid(i) := false.B
    io.out.bits.bits(i) := 0.U.asTypeOf(new IFU_IDU_IO)
  }

  io.out.bits.valid(0) := ready && io.in.valid
  io.out.bits.bits(0).pc := fetchPc
  io.out.bits.bits(0).inst := io.imem.rdata
  io.out.bits.bits(0).state := io.state
  io.out.bits.bits(0).state.state := slot0Fault
  io.out.bits.bits(0).state.state_num := Mux(slot0Fault, IRQ_IAF, 0.U)
  io.out.bits.bits(0).bp_valid := bpu.io.bp_valid
  io.out.bits.bits(0).bp_taken := bpu.io.bp_valid && bpu.io.bp_taken
  io.out.bits.bits(0).bp_target := bpu.io.bp_target
  io.out.bits.bits(0).bp_index := bpu.io.bp_index

  io.out.bits.valid(1) := ready && io.in.valid && slot1CanUse
  io.out.bits.bits(1).pc := fetchPc1
  io.out.bits.bits(1).inst := io.imem.rdata1
  io.out.bits.bits(1).state := io.state
  io.out.bits.bits(1).state.state := io.imem.rresp1 =/= 0.U
  io.out.bits.bits(1).state.state_num := Mux(io.imem.rresp1 =/= 0.U, IRQ_IAF, 0.U)
  io.out.bits.bits(1).bp_valid := bpu.io.bp1_valid
  io.out.bits.bits(1).bp_taken := bpu.io.bp1_valid && bpu.io.bp1_taken
  io.out.bits.bits(1).bp_target := bpu.io.bp1_target
  io.out.bits.bits(1).bp_index := bpu.io.bp1_index

  val predictedNext = Mux(slot0Taken, bpu.io.bp_target,
    Mux(slot1Taken, bpu.io.bp1_target, seqNext))
  io.pc.bits.next_pc := Mux(io.is_flush, io.correct_pc, predictedNext)

  ready := io.imem.rvalid && (state =/= s_FLUSH)
  idle := io.in.valid && io.imem.arready && !io.is_flush
  work := ready && io.out.ready
  io.pc.valid := !reset.asBool && io.pc.ready

  io.in.ready := (!io.in.valid || work || io.is_flush)
  io.out.valid := ready && io.in.valid

  if(conf.statistics){
    val packetFire = io.out.valid && io.out.ready
    PM(conf, clock, EVENT_IFU_FETCH, 1.U, io.imem.arvalid && io.imem.arready)
    PM(conf, clock, EVENT_IFU_STALL_ICACHE, 1.U, state === s_WORK && !io.imem.rvalid)
    PM(conf, clock, EVENT_IFU_STALL_IDU, 1.U, state === s_WORK && io.imem.rvalid && !io.out.ready)
    PM(conf, clock, EVENT_FETCH_SLOT0_VALID, 1.U, packetFire && io.out.bits.valid(0))
    PM(conf, clock, EVENT_FETCH_SLOT1_VALID, 1.U, packetFire && io.out.bits.valid(1))
    PM(conf, clock, EVENT_FETCH_SLOT1_KILLED, 1.U,
      ready && io.in.valid && io.imem.rvalid1 && slot0Taken)
    PM(conf, clock, EVENT_BPU_TAGGED_HIT, 1.U,
      packetFire && ((io.out.bits.valid(0) && bpu.io.bp_tagged_hit) ||
        (io.out.bits.valid(1) && bpu.io.bp1_tagged_hit)))
    PM(conf, clock, EVENT_BPU_INDIRECT_HIT, 1.U,
      packetFire && ((io.out.bits.valid(0) && bpu.io.bp_indirect_hit) ||
        (io.out.bits.valid(1) && bpu.io.bp1_indirect_hit)))
    val redirectBubble = RegNext(io.is_flush, false.B) && !packetFire
    PM(conf, clock, EVENT_FETCH_REDIRECT_BUBBLE, 1.U, redirectBubble)
  }
}
