`include "include/defs.vh"

module ALU(
    input [`DATA_WIDTH-1:0] A,
    input [`DATA_WIDTH-1:0] B,
    input [3:0] op,
    output reg [`DATA_WIDTH-1:0] Result
);

    wire [`DATA_WIDTH-1:0] add_result = A + B;
    wire [`DATA_WIDTH-1:0] sub_result = A - B;
    wire [`DATA_WIDTH-1:0] and_result = A & B;
    wire [`DATA_WIDTH-1:0] or_result = A | B;
    wire [`DATA_WIDTH-1:0] xor_result = A ^ B;
    wire [`DATA_WIDTH-1:0] slt_result = {31'b0,$signed(A) < $signed(B)};
    wire [`DATA_WIDTH-1:0] sltu_result = {31'b0,A < B};
    wire [`DATA_WIDTH-1:0] sll_result = A << (B & 32'h1F);
    wire [`DATA_WIDTH-1:0] srl_result = A >> (B & 32'h1F);
    wire [`DATA_WIDTH-1:0] sra_result = $signed(A) >>> (B & 32'h1F);
    wire [`DATA_WIDTH-1:0] op1_result = A;

    MuxKey #(11,4,`DATA_WIDTH) alu_mux (
        .out(Result),
       .key(op),
       .lut({
           4'b0000, add_result,
           4'b0001, sub_result,
           4'b0010, slt_result,
           4'b0011, sltu_result,
           4'b0100, xor_result,
           4'b0101, or_result,
           4'b0110, and_result,
           4'b0111, sll_result,
           4'b1000, srl_result,
           4'b1001, sra_result,
           4'b1010, op1_result
       })
    );

endmodule
