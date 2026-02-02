package resources

import chisel3._
import chisel3.util._

def Strob():Unit = {
  val is_jump = (control.io.signals.exu.jump =/= NO_JUMP)
  val is_store = control.io.signals.lsu.mem_write
  val is_load = (control.io.signals.lsu.mem_valid & ~control.io.signals.lsu.mem_write)
  val is_cal = (control.io.signals.exu.alu_op =/= ALU_NONE && ~is_load && ~is_jump && ~is_store)
  val is_csr = (control.io.signals.wbu.csr_write_e === CSRWRITE || control.io.irq === NIRQ)
  
  val sub_type = WireInit(VecInit(Seq.fill(8)(0.U(1.W))))
  io.out.bits.sub_type := sub_type.asUInt
  sub_type(0) := is_jump.asUInt
  sub_type(1) := is_store.asUInt
  sub_type(2) := is_load.asUInt
  sub_type(3) := is_cal.asUInt
  sub_type(4) := is_csr.asUInt

  PerformanceProbe(clock, IDUFinDec, RegNext(io.in.ready) & io.in.valid, sub_type.asUInt, RegNext(io.in.ready) & io.in.valid, false.B)
} 

object EVENT{
  val IFUGetInst = 0.U;
  val LSUGetData = 1.U;
  val EXUFinCal  = 2.U;
  val IDUFinDec  = 3.U;
  val ICacheHit  = 4.U;
  val ICacheMiss = 5.U;
  val IFUNGetInst = 6.U;
  val LSUNGetData = 7.U;
}

class PerformanceProbe(bits: Int) extends Module {
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val eventType = Input(UInt(4.W))
    val inc = Input(UInt(bits.W))
    val subType = Input(UInt(8.W))
    val start = Input(Bool())
    val e = Input(Bool())
    val timeEn = Input(Bool())
  })
  
  // Empty implementation to avoid DPI issues for now
}

object PerformanceProbe {
  def apply(clock: Clock, eventType: UInt, inc: UInt, subType: UInt, start: Bool = false.B, end: Bool = false.B, timeEn: Bool = true.B) = {
    val probe = Module(new PerformanceProbe(inc.getWidth))
    probe.io.clock := clock
    probe.io.eventType := eventType
    probe.io.inc := inc
    probe.io.subType := subType
    probe.io.start := start
    probe.io.e := end
    probe.io.timeEn := timeEn
  }
}