package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import common.ALU_OP._
import common.JUMP_TYPE._
import common.OoOParams
import core.NPC_Config

class WideUnitTest extends AnyFlatSpec {
  private val width = OoOParams.CORE_WIDTH
  private val conf = NPC_Config()

  private def resetDut(clock: Clock, reset: Reset): Unit = {
    reset.poke(true.B)
    clock.step()
    reset.poke(false.B)
    clock.step()
  }

  private def zeroRobEntry(e: ROBEntry): Unit = {
    e.valid.poke(false.B)
    e.done.poke(false.B)
    e.issued.poke(false.B)
    e.pc.poke(0.U)
    e.inst.poke(0.U)
    e.reg_write.poke(false.B)
    e.reg_write_sel.poke(0.U)
    e.csr_write.poke(false.B)
    e.csr_sel.poke(0.U)
    e.mem_valid.poke(false.B)
    e.mem_write.poke(false.B)
    e.mem_rd.poke(0.U)
    e.mem_wmask.poke(0.U)
    e.alu_control.poke(0.U)
    e.alu_srcA.poke(0.U)
    e.alu_srcB.poke(0.U)
    e.jump.poke(JUMP_NONE)
    e.imm_ext.poke(0.U)
    e.arch_rd.poke(0.U)
    e.src1_phys.poke(0.U)
    e.src2_phys.poke(0.U)
    e.old_phys.poke(0.U)
    e.new_phys.poke(0.U)
    e.dest_val.poke(0.U)
    e.is_ebreak.poke(false.B)
    e.is_fencei.poke(false.B)
    e.state.state.poke(false.B)
    e.state.state_num.poke(0.U)
    e.bp_valid.poke(false.B)
    e.bp_taken.poke(false.B)
    e.bp_target.poke(0.U)
    e.bp_index.poke(0.U)
    e.ftq_idx.poke(0.U)
    e.ftq_generation.poke(0.U)
    e.cp_idx.poke(0.U)
    e.actual_taken.poke(false.B)
    e.actual_target.poke(0.U)
    e.rs1_val.poke(0.U)
    e.rs2_val.poke(0.U)
    e.csr_waddr.poke(0.U)
    e.csr_rd1.poke(0.U)
    e.mem_addr.poke(0.U)
    e.mem_wdata.poke(0.U)
    e.addr_ready.poke(false.B)
  }

  private def zeroRsEntry(e: RSEntry): Unit = {
    e.valid.poke(false.B)
    e.issued.poke(false.B)
    e.rob_idx.poke(0.U)
    e.cp_idx.poke(0.U)
    e.src1_ready.poke(true.B)
    e.src2_ready.poke(true.B)
    e.src1_phys.poke(0.U)
    e.src2_phys.poke(0.U)
    e.src1_val.poke(0.U)
    e.src2_val.poke(0.U)
    e.pdest.poke(0.U)
    e.old_phys.poke(0.U)
    e.do_rename.poke(false.B)
    e.pc.poke(0.U)
    e.inst.poke(0.U)
    e.imm_ext.poke(0.U)
    e.waddr.poke(0.U)
    e.is_ebreak.poke(false.B)
    e.is_fencei.poke(false.B)
    e.csr_rd1.poke(0.U)
    e.csr_waddr.poke(0.U)
    e.state.state.poke(false.B)
    e.state.state_num.poke(0.U)
    e.bp_valid.poke(false.B)
    e.bp_taken.poke(false.B)
    e.bp_target.poke(0.U)
    e.bp_index.poke(0.U)
    e.ftq_idx.poke(0.U)
    e.ftq_generation.poke(0.U)
    e.exu_alu_srcA.poke(0.U)
    e.exu_alu_srcB.poke(0.U)
    e.exu_alu_control.poke(ALU_ADD)
    e.exu_jump.poke(JUMP_NONE)
    e.lsu_mem_wmask.poke(0.U)
    e.lsu_mem_rd.poke(0.U)
    e.lsu_mem_write.poke(false.B)
    e.lsu_mem_valid.poke(false.B)
    e.wbu_reg_write.poke(false.B)
    e.wbu_reg_write_sel.poke(0.U)
    e.wbu_csr_write.poke(false.B)
    e.wbu_csr_sel.poke(0.U)
  }

