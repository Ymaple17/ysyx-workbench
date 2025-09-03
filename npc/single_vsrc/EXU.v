`include "include/defs.vh"

module EXU (
    input wire clk,
    input wire rst,
    input wire [`DATA_WIDTH-1:0] Op1,
    input wire [`DATA_WIDTH-1:0] Op2,
    input wire [4:0] alu_op,
    output wire [`DATA_WIDTH-1:0] alu_result,
    input wire is_csr_instr,
    input wire [2:0] csr_op,
    input wire [`DATA_WIDTH-1:0] csr_rdata,
    output reg csr_we,
    output reg [`DATA_WIDTH-1:0] csr_wdata
);

    assign alu_result = result;
    wire [`DATA_WIDTH-1:0] result;
    ALU alu_instance (
       .A(Op1),
       .B(Op2),
      .ALUFun(alu_op),
      .Result(result)
    );

    always @(*) begin
        csr_we = 1'b0;
        csr_wdata = 32'b0;
        if(is_csr_instr) begin
            case(csr_op)
                3'b001: begin // CSRRW
                    csr_we = 1'b1;
                    csr_wdata = Op1;
                end
                3'b010: begin // CSRRS
                    csr_we = (Op1 != 32'b0);
                    csr_wdata = Op1 | csr_rdata;
                end
                3'b011: begin // CSRRC
                    csr_we = (Op1 != 32'b0);
                    csr_wdata = (~Op1) & csr_rdata;
                end
                default: begin
                    csr_we = 1'b0;
                    csr_wdata = 32'b0;
                end
            endcase
        end
    end

endmodule
