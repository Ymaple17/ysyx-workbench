`include "include/defs.vh"

module EXU (
    input clk,
    input rst,
    input [`DATA_WIDTH-1:0] Op1,
    input [`DATA_WIDTH-1:0] Op2,
    input [3:0]             alu_op,
    input                   is_csr_instr,
    input [2:0]                  csr_op,
    input [`DATA_WIDTH-1:0] csr_rdata,
    output reg csr_we,
    output reg [`DATA_WIDTH-1:0] csr_wdata,
    output [`DATA_WIDTH-1:0] alu_result
);

    assign alu_result = result;
    wire [`DATA_WIDTH-1:0] result;
    
    ALU alu (
        .A(Op1),
        .B(Op2),
        .op(alu_op),
        .Result(result)
    );

    always @(*) begin
        csr_we = 0;
        csr_wdata = 0;
        if(is_csr_instr) begin
            case(csr_op)
                3'b001: begin// CSRRW
                    csr_we = 1;
                    csr_wdata = Op1;
                end
                3'b010: begin// CSRRS
                    csr_we = (Op1 != 32'b0);
                    csr_wdata = Op1 | csr_rdata;
                end
                3'b011: begin// CSRRC
                    csr_we = (Op1 != 32'b0);
                    csr_wdata = (~Op1) & csr_rdata;
                end
                default: begin
                    csr_we = 0;
                    csr_wdata = 0;
                end
            endcase
        end
    end

endmodule




    
