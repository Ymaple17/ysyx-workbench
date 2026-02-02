
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

  import "DPI-C" function int unsigned pmem_read(input int unsigned raddr, input int len);
  import "DPI-C" function void pmem_write(input int unsigned waddr, input int unsigned wdata, input int len);

  reg [31:0] rdata_reg;
  assign rdata = rdata_reg;

  always @(posedge clk) begin
    if (ren) begin
      rdata_reg <= pmem_read(raddr, 4);
    end
  end

  always @(posedge clk) begin
    if (wen) begin
      case(wmask)
        4'b0001: pmem_write(waddr, wdata, 1);
        4'b0011: pmem_write(waddr, wdata, 2);
        4'b1111: pmem_write(waddr, wdata, 4);
        default: pmem_write(waddr, wdata, 4);
      endcase
    end
  end

endmodule
    