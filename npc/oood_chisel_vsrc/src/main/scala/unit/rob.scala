package unit

import common.OoOParams
import chisel3._
import core.State
import chisel3.util._
import core.PerfMonitor
import core.PerfEvents._
import common.BPU_Config._

class ROBEntry extends Bundle {
  val valid         = Bool()
  val done          = Bool()
  val issued        = Bool()
  val pc            = UInt(32.W)
  val inst          = UInt(32.W)
  val reg_write     = Bool()
  val reg_write_sel = UInt(3.W)
  val csr_write     = Bool()
  val csr_sel       = UInt(2.W)
  val mem_valid     = Bool()
  val mem_write     = Bool()
  val mem_rd        = UInt(3.W)
  val mem_wmask     = UInt(8.W)
  val alu_control   = UInt(5.W)
  val alu_srcA      = UInt(2.W)
  val alu_srcB      = UInt(2.W)
  val jump          = UInt(4.W)
  val imm_ext       = UInt(32.W)
  val arch_rd       = UInt(5.W)
  val src1_phys     = UInt(OoOParams.PHYS_W.W)
  val src2_phys     = UInt(OoOParams.PHYS_W.W)
  val old_phys      = UInt(OoOParams.PHYS_W.W)
  val new_phys      = UInt(OoOParams.PHYS_W.W)
  val dest_val      = UInt(32.W)
  val is_ebreak     = Bool()
  val is_fencei     = Bool()
  val state         = new State
  val bp_valid      = Bool()
  val bp_taken      = Bool()
  val bp_target     = UInt(32.W)
  val bp_index      = UInt(BP_META_WIDTH.W)
  val ftq_idx       = UInt(OoOParams.FTQ_PTR_W.W)
  val ftq_generation = UInt(OoOParams.FTQ_GEN_W.W)
  val cp_idx        = UInt(log2Ceil(OoOParams.CP_DEPTH).W)
  val actual_taken  = Bool()
  val actual_target = UInt(32.W)
  val rs1_val       = UInt(32.W)
  val rs2_val       = UInt(32.W)
  val csr_waddr     = UInt(12.W)
  val csr_rd1       = UInt(32.W)
  val mem_addr      = UInt(32.W)
  val mem_wdata     = UInt(32.W) // store 閸樼喎顫?rs2閿涘牊婀粔璁崇秴閿涘绱眂ommit 閸愭瑦妞傞崘宥嗗瘻 addr 鐎靛綊缍?
  val addr_ready    = Bool()    // wb 閸氬骸婀撮崸鈧?閺佺増宓佹鎰剁礄store 閸?commit 閸愭瑱绱眑oad 閸愯尙鐛婇悽顭掔礆
}

class ROB(statistics: Boolean = false, n: Int = OoOParams.ROB_SIZE) extends Module {
  val ptrW = log2Ceil(n)
  require(n >= 2 && isPow2(n) && n <= 32)

  val io = IO(new Bundle {
    val enq_fire = Input(Bool())
    val enq_bits = Input(new ROBEntry)
    val enq_idx  = Output(UInt(ptrW.W))
    val full     = Output(Bool())
    val space    = Output(UInt(log2Ceil(n + 1).W))
    val enq1_fire = Input(Bool())
    val enq1_bits = Input(new ROBEntry)
    val enq1_idx  = Output(UInt(ptrW.W))

    val issue_valid = Output(Bool())
    val issue_idx   = Output(UInt(ptrW.W))
    val issue_bits  = Output(new ROBEntry)
    val issue_fire  = Input(Bool())

    val wb_fire         = Input(Bool())
    val wb_idx          = Input(UInt(ptrW.W))
    val wb_val          = Input(UInt(32.W))
    val wb_state        = Input(new State)
    val wb_mem_addr     = Input(UInt(32.W))
    val wb_mem_wdata    = Input(UInt(32.W))
    val wb_actual_taken = Input(Bool())
    val wb_actual_target = Input(UInt(32.W))
    val wb1_fire         = Input(Bool())
    val wb1_idx          = Input(UInt(ptrW.W))
    val wb1_val          = Input(UInt(32.W))
    val wb1_state        = Input(new State)
    val wb1_mem_addr     = Input(UInt(32.W))
    val wb1_mem_wdata    = Input(UInt(32.W))
    val wb1_actual_taken = Input(Bool())
    val wb1_actual_target = Input(UInt(32.W))
    val ctrl_wb_fire         = Input(Bool())
    val ctrl_wb_idx          = Input(UInt(ptrW.W))
    val ctrl_wb_state        = Input(new State)
    val ctrl_wb_actual_taken = Input(Bool())
    val ctrl_wb_actual_target = Input(UInt(32.W))
    val store_wb_fire     = Input(Bool())
    val store_wb_idx      = Input(UInt(ptrW.W))
    val store_wb_state    = Input(new State)
    val store_wb_mem_addr = Input(UInt(32.W))
    val store_wb_mem_wdata = Input(UInt(32.W))

    val commit_valid = Output(Bool())
    val commit_idx   = Output(UInt(ptrW.W))
    val commit_bits  = Output(new ROBEntry)
    val commit_fire  = Input(Bool())
    val commit1_valid = Output(Bool())
    val commit1_idx   = Output(UInt(ptrW.W))
    val commit1_bits  = Output(new ROBEntry)
    val commit1_fire  = Input(Bool())

    val flush     = Input(Bool())
    val flush_idx = Input(UInt(ptrW.W))
    val flush_all = Input(Bool()) // 4c閿涙艾绱撶敮绋挎倵閺佺銆冨〒鍛敄閿涘潒call 瀹告彃婀稉濠佺閹峰秵褰佹禍銈忕礆

    val entries = Output(Vec(n, new ROBEntry))
    val head    = Output(UInt(ptrW.W))
    val tail    = Output(UInt(ptrW.W))
    val count   = Output(UInt(log2Ceil(n + 1).W))
  })

