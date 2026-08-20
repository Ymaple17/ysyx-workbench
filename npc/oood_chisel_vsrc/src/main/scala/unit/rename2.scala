package unit

import chisel3._
import chisel3.util._
import common.OoOParams

class CPEntry(val physW: Int) extends Bundle {
  val valid = Bool()
  val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  val rat_snap = Vec(32, UInt(physW.W))
  val free_snap = UInt(OoOParams.N_PHYS.W)
}

class Rename2(nPhys: Int = OoOParams.N_PHYS, cpDepth: Int = OoOParams.CP_DEPTH) extends Module {
  val physW = log2Ceil(nPhys)
  val cpW = log2Ceil(cpDepth)

  val io = IO(new Bundle {
    val fire = Input(Bool())
    val rs1 = Input(UInt(5.W))
    val rs2 = Input(UInt(5.W))
    val rd = Input(UInt(5.W))
    val reg_write = Input(Bool())

    val fire0 = Input(Bool())
    val rs1_0 = Input(UInt(5.W))
    val rs2_0 = Input(UInt(5.W))
    val rd0 = Input(UInt(5.W))
    val reg_write0 = Input(Bool())
    val is_branch0 = Input(Bool())
    val cp_rob_idx0 = Input(UInt(OoOParams.ROB_PTR_W.W))

    val fire1 = Input(Bool())
    val rs1_1 = Input(UInt(5.W))
    val rs2_1 = Input(UInt(5.W))
    val rd1 = Input(UInt(5.W))
    val reg_write1 = Input(Bool())
    val is_branch1 = Input(Bool())
    val cp_rob_idx1 = Input(UInt(OoOParams.ROB_PTR_W.W))

    val rs1_phys0 = Output(UInt(physW.W))
    val rs2_phys0 = Output(UInt(physW.W))
    val dest_phys0 = Output(UInt(physW.W))
    val old_phys0 = Output(UInt(physW.W))
    val do_rename0 = Output(Bool())
    val cp_idx0 = Output(UInt(cpW.W))

    val rs1_phys = Output(UInt(physW.W))
    val rs2_phys = Output(UInt(physW.W))
    val dest_phys = Output(UInt(physW.W))
    val old_phys = Output(UInt(physW.W))
    val do_rename = Output(Bool())

    val rs1_phys1 = Output(UInt(physW.W))
    val rs2_phys1 = Output(UInt(physW.W))
    val dest_phys1 = Output(UInt(physW.W))
    val old_phys1 = Output(UInt(physW.W))
    val do_rename1 = Output(Bool())
    val cp_idx1 = Output(UInt(cpW.W))

    val commit_fire = Input(Bool())
    val cm_do_ren   = Input(Bool())
    val cm_old_phys = Input(UInt(physW.W))
    val cm_new_phys = Input(UInt(physW.W))
    val cm_arch_rd  = Input(UInt(5.W))
    val cm_cp_valid = Input(Bool())
    val cm_cp_idx   = Input(UInt(cpW.W))

    val rob_head = Input(UInt(OoOParams.ROB_PTR_W.W))

    val restore_arch = Input(Bool())
    val restore_cp   = Input(Bool())
    val restore_cp_idx = Input(UInt(cpW.W))
    val restore_rob_idx = Input(UInt(OoOParams.ROB_PTR_W.W))

    val rat_out      = Output(Vec(32, UInt(physW.W)))
    val arch_rat_out = Output(Vec(32, UInt(physW.W)))
    val fl_empty     = Output(Bool())
    val free_cnt     = Output(UInt(log2Ceil(nPhys + 1).W))
    val cp_full      = Output(Bool())

    val free_mask = Input(UInt(3.W))
    val free_vec  = Input(Vec(3, UInt(physW.W)))
    val rebuild      = Input(Bool())
    val rebuild_rat  = Input(Vec(32, UInt(physW.W)))
    val rebuild_free = Input(UInt(nPhys.W))
    val rb_fire     = Input(Bool())
    val rb_do_ren   = Input(Bool())
    val rb_arch_rd  = Input(UInt(5.W))
    val rb_old_phys = Input(UInt(physW.W))
    val rb_new_phys = Input(UInt(physW.W))
  })

