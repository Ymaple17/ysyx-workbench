module RegisterFile #(parameter ADDR_WIDTH = 5, DATA_WIDTH = 32) (
    input clk,
    input [DATA_WIDTH-1:0] wdata,
    input [ADDR_WIDTH-1:0] waddr,
    input wen,
    input [DATA_WIDTH-1:0] pc,
    input [ADDR_WIDTH-1:0] raddr1,
    input [ADDR_WIDTH-1:0] raddr2,
    output [DATA_WIDTH-1:0] rdata1,
    output [DATA_WIDTH-1:0] rdata2,
    input  is_csr_instr
);    
    reg [DATA_WIDTH-1:0] rg [0:(1<<ADDR_WIDTH)-1];

    initial begin
        integer i;
        for (i = 0; i < (1 << ADDR_WIDTH); i = i + 1) begin
            rg[i] = {DATA_WIDTH{1'b0}};
        end
    end

    always @(posedge clk) begin
        if ((is_csr_instr || wen) && waddr != 0) begin
            rg[waddr] <= wdata;
        end
    end

    assign rdata1 = (raddr1 == 0) ? {DATA_WIDTH{1'b0}} : rg[raddr1];
    assign rdata2 = (raddr2 == 0) ? {DATA_WIDTH{1'b0}} : rg[raddr2];

endmodule
