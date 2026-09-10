package unit

import common.OoOParams
import chisel3._
import chisel3.util._

/**
 * RAT + arch_rat + 位图 FreeList
 * freeBits(p)=1 表示空闲；phys0 永不空闲。
 *
 * restore_arch：RAT←arch_rat，freeBits 按 arch_rat 占用重建（正确 bit-or，禁止动态 Vec 写）。
 */
class Rename(nPhys: Int = OoOParams.N_PHYS) extends Module {
  val physW = log2Ceil(nPhys)

  val io = IO(new Bundle {
    val fire      = Input(Bool())
    val rs1       = Input(UInt(5.W))
    val rs2       = Input(UInt(5.W))
    val rd        = Input(UInt(5.W))
    val reg_write = Input(Bool())

    val rs1_phys  = Output(UInt(physW.W))
    val rs2_phys  = Output(UInt(physW.W))
    val dest_phys = Output(UInt(physW.W))
    val old_phys  = Output(UInt(physW.W))
    val do_rename = Output(Bool())
    val fl_empty  = Output(Bool())

    val commit_fire = Input(Bool())
    val cm_do_ren   = Input(Bool())
    val cm_old_phys = Input(UInt(physW.W))
    val cm_new_phys = Input(UInt(physW.W))
    val cm_arch_rd  = Input(UInt(5.W))

    val rb_fire     = Input(Bool())
    val rb_do_ren   = Input(Bool())
    val rb_arch_rd  = Input(UInt(5.W))
    val rb_old_phys = Input(UInt(physW.W))
    val rb_new_phys = Input(UInt(physW.W))

    val rat_out      = Output(Vec(32, UInt(physW.W)))
    val arch_rat_out = Output(Vec(32, UInt(physW.W)))
    val restore_arch = Input(Bool())

    val free_mask = Input(UInt(3.W))
    val free_vec  = Input(Vec(3, UInt(physW.W)))

    // flush 重建：RAT/freeBits 一次写回（按 arch_rat∪kept ROB 计算，禁止重置 32..47）
    val rebuild      = Input(Bool())
    val rebuild_rat  = Input(Vec(32, UInt(physW.W)))
    val rebuild_free = Input(UInt(nPhys.W))
  })

  val rat      = RegInit(VecInit((0 until 32).map(i => i.U(physW.W))))
  val arch_rat = RegInit(VecInit((0 until 32).map(i => i.U(physW.W))))
  // 初值 32..nPhys-1 空闲
  val freeInit = ((BigInt(1) << nPhys) - (BigInt(1) << 32)).U(nPhys.W)
  val freeBits = RegInit(freeInit)

  io.fl_empty     := !freeBits.orR
  io.rat_out      := rat
  io.arch_rat_out := arch_rat

  io.rs1_phys := Mux(io.rs1 === 0.U, 0.U, rat(io.rs1))
  io.rs2_phys := Mux(io.rs2 === 0.U, 0.U, rat(io.rs2))

  val do_ren   = io.reg_write && (io.rd =/= 0.U)
  val has_free = freeBits.orR
  val new_p    = PriorityEncoder(freeBits)
  val old_p    = Mux(io.rd === 0.U, 0.U, rat(io.rd))
  io.do_rename := do_ren
  io.old_phys  := Mux(do_ren, old_p, 0.U)
  io.dest_phys := Mux(do_ren && has_free, new_p, 0.U)

  def setBit(v: UInt, i: UInt): UInt =
    Mux(i === 0.U, v, v | (1.U(nPhys.W) << i))
  def clrBit(v: UInt, i: UInt): UInt =
    Mux(i === 0.U, v, v & ~(1.U(nPhys.W) << i))

  // 正确：or-reduce 所有 arch 占用，禁止 used(arch_rat(a)):=true 动态写
  val usedMask = (1.U(nPhys.W) << 0.U) | // phys0
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
  val freeFromArch = ~usedMask

  val do_alloc  = io.fire && do_ren && has_free
  val do_commit = io.commit_fire && io.cm_do_ren && (io.cm_arch_rd =/= 0.U)
  val do_rb     = io.rb_fire && io.rb_do_ren && (io.rb_arch_rd =/= 0.U)

  when(io.rebuild) {
    for (i <- 0 until 32) { rat(i) := io.rebuild_rat(i) }
    freeBits := io.rebuild_free
    when(do_commit) {
      arch_rat(io.cm_arch_rd) := io.cm_new_phys
    }
  }.elsewhen(io.restore_arch) {
    for (i <- 0 until 32) { rat(i) := arch_rat(i) }
    freeBits := freeFromArch
  }.elsewhen(do_rb) {
    rat(io.rb_arch_rd) := io.rb_old_phys
    freeBits := setBit(clrBit(freeBits, io.rb_old_phys), io.rb_new_phys)
  }.otherwise {
    when(do_alloc) {
      rat(io.rd) := new_p
    }
    when(do_commit) {
      arch_rat(io.cm_arch_rd) := io.cm_new_phys
    }
    val a0 = Mux(do_alloc, clrBit(freeBits, new_p), freeBits)
    val a1 = Mux(do_commit && io.cm_old_phys =/= 0.U, setBit(a0, io.cm_old_phys), a0)
    val a2 = Mux(io.free_mask(0) && io.free_vec(0) =/= 0.U, setBit(a1, io.free_vec(0)), a1)
    val a3 = Mux(io.free_mask(1) && io.free_vec(1) =/= 0.U, setBit(a2, io.free_vec(1)), a2)
    val a4 = Mux(io.free_mask(2) && io.free_vec(2) =/= 0.U, setBit(a3, io.free_vec(2)), a3)
    freeBits := a4
  }
}
