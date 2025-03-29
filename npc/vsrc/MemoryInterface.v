module MemoryInterface (
    input  wire        clock,
    input  wire        mem_read,
    input  wire        mem_write,
    input  wire [31:0] addr,
    input  wire [31:0] wdata,
    input  wire [7:0]  wmask,
    output reg  [31:0] rdata
);
    import "DPI-C" function void paddr_read(input bit [31:0] addr, output bit [31:0] data);
    import "DPI-C" function void paddr_write(input bit [31:0] addr, input bit [31:0] data, input byte mask);
    always @(posedge clock) begin
        if (mem_read) begin
            paddr_read(addr, rdata);
        end else if (mem_write) begin
            paddr_write(addr, wdata, wmask);
        end
    end
endmodule
