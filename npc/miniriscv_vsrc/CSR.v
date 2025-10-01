`include "include/defs.vh"

module CSR(
    input clk,
    input rst,
    input [11:0] csr_addr,
    output reg [31:0] csr_rdata
);

    reg [63:0] mcycle_reg;

    wire [31:0] mvendorid = 32'h79737978;  // "ysyx"
    wire [31:0] marchid   = 32'h0017DC687; //25020039

    always @(posedge clk) begin
        if (rst) begin
            mcycle_reg <= 64'b0;
        end else begin
            mcycle_reg <= mcycle_reg + 64'd1;
        end
    end

    always @(*) begin
        case (csr_addr)
            `CSR_MCYCLE: begin
                csr_rdata = mcycle_reg[31:0];
            end
            `CSR_MCYCLEH: begin
                csr_rdata = mcycle_reg[63:32];
            end
            `CSR_MVENDORID: begin
                csr_rdata = mvendorid;
            end
            `CSR_MARCHID: begin
                csr_rdata = marchid;
            end
            default: begin
                csr_rdata = 32'b0;
            end
        endcase
    end

endmodule