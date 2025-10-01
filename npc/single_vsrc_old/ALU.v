`include "include/defs.vh"

module ALU (
    input [`DATA_WIDTH-1:0] A,
    input [`DATA_WIDTH-1:0] B,
    input [4:0] ALUFun,
    output reg [`DATA_WIDTH-1:0] Result
);

    // Internal signals declaration
    wire [`DATA_WIDTH-1:0] add_result     = A + B;                  // Addition
    wire [`DATA_WIDTH-1:0] sub_result     = A - B;                  // Subtraction
    wire [`DATA_WIDTH-1:0] slt_result     = {31'b0, $signed(A) < $signed(B)}; // Less than (signed)
    wire [`DATA_WIDTH-1:0] sltu_result    = {31'b0, A < B};         // Less than (unsigned)
    wire [`DATA_WIDTH-1:0] xor_result     = A ^ B;                  // Bitwise XOR
    wire [`DATA_WIDTH-1:0] or_result      = A | B;                  // Bitwise OR
    wire [`DATA_WIDTH-1:0] and_result     = A & B;                  // Bitwise AND
    wire [`DATA_WIDTH-1:0] sll_result     = A << (B & 32'h1f);      // Logical shift left
    wire [`DATA_WIDTH-1:0] srl_result     = A >> (B & 32'h1f);      // Logical shift right
    wire [`DATA_WIDTH-1:0] sra_result = $signed(A) >>> (B & 32'h1f); // Arithmetic shift right
    wire [`DATA_WIDTH-1:0] op1_result = A;                          // Pass A as result
    wire [63:0] mul_signed                = $signed(A) * $signed(B);  
    wire [63:0] mul_unsigned              = $unsigned(A) * $unsigned(B); 
    wire [`DATA_WIDTH-1:0] mul_result     = mul_signed[31:0];        
    wire [`DATA_WIDTH-1:0] mulh_result    = mul_signed[63:32]; 
    wire [`DATA_WIDTH-1:0] mulhu_result   = mul_unsigned[63:32];   
    wire [`DATA_WIDTH-1:0] div_result     = ($signed(B) == 32'd0) ? 32'h0 : $signed(A) / $signed(B);
    wire [`DATA_WIDTH-1:0] divu_result    = (B == 32'd0) ? 32'h0 : $unsigned(A) / $unsigned(B);
    wire [`DATA_WIDTH-1:0] rem_result     = ($signed(B) == 32'd0) ? 32'h0 : $signed(A) % $signed(B);
    wire [`DATA_WIDTH-1:0] remu_result    = (B == 32'd0) ? 32'h0 : $unsigned(A) % $unsigned(B);


    // Instantiate MuxKey module
    MuxKey #(18, 5, `DATA_WIDTH) alu_mux (
        .out(Result),
        .key(ALUFun),
        .lut({
            5'b00000, add_result,  // Addition
            5'b00001, sub_result,  // Subtraction
            5'b00010, slt_result,  // Signed less than
            5'b00011, sltu_result, // Unsigned less than
            5'b00100, xor_result,  // Bitwise XOR
            5'b00101, or_result,   // Bitwise OR
            5'b00110, and_result,  // Bitwise AND
            5'b00111, sll_result,  // Logical shift left
            5'b01000, srl_result,  // Logical shift right
            5'b01001, sra_result,  // Arithmetic shift right
            5'b01010, op1_result,   // Pass A as result
            5'b01011, mul_result,    // mul：乘法（低32位）
            5'b01100, mulh_result,   // mulh：有符号乘法高位
            5'b01101, mulhu_result,  // mulhu：无符号乘法高位
            5'b01111, div_result,    // div：有符号除法
            5'b10000, divu_result,   // divu：无符号除法
            5'b10001, rem_result,    // rem：有符号取余
            5'b10010, remu_result    // remu：无符号取余
        })
    );

endmodule