  val rat = RegInit(VecInit((0 until 32).map(i => i.U(physW.W))))
  val arch_rat = RegInit(VecInit((0 until 32).map(i => i.U(physW.W))))
  val freeInit = ((BigInt(1) << nPhys) - (BigInt(1) << 32)).U(nPhys.W)
  val freeBits = RegInit(freeInit)
  val cps = RegInit(VecInit(Seq.fill(cpDepth)(0.U.asTypeOf(new CPEntry(physW)))))

  def noP0(v: UInt): UInt = v & ~1.U(nPhys.W)

  val usedMask = (1.U(nPhys.W) << 0.U) |
    (1.U(nPhys.W) << arch_rat(1)) |
    (1.U(nPhys.W) << arch_rat(2)) |
    (1.U(nPhys.W) << arch_rat(3)) |
    (1.U(nPhys.W) << arch_rat(4)) |
    (1.U(nPhys.W) << arch_rat(5)) |
    (1.U(nPhys.W) << arch_rat(6)) |
    (1.U(nPhys.W) << arch_rat(7)) |
    (1.U(nPhys.W) << arch_rat(8)) |
    (1.U(nPhys.W) << arch_rat(9)) |
    (1.U(nPhys.W) << arch_rat(10)) |
    (1.U(nPhys.W) << arch_rat(11)) |
    (1.U(nPhys.W) << arch_rat(12)) |
    (1.U(nPhys.W) << arch_rat(13)) |
    (1.U(nPhys.W) << arch_rat(14)) |
    (1.U(nPhys.W) << arch_rat(15)) |
    (1.U(nPhys.W) << arch_rat(16)) |
    (1.U(nPhys.W) << arch_rat(17)) |
    (1.U(nPhys.W) << arch_rat(18)) |
    (1.U(nPhys.W) << arch_rat(19)) |
    (1.U(nPhys.W) << arch_rat(20)) |
    (1.U(nPhys.W) << arch_rat(21)) |
    (1.U(nPhys.W) << arch_rat(22)) |
    (1.U(nPhys.W) << arch_rat(23)) |
    (1.U(nPhys.W) << arch_rat(24)) |
    (1.U(nPhys.W) << arch_rat(25)) |
    (1.U(nPhys.W) << arch_rat(26)) |
    (1.U(nPhys.W) << arch_rat(27)) |
    (1.U(nPhys.W) << arch_rat(28)) |
    (1.U(nPhys.W) << arch_rat(29)) |
    (1.U(nPhys.W) << arch_rat(30)) |
    (1.U(nPhys.W) << arch_rat(31))
  val freeFromArch = noP0(~usedMask)
  val allocFree = noP0(freeBits)

  io.rat_out := rat
  io.arch_rat_out := arch_rat
  io.fl_empty := !allocFree.orR
  io.free_cnt := PopCount(allocFree)
  io.cp_full := VecInit(cps.map(_.valid)).asUInt.andR

  def setBit(v: UInt, i: UInt): UInt =
    Mux(i === 0.U, v, v | (1.U(nPhys.W) << i))
  def clrBit(v: UInt, i: UInt): UInt =
    Mux(i === 0.U, v, v & ~(1.U(nPhys.W) << i))
  def robAge(idx: UInt, head: UInt): UInt =
    (idx - head)(OoOParams.ROB_PTR_W - 1, 0)
  val commitReleasesPhys = io.commit_fire && io.cm_do_ren && (io.cm_old_phys =/= 0.U)
  def withCommitFree(v: UInt): UInt =
    noP0(Mux(commitReleasesPhys, setBit(v, io.cm_old_phys), v))