  behavior of "WidePRF and WideBusyTable"

  it should "accept four independent result broadcasts" in {
    simulate(new WidePRF(conf)) { dut =>
      for (i <- 0 until width * 2) dut.io.raddr(i).poke(0.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      for (i <- 0 until width) {
        dut.io.wen(i).poke(false.B)
        dut.io.waddr(i).poke(0.U)
        dut.io.wdata(i).poke(0.U)
      }
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until width) {
        dut.io.wen(i).poke(true.B)
        dut.io.waddr(i).poke((32 + i).U)
        dut.io.wdata(i).poke((0x1000 + i).U)
      }
      dut.clock.step()
      for (i <- 0 until width) {
        dut.io.raddr(i).poke((32 + i).U)
        dut.io.rdata(i).expect((0x1000 + i).U)
      }
      dut.io.wen(0).poke(true.B)
      dut.io.waddr(0).poke(0.U)
      dut.io.wdata(0).poke("hdeadbeef".U)
      for (i <- 1 until width) dut.io.wen(i).poke(false.B)
      dut.clock.step()
      dut.io.raddr(0).poke(0.U)
      dut.io.rdata(0).expect(0.U)
    }
  }

  it should "set and clear four busy bits in one cycle" in {
    simulate(new WideBusyTable()) { dut =>
      for (i <- 0 until width * 2) dut.io.raddr(i).poke((32 + (i % width)).U)
      for (i <- 0 until width) {
        dut.io.set_valid(i).poke(false.B)
        dut.io.set_addr(i).poke(0.U)
        dut.io.clr_valid(i).poke(false.B)
        dut.io.clr_addr(i).poke(0.U)
      }
      dut.io.clr_mask.poke(0.U)
      dut.io.rebuild.poke(false.B)
      dut.io.rebuild_mask.poke(0.U)
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until width) {
        dut.io.set_valid(i).poke(true.B)
        dut.io.set_addr(i).poke((32 + i).U)
      }
      dut.clock.step()
      for (i <- 0 until width) dut.io.ready(i).expect(false.B)

      for (i <- 0 until width) {
        dut.io.set_valid(i).poke(i == 0)
        dut.io.set_addr(i).poke(32.U)
        dut.io.clr_valid(i).poke(true.B)
        dut.io.clr_addr(i).poke((32 + i).U)
      }
      dut.clock.step()
      for (i <- 0 until width) dut.io.ready(i).expect(true.B)
    }
  }

  behavior of "WideRename"

  it should "bypass four-lane WAW and RAW dependencies" in {
    simulate(new WideRename()) { dut =>
      for (i <- 0 until width) {
        dut.io.fire(i).poke(false.B)
        dut.io.rs1(i).poke(0.U)
        dut.io.rs2(i).poke(0.U)
        dut.io.rd(i).poke(0.U)
        dut.io.reg_write(i).poke(false.B)
        dut.io.is_branch(i).poke(false.B)
        dut.io.rob_idx(i).poke(i.U)
        dut.io.commit_fire(i).poke(false.B)
        dut.io.commit_do_rename(i).poke(false.B)
        dut.io.commit_old_phys(i).poke(0.U)
        dut.io.commit_new_phys(i).poke(0.U)
        dut.io.commit_arch_rd(i).poke(0.U)
        dut.io.commit_cp_valid(i).poke(false.B)
        dut.io.commit_cp_idx(i).poke(0.U)
      }
      dut.io.rob_head.poke(0.U)
      dut.io.restore_cp.poke(false.B)
      dut.io.restore_cp_idx.poke(0.U)
      dut.io.restore_arch.poke(false.B)
      dut.io.rebuild.poke(false.B)
      for (i <- 0 until 32) dut.io.rebuild_rat(i).poke(i.U)
      dut.io.rebuild_free.poke(0.U)
      dut.io.reserve_mask.poke(0.U)
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until width) {
        dut.io.fire(i).poke(true.B)
        dut.io.reg_write(i).poke(true.B)
      }
      dut.io.rd(0).poke(5.U)
      dut.io.rd(1).poke(5.U)
      dut.io.rd(2).poke(6.U)
      dut.io.rd(3).poke(7.U)
      dut.io.rs1(1).poke(5.U)
      dut.io.rs1(2).poke(5.U)
      dut.io.rs1(3).poke(6.U)

      dut.io.pdest(0).expect(32.U)
      dut.io.pdest(1).expect(33.U)
      dut.io.pdest(2).expect(34.U)
      dut.io.pdest(3).expect(35.U)
      dut.io.old_phys(0).expect(5.U)
      dut.io.old_phys(1).expect(32.U)
      dut.io.psrc1(1).expect(32.U)
      dut.io.psrc1(2).expect(33.U)
      dut.io.psrc1(3).expect(34.U)
      dut.clock.step()

      dut.io.rat_out(5).expect(33.U)
      dut.io.rat_out(6).expect(34.U)
      dut.io.rat_out(7).expect(35.U)
      dut.io.free_cnt.expect(28.U)
    }
  }

  it should "exclude live ownership from a stale free-list bit" in {
    simulate(new WideRename()) { dut =>
      for (i <- 0 until width) {
        dut.io.fire(i).poke(false.B)
        dut.io.rs1(i).poke(0.U)
        dut.io.rs2(i).poke(0.U)
        dut.io.rd(i).poke(0.U)
        dut.io.reg_write(i).poke(false.B)
        dut.io.is_branch(i).poke(false.B)
        dut.io.rob_idx(i).poke(i.U)
        dut.io.commit_fire(i).poke(false.B)
        dut.io.commit_do_rename(i).poke(false.B)
        dut.io.commit_old_phys(i).poke(0.U)
        dut.io.commit_new_phys(i).poke(0.U)
        dut.io.commit_arch_rd(i).poke(0.U)
        dut.io.commit_cp_valid(i).poke(false.B)
        dut.io.commit_cp_idx(i).poke(0.U)
      }
      dut.io.rob_head.poke(0.U)
      dut.io.restore_cp.poke(false.B)
      dut.io.restore_cp_idx.poke(0.U)
      dut.io.restore_arch.poke(false.B)
      dut.io.rebuild.poke(false.B)
      for (i <- 0 until 32) dut.io.rebuild_rat(i).poke(i.U)
      dut.io.rebuild_free.poke(0.U)
      dut.io.reserve_mask.poke((BigInt(1) << 32).U)
      resetDut(dut.clock, dut.reset)

      dut.io.fire(0).poke(true.B)
      dut.io.reg_write(0).poke(true.B)
      dut.io.rd(0).poke(5.U)
      dut.io.pdest(0).expect(33.U)
      dut.io.free_cnt.expect(31.U)
    }
  }

  behavior of "WideROB"

  it should "enqueue, complete, and retire four instructions together" in {
    simulate(new WideROB()) { dut =>
      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
        zeroRobEntry(dut.io.enq_bits(i))
        dut.io.wb_fire(i).poke(false.B)
        dut.io.wb_idx(i).poke(0.U)
        dut.io.wb_val(i).poke(0.U)
        dut.io.wb_state(i).state.poke(false.B)
        dut.io.wb_state(i).state_num.poke(0.U)
        dut.io.wb_mem_addr(i).poke(0.U)
        dut.io.wb_mem_wdata(i).poke(0.U)
        dut.io.wb_actual_taken(i).poke(false.B)
        dut.io.wb_actual_target(i).poke(0.U)
        dut.io.commit_fire(i).poke(false.B)
      }
      dut.io.ctrl_wb_fire.poke(false.B)
      dut.io.ctrl_wb_idx.poke(0.U)
      dut.io.ctrl_wb_state.state.poke(false.B)
      dut.io.ctrl_wb_state.state_num.poke(0.U)
      dut.io.ctrl_wb_actual_taken.poke(false.B)
      dut.io.ctrl_wb_actual_target.poke(0.U)
      dut.io.store_wb_fire.poke(false.B)
      dut.io.store_wb_idx.poke(0.U)
      dut.io.store_wb_state.state.poke(false.B)
      dut.io.store_wb_state.state_num.poke(0.U)
      dut.io.store_wb_mem_addr.poke(0.U)
      dut.io.store_wb_mem_wdata.poke(0.U)
      dut.io.flush.poke(false.B)
      dut.io.flush_idx.poke(0.U)
      dut.io.flush_all.poke(false.B)
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(true.B)
        dut.io.enq_bits(i).pc.poke((0x80000000L + i * 4).U)
        dut.io.enq_bits(i).reg_write.poke(true.B)
        dut.io.enq_bits(i).arch_rd.poke((i + 1).U)
        dut.io.enq_bits(i).new_phys.poke((32 + i).U)
        dut.io.enq_idx(i).expect(i.U)
      }
      dut.clock.step()
      dut.io.count.expect(4.U)
      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
        dut.io.wb_fire(i).poke(true.B)
        dut.io.wb_idx(i).poke(i.U)
        dut.io.wb_val(i).poke((0x2000 + i).U)
        dut.io.commit_valid(i).expect(true.B)
        dut.io.commit_fire(i).poke(true.B)
      }
      dut.clock.step()
      dut.io.count.expect(0.U)
      dut.io.head.expect(4.U)
      dut.io.tail.expect(4.U)
    }
  }

  behavior of "WideRS"

  it should "fresh-issue four ordinary ALU operations" in {
    simulate(new WideRS()) { dut =>
      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
        zeroRsEntry(dut.io.enq_bits(i))
        dut.io.issue_alu_fire(i).poke(false.B)
        dut.io.free_data_fire(i).poke(false.B)
        dut.io.free_data_idx(i).poke(0.U)
        dut.io.cdb_valid(i).poke(false.B)
        dut.io.cdb_pdest(i).poke(0.U)
        dut.io.cdb_val(i).poke(0.U)
      }
      dut.io.rob_head.poke(0.U)
      dut.io.rob_st_pending.poke(0.U)
      dut.io.issue_div_fire.poke(false.B)
      dut.io.issue_lsu_fire.poke(false.B)
      dut.io.issue_lsu1_fire.poke(false.B)
      dut.io.free_ctrl_fire.poke(false.B)
      dut.io.free_ctrl_idx.poke(0.U)
      dut.io.free_store_fire.poke(false.B)
      dut.io.free_store_idx.poke(0.U)
      dut.io.flush.poke(false.B)
      dut.io.flush_idx.poke(0.U)
      dut.io.flush_all.poke(false.B)
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(true.B)
        dut.io.enq_bits(i).rob_idx.poke(i.U)
        dut.io.enq_bits(i).pc.poke((0x80000000L + i * 4).U)
        dut.io.issue_alu_valid(i).expect(true.B)
        dut.io.issue_alu_bits(i).rob_idx.expect(i.U)
        dut.io.issue_alu_fire(i).poke(true.B)
      }
      dut.io.fresh_issue_count.expect(4.U)
      dut.clock.step()
      dut.io.count.expect(4.U)

      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
        dut.io.issue_alu_fire(i).poke(false.B)
        dut.io.free_data_fire(i).poke(true.B)
        dut.io.free_data_idx(i).poke(i.U)
      }
      dut.clock.step()
      dut.io.count.expect(0.U)
    }
  }

  it should "issue a secondary load independently beside a store" in {
    simulate(new WideRS()) { dut =>
      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
        zeroRsEntry(dut.io.enq_bits(i))
        dut.io.issue_alu_fire(i).poke(false.B)
        dut.io.free_data_fire(i).poke(false.B)
        dut.io.free_data_idx(i).poke(0.U)
        dut.io.cdb_valid(i).poke(false.B)
        dut.io.cdb_pdest(i).poke(0.U)
        dut.io.cdb_val(i).poke(0.U)
      }
      dut.io.rob_head.poke(0.U)
      dut.io.rob_st_pending.poke(0.U)
      dut.io.issue_div_fire.poke(false.B)
      dut.io.issue_lsu_fire.poke(false.B)
      dut.io.issue_lsu1_fire.poke(false.B)
      dut.io.free_ctrl_fire.poke(false.B)
      dut.io.free_ctrl_idx.poke(0.U)
      dut.io.free_store_fire.poke(false.B)
      dut.io.free_store_idx.poke(0.U)
      dut.io.flush.poke(false.B)
      dut.io.flush_idx.poke(0.U)
      dut.io.flush_all.poke(false.B)
      resetDut(dut.clock, dut.reset)

      for (i <- 0 until 2) {
        dut.io.enq_fire(i).poke(true.B)
        dut.io.enq_bits(i).rob_idx.poke((i + 1).U)
        dut.io.enq_bits(i).pc.poke((0x80000100L + i * 4).U)
        dut.io.enq_bits(i).lsu_mem_valid.poke(true.B)
        dut.io.enq_bits(i).lsu_mem_write.poke(false.B)
      }
      dut.io.issue_lsu_valid.expect(true.B)
      dut.io.issue_lsu_bits.rob_idx.expect(1.U)
      dut.io.issue_lsu1_valid.expect(true.B)
      dut.io.issue_lsu1_bits.rob_idx.expect(2.U)
      dut.io.issue_lsu_fire.poke(true.B)
      dut.io.issue_lsu1_fire.poke(true.B)
      dut.clock.step()

      for (i <- 0 until width) {
        dut.io.enq_fire(i).poke(false.B)
      }
      dut.io.issue_lsu_fire.poke(false.B)
      dut.io.issue_lsu1_fire.poke(false.B)
      dut.io.flush.poke(true.B)
      dut.io.flush_all.poke(true.B)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.flush_all.poke(false.B)

      dut.io.enq_fire(0).poke(true.B)
      dut.io.enq_bits(0).rob_idx.poke(3.U)
      dut.io.enq_bits(0).lsu_mem_valid.poke(true.B)
      dut.io.enq_bits(0).lsu_mem_write.poke(true.B)
      dut.io.enq_fire(1).poke(true.B)
      dut.io.enq_bits(1).rob_idx.poke(4.U)
      dut.io.enq_bits(1).lsu_mem_valid.poke(true.B)
      dut.io.enq_bits(1).lsu_mem_write.poke(false.B)
      // A same-cycle younger load cannot snapshot the fresh older store yet.
      dut.io.issue_lsu_valid.expect(true.B)
      dut.io.issue_lsu_bits.rob_idx.expect(3.U)
      dut.io.issue_lsu1_valid.expect(false.B)
      dut.clock.step()

      dut.io.enq_fire(0).poke(false.B)
      dut.io.enq_fire(1).poke(false.B)
      // Once resident, the secondary load may issue independently while the
      // older store remains on the primary port for dependency tracking.
      dut.io.issue_lsu_valid.expect(true.B)
      dut.io.issue_lsu_bits.rob_idx.expect(3.U)
      dut.io.issue_lsu1_valid.expect(true.B)
      dut.io.issue_lsu1_bits.rob_idx.expect(4.U)
      dut.io.issue_lsu1_bits.lsu_mem_write.expect(false.B)
      dut.io.issue_lsu1_fire.poke(true.B)
      dut.clock.step()

      dut.io.enq_fire(0).poke(false.B)
      dut.io.enq_fire(1).poke(false.B)
      dut.io.issue_lsu1_fire.poke(false.B)
      dut.io.issue_lsu_valid.expect(true.B)
      dut.io.issue_lsu_bits.rob_idx.expect(3.U)
      dut.io.issue_lsu_bits.lsu_mem_write.expect(true.B)
      dut.io.issue_lsu1_valid.expect(false.B)
    }
  }
}
