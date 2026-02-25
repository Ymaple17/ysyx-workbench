

module DPI_Mem(
  input         clk,
  input         rst,
  input         ren,
  input         wen,
  input  [31:0] raddr,
  input  [31:0] waddr,
  input  [31:0] wdata,
  input  [3:0]  wmask,
  output [31:0] rdata
);

`ifdef __ICARUS__
  reg [7:0] mem [0:16*1024-1];
  initial begin
    $readmemh("mem.hex", mem);
  end

  reg [31:0] rdata_reg;
  
    always @(*) begin
     if (ren) begin
        if (raddr >= 32'h80000000 && raddr < 32'h80000000 +16*1024) begin
            rdata_reg[7:0]   = mem[(raddr - 32'h80000000)];
            rdata_reg[15:8]  = mem[(raddr - 32'h80000000) + 1];
            rdata_reg[23:16] = mem[(raddr - 32'h80000000) + 2];
            rdata_reg[31:24] = mem[(raddr - 32'h80000000) + 3];
        end else begin
            rdata_reg = 32'h0;
        end
     end else begin
        rdata_reg = 32'h0;
     end
  end

  always @(posedge clk) begin
     if (wen) begin
        if (waddr == 32'ha00003f8) begin
            $write("%c", wdata[7:0]);
        end

        else if (waddr >= 32'h80000000 && waddr < 32'h80000000 + 16*1024) begin
            if (wmask[0]) mem[(waddr - 32'h80000000)]     <= wdata[7:0];
            if (wmask[1]) mem[(waddr - 32'h80000000) + 1] <= wdata[15:8];
            if (wmask[2]) mem[(waddr - 32'h80000000) + 2] <= wdata[23:16];
            if (wmask[3]) mem[(waddr - 32'h80000000) + 3] <= wdata[31:24];
        end
     end
  end

  assign rdata = rdata_reg;

`else

`ifndef YOSYS
  import "DPI-C" function int unsigned pmem_read(input int unsigned raddr, input int len);
  import "DPI-C" function void pmem_write(input int unsigned waddr, input int unsigned wdata, input int len);

  reg [31:0] rdata_reg;
  assign rdata = rdata_reg;

  always @(*) begin
    if (ren) begin
      rdata_reg = pmem_read(raddr, 4);
    end else begin
      rdata_reg = 0;
    end
  end

  always @(posedge clk) begin
    if (wen) begin
      // case(wmask)
      //   4'b0001: pmem_write(waddr, wdata, 1);
      //   4'b0011: pmem_write(waddr, wdata, 2);
      //   4'b1111: pmem_write(waddr, wdata, 4);
      //   default: pmem_write(waddr, wdata, 4);
      // endcase
      if (wmask[0]) pmem_write(waddr, wdata[7:0], 1);
      if (wmask[1]) pmem_write(waddr + 1, wdata[15:8], 1);
      if (wmask[2]) pmem_write(waddr + 2, wdata[23:16], 1);
      if (wmask[3]) pmem_write(waddr + 3, wdata[31:24], 1);
    end
  end
`endif

`endif

endmodule
    