  val fire0Eff = io.fire0 || io.fire
  val rs1_0Eff = Mux(io.fire0, io.rs1_0, io.rs1)
  val rs2_0Eff = Mux(io.fire0, io.rs2_0, io.rs2)
  val rd0Eff = Mux(io.fire0, io.rd0, io.rd)
  val regWrite0Eff = Mux(io.fire0, io.reg_write0, io.reg_write)
  val branch0Eff = io.fire0 && io.is_branch0
  val doRen0Req = fire0Eff && regWrite0Eff && (rd0Eff =/= 0.U)

  val fire1Eff = io.fire1
  val rs1_1Eff = io.rs1_1
  val rs2_1Eff = io.rs2_1
  val rd1Eff = io.rd1
  val regWrite1Eff = io.reg_write1
  val branch1Eff = io.fire1 && io.is_branch1
  val doRen1Req = fire1Eff && regWrite1Eff && (rd1Eff =/= 0.U)

  val old0 = Mux(rd0Eff === 0.U, 0.U, rat(rd0Eff))
  val old1Base = Mux(rd1Eff === 0.U, 0.U, rat(rd1Eff))

  val freeAfter0 = Wire(UInt(nPhys.W))
  val freeAfter1 = Wire(UInt(nPhys.W))
  val new0 = PriorityEncoder(allocFree)
  val doRen0 = doRen0Req && allocFree.orR
  freeAfter0 := Mux(doRen0, clrBit(allocFree, new0), allocFree)
  val new1 = PriorityEncoder(freeAfter0)
  val doRen1 = doRen1Req && freeAfter0.orR
  freeAfter1 := Mux(doRen1, clrBit(freeAfter0, new1), freeAfter0)

  val ratAfter0 = Wire(Vec(32, UInt(physW.W)))
  val ratAfter1 = Wire(Vec(32, UInt(physW.W)))
  for (i <- 0 until 32) {
    ratAfter0(i) := Mux(doRen0 && (rd0Eff === i.U), new0, rat(i))
    ratAfter1(i) := Mux(doRen1 && (rd1Eff === i.U), new1, ratAfter0(i))
  }

  val cpFreeMask = VecInit(cps.map(!_.valid)).asUInt
  val cpOH0 = PriorityEncoderOH(cpFreeMask)
  val cpMask1 = cpFreeMask & ~cpOH0.asUInt
  val cpMaskFor1 = Mux(branch0Eff && !io.cp_full, cpMask1, cpFreeMask)
  val cpIdx0 = PriorityEncoder(cpFreeMask)
  val cpIdx1 = PriorityEncoder(cpMaskFor1)
  val push0 = branch0Eff && !io.cp_full
  val push1 = branch1Eff && cpMaskFor1.orR

  val slot0Rs1Phys = Mux(rs1_0Eff === 0.U, 0.U, rat(rs1_0Eff))
  val slot0Rs2Phys = Mux(rs2_0Eff === 0.U, 0.U, rat(rs2_0Eff))
  val slot1Rs1Phys = Mux(rs1_1Eff === 0.U, 0.U, ratAfter0(rs1_1Eff))
  val slot1Rs2Phys = Mux(rs2_1Eff === 0.U, 0.U, ratAfter0(rs2_1Eff))

  io.do_rename0 := doRen0
  io.do_rename1 := doRen1
  io.rs1_phys0 := slot0Rs1Phys
  io.rs2_phys0 := slot0Rs2Phys
  io.rs1_phys1 := slot1Rs1Phys
  io.rs2_phys1 := slot1Rs2Phys
  io.rs1_phys := slot0Rs1Phys
  io.rs2_phys := slot0Rs2Phys
  io.old_phys0 := Mux(doRen0, old0, 0.U)
  io.old_phys1 := Mux(doRen1, Mux(rd1Eff === 0.U, 0.U, ratAfter0(rd1Eff)), 0.U)
  io.old_phys := Mux(doRen0, old0, 0.U)
  io.dest_phys0 := Mux(doRen0 && allocFree.orR, new0, 0.U)
  io.dest_phys1 := Mux(doRen1 && freeAfter0.orR, new1, 0.U)
  io.dest_phys := Mux(doRen0 && allocFree.orR, new0, 0.U)
  io.do_rename := doRen0
  io.cp_idx0 := Mux(push0, cpIdx0, 0.U)
  io.cp_idx1 := Mux(push1, cpIdx1, 0.U)

