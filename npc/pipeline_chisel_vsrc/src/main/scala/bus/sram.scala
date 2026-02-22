package bus

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR

class DPI_Mem extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    val ren = Input(Bool())
    val wen = Input(Bool())
    val raddr = Input(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wmask = Input(UInt(4.W))
    val rdata = Output(UInt(32.W))
  })

  setInline("DPI_Mem.v",
    """
      |module DPI_Mem(
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
      |    always @(*) begin
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
      |  import "DPI-C" function int unsigned pmem_read(input int unsigned raddr, input int len);
      |  import "DPI-C" function void pmem_write(input int unsigned waddr, input int unsigned wdata, input int len);
      |
      |  reg [31:0] rdata_reg;
      |  assign rdata = rdata_reg;
      |
      |  always @(*) begin
      |    if (ren) begin
      |      rdata_reg = pmem_read(raddr, 4);
      |    end else begin
      |      rdata_reg = 0;
      |    end
      |  end
      |
      |  always @(posedge clk) begin
      |    if (wen) begin
      |      // case(wmask)
      |      //   4'b0001: pmem_write(waddr, wdata, 1);
      |      //   4'b0011: pmem_write(waddr, wdata, 2);
      |      //   4'b1111: pmem_write(waddr, wdata, 4);
      |      //   default: pmem_write(waddr, wdata, 4);
      |      // endcase
      |      if (wmask[0]) pmem_write(waddr, wdata[7:0], 1);
      |      if (wmask[1]) pmem_write(waddr + 1, wdata[15:8], 1);
      |      if (wmask[2]) pmem_write(waddr + 2, wdata[23:16], 1);
      |      if (wmask[3]) pmem_write(waddr + 3, wdata[31:24], 1);
      |    end
      |  end
      |`endif
      |
      |`endif
      |
      |endmodule
    """.stripMargin)
}

// ===== SRAM Module =====
class SRAM_IO extends Bundle {
  val axi = new AXI4Slave
}

class SRAM extends Module {
  override def desiredName = "ysyx_25020039_SRAM"

  val io = IO(new SRAM_IO)
  
  // Set default output values
  io.axi.arready := false.B
  io.axi.rvalid  := false.B
  io.axi.rdata   := 0.U
  io.axi.rresp   := 0.U
  io.axi.rlast   := true.B 
  io.axi.rid     := 0.U
  
  io.axi.awready := false.B
  io.axi.wready  := false.B
  io.axi.bvalid  := false.B
  io.axi.bresp   := 0.U
  io.axi.bid     := 0.U

  val mem = Module(new DPI_Mem)
  mem.io.clk   := clock
  mem.io.rst   := reset.asBool
  mem.io.ren   := false.B
  mem.io.wen   := false.B
  mem.io.raddr := 0.U
  mem.io.waddr := 0.U
  mem.io.wdata := 0.U
  mem.io.wmask := 0.U

  val OKAY   = 0.U(2.W)
  val SLVERR = 2.U(2.W)
  
  val s_IDLE :: s_READ_ADDR :: s_READ_DATA :: s_WRITE_ADDR :: s_WRITE_DATA :: s_WRITE_RESP :: Nil = Enum(6)
  val state = RegInit(s_IDLE)
  
  val LFSR_DELAY = Reg(UInt(32.W))
  val araddr_reg = RegInit(0.U(32.W))
  val arid_reg   = RegInit(0.U(4.W))
  
  val awaddr_reg = RegInit(0.U(32.W))
  val awid_reg   = RegInit(0.U(4.W))
  val wdata_reg  = RegInit(0.U(32.W))
  val wstrb_reg  = RegInit(0.U(4.W))
  
  val rdata_reg  = RegInit(0.U(32.W))
  val rresp_reg  = RegInit(OKAY)
  val bresp_reg  = RegInit(OKAY)
  
  def isValidAddr(addr: UInt): Bool = {
     (addr >= "h80000000".U && addr <= "h8fffffff".U) || 
     (addr >= "ha0000000".U && addr <= "ha0000007".U)
  }

  // Generate random delay
  // val lfsr_val = LFSR(16)
  // val random_delay = (lfsr_val % 10.U) + 1.U
  val random_delay = 0.U

  switch(state) {
    is(s_IDLE) {
      io.axi.arready := true.B
      io.axi.awready := true.B
      
      when(io.axi.arvalid) {
        araddr_reg := io.axi.araddr
        arid_reg   := io.axi.arid
        LFSR_DELAY := random_delay
        state      := s_READ_ADDR
      }.elsewhen(io.axi.awvalid) {
        awaddr_reg := io.axi.awaddr
        awid_reg   := io.axi.awid
        LFSR_DELAY := random_delay
        // Do not deassert ready here
        state      := s_WRITE_ADDR
      }
    }
    
    is(s_READ_ADDR) {
      when(LFSR_DELAY > 0.U) {
        LFSR_DELAY := LFSR_DELAY - 1.U
      }.otherwise {
        when(isValidAddr(araddr_reg)) {
          mem.io.ren   := true.B
          mem.io.raddr := araddr_reg & "hfffffffc".U
          rdata_reg    := mem.io.rdata
          rresp_reg    := OKAY
        }.otherwise {
          rdata_reg    := 0.U
          rresp_reg    := SLVERR
        }
        state := s_READ_DATA
      }
    }
    
    is(s_READ_DATA) {
      io.axi.rvalid := true.B
      io.axi.rdata  := rdata_reg
      io.axi.rresp  := rresp_reg
      io.axi.rid    := arid_reg
      io.axi.rlast  := true.B
      
      when(io.axi.rready) {
         araddr_reg := 0.U
         state := s_IDLE
      }
    }
    
    is(s_WRITE_ADDR) {
      when(LFSR_DELAY > 0.U) {
        LFSR_DELAY := LFSR_DELAY - 1.U
      }.otherwise {
        // Do not assert wready here. Wait until state changes to s_WRITE_DATA
        state := s_WRITE_DATA
      }
    }
    
    is(s_WRITE_DATA) {
       io.axi.wready := true.B
       
       when(io.axi.wvalid) {
         wdata_reg := io.axi.wdata
         wstrb_reg := io.axi.wstrb
         
         when(isValidAddr(awaddr_reg)) {
            mem.io.wen   := true.B
            mem.io.waddr := awaddr_reg & "hfffffffc".U // Align to 4-byte boundary
            mem.io.wdata := io.axi.wdata
            mem.io.wmask := io.axi.wstrb
            bresp_reg    := OKAY
         }.otherwise {
            bresp_reg    := SLVERR
         }
         state := s_WRITE_RESP
       }
    }
    
    is(s_WRITE_RESP) {
      io.axi.bvalid := true.B
      io.axi.bresp  := bresp_reg
      io.axi.bid    := awid_reg
      
      when(io.axi.bready) {
        state := s_IDLE
      }
    }
  }
}
