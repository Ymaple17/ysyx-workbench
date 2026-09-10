package sim

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR
import bus._

class Pmem extends BlackBox with HasBlackBoxInline {
  override def desiredName = "ysyx_25020039_Pmem"
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

  setInline("ysyx_25020039_Pmem.v",
    """
      |module ysyx_25020039_Pmem(
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
      |  localparam int MEM_BYTES = 128*1024;
      |  reg [7:0]  bytes [0:MEM_BYTES-1];
      |  reg [31:0] mem   [0:MEM_BYTES/4-1];
      |  integer i;
      |  initial begin
      |    for (i = 0; i < MEM_BYTES; i = i + 1)
      |      bytes[i] = 8'h0;
      |    $readmemh("mem.hex", bytes);
      |    for (i = 0; i < MEM_BYTES/4; i = i + 1)
      |      mem[i] = {bytes[4*i+3], bytes[4*i+2], bytes[4*i+1], bytes[4*i]};
      |  end
      |  wire [16:0] roff = raddr - 32'h80000000;
      |  wire [16:0] woff = waddr - 32'h80000000;
      |  assign rdata = (ren && raddr >= 32'h80000000 && raddr < 32'h80000000 + MEM_BYTES)
      |                   ? mem[roff[16:2]] : 32'h0;
      |  always @(posedge clk) begin
      |     if (wen) begin
      |        if (waddr == 32'ha00003f8) begin
      |            $write("%c", wdata[7:0]);
      |        end
      |        else if (waddr >= 32'h80000000 && waddr < 32'h80000000 + MEM_BYTES) begin
      |            if (wmask[0]) mem[woff[16:2]][7:0]   <= wdata[7:0];
      |            if (wmask[1]) mem[woff[16:2]][15:8]  <= wdata[15:8];
      |            if (wmask[2]) mem[woff[16:2]][23:16] <= wdata[23:16];
      |            if (wmask[3]) mem[woff[16:2]][31:24] <= wdata[31:24];
      |        end
      |     end
      |  end
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
    override def desiredName = "ysyx_25020039_SRAM"
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

  val r_delay_cnt = RegInit(0.U(32.W))
  val araddr_reg = RegInit(0.U(32.W))
  val arid_reg = RegInit(0.U(4.W))
  val rdata_reg = RegInit(0.U(32.W))
  val rresp_reg = RegInit(OKAY)

  r_next_state := MuxLookup(r_state, s_R_IDLE)(Seq(
    s_R_IDLE -> Mux(io.sram.arvalid && is_valid_addr(io.sram.araddr), s_R_WAIT, s_R_IDLE),
    s_R_WAIT -> Mux(r_delay_cnt === random_delay, s_R_DATA, s_R_WAIT),
    s_R_DATA -> Mux(io.sram.rready, s_R_IDLE, s_R_DATA)
  ))
  r_state := r_next_state

  io.sram.arready := (r_state === s_R_IDLE)
  io.sram.rvalid := (r_state === s_R_DATA)
  io.sram.rid := arid_reg
  io.sram.rresp := rresp_reg
  io.sram.rdata := rdata_reg
  io.sram.rlast := true.B

  when(io.sram.arvalid && io.sram.arready) {
    araddr_reg := io.sram.araddr
    arid_reg := io.sram.arid
    r_delay_cnt := 0.U
  }

  when(r_state === s_R_WAIT) {
    when(r_delay_cnt =/= 0.U) {
      r_delay_cnt := r_delay_cnt - 1.U
    }.otherwise {
      when(is_valid_addr(araddr_reg)) {
        pmem.io.ren   := true.B
        pmem.io.raddr := araddr_reg & "hfffffffc".U
        rdata_reg    := pmem.io.rdata
        rresp_reg    := OKAY
      }.otherwise {
        rdata_reg := 0.U
        rresp_reg := SLVERR
      }
    }
  }

  val s_W_IDLE :: s_W_WAIT :: s_W_DATA :: s_W_RESP :: Nil = Enum(4)
  val w_state = RegInit(s_W_IDLE)
  val w_next_state = WireDefault(s_W_IDLE)
  val w_delay_cnt = RegInit(0.U(32.W))

  val awaddr_reg = RegInit(0.U(32.W))
  val awid_reg = RegInit(0.U(4.W))
  val bresp_reg = RegInit(OKAY)

  w_next_state := MuxLookup(w_state, s_W_IDLE)(Seq(
    s_W_IDLE -> Mux(io.sram.awvalid, s_W_WAIT, s_W_IDLE),
    s_W_WAIT -> Mux(w_delay_cnt === random_delay, s_W_DATA, s_W_WAIT),
    s_W_DATA -> Mux(io.sram.wvalid, s_W_RESP, s_W_DATA),
    s_W_RESP -> Mux(io.sram.bready, s_W_IDLE, s_W_RESP)
  ))
  w_state := w_next_state

  io.sram.awready := (w_state === s_W_IDLE)
  io.sram.wready := (w_state === s_W_DATA)
  io.sram.bvalid := (w_state === s_W_RESP)
  io.sram.bid := awid_reg
  io.sram.bresp := bresp_reg

  when(io.sram.awvalid && io.sram.awready) {
    awaddr_reg := io.sram.awaddr
    awid_reg := io.sram.awid
    w_delay_cnt := random_delay
  }

  when(w_state === s_W_WAIT && w_delay_cnt =/= 0.U) {
    w_delay_cnt := w_delay_cnt - 1.U
  }

  when(io.sram.wvalid && io.sram.wready) {
    when(is_valid_addr(awaddr_reg)) {
      pmem.io.wen := true.B
      pmem.io.waddr := awaddr_reg & "hfffffffc".U
      pmem.io.wdata := io.sram.wdata
      pmem.io.wmask := io.sram.wstrb
      bresp_reg := OKAY
    }.otherwise {
      bresp_reg := SLVERR
    }
  }
  
}