  when(io.rebuild) {
    for (i <- 0 until 32) {
      rat(i) := io.rebuild_rat(i)
    }
    freeBits := noP0(io.rebuild_free)
    for (i <- 0 until cpDepth) {
      cps(i).valid := false.B
    }
    when(io.commit_fire && io.cm_do_ren && (io.cm_arch_rd =/= 0.U)) {
      arch_rat(io.cm_arch_rd) := io.cm_new_phys
    }
  }.elsewhen(io.restore_arch) {
    for (i <- 0 until 32) {
      rat(i) := arch_rat(i)
    }
    freeBits := freeFromArch
    for (i <- 0 until cpDepth) {
      cps(i).valid := false.B
    }
  }.elsewhen(io.restore_cp) {
    val snap = cps(io.restore_cp_idx)
    for (i <- 0 until 32) {
      rat(i) := snap.rat_snap(i)
    }
    freeBits := withCommitFree(snap.free_snap)
    when(io.commit_fire && io.cm_do_ren && (io.cm_arch_rd =/= 0.U)) {
      arch_rat(io.cm_arch_rd) := io.cm_new_phys
    }
    val targetAge = robAge(snap.rob_idx, io.rob_head)
    for (i <- 0 until cpDepth) {
      val younger = cps(i).valid && (robAge(cps(i).rob_idx, io.rob_head) > targetAge)
      when(younger) {
        cps(i).valid := false.B
      }
    }
    when(io.commit_fire && io.cm_cp_valid) {
      cps(io.cm_cp_idx).valid := false.B
    }
  }.elsewhen(io.rb_fire && io.rb_do_ren) {
    rat(io.rb_arch_rd) := io.rb_old_phys
    freeBits := noP0(setBit(clrBit(freeBits, io.rb_old_phys), io.rb_new_phys))
  }.otherwise {
    when(commitReleasesPhys) {
      for (i <- 0 until cpDepth) {
        when(cps(i).valid) {
          cps(i).free_snap := withCommitFree(cps(i).free_snap)
        }
      }
    }

    when(push0) {
      cps(cpIdx0).valid := true.B
      cps(cpIdx0).rob_idx := io.cp_rob_idx0
      for (i <- 0 until 32) {
        cps(cpIdx0).rat_snap(i) := ratAfter0(i)
      }
      cps(cpIdx0).free_snap := withCommitFree(freeAfter0)
    }
    when(push1) {
      cps(cpIdx1).valid := true.B
      cps(cpIdx1).rob_idx := io.cp_rob_idx1
      for (i <- 0 until 32) {
        cps(cpIdx1).rat_snap(i) := ratAfter1(i)
      }
      cps(cpIdx1).free_snap := withCommitFree(freeAfter1)
    }

    when(io.commit_fire && io.cm_do_ren && (io.cm_arch_rd =/= 0.U)) {
      arch_rat(io.cm_arch_rd) := io.cm_new_phys
    }
    when(io.commit_fire && io.cm_cp_valid) {
      cps(io.cm_cp_idx).valid := false.B
    }

    val afterCommit = Mux(io.commit_fire && io.cm_do_ren && (io.cm_old_phys =/= 0.U),
      setBit(freeAfter1, io.cm_old_phys), freeAfter1)
    val afterMask = (0 until 3).foldLeft(afterCommit) { (acc, i) =>
      Mux(io.free_mask(i) && io.free_vec(i) =/= 0.U, setBit(acc, io.free_vec(i)), acc)
    }
    freeBits := noP0(afterMask)
    rat := ratAfter1
  }
}
