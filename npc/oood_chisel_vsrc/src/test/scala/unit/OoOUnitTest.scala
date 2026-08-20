package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import core.NPC_Config
import common.MEM_READ._
import common.MEM_WMASK._

/** 四件套模块级仿真 —— 必须全绿才能接线进 core */
class OoOUnitTest extends AnyFlatSpec {

  val conf = NPC_Config()

  def resetDut(clock: Clock, reset: Reset): Unit = {
    reset.poke(true.B)
    clock.step()
    reset.poke(false.B)
    clock.step()
  }

  // ---------- PRF ----------
  behavior of "PRF"

  it should "keep phys0 as x0" in {
    simulate(new PRF(conf)) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.wen1.poke(true.B)
      dut.io.waddr1.poke(0.U)
      dut.io.wdata1.poke("hdeadbeef".U)
      dut.io.wen2.poke(false.B)
      dut.io.waddr2.poke(0.U)
      dut.io.wdata2.poke(0.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      dut.clock.step()
      dut.io.raddr1.poke(0.U)
      dut.io.rdata1.expect(0.U)
    }
  }

  it should "write and read back" in {
    simulate(new PRF(conf)) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.wen1.poke(true.B)
      dut.io.waddr1.poke(5.U)
      dut.io.wdata1.poke(0x1234.U)
      dut.io.wen2.poke(false.B)
      dut.io.waddr2.poke(0.U)
      dut.io.wdata2.poke(0.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      dut.clock.step()
      dut.io.raddr1.poke(5.U)
      dut.io.rdata1.expect(0x1234.U)
    }
  }

  it should "dual write ports" in {
    simulate(new PRF(conf)) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.wen1.poke(true.B)
      dut.io.waddr1.poke(3.U)
      dut.io.wdata1.poke(0x1111.U)
      dut.io.wen2.poke(true.B)
      dut.io.waddr2.poke(7.U)
      dut.io.wdata2.poke(0x2222.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      dut.clock.step()
      dut.io.raddr1.poke(3.U)
      dut.io.rdata1.expect(0x1111.U)
      dut.io.raddr2.poke(7.U)
      dut.io.rdata2.expect(0x2222.U)
    }
  }

  // ---------- BusyTable ----------
  behavior of "BusyTable"

  def idleBusy(dut: BusyTable): Unit = {
    dut.io.set_en.poke(false.B)
    dut.io.clr_en.poke(false.B)
    dut.io.set_addr.poke(0.U)
    dut.io.clr_addr.poke(0.U)
    dut.io.clr_mask.poke(0.U)
    dut.io.rebuild.poke(false.B)
    dut.io.rebuild_mask.poke(0.U)
    dut.io.raddr1.poke(0.U)
    dut.io.raddr2.poke(0.U)
  }