  val entries = RegInit(VecInit(Seq.fill(n)(0.U.asTypeOf(new ROBEntry))))
  val head  = RegInit(0.U(ptrW.W))
  val tail  = RegInit(0.U(ptrW.W))
  val count = RegInit(0.U(log2Ceil(n + 1).W))

  def age(idx: UInt): UInt =
    (idx - head)(ptrW - 1, 0)

  io.entries := entries
  io.head := head
  io.tail := tail
  io.count := count
  io.full := count === n.U
  io.enq_idx := tail
  io.enq1_idx := (tail + 1.U)(ptrW - 1, 0)
  io.space := n.U - count

  // 娴?head 鐠ч攱澹橀張鈧懓浣规弓閸欐垵鐨犻敍姘辨暏 Vec + PriorityEncoder閿涘矂浼╅崗宥囩矋閸氬牏骞?
  val canIss = Wire(Vec(n, Bool()))
  for (off <- 0 until n) {
    val id = (head + off.U)(ptrW - 1, 0)
    canIss(off) := (off.U < count) && entries(id).valid && !entries(id).issued
  }
  val issue_off = PriorityEncoder(canIss.asUInt)
  val issue_id  = (head + issue_off)(ptrW - 1, 0)
  io.issue_valid := canIss.asUInt.orR
  io.issue_idx   := issue_id
  io.issue_bits  := entries(issue_id)

  // commit_valid閿涙艾褰查幓鎰唉閿涘潐one閿涘鍨ㄩ張顒佸濮濓絽婀?wb 閸氬奔绔?head閿涘牓銆庢惔蹇撴倱閹峰秴鐣幋?闁偓娴兼埊绱?
  val head1 = Mux(head === (n - 1).U, 0.U, head + 1.U)
  val wb_is_head = io.wb_fire && (io.wb_idx === head) && entries(head).valid
  val wb1_is_head = io.wb1_fire && (io.wb1_idx === head) && entries(head).valid
  val wb_is_head1 = io.wb_fire && (io.wb_idx === head1) && entries(head1).valid
  val wb1_is_head1 = io.wb1_fire && (io.wb1_idx === head1) && entries(head1).valid
  val ctrl_wb_is_head = io.ctrl_wb_fire && (io.ctrl_wb_idx === head) && entries(head).valid
  val ctrl_wb_is_head1 = io.ctrl_wb_fire && (io.ctrl_wb_idx === head1) && entries(head1).valid
  val store_wb_is_head = io.store_wb_fire && (io.store_wb_idx === head) && entries(head).valid
  val store_wb_is_head1 = io.store_wb_fire && (io.store_wb_idx === head1) && entries(head1).valid
  io.commit_valid := entries(head).valid &&
    (entries(head).done || wb_is_head || wb1_is_head || ctrl_wb_is_head || store_wb_is_head)
  io.commit_idx := head
  io.commit_bits := entries(head)
  io.commit1_valid := (count > 1.U) && entries(head1).valid &&
    (entries(head1).done || wb_is_head1 || wb1_is_head1 || ctrl_wb_is_head1 || store_wb_is_head1)
  io.commit1_idx := head1
  io.commit1_bits := entries(head1)

  // flush 閸氬本濯挎禒宥呭帒鐠?head commit閿涘湹BU 閸欘垵鍏樺锝呮躬闁偓娴兼垶鐦崚鍡樻暜閺囩鈧胶娈戦幐鍥︽姢閿?  val do_cm = io.commit_fire && entries(head).valid
  val do_cm = io.commit_fire && entries(head).valid
  val do_cm1 = io.commit1_fire && do_cm && entries(head1).valid
  val cm_count = do_cm.asUInt +& do_cm1.asUInt
  val cm_head = (head + cm_count)(ptrW - 1, 0)

