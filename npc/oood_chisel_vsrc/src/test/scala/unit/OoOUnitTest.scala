package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import core.NPC_Config
import common.MEM_READ._
import common.MEM_WMASK._
import common.OoOParams

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
    dut.io.set_en2.poke(false.B)
    dut.io.set_addr2.poke(0.U)
    dut.io.clr_addr.poke(0.U)
    dut.io.clr_en2.poke(false.B)
    dut.io.clr_addr2.poke(0.U)
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

  it should "clear two writeback destinations in one cycle" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(35.U)
      dut.clock.step()
      dut.io.set_addr.poke(36.U)
      dut.clock.step()
      dut.io.set_en.poke(false.B)
      dut.io.raddr1.poke(35.U)
      dut.io.raddr2.poke(36.U)
      dut.io.ready1.expect(false.B)
      dut.io.ready2.expect(false.B)
      dut.io.clr_en.poke(true.B)
      dut.io.clr_addr.poke(35.U)
      dut.io.clr_en2.poke(true.B)
      dut.io.clr_addr2.poke(36.U)
      dut.clock.step()
      idleBusy(dut)
      dut.io.raddr1.poke(35.U)
      dut.io.raddr2.poke(36.U)
      dut.io.ready1.expect(true.B)
      dut.io.ready2.expect(true.B)
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
    dut.io.enq1_fire.poke(false.B)
    dut.io.issue_fire.poke(false.B)
    dut.io.wb_fire.poke(false.B)
    dut.io.commit_fire.poke(false.B)
    dut.io.commit1_fire.poke(false.B)
    dut.io.flush.poke(false.B)
    dut.io.flush_all.poke(false.B)
    dut.io.flush_idx.poke(0.U)
    dut.io.wb_idx.poke(0.U)
    dut.io.wb_val.poke(0.U)
    dut.io.wb_mem_addr.poke(0.U)
    dut.io.wb_mem_wdata.poke(0.U)
    dut.io.wb_actual_taken.poke(false.B)
    dut.io.wb_actual_target.poke(0.U)
    dut.io.wb_state.state.poke(false.B)
    dut.io.wb_state.state_num.poke(0.U)
    dut.io.wb1_fire.poke(false.B)
    dut.io.wb1_idx.poke(0.U)
    dut.io.wb1_val.poke(0.U)
    dut.io.wb1_mem_addr.poke(0.U)
    dut.io.wb1_mem_wdata.poke(0.U)
    dut.io.wb1_actual_taken.poke(false.B)
    dut.io.wb1_actual_target.poke(0.U)
    dut.io.wb1_state.state.poke(false.B)
    dut.io.wb1_state.state_num.poke(0.U)
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
    dut.io.enq_bits.mem_wdata.poke(0.U)
    dut.io.enq_bits.addr_ready.poke(false.B)
    dut.io.enq_bits.cp_idx.poke(0.U)
    dut.io.enq_bits.actual_taken.poke(false.B)
    dut.io.enq_bits.actual_target.poke(0.U)
    dut.io.enq1_bits.valid.poke(false.B)
    dut.io.enq1_bits.done.poke(false.B)
    dut.io.enq1_bits.issued.poke(false.B)
    dut.io.enq1_bits.pc.poke(0.U)
    dut.io.enq1_bits.inst.poke(0.U)
    dut.io.enq1_bits.reg_write.poke(false.B)
    dut.io.enq1_bits.reg_write_sel.poke(0.U)
    dut.io.enq1_bits.csr_write.poke(false.B)
    dut.io.enq1_bits.csr_sel.poke(0.U)
    dut.io.enq1_bits.mem_valid.poke(false.B)
    dut.io.enq1_bits.mem_write.poke(false.B)
    dut.io.enq1_bits.mem_rd.poke(0.U)
    dut.io.enq1_bits.mem_wmask.poke(0.U)
    dut.io.enq1_bits.alu_control.poke(0.U)
    dut.io.enq1_bits.alu_srcA.poke(0.U)
    dut.io.enq1_bits.alu_srcB.poke(0.U)
    dut.io.enq1_bits.jump.poke(0.U)
    dut.io.enq1_bits.imm_ext.poke(0.U)
    dut.io.enq1_bits.arch_rd.poke(0.U)
    dut.io.enq1_bits.src1_phys.poke(0.U)
    dut.io.enq1_bits.src2_phys.poke(0.U)
    dut.io.enq1_bits.old_phys.poke(0.U)
    dut.io.enq1_bits.new_phys.poke(0.U)
    dut.io.enq1_bits.dest_val.poke(0.U)
    dut.io.enq1_bits.is_ebreak.poke(false.B)
    dut.io.enq1_bits.is_fencei.poke(false.B)
    dut.io.enq1_bits.state.state.poke(false.B)
    dut.io.enq1_bits.state.state_num.poke(0.U)
    dut.io.enq1_bits.bp_valid.poke(false.B)
    dut.io.enq1_bits.bp_taken.poke(false.B)
    dut.io.enq1_bits.bp_target.poke(0.U)
    dut.io.enq1_bits.bp_index.poke(0.U)
    dut.io.enq1_bits.rs1_val.poke(0.U)
    dut.io.enq1_bits.rs2_val.poke(0.U)
    dut.io.enq1_bits.csr_waddr.poke(0.U)
    dut.io.enq1_bits.csr_rd1.poke(0.U)
    dut.io.enq1_bits.mem_addr.poke(0.U)
    dut.io.enq1_bits.mem_wdata.poke(0.U)
    dut.io.enq1_bits.addr_ready.poke(false.B)
    dut.io.enq1_bits.cp_idx.poke(0.U)
    dut.io.enq1_bits.actual_taken.poke(false.B)
    dut.io.enq1_bits.actual_target.poke(0.U)
  }

  it should "fill all ROB entries" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      for (i <- 0 until OoOParams.ROB_SIZE) {
        dut.io.enq_fire.poke(true.B)
        dut.io.enq_bits.pc.poke(i.U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.full.expect(true.B)
      dut.io.count.expect(OoOParams.ROB_SIZE.U)
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

  it should "accept two writebacks in one cycle" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.io.enq_bits.arch_rd.poke(1.U)
      dut.io.enq_bits.new_phys.poke(32.U)
      dut.io.enq1_fire.poke(true.B)
      dut.io.enq1_bits.pc.poke("h80000004".U)
      dut.io.enq1_bits.arch_rd.poke(2.U)
      dut.io.enq1_bits.new_phys.poke(33.U)
      dut.clock.step()
      idleRob(dut)
      dut.io.wb_fire.poke(true.B)
      dut.io.wb_idx.poke(0.U)
      dut.io.wb_val.poke(0x11.U)
      dut.io.wb1_fire.poke(true.B)
      dut.io.wb1_idx.poke(1.U)
      dut.io.wb1_val.poke(0x22.U)
      dut.clock.step()
      idleRob(dut)
      dut.io.entries(0).done.expect(true.B)
      dut.io.entries(0).dest_val.expect(0x11.U)
      dut.io.entries(1).done.expect(true.B)
      dut.io.entries(1).dest_val.expect(0x22.U)
    }
  }

  it should "commit two completed entries in one cycle" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.io.enq_bits.arch_rd.poke(1.U)
      dut.io.enq_bits.new_phys.poke(32.U)
      dut.io.enq1_fire.poke(true.B)
      dut.io.enq1_bits.pc.poke("h80000004".U)
      dut.io.enq1_bits.arch_rd.poke(2.U)
      dut.io.enq1_bits.new_phys.poke(33.U)
      dut.clock.step()
      idleRob(dut)

      dut.io.wb_fire.poke(true.B)
      dut.io.wb_idx.poke(0.U)
      dut.io.wb_val.poke(0x11.U)
      dut.io.wb1_fire.poke(true.B)
      dut.io.wb1_idx.poke(1.U)
      dut.io.wb1_val.poke(0x22.U)
      dut.clock.step()
      idleRob(dut)

      dut.io.commit_valid.expect(true.B)
      dut.io.commit1_valid.expect(true.B)
      dut.io.commit_bits.dest_val.expect(0x11.U)
      dut.io.commit1_bits.dest_val.expect(0x22.U)
      dut.io.commit_fire.poke(true.B)
      dut.io.commit1_fire.poke(true.B)
      dut.clock.step()
      idleRob(dut)
      dut.io.count.expect(0.U)
      dut.io.head.expect(2.U)
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
      for (i <- 0 until OoOParams.ROB_SIZE) {
        dut.io.enq_bits.pc.poke((0x2000 + i).U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect(OoOParams.ROB_SIZE.U)
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
      dut.io.entries(OoOParams.ROB_SIZE - 1).valid.expect(false.B)
    }
  }

  // ---------- RS（3b 壳）----------
  behavior of "RS"

  def idleRs(dut: RS): Unit = {
    dut.io.enq_fire.poke(false.B)
    dut.io.enq1_fire.poke(false.B)
    dut.io.issue_fire.poke(false.B)
    dut.io.issue_alu_fire.poke(false.B)
    dut.io.issue_alu1_fire.poke(false.B)
    dut.io.issue_div_fire.poke(false.B)
    dut.io.issue_lsu_fire.poke(false.B)
    dut.io.free_rob_fire.poke(false.B)
    dut.io.free_rob_idx.poke(0.U)
    dut.io.free_rob1_fire.poke(false.B)
    dut.io.free_rob1_idx.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flush_all.poke(false.B)
    dut.io.flush_idx.poke(0.U)
    dut.io.rob_head.poke(0.U)
    dut.io.rob_st_pending.poke(0.U)
    dut.io.cdb_valid.poke(false.B)
    dut.io.cdb_pdest.poke(0.U)
    dut.io.cdb_val.poke(0.U)
    dut.io.cdb1_valid.poke(false.B)
    dut.io.cdb1_pdest.poke(0.U)
    dut.io.cdb1_val.poke(0.U)
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
    dut.io.enq_bits.cp_idx.poke(0.U)
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
    dut.io.enq1_bits.valid.poke(false.B)
    dut.io.enq1_bits.rob_idx.poke(0.U)
    dut.io.enq1_bits.src1_ready.poke(true.B)
    dut.io.enq1_bits.src2_ready.poke(true.B)
    dut.io.enq1_bits.src1_phys.poke(0.U)
    dut.io.enq1_bits.src2_phys.poke(0.U)
    dut.io.enq1_bits.src1_val.poke(0.U)
    dut.io.enq1_bits.src2_val.poke(0.U)
    dut.io.enq1_bits.pdest.poke(0.U)
    dut.io.enq1_bits.old_phys.poke(0.U)
    dut.io.enq1_bits.do_rename.poke(false.B)
    dut.io.enq1_bits.pc.poke(0.U)
    dut.io.enq1_bits.inst.poke(0.U)
    dut.io.enq1_bits.imm_ext.poke(0.U)
    dut.io.enq1_bits.waddr.poke(0.U)
    dut.io.enq1_bits.is_ebreak.poke(false.B)
    dut.io.enq1_bits.is_fencei.poke(false.B)
    dut.io.enq1_bits.csr_rd1.poke(0.U)
    dut.io.enq1_bits.csr_waddr.poke(0.U)
    dut.io.enq1_bits.state.state.poke(false.B)
    dut.io.enq1_bits.state.state_num.poke(0.U)
    dut.io.enq1_bits.bp_valid.poke(false.B)
    dut.io.enq1_bits.bp_taken.poke(false.B)
    dut.io.enq1_bits.bp_target.poke(0.U)
    dut.io.enq1_bits.bp_index.poke(0.U)
    dut.io.enq1_bits.cp_idx.poke(0.U)
    dut.io.enq1_bits.exu_alu_srcA.poke(0.U)
    dut.io.enq1_bits.exu_alu_srcB.poke(0.U)
    dut.io.enq1_bits.exu_alu_control.poke(0.U)
    dut.io.enq1_bits.exu_jump.poke("b1111".U)
    dut.io.enq1_bits.lsu_mem_wmask.poke(0.U)
    dut.io.enq1_bits.lsu_mem_rd.poke(0.U)
    dut.io.enq1_bits.lsu_mem_write.poke(false.B)
    dut.io.enq1_bits.lsu_mem_valid.poke(false.B)
    dut.io.enq1_bits.wbu_reg_write.poke(false.B)
    dut.io.enq1_bits.wbu_reg_write_sel.poke(0.U)
    dut.io.enq1_bits.wbu_csr_write.poke(false.B)
    dut.io.enq1_bits.wbu_csr_sel.poke(0.U)
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

  it should "advance the ALU issue port after a fire without freeing the first entry" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.io.enq_bits.pc.poke("h80000004".U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_alu_valid.expect(true.B)
      dut.io.issue_alu_bits.rob_idx.expect(0.U)
      dut.io.issue_alu_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_alu_fire.poke(false.B)
      dut.io.issue_alu_valid.expect(true.B)
      dut.io.issue_alu_bits.rob_idx.expect(1.U)
      dut.io.count.expect(2.U)
    }
  }

  it should "select two independent integer operations on separate ALU ports" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.pc.poke("h80000000".U)
      dut.clock.step()
      dut.io.enq_bits.rob_idx.poke(1.U)
      dut.io.enq_bits.pc.poke("h80000004".U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_alu_valid.expect(true.B)
      dut.io.issue_alu_bits.rob_idx.expect(0.U)
      dut.io.issue_alu1_valid.expect(true.B)
      dut.io.issue_alu1_bits.rob_idx.expect(1.U)
      dut.io.issue_alu_fire.poke(true.B)
      dut.io.issue_alu1_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_alu_valid.expect(false.B)
      dut.io.issue_alu1_valid.expect(false.B)
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

  it should "wake operands from two CDB lanes" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(0.U)
      dut.io.enq_bits.src1_ready.poke(false.B)
      dut.io.enq_bits.src2_ready.poke(false.B)
      dut.io.enq_bits.src1_phys.poke(5.U)
      dut.io.enq_bits.src2_phys.poke(6.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_valid.expect(false.B)
      dut.io.cdb_valid.poke(true.B)
      dut.io.cdb_pdest.poke(5.U)
      dut.io.cdb_val.poke(0x55.U)
      dut.io.cdb1_valid.poke(true.B)
      dut.io.cdb1_pdest.poke(6.U)
      dut.io.cdb1_val.poke(0x66.U)
      dut.clock.step()
      idleRs(dut)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
      dut.io.issue_bits.src1_val.expect(0x55.U)
      dut.io.issue_bits.src2_val.expect(0x66.U)
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

  it should "free two completed rob entries in one cycle" in {
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
      dut.io.count.expect(2.U)
      dut.io.free_rob_fire.poke(true.B)
      dut.io.free_rob_idx.poke(0.U)
      dut.io.free_rob1_fire.poke(true.B)
      dut.io.free_rob1_idx.poke(1.U)
      dut.clock.step()
      idleRs(dut)
      dut.io.count.expect(0.U)
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

  behavior of "WritebackArbiter"

  def pokeWritebackCandidate(bits: core.LSU_WBU_IO, rob: Int, value: BigInt): Unit = {
    bits.signals.wbu.reg_write.poke(true.B)
    bits.signals.wbu.reg_write_sel.poke(0.U)
    bits.signals.wbu.csr_write.poke(false.B)
    bits.signals.wbu.csr_sel.poke(0.U)
    bits.signals.wbu.irq.poke(false.B)
    bits.signals.wbu.irq_num.poke(0.U)
    bits.rd1.poke(0.U)
    bits.alu_result.poke(value.U)
    bits.pc.poke((BigInt("80000000", 16) + rob * 4).U)
    bits.next_pc.poke(0.U)
    bits.imm_ext.poke(0.U)
    bits.mem_read.poke(0.U)
    bits.waddr.poke(1.U)
    bits.is_ebreak.poke(false.B)
    bits.csr_rd1.poke(0.U)
    bits.csr_waddr.poke(0.U)
    bits.state.state.poke(false.B)
    bits.state.state_num.poke(0.U)
    bits.rob_idx.poke(rob.U)
    bits.pdest.poke(32.U)
    bits.old_phys.poke(1.U)
    bits.do_rename.poke(true.B)
    bits.br_taken.poke(false.B)
    bits.store_data.poke(0.U)
    bits.fwd_valid.poke(false.B)
    bits.fwd_data.poke(0.U)
  }

  it should "grant the two oldest results across ROB wraparound" in {
    simulate(new WritebackArbiter()) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.robHead.poke(30.U)
      dut.io.out(0).ready.poke(true.B)
      dut.io.out(1).ready.poke(true.B)
      val robs = Seq(5, 31, 20, 1)
      for (i <- robs.indices) {
        dut.io.in(i).valid.poke(true.B)
        pokeWritebackCandidate(dut.io.in(i).bits, robs(i), BigInt(i + 1))
      }

      dut.io.out(0).valid.expect(true.B)
      dut.io.out(0).bits.rob_idx.expect(31.U)
      dut.io.out(0).bits.alu_result.expect(2.U)
      dut.io.out(1).valid.expect(true.B)
      dut.io.out(1).bits.rob_idx.expect(1.U)
      dut.io.out(1).bits.alu_result.expect(4.U)
      dut.io.in(0).ready.expect(false.B)
      dut.io.in(1).ready.expect(true.B)
      dut.io.in(2).ready.expect(false.B)
      dut.io.in(3).ready.expect(true.B)
    }
  }

  // ---------- StoreQueue ----------
  behavior of "StoreQueue"

  def idleStoreQueue(dut: StoreQueue): Unit = {
    dut.io.rob_head.poke(0.U)
    dut.io.alloc0_valid.poke(false.B)
    dut.io.alloc0_rob.poke(0.U)
    dut.io.alloc0_mask.poke(0.U)
    dut.io.alloc1_valid.poke(false.B)
    dut.io.alloc1_rob.poke(0.U)
    dut.io.alloc1_mask.poke(0.U)
    dut.io.wb_valid.poke(false.B)
    dut.io.wb_rob.poke(0.U)
    dut.io.wb_addr.poke(0.U)
    dut.io.wb_data.poke(0.U)
    dut.io.wb_mask.poke(0.U)
    dut.io.wb1_valid.poke(false.B)
    dut.io.wb1_rob.poke(0.U)
    dut.io.wb1_addr.poke(0.U)
    dut.io.wb1_data.poke(0.U)
    dut.io.wb1_mask.poke(0.U)
    dut.io.commit_valid.poke(false.B)
    dut.io.commit_rob.poke(0.U)
    dut.io.commit1_valid.poke(false.B)
    dut.io.commit1_rob.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flush_idx.poke(0.U)
    dut.io.flush_all.poke(false.B)
    dut.io.ld_valid.poke(false.B)
    dut.io.ld_rob.poke(0.U)
    dut.io.ld_addr.poke(0.U)
    dut.io.ld_mem_rd.poke(RWORD)
  }

  it should "wait on older unresolved store" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.unresolved_mask.expect(1.U)
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
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h1000".U)
      dut.io.wb_data.poke("h12345678".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()
      dut.io.wb_valid.poke(false.B)
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

  it should "wait on partial overlap with an older store" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(1.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h1001".U)
      dut.io.wb_data.poke("haa".U)
      dut.io.wb_mask.poke(1.U)
      dut.clock.step()
      dut.io.wb_valid.poke(false.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(1.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(true.B)
      dut.io.fwd_valid.expect(false.B)
    }
  }

  it should "clear a committed store entry" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h1000".U)
      dut.io.wb_data.poke("h12345678".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()
      dut.io.wb_valid.poke(false.B)
      dut.io.commit_valid.poke(true.B)
      dut.io.commit_rob.poke(0.U)
      dut.clock.step()
      dut.io.commit_valid.poke(false.B)
      dut.io.unresolved_mask.expect(0.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(1.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(false.B)
    }
  }

  it should "clear two committed store entries in one cycle" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(15.U)
      dut.clock.step()
      idleStoreQueue(dut)
      dut.io.commit_valid.poke(true.B)
      dut.io.commit_rob.poke(0.U)
      dut.io.commit1_valid.poke(true.B)
      dut.io.commit1_rob.poke(1.U)
      dut.clock.step()
      idleStoreQueue(dut)
      dut.io.unresolved_mask.expect(0.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.wait_load.expect(false.B)
    }
  }

  it should "flush younger store entries" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(15.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.alloc1_valid.poke(false.B)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h2000".U)
      dut.io.wb_data.poke("h11111111".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()
      dut.io.wb_valid.poke(false.B)
      dut.io.flush.poke(true.B)
      dut.io.flush_idx.poke(0.U)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      dut.io.unresolved_mask.expect(0.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(false.B)
    }
  }

  it should "forward from the youngest older matching store" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.rob_head.poke(0.U)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(15.U)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(15.U)
      dut.clock.step()
      dut.io.alloc0_valid.poke(false.B)
      dut.io.alloc1_valid.poke(false.B)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h1000".U)
      dut.io.wb_data.poke("h11111111".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()
      dut.io.wb_rob.poke(1.U)
      dut.io.wb_addr.poke("h1000".U)
      dut.io.wb_data.poke("h22222222".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()
      dut.io.wb_valid.poke(false.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.clock.step()
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(true.B)
      dut.io.fwd_data.expect("h22222222".U)
    }
  }

  it should "capture two store writebacks in one cycle" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(WWORD)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(WWORD)
      dut.clock.step()
      idleStoreQueue(dut)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h80000100".U)
      dut.io.wb_data.poke("h11111111".U)
      dut.io.wb_mask.poke(WWORD)
      dut.io.wb1_valid.poke(true.B)
      dut.io.wb1_rob.poke(1.U)
      dut.io.wb1_addr.poke("h80000100".U)
      dut.io.wb1_data.poke("h22222222".U)
      dut.io.wb1_mask.poke(WWORD)
      dut.clock.step()
      idleStoreQueue(dut)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h80000100".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(true.B)
      dut.io.fwd_data.expect("h22222222".U)
    }
  }

  behavior of "LoadQueue"

  def idleLoadQueue(dut: LoadQueue): Unit = {
    dut.io.alloc.valid.poke(false.B)
    dut.io.alloc.bits.meta.signals.wbu.reg_write.poke(false.B)
    dut.io.alloc.bits.meta.signals.wbu.reg_write_sel.poke(0.U)
    dut.io.alloc.bits.meta.signals.wbu.csr_write.poke(false.B)
    dut.io.alloc.bits.meta.signals.wbu.csr_sel.poke(0.U)
    dut.io.alloc.bits.meta.signals.wbu.irq.poke(false.B)
    dut.io.alloc.bits.meta.signals.wbu.irq_num.poke(0.U)
    dut.io.alloc.bits.meta.rd1.poke(0.U)
    dut.io.alloc.bits.meta.alu_result.poke(0.U)
    dut.io.alloc.bits.meta.pc.poke(0.U)
    dut.io.alloc.bits.meta.next_pc.poke(0.U)
    dut.io.alloc.bits.meta.imm_ext.poke(0.U)
    dut.io.alloc.bits.meta.mem_read.poke(0.U)
    dut.io.alloc.bits.meta.waddr.poke(0.U)
    dut.io.alloc.bits.meta.is_ebreak.poke(false.B)
    dut.io.alloc.bits.meta.csr_rd1.poke(0.U)
    dut.io.alloc.bits.meta.csr_waddr.poke(0.U)
    dut.io.alloc.bits.meta.state.state.poke(false.B)
    dut.io.alloc.bits.meta.state.state_num.poke(0.U)
    dut.io.alloc.bits.meta.rob_idx.poke(0.U)
    dut.io.alloc.bits.meta.pdest.poke(0.U)
    dut.io.alloc.bits.meta.old_phys.poke(0.U)
    dut.io.alloc.bits.meta.do_rename.poke(false.B)
    dut.io.alloc.bits.meta.br_taken.poke(false.B)
    dut.io.alloc.bits.meta.store_data.poke(0.U)
    dut.io.alloc.bits.meta.fwd_valid.poke(false.B)
    dut.io.alloc.bits.meta.fwd_data.poke(0.U)
    dut.io.alloc.bits.addr.poke(0.U)
    dut.io.alloc.bits.memRd.poke(RWORD)
    dut.io.wb.ready.poke(false.B)
    dut.io.robHead.poke(0.U)
    dut.io.commit0Valid.poke(false.B)
    dut.io.commit0Rob.poke(0.U)
    dut.io.commit1Valid.poke(false.B)
    dut.io.commit1Rob.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flushIdx.poke(0.U)
    dut.io.flushAll.poke(false.B)
    dut.io.fwdWait.poke(false.B)
    dut.io.fwdValid.poke(false.B)
    dut.io.fwdData.poke(0.U)
    dut.io.fwdUnknownOnly.poke(false.B)
    dut.io.fwdUnknownMask.poke(0.U)
    dut.io.mmioReady.poke(true.B)
    dut.io.storeResolve0Valid.poke(false.B)
    dut.io.storeResolve0Rob.poke(0.U)
    dut.io.storeResolve0Addr.poke(0.U)
    dut.io.storeResolve0Mask.poke(0.U)
    dut.io.storeResolve1Valid.poke(false.B)
    dut.io.storeResolve1Rob.poke(0.U)
    dut.io.storeResolve1Addr.poke(0.U)
    dut.io.storeResolve1Mask.poke(0.U)
    dut.io.dmem.arready.poke(false.B)
    dut.io.dmem.rdata.poke(0.U)
    dut.io.dmem.rresp.poke(0.U)
    dut.io.dmem.rvalid.poke(false.B)
    dut.io.dmem.rlast.poke(false.B)
    dut.io.dmem.rid.poke(0.U)
    dut.io.dmem.awready.poke(false.B)
    dut.io.dmem.wready.poke(false.B)
    dut.io.dmem.bvalid.poke(false.B)
    dut.io.dmem.bresp.poke(0.U)
    dut.io.dmem.bid.poke(0.U)
  }

  def allocLoad(dut: LoadQueue, rob: Int, pc: BigInt, addr: BigInt): Unit = {
    dut.io.alloc.valid.poke(true.B)
    dut.io.alloc.bits.meta.pc.poke(pc.U)
    dut.io.alloc.bits.meta.next_pc.poke((pc + 4).U)
    dut.io.alloc.bits.meta.alu_result.poke(addr.U)
    dut.io.alloc.bits.meta.rob_idx.poke(rob.U)
    dut.io.alloc.bits.meta.pdest.poke((rob + 32).U)
    dut.io.alloc.bits.meta.waddr.poke((rob + 1).U)
    dut.io.alloc.bits.addr.poke(addr.U)
    dut.io.alloc.bits.memRd.poke(RWORD)
  }

  it should "tag a load response and write it back once" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 1, BigInt("80000020", 16), BigInt("80001000", 16))
      dut.io.alloc.ready.expect(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.queryValid.expect(true.B)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(0.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("haabbccdd".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.dmem.rready.expect(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(1.U)
      dut.io.wb.bits.mem_read.expect("haabbccdd".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(false.B)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "drain a stale response after selective flush" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000040", 16), BigInt("80002000", 16))
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.dmem.arready.poke(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.flush.poke(true.B)
      dut.io.flushIdx.poke(1.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.staleResp.expect(true.B)
      dut.io.dmem.rready.expect(true.B)
      dut.io.wb.valid.expect(false.B)
    }
  }

  it should "detect a conflict after bypassing an unknown older store" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000080", 16), BigInt("80003000", 16))
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.fwdWait.poke(true.B)
      dut.io.fwdUnknownOnly.poke(true.B)
      dut.io.fwdUnknownMask.poke(2.U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(1.U)
      dut.io.storeResolve0Addr.poke("h80003000".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.violationValid.expect(true.B)
      dut.io.violationRob.expect(2.U)
      dut.io.violationPc.expect("h80000080".U)
    }
  }

  it should "match reversed responses and write back loads in ROB age order" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)

      allocLoad(dut, 1, BigInt("800000a0", 16), BigInt("80004000", 16))
      dut.clock.step()
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("800000a4", 16), BigInt("80005000", 16))
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(0.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(2.U)

      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(1.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(1.U)
      dut.io.dmem.rdata.poke("h22222222".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("h11111111".U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(1.U)
      dut.io.wb.bits.mem_read.expect("h11111111".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(2.U)
      dut.io.wb.bits.mem_read.expect("h22222222".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
  }

  behavior of "DCache"

  def idleDCache(dut: DCache): Unit = {
    dut.io.cpu.araddr.poke(0.U)
    dut.io.cpu.arvalid.poke(false.B)
    dut.io.cpu.arid.poke(0.U)
    dut.io.cpu.arlen.poke(0.U)
    dut.io.cpu.arsize.poke(2.U)
    dut.io.cpu.arburst.poke(1.U)
    dut.io.cpu.rready.poke(false.B)
    dut.io.cpu.awaddr.poke(0.U)
    dut.io.cpu.awvalid.poke(false.B)
    dut.io.cpu.awid.poke(0.U)
    dut.io.cpu.awlen.poke(0.U)
    dut.io.cpu.awsize.poke(0.U)
    dut.io.cpu.awburst.poke(0.U)
    dut.io.cpu.wdata.poke(0.U)
    dut.io.cpu.wstrb.poke(0.U)
    dut.io.cpu.wvalid.poke(false.B)
    dut.io.cpu.wlast.poke(false.B)
    dut.io.cpu.bready.poke(false.B)
    dut.io.mem.arready.poke(false.B)
    dut.io.mem.rdata.poke(0.U)
    dut.io.mem.rresp.poke(0.U)
    dut.io.mem.rvalid.poke(false.B)
    dut.io.mem.rlast.poke(false.B)
    dut.io.mem.rid.poke(0.U)
    dut.io.mem.awready.poke(false.B)
    dut.io.mem.wready.poke(false.B)
    dut.io.mem.bvalid.poke(false.B)
    dut.io.mem.bresp.poke(0.U)
    dut.io.mem.bid.poke(0.U)
    dut.io.invalidate_valid.poke(false.B)
    dut.io.invalidate_addr.poke(0.U)
    dut.io.invalidate2_valid.poke(false.B)
    dut.io.invalidate2_addr.poke(0.U)
    dut.io.invalidate3_valid.poke(false.B)
    dut.io.invalidate3_addr.poke(0.U)
  }

  def fillDCacheLine(dut: DCache, base: BigInt, values: Seq[BigInt]): Unit = {
    for (i <- values.indices) {
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base + i * 4).U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(i).U)
      dut.io.mem.rresp.poke(0.U)
      dut.io.mem.rlast.poke(true.B)
      dut.io.mem.rid.poke(0.U)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)
    }
  }

  it should "fill a line on miss and hit within the same line" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80000000", 16)
      val values = (0 until 8).map(i => BigInt("10000000", 16) + i)

      dut.io.cpu.araddr.poke((base + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arsize.poke(2.U)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)

      fillDCacheLine(dut, base, values)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(1).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.mem.arready.poke(false.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(2).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
    }
  }

  it should "serve a cached hit while one miss is outstanding" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val hitBase = BigInt("80002000", 16)
      val missBase = BigInt("80003000", 16)
      val hitValues = (0 until 8).map(i => BigInt("40000000", 16) + i)
      val missValues = (0 until 8).map(i => BigInt("50000000", 16) + i)

      dut.io.cpu.arid.poke(0.U)
      dut.io.cpu.araddr.poke((hitBase + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arsize.poke(2.U)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, hitBase, hitValues)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(0.U)
      dut.io.cpu.rdata.expect(hitValues(1).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.arid.poke(0.U)
      dut.io.cpu.araddr.poke((missBase + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arsize.poke(2.U)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(missBase.U)

      dut.io.cpu.arid.poke(1.U)
      dut.io.cpu.araddr.poke((hitBase + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(1.U)
      dut.io.cpu.rdata.expect(hitValues(2).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      fillDCacheLine(dut, missBase, missValues)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(0.U)
      dut.io.cpu.rdata.expect(missValues(1).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
    }
  }

  it should "invalidate a cached line on store notification" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80000000", 16)
      val values = (0 until 8).map(i => BigInt("20000000", 16) + i)

      dut.io.cpu.araddr.poke(base.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base, values)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.invalidate_valid.poke(true.B)
      dut.io.invalidate_addr.poke((base + 12).U)
      dut.clock.step()
      dut.io.invalidate_valid.poke(false.B)

      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
    }
  }

  it should "invalidate a cached line on store drain notification" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80000040", 16)
      val values = (0 until 8).map(i => BigInt("24000000", 16) + i)

      dut.io.cpu.araddr.poke((base + 16).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base, values)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.invalidate2_valid.poke(true.B)
      dut.io.invalidate2_addr.poke((base + 20).U)
      dut.clock.step()
      dut.io.invalidate2_valid.poke(false.B)

      dut.io.cpu.araddr.poke((base + 16).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
    }
  }

  it should "not install a line invalidated during miss fill" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80001000", 16)
      val values = (0 until 8).map(i => BigInt("30000000", 16) + i)

      dut.io.cpu.araddr.poke((base + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)

      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(0).U)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)

      dut.io.invalidate_valid.poke(true.B)
      dut.io.invalidate_addr.poke((base + 16).U)
      dut.clock.step()
      dut.io.invalidate_valid.poke(false.B)

      for (i <- 1 until values.length) {
        dut.io.mem.arvalid.expect(true.B)
        dut.io.mem.araddr.expect((base + i * 4).U)
        dut.io.mem.arready.poke(true.B)
        dut.clock.step()
        dut.io.mem.arready.poke(false.B)
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.clock.step()
        dut.io.mem.rvalid.poke(false.B)
      }
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(1).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
    }
  }

  behavior of "ICache"

  def idleICache(dut: ICache): Unit = {
    dut.io.in.araddr.poke(0.U)
    dut.io.in.arvalid.poke(false.B)
    dut.io.in.rready.poke(true.B)
    dut.io.fencei.valid.poke(false.B)
    dut.io.fencei.bits.is_fencei.poke(false.B)
    dut.io.out.arready.poke(false.B)
    dut.io.out.rdata.poke(0.U)
    dut.io.out.rresp.poke(0.U)
    dut.io.out.rvalid.poke(false.B)
    dut.io.out.rlast.poke(false.B)
    dut.io.out.rid.poke(0.U)
    dut.io.out.awready.poke(false.B)
    dut.io.out.wready.poke(false.B)
    dut.io.out.bvalid.poke(false.B)
    dut.io.out.bresp.poke(0.U)
    dut.io.out.bid.poke(0.U)
  }

  def fillICacheLine(dut: ICache, base: BigInt, words: Seq[BigInt]): Unit = {
    require(words.length == 2)
    dut.io.in.araddr.poke(base.U)
    dut.io.in.arvalid.poke(true.B)
    dut.io.in.arready.expect(true.B)
    dut.clock.step()
    dut.io.in.arvalid.poke(false.B)

    for (i <- words.indices) {
      dut.io.out.arvalid.expect(true.B)
      dut.io.out.araddr.expect((base + i * 4).U)
      dut.io.out.arready.poke(true.B)
      dut.clock.step()
      dut.io.out.arready.poke(false.B)
      dut.io.out.rvalid.poke(true.B)
      dut.io.out.rdata.poke(words(i).U)
      dut.io.out.rresp.poke(0.U)
      dut.io.out.rlast.poke(true.B)
      dut.io.out.rid.poke(0.U)
      dut.clock.step()
      dut.io.out.rvalid.poke(false.B)
    }
  }

  it should "return a cached next-line word and hold both slots under backpressure" in {
    simulate(new ICache(set = 4, way = 1, block_size = 8,
      conf = new core.CoreConfig(32))) { dut =>
      idleICache(dut)
      resetDut(dut.clock, dut.reset)
      idleICache(dut)
      val base = BigInt("80000000", 16)
      val line0 = Seq(BigInt("11111111", 16), BigInt("22222222", 16))
      val line1 = Seq(BigInt("33333333", 16), BigInt("44444444", 16))
      fillICacheLine(dut, base, line0)
      fillICacheLine(dut, base + 8, line1)

      dut.io.in.rready.poke(false.B)
      dut.io.in.araddr.poke((base + 4).U)
      dut.io.in.arvalid.poke(true.B)
      dut.io.in.arready.expect(true.B)
      dut.io.in.rvalid.expect(true.B)
      dut.io.in.rvalid1.expect(true.B)
      dut.io.in.rdata.expect(line0(1).U)
      dut.io.in.rdata1.expect(line1(0).U)
      dut.clock.step()
      dut.io.in.arvalid.poke(false.B)

      for (_ <- 0 until 2) {
        dut.io.in.rvalid.expect(true.B)
        dut.io.in.rvalid1.expect(true.B)
        dut.io.in.rdata.expect(line0(1).U)
        dut.io.in.rdata1.expect(line1(0).U)
        dut.clock.step()
      }
      dut.io.in.rready.poke(true.B)
      dut.clock.step()
    }
  }

  behavior of "StoreBuffer"

  def idleStoreBuffer(dut: StoreBuffer): Unit = {
    dut.io.enq.valid.poke(false.B)
    dut.io.enq.bits.addr.poke(0.U)
    dut.io.enq.bits.data.poke(0.U)
    dut.io.enq.bits.mask.poke(0.U)
    dut.io.enq1.valid.poke(false.B)
    dut.io.enq1.bits.addr.poke(0.U)
    dut.io.enq1.bits.data.poke(0.U)
    dut.io.enq1.bits.mask.poke(0.U)
    dut.io.ld_valid.poke(false.B)
    dut.io.ld_addr.poke(0.U)
    dut.io.ld_mem_rd.poke(RWORD)
    dut.io.bus_busy.poke(false.B)
    dut.io.dmem.arready.poke(false.B)
    dut.io.dmem.rdata.poke(0.U)
    dut.io.dmem.rresp.poke(0.U)
    dut.io.dmem.rvalid.poke(false.B)
    dut.io.dmem.rlast.poke(false.B)
    dut.io.dmem.rid.poke(0.U)
    dut.io.dmem.awready.poke(false.B)
    dut.io.dmem.wready.poke(false.B)
    dut.io.dmem.bvalid.poke(false.B)
    dut.io.dmem.bresp.poke(0.U)
    dut.io.dmem.bid.poke(0.U)
  }

  it should "enqueue then drain one store through AXI" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h12345678".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq.ready.expect(true.B)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(1.U)
      dut.clock.step()

      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.wdata.expect("h12345678".U)
      dut.io.dmem.wstrb.expect("b1111".U)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.clock.step()

      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.dmem.bready.expect(true.B)
      dut.clock.step()

      dut.io.dmem.bvalid.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.count.expect(0.U)
    }
  }

  it should "start the next write immediately after a response" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.addr.poke("h80000004".U)
      dut.io.enq1.bits.data.poke("h22222222".U)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.clock.step()
      idleStoreBuffer(dut)
      dut.clock.step()
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.clock.step()
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.clock.step()
      dut.io.dmem.bvalid.poke(false.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.awaddr.expect("h80000004".U)
      dut.io.count.expect(1.U)
    }
  }

  it should "forward the youngest matching buffered store" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)

      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h22222222".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_wait.expect(false.B)
      dut.io.ld_fwd_valid.expect(true.B)
      dut.io.ld_fwd_data.expect("h22222222".U)
    }
  }

  it should "wait on partial overlap with a buffered store" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000001".U)
      dut.io.enq.bits.data.poke("h000000aa".U)
      dut.io.enq.bits.mask.poke(WBYTE)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_wait.expect(true.B)
      dut.io.ld_fwd_valid.expect(false.B)
    }
  }

  it should "atomically enqueue two stores and preserve their order" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.addr.poke("h80000000".U)
      dut.io.enq1.bits.data.poke("h22222222".U)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.io.enq.ready.expect(true.B)
      dut.io.enq1.ready.expect(true.B)
      dut.clock.step()
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.count.expect(2.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_fwd_valid.expect(true.B)
      dut.io.ld_fwd_data.expect("h22222222".U)
    }
  }

  it should "reject a two-store batch when only one slot is free" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.mask.poke(WWORD)
      for (i <- 0 until OoOParams.STORE_BUFFER_SIZE - 1) {
        dut.io.enq.bits.addr.poke((0x80000000L + i * 4).U)
        dut.io.enq.bits.data.poke(i.U)
        dut.clock.step()
      }
      dut.io.count.expect((OoOParams.STORE_BUFFER_SIZE - 1).U)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.addr.poke("h80000100".U)
      dut.io.enq1.bits.data.poke("h55".U)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.io.enq.ready.expect(false.B)
      dut.io.enq1.ready.expect(false.B)
      dut.clock.step()
      dut.io.count.expect((OoOParams.STORE_BUFFER_SIZE - 1).U)
    }
  }

  behavior of "FetchBuffer"

  def idleFetchBuffer(dut: FetchBuffer): Unit = {
    dut.io.in.valid.poke(false.B)
    for (i <- 0 until OoOParams.FETCH_WIDTH) {
      dut.io.in.bits.valid(i).poke(false.B)
      dut.io.in.bits.bits(i).inst.poke(0.U)
      dut.io.in.bits.bits(i).pc.poke(0.U)
      dut.io.in.bits.bits(i).state.state.poke(false.B)
      dut.io.in.bits.bits(i).state.state_num.poke(0.U)
      dut.io.in.bits.bits(i).bp_valid.poke(false.B)
      dut.io.in.bits.bits(i).bp_taken.poke(false.B)
      dut.io.in.bits.bits(i).bp_target.poke(0.U)
      dut.io.in.bits.bits(i).bp_index.poke(0.U)
      dut.io.in.bits.bits(i).ftq_idx.poke(0.U)
      dut.io.in.bits.bits(i).ftq_generation.poke(0.U)
    }
    dut.io.out.ready.poke(false.B)
    dut.io.flush.poke(false.B)
  }

  it should "hold a fetch packet stable under backpressure" in {
    simulate(new FetchBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFetchBuffer(dut)
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.valid(0).poke(true.B)
      dut.io.in.bits.bits(0).pc.poke("h80000000".U)
      dut.io.in.bits.bits(0).inst.poke("h00100013".U)
      dut.clock.step()
      idleFetchBuffer(dut)
      dut.io.out.valid.expect(true.B)
      dut.io.out.bits.bits(0).pc.expect("h80000000".U)
      dut.clock.step(2)
      dut.io.out.bits.bits(0).pc.expect("h80000000".U)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      dut.io.empty.expect(true.B)
    }
  }

  it should "accept and release packets in the same cycle" in {
    simulate(new FetchBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFetchBuffer(dut)
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.valid(0).poke(true.B)
      dut.io.in.bits.bits(0).pc.poke("h80000000".U)
      dut.clock.step()
      dut.io.in.bits.bits(0).pc.poke("h80000008".U)
      dut.io.out.ready.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.count.expect(1.U)
      dut.io.out.bits.bits(0).pc.expect("h80000008".U)
      dut.io.flush.poke(true.B)
      dut.clock.step()
      dut.io.empty.expect(true.B)
    }
  }

  behavior of "FTQ"

  def idleFtq(dut: FTQ): Unit = {
    dut.io.alloc.valid.poke(false.B)
    dut.io.alloc.bits.basePc.poke(0.U)
    dut.io.alloc.bits.validMask.poke(0.U)
    dut.io.alloc.bits.predictedNextPc.poke(0.U)
    dut.io.alloc.bits.cfiSlot.poke(2.U)
    dut.io.alloc.bits.ghr.poke(0.U)
    for (i <- 0 until common.BPU_Config.RAS_SIZE) {
      dut.io.alloc.bits.ras(i).poke(0.U)
    }
    dut.io.alloc.bits.rasPtr.poke(0.U)
    dut.io.alloc.bits.rasCount.poke(0.U)
    dut.io.commit0Valid.poke(false.B)
    dut.io.commit0Idx.poke(0.U)
    dut.io.commit0Generation.poke(0.U)
    dut.io.commit1Valid.poke(false.B)
    dut.io.commit1Idx.poke(0.U)
    dut.io.commit1Generation.poke(0.U)
    dut.io.recoverIdx.poke(0.U)
    dut.io.recoverGeneration.poke(0.U)
    dut.io.recoverFlush.poke(false.B)
    dut.io.recoverSlot1.poke(false.B)
    dut.io.flush.poke(false.B)
  }

  it should "free one block after two same-generation commits" in {
    simulate(new FTQ()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFtq(dut)
      val idx = dut.io.allocIdx.peek().litValue
      val generation = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.basePc.poke("h80000000".U)
      dut.io.alloc.bits.validMask.poke(3.U)
      dut.clock.step()
      idleFtq(dut)
      dut.io.recoverIdx.poke(idx.U)
      dut.io.recoverGeneration.poke(generation.U)
      dut.io.recoverValid.expect(true.B)
      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Idx.poke(idx.U)
      dut.io.commit0Generation.poke(generation.U)
      dut.io.commit1Valid.poke(true.B)
      dut.io.commit1Idx.poke(idx.U)
      dut.io.commit1Generation.poke(generation.U)
      dut.clock.step()
      dut.io.count.expect(0.U)
      dut.io.recoverValid.expect(false.B)
    }
  }

  it should "free two adjacent blocks and reject a stale generation" in {
    simulate(new FTQ()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFtq(dut)
      val idx0 = dut.io.allocIdx.peek().litValue
      val gen0 = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.validMask.poke(1.U)
      dut.clock.step()
      idleFtq(dut)
      val idx1 = dut.io.allocIdx.peek().litValue
      val gen1 = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.validMask.poke(1.U)
      dut.clock.step()
      idleFtq(dut)
      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Idx.poke(idx0.U)
      dut.io.commit0Generation.poke(gen0.U)
      dut.io.commit1Valid.poke(true.B)
      dut.io.commit1Idx.poke(idx1.U)
      dut.io.commit1Generation.poke(gen1.U)
      dut.clock.step()
      dut.io.count.expect(0.U)
      idleFtq(dut)
      dut.io.flush.poke(true.B)
      dut.clock.step()
      idleFtq(dut)
      dut.io.recoverIdx.poke(idx0.U)
      dut.io.recoverGeneration.poke(gen0.U)
      dut.io.recoverValid.expect(false.B)
      dut.io.allocGeneration.expect((gen0 + 2).U)
    }
  }

  it should "truncate younger blocks while preserving older recovery state" in {
    simulate(new FTQ()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFtq(dut)

      val idx0 = dut.io.allocIdx.peek().litValue
      val gen0 = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.validMask.poke(1.U)
      dut.clock.step()

      idleFtq(dut)
      val idx1 = dut.io.allocIdx.peek().litValue
      val gen1 = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.validMask.poke(3.U)
      dut.clock.step()

      idleFtq(dut)
      val idx2 = dut.io.allocIdx.peek().litValue
      val gen2 = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.validMask.poke(1.U)
      dut.clock.step()

      idleFtq(dut)
      dut.io.recoverIdx.poke(idx1.U)
      dut.io.recoverGeneration.poke(gen1.U)
      dut.io.recoverFlush.poke(true.B)
      dut.io.recoverSlot1.poke(false.B)
      dut.io.recoverValid.expect(true.B)
      dut.clock.step()

      idleFtq(dut)
      dut.io.count.expect(2.U)
      dut.io.recoverIdx.poke(idx0.U)
      dut.io.recoverGeneration.poke(gen0.U)
      dut.io.recoverValid.expect(true.B)
      dut.io.recoverIdx.poke(idx2.U)
      dut.io.recoverGeneration.poke(gen2.U)
      dut.io.recoverValid.expect(false.B)
      dut.io.allocIdx.expect(idx2.U)
      dut.io.allocGeneration.expect((gen2 + 1).U)

      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Idx.poke(idx0.U)
      dut.io.commit0Generation.poke(gen0.U)
      dut.clock.step()
      idleFtq(dut)
      dut.io.count.expect(1.U)

      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Idx.poke(idx1.U)
      dut.io.commit0Generation.poke(gen1.U)
      dut.clock.step()
      idleFtq(dut)
      dut.io.count.expect(0.U)
    }
  }

  behavior of "FetchQueue"

  def idleFq(dut: FetchQueue): Unit = {
    dut.io.enq.valid.poke(false.B)
    for (i <- 0 until OoOParams.FETCH_WIDTH) {
      dut.io.enq.bits.valid(i).poke(false.B)
      dut.io.enq.bits.bits(i).inst.poke(0.U)
      dut.io.enq.bits.bits(i).pc.poke(0.U)
      dut.io.enq.bits.bits(i).state.state.poke(false.B)
      dut.io.enq.bits.bits(i).state.state_num.poke(0.U)
      dut.io.enq.bits.bits(i).bp_valid.poke(false.B)
      dut.io.enq.bits.bits(i).bp_taken.poke(false.B)
      dut.io.enq.bits.bits(i).bp_target.poke(0.U)
      dut.io.enq.bits.bits(i).bp_index.poke(0.U)
      dut.io.enq.bits.bits(i).ftq_idx.poke(0.U)
      dut.io.enq.bits.bits(i).ftq_generation.poke(0.U)
    }
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
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(false.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000000".U)
      dut.io.enq.bits.bits(0).inst.poke("h00100013".U)
      dut.clock.step()
      dut.io.enq.bits.bits(0).pc.poke("h80000004".U)
      dut.io.enq.bits.bits(0).inst.poke("h00200013".U)
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
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(false.B)
      for (i <- 0 until OoOParams.FQ_SIZE) {
        dut.io.enq.bits.bits(0).pc.poke((0x80000000L + i * 4).U)
        dut.clock.step()
      }
      dut.io.full.expect(true.B)
      dut.io.enq.ready.expect(false.B)
      dut.io.count.expect(OoOParams.FQ_SIZE.U)
    }
  }

  it should "dual deq advance head by two" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(true.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000000".U)
      dut.io.enq.bits.bits(0).inst.poke("h00100013".U)
      dut.io.enq.bits.bits(1).pc.poke("h80000004".U)
      dut.io.enq.bits.bits(1).inst.poke("h00100014".U)
      dut.clock.step()
      dut.io.enq.bits.valid(1).poke(false.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000008".U)
      dut.io.enq.bits.bits(0).inst.poke("h00100015".U)
      dut.clock.step()
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
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(true.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000000".U)
      dut.io.enq.bits.bits(1).pc.poke("h80000004".U)
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

  it should "block a two-slot packet when only one entry is free" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(false.B)
      for (i <- 0 until (OoOParams.FQ_SIZE - 1)) {
        dut.io.enq.bits.bits(0).pc.poke((0x80000000L + i * 4).U)
        dut.clock.step()
      }
      dut.io.count.expect((OoOParams.FQ_SIZE - 1).U)
      dut.io.enq.bits.valid(1).poke(true.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000100".U)
      dut.io.enq.bits.bits(1).pc.poke("h80000104".U)
      dut.io.enq.ready.expect(false.B)
    }
  }

  behavior of "BPUUpdateQueue"

  it should "serialize two same-cycle commit updates in lane order" in {
    simulate(new BPUUpdateQueue(depth = 4)) { dut =>
      resetDut(dut.clock, dut.reset)
      for (lane <- 0 until 2) {
        dut.io.enq(lane).valid.poke(false.B)
        dut.io.enq(lane).bits.pc.poke(0.U)
        dut.io.enq(lane).bits.target.poke(0.U)
        dut.io.enq(lane).bits.taken.poke(false.B)
        dut.io.enq(lane).bits.isBranch.poke(false.B)
        dut.io.enq(lane).bits.isJalr.poke(false.B)
        dut.io.enq(lane).bits.index.poke(0.U)
        dut.io.enq(lane).bits.isCall.poke(false.B)
        dut.io.enq(lane).bits.isRet.poke(false.B)
      }
      dut.io.free.expect(4.U)
      dut.io.enq(0).valid.poke(true.B)
      dut.io.enq(0).bits.pc.poke("h80000000".U)
      dut.io.enq(0).bits.taken.poke(true.B)
      dut.io.enq(0).bits.isBranch.poke(true.B)
      dut.io.enq(1).valid.poke(true.B)
      dut.io.enq(1).bits.pc.poke("h80000004".U)
      dut.io.enq(1).bits.target.poke("h80001000".U)
      dut.io.enq(1).bits.isJalr.poke(true.B)
      dut.clock.step()
      dut.io.enq(0).valid.poke(false.B)
      dut.io.enq(1).valid.poke(false.B)
      dut.io.deq.valid.expect(true.B)
      dut.io.deq.bits.pc.expect("h80000000".U)
      dut.clock.step()
      dut.io.deq.valid.expect(true.B)
      dut.io.deq.bits.pc.expect("h80000004".U)
      dut.clock.step()
      dut.io.deq.valid.expect(false.B)
      dut.io.free.expect(4.U)
    }
  }

  behavior of "BPU"

  def idleBpu(dut: BPU): Unit = {
    dut.io.predict_pc.poke("h80000000".U)
    dut.io.predict_inst.poke("h00000013".U)
    dut.io.predict_pc1.poke("h80000004".U)
    dut.io.predict_inst1.poke("h00000013".U)
    dut.io.update_pc.poke(0.U)
    dut.io.update_target.poke(0.U)
    dut.io.update_valid.poke(false.B)
    dut.io.update_taken.poke(false.B)
    dut.io.update_is_branch.poke(false.B)
    dut.io.update_is_jalr.poke(false.B)
    dut.io.update_index.poke(0.U)
    dut.io.update_is_call.poke(false.B)
    dut.io.update_is_ret.poke(false.B)
    dut.io.spec_advance_valid.poke(false.B)
    dut.io.spec_advance_mask.poke(0.U)
    dut.io.recover_valid.poke(false.B)
    dut.io.recover_pc.poke(0.U)
    dut.io.recover_index.poke(0.U)
    dut.io.recover_taken.poke(false.B)
    dut.io.recover_is_branch.poke(false.B)
    dut.io.recover_is_call.poke(false.B)
    dut.io.recover_is_ret.poke(false.B)
    for (i <- 0 until common.BPU_Config.RAS_SIZE) {
      dut.io.recover_ras(i).poke(0.U)
    }
    dut.io.recover_ras_ptr.poke(0.U)
    dut.io.recover_ras_count.poke(0.U)
    dut.io.reset_spec.poke(false.B)
  }

  it should "advance speculative history and recover the exact branch history" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val backwardBeq = "hfe000ee3".U
      dut.io.predict_inst.poke(backwardBeq)
      val branchIndex = dut.io.bp_index.peek().litValue
      dut.io.bp_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(1.U)
      dut.clock.step()
      idleBpu(dut)
      dut.io.spec_ghr.expect(1.U)
      dut.io.recover_valid.poke(true.B)
      dut.io.recover_pc.poke("h80000000".U)
      dut.io.recover_index.poke(branchIndex.U)
      dut.io.recover_is_branch.poke(true.B)
      dut.io.recover_taken.poke(false.B)
      dut.clock.step()
      idleBpu(dut)
      dut.io.spec_ghr.expect(0.U)
    }
  }

  it should "cover dual slot, indirect target, and tagged override prediction" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16, tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      dut.io.predict_inst.poke("h00000013".U)
      dut.io.predict_inst1.poke("h0000006f".U) // jal x0, 0
      dut.io.bp_valid.expect(false.B)
      dut.io.bp1_valid.expect(true.B)
      dut.io.bp1_taken.expect(true.B)
      dut.io.bp1_target.expect("h80000004".U)
      
      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val jalrX5 = "h00028067".U // jalr x0, 0(x5)
      dut.io.predict_inst.poke(jalrX5)
      dut.io.bp_valid.expect(false.B)

      dut.io.update_valid.poke(true.B)
      dut.io.update_is_jalr.poke(true.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_target.poke("h80001000".U)
      dut.clock.step()
      idleBpu(dut)
      dut.io.update_valid.poke(true.B)
      dut.io.update_is_jalr.poke(true.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_target.poke("h80001000".U)
      dut.clock.step()
      idleBpu(dut)
      dut.io.update_valid.poke(true.B)
      dut.io.update_is_jalr.poke(true.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_target.poke("h80001000".U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_inst.poke(jalrX5)
      dut.io.bp_valid.expect(true.B)
      dut.io.bp_taken.expect(true.B)
      dut.io.bp_target.expect("h80001000".U)
      dut.io.bp_indirect_hit.expect(true.B)

      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val backwardBeq = "hfe000ee3".U // beq x0, x0, -4
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_valid.expect(true.B)
      dut.io.bp_taken.expect(true.B)
      val idx = dut.io.bp_index.peek().litValue

      dut.io.update_valid.poke(true.B)
      dut.io.update_is_branch.poke(true.B)
      dut.io.update_taken.poke(false.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_index.poke(idx.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_tagged_hit.expect(true.B)
      dut.io.bp_taken.expect(false.B)
    }
  }

  it should "allocate TAGE providers and promote a stable ITAGE target" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16, tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val backwardBeq = "hfe000ee3".U // beq x0, x0, -4
      dut.io.predict_inst.poke(backwardBeq)
      val branchIndex = dut.io.bp_index.peek().litValue
      dut.io.update_valid.poke(true.B)
      dut.io.update_is_branch.poke(true.B)
      dut.io.update_taken.poke(false.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_index.poke(branchIndex.U)
      dut.io.tage_alloc.expect(true.B)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_tagged_hit.expect(true.B)
      dut.io.bp_tage_use_alt.expect(true.B)
      dut.io.bp_taken.expect(false.B)

      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val jalrX5 = "h00028067".U // jalr x0, 0(x5)
      dut.io.predict_inst.poke(jalrX5)
      val jalrIndex = dut.io.bp_index.peek().litValue
      for (iteration <- 0 until 4) {
        dut.io.update_valid.poke(true.B)
        dut.io.update_is_jalr.poke(true.B)
        dut.io.update_pc.poke("h80000000".U)
        dut.io.update_target.poke("h80001000".U)
        dut.io.update_index.poke(jalrIndex.U)
        if (iteration == 0) {
          dut.io.itage_alloc.expect(true.B)
        }
        dut.clock.step()
        idleBpu(dut)
        dut.io.predict_inst.poke(jalrX5)
      }
      dut.io.bp_valid.expect(true.B)
      dut.io.bp_target.expect("h80001000".U)
      dut.io.bp_itage_hit.expect(true.B)
    }
  }
}
