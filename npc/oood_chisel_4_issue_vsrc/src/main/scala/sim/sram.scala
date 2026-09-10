package sim

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR
import bus._

class Pmem extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk   = Input(Clock())
    val rst   = Input(Bool())
    val ren   = Input(Bool())
    val wen   = Input(Bool())
    val raddr = Input(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wmask = Input(UInt(4.W))
    val rdata = Output(UInt(32.W))
  })

  setInline("Pmem.v",
    """
      |module Pmem(
      |  input         clk,
      |  input         rst,
      |  input         ren,
      |  input         wen,
      |  input  [31:0] raddr,
      |  input  [31:0] waddr,
      |  input  [31:0] wdata,
      |  input  [3:0]  wmask,
      |  output [31:0] rdata
      |);
      |
      |`ifdef __ICARUS__
      |  reg [7:0] mem [0:128*1024-1];
      |  initial begin
      |    $readmemh("mem.hex", mem);
      |  end
      |
      |  reg [31:0] rdata_reg;
      |
      |  always @(*) begin
      |     if (ren) begin
      |        if (raddr >= 32'h80000000 && raddr < 32'h80000000 +128*1024) begin
      |            rdata_reg[7:0]   = mem[(raddr - 32'h80000000)];
      |            rdata_reg[15:8]  = mem[(raddr - 32'h80000000) + 1];
      |            rdata_reg[23:16] = mem[(raddr - 32'h80000000) + 2];
      |            rdata_reg[31:24] = mem[(raddr - 32'h80000000) + 3];
      |        end else begin
      |            rdata_reg = 32'h0;
      |        end
      |     end else begin
      |        rdata_reg = 32'h0;
      |     end
      |  end
      |
      |  always @(posedge clk) begin
      |     if (wen) begin
      |        if (waddr == 32'ha00003f8) begin
      |            $write("%c", wdata[7:0]);
      |        end
      |
      |        else if (waddr >= 32'h80000000 && waddr < 32'h80000000 + 128*1024) begin
      |            if (wmask[0]) mem[(waddr - 32'h80000000)]     <= wdata[7:0];
      |            if (wmask[1]) mem[(waddr - 32'h80000000) + 1] <= wdata[15:8];
      |            if (wmask[2]) mem[(waddr - 32'h80000000) + 2] <= wdata[23:16];
      |            if (wmask[3]) mem[(waddr - 32'h80000000) + 3] <= wdata[31:24];
      |        end
      |     end
      |  end

      |  assign rdata = rdata_reg;
      |
      |`else
      |
      |`ifndef YOSYS
      |  import "DPI-C" function int unsigned paddr_read(input int unsigned raddr, input int len);
      |  import "DPI-C" function void paddr_write(input int unsigned waddr, input int unsigned wdata, input int len);
      |
      |  reg [31:0] rdata_reg;
      |  assign rdata = rdata_reg;
      |
      |  always @(*) begin
      |    if (ren) begin
      |      rdata_reg = paddr_read(raddr, 4);
      |    end else begin
      |      rdata_reg = 0;
      |    end
      |  end
      |
      |  always @(posedge clk) begin
      |    if (wen) begin
      |      if (wmask[0]) paddr_write(waddr, wdata[7:0], 1);
      |      if (wmask[1]) paddr_write(waddr + 1, wdata[15:8], 1);
      |      if (wmask[2]) paddr_write(waddr + 2, wdata[23:16], 1);
      |      if (wmask[3]) paddr_write(waddr + 3, wdata[31:24], 1);
      |    end
      |  end
      |`endif
      |
      |`endif
      |
      |endmodule
    """.stripMargin)
}

class SRAM_IO extends Bundle{
  val sram = new AXI4Slave
}

class SRAM extends Module{
  val io = IO(new SRAM_IO)

  val pmem = Module(new Pmem)
  pmem.io.clk := clock
  pmem.io.rst := reset.asBool
  pmem.io.ren := false.B
  pmem.io.wen := false.B
  pmem.io.raddr := 0.U
  pmem.io.waddr := 0.U
  pmem.io.wdata := 0.U
  pmem.io.wmask := 0.U

  val OKAY = 0.U(2.W)
  val SLVERR = 2.U(2.W)

  def is_valid_addr(addr: UInt): Bool = {
    (addr >= "h80000000".U && addr < "h8fffffff".U) || (addr >= "ha0000000".U && addr < "ha0000007".U)
  }

  // val lfsr_val    = LFSR(16)
  // val random_delay = (lfsr_val % 10.U) + 1.U
  val random_delay = 0.U

  val s_R_IDLE :: s_R_WAIT :: s_R_DATA :: Nil = Enum(3)
  val r_state = RegInit(s_R_IDLE)
  val r_next_state = WireDefault(s_R_IDLE)

  val r_delay_cnt = Reg(UInt(32.W))
  val araddr_reg = RegInit(0.U(32.W))
  val arid_reg = RegInit(0.U(4.W))
  val arlen_reg = RegInit(0.U(8.W))
  val arsize_reg = RegInit(2.U(3.W))
  val arburst_reg = RegInit(1.U(2.W))
  val rbeat_reg = RegInit(0.U(8.W))

