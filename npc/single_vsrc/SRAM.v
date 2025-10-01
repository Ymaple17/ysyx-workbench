module  SRAM(
    input clk,
    input rst,
    input [31:0] ifu_raddr,
    output [31:0] ifu_rdata
);

    import "DPI-C" context function int paddr_read(input int raddr);

    reg [31:0] rdata_reg;

    always @(*) begin
        if (rst) begin
            rdata_reg = 0;
        end else begin
            rdata_reg = paddr_read(ifu_raddr);
        end
    end

    assign ifu_rdata = rdata_reg;
    
endmodule
