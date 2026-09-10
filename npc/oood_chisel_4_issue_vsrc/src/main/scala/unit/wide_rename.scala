package unit

import chisel3._
import chisel3.util._
import common.OoOParams

class WideCheckpoint(physW: Int, nPhys: Int) extends Bundle {
  val valid = Bool()
  val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  val rat = Vec(32, UInt(physW.W))
  val free = UInt(nPhys.W)
}

class WideRename(
  nPhys: Int = OoOParams.N_PHYS,
  cpDepth: Int = 8
) extends Module {
  private val width = OoOParams.CORE_WIDTH
  private val physW = log2Ceil(nPhys)
  private val cpW = log2Ceil(cpDepth)
  require(isPow2(cpDepth))

  val io = IO(new Bundle {
    val fire = Input(Vec(width, Bool()))
    val rs1 = Input(Vec(width, UInt(5.W)))
    val rs2 = Input(Vec(width, UInt(5.W)))
    val rd = Input(Vec(width, UInt(5.W)))
    val reg_write = Input(Vec(width, Bool()))
    val is_branch = Input(Vec(width, Bool()))
    val rob_idx = Input(Vec(width, UInt(OoOParams.ROB_PTR_W.W)))

    val psrc1 = Output(Vec(width, UInt(physW.W)))
    val psrc2 = Output(Vec(width, UInt(physW.W)))
    val pdest = Output(Vec(width, UInt(physW.W)))
    val old_phys = Output(Vec(width, UInt(physW.W)))
    val do_rename = Output(Vec(width, Bool()))
    val cp_idx = Output(Vec(width, UInt(cpW.W)))

    val commit_fire = Input(Vec(width, Bool()))
    val commit_do_rename = Input(Vec(width, Bool()))
    val commit_old_phys = Input(Vec(width, UInt(physW.W)))
    val commit_new_phys = Input(Vec(width, UInt(physW.W)))
    val commit_arch_rd = Input(Vec(width, UInt(5.W)))
    val commit_cp_valid = Input(Vec(width, Bool()))
    val commit_cp_idx = Input(Vec(width, UInt(cpW.W)))

    val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))
    val restore_cp = Input(Bool())
    val restore_cp_idx = Input(UInt(cpW.W))
    val restore_do_rename = Input(Bool())
    val restore_arch_rd = Input(UInt(5.W))
    val restore_new_phys = Input(UInt(physW.W))
    val restore_arch = Input(Bool())
    val rebuild = Input(Bool())
    val rebuild_rat = Input(Vec(32, UInt(physW.W)))
    val rebuild_free = Input(UInt(nPhys.W))
    val reserve_mask = Input(UInt(nPhys.W))

    val rat_out = Output(Vec(32, UInt(physW.W)))
    val arch_rat_out = Output(Vec(32, UInt(physW.W)))
    val free_cnt = Output(UInt(log2Ceil(nPhys + 1).W))
    val cp_free = Output(UInt(log2Ceil(cpDepth + 1).W))
  })

  val rat = RegInit(VecInit((0 until 32).map(_.U(physW.W))))
  val archRat = RegInit(VecInit((0 until 32).map(_.U(physW.W))))
  val freeInit = ((BigInt(1) << nPhys) - (BigInt(1) << 32)).U(nPhys.W)
  val freeBits = RegInit(freeInit)
  val checkpoints = RegInit(VecInit(Seq.fill(cpDepth)(
    0.U.asTypeOf(new WideCheckpoint(physW, nPhys)))))

  def noP0(v: UInt): UInt = v & ~1.U(nPhys.W)
  def setBit(v: UInt, idx: UInt): UInt =
    Mux(idx === 0.U, v, v | (1.U(nPhys.W) << idx))
  def clearBit(v: UInt, idx: UInt): UInt =
    Mux(idx === 0.U, v, v & ~(1.U(nPhys.W) << idx))
  def age(idx: UInt): UInt =
    (idx - io.rob_head)(OoOParams.ROB_PTR_W - 1, 0)

  io.rat_out := rat
  io.arch_rat_out := archRat
  // Recovery reconstructs the free list from the ROB.  Keep allocation safe
  // even if a boundary-cycle reconstruction temporarily exposes a physical
  // register still owned by the speculative RAT or a live ROB entry.
  val allocFreeBits = noP0(freeBits & ~io.reserve_mask)
  io.free_cnt := PopCount(allocFreeBits)
  io.cp_free := PopCount(VecInit(checkpoints.map(!_.valid)).asUInt)

  val ratStep = Wire(Vec(width + 1, Vec(32, UInt(physW.W))))
  val freeStep = Wire(Vec(width + 1, UInt(nPhys.W)))
  ratStep(0) := rat
  freeStep(0) := allocFreeBits

  for (lane <- 0 until width) {
    val request = io.fire(lane) && io.reg_write(lane) && io.rd(lane) =/= 0.U
    val nextPhys = PriorityEncoder(freeStep(lane))
    val alloc = request && freeStep(lane).orR
    io.psrc1(lane) := Mux(io.rs1(lane) === 0.U, 0.U, ratStep(lane)(io.rs1(lane)))
    io.psrc2(lane) := Mux(io.rs2(lane) === 0.U, 0.U, ratStep(lane)(io.rs2(lane)))
    io.pdest(lane) := Mux(alloc, nextPhys, 0.U)
    io.old_phys(lane) := Mux(alloc, ratStep(lane)(io.rd(lane)), 0.U)
    io.do_rename(lane) := alloc
    freeStep(lane + 1) := Mux(alloc, clearBit(freeStep(lane), nextPhys), freeStep(lane))
    for (a <- 0 until 32) {
      ratStep(lane + 1)(a) := Mux(alloc && io.rd(lane) === a.U,
        nextPhys, ratStep(lane)(a))
    }
  }

  val commitRelease = (0 until width).foldLeft(0.U(nPhys.W)) { (mask, lane) =>
    val release = io.commit_fire(lane) && io.commit_do_rename(lane) &&
      io.commit_old_phys(lane) =/= 0.U
    mask | Mux(release, 1.U(nPhys.W) << io.commit_old_phys(lane), 0.U)
  }
  def withCommitFree(v: UInt): UInt = noP0(v | commitRelease)

  val archNext = Wire(Vec(32, UInt(physW.W)))
  for (a <- 0 until 32) {
    val values = Wire(Vec(width + 1, UInt(physW.W)))
    values(0) := archRat(a)
    for (lane <- 0 until width) {
      val write = io.commit_fire(lane) && io.commit_do_rename(lane) &&
        io.commit_arch_rd(lane) === a.U && a.U =/= 0.U
      values(lane + 1) := Mux(write, io.commit_new_phys(lane), values(lane))
    }
    archNext(a) := values(width)
  }

  val cpMask = Wire(Vec(width + 1, UInt(cpDepth.W)))
  val cpPush = Wire(Vec(width, Bool()))
  val cpAllocIdx = Wire(Vec(width, UInt(cpW.W)))
  cpMask(0) := VecInit(checkpoints.map(!_.valid)).asUInt
  for (lane <- 0 until width) {
    cpAllocIdx(lane) := PriorityEncoder(cpMask(lane))
    cpPush(lane) := io.fire(lane) && io.is_branch(lane) && cpMask(lane).orR
    cpMask(lane + 1) := Mux(cpPush(lane),
      cpMask(lane) & ~UIntToOH(cpAllocIdx(lane), cpDepth), cpMask(lane))
    io.cp_idx(lane) := Mux(cpPush(lane), cpAllocIdx(lane), 0.U)
  }

  val archUsed = (1 until 32).foldLeft(1.U(nPhys.W)) { (mask, a) =>
    mask | (1.U(nPhys.W) << archNext(a))
  }
  val freeFromArch = noP0(~archUsed)

  when(io.rebuild) {
    rat := io.rebuild_rat
    freeBits := noP0(io.rebuild_free)
    archRat := archNext
    for (i <- 0 until cpDepth) {
      checkpoints(i).valid := false.B
    }
  }.elsewhen(io.restore_arch) {
    rat := archNext
    archRat := archNext
    freeBits := freeFromArch
    for (i <- 0 until cpDepth) {
      checkpoints(i).valid := false.B
    }
  }.elsewhen(io.restore_cp) {
    val snap = checkpoints(io.restore_cp_idx)
    val restoredRat = Wire(Vec(32, UInt(physW.W)))
    for (i <- 0 until 32) {
      restoredRat(i) := Mux(io.restore_do_rename && io.restore_arch_rd === i.U,
        io.restore_new_phys, snap.rat(i))
    }
    rat := restoredRat
    archRat := archNext
    val restoredFree = Mux(io.restore_do_rename && io.restore_new_phys =/= 0.U,
      withCommitFree(snap.free) & ~(1.U(nPhys.W) << io.restore_new_phys),
      withCommitFree(snap.free))
    freeBits := noP0(restoredFree)
    val targetAge = age(snap.rob_idx)
    for (i <- 0 until cpDepth) {
      when(checkpoints(i).valid && age(checkpoints(i).rob_idx) > targetAge) {
        checkpoints(i).valid := false.B
      }
    }
    for (lane <- 0 until width) {
      when(io.commit_fire(lane) && io.commit_cp_valid(lane)) {
        checkpoints(io.commit_cp_idx(lane)).valid := false.B
      }
    }
  }.otherwise {
    rat := ratStep(width)
    archRat := archNext
    freeBits := withCommitFree(freeStep(width))

    when(commitRelease.orR) {
      for (i <- 0 until cpDepth) {
        when(checkpoints(i).valid) {
          checkpoints(i).free := withCommitFree(checkpoints(i).free)
        }
      }
    }
    for (lane <- 0 until width) {
      when(io.commit_fire(lane) && io.commit_cp_valid(lane)) {
        checkpoints(io.commit_cp_idx(lane)).valid := false.B
      }
      when(cpPush(lane)) {
        checkpoints(cpAllocIdx(lane)).valid := true.B
        checkpoints(cpAllocIdx(lane)).rob_idx := io.rob_idx(lane)
        checkpoints(cpAllocIdx(lane)).rat := ratStep(lane + 1)
        checkpoints(cpAllocIdx(lane)).free := withCommitFree(freeStep(lane + 1))
      }
    }
  }

  when(!reset.asBool) {
    assert(io.fire.asUInt === Fill(width, io.fire(0)) ||
      (0 until width - 1).map(i => !io.fire(i + 1) || io.fire(i)).reduce(_ && _),
      "wide rename fire must be a valid prefix")
    for (lane <- 0 until width) {
      assert(!(io.fire(lane) && io.reg_write(lane) && io.rd(lane) =/= 0.U) ||
        io.do_rename(lane), "wide rename requires pre-checked free-list credit")
      assert(!io.do_rename(lane) || !io.reserve_mask(io.pdest(lane)),
        "wide rename must not allocate a live physical register")
    }
  }
}