  r_next_state := MuxLookup(r_state, s_R_IDLE)(Seq(
    s_R_IDLE -> Mux(io.sram.arvalid, s_R_WAIT, s_R_IDLE),
    s_R_WAIT -> Mux(r_delay_cnt === random_delay, s_R_DATA, s_R_WAIT),
    s_R_DATA -> Mux(io.sram.rready && (rbeat_reg === arlen_reg), s_R_IDLE, s_R_DATA)
  ))
  r_state := r_next_state

  io.sram.arready := (r_state === s_R_IDLE)
  io.sram.rvalid := (r_state === s_R_DATA)
  io.sram.rid := arid_reg
  val beatOffset = Mux(arburst_reg === 0.U, 0.U(32.W), rbeat_reg.pad(32) << arsize_reg)
  val beatAddr = araddr_reg + beatOffset
  val beatAddrValid = is_valid_addr(beatAddr)
  io.sram.rresp := Mux(beatAddrValid, OKAY, SLVERR)
  io.sram.rdata := Mux(beatAddrValid, pmem.io.rdata, 0.U)
  io.sram.rlast := rbeat_reg === arlen_reg
  pmem.io.ren := (r_state === s_R_DATA) && beatAddrValid
  pmem.io.raddr := beatAddr & "hfffffffc".U

  when(io.sram.arvalid && io.sram.arready) {
    araddr_reg := io.sram.araddr
    arid_reg := io.sram.arid
    arlen_reg := io.sram.arlen
    arsize_reg := io.sram.arsize
    arburst_reg := io.sram.arburst
    rbeat_reg := 0.U
    r_delay_cnt := random_delay
  }

  when(r_state === s_R_WAIT && r_delay_cnt =/= 0.U) {
    r_delay_cnt := r_delay_cnt - 1.U
  }

  when(r_state === s_R_DATA && io.sram.rready && (rbeat_reg =/= arlen_reg)) {
    rbeat_reg := rbeat_reg + 1.U
  }

  val s_W_IDLE :: s_W_WAIT :: s_W_DATA :: s_W_RESP :: Nil = Enum(4)
  val w_state = RegInit(s_W_IDLE)
  val w_next_state = WireDefault(s_W_IDLE)
  val w_delay_cnt = Reg(UInt(32.W))

  val awaddr_reg = RegInit(0.U(32.W))
  val awid_reg = RegInit(0.U(4.W))
  val awlen_reg = RegInit(0.U(8.W))
  val awsize_reg = RegInit(0.U(3.W))
  val awburst_reg = RegInit(0.U(2.W))
  val wbeat_reg = RegInit(0.U(8.W))
  val bresp_reg = RegInit(OKAY)
  val wFire = io.sram.wvalid && io.sram.wready
  val wFinal = wbeat_reg === awlen_reg

  w_next_state := MuxLookup(w_state, s_W_IDLE)(Seq(
    s_W_IDLE -> Mux(io.sram.awvalid, s_W_WAIT, s_W_IDLE),
    s_W_WAIT -> Mux(w_delay_cnt === random_delay, s_W_DATA, s_W_WAIT),
    s_W_DATA -> Mux(wFire && wFinal, s_W_RESP, s_W_DATA),
    s_W_RESP -> Mux(io.sram.bready,
      Mux(io.sram.awvalid, s_W_WAIT, s_W_IDLE), s_W_RESP)
  ))
  w_state := w_next_state

  io.sram.awready := (w_state === s_W_IDLE) ||
    (w_state === s_W_RESP && io.sram.bready)
  io.sram.wready := (w_state === s_W_DATA)
  io.sram.bvalid := (w_state === s_W_RESP)
  io.sram.bid := awid_reg
  io.sram.bresp := bresp_reg

  when(io.sram.awvalid && io.sram.awready) {
    awaddr_reg := io.sram.awaddr
    awid_reg := io.sram.awid
    awlen_reg := io.sram.awlen
    awsize_reg := io.sram.awsize
    awburst_reg := io.sram.awburst
    wbeat_reg := 0.U
    bresp_reg := OKAY
    w_delay_cnt := random_delay
  }

  when(w_state === s_W_WAIT && w_delay_cnt =/= 0.U) {
    w_delay_cnt := w_delay_cnt - 1.U
  }

  val writeBeatAddr = Mux(awburst_reg === 1.U,
    awaddr_reg + (wbeat_reg << awsize_reg), awaddr_reg)
  when(wFire) {
    assert(io.sram.wlast === wFinal, "AXI write last must match AWLEN")
    when(is_valid_addr(writeBeatAddr)) {
      pmem.io.wen := true.B
      pmem.io.waddr := writeBeatAddr & "hfffffffc".U
      pmem.io.wdata := io.sram.wdata
      pmem.io.wmask := io.sram.wstrb
    }.otherwise {
      bresp_reg := SLVERR
    }
    when(!wFinal) {
      wbeat_reg := wbeat_reg + 1.U
    }
  }
  
}
