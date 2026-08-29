import chisel3._
import chisel3.util._
import core._
import bus._
import sim._

class NPC_IO(conf: CoreConfig) extends Bundle {
  val interrupt = Input(Bool())
  val master  = if (conf.ysyxsoc) Some(new AXI4Master) else None
  val slave   = if (conf.ysyxsoc) Some(new AXI4Slave)  else None
  val ebreak  = if (!conf.useDPIC) Some(Output(Bool())) else None
  // 仿真探针（NPC）：保证 Verilator 可见
  val commit_valid    = if (conf.npc) Some(Output(Bool())) else None
  val commit_pc       = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_mem_addr = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_is_load  = if (conf.npc) Some(Output(Bool())) else None
  val commit_valid1    = if (conf.npc) Some(Output(Bool())) else None
  val commit_pc1       = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_mem_addr1 = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_is_load1  = if (conf.npc) Some(Output(Bool())) else None
  val commit_valid2    = if (conf.npc) Some(Output(Bool())) else None
  val commit_pc2       = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_mem_addr2 = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_is_load2  = if (conf.npc) Some(Output(Bool())) else None
  val commit_valid3    = if (conf.npc) Some(Output(Bool())) else None
  val commit_pc3       = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_mem_addr3 = if (conf.npc) Some(Output(UInt(32.W))) else None
  val commit_is_load3  = if (conf.npc) Some(Output(Bool())) else None
  val arch_rdata      = if (conf.npc) Some(Output(Vec(32, UInt(32.W)))) else None
  val debug_lq_head_alloc_pc = if (conf.npc) Some(Output(UInt(32.W))) else None
  val debug_lq_head_remove_reason = if (conf.npc) Some(Output(UInt(2.W))) else None
  val debug_wb_head_reject_flags = if (conf.npc) Some(Output(UInt(4.W))) else None
  val debug_rob_head_pc = if (conf.npc) Some(Output(UInt(32.W))) else None
  val debug_rob_head_valid = if (conf.npc) Some(Output(Bool())) else None
  val debug_rob_head_done = if (conf.npc) Some(Output(Bool())) else None
  val debug_rob_head_mem = if (conf.npc) Some(Output(Bool())) else None
  val debug_rob_head_ctrl = if (conf.npc) Some(Output(Bool())) else None
  val debug_rob_count = if (conf.npc) Some(Output(UInt(log2Ceil(common.OoOParams.ROB_SIZE + 1).W))) else None
  val debug_rs_count = if (conf.npc) Some(Output(UInt(log2Ceil(common.OoOParams.WIDE_RS_SIZE + 1).W))) else None
  val debug_brq_count = if (conf.npc) Some(Output(UInt(log2Ceil(common.OoOParams.BRQ_SIZE + 1).W))) else None
  val debug_fq_count = if (conf.npc) Some(Output(UInt(log2Ceil(common.OoOParams.FQ_SIZE + 1).W))) else None
  val debug_brq_issue_valid = if (conf.npc) Some(Output(Bool())) else None
  val debug_bru_dispatch_valid = if (conf.npc) Some(Output(Bool())) else None
}

class ysyx_25020039(val coreConfig: CoreConfig) extends Module {
  val io = IO(new NPC_IO(coreConfig))
  if (coreConfig.ysyxsoc) {
    io.master.get.setDefaults()
    io.slave.get.setDefaults()
  }

  val core = Module(new Core(coreConfig))
  val xbar = Module(new Xbar(coreConfig))
  val clint = Module(new Clint(coreConfig))
  val uart = if (coreConfig.npc) Some(Module(new UART)) else None

  if (!coreConfig.useDPIC) io.ebreak.get := core.io.ebreak.get
  core.io.interrupt := io.interrupt

  if (coreConfig.npc) {
    io.commit_valid.get    := core.io.commit_valid
    io.commit_pc.get       := core.io.commit_pc
    io.commit_mem_addr.get := core.io.commit_mem_addr
    io.commit_is_load.get  := core.io.commit_is_load
    io.commit_valid1.get    := core.io.commit_valid1
    io.commit_pc1.get       := core.io.commit_pc1
    io.commit_mem_addr1.get := core.io.commit_mem_addr1
    io.commit_is_load1.get  := core.io.commit_is_load1
    io.commit_valid2.get    := core.io.commit_valid2
    io.commit_pc2.get       := core.io.commit_pc2
    io.commit_mem_addr2.get := core.io.commit_mem_addr2
    io.commit_is_load2.get  := core.io.commit_is_load2
    io.commit_valid3.get    := core.io.commit_valid3
    io.commit_pc3.get       := core.io.commit_pc3
    io.commit_mem_addr3.get := core.io.commit_mem_addr3
    io.commit_is_load3.get  := core.io.commit_is_load3
    io.arch_rdata.get      := core.io.arch_rdata
    io.debug_lq_head_alloc_pc.get := core.io.debug_lq_head_alloc_pc
    io.debug_lq_head_remove_reason.get := core.io.debug_lq_head_remove_reason
    io.debug_wb_head_reject_flags.get := core.io.debug_wb_head_reject_flags
    io.debug_rob_head_pc.get := core.io.debug_rob_head_pc
    io.debug_rob_head_valid.get := core.io.debug_rob_head_valid
    io.debug_rob_head_done.get := core.io.debug_rob_head_done
    io.debug_rob_head_mem.get := core.io.debug_rob_head_mem
    io.debug_rob_head_ctrl.get := core.io.debug_rob_head_ctrl
    io.debug_rob_count.get := core.io.debug_rob_count
    io.debug_rs_count.get := core.io.debug_rs_count
    io.debug_brq_count.get := core.io.debug_brq_count
    io.debug_fq_count.get := core.io.debug_fq_count
    io.debug_brq_issue_valid.get := core.io.debug_brq_issue_valid
    io.debug_bru_dispatch_valid.get := core.io.debug_bru_dispatch_valid
  }

  core.io.imem <> xbar.io.imem
  core.io.dmem <> xbar.io.dmem
  xbar.io.clint <> clint.io
  uart.foreach(u => xbar.io.uart.get <> u.io)

  if (coreConfig.ysyxsoc) {
    xbar.io.soc <> io.master.get
  } else {
    val sram = Module(new SRAM)
    xbar.io.soc <> sram.io.sram
  }
}
