package unit

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import core.NPC_Config
import common.MEM_READ._
import common.MEM_WMASK._
import common.ALU_OP._
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

  behavior of "DIV"

  it should "produce RV32M quotient and remainder results without an iterative stall" in {
    simulate(new DIV()) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.kill.poke(false.B)
      dut.io.req_valid.poke(true.B)
      dut.io.req_ready.expect(true.B)
      dut.io.busy.expect(false.B)

      dut.io.a.poke((-7).S(32.W).asUInt)
      dut.io.b.poke(3.U)
      dut.io.op.poke(ALU_DIV)
      dut.io.result_valid.expect(true.B)
      dut.io.result.expect("hffff_fffe".U)
      dut.io.op.poke(ALU_REM)
      dut.io.result.expect("hffff_ffff".U)

      dut.io.a.poke("hffff_ffff".U)
      dut.io.b.poke(16.U)
      dut.io.op.poke(ALU_DIVU)
      dut.io.result.expect("h0fff_ffff".U)
      dut.io.op.poke(ALU_REMU)
      dut.io.result.expect(15.U)

      dut.io.b.poke(0.U)
      dut.io.op.poke(ALU_DIV)
      dut.io.result.expect("hffff_ffff".U)
      dut.io.op.poke(ALU_REM)
      dut.io.result.expect("hffff_ffff".U)
      dut.io.kill.poke(true.B)
      dut.io.result_valid.expect(false.B)
    }
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
      dut.io.wen3.poke(false.B)
      dut.io.waddr3.poke(0.U)
      dut.io.wdata3.poke(0.U)
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
      dut.io.wen3.poke(false.B)
      dut.io.waddr3.poke(0.U)
      dut.io.wdata3.poke(0.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      dut.clock.step()
      dut.io.raddr1.poke(5.U)
      dut.io.rdata1.expect(0x1234.U)
    }
  }

  it should "write three physical results in one cycle" in {
    simulate(new PRF(conf)) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.wen1.poke(true.B)
      dut.io.waddr1.poke(3.U)
      dut.io.wdata1.poke(0x1111.U)
      dut.io.wen2.poke(true.B)
      dut.io.waddr2.poke(7.U)
      dut.io.wdata2.poke(0x2222.U)
      dut.io.wen3.poke(true.B)
      dut.io.waddr3.poke(9.U)
      dut.io.wdata3.poke(0x3333.U)
      for (i <- 0 until 32) dut.io.arch_raddr(i).poke(i.U)
      dut.clock.step()
      dut.io.raddr1.poke(3.U)
      dut.io.rdata1.expect(0x1111.U)
      dut.io.raddr2.poke(7.U)
      dut.io.rdata2.expect(0x2222.U)
      dut.io.raddr3.poke(9.U)
      dut.io.rdata3.expect(0x3333.U)
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
    dut.io.clr_en3.poke(false.B)
    dut.io.clr_addr3.poke(0.U)
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

  it should "clear three writeback destinations in one cycle" in {
    simulate(new BusyTable()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBusy(dut)
      dut.io.set_en.poke(true.B)
      dut.io.set_addr.poke(35.U)
      dut.clock.step()
      dut.io.set_addr.poke(36.U)
      dut.clock.step()
      dut.io.set_addr.poke(37.U)
      dut.clock.step()
      dut.io.set_en.poke(false.B)
      dut.io.raddr1.poke(35.U)
      dut.io.raddr2.poke(36.U)
      dut.io.raddr3.poke(37.U)
      dut.io.ready1.expect(false.B)
      dut.io.ready2.expect(false.B)
      dut.io.ready3.expect(false.B)
      dut.io.clr_en.poke(true.B)
      dut.io.clr_addr.poke(35.U)
      dut.io.clr_en2.poke(true.B)
      dut.io.clr_addr2.poke(36.U)
      dut.io.clr_en3.poke(true.B)
      dut.io.clr_addr3.poke(37.U)
      dut.clock.step()
      idleBusy(dut)
      dut.io.raddr1.poke(35.U)
      dut.io.raddr2.poke(36.U)
      dut.io.raddr3.poke(37.U)
      dut.io.ready1.expect(true.B)
      dut.io.ready2.expect(true.B)
      dut.io.ready3.expect(true.B)
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
    dut.io.wb2_fire.poke(false.B)
    dut.io.wb2_idx.poke(0.U)
    dut.io.wb2_val.poke(0.U)
    dut.io.wb2_mem_addr.poke(0.U)
    dut.io.wb2_mem_wdata.poke(0.U)
    dut.io.wb2_actual_taken.poke(false.B)
    dut.io.wb2_actual_target.poke(0.U)
    dut.io.wb2_state.state.poke(false.B)
    dut.io.wb2_state.state_num.poke(0.U)
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

  it should "accept three writebacks in one cycle" in {
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
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000008".U)
      dut.io.enq_bits.arch_rd.poke(3.U)
      dut.io.enq_bits.new_phys.poke(34.U)
      dut.clock.step()
      idleRob(dut)
      dut.io.wb_fire.poke(true.B)
      dut.io.wb_idx.poke(0.U)
      dut.io.wb_val.poke(0x11.U)
      dut.io.wb1_fire.poke(true.B)
      dut.io.wb1_idx.poke(1.U)
      dut.io.wb1_val.poke(0x22.U)
      dut.io.wb2_fire.poke(true.B)
      dut.io.wb2_idx.poke(2.U)
      dut.io.wb2_val.poke(0x33.U)
      dut.clock.step()
      idleRob(dut)
      dut.io.entries(0).done.expect(true.B)
      dut.io.entries(0).dest_val.expect(0x11.U)
      dut.io.entries(1).done.expect(true.B)
      dut.io.entries(1).dest_val.expect(0x22.U)
      dut.io.entries(2).done.expect(true.B)
      dut.io.entries(2).dest_val.expect(0x33.U)
    }
  }

  it should "retire a direct control completion without a data writeback" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000100".U)
      dut.io.enq_bits.jump.poke(0.U)
      dut.clock.step()
      idleRob(dut)

      dut.io.ctrl_wb_fire.poke(true.B)
      dut.io.ctrl_wb_idx.poke(0.U)
      dut.io.ctrl_wb_actual_taken.poke(true.B)
      dut.io.ctrl_wb_actual_target.poke("h80000200".U)
      dut.io.commit_valid.expect(true.B)
      dut.io.commit_fire.poke(true.B)
      dut.clock.step()
      idleRob(dut)
      dut.io.count.expect(0.U)
    }
  }

  it should "retire a direct store completion with same-cycle address bypass" in {
    simulate(new ROB()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRob(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.pc.poke("h80000140".U)
      dut.io.enq_bits.mem_valid.poke(true.B)
      dut.io.enq_bits.mem_write.poke(true.B)
      dut.io.enq_bits.mem_wmask.poke("b1111".U)
      dut.clock.step()
      idleRob(dut)

      dut.io.store_wb_fire.poke(true.B)
      dut.io.store_wb_idx.poke(0.U)
      dut.io.store_wb_mem_addr.poke("h80001000".U)
      dut.io.store_wb_mem_wdata.poke("h12345678".U)
      dut.io.commit_valid.expect(true.B)
      dut.io.commit_fire.poke(true.B)
      dut.clock.step()
      idleRob(dut)
      dut.io.count.expect(0.U)
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
    dut.io.free_rob2_fire.poke(false.B)
    dut.io.free_rob2_idx.poke(0.U)
    dut.io.free_ctrl_fire.poke(false.B)
    dut.io.free_ctrl_idx.poke(0.U)
    dut.io.free_store_fire.poke(false.B)
    dut.io.free_store_idx.poke(0.U)
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
    dut.io.cdb2_valid.poke(false.B)
    dut.io.cdb2_pdest.poke(0.U)
    dut.io.cdb2_val.poke(0.U)
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

  it should "issue two fresh independent integer operations on their enqueue cycle" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(4.U)
      dut.io.enq_bits.pc.poke("h80000100".U)
      dut.io.enq1_fire.poke(true.B)
      dut.io.enq1_bits.rob_idx.poke(5.U)
      dut.io.enq1_bits.pc.poke("h80000104".U)

      dut.io.issue_alu_valid.expect(true.B)
      dut.io.issue_alu_bits.rob_idx.expect(4.U)
      dut.io.issue_alu1_valid.expect(true.B)
      dut.io.issue_alu1_bits.rob_idx.expect(5.U)
      dut.io.issue_alu_fire.poke(true.B)
      dut.io.issue_alu1_fire.poke(true.B)
      dut.clock.step()

      idleRs(dut)
      dut.io.count.expect(2.U)
      dut.io.issue_alu_valid.expect(false.B)
      dut.io.issue_alu1_valid.expect(false.B)
    }
  }

  it should "not fresh-issue a lane1 load behind an unresolved lane0 store" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(8.U)
      dut.io.enq_bits.src1_ready.poke(false.B)
      dut.io.enq_bits.lsu_mem_valid.poke(true.B)
      dut.io.enq_bits.lsu_mem_write.poke(true.B)
      dut.io.enq1_fire.poke(true.B)
      dut.io.enq1_bits.rob_idx.poke(9.U)
      dut.io.enq1_bits.lsu_mem_valid.poke(true.B)
      dut.io.enq1_bits.lsu_mem_write.poke(false.B)

      dut.io.issue_lsu_valid.expect(false.B)
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

  it should "wake operands from all three CDB lanes" in {
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
      dut.io.cdb2_valid.poke(true.B)
      dut.io.cdb2_pdest.poke(6.U)
      dut.io.cdb2_val.poke(0x66.U)
      dut.clock.step()
      idleRs(dut)
      dut.io.issue_valid.expect(true.B)
      dut.io.issue_bits.rob_idx.expect(0.U)
      dut.io.issue_bits.src1_val.expect(0x55.U)
      dut.io.issue_bits.src2_val.expect(0x66.U)
    }
  }

  it should "select and bypass a resident entry on the CDB wakeup cycle" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(3.U)
      dut.io.enq_bits.src1_ready.poke(false.B)
      dut.io.enq_bits.src2_ready.poke(false.B)
      dut.io.enq_bits.src1_phys.poke(9.U)
      dut.io.enq_bits.src2_phys.poke(10.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_alu_valid.expect(false.B)

      dut.io.cdb_valid.poke(true.B)
      dut.io.cdb_pdest.poke(9.U)
      dut.io.cdb_val.poke("h12345678".U)
      dut.io.cdb1_valid.poke(true.B)
      dut.io.cdb1_pdest.poke(10.U)
      dut.io.cdb1_val.poke("h89abcdef".U)
      dut.io.issue_alu_valid.expect(true.B)
      dut.io.issue_alu_bits.rob_idx.expect(3.U)
      dut.io.issue_alu_bits.src1_val.expect("h12345678".U)
      dut.io.issue_alu_bits.src2_val.expect("h89abcdef".U)
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

  it should "free three completed rob entries in one cycle" in {
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
      dut.io.free_rob_fire.poke(true.B)
      dut.io.free_rob_idx.poke(0.U)
      dut.io.free_rob1_fire.poke(true.B)
      dut.io.free_rob1_idx.poke(1.U)
      dut.io.free_rob2_fire.poke(true.B)
      dut.io.free_rob2_idx.poke(2.U)
      dut.clock.step()
      idleRs(dut)
      dut.io.count.expect(0.U)
    }
  }

  it should "free an issued control entry through the completion sideband" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(7.U)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_alu_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_alu_fire.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.free_ctrl_fire.poke(true.B)
      dut.io.free_ctrl_idx.poke(7.U)
      dut.clock.step()
      idleRs(dut)
      dut.io.count.expect(0.U)
    }
  }

  it should "free an issued store entry through the completion sideband" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      dut.io.enq_bits.rob_idx.poke(9.U)
      dut.io.enq_bits.lsu_mem_valid.poke(true.B)
      dut.io.enq_bits.lsu_mem_write.poke(true.B)
      dut.clock.step()
      dut.io.enq_fire.poke(false.B)
      dut.io.issue_lsu_fire.poke(true.B)
      dut.clock.step()
      dut.io.issue_lsu_fire.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.free_store_fire.poke(true.B)
      dut.io.free_store_idx.poke(9.U)
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

  it should "issue a load past a registered unresolved store for LQ replay" in {
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

  it should "accept a lane1-only enqueue into the final free slot" in {
    simulate(new RS()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleRs(dut)
      dut.io.enq_fire.poke(true.B)
      for (i <- 0 until OoOParams.RS_SIZE - 1) {
        dut.io.enq_bits.rob_idx.poke(i.U)
        dut.io.enq_bits.pc.poke((0x80000000L + i * 4).U)
        dut.clock.step()
      }
      dut.io.enq_fire.poke(false.B)
      dut.io.count.expect((OoOParams.RS_SIZE - 1).U)
      dut.io.enq1_fire.poke(true.B)
      dut.io.enq1_bits.rob_idx.poke((OoOParams.RS_SIZE - 1).U)
      dut.io.enq1_bits.pc.poke((0x80000000L + (OoOParams.RS_SIZE - 1) * 4).U)
      dut.io.enq1_idx.expect((OoOParams.RS_SIZE - 1).U)
      dut.clock.step()
      dut.io.enq1_fire.poke(false.B)
      dut.io.count.expect(OoOParams.RS_SIZE.U)
    }
  }

  behavior of "BranchIssueQueue"

  def idleBranchQueue(dut: BranchIssueQueue): Unit = {
    dut.io.enq.valid.poke(false.B)
    dut.io.issue.ready.poke(false.B)
    dut.io.robHead.poke(0.U)
    dut.io.cdb0Valid.poke(false.B)
    dut.io.cdb0Pdest.poke(0.U)
    dut.io.cdb0Value.poke(0.U)
    dut.io.cdb1Valid.poke(false.B)
    dut.io.cdb1Pdest.poke(0.U)
    dut.io.cdb1Value.poke(0.U)
    dut.io.cdb2Valid.poke(false.B)
    dut.io.cdb2Pdest.poke(0.U)
    dut.io.cdb2Value.poke(0.U)
    dut.io.free0Valid.poke(false.B)
    dut.io.free0Rob.poke(0.U)
    dut.io.free1Valid.poke(false.B)
    dut.io.free1Rob.poke(0.U)
    dut.io.free2Valid.poke(false.B)
    dut.io.free2Rob.poke(0.U)
    dut.io.freeDirectValid.poke(false.B)
    dut.io.freeDirectRob.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flushIdx.poke(0.U)
    dut.io.flushAll.poke(false.B)
    dut.io.enq.bits.valid.poke(false.B)
    dut.io.enq.bits.issued.poke(false.B)
    dut.io.enq.bits.rob_idx.poke(0.U)
    dut.io.enq.bits.cp_idx.poke(0.U)
    dut.io.enq.bits.src1_ready.poke(true.B)
    dut.io.enq.bits.src2_ready.poke(true.B)
    dut.io.enq.bits.src1_phys.poke(0.U)
    dut.io.enq.bits.src2_phys.poke(0.U)
    dut.io.enq.bits.src1_val.poke(0.U)
    dut.io.enq.bits.src2_val.poke(0.U)
    dut.io.enq.bits.pdest.poke(0.U)
    dut.io.enq.bits.old_phys.poke(0.U)
    dut.io.enq.bits.do_rename.poke(false.B)
    dut.io.enq.bits.pc.poke(0.U)
    dut.io.enq.bits.inst.poke(0.U)
    dut.io.enq.bits.imm_ext.poke(0.U)
    dut.io.enq.bits.waddr.poke(0.U)
    dut.io.enq.bits.is_ebreak.poke(false.B)
    dut.io.enq.bits.is_fencei.poke(false.B)
    dut.io.enq.bits.csr_rd1.poke(0.U)
    dut.io.enq.bits.csr_waddr.poke(0.U)
    dut.io.enq.bits.state.state.poke(false.B)
    dut.io.enq.bits.state.state_num.poke(0.U)
    dut.io.enq.bits.bp_valid.poke(false.B)
    dut.io.enq.bits.bp_taken.poke(false.B)
    dut.io.enq.bits.bp_target.poke(0.U)
    dut.io.enq.bits.bp_index.poke(0.U)
    dut.io.enq.bits.ftq_idx.poke(0.U)
    dut.io.enq.bits.ftq_generation.poke(0.U)
    dut.io.enq.bits.exu_alu_srcA.poke(0.U)
    dut.io.enq.bits.exu_alu_srcB.poke(0.U)
    dut.io.enq.bits.exu_alu_control.poke(0.U)
    dut.io.enq.bits.exu_jump.poke(0.U)
    dut.io.enq.bits.lsu_mem_wmask.poke(0.U)
    dut.io.enq.bits.lsu_mem_rd.poke(0.U)
    dut.io.enq.bits.lsu_mem_write.poke(false.B)
    dut.io.enq.bits.lsu_mem_valid.poke(false.B)
    dut.io.enq.bits.wbu_reg_write.poke(false.B)
    dut.io.enq.bits.wbu_reg_write_sel.poke(0.U)
    dut.io.enq.bits.wbu_csr_write.poke(false.B)
    dut.io.enq.bits.wbu_csr_sel.poke(0.U)
  }

  it should "issue a younger ready branch while an older branch waits" in {
    simulate(new BranchIssueQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBranchQueue(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.rob_idx.poke(4.U)
      dut.io.enq.bits.src1_ready.poke(false.B)
      dut.io.enq.bits.src1_phys.poke(9.U)
      dut.clock.step()
      dut.io.enq.bits.rob_idx.poke(5.U)
      dut.io.enq.bits.src1_ready.poke(true.B)
      dut.io.enq.bits.src1_phys.poke(0.U)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.issue.valid.expect(true.B)
      dut.io.issue.bits.rob_idx.expect(5.U)
      dut.io.issue.ready.poke(true.B)
      dut.clock.step()

      dut.io.issue.ready.poke(false.B)
      dut.io.cdb0Valid.poke(true.B)
      dut.io.cdb0Pdest.poke(9.U)
      dut.io.cdb0Value.poke("h12345678".U)
      dut.io.issue.valid.expect(true.B)
      dut.io.issue.bits.rob_idx.expect(4.U)
      dut.io.issue.bits.src1_val.expect("h12345678".U)
    }
  }

  it should "issue a fresh ready branch on its enqueue cycle" in {
    simulate(new BranchIssueQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBranchQueue(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.rob_idx.poke(3.U)
      dut.io.enq.bits.pc.poke("h80000180".U)
      dut.io.issue.ready.poke(true.B)
      dut.io.enq.ready.expect(true.B)
      dut.io.issue.valid.expect(true.B)
      dut.io.issue.bits.rob_idx.expect(3.U)
      dut.io.issue.bits.pc.expect("h80000180".U)
      dut.clock.step()

      idleBranchQueue(dut)
      dut.io.count.expect(1.U)
      dut.io.issue.valid.expect(false.B)
      dut.io.freeDirectValid.poke(true.B)
      dut.io.freeDirectRob.poke(3.U)
      dut.clock.step()
      idleBranchQueue(dut)
      dut.io.count.expect(0.U)
    }
  }

  it should "free completed entries and selectively flush younger branches" in {
    simulate(new BranchIssueQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBranchQueue(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.rob_idx.poke(6.U)
      dut.clock.step()
      dut.io.enq.bits.rob_idx.poke(7.U)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(2.U)

      dut.io.flush.poke(true.B)
      dut.io.flushIdx.poke(6.U)
      dut.io.freeDirectValid.poke(true.B)
      dut.io.freeDirectRob.poke(6.U)
      dut.clock.step()
      dut.io.flush.poke(false.B)
      idleBranchQueue(dut)
      dut.io.count.expect(0.U)
    }
  }

  it should "retain a CDB wakeup for an older branch during a younger flush" in {
    simulate(new BranchIssueQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBranchQueue(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.rob_idx.poke(4.U)
      dut.io.enq.bits.src1_ready.poke(false.B)
      dut.io.enq.bits.src1_phys.poke(9.U)
      dut.clock.step()
      dut.io.enq.bits.rob_idx.poke(7.U)
      dut.io.enq.bits.src1_ready.poke(true.B)
      dut.io.enq.bits.src1_phys.poke(0.U)
      dut.clock.step()
      idleBranchQueue(dut)

      dut.io.flush.poke(true.B)
      dut.io.flushIdx.poke(6.U)
      dut.io.cdb0Valid.poke(true.B)
      dut.io.cdb0Pdest.poke(9.U)
      dut.io.cdb0Value.poke("h12345678".U)
      dut.clock.step()

      idleBranchQueue(dut)
      dut.io.count.expect(1.U)
      dut.io.issue.valid.expect(true.B)
      dut.io.issue.bits.rob_idx.expect(4.U)
      dut.io.issue.bits.src1_val.expect("h12345678".U)
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

  it should "grant three oldest non-duplicate results across ROB wraparound" in {
    simulate(new WritebackArbiter(inputCount = 5, outputCount = 3)) { dut =>
      resetDut(dut.clock, dut.reset)
      dut.io.robHead.poke(30.U)
      for (o <- 0 until 3) dut.io.out(o).ready.poke(true.B)
      val robs = Seq(5, 31, 20, 1, 0)
      for (i <- robs.indices) {
        dut.io.in(i).valid.poke(true.B)
        pokeWritebackCandidate(dut.io.in(i).bits, robs(i), BigInt(i + 1))
      }

      Seq(31, 0, 1).zipWithIndex.foreach { case (rob, o) =>
        dut.io.out(o).valid.expect(true.B)
        dut.io.out(o).bits.rob_idx.expect(rob.U)
      }
      Seq(false, true, false, true, true).zipWithIndex.foreach { case (ready, i) =>
        dut.io.in(i).ready.expect(ready.B)
      }
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
    dut.io.ld1_valid.poke(false.B)
    dut.io.ld1_rob.poke(0.U)
    dut.io.ld1_addr.poke(0.U)
    dut.io.ld1_mem_rd.poke(RWORD)
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
      dut.io.partial_valid.expect(false.B)
    }
  }

  it should "combine two partial older stores into one full forward" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(WHALF)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(WHALF)
      dut.clock.step()

      idleStoreQueue(dut)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h80000100".U)
      dut.io.wb_data.poke("hbbaa".U)
      dut.io.wb_mask.poke(WHALF)
      dut.io.wb1_valid.poke(true.B)
      dut.io.wb1_rob.poke(1.U)
      dut.io.wb1_addr.poke("h80000102".U)
      dut.io.wb1_data.poke("hddcc".U)
      dut.io.wb1_mask.poke(WHALF)
      dut.clock.step()

      idleStoreQueue(dut)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h80000100".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(true.B)
      dut.io.fwd_data.expect("hddccbbaa".U)
    }
  }

  it should "overlay a younger partial store on an older full store" in {
    simulate(new StoreQueue()) { dut =>
      idleStoreQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleStoreQueue(dut)
      dut.io.alloc0_valid.poke(true.B)
      dut.io.alloc0_rob.poke(0.U)
      dut.io.alloc0_mask.poke(WWORD)
      dut.io.alloc1_valid.poke(true.B)
      dut.io.alloc1_rob.poke(1.U)
      dut.io.alloc1_mask.poke(WBYTE)
      dut.clock.step()

      idleStoreQueue(dut)
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(0.U)
      dut.io.wb_addr.poke("h80000200".U)
      dut.io.wb_data.poke("h11223344".U)
      dut.io.wb_mask.poke(WWORD)
      dut.io.wb1_valid.poke(true.B)
      dut.io.wb1_rob.poke(1.U)
      dut.io.wb1_addr.poke("h80000201".U)
      dut.io.wb1_data.poke("haa".U)
      dut.io.wb1_mask.poke(WBYTE)
      dut.clock.step()

      idleStoreQueue(dut)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h80000200".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.wait_load.expect(false.B)
      dut.io.fwd_valid.expect(true.B)
      dut.io.fwd_data.expect("h1122aa44".U)
    }
  }

  it should "not classify an unknown wait as bypass-only when a store can forward" in {
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
      dut.io.wb_valid.poke(true.B)
      dut.io.wb_rob.poke(1.U)
      dut.io.wb_addr.poke("h1000".U)
      dut.io.wb_data.poke("h12345678".U)
      dut.io.wb_mask.poke(15.U)
      dut.clock.step()

      idleStoreQueue(dut)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_rob.poke(2.U)
      dut.io.ld_addr.poke("h1000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.wait_unknown.expect(true.B)
      dut.io.has_fwd_candidate.expect(true.B)
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
      dut.io.partial_valid.expect(true.B)
      dut.io.partial_data.expect("h0000aa00".U)
      dut.io.partial_mask.expect("b0010".U)
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
    dut.io.alloc1.valid.poke(false.B)
    dut.io.alloc1.bits.meta.signals.wbu.reg_write.poke(false.B)
    dut.io.alloc1.bits.meta.signals.wbu.reg_write_sel.poke(0.U)
    dut.io.alloc1.bits.meta.signals.wbu.csr_write.poke(false.B)
    dut.io.alloc1.bits.meta.signals.wbu.csr_sel.poke(0.U)
    dut.io.alloc1.bits.meta.signals.wbu.irq.poke(false.B)
    dut.io.alloc1.bits.meta.signals.wbu.irq_num.poke(0.U)
    dut.io.alloc1.bits.meta.rd1.poke(0.U)
    dut.io.alloc1.bits.meta.alu_result.poke(0.U)
    dut.io.alloc1.bits.meta.pc.poke(0.U)
    dut.io.alloc1.bits.meta.next_pc.poke(0.U)
    dut.io.alloc1.bits.meta.imm_ext.poke(0.U)
    dut.io.alloc1.bits.meta.mem_read.poke(0.U)
    dut.io.alloc1.bits.meta.waddr.poke(0.U)
    dut.io.alloc1.bits.meta.is_ebreak.poke(false.B)
    dut.io.alloc1.bits.meta.csr_rd1.poke(0.U)
    dut.io.alloc1.bits.meta.csr_waddr.poke(0.U)
    dut.io.alloc1.bits.meta.state.state.poke(false.B)
    dut.io.alloc1.bits.meta.state.state_num.poke(0.U)
    dut.io.alloc1.bits.meta.rob_idx.poke(0.U)
    dut.io.alloc1.bits.meta.pdest.poke(0.U)
    dut.io.alloc1.bits.meta.old_phys.poke(0.U)
    dut.io.alloc1.bits.meta.do_rename.poke(false.B)
    dut.io.alloc1.bits.meta.br_taken.poke(false.B)
    dut.io.alloc1.bits.meta.store_data.poke(0.U)
    dut.io.alloc1.bits.meta.fwd_valid.poke(false.B)
    dut.io.alloc1.bits.meta.fwd_data.poke(0.U)
    dut.io.alloc1.bits.addr.poke(0.U)
    dut.io.alloc1.bits.memRd.poke(RWORD)
    dut.io.wb.ready.poke(false.B)
    dut.io.wb1.ready.poke(false.B)
    dut.io.robHead.poke(0.U)
    dut.io.commit0Valid.poke(false.B)
    dut.io.commit0Rob.poke(0.U)
    dut.io.commit1Valid.poke(false.B)
    dut.io.commit1Rob.poke(0.U)
    dut.io.commit2Valid.poke(false.B)
    dut.io.commit2Rob.poke(0.U)
    dut.io.commit3Valid.poke(false.B)
    dut.io.commit3Rob.poke(0.U)
    dut.io.flush.poke(false.B)
    dut.io.flushIdx.poke(0.U)
    dut.io.flushAll.poke(false.B)
    dut.io.fwdWait.poke(false.B)
    dut.io.fwdValid.poke(false.B)
    dut.io.fwdData.poke(0.U)
    dut.io.partialValid.poke(false.B)
    dut.io.partialData.poke(0.U)
    dut.io.partialMask.poke(0.U)
    dut.io.unknownValid.poke(false.B)
    dut.io.unknownMask.poke(0.U)
    dut.io.fwd1Wait.poke(false.B)
    dut.io.fwd1Valid.poke(false.B)
    dut.io.fwd1Data.poke(0.U)
    dut.io.partial1Valid.poke(false.B)
    dut.io.partial1Data.poke(0.U)
    dut.io.partial1Mask.poke(0.U)
    dut.io.unknown1Valid.poke(false.B)
    dut.io.unknown1Mask.poke(0.U)
    dut.io.unresolvedStores.poke(0.U)
    dut.io.mmioReady.poke(true.B)
    dut.io.storeResolve0Valid.poke(false.B)
    dut.io.storeResolve0Rob.poke(0.U)
    dut.io.storeResolve0Addr.poke(0.U)
    dut.io.storeResolve0Mask.poke(0.U)
    dut.io.storeResolve1Valid.poke(false.B)
    dut.io.storeResolve1Rob.poke(0.U)
    dut.io.storeResolve1Addr.poke(0.U)
    dut.io.storeResolve1Mask.poke(0.U)
    dut.io.storeResolveHead.poke(0.U)
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
    dut.io.dmem1.arready.poke(false.B)
    dut.io.dmem1.rdata.poke(0.U)
    dut.io.dmem1.rresp.poke(0.U)
    dut.io.dmem1.rvalid.poke(false.B)
    dut.io.dmem1.rlast.poke(false.B)
    dut.io.dmem1.rid.poke(0.U)
    dut.io.dmem1.awready.poke(false.B)
    dut.io.dmem1.wready.poke(false.B)
    dut.io.dmem1.bvalid.poke(false.B)
    dut.io.dmem1.bresp.poke(0.U)
    dut.io.dmem1.bid.poke(0.U)
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

  def allocLoad1(dut: LoadQueue, rob: Int, pc: BigInt, addr: BigInt): Unit = {
    dut.io.alloc1.valid.poke(true.B)
    dut.io.alloc1.bits.meta.pc.poke(pc.U)
    dut.io.alloc1.bits.meta.next_pc.poke((pc + 4).U)
    dut.io.alloc1.bits.meta.alu_result.poke(addr.U)
    dut.io.alloc1.bits.meta.rob_idx.poke(rob.U)
    dut.io.alloc1.bits.meta.pdest.poke((rob + 32).U)
    dut.io.alloc1.bits.meta.waddr.poke((rob + 1).U)
    dut.io.alloc1.bits.addr.poke(addr.U)
    dut.io.alloc1.bits.memRd.poke(RWORD)
  }

  it should "dual-allocate loads and schedule the older ROB entry first" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000024", 16), BigInt("80001104", 16))
      allocLoad1(dut, 1, BigInt("80000020", 16), BigInt("80001100", 16))
      dut.io.alloc.ready.expect(true.B)
      dut.io.alloc1.ready.expect(true.B)
      dut.io.queryValid.expect(true.B)
      dut.io.queryRob.expect(1.U)
      dut.io.query1Valid.expect(true.B)
      dut.io.query1Rob.expect(2.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(2.U)
      dut.io.queryValid.expect(true.B)
      dut.io.query1Valid.expect(true.B)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem1.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem1.arvalid.expect(true.B)
      val id0 = dut.io.dmem.arid.peek().litValue
      val id1 = dut.io.dmem1.arid.peek().litValue
      assert(id0 != id1, "dual load requests must use distinct response IDs")
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(id0.U)
      dut.io.dmem.rdata.poke("haaaa1111".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.dmem1.rvalid.poke(true.B)
      dut.io.dmem1.rid.poke(id1.U)
      dut.io.dmem1.rdata.poke("hbbbb2222".U)
      dut.io.dmem1.rlast.poke(true.B)
      dut.io.dmem.rready.expect(true.B)
      dut.io.dmem1.rready.expect(true.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb1.ready.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb1.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(1.U)
      dut.io.wb1.bits.rob_idx.expect(2.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "directly write back two same-cycle load responses" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 1, BigInt("80000020", 16), BigInt("80001200", 16))
      allocLoad1(dut, 2, BigInt("80000024", 16), BigInt("80001204", 16))
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem1.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem1.arvalid.expect(true.B)
      val id0 = dut.io.dmem.arid.peek().litValue
      val id1 = dut.io.dmem1.arid.peek().litValue
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb1.ready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(id0.U)
      dut.io.dmem.rdata.poke("h11112222".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.dmem1.rvalid.poke(true.B)
      dut.io.dmem1.rid.poke(id1.U)
      dut.io.dmem1.rdata.poke("h33334444".U)
      dut.io.dmem1.rlast.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb1.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(1.U)
      dut.io.wb1.bits.rob_idx.expect(2.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
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

  it should "merge a delayed cache response with a snapshotted partial store" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 3, BigInt("80000028", 16), BigInt("80001001", 16))
      dut.io.alloc.bits.memRd.poke(RHALFU)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.partialValid.poke(true.B)
      dut.io.partialData.poke("h0000aa00".U)
      dut.io.partialMask.poke("b0010".U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      val requestId = dut.io.dmem.arid.peek().litValue
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(requestId.U)
      dut.io.dmem.rdata.poke("h44332211".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(3.U)
      dut.io.wb.bits.mem_read.expect("h000033aa".U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "release four committed loads in one cycle" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      for (rob <- 1 to 4) {
        idleLoadQueue(dut)
        allocLoad(dut, rob, BigInt("80000000", 16) + rob * 4,
          BigInt("80001000", 16) + rob * 4)
        dut.io.alloc.ready.expect(true.B)
        dut.clock.step()
      }

      idleLoadQueue(dut)
      dut.io.outstanding.expect(4.U)
      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Rob.poke(1.U)
      dut.io.commit1Valid.poke(true.B)
      dut.io.commit1Rob.poke(2.U)
      dut.io.commit2Valid.poke(true.B)
      dut.io.commit2Rob.poke(3.U)
      dut.io.commit3Valid.poke(true.B)
      dut.io.commit3Rob.poke(4.U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "accept a same-cycle cache response for a new request" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 3, BigInt("80000060", 16), BigInt("80001204", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("ha1b2c3d4".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(0.U)
      dut.io.staleResp.expect(false.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(3.U)
      dut.io.wb.bits.mem_read.expect("ha1b2c3d4".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(false.B)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "retain same-cycle speculative responses for replay" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000068", 16), BigInt("80001208", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("hdeadbeef".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(2.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.commit0Rob.poke(2.U)
      dut.io.outstanding.expect(0.U)
      dut.io.commitWait0.expect(true.B)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(1.U)
      dut.io.storeResolve0Addr.poke("h80005000".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.commit0Rob.poke(2.U)
      dut.io.commitWait0.expect(false.B)
      dut.io.commit0Valid.poke(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "block a load response when its older store resolves in the same cycle" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("8000006a", 16), BigInt("80001208", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("hdeadbeef".U)
      dut.io.dmem.rlast.poke(true.B)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(1.U)
      dut.io.storeResolve0Addr.poke("h80001208".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.wb.valid.expect(false.B)
      dut.io.violationValid.expect(true.B)
      dut.io.violationRob.expect(2.U)
    }
  }

  it should "merge known partial bytes while tracking an unknown older store" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("8000006c", 16), BigInt("80004100", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.partialValid.poke(true.B)
      dut.io.partialData.poke("h0000aa00".U)
      dut.io.partialMask.poke("b0010".U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("h11223344".U)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.storeReplayCount.expect(0.U)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.mem_read.expect("h1122aa44".U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.commit0Rob.poke(2.U)
      dut.io.commitWait0.expect(true.B)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "speculatively forward known full bytes under an unknown store" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000070", 16), BigInt("80004200", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.fwdValid.poke(true.B)
      dut.io.fwdData.poke("hdeadbeef".U)
      dut.io.dmem.arvalid.expect(false.B)
      dut.io.storeReplayCount.expect(0.U)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.mem_read.expect("hdeadbeef".U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(1.U)
      dut.io.storeResolve0Addr.poke("h80004200".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.violationValid.expect(true.B)
      dut.io.violationRob.expect(2.U)
    }
  }

  it should "schedule a fresh allocation without an sWait bubble" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 5, BigInt("80000074", 16), BigInt("80001400", 16))
      dut.io.wb.ready.poke(true.B)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("h12345678".U)
      dut.io.queryValid.expect(true.B)
      dut.io.queryRob.expect(5.U)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(5.U)
      dut.io.wb.bits.mem_read.expect("h12345678".U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(false.B)
      dut.io.outstanding.expect(0.U)
    }
  }

  it should "buffer a direct response when writeback is backpressured" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 4, BigInt("80000070", 16), BigInt("80001300", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("h55667788".U)
      dut.io.wb.ready.poke(false.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(4.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(false.B)
      dut.io.wb.valid.expect(true.B)
      dut.io.wb.bits.rob_idx.expect(4.U)
      dut.io.wb.bits.mem_read.expect("h55667788".U)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.wb.valid.expect(true.B)
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.wb.valid.expect(false.B)
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

  it should "quarantine a flushed response ID until its stale response drains" in {
    simulate(new LoadQueue(new core.CoreConfig(32))) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000070", 16), BigInt("80002100", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(0.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.flush.poke(true.B)
      dut.io.flushIdx.poke(1.U)
      dut.clock.step()

      // Index 0 still owns stale ID0, so the next load must use index 1.
      idleLoadQueue(dut)
      allocLoad(dut, 3, BigInt("80000074", 16), BigInt("80002200", 16))
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(1.U)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.dmem.rvalid.poke(true.B)
      dut.io.dmem.rid.poke(0.U)
      dut.io.dmem.rdata.poke("hdeadbeef".U)
      dut.io.staleResp.expect(true.B)
      dut.io.wb.valid.expect(false.B)
      dut.clock.step()

      // Once stale ID0 drains, index 0 can return with its next generation.
      idleLoadQueue(dut)
      allocLoad(dut, 4, BigInt("80000078", 16), BigInt("80002300", 16))
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.io.dmem.arid.expect(8.U)
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
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
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

  it should "hold a bypassed load at commit until its older store resolves" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000090", 16), BigInt("80003100", 16))
      dut.clock.step()
      idleLoadQueue(dut)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.dmem.arready.poke(true.B)
      dut.io.dmem.arvalid.expect(true.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.commit0Rob.poke(2.U)
      dut.io.commitWait0.expect(true.B)
      dut.io.unresolvedStores.poke(0.U)
      dut.io.commitWait0.expect(false.B)
    }
  }

  it should "discard stale speculative dependencies before a ROB slot is reused" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 2, BigInt("80000094", 16), BigInt("80003200", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(2.U)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.fwdValid.poke(true.B)
      dut.io.fwdData.poke("h12345678".U)
      dut.io.wb.valid.expect(true.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.commit0Rob.poke(2.U)
      dut.io.commitWait0.expect(true.B)

      // If the SQ no longer owns the original store, the ROB index bit is stale.
      dut.io.unresolvedStores.poke(0.U)
      dut.io.commitWait0.expect(false.B)
      dut.clock.step()

      // A younger store may now reuse that ROB slot without becoming a false
      // dependency or causing a false memory-order violation.
      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(2.U)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(1.U)
      dut.io.storeResolve0Addr.poke("h80003200".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.violationValid.expect(false.B)
      dut.io.commit0Rob.poke(2.U)
      dut.io.commitWait0.expect(false.B)
    }
  }

  it should "ignore a younger store that reuses a dependency ROB slot" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      allocLoad(dut, 16, BigInt("80000098", 16), BigInt("80003300", 16))
      dut.clock.step()

      // With ROB head 0, store slot 3 is older than load slot 16 and must be
      // captured as a real speculative dependency.
      idleLoadQueue(dut)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke(8.U)
      dut.io.unresolvedStores.poke(8.U)
      dut.io.fwdValid.poke(true.B)
      dut.io.fwdData.poke("h89abcdef".U)
      dut.io.wb.valid.expect(true.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.unresolvedStores.poke(8.U)
      dut.io.commit0Rob.poke(16.U)
      dut.io.commitWait0.expect(true.B)

      // After the head reaches the load, slot 3 denotes a younger dynamic
      // store. Reusing the bit must neither block retirement nor violate.
      dut.io.robHead.poke(16.U)
      dut.io.storeResolveHead.poke(16.U)
      dut.io.commitWait0.expect(false.B)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(3.U)
      dut.io.storeResolve0Addr.poke("h80003300".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.violationValid.expect(false.B)
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.robHead.poke(16.U)
      dut.io.unresolvedStores.poke(8.U)
      dut.io.commit0Rob.poke(16.U)
      dut.io.commitWait0.expect(false.B)
    }
  }

  it should "use the store resolve epoch after ROB head advances" in {
    simulate(new LoadQueue(new core.CoreConfig(32), speculateUnknownStores = true)) { dut =>
      idleLoadQueue(dut)
      resetDut(dut.clock, dut.reset)
      idleLoadQueue(dut)
      dut.io.robHead.poke(21.U)
      allocLoad(dut, 6, BigInt("8000009c", 16), BigInt("80003400", 16))
      dut.clock.step()

      idleLoadQueue(dut)
      dut.io.robHead.poke(21.U)
      dut.io.wb.ready.poke(true.B)
      dut.io.unknownValid.poke(true.B)
      dut.io.unknownMask.poke((BigInt(1) << 28).U)
      dut.io.unresolvedStores.poke((BigInt(1) << 28).U)
      dut.io.fwdValid.poke(true.B)
      dut.io.fwdData.poke("h12345678".U)
      dut.io.wb.valid.expect(true.B)
      dut.clock.step()

      // The resolve sideband is delayed by one cycle. The current head may
      // already have passed the store, but its captured resolve epoch has not.
      idleLoadQueue(dut)
      dut.io.robHead.poke(29.U)
      dut.io.storeResolveHead.poke(21.U)
      dut.io.storeResolve0Valid.poke(true.B)
      dut.io.storeResolve0Rob.poke(28.U)
      dut.io.storeResolve0Addr.poke("h80003400".U)
      dut.io.storeResolve0Mask.poke(WWORD)
      dut.io.violationValid.expect(true.B)
      dut.io.violationRob.expect(6.U)
      dut.io.violationPc.expect("h8000009c".U)
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

  behavior of "Xbar"

  def idleXbarUp(up: bus.AXI4Slave): Unit = {
    up.araddr.poke(0.U)
    up.arvalid.poke(false.B)
    up.arid.poke(0.U)
    up.arlen.poke(0.U)
    up.arsize.poke(2.U)
    up.arburst.poke(1.U)
    up.rready.poke(false.B)
    up.awaddr.poke(0.U)
    up.awvalid.poke(false.B)
    up.awid.poke(0.U)
    up.awlen.poke(0.U)
    up.awsize.poke(2.U)
    up.awburst.poke(1.U)
    up.wdata.poke(0.U)
    up.wstrb.poke(0.U)
    up.wvalid.poke(false.B)
    up.wlast.poke(false.B)
    up.bready.poke(false.B)
  }

  def idleXbarDown(down: bus.AXI4Master): Unit = {
    down.arready.poke(false.B)
    down.rdata.poke(0.U)
    down.rresp.poke(0.U)
    down.rvalid.poke(false.B)
    down.rlast.poke(false.B)
    down.rid.poke(0.U)
    down.awready.poke(false.B)
    down.wready.poke(false.B)
    down.bresp.poke(0.U)
    down.bvalid.poke(false.B)
    down.bid.poke(0.U)
  }

  it should "route read and write channels independently" in {
    simulate(new bus.Xbar(conf)) { dut =>
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)
      resetDut(dut.clock, dut.reset)
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)

      dut.io.dmem.araddr.poke("h80001000".U)
      dut.io.dmem.arvalid.poke(true.B)
      dut.io.dmem.arid.poke(2.U)
      dut.io.dmem.awaddr.poke("h80002000".U)
      dut.io.dmem.awvalid.poke(true.B)
      dut.io.dmem.awid.poke(3.U)
      dut.io.dmem.wdata.poke("h12345678".U)
      dut.io.dmem.wstrb.poke("hf".U)
      dut.io.dmem.wvalid.poke(true.B)
      dut.io.dmem.wlast.poke(true.B)
      dut.io.soc.arready.poke(true.B)
      dut.io.soc.awready.poke(true.B)
      dut.io.soc.wready.poke(true.B)
      dut.io.dmem.arready.expect(true.B)
      dut.io.dmem.awready.expect(true.B)
      dut.io.dmem.wready.expect(true.B)
      dut.io.soc.arvalid.expect(true.B)
      dut.io.soc.awvalid.expect(true.B)
      dut.io.soc.wvalid.expect(true.B)
      dut.clock.step()

      dut.io.dmem.arvalid.poke(false.B)
      dut.io.dmem.awvalid.poke(false.B)
      dut.io.dmem.wvalid.poke(false.B)
      dut.io.dmem.rready.poke(true.B)
      dut.io.dmem.bready.poke(true.B)
      dut.io.soc.rdata.poke("h89abcdef".U)
      dut.io.soc.rresp.poke(0.U)
      dut.io.soc.rvalid.poke(true.B)
      dut.io.soc.rlast.poke(true.B)
      dut.io.soc.rid.poke(2.U)
      dut.io.soc.bresp.poke(0.U)
      dut.io.soc.bvalid.poke(true.B)
      dut.io.soc.bid.poke(3.U)
      dut.io.dmem.rvalid.expect(true.B)
      dut.io.dmem.rdata.expect("h89abcdef".U)
      dut.io.dmem.bvalid.expect(true.B)
      dut.clock.step()

      dut.io.soc.rvalid.poke(false.B)
      dut.io.soc.rlast.poke(false.B)
      dut.io.soc.bvalid.poke(false.B)
      dut.io.dmem.rready.poke(false.B)
      dut.io.dmem.bready.poke(false.B)
      dut.io.dmem.awaddr.poke("h80003000".U)
      dut.io.dmem.awvalid.poke(true.B)
      dut.io.soc.awready.poke(true.B)
      dut.clock.step()

      dut.io.dmem.awvalid.poke(false.B)
      dut.io.soc.awready.poke(false.B)
      dut.io.imem.araddr.poke("h80004000".U)
      dut.io.imem.arvalid.poke(true.B)
      dut.io.imem.arid.poke(4.U)
      dut.io.soc.arready.poke(true.B)
      dut.io.imem.arready.expect(true.B)
      dut.io.soc.arvalid.expect(true.B)
      dut.io.soc.araddr.expect("h80004000".U)
    }
  }

  it should "retain PMEM write ownership across a same-cycle B to AW handoff" in {
    simulate(new bus.Xbar(conf)) { dut =>
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)
      resetDut(dut.clock, dut.reset)
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)

      dut.io.dmem.awaddr.poke("h80001000".U)
      dut.io.dmem.awvalid.poke(true.B)
      dut.io.soc.awready.poke(true.B)
      dut.clock.step()

      dut.io.dmem.bready.poke(true.B)
      dut.io.soc.bvalid.poke(true.B)
      dut.io.dmem.awaddr.poke("h80002000".U)
      dut.io.dmem.awvalid.poke(true.B)
      dut.io.soc.awready.poke(true.B)
      dut.io.dmem.bvalid.expect(true.B)
      dut.io.dmem.awready.expect(true.B)
      dut.io.soc.awvalid.expect(true.B)
      dut.io.soc.awaddr.expect("h80002000".U)
      dut.clock.step()

      dut.io.soc.bvalid.poke(false.B)
      dut.io.dmem.awvalid.poke(false.B)
      dut.io.soc.awready.poke(false.B)
      dut.io.soc.bvalid.poke(true.B)
      dut.io.soc.bid.poke(1.U)
      dut.io.dmem.bvalid.expect(true.B)
    }
  }

  it should "release single-beat peripheral reads without rlast" in {
    simulate(new bus.Xbar(conf)) { dut =>
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)
      resetDut(dut.clock, dut.reset)
      idleXbarUp(dut.io.imem)
      idleXbarUp(dut.io.dmem)
      idleXbarDown(dut.io.soc)
      idleXbarDown(dut.io.clint)
      idleXbarDown(dut.io.uart.get)

      dut.io.dmem.araddr.poke("ha0000048".U)
      dut.io.dmem.arvalid.poke(true.B)
      dut.io.clint.arready.poke(true.B)
      dut.io.dmem.arready.expect(true.B)
      dut.clock.step()

      dut.io.dmem.arvalid.poke(false.B)
      dut.io.dmem.rready.poke(true.B)
      dut.io.clint.arready.poke(false.B)
      dut.io.clint.rvalid.poke(true.B)
      dut.io.clint.rlast.poke(false.B)
      dut.io.dmem.rvalid.expect(true.B)
      dut.clock.step()

      dut.io.clint.rvalid.poke(false.B)
      dut.io.dmem.rready.poke(false.B)
      dut.io.imem.araddr.poke("h80000000".U)
      dut.io.imem.arvalid.poke(true.B)
      dut.io.soc.arready.poke(true.B)
      dut.io.imem.arready.expect(true.B)
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
    dut.io.cpu1.araddr.poke(0.U)
    dut.io.cpu1.arvalid.poke(false.B)
    dut.io.cpu1.arid.poke(0.U)
    dut.io.cpu1.arlen.poke(0.U)
    dut.io.cpu1.arsize.poke(2.U)
    dut.io.cpu1.arburst.poke(1.U)
    dut.io.cpu1.rready.poke(false.B)
    dut.io.cpu1.awaddr.poke(0.U)
    dut.io.cpu1.awvalid.poke(false.B)
    dut.io.cpu1.awid.poke(0.U)
    dut.io.cpu1.awlen.poke(0.U)
    dut.io.cpu1.awsize.poke(0.U)
    dut.io.cpu1.awburst.poke(0.U)
    dut.io.cpu1.wdata.poke(0.U)
    dut.io.cpu1.wstrb.poke(0.U)
    dut.io.cpu1.wvalid.poke(false.B)
    dut.io.cpu1.wlast.poke(false.B)
    dut.io.cpu1.bready.poke(false.B)
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
    dut.io.store_valid.poke(false.B)
    dut.io.store_addr.poke(0.U)
    dut.io.store_data.poke(0.U)
    dut.io.store_mask.poke(0.U)
    dut.io.store2_valid.poke(false.B)
    dut.io.store2_addr.poke(0.U)
    dut.io.store2_data.poke(0.U)
    dut.io.store2_mask.poke(0.U)
    dut.io.store3_valid.poke(false.B)
    dut.io.store3_addr.poke(0.U)
    dut.io.store3_data.poke(0.U)
    dut.io.store3_mask.poke(0.U)
  }

  def acceptDCacheRefillAr(dut: DCache, base: BigInt,
                           words: Int, criticalWord: Int): Unit = {
    dut.io.mem.arvalid.expect(true.B)
    dut.io.mem.araddr.expect((base + criticalWord * 4).U)
    dut.io.mem.arlen.expect((words - criticalWord - 1).U)
    dut.io.mem.arburst.expect(1.U)
    dut.io.mem.rid.poke(dut.io.mem.arid.peek().litValue.U)
    dut.io.mem.arready.poke(true.B)
    dut.clock.step()
    dut.io.mem.arready.poke(false.B)
  }

  def sendDCacheRefillData(dut: DCache, base: BigInt,
                           values: Seq[BigInt], criticalWord: Int): Unit = {
    for (i <- criticalWord until values.length) {
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(i).U)
      dut.io.mem.rresp.poke(0.U)
      dut.io.mem.rlast.poke((i == values.length - 1).B)
      dut.clock.step()
    }
    dut.io.mem.rvalid.poke(false.B)
    dut.io.mem.rlast.poke(false.B)
    if (criticalWord > 0) {
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
      dut.io.mem.arlen.expect((criticalWord - 1).U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      for (i <- 0 until criticalWord) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rresp.poke(0.U)
        dut.io.mem.rlast.poke((i == criticalWord - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
    }
  }

  def fillDCacheLine(dut: DCache, base: BigInt, values: Seq[BigInt]): Unit = {
    val firstAddr = dut.io.mem.araddr.peek().litValue
    val criticalWord = ((firstAddr - base) / 4).toInt
    acceptDCacheRefillAr(dut, base, values.length, criticalWord)
    sendDCacheRefillData(dut, base, values, criticalWord)
  }

  it should "accept a zero-latency uncached response with the launching MSHR" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)

      dut.io.cpu.araddr.poke("ha0000048".U)
      dut.io.cpu.arid.poke(7.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()

      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rready.poke(true.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect("ha0000048".U)
      dut.io.mem.arlen.expect(0.U)
      val missId = dut.io.mem.arid.peek().litValue
      dut.io.mem.arready.poke(true.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rid.poke(missId.U)
      dut.io.mem.rdata.poke("h12345678".U)
      dut.io.mem.rlast.poke(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(7.U)
      dut.io.cpu.rdata.expect("h12345678".U)
      dut.clock.step()

      idleDCache(dut)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rvalid.expect(false.B)
      dut.io.busy.expect(false.B)
    }
  }

  it should "route back-to-back uncached responses without relying on slave RID" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)

      dut.io.cpu.araddr.poke("ha000004c".U)
      dut.io.cpu.arid.poke(5.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)

      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect("ha000004c".U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)

      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.araddr.poke("ha0000048".U)
      dut.io.cpu.arid.poke(6.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rid.poke(0.U)
      dut.io.mem.rdata.poke("h11112222".U)
      dut.io.mem.rlast.poke(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(5.U)
      dut.io.cpu.rdata.expect("h11112222".U)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.rvalid.poke(false.B)

      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect("ha0000048".U)
      dut.io.mem.arid.expect(1.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)

      // Clint leaves RID at its default zero even though this transaction owns
      // MSHR1. The active downstream owner must still receive the response.
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rid.poke(0.U)
      dut.io.mem.rdata.poke("h33334444".U)
      dut.io.mem.rlast.poke(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(6.U)
      dut.io.cpu.rdata.expect("h33334444".U)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)

      idleDCache(dut)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.busy.expect(false.B)
    }
  }

  it should "return the critical word before the refill completes" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80009000", 16)
      val values = (0 until 8).map(i => BigInt("71000000", 16) + i)

      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arid.poke(5.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base + 8).U)
      dut.io.mem.arlen.expect(5.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)

      dut.io.mem.rready.expect(true.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(2).U)
      dut.io.mem.rlast.poke(false.B)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(5.U)
      dut.io.cpu.rdata.expect(values(2).U)
      dut.io.busy.expect(true.B)

      for (i <- 3 until values.length) {
        dut.io.mem.rready.expect(true.B)
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rlast.poke((i == values.length - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
      dut.io.mem.arlen.expect(1.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      for (i <- 0 to 1) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rlast.poke((i == 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((base + 12).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(3).U)
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

  it should "return an accepted cached hit in the request cycle" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80001000", 16)
      val values = (0 until 8).map(i => BigInt("16000000", 16) + i)

      dut.io.cpu.araddr.poke(base.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base, values)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()

      dut.io.cpu.araddr.poke((base + 12).U)
      dut.io.cpu.arid.poke(7.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(7.U)
      dut.io.cpu.rdata.expect(values(3).U)
      dut.clock.step()

      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rvalid.expect(false.B)
    }
  }

  it should "serve two cached load hits in one cycle" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base0 = BigInt("8000b000", 16)
      val base1 = base0 + 32
      val missBase = base0 + 64
      val values0 = (0 until 8).map(i => BigInt("21000000", 16) + i)
      val values1 = (0 until 8).map(i => BigInt("22000000", 16) + i)

      dut.io.cpu.araddr.poke(base0.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base0, values0)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke(base1.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base1, values1)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((base0 + 8).U)
      dut.io.cpu.arid.poke(3.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu1.araddr.poke((base1 + 12).U)
      dut.io.cpu1.arid.poke(9.U)
      dut.io.cpu1.arvalid.poke(true.B)
      dut.io.cpu1.rready.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.io.cpu1.arready.expect(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(3.U)
      dut.io.cpu.rdata.expect(values0(2).U)
      dut.io.cpu1.rvalid.expect(true.B)
      dut.io.cpu1.rid.expect(9.U)
      dut.io.cpu1.rdata.expect(values1(3).U)
      dut.clock.step()

      idleDCache(dut)
      dut.io.cpu1.araddr.poke(missBase.U)
      dut.io.cpu1.arvalid.poke(true.B)
      dut.io.cpu1.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu1.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(missBase.U)
    }
  }

  it should "accept a cached hit while the previous hit response leaves" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80000000", 16)
      val values = (0 until 8).map(i => BigInt("18000000", 16) + i)

      dut.io.cpu.araddr.poke(base.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base, values)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()

      dut.io.cpu.rready.poke(false.B)
      dut.io.cpu.araddr.poke((base + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()

      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.rdata.expect(values(1).U)
      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()

      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rready.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(2).U)
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
      dut.io.mem.araddr.expect((missBase + 4).U)

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

  it should "interleave two tagged misses at critical-word boundaries" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base0 = BigInt("80004000", 16)
      val base1 = BigInt("80005000", 16)
      val values0 = (0 until 8).map(i => BigInt("61000000", 16) + i)
      val values1 = (0 until 8).map(i => BigInt("62000000", 16) + i)

      dut.io.cpu.arid.poke(1.U)
      dut.io.cpu.araddr.poke((base0 + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base0, values0.length, criticalWord = 1)

      dut.io.cpu.arid.poke(2.U)
      dut.io.cpu.araddr.poke((base1 + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)

      for (i <- 1 until values0.length) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values0(i).U)
        dut.io.mem.rlast.poke((i == values0.length - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(1.U)
      dut.io.cpu.rdata.expect(values0(1).U)

      // A new critical segment outranks the older line's refill tail.
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base1 + 8).U)
      acceptDCacheRefillAr(dut, base1, values1.length, criticalWord = 2)
      for (i <- 2 until values1.length) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values1(i).U)
        dut.io.mem.rlast.poke((i == values1.length - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)

      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base0.U)
      dut.io.mem.rid.poke(dut.io.mem.arid.peek().litValue.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values0(0).U)
      dut.io.mem.rlast.poke(true.B)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)

      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base1.U)
      dut.io.mem.rid.poke(dut.io.mem.arid.peek().litValue.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      for (i <- 0 until 2) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values1(i).U)
        dut.io.mem.rlast.poke((i == 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)

      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.rid.expect(1.U)
      dut.clock.step()
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(2.U)
      dut.io.cpu.rdata.expect(values1(2).U)
    }
  }

  it should "merge a second request to the line being refilled" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80006000", 16)
      val values = (0 until 8).map(i => BigInt("63000000", 16) + i)

      dut.io.cpu.arid.poke(3.U)
      dut.io.cpu.araddr.poke((base + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base, values.length, criticalWord = 1)

      dut.io.cpu.arid.poke(4.U)
      dut.io.cpu.araddr.poke((base + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      sendDCacheRefillData(dut, base, values, criticalWord = 1)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(3.U)
      dut.io.cpu.rdata.expect(values(1).U)
      dut.io.mem.arvalid.expect(false.B)

      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(4.U)
      dut.io.cpu.rdata.expect(values(2).U)
      dut.io.mem.arvalid.expect(false.B)
    }
  }

  it should "merge a secondary-port request into a primary-port miss" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80006800", 16)
      val values = (0 until 8).map(i => BigInt("63800000", 16) + i)

      dut.io.cpu.arid.poke(3.U)
      dut.io.cpu.araddr.poke((base + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base, values.length, criticalWord = 1)

      dut.io.cpu1.arid.poke(9.U)
      dut.io.cpu1.araddr.poke((base + 8).U)
      dut.io.cpu1.arvalid.poke(true.B)
      dut.io.cpu1.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu1.arvalid.poke(false.B)

      sendDCacheRefillData(dut, base, values, criticalWord = 1)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(3.U)
      dut.io.cpu.rdata.expect(values(1).U)
      dut.io.cpu1.rvalid.expect(true.B)
      dut.io.cpu1.rid.expect(9.U)
      dut.io.cpu1.rdata.expect(values(2).U)
      dut.io.mem.arvalid.expect(false.B)
    }
  }

  it should "accept a third miss while merged responses are buffered" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base0 = BigInt("80007000", 16)
      val base1 = BigInt("80008000", 16)
      val values0 = (0 until 8).map(i => BigInt("64000000", 16) + i)

      dut.io.cpu.arid.poke(5.U)
      dut.io.cpu.araddr.poke((base0 + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base0, values0.length, criticalWord = 1)

      dut.io.cpu.arid.poke(6.U)
      dut.io.cpu.araddr.poke((base0 + 8).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      sendDCacheRefillData(dut, base0, values0, criticalWord = 1)

      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(5.U)
      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.arid.poke(7.U)
      dut.io.cpu.araddr.poke((base1 + 12).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()

      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(6.U)
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base1 + 12).U)
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
      dut.io.mem.araddr.expect((base + 8).U)
    }
  }

  it should "update a cached word from three ordered store ports" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("80000000", 16)
      val values = Seq(BigInt("11223344", 16)) ++
        (1 until 8).map(i => BigInt("30000000", 16) + i)

      dut.io.cpu.araddr.poke(base.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, base, values)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.store3_valid.poke(true.B)
      dut.io.store3_addr.poke(base.U)
      dut.io.store3_data.poke("h55".U)
      dut.io.store3_mask.poke(WBYTE)
      dut.io.store_valid.poke(true.B)
      dut.io.store_addr.poke((base + 1).U)
      dut.io.store_data.poke("haa".U)
      dut.io.store_mask.poke(WBYTE)
      dut.io.store2_valid.poke(true.B)
      dut.io.store2_addr.poke((base + 2).U)
      dut.io.store2_data.poke("hbb".U)
      dut.io.store2_mask.poke(WBYTE)
      dut.clock.step()
      dut.io.store3_valid.poke(false.B)
      dut.io.store_valid.poke(false.B)
      dut.io.store2_valid.poke(false.B)

      dut.io.cpu.araddr.poke(base.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect("h11bbaa55".U)
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
      dut.io.mem.araddr.expect((base + 16).U)
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

      acceptDCacheRefillAr(dut, base, values.length, criticalWord = 1)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(1).U)
      dut.io.mem.rlast.poke(false.B)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)

      dut.io.invalidate_valid.poke(true.B)
      dut.io.invalidate_addr.poke((base + 16).U)
      dut.clock.step()
      dut.io.invalidate_valid.poke(false.B)

      for (i <- 2 until values.length) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rlast.poke((i == values.length - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
      dut.io.mem.arlen.expect(0.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      dut.io.mem.rvalid.poke(true.B)
      dut.io.mem.rdata.poke(values(0).U)
      dut.io.mem.rlast.poke(true.B)
      dut.clock.step()
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
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
      dut.io.mem.araddr.expect((base + 8).U)
    }
  }

  it should "poison only the matching MSHR" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base0 = BigInt("8000c000", 16)
      val base1 = base0 + 32
      val values0 = (0 until 8).map(i => BigInt("75000000", 16) + i)
      val values1 = (0 until 8).map(i => BigInt("76000000", 16) + i)

      dut.io.cpu.arid.poke(4.U)
      dut.io.cpu.araddr.poke(base0.U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base0, values0.length, criticalWord = 0)

      dut.io.cpu1.arid.poke(10.U)
      dut.io.cpu1.araddr.poke(base1.U)
      dut.io.cpu1.arvalid.poke(true.B)
      dut.io.cpu1.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu1.arvalid.poke(false.B)

      dut.io.invalidate_valid.poke(true.B)
      dut.io.invalidate_addr.poke(base1.U)
      dut.clock.step()
      dut.io.invalidate_valid.poke(false.B)

      sendDCacheRefillData(dut, base0, values0, criticalWord = 0)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(4.U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      // The unrelated line was installed even though the second MSHR was poisoned.
      dut.io.cpu.arid.poke(5.U)
      dut.io.cpu.araddr.poke((base0 + 4).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rid.expect(5.U)
      dut.io.cpu.rdata.expect(values0(1).U)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.cpu.rready.poke(false.B)

      acceptDCacheRefillAr(dut, base1, values1.length, criticalWord = 0)
      sendDCacheRefillData(dut, base1, values1, criticalWord = 0)
      dut.io.cpu1.rvalid.expect(true.B)
      dut.io.cpu1.rid.expect(10.U)
      dut.io.cpu1.rdata.expect(values1.head.U)
      dut.io.cpu1.rready.poke(true.B)
      dut.io.cpu1.arid.poke(11.U)
      dut.io.cpu1.araddr.poke((base1 + 4).U)
      dut.io.cpu1.arvalid.poke(true.B)
      dut.io.cpu1.arready.expect(true.B)
      dut.clock.step()
      dut.io.cpu1.rready.poke(false.B)
      dut.io.cpu1.arvalid.poke(false.B)

      // Consuming the old waiter must not merge the new request into the
      // completed poisoned entry; the line was never installed.
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base1 + 4).U)
    }
  }

  it should "not install stale refill data after a committed store" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val base = BigInt("8000a000", 16)
      val values = (0 until 8).map(i => BigInt("72000000", 16) + i)

      dut.io.cpu.araddr.poke((base + 24).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, base, values.length, criticalWord = 6)

      dut.io.store_valid.poke(true.B)
      dut.io.store_addr.poke((base + 24).U)
      dut.io.store_data.poke("hff".U)
      dut.io.store_mask.poke(WWORD)
      for (i <- 6 until values.length) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rlast.poke((i == values.length - 1).B)
        dut.clock.step()
        dut.io.store_valid.poke(false.B)
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(base.U)
      dut.io.mem.arlen.expect(5.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      for (i <- 0 until 6) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(values(i).U)
        dut.io.mem.rlast.poke((i == 5).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(values(6).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((base + 24).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect((base + 24).U)
    }
  }

  it should "not mix an old-tag store hit into a same-set refill" in {
    simulate(new DCache(conf = new core.CoreConfig(32))) { dut =>
      idleDCache(dut)
      resetDut(dut.clock, dut.reset)
      idleDCache(dut)
      val oldBase = BigInt("8000a000", 16)
      val newBase = oldBase + BigInt("800", 16)
      val oldValues = (0 until 8).map(i => BigInt("73000000", 16) + i)
      val newValues = (0 until 8).map(i => BigInt("74000000", 16) + i)

      dut.io.cpu.araddr.poke((oldBase + 24).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      fillDCacheLine(dut, oldBase, oldValues)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((newBase + 24).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.clock.step()
      dut.io.cpu.arvalid.poke(false.B)
      acceptDCacheRefillAr(dut, newBase, newValues.length, criticalWord = 6)
      for (i <- 6 until newValues.length) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(newValues(i).U)
        dut.io.mem.rlast.poke((i == newValues.length - 1).B)
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.mem.arvalid.expect(true.B)
      dut.io.mem.araddr.expect(newBase.U)
      dut.io.mem.arlen.expect(5.U)
      dut.io.mem.arready.poke(true.B)
      dut.clock.step()
      dut.io.mem.arready.poke(false.B)
      for (i <- 0 until 6) {
        dut.io.mem.rvalid.poke(true.B)
        dut.io.mem.rdata.poke(newValues(i).U)
        dut.io.mem.rlast.poke((i == 5).B)
        if (i == 5) {
          dut.io.store_valid.poke(true.B)
          dut.io.store_addr.poke((oldBase + 24).U)
          dut.io.store_data.poke("h118".U)
          dut.io.store_mask.poke(WWORD)
        }
        dut.clock.step()
      }
      dut.io.mem.rvalid.poke(false.B)
      dut.io.mem.rlast.poke(false.B)
      dut.io.store_valid.poke(false.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(newValues(6).U)
      dut.io.cpu.rready.poke(true.B)
      dut.clock.step()
      dut.io.cpu.rready.poke(false.B)

      dut.io.cpu.araddr.poke((newBase + 24).U)
      dut.io.cpu.arvalid.poke(true.B)
      dut.io.cpu.rready.poke(true.B)
      dut.io.cpu.arready.expect(true.B)
      dut.io.cpu.rvalid.expect(true.B)
      dut.io.cpu.rdata.expect(newValues(6).U)
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
    dut.io.in.araddr.poke(base.U)
    dut.io.in.arvalid.poke(true.B)
    dut.io.in.arready.expect(true.B)
    dut.clock.step()
    dut.io.in.arvalid.poke(false.B)

    dut.io.out.arvalid.expect(true.B)
    dut.io.out.araddr.expect(base.U)
    dut.io.out.arlen.expect((words.length - 1).U)
    dut.io.out.arburst.expect("b01".U)
    dut.io.out.arready.poke(true.B)
    dut.clock.step()
    dut.io.out.arready.poke(false.B)

    for (i <- words.indices) {
      dut.io.out.rvalid.poke(true.B)
      dut.io.out.rdata.poke(words(i).U)
      dut.io.out.rresp.poke(0.U)
      dut.io.out.rlast.poke((i == words.length - 1).B)
      dut.io.out.rid.poke(0.U)
      dut.clock.step()
      dut.io.out.rvalid.poke(false.B)
    }
  }

  it should "return four cached words across a line and hold them under backpressure" in {
    simulate(new ICache(set = 4, way = 1, block_size = 16,
      conf = new core.CoreConfig(32))) { dut =>
      idleICache(dut)
      resetDut(dut.clock, dut.reset)
      idleICache(dut)
      val base = BigInt("80000000", 16)
      val line0 = Seq(BigInt("11111111", 16), BigInt("22222222", 16),
        BigInt("33333333", 16), BigInt("44444444", 16))
      val line1 = Seq(BigInt("55555555", 16), BigInt("66666666", 16),
        BigInt("77777777", 16), BigInt("88888888", 16))
      fillICacheLine(dut, base, line0)
      fillICacheLine(dut, base + 16, line1)

      dut.io.in.rready.poke(false.B)
      dut.io.in.araddr.poke((base + 12).U)
      dut.io.in.arvalid.poke(true.B)
      dut.io.in.arready.expect(true.B)
      dut.io.in.rvalid.expect(true.B)
      dut.io.in.rvalid1.expect(true.B)
      dut.io.in.rvalid2.expect(true.B)
      dut.io.in.rvalid3.expect(true.B)
      dut.io.in.rdata.expect(line0(3).U)
      dut.io.in.rdata1.expect(line1(0).U)
      dut.io.in.rdata2.expect(line1(1).U)
      dut.io.in.rdata3.expect(line1(2).U)
      dut.clock.step()
      dut.io.in.arvalid.poke(false.B)

      for (_ <- 0 until 2) {
        dut.io.in.rvalid.expect(true.B)
        dut.io.in.rvalid1.expect(true.B)
        dut.io.in.rvalid2.expect(true.B)
        dut.io.in.rvalid3.expect(true.B)
        dut.io.in.rdata.expect(line0(3).U)
        dut.io.in.rdata1.expect(line1(0).U)
        dut.io.in.rdata2.expect(line1(1).U)
        dut.io.in.rdata3.expect(line1(2).U)
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
    dut.io.ld1_valid.poke(false.B)
    dut.io.ld1_addr.poke(0.U)
    dut.io.ld1_mem_rd.poke(RWORD)
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

  it should "launch the first AXI write in the burst-start cycle" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000010".U)
      dut.io.enq.bits.data.poke("h12345678".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.io.write_burst.expect(true.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.awaddr.expect("h80000010".U)
      dut.io.dmem.awlen.expect(0.U)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.wdata.expect("h12345678".U)
      dut.io.dmem.wstrb.expect("b1111".U)
      dut.io.dmem.wlast.expect(true.B)

      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.clock.step()
      dut.io.dmem.bready.expect(true.B)
    }
  }

  it should "keep a same-word enqueue younger than a launching burst" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000010".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()

      dut.io.enq.bits.data.poke("h22222222".U)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.write_burst.expect(true.B)
      dut.io.merged.expect(0.U)
      dut.io.dmem.wdata.expect("h11111111".U)
      dut.clock.step()
      dut.io.count.expect(2.U)

      dut.io.enq.valid.poke(false.B)
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.deq_count.expect(1.U)
      dut.clock.step()

      dut.io.dmem.bvalid.poke(false.B)
      dut.io.count.expect(1.U)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.wdata.expect("h22222222".U)
    }
  }

  it should "handoff a completed burst directly to the next buffered line" in {
    simulate(new StoreBuffer(size = 8)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq1.bits.addr.poke("h80000004".U)
      dut.io.enq1.bits.data.poke("h22222222".U)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000020".U)
      dut.io.enq.bits.data.poke("h33333333".U)
      dut.io.enq1.bits.addr.poke("h80000024".U)
      dut.io.enq1.bits.data.poke("h44444444".U)
      dut.clock.step()

      idleStoreBuffer(dut)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.awlen.expect(1.U)
      dut.clock.step()
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.wdata.expect("h22222222".U)
      dut.io.dmem.wlast.expect(true.B)
      dut.clock.step()

      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.dmem.awready.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000020".U)
      dut.io.enq.bits.data.poke("h55555555".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.deq_count.expect(2.U)
      dut.io.merged.expect(0.U)
      dut.io.write_burst.expect(true.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.awaddr.expect("h80000020".U)
      dut.io.dmem.awlen.expect(1.U)
      dut.io.dmem.wvalid.expect(false.B)
      dut.clock.step()

      dut.io.dmem.bvalid.poke(false.B)
      dut.io.dmem.awready.poke(false.B)
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(3.U)
      dut.io.dmem.awvalid.expect(false.B)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.wdata.expect("h33333333".U)
    }
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
      dut.io.dmem.awlen.expect(0.U)
      dut.io.dmem.wvalid.expect(true.B)
      dut.io.dmem.wlast.expect(true.B)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.drain_valid.expect(true.B)
      dut.io.drain_addr.expect("h80000000".U)
      dut.io.drain_data.expect("h12345678".U)
      dut.io.drain_mask.expect("b1111".U)
      dut.clock.step()

      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.dmem.bready.expect(true.B)
      dut.io.deq_valid.expect(true.B)
      dut.io.deq_count.expect(1.U)
      dut.clock.step()

      dut.io.dmem.bvalid.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.count.expect(0.U)
    }
  }

  it should "drain adjacent words in one ordered AXI burst" in {
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
      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.awlen.expect(1.U)
      dut.io.dmem.wdata.expect("h11111111".U)
      dut.io.dmem.wlast.expect(false.B)
      dut.io.drain_valid.expect(true.B)
      dut.io.drain_addr.expect("h80000000".U)
      dut.io.drain_data.expect("h11111111".U)
      dut.clock.step()
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wdata.expect("h22222222".U)
      dut.io.dmem.wlast.expect(true.B)
      dut.io.drain_valid.expect(true.B)
      dut.io.drain_addr.expect("h80000004".U)
      dut.io.drain_data.expect("h22222222".U)
      dut.clock.step()
      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.dmem.bready.expect(true.B)
      dut.io.deq_valid.expect(true.B)
      dut.io.deq_count.expect(2.U)
      dut.clock.step()
      dut.io.dmem.bvalid.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.count.expect(0.U)
    }
  }

  it should "coalesce a non-monotonic same-line prefix with masked burst holes" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h8000000c".U)
      dut.io.enq.bits.data.poke("h33333333".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.addr.poke("h80000000".U)
      dut.io.enq1.bits.data.poke("h00000000".U)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.clock.step()
      idleStoreBuffer(dut)
      dut.clock.step()

      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.awlen.expect(3.U)
      dut.io.dmem.wdata.expect("h00000000".U)
      dut.io.dmem.wstrb.expect("b1111".U)
      dut.clock.step()
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wstrb.expect(0.U)
      dut.clock.step()
      dut.io.dmem.wstrb.expect(0.U)
      dut.clock.step()
      dut.io.dmem.wdata.expect("h33333333".U)
      dut.io.dmem.wstrb.expect("b1111".U)
      dut.io.dmem.wlast.expect(true.B)
      dut.clock.step()

      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.deq_valid.expect(true.B)
      dut.io.deq_count.expect(2.U)
      dut.clock.step()
      dut.io.dmem.bvalid.poke(false.B)
      dut.io.empty.expect(true.B)
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
      dut.io.merged.expect(1.U)
      dut.clock.step()
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.count.expect(1.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_fwd_valid.expect(true.B)
      dut.io.ld_fwd_data.expect("h22222222".U)
    }
  }

  it should "merge partial stores byte-wise for forwarding and drain" in {
    simulate(new StoreBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("haa".U)
      dut.io.enq.bits.mask.poke(WBYTE)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000001".U)
      dut.io.enq.bits.data.poke("hbb".U)
      dut.io.enq.bits.mask.poke(WBYTE)
      dut.io.merged.expect(1.U)
      dut.clock.step()

      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.count.expect(1.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RHALFU)
      dut.io.ld_wait.expect(false.B)
      dut.io.ld_fwd_valid.expect(true.B)
      dut.io.ld_fwd_data.expect("hbbaa".U)

      dut.io.ld_valid.poke(false.B)
      dut.io.bus_busy.poke(false.B)
      dut.clock.step()
      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.awsize.expect(2.U)
      dut.io.dmem.wstrb.expect("b0011".U)
      dut.io.dmem.wdata.expect("h0000bbaa".U)
    }
  }

  it should "merge the youngest matching word across unrelated buffered stores" in {
    simulate(new StoreBuffer(size = 8)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000004".U)
      dut.io.enq.bits.data.poke("h22222222".U)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h33333333".U)
      dut.io.merged.expect(1.U)
      dut.clock.step()

      idleStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.count.expect(2.U)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_fwd_valid.expect(true.B)
      dut.io.ld_fwd_data.expect("h33333333".U)
    }
  }

  it should "gather a short contiguous stream before opening an AXI burst" in {
    simulate(new StoreBuffer(size = 8, gatherCycles = 3)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleStoreBuffer(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h12345678".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.write_burst.expect(false.B)
      dut.clock.step(3)
      dut.io.write_burst.expect(true.B)
      dut.clock.step()
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.awlen.expect(0.U)
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
      dut.io.enq.bits.addr.poke("h80000200".U)
      dut.io.enq.bits.data.poke("h44".U)
      dut.io.enq1.valid.poke(true.B)
      dut.io.enq1.bits.addr.poke("h80000204".U)
      dut.io.enq1.bits.data.poke("h55".U)
      dut.io.enq1.bits.mask.poke(WWORD)
      dut.io.enq.ready.expect(false.B)
      dut.io.enq1.ready.expect(false.B)
      dut.clock.step()
      dut.io.count.expect((OoOParams.STORE_BUFFER_SIZE - 1).U)
    }
  }

  behavior of "WriteCombiningStoreBuffer"

  def idleWriteCombiningStoreBuffer(dut: WriteCombiningStoreBuffer): Unit = {
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
    dut.io.ld1_valid.poke(false.B)
    dut.io.ld1_addr.poke(0.U)
    dut.io.ld1_mem_rd.poke(RWORD)
    dut.io.bus_busy.poke(false.B)
    dut.io.drain_all.poke(false.B)
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

  it should "combine interleaved stores by line and chain their writebacks" in {
    simulate(new WriteCombiningStoreBuffer(lines = 4, retentionCycles = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000020".U)
      dut.io.enq.bits.data.poke("h22222222".U)
      dut.clock.step()
      dut.io.enq.bits.addr.poke("h80000004".U)
      dut.io.enq.bits.data.poke("h33333333".U)
      dut.io.merged.expect(1.U)
      dut.clock.step()

      idleWriteCombiningStoreBuffer(dut)
      dut.io.count.expect(2.U)
      dut.io.drain_all.poke(true.B)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.awaddr.expect("h80000020".U)
      dut.io.dmem.awlen.expect(0.U)
      dut.io.dmem.wdata.expect("h22222222".U)
      dut.clock.step()

      dut.io.dmem.wready.poke(false.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.write_chain.expect(true.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.awaddr.expect("h80000000".U)
      dut.io.dmem.awlen.expect(1.U)
      dut.io.dmem.wvalid.expect(false.B)
      dut.clock.step()

      dut.io.dmem.bvalid.poke(false.B)
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.dmem.wdata.expect("h11111111".U)
      dut.io.dmem.wlast.expect(false.B)
      dut.clock.step()
      dut.io.dmem.wdata.expect("h33333333".U)
      dut.io.dmem.wlast.expect(true.B)
    }
  }

  it should "keep a launching line immutable against a younger same-line store" in {
    simulate(new WriteCombiningStoreBuffer(lines = 4, retentionCycles = 1)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000000".U)
      dut.io.enq.bits.data.poke("h11111111".U)
      dut.io.enq.bits.mask.poke(WWORD)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.clock.step()

      dut.io.bus_busy.poke(false.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000004".U)
      dut.io.enq.bits.data.poke("h22222222".U)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.wready.poke(true.B)
      dut.io.write_burst.expect(true.B)
      dut.io.merged.expect(0.U)
      dut.io.dmem.wdata.expect("h11111111".U)
      dut.clock.step()

      dut.io.enq.valid.poke(false.B)
      dut.io.dmem.awready.poke(false.B)
      dut.io.dmem.wready.poke(false.B)
      dut.io.count.expect(2.U)
      dut.io.drain_all.poke(true.B)
      dut.io.dmem.bvalid.poke(true.B)
      dut.io.dmem.awready.poke(true.B)
      dut.io.dmem.awvalid.expect(true.B)
      dut.io.dmem.awaddr.expect("h80000004".U)
      dut.io.dmem.awlen.expect(0.U)
      dut.clock.step()
    }
  }

  it should "return partial bytes without blocking a cache lookup" in {
    simulate(new WriteCombiningStoreBuffer(lines = 4, retentionCycles = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h80000001".U)
      dut.io.enq.bits.data.poke("haa".U)
      dut.io.enq.bits.mask.poke(WBYTE)
      dut.clock.step()

      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h80000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_wait.expect(false.B)
      dut.io.ld_fwd_valid.expect(false.B)
      dut.io.ld_partial_valid.expect(true.B)
      dut.io.ld_partial_data.expect("h0000aa00".U)
      dut.io.ld_partial_mask.expect("b0010".U)
    }
  }

  it should "keep a partial MMIO overlap blocked" in {
    simulate(new WriteCombiningStoreBuffer(lines = 4, retentionCycles = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.addr.poke("h10000001".U)
      dut.io.enq.bits.data.poke("h55".U)
      dut.io.enq.bits.mask.poke(WBYTE)
      dut.clock.step()

      idleWriteCombiningStoreBuffer(dut)
      dut.io.bus_busy.poke(true.B)
      dut.io.ld_valid.poke(true.B)
      dut.io.ld_addr.poke("h10000000".U)
      dut.io.ld_mem_rd.poke(RWORD)
      dut.io.ld_wait.expect(true.B)
      dut.io.ld_fwd_valid.expect(false.B)
      dut.io.ld_partial_valid.expect(false.B)
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
    dut.io.replaceReady.poke(false.B)
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

  it should "flow an empty-buffer packet directly to a ready consumer" in {
    simulate(new FetchBuffer()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFetchBuffer(dut)
      dut.io.out.ready.poke(true.B)
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.valid(0).poke(true.B)
      dut.io.in.bits.bits(0).pc.poke("h80000040".U)
      dut.io.in.ready.expect(true.B)
      dut.io.out.valid.expect(true.B)
      dut.io.out.bits.bits(0).pc.expect("h80000040".U)
      dut.clock.step()
      idleFetchBuffer(dut)
      dut.io.empty.expect(true.B)
      dut.io.count.expect(0.U)
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

  it should "replace a full buffer using registered downstream credit" in {
    simulate(new FetchBuffer(n = 2)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFetchBuffer(dut)
      for (pc <- Seq(BigInt("80000000", 16), BigInt("80000008", 16))) {
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.valid(0).poke(true.B)
        dut.io.in.bits.bits(0).pc.poke(pc.U)
        dut.clock.step()
      }
      dut.io.full.expect(true.B)
      dut.io.in.bits.bits(0).pc.poke("h80000010".U)
      dut.io.out.ready.poke(true.B)
      dut.io.replaceReady.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.io.out.bits.bits(0).pc.expect("h80000000".U)
      dut.clock.step()
      dut.io.count.expect(2.U)
      dut.io.out.bits.bits(0).pc.expect("h80000008".U)
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
    dut.io.alloc.bits.pathHistory.poke(0.U)
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
    dut.io.commit2Valid.poke(false.B)
    dut.io.commit2Idx.poke(0.U)
    dut.io.commit2Generation.poke(0.U)
    dut.io.commit3Valid.poke(false.B)
    dut.io.commit3Idx.poke(0.U)
    dut.io.commit3Generation.poke(0.U)
    dut.io.recoverIdx.poke(0.U)
    dut.io.recoverGeneration.poke(0.U)
    dut.io.recoverFlush.poke(false.B)
    dut.io.recoverSlot.poke(0.U)
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
      dut.io.recoverSlot.poke(0.U)
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

  it should "preserve all four slots when recovering the last slot" in {
    simulate(new FTQ()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFtq(dut)

      val idx = dut.io.allocIdx.peek().litValue
      val generation = dut.io.allocGeneration.peek().litValue
      dut.io.alloc.valid.poke(true.B)
      dut.io.alloc.bits.basePc.poke("h80000080".U)
      dut.io.alloc.bits.validMask.poke("hf".U)
      dut.clock.step()

      idleFtq(dut)
      dut.io.recoverIdx.poke(idx.U)
      dut.io.recoverGeneration.poke(generation.U)
      dut.io.recoverSlot.poke(3.U)
      dut.io.recoverFlush.poke(true.B)
      dut.io.recoverValid.expect(true.B)
      dut.clock.step()

      idleFtq(dut)
      dut.io.count.expect(1.U)
      dut.io.recoverIdx.poke(idx.U)
      dut.io.recoverGeneration.poke(generation.U)
      dut.io.recoverValid.expect(true.B)
      dut.io.recover.validMask.expect("hf".U)

      dut.io.commit0Valid.poke(true.B)
      dut.io.commit0Idx.poke(idx.U)
      dut.io.commit0Generation.poke(generation.U)
      dut.io.commit1Valid.poke(true.B)
      dut.io.commit1Idx.poke(idx.U)
      dut.io.commit1Generation.poke(generation.U)
      dut.io.commit2Valid.poke(true.B)
      dut.io.commit2Idx.poke(idx.U)
      dut.io.commit2Generation.poke(generation.U)
      dut.io.commit3Valid.poke(true.B)
      dut.io.commit3Idx.poke(idx.U)
      dut.io.commit3Generation.poke(generation.U)
      dut.clock.step()
      dut.io.count.expect(0.U)
      dut.io.recoverValid.expect(false.B)
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
    dut.io.deq2.ready.poke(false.B)
    dut.io.deq3.ready.poke(false.B)
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

  it should "dequeue a four-instruction prefix in one cycle" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      for (i <- 0 until OoOParams.FETCH_WIDTH) {
        dut.io.enq.bits.valid(i).poke(true.B)
        dut.io.enq.bits.bits(i).pc.poke((0x80000000L + i * 4).U)
        dut.io.enq.bits.bits(i).inst.poke((0x00100013L + i).U)
      }
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      dut.io.count.expect(OoOParams.FETCH_WIDTH.U)
      val deq = Seq(dut.io.deq, dut.io.deq1, dut.io.deq2, dut.io.deq3)
      for (i <- deq.indices) {
        deq(i).valid.expect(true.B)
        deq(i).bits.pc.expect((0x80000000L + i * 4).U)
        deq(i).ready.poke(true.B)
      }
      dut.clock.step()
      dut.io.count.expect(0.U)
      dut.io.empty.expect(true.B)
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

  it should "use same-cycle dequeue credit for a full pop and push" in {
    simulate(new FetchQueue()) { dut =>
      resetDut(dut.clock, dut.reset)
      idleFq(dut)
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.valid(0).poke(true.B)
      dut.io.enq.bits.valid(1).poke(true.B)
      for (i <- 0 until (OoOParams.FQ_SIZE / 2)) {
        dut.io.enq.bits.bits(0).pc.poke((0x80000000L + i * 8).U)
        dut.io.enq.bits.bits(1).pc.poke((0x80000004L + i * 8).U)
        dut.clock.step()
      }
      dut.io.full.expect(true.B)
      dut.io.enqSpace.expect(0.U)

      dut.io.deq.ready.poke(true.B)
      dut.io.deq1.ready.poke(true.B)
      dut.io.enq.bits.bits(0).pc.poke("h80000100".U)
      dut.io.enq.bits.bits(1).pc.poke("h80000104".U)
      dut.io.enqSpace.expect(2.U)
      dut.io.enq.ready.expect(true.B)
      dut.clock.step()

      dut.io.count.expect(OoOParams.FQ_SIZE.U)
      dut.io.enq.valid.poke(false.B)
      for (_ <- 0 until ((OoOParams.FQ_SIZE - 2) / 2)) {
        dut.clock.step()
      }
      dut.io.deq.bits.pc.expect("h80000100".U)
      dut.io.deq1.bits.pc.expect("h80000104".U)
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
    dut.io.predict_pc2.poke("h80000008".U)
    dut.io.predict_inst2.poke("h00000013".U)
    dut.io.predict_pc3.poke("h8000000c".U)
    dut.io.predict_inst3.poke("h00000013".U)
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
    dut.io.recover_target.poke(0.U)
    dut.io.recover_taken.poke(false.B)
    dut.io.recover_is_branch.poke(false.B)
    dut.io.recover_is_jalr.poke(false.B)
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
      dut.io.predict_inst.poke(jalrX5)
      dut.io.bp_valid.expect(true.B)
      dut.io.bp_target.expect("h80001000".U)

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

  it should "separate indirect targets with older target-path context" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      val pc = BigInt("80000100", 16)
      val targetA = BigInt("80001000", 16)
      val targetB = BigInt("80002000", 16)
      val pathA = BigInt("10aabbcc", 16)
      val pathB = BigInt("20aabbcc", 16)
      val providerLo = common.BPU_Config.GHR_LENGTH +
        common.BPU_Config.PATH_HISTORY_LENGTH + common.BPU_Config.LOOP_ITER_BITS +
        1 + common.BPU_Config.TAGE_PROVIDER_BITS
      val rank3 = BigInt(3) << providerLo
      val metaA = (pathA << common.BPU_Config.GHR_LENGTH) | rank3
      val metaB = (pathB << common.BPU_Config.GHR_LENGTH) | rank3
      val jalrX5 = "h00028067".U

      def train(meta: BigInt, target: BigInt): Unit = {
        idleBpu(dut)
        dut.io.update_valid.poke(true.B)
        dut.io.update_is_jalr.poke(true.B)
        dut.io.update_pc.poke(pc.U)
        dut.io.update_target.poke(target.U)
        dut.io.update_index.poke(meta.U)
        dut.io.itage_alloc.expect(true.B)
        dut.clock.step()
      }

      def selectPath(meta: BigInt): Unit = {
        idleBpu(dut)
        dut.io.recover_valid.poke(true.B)
        dut.io.recover_index.poke(meta.U)
        dut.clock.step()
        idleBpu(dut)
        dut.io.predict_pc.poke(pc.U)
        dut.io.predict_inst.poke(jalrX5)
      }

      train(metaA, targetA)
      train(metaB, targetB)
      selectPath(metaA)
      dut.io.bp_itage_hit.expect(true.B)
      dut.io.bp_target.expect(targetA.U)
      selectPath(metaB)
      dut.io.bp_itage_hit.expect(true.B)
      dut.io.bp_target.expect(targetB.U)
    }
  }

  it should "select bimodal after a PC-stable branch beats the fetched history provider" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      idleBpu(dut)
      val backwardBeq = "hfe000ee3".U
      dut.io.predict_inst.poke(backwardBeq)
      val indexA = dut.io.bp_index.peek().litValue

      dut.io.update_valid.poke(true.B)
      dut.io.update_is_branch.poke(true.B)
      dut.io.update_taken.poke(true.B)
      dut.io.update_pc.poke("h80000000".U)
      dut.io.update_index.poke(indexA.U)
      dut.clock.step()

      val localPredBit = common.BPU_Config.GHR_LENGTH +
        common.BPU_Config.PATH_HISTORY_LENGTH + common.BPU_Config.LOOP_ITER_BITS + 1 +
        common.BPU_Config.TAGE_PROVIDER_BITS + common.BPU_Config.ITAGE_PROVIDER_BITS +
        common.BPU_Config.LOCAL_HISTORY_BITS
      val primaryPredBit = localPredBit + 1
      val providerDisagreementMeta = indexA &
        ~(BigInt(1) << localPredBit) & ~(BigInt(1) << primaryPredBit)

      idleBpu(dut)
      dut.io.update_valid.poke(true.B)
      dut.io.update_is_branch.poke(true.B)
      dut.io.update_taken.poke(true.B)
      dut.io.update_pc.poke("h80000000".U)
      // Isolate chooser ownership: the fetched history/local providers said
      // not-taken while the already-trained PC-only predictor said taken.
      dut.io.update_index.poke(providerDisagreementMeta.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_bimodal_selected.expect(true.B)
      dut.io.bp_taken.expect(true.B)
    }
  }

  it should "learn a stable loop trip count and recover its speculative iteration" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      val pc = BigInt("80000100", 16)
      val target = pc - 4
      val backwardBeq = "hfe000ee3".U

      def commitOutcome(taken: Boolean): Unit = {
        idleBpu(dut)
        dut.io.update_valid.poke(true.B)
        dut.io.update_is_branch.poke(true.B)
        dut.io.update_taken.poke(taken.B)
        dut.io.update_pc.poke(pc.U)
        dut.io.update_target.poke((if (taken) target else pc + 4).U)
        dut.clock.step()
        idleBpu(dut)
        dut.io.reset_spec.poke(true.B)
        dut.clock.step()
      }

      for (_ <- 0 until 3) {
        commitOutcome(taken = true)
        commitOutcome(taken = true)
        commitOutcome(taken = false)
      }

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_loop_hit.expect(true.B)
      dut.io.bp_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(1.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_taken.expect(true.B)

      idleBpu(dut)
      dut.io.reset_spec.poke(true.B)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc2.poke(pc.U)
      dut.io.predict_inst2.poke(backwardBeq)
      dut.io.bp2_loop_hit.expect(true.B)
      dut.io.bp2_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(4.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc2.poke(pc.U)
      dut.io.predict_inst2.poke(backwardBeq)
      dut.io.bp2_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(4.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc2.poke(pc.U)
      dut.io.predict_inst2.poke(backwardBeq)
      dut.io.bp2_loop_hit.expect(true.B)
      dut.io.bp2_taken.expect(false.B)

      idleBpu(dut)
      dut.io.reset_spec.poke(true.B)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc3.poke(pc.U)
      dut.io.predict_inst3.poke(backwardBeq)
      dut.io.bp3_loop_hit.expect(true.B)
      dut.io.bp3_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(8.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_loop_hit.expect(true.B)
      dut.io.bp_taken.expect(true.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(1.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      val exitMeta = dut.io.bp_index.peek().litValue
      dut.io.bp_loop_hit.expect(true.B)
      dut.io.bp_taken.expect(false.B)
      dut.io.spec_advance_valid.poke(true.B)
      dut.io.spec_advance_mask.poke(1.U)
      dut.clock.step()

      idleBpu(dut)
      dut.io.recover_valid.poke(true.B)
      dut.io.recover_pc.poke(pc.U)
      dut.io.recover_target.poke(target.U)
      dut.io.recover_index.poke(exitMeta.U)
      dut.io.recover_taken.poke(true.B)
      dut.io.recover_is_branch.poke(true.B)
      dut.clock.step()

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_taken.expect(true.B)
    }
  }

  it should "select committed local history for a repeating per-PC pattern" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      val pc = BigInt("80000200", 16)
      val backwardBeq = "hfe000ee3".U
      val pattern = Seq(true, true, false, true, false, false)

      for (_ <- 0 until 16; taken <- pattern) {
        idleBpu(dut)
        dut.io.predict_pc.poke(pc.U)
        dut.io.predict_inst.poke(backwardBeq)
        val meta = dut.io.bp_index.peek().litValue
        dut.io.update_valid.poke(true.B)
        dut.io.update_is_branch.poke(true.B)
        dut.io.update_taken.poke(taken.B)
        dut.io.update_pc.poke(pc.U)
        dut.io.update_target.poke((if (taken) pc - 4 else pc + 4).U)
        dut.io.update_index.poke(meta.U)
        dut.clock.step()
      }

      idleBpu(dut)
      dut.io.predict_pc.poke(pc.U)
      dut.io.predict_inst.poke(backwardBeq)
      dut.io.bp_local_selected.expect(true.B)
      dut.io.bp_taken.expect(true.B)
    }
  }

  it should "use the statistical corrector to separate aliased lite-lane branches" in {
    simulate(new BPU(conf, bhtSize = 16, indirectSize = 16,
      tageTableSize = 16, itageTableSize = 16)) { dut =>
      resetDut(dut.clock, dut.reset)
      val takenPc = BigInt("80001000", 16)
      val notTakenPc = takenPc + 0x40
      val forwardBeq = "h00000463".U

      for (_ <- 0 until 12; (pc, taken) <- Seq(
        (takenPc, true), (notTakenPc, false))) {
        idleBpu(dut)
        dut.io.predict_pc2.poke(pc.U)
        dut.io.predict_inst2.poke(forwardBeq)
        val meta = dut.io.bp2_index.peek().litValue
        dut.io.update_valid.poke(true.B)
        dut.io.update_is_branch.poke(true.B)
        dut.io.update_taken.poke(taken.B)
        dut.io.update_pc.poke(pc.U)
        dut.io.update_target.poke((if (taken) pc + 8 else pc + 4).U)
        dut.io.update_index.poke(meta.U)
        dut.clock.step()
      }

      idleBpu(dut)
      dut.io.predict_pc2.poke(takenPc.U)
      dut.io.predict_inst2.poke(forwardBeq)
      dut.io.bp2_sc_selected.expect(true.B)
      dut.io.bp2_taken.expect(true.B)
    }
  }
}