  if (statistics) {
    val pm = Module(new PerfMonitor)
    pm.io.clock := clock
    pm.io.event_id := EVENT_COMMIT
    pm.io.data := cm_count
    pm.io.enable := cm_count =/= 0.U
  }

  when(io.wb_fire && entries(io.wb_idx).valid) {
    entries(io.wb_idx).done          := true.B
    entries(io.wb_idx).dest_val      := io.wb_val
    entries(io.wb_idx).state         := io.wb_state
    entries(io.wb_idx).mem_addr      := io.wb_mem_addr
    entries(io.wb_idx).mem_wdata     := io.wb_mem_wdata
    entries(io.wb_idx).addr_ready    := true.B
    entries(io.wb_idx).actual_taken  := io.wb_actual_taken
    entries(io.wb_idx).actual_target := io.wb_actual_target
  }
  when(io.wb1_fire && entries(io.wb1_idx).valid) {
    entries(io.wb1_idx).done          := true.B
    entries(io.wb1_idx).dest_val      := io.wb1_val
    entries(io.wb1_idx).state         := io.wb1_state
    entries(io.wb1_idx).mem_addr      := io.wb1_mem_addr
    entries(io.wb1_idx).mem_wdata     := io.wb1_mem_wdata
    entries(io.wb1_idx).addr_ready    := true.B
    entries(io.wb1_idx).actual_taken  := io.wb1_actual_taken
    entries(io.wb1_idx).actual_target := io.wb1_actual_target
  }
  when(io.ctrl_wb_fire && entries(io.ctrl_wb_idx).valid) {
    entries(io.ctrl_wb_idx).done          := true.B
    entries(io.ctrl_wb_idx).state         := io.ctrl_wb_state
    entries(io.ctrl_wb_idx).actual_taken  := io.ctrl_wb_actual_taken
    entries(io.ctrl_wb_idx).actual_target := io.ctrl_wb_actual_target
  }
  when(io.store_wb_fire && entries(io.store_wb_idx).valid) {
    entries(io.store_wb_idx).done       := true.B
    entries(io.store_wb_idx).state      := io.store_wb_state
    entries(io.store_wb_idx).mem_addr   := io.store_wb_mem_addr
    entries(io.store_wb_idx).mem_wdata  := io.store_wb_mem_wdata
    entries(io.store_wb_idx).addr_ready := true.B
  }

  when(io.flush_all) {
    for (i <- 0 until n) { entries(i).valid := false.B }
    head  := 0.U
    tail  := 0.U
    count := 0.U
  }.elsewhen(io.flush) {
    val flushAge = age(io.flush_idx)
    val killVec = Wire(Vec(n, Bool()))
    for (i <- 0 until n) {
      val idx = i.U(ptrW.W)
      val idxAge = age(idx)
      val after = (idxAge < count) && (idxAge > flushAge)
      val committed = (do_cm && idx === head) || (do_cm1 && idx === head1)
      killVec(i) := after && entries(idx).valid && !committed
      when(after && !committed) {
        entries(idx).valid := false.B
      }
    }
    val killCnt = PopCount(killVec.asUInt)
    when(do_cm) {
      entries(head).valid := false.B
    }
    when(do_cm1) {
      entries(head1).valid := false.B
    }
    head := cm_head
    tail := Mux(io.flush_idx === (n - 1).U, 0.U, io.flush_idx + 1.U)
    count := count - killCnt - cm_count
  }.otherwise {
    val tail1 = Mux(tail === (n - 1).U, 0.U, tail + 1.U)
    val tail2 = Mux(tail1 === (n - 1).U, 0.U, tail1 + 1.U)
    val do_enq0 = io.enq_fire && (count < n.U)
    val do_enq1 = io.enq1_fire && Mux(do_enq0, count < (n - 1).U, count < n.U)
    val enq0Idx = tail
    val enq1Idx = Mux(do_enq0, tail1, tail)

    when(do_enq0) {
      val e = WireDefault(io.enq_bits)
      e.valid  := true.B
      e.done   := false.B
      e.issued := io.enq_bits.issued
      entries(enq0Idx) := e
    }
    when(do_enq1) {
      val e = WireDefault(io.enq1_bits)
      e.valid  := true.B
      e.done   := false.B
      e.issued := io.enq1_bits.issued
      entries(enq1Idx) := e
    }
    tail := Mux(do_enq0 && do_enq1, tail2,
      Mux(do_enq0 || do_enq1, tail1, tail))
    when(io.issue_fire) {
      entries(io.issue_idx).issued := true.B
    }
    when(do_cm) {
      entries(head).valid := false.B
    }
    when(do_cm1) {
      entries(head1).valid := false.B
    }
    head := cm_head
    count := count + do_enq0.asUInt + do_enq1.asUInt - cm_count
  }
}



