package core

import chisel3._
import unit.ROBEntry

object IFUCommitWiring {
  def connect(
      io: IFU_IO,
      valid0: Bool, entry0: ROBEntry,
      valid1: Bool, entry1: ROBEntry,
      valid2: Bool, entry2: ROBEntry,
      valid3: Bool, entry3: ROBEntry): Unit = {
    val valid = Seq(valid0, valid1, valid2, valid3)
    val entry = Seq(entry0, entry1, entry2, entry3)

    io.ftq_commit0_valid := valid0
    io.ftq_commit0_idx := entry0.ftq_idx
    io.ftq_commit0_generation := entry0.ftq_generation
    io.ftq_commit1_valid := valid1
    io.ftq_commit1_idx := entry1.ftq_idx
    io.ftq_commit1_generation := entry1.ftq_generation
    io.ftq_commit2_valid := valid2
    io.ftq_commit2_idx := entry2.ftq_idx
    io.ftq_commit2_generation := entry2.ftq_generation
    io.ftq_commit3_valid := valid3
    io.ftq_commit3_idx := entry3.ftq_idx
    io.ftq_commit3_generation := entry3.ftq_generation

    for (lane <- 0 until 4) {
      io.trace_commit(lane).valid := valid(lane) && !entry(lane).state.state &&
        !entry(lane).is_fencei && !entry(lane).is_ebreak
      io.trace_commit(lane).bits.pc := entry(lane).pc
      io.trace_commit(lane).bits.inst := entry(lane).inst
      io.trace_commit(lane).bits.bpIndex := entry(lane).bp_index
      io.trace_commit(lane).bits.actualNextPc := entry(lane).actual_target
    }
    io.trace_invalidate := valid.zip(entry).map { case (fire, bits) =>
      fire && bits.is_fencei
    }.reduce(_ || _)
  }
}