  it should "reset ready" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.raddr1.poke(10.U)
      dut.io.ready1.expect(true.B)
    }
  }

  it should "set then clear" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.raddr1.poke(32.U)
      dut.io.raddr2.poke(0.U)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(32.U)
      dut.clock.step()
      dut.io.ready1.expect(false.B)
      dut.io.set_en.poke(false.B)
      dut.io.clr_en.poke(true.B)
      dut.io.clr_addr.poke(32.U)
      dut.clock.step()
      dut.io.ready1.expect(true.B)
    }
  }

  it should "prefer clear over set same cycle" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(40.U)
      dut.io.raddr1.poke(40.U)
      dut.clock.step()
      dut.io.ready1.expect(false.B)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(40.U)
      dut.io.clr_en.poke(true.B)
      dut.io.clr_addr.poke(40.U)
      dut.clock.step()
      dut.io.ready1.expect(true.B)
    }
  }

  it should "clr_mask clear multiple" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(33.U)
      dut.clock.step()
      dut.io.set_addr.poke(34.U)
      dut.clock.step()
      dut.io.set_en.poke(false.B)
      dut.io.raddr1.poke(33.U)
      dut.io.ready1.expect(false.B)
      // bit33|bit34
      dut.io.clr_mask.poke(((BigInt(1) << 33) | (BigInt(1) << 34)).U)
      dut.clock.step()
      dut.io.clr_mask.poke(0.U)
      dut.io.raddr1.poke(33.U)
      dut.io.ready1.expect(true.B)
      dut.io.raddr1.poke(34.U)
      dut.io.ready1.expect(true.B)
    }
  }

  // ---------- Rename ----------
  behavior of "Rename"

  def idleRename(dut: Rename): Unit = {
    dut.io.fire.poke(false.B)
    dut.io.rs1.poke(0.U)
    dut.io.rs2.poke(0.U)
    dut.io.rd.poke(0.U)
    dut.io.reg_write.poke(false.B)
    dut.io.commit_fire.poke(false.B)
    dut.io.cm_do_ren.poke(false.B)
    dut.io.cm_old_phys.poke(0.U)
    dut.io.cm_new_phys.poke(0.U)
    dut.io.cm_arch_rd.poke(0.U)
    dut.io.rb_fire.poke(false.B)
    dut.io.rb_do_ren.poke(false.B)
    dut.io.rb_arch_rd.poke(0.U)
    dut.io.rb_old_phys.poke(0.U)
    dut.io.rb_new_phys.poke(0.U)
    dut.io.restore_arch.poke(false.B)
    dut.io.free_mask.poke(0.U)
    for (i <- 0 until 3) dut.io.free_vec(i).poke(0.U)
    dut.io.rebuild.poke(false.B)
    dut.io.rebuild_free.poke(0.U)
    for (i <- 0 until 32) dut.io.rebuild_rat(i).poke(i.U)
  }

  it should "not allocate when reg_write=0 (store ★1)" in {
    simulate(new Rename()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRename(dut)
      // fire 但 reg_write=0：不得 pop freelist
      dut.io.fire.poke(true.B)
      dut.io.rs1.poke(1.U)
      dut.io.rs2.poke(2.U)
      dut.io.rd.poke(5.U)
      dut.io.reg_write.poke(false.B)
      dut.io.do_rename.expect(false.B)
      dut.io.dest_phys.expect(0.U)
      dut.clock.step()
      // 下一拍真 rename，应从 32 开始
      dut.io.reg_write.poke(true.B)
      dut.io.rd.poke(3.U)
      dut.io.do_rename.expect(true.B)
      dut.io.dest_phys.expect(32.U)
      dut.clock.step()
    }
  }

  it should "allocate and update RAT" in {
    simulate(new Rename()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRename(dut)
      dut.io.fire.poke(true.B)
      dut.io.rs1.poke(0.U)
      dut.io.rs2.poke(0.U)
      dut.io.rd.poke(1.U)
      dut.io.reg_write.poke(true.B)
      dut.io.do_rename.expect(true.B)
      dut.io.old_phys.expect(1.U)
      dut.io.dest_phys.expect(32.U)
      dut.clock.step()
      dut.io.fire.poke(false.B)
      dut.io.reg_write.poke(false.B)
      dut.io.rs1.poke(1.U)
      dut.io.rs1_phys.expect(32.U)
    }
  }

  it should "commit update arch_rat" in {
    simulate(new Rename()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRename(dut)
      dut.io.fire.poke(true.B)
      dut.io.rd.poke(2.U)
      dut.io.reg_write.poke(true.B)
      val oldp = dut.io.old_phys.peek().litValue
      val newp = dut.io.dest_phys.peek().litValue
      dut.clock.step()
      idleRename(dut)
      dut.io.commit_fire.poke(true.B)
      dut.io.cm_do_ren.poke(true.B)
      dut.io.cm_old_phys.poke(oldp.U)
      dut.io.cm_new_phys.poke(newp.U)
      dut.io.cm_arch_rd.poke(2.U)
      dut.clock.step()
      dut.io.arch_rat_out(2).expect(newp.U)
    }
  }

  it should "rollback restore mapping" in {
    simulate(new Rename()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRename(dut)
      dut.io.fire.poke(true.B)
      dut.io.rd.poke(4.U)
      dut.io.reg_write.poke(true.B)
      val oldp = dut.io.old_phys.peek().litValue
      val newp = dut.io.dest_phys.peek().litValue
      dut.clock.step()
      idleRename(dut)
      dut.io.rb_fire.poke(true.B)
      dut.io.rb_do_ren.poke(true.B)
      dut.io.rb_arch_rd.poke(4.U)
      dut.io.rb_old_phys.poke(oldp.U)
      dut.io.rb_new_phys.poke(newp.U)
      dut.clock.step()
      idleRename(dut)
      dut.io.rs1.poke(4.U)
      dut.io.rs1_phys.expect(oldp.U)
    }
  }

  // ---------- ROB：字段级 poke，不 poke 整个 Bundle ----------
  behavior of "ROB"

  def idleRob(dut: ROB): Unit = {
    dut.io.enq_fire.poke(false.B)
    dut.io.issue_fire.poke(false.B)
    dut.io.wb_fire.poke(false.B)
    dut.io.commit_fire.poke(false.B)
    dut.io.flush.poke(false.B)
    dut.io.flush_all.poke(false.B)
    dut.io.flush_idx.poke(0.U)
    dut.io.wb_idx.poke(0.U)
    dut.io.wb_val.poke(0.U)
    dut.io.wb_mem_addr.poke(0.U)
    dut.io.wb_mem_wdata.poke(0.U)
    dut.io.wb_actual_taken.poke(false.B)
    dut.io.wb_state.state.poke(false.B)
    dut.io.wb_state.state_num.poke(0.U)
    // zero critical enq fields
    dut.io.enq_bits.valid.poke(false.B)
    dut.io.enq_bits.done.poke(false.B)
    dut.io.enq_bits.issued.poke(false.B)
    dut.io.enq_bits.pc.poke(0.U)
    dut.io.enq_bits.inst.poke(0.U)
    dut.io.enq_bits.reg_write.poke(false.B)
    dut.io.enq_bits.reg_write_sel.poke(0.U)
    dut.io.enq_bits.csr_write.poke(false.B)
    dut.io.enq_bits.csr_sel.poke(0.U)
    dut.io.enq_bits.mem_valid.poke(false.B)
    dut.io.enq_bits.mem_write.poke(false.B)
    dut.io.enq_bits.mem_rd.poke(0.U)
    dut.io.enq_bits.mem_wmask.poke(0.U)
    dut.io.enq_bits.alu_control.poke(0.U)
    dut.io.enq_bits.alu_srcA.poke(0.U)
    dut.io.enq_bits.alu_srcB.poke(0.U)
    dut.io.enq_bits.jump.poke(0.U)
    dut.io.enq_bits.imm_ext.poke(0.U)
    dut.io.enq_bits.arch_rd.poke(0.U)
    dut.io.enq_bits.src1_phys.poke(0.U)
    dut.io.enq_bits.src2_phys.poke(0.U)
    dut.io.enq_bits.old_phys.poke(0.U)
    dut.io.enq_bits.new_phys.poke(0.U)
    dut.io.enq_bits.dest_val.poke(0.U)
    dut.io.enq_bits.is_ebreak.poke(false.B)
    dut.io.enq_bits.is_fencei.poke(false.B)
    dut.io.enq_bits.state.state.poke(false.B)
    dut.io.enq_bits.state.state_num.poke(0.U)
    dut.io.enq_bits.bp_valid.poke(false.B)
    dut.io.enq_bits.bp_taken.poke(false.B)
    dut.io.enq_bits.bp_target.poke(0.U)
    dut.io.enq_bits.bp_index.poke(0.U)
    dut.io.enq_bits.rs1_val.poke(0.U)
    dut.io.enq_bits.rs2_val.poke(0.U)
    dut.io.enq_bits.csr_waddr.poke(0.U)
    dut.io.enq_bits.csr_rd1.poke(0.U)
    dut.io.enq_bits.mem_addr.poke(0.U)
  }

  it should "fill 16 entries" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      for (i <- 0 until 16) {
        dut.io.enq_fire.poke(true.B)
        dut.io.enq_bits.pc.poke(i.U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.full.expect(true.B)
      dut.io.count.expect(16.U)
    }
  }

  it should "issue wb commit pipeline" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.io.enq_bits.reg_write.poke(true.B)
      dut.io.enq_bits.arch_rd.poke(1.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)

      dut.io.issue_valid.expect(true.B)
      val idx = dut.io.issue_idx.peek().litValue
      dut.io.issue_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_fire.poke(false.B)
      dut.io.issue_valid.expect(false.B)

      dut.io.wb_fire.poke(true.B)
      dut.io.wb_idx.poke(idx.U)
      dut.io.wb_val.poke(0x42.U)
      dut.clock.step()
      dut.io.wb_fire.poke(false.B)

      dut.io.commit_valid.expect(true.B)
      dut.io.commit_bits.dest_val.expect(0x42.U)
      dut.io.commit_fire.poke(true.B)
      dut.clock.step()
      dut.io.commit_fire.poke(false.B)
      dut.io.count.expect(0.U)
    }
  }

  it should "flush after flush_idx" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      for (i <- 0 until 3) {
        dut.io.enq_fire.poke(true.B)
        dut.io.enq_bits.pc.poke((0x1000 + i).U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(3.U)
      dut.io.flush.poke(true.B)
      dut.io.flush_idx.poke(0.U)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.tail.expect(1.U)
    }
  }

  it should "invalidate younger entries when full and tail wraps to head" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      for (i <- 0 until 16) {
        dut.io.enq_bits.pc.poke((0x2000 + i).U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(16.U)
      dut.io.head.expect(0.U)
      dut.io.tail.expect(0.U)
      dut.io.flush.poke(true.B)
      dut.io.flush_idx.poke(0.U)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.tail.expect(1.U)
      dut.io.entries(0).valid.expect(true.B)
      dut.io.entries(1).valid.expect(false.B)
      dut.io.entries(15).valid.expect(false.B)
    }
  }

  // ---------- RS（3b 壳）----------
  behavior of "RS"

  def idleRs(dut: RS): Unit = {
    dut.io.enq_fire.poke(false.B)
    dut.io.issue_fire.poke(false.B)
    dut.io.free_rob_fire.poke(false.B)
    dut.io.free_rob_idx.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flush_all.poke(false.B)
    dut.io.flush_idx.poke(0.U)
    dut.io.rob_head.poke(0.U)
    dut.io.rob_st_pending.poke(0.U)
    dut.io.cdb_valid.poke(false.B)
    dut.io.cdb_pdest.poke(0.U)
    dut.io.cdb_val.poke(0.U)
    dut.io.enq_bits.valid.poke(false.B)
    dut.io.enq_bits.rob_idx.poke(0.U)
    dut.io.enq_bits.src1_ready.poke(true.B)
    dut.io.enq_bits.src2_ready.poke(true.B)
    dut.io.enq_bits.src1_phys.poke(0.U)
    dut.io.enq_bits.src2_phys.poke(0.U)
    dut.io.enq_bits.src1_val.poke(0.U)
    dut.io.enq_bits.src2_val.poke(0.U)
    dut.io.enq_bits.pdest.poke(0.U)
    dut.io.enq_bits.old_phys.poke(0.U)
    dut.io.enq_bits.do_rename.poke(false.B)
    dut.io.enq_bits.pc.poke(0.U)
    dut.io.enq_bits.inst.poke(0.U)
    dut.io.enq_bits.imm_ext.poke(0.U)
    dut.io.enq_bits.waddr.poke(0.U)
    dut.io.enq_bits.is_ebreak.poke(false.B)
    dut.io.enq_bits.is_fencei.poke(false.B)
    dut.io.enq_bits.csr_rd1.poke(0.U)
    dut.io.enq_bits.csr_waddr.poke(0.U)
    dut.io.enq_bits.state.state.poke(false.B)
    dut.io.enq_bits.state.state_num.poke(0.U)
    dut.io.enq_bits.bp_valid.poke(false.B)
    dut.io.enq_bits.bp_taken.poke(false.B)
    dut.io.enq_bits.bp_target.poke(0.U)
    dut.io.enq_bits.bp_index.poke(0.U)
    dut.io.enq_bits.exu_alu_srcA.poke(0.U)
    dut.io.enq_bits.exu_alu_srcB.poke(0.U)
    dut.io.enq_bits.exu_alu_control.poke(0.U)
    dut.io.enq_bits.exu_jump.poke("b1111".U) // JUMP_NONE
    dut.io.enq_bits.lsu_mem_wmask.poke(0.U)
    dut.io.enq_bits.lsu_mem_rd.poke(0.U)
    dut.io.enq_bits.lsu_mem_write.poke(false.B)
    dut.io.enq_bits.lsu_mem_valid.poke(false.B)
    dut.io.enq_bits.wbu_reg_write.poke(false.B)
    dut.io.enq_bits.wbu_reg_write_sel.poke(0.U)
    dut.io.enq_bits.wbu_csr_write.poke(false.B)
    dut.io.enq_bits.wbu_csr_sel.poke(0.U)
  }

  it should "issue oldest ready only" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      // enq rob 0 ready
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.clock.step()
      // enq rob 1 ready
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.io.enq_bits.pc.poke("h80000004".U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(2.U)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
      dut.io.issue_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_fire.poke(false.B)
      dut.io.issue_bits.rob_idx.expect(1.U)
    }
  }

  it should "issue younger when oldest not ready (3d OoO)" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.src1_ready.poke(false.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.io.enq_bits.src1_phys.poke(5.U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.io.enq_bits.src1_phys.poke(0.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(1.U)
      dut.io.free_rob_fire.poke(true.B)
      dut.io.free_rob_idx.poke(1.U)
      dut.clock.step()
      dut.io.free_rob_fire.poke(false.B)
      dut.io.issue_valid.expect(false.B)
      dut.io.cdb_valid.poke(true.B)
      dut.io.cdb_pdest.poke(5.U)
      dut.io.cdb_val.poke(0x99.U)
      dut.clock.step()
      dut.io.cdb_valid.poke(false.B)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
      dut.io.issue_bits.src1_val.expect(0x99.U)
    }
  }

  it should "free_rob by idx not issue slot" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
      // free younger while oldest is issue candidate
      dut.io.free_rob_fire.poke(true.B)
      dut.io.free_rob_idx.poke(1.U)
      dut.clock.step()
      dut.io.free_rob_fire.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.issue_bits.rob_idx.expect(0.U)
    }
  }

  it should "prefer oldest among multiple ready" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(2.U)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
    }
  }

  it should "block load until older store address resolves (8d)" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.rob_st_pending.poke(1.U) // rob0 store address unresolved
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.io.enq_bits.lsu_mem_valid.poke(true.B)
      dut.io.enq_bits.lsu_mem_write.poke(false.B)
      dut.io.enq_bits.exu_jump.poke("b1111".U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_valid.expect(false.B)
      dut.io.issue_lsu_valid.expect(false.B)

      // Once the older store address is resolved, StoreQueue can decide
      // forward/wait/pass and RS may issue the load.
      dut.io.rob_st_pending.poke(0.U)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_lsu_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(1.U)
      dut.io.issue_lsu_bits.rob_idx.expect(1.U)
    }
  }

  it should "flush clear all" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.flush.poke(true.B)
      dut.io.flush_all.poke(true.B)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.flush_all.poke(false.B)
      dut.io.count.expect(0.U)
      dut.io.issue_valid.expect(false.B)
      dut.io.full.expect(false.B)
    }
  }

  it should "selective flush keep older" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.src1_ready.poke(true.B)
      dut.io.enq_bits.src2_ready.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(2.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(3.U)
      // flush_idx=0 keep rob0, kill 1,2
      dut.io.flush.poke(true.B)
      dut.io.flush_all.poke(false.B)
      dut.io.flush_idx.poke(0.U)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
    }
  }

  // ---------- FetchQueue（阶段 6）----------
  behavior of "StoreQueue"

  it should "wait on older unresolved store" in {
    simulate(new StoreQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.rob_head.poke(0.U)
      dut.io.entries(0).valid.poke(true.B)
      dut.io.entries(0).mem_valid.poke(true.B)
      dut.io.entries(0).mem_write.poke(true.B)
      dut.io.entries(0).addr_ready.poke(false.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(1.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(true.B)
      dut.io.fwd_valid.expect(false.B)
    }
  }

  it should "forward from older matching store" in {
    simulate(new StoreQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.rob_head.poke(0.U)
      dut.io.entries(0).valid.poke(true.B)
      dut.io.entries(0).mem_valid.poke(true.B)
      dut.io.entries(0).mem_write.poke(true.B)
      dut.io.entries(0).addr_ready.poke(true.B)
      dut.io.entries(0).mem_addr.poke("h1000".U)
      dut.io.entries(0).mem_wmask.poke(WWORD)
      dut.io.entries(0).mem_wdata.poke("h12345678".U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(1.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(true.B)
      dut.io.fwd_data.expect("h12345678".U)
    }
  }

  behavior of "FetchQueue"

  def idleFq(dut: FetchQueue): Unit = {
    dut.io.enq.valid.poke(false.B)
    dut.io.enq.bits.inst.poke(0.U)
    dut.io.enq.bits.pc.poke(0.U)
    dut.io.enq.bits.state.state.poke(false.B)
    dut.io.enq.bits.state.state_num.poke(0.U)
    dut.io.enq.bits.bp_valid.poke(false.B)
    dut.io.enq.bits.bp_taken.poke(false.B)
    dut.io.enq.bits.bp_target.poke(0.U)
    dut.io.enq.bits.bp_index.poke(0.U)
    dut.io.deq.ready.poke(false.B)
    dut.io.deq1.ready.poke(false.B)
    dut.io.flush.poke(false.B)
  }

  it should "enq then deq in order" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.empty.expect(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.pc.poke("h80000000".U)
      dut.io.enq.bits.inst.poke("h00100013".U)
      dut.clock.step()
      dut.io.enq.bits.pc.poke("h80000004".U)
      dut.io.enq.bits.inst.poke("h00200013".U)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(2.U)
      dut.io.deq.valid.expect(true.B)
      dut.io.deq.bits.pc.expect("h80000000".U)
      dut.io.deq.ready.poke(true.B)
      dut.clock.step()
      dut.io.deq.bits.pc.expect("h80000004".U)
      dut.clock.step()
      dut.io.deq.ready.poke(false.B)
      dut.io.empty.expect(true.B)
    }
  }

  it should "fill until full then block enq" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      for (i <- 0 until 4) {
        dut.io.enq.bits.pc.poke((0x80000000L + i * 4).U)
        dut.clock.step()
      }
      dut.io.full.expect(true.B)
      dut.io.enq.ready.expect(false.B)
      dut.io.count.expect(4.U)
    }
  }

  it should "dual deq advance head by two" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      for (i <- 0 until 3) {
        dut.io.enq.bits.pc.poke((0x80000000L + i * 4).U)
        dut.io.enq.bits.inst.poke((0x00100013L + i).U)
        dut.clock.step()
      }
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(3.U)
      dut.io.deq.bits.pc.expect("h80000000".U)
      dut.io.deq1.valid.expect(true.B)
      dut.io.deq1.bits.pc.expect("h80000004".U)
      dut.io.deq.ready.poke(true.B)
      dut.io.deq1.ready.poke(true.B)
      dut.clock.step()
      dut.io.count.expect(1.U)
      dut.io.deq.bits.pc.expect("h80000008".U)
      dut.io.deq1.valid.expect(false.B)
    }
  }

  it should "flush clear all (not just mask valid)" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.pc.poke("h80000000".U)
      dut.clock.step()
      dut.io.enq.bits.pc.poke("h80000004".U)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(2.U)
      dut.io.flush.poke(true.B)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.count.expect(0.U)
      dut.io.empty.expect(true.B)
      dut.io.deq.valid.expect(false.B)
      dut.io.full.expect(false.B)
    }
  }
}
