package core

import chisel3._
import chisel3.util._
import common.IRQ_CTRL._
import common.OoOParams
import core.PerfEvents._
import unit.{BPU, BPUUpdateQueue, FetchBuffer, FTQ}
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
  val bp_index = UInt(BP_META_WIDTH.W)
  val ftq_idx = UInt(OoOParams.FTQ_PTR_W.W)
  val ftq_generation = UInt(OoOParams.FTQ_GEN_W.W)
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
  val fetch_buffer_replace_ready = Input(Bool())

  val bpu_update_valid = Input(Bool())
  val bpu_update_taken = Input(Bool())
  val bpu_update_pc = Input(UInt(32.W))
  val bpu_update_target = Input(UInt(32.W))
  val bpu_update_is_branch = Input(Bool())
  val bpu_update_is_jalr = Input(Bool())
  val bpu_update_index = Input(UInt(BP_META_WIDTH.W))
  val bpu_update_is_call = Input(Bool())
  val bpu_update_is_ret = Input(Bool())
  val bpu_update1_valid = Input(Bool())
  val bpu_update1_taken = Input(Bool())
  val bpu_update1_pc = Input(UInt(32.W))
  val bpu_update1_target = Input(UInt(32.W))
  val bpu_update1_is_branch = Input(Bool())
  val bpu_update1_is_jalr = Input(Bool())
  val bpu_update1_index = Input(UInt(BP_META_WIDTH.W))
  val bpu_update1_is_call = Input(Bool())
  val bpu_update1_is_ret = Input(Bool())
  val bpu_update_free = Output(UInt(log2Ceil(9).W))

  val ftq_commit0_valid = Input(Bool())
  val ftq_commit0_idx = Input(UInt(OoOParams.FTQ_PTR_W.W))
  val ftq_commit0_generation = Input(UInt(OoOParams.FTQ_GEN_W.W))
  val ftq_commit1_valid = Input(Bool())
  val ftq_commit1_idx = Input(UInt(OoOParams.FTQ_PTR_W.W))
  val ftq_commit1_generation = Input(UInt(OoOParams.FTQ_GEN_W.W))

  val bp_recover_valid = Input(Bool())
  val bp_recover_ftq_idx = Input(UInt(OoOParams.FTQ_PTR_W.W))
  val bp_recover_ftq_generation = Input(UInt(OoOParams.FTQ_GEN_W.W))
  val bp_recover_pc = Input(UInt(32.W))
  val bp_recover_index = Input(UInt(BP_META_WIDTH.W))
  val bp_recover_target = Input(UInt(32.W))
  val bp_recover_is_jalr = Input(Bool())
  val bp_recover_taken = Input(Bool())
  val bp_recover_is_branch = Input(Bool())
  val bp_recover_is_call = Input(Bool())
  val bp_recover_is_ret = Input(Bool())
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

  io.imem.araddr := fetchPc

  val bpu = Module(new BPU(conf))
  val bpuUpdates = Module(new BPUUpdateQueue(depth = 8))
  val fetchBuffer = Module(new FetchBuffer())
  val ftq = Module(new FTQ())
  bpu.io.predict_pc := fetchPc
  bpu.io.predict_inst := io.imem.rdata
  bpu.io.predict_pc1 := fetchPc1
  bpu.io.predict_inst1 := io.imem.rdata1
  bpuUpdates.io.enq(0).valid := io.bpu_update_valid
  bpuUpdates.io.enq(0).bits.pc := io.bpu_update_pc
  bpuUpdates.io.enq(0).bits.target := io.bpu_update_target
  bpuUpdates.io.enq(0).bits.taken := io.bpu_update_taken
  bpuUpdates.io.enq(0).bits.isBranch := io.bpu_update_is_branch
  bpuUpdates.io.enq(0).bits.isJalr := io.bpu_update_is_jalr
  bpuUpdates.io.enq(0).bits.index := io.bpu_update_index
  bpuUpdates.io.enq(0).bits.isCall := io.bpu_update_is_call
  bpuUpdates.io.enq(0).bits.isRet := io.bpu_update_is_ret
  bpuUpdates.io.enq(1).valid := io.bpu_update1_valid
  bpuUpdates.io.enq(1).bits.pc := io.bpu_update1_pc
  bpuUpdates.io.enq(1).bits.target := io.bpu_update1_target
  bpuUpdates.io.enq(1).bits.taken := io.bpu_update1_taken
  bpuUpdates.io.enq(1).bits.isBranch := io.bpu_update1_is_branch
  bpuUpdates.io.enq(1).bits.isJalr := io.bpu_update1_is_jalr
  bpuUpdates.io.enq(1).bits.index := io.bpu_update1_index
  bpuUpdates.io.enq(1).bits.isCall := io.bpu_update1_is_call
  bpuUpdates.io.enq(1).bits.isRet := io.bpu_update1_is_ret
  io.bpu_update_free := bpuUpdates.io.free

  bpu.io.update_pc := bpuUpdates.io.deq.bits.pc
  bpu.io.update_target := bpuUpdates.io.deq.bits.target
  bpu.io.update_valid := bpuUpdates.io.deq.valid
  bpu.io.update_taken := bpuUpdates.io.deq.bits.taken
  bpu.io.update_is_branch := bpuUpdates.io.deq.bits.isBranch
  bpu.io.update_is_jalr := bpuUpdates.io.deq.bits.isJalr
  bpu.io.update_index := bpuUpdates.io.deq.bits.index
  bpu.io.update_is_call := bpuUpdates.io.deq.bits.isCall
  bpu.io.update_is_ret := bpuUpdates.io.deq.bits.isRet

  fetchBuffer.io.flush := io.is_flush
  fetchBuffer.io.replaceReady := io.fetch_buffer_replace_ready
  ftq.io.flush := io.is_flush && !io.bp_recover_valid
  ftq.io.recoverFlush := io.is_flush && io.bp_recover_valid
  ftq.io.recoverSlot1 := io.bp_recover_pc =/= ftq.io.recover.basePc
  ftq.io.commit0Valid := io.ftq_commit0_valid
  ftq.io.commit0Idx := io.ftq_commit0_idx
  ftq.io.commit0Generation := io.ftq_commit0_generation
  ftq.io.commit1Valid := io.ftq_commit1_valid
  ftq.io.commit1Idx := io.ftq_commit1_idx
  ftq.io.commit1Generation := io.ftq_commit1_generation
  ftq.io.recoverIdx := io.bp_recover_ftq_idx
  ftq.io.recoverGeneration := io.bp_recover_ftq_generation

  val slot0Taken = bpu.io.bp_valid && bpu.io.bp_taken
  val slot0Fault = io.imem.rresp =/= 0.U
  val slot1RawUse = io.imem.rvalid1 && !slot0Taken && !slot0Fault
  val slot1CanUse = io.slot1_enable && slot1RawUse
  val slot1Taken = slot1CanUse && bpu.io.bp1_valid && bpu.io.bp1_taken
  val seqNext = Mux(slot1CanUse, fetchPc + 8.U, fetchPc + 4.U)
  val predictedNext = Mux(slot0Taken, bpu.io.bp_target,
    Mux(slot1Taken, bpu.io.bp1_target, seqNext))

  val fetchPacket = Wire(new IFUPacket)
  fetchPacket := 0.U.asTypeOf(new IFUPacket)
  fetchPacket.valid(0) := ready && io.in.valid
  fetchPacket.bits(0).pc := fetchPc
  fetchPacket.bits(0).inst := io.imem.rdata
  fetchPacket.bits(0).state := io.state
  fetchPacket.bits(0).state.state := slot0Fault
  fetchPacket.bits(0).state.state_num := Mux(slot0Fault, IRQ_IAF, 0.U)
  fetchPacket.bits(0).bp_valid := bpu.io.bp_valid
  fetchPacket.bits(0).bp_taken := bpu.io.bp_valid && bpu.io.bp_taken
  fetchPacket.bits(0).bp_target := bpu.io.bp_target
  fetchPacket.bits(0).bp_index := bpu.io.bp_index
  fetchPacket.bits(0).ftq_idx := ftq.io.allocIdx
  fetchPacket.bits(0).ftq_generation := ftq.io.allocGeneration

  fetchPacket.valid(1) := ready && io.in.valid && slot1CanUse
  fetchPacket.bits(1).pc := fetchPc1
  fetchPacket.bits(1).inst := io.imem.rdata1
  fetchPacket.bits(1).state := io.state
  fetchPacket.bits(1).state.state := io.imem.rresp1 =/= 0.U
  fetchPacket.bits(1).state.state_num := Mux(io.imem.rresp1 =/= 0.U, IRQ_IAF, 0.U)
  fetchPacket.bits(1).bp_valid := bpu.io.bp1_valid
  fetchPacket.bits(1).bp_taken := bpu.io.bp1_valid && bpu.io.bp1_taken
  fetchPacket.bits(1).bp_target := bpu.io.bp1_target
  fetchPacket.bits(1).bp_index := bpu.io.bp1_index
  fetchPacket.bits(1).ftq_idx := ftq.io.allocIdx
  fetchPacket.bits(1).ftq_generation := ftq.io.allocGeneration

  val captureCandidate = ready && io.in.valid && !io.is_flush
  fetchBuffer.io.in.valid := captureCandidate && ftq.io.alloc.ready
  fetchBuffer.io.in.bits := fetchPacket
  ftq.io.alloc.valid := captureCandidate && fetchBuffer.io.in.ready
  ftq.io.alloc.bits.basePc := fetchPc
  ftq.io.alloc.bits.validMask := fetchPacket.valid.asUInt
  ftq.io.alloc.bits.predictedNextPc := predictedNext
  ftq.io.alloc.bits.cfiSlot := Mux(slot0Taken, 0.U,
    Mux(slot1Taken, 1.U, 2.U))
  ftq.io.alloc.bits.ghr := bpu.io.spec_ghr
  ftq.io.alloc.bits.pathHistory := bpu.io.spec_path_history
  ftq.io.alloc.bits.ras := bpu.io.spec_ras
  ftq.io.alloc.bits.rasPtr := bpu.io.spec_ras_ptr
  ftq.io.alloc.bits.rasCount := bpu.io.spec_ras_count
  val captureFire = fetchBuffer.io.in.fire

  io.out.valid := fetchBuffer.io.out.valid
  io.out.bits := fetchBuffer.io.out.bits
  fetchBuffer.io.out.ready := io.out.ready

  bpu.io.spec_advance_valid := captureFire
  bpu.io.spec_advance_mask := fetchPacket.valid.asUInt
  bpu.io.recover_valid := io.bp_recover_valid && ftq.io.recoverValid
  bpu.io.recover_pc := io.bp_recover_pc
  bpu.io.recover_index := io.bp_recover_index
  bpu.io.recover_target := io.bp_recover_target
  bpu.io.recover_taken := io.bp_recover_taken
  bpu.io.recover_is_branch := io.bp_recover_is_branch
  bpu.io.recover_is_jalr := io.bp_recover_is_jalr
  bpu.io.recover_is_call := io.bp_recover_is_call
  bpu.io.recover_is_ret := io.bp_recover_is_ret
  bpu.io.recover_ras := ftq.io.recover.ras
  bpu.io.recover_ras_ptr := ftq.io.recover.rasPtr
  bpu.io.recover_ras_count := ftq.io.recover.rasCount
  bpu.io.reset_spec := io.is_flush && (!io.bp_recover_valid || !ftq.io.recoverValid)

  io.pc.bits.next_pc := Mux(io.is_flush, io.correct_pc, predictedNext)

  ready := io.imem.rvalid && (state =/= s_FLUSH)
  val fetchResourcesReady = fetchBuffer.io.in.ready && ftq.io.alloc.ready
  io.imem.arvalid := io.in.valid && state === s_IDLE && !io.is_flush && fetchResourcesReady
  idle := io.imem.arvalid && io.imem.arready
  work := captureFire
  io.imem.rready := captureFire || (state === s_FLUSH) ||
    (state === s_WORK && io.is_flush)
  io.pc.valid := !reset.asBool && io.pc.ready

  io.in.ready := !io.in.valid || work || io.is_flush

  when(!reset.asBool) {
    assert(captureFire === ftq.io.alloc.fire,
      "FetchBuffer and FTQ must allocate the same fetch block")
    assert(!io.bp_recover_valid || ftq.io.recoverValid,
      "branch recovery must find its FTQ generation")
  }

  if(conf.statistics){
    val packetFire = io.out.valid && io.out.ready
    val ftqHighWater = RegInit(0.U(log2Ceil(OoOParams.FTQ_SIZE + 1).W))
    when(ftq.io.count > ftqHighWater) {
      ftqHighWater := ftq.io.count
    }
    PM(conf, clock, EVENT_IFU_FETCH, 1.U, io.imem.arvalid && io.imem.arready)
    PM(conf, clock, EVENT_IFU_STALL_ICACHE, 1.U, state === s_WORK && !io.imem.rvalid)
    PM(conf, clock, EVENT_IFU_STALL_IDU, 1.U, state === s_WORK && io.imem.rvalid && !captureFire)
    PM(conf, clock, EVENT_FETCH_SLOT0_VALID, 1.U, captureFire && fetchPacket.valid(0))
    PM(conf, clock, EVENT_FETCH_SLOT1_VALID, 1.U, captureFire && fetchPacket.valid(1))
    PM(conf, clock, EVENT_FETCH_SLOT1_KILLED, 1.U,
      ready && io.in.valid && io.imem.rvalid1 && slot0Taken)
    PM(conf, clock, EVENT_BPU_TAGGED_HIT, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_tagged_hit) ||
        (fetchPacket.valid(1) && bpu.io.bp1_tagged_hit)))
    PM(conf, clock, EVENT_BPU_INDIRECT_HIT, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_indirect_hit) ||
        (fetchPacket.valid(1) && bpu.io.bp1_indirect_hit)))
    PM(conf, clock, EVENT_TAGE_USE_ALT, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_tage_use_alt) ||
        (fetchPacket.valid(1) && bpu.io.bp1_tage_use_alt)))
    PM(conf, clock, EVENT_BIMODAL_SELECTED, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_bimodal_selected) ||
        (fetchPacket.valid(1) && bpu.io.bp1_bimodal_selected)))
    PM(conf, clock, EVENT_TAGE_ALLOC, 1.U, bpu.io.tage_alloc)
    PM(conf, clock, EVENT_ITAGE_HIT, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_itage_hit) ||
        (fetchPacket.valid(1) && bpu.io.bp1_itage_hit)))
    PM(conf, clock, EVENT_ITAGE_ALLOC, 1.U, bpu.io.itage_alloc)
    PM(conf, clock, EVENT_LOOP_PREDICT_HIT, 1.U,
      captureFire && ((fetchPacket.valid(0) && bpu.io.bp_loop_hit) ||
        (fetchPacket.valid(1) && bpu.io.bp1_loop_hit)))
    PM(conf, clock, EVENT_FETCH_BLOCK, 1.U, captureFire)
    PM(conf, clock, EVENT_FETCH_VALID_INST, PopCount(fetchPacket.valid), captureFire)
    PM(conf, clock, EVENT_FETCH_BLOCK2, 1.U,
      captureFire && fetchPacket.valid(0) && fetchPacket.valid(1))
    PM(conf, clock, EVENT_FETCH_BUFFER_FULL, 1.U,
      captureCandidate && fetchBuffer.io.full)
    PM(conf, clock, EVENT_FTQ_FULL, 1.U, captureCandidate && ftq.io.full)
    PM(conf, clock, EVENT_FETCH_LINE_TAIL, 1.U,
      captureFire && !io.imem.rvalid1 && !slot0Taken && !slot0Fault)
    PM(conf, clock, EVENT_SPEC_GHR_ROLLBACK, 1.U,
      io.bp_recover_valid && ftq.io.recoverValid)
    PM(conf, clock, EVENT_RAS_ROLLBACK, 1.U,
      io.bp_recover_valid && ftq.io.recoverValid &&
        (io.bp_recover_is_call || io.bp_recover_is_ret))
    PM(conf, clock, EVENT_FTQ_HIGH_WATER, ftq.io.count - ftqHighWater,
      ftq.io.count > ftqHighWater)
    PM(conf, clock, EVENT_FTQ_STALE_RECOVER, 1.U,
      io.bp_recover_valid && !ftq.io.recoverValid)
    PM(conf, clock, EVENT_FETCH_WAIT_RESOURCE, 1.U,
      state === s_IDLE && io.in.valid && !io.is_flush && !fetchResourcesReady)
    PM(conf, clock, EVENT_FETCH_WAIT_PC, 1.U,
      state === s_IDLE && !io.in.valid && !io.is_flush)
    PM(conf, clock, EVENT_FETCH_SLOT1_CREDIT_BLOCK, 1.U,
      captureFire && slot1RawUse && !io.slot1_enable)
    val redirectBubble = RegNext(io.is_flush, false.B) && !packetFire
    PM(conf, clock, EVENT_FETCH_REDIRECT_BUBBLE, 1.U, redirectBubble)
  }
}
