package bus

import chisel3._
import chisel3.util._

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
      |  import "DPI-C" function int unsigned pmem_read(input int unsigned raddr, input int len);
      |  import "DPI-C" function void pmem_write(input int unsigned waddr, input int unsigned wdata, input int len);
      |
      |  reg [31:0] rdata_reg;
      |  assign rdata = rdata_reg;
      |
      |  always @(posedge clk) begin
      |    if (ren) begin
      |      rdata_reg <= pmem_read(raddr, 4);
      |    end
      |  end
      |
      |  always @(posedge clk) begin
      |    if (wen) begin
      |      case(wmask)
      |        4'b0001: pmem_write(waddr, wdata, 1);
      |        4'b0011: pmem_write(waddr, wdata, 2);
      |        4'b1111: pmem_write(waddr, wdata, 4);
      |        default: pmem_write(waddr, wdata, 4);
      |      endcase
      |    end
      |  end
      |
      |endmodule
    """.stripMargin)
}

// ===== SRAM Module =====
class SRAM_IO extends Bundle {
  val axi = new AXI4Slave
}

class SRAM extends Module {
  val io = IO(new SRAM_IO)
  
  io.axi.setDefaults()
  
  // DPI-C 存储器实例
  val mem = Module(new DPI_Mem)
  mem.io.clk := clock
  mem.io.rst := reset
  mem.io.ren := false.B
  mem.io.wen := false.B
  mem.io.raddr := 0.U
  mem.io.waddr := 0.U
  mem.io.wdata := 0.U
  mem.io.wmask := 0.U
  
  // 状态机
  val s_IDLE :: s_READ :: s_WRITE_ADDR :: s_WRITE_DATA :: s_WRITE_RESP :: Nil = Enum(5)
  val state = RegInit(s_IDLE)
  
  // 寄存器
  val araddr_reg = RegInit(0.U(32.W))
  val arid_reg = RegInit(0.U(4.W))
  val arlen_reg = RegInit(0.U(8.W))
  val arsize_reg = RegInit(0.U(3.W))
  val arburst_reg = RegInit(0.U(2.W))
  
  val awaddr_reg = RegInit(0.U(32.W))
  val awid_reg = RegInit(0.U(4.W))
  
  val wdata_reg = RegInit(0.U(32.W))
  val wstrb_reg = RegInit(0.U(4.W))
  
  val beat_cnt = RegInit(0.U(8.W))
  
  // 地址有效性检查（0x80000000 - 0x8fffffff）
  val addr_valid_read = araddr_reg >= "h8000_0000".U && araddr_reg <= "h8fff_ffff".U
  val addr_valid_write = awaddr_reg >= "h8000_0000".U && awaddr_reg <= "h8fff_ffff".U
  
  // 计算 burst 地址
  def getCurrentAddr(base: UInt, beat: UInt, size: UInt, burst: UInt): UInt = {
    val byte_offset = beat << size
    val addr = WireDefault(base)
    when(burst === 1.U) {  // INCR
      addr := base + byte_offset
    }.otherwise {  // FIXED
      addr := base
    }
    addr
  }
  
  // 状态机逻辑
  switch(state) {
    is(s_IDLE) {
      io.axi.arready := true.B
      io.axi.awready := true.B
      io.axi.rvalid := false.B
      io.axi.bvalid := false.B
      beat_cnt := 0.U
      
      when(io.axi.arvalid && io.axi.arready) {
        araddr_reg := io.axi.araddr
        arid_reg := io.axi.arid
        arlen_reg := io.axi.arlen
        arsize_reg := io.axi.arsize
        arburst_reg := io.axi.arburst
        state := s_READ
      }.elsewhen(io.axi.awvalid && io.axi.awready) {
        awaddr_reg := io.axi.awaddr
        awid_reg := io.axi.awid
        state := s_WRITE_ADDR
      }
    }
    
    is(s_READ) {
      io.axi.arready := false.B
      io.axi.rvalid := true.B
      io.axi.rlast := (beat_cnt === arlen_reg)
      io.axi.rid := arid_reg
      
      val current_addr = getCurrentAddr(araddr_reg, beat_cnt, arsize_reg, arburst_reg)
      
      mem.io.ren := addr_valid_read
      mem.io.raddr := current_addr
      io.axi.rdata := mem.io.rdata
      io.axi.rresp := Mux(addr_valid_read, 0.U, 2.U)
      
      when(io.axi.rready) {
        when(io.axi.rlast) {
          state := s_IDLE
        }.otherwise {
          beat_cnt := beat_cnt + 1.U
        }
      }
    }
    
    is(s_WRITE_ADDR) {
      io.axi.awready := false.B
      io.axi.wready := true.B
      
      when(io.axi.wvalid && io.axi.wready) {
        wdata_reg := io.axi.wdata
        wstrb_reg := io.axi.wstrb
        state := s_WRITE_DATA
      }
    }
    
    is(s_WRITE_DATA) {
      io.axi.wready := false.B
      
      mem.io.wen := addr_valid_write
      mem.io.waddr := awaddr_reg
      mem.io.wdata := wdata_reg
      mem.io.wmask := wstrb_reg
      
      state := s_WRITE_RESP
    }
    
    is(s_WRITE_RESP) {
      io.axi.bvalid := true.B
      io.axi.bresp := Mux(addr_valid_write, 0.U, 2.U)
      io.axi.bid := awid_reg
      
      when(io.axi.bready) {
        state := s_IDLE
      }
    }
  }
}