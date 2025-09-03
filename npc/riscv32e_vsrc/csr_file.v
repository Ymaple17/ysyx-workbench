`include "defs.vh"

module csr_file(
    input clk,
    input rst,
    input [11:0] csr_addr,
    input [`DATA_WIDTH-1:0] pc,
    input [`DATA_WIDTH-1:0] csr_wdata,
    input csr_we,
    input is_ecall,
    input is_mret,
    output reg [`DATA_WIDTH-1:0] mtvec_val,
    output reg [`DATA_WIDTH-1:0] mepc_val,
    output reg [`DATA_WIDTH-1:0] csr_rdata
);
    reg [`DATA_WIDTH-1:0] mcause;
    reg [`DATA_WIDTH-1:0] mstaus;

    always @ (posedge clk) begin
        if (rst) begin
            mtvec_val <= 32'h0;
            mepc_val <= 32'h0;
            mcause <= 32'h0;
            mstaus <= 32'h1800;
        end else if(is_ecall) begin
            mepc_val <= pc;
            mcause <= 32'h0000000b;
        end else if(csr_we) begin
            case (csr_addr)
                12'h300: mstaus <= csr_wdata;
                12'h305: mtvec_val <= {csr_wdata[31:2], 2'b00};
                12'h341: mepc_val <= csr_wdata;
                12'h342: mcause <= csr_wdata;
                default: ;
            endcase
        end
    end

    always @(*) begin
        case (csr_addr)
            12'h300: csr_rdata = mstaus;
            12'h305: csr_rdata = mtvec_val;
            12'h341: csr_rdata = mepc_val;
            12'h342: csr_rdata = mcause;
            default: csr_rdata = 32'h0;
        endcase
    end

endmodule