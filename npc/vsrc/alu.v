`include "defs.vh"
module ALU (
    input [`DATA_WIDTH-1:0] A,
    input [`DATA_WIDTH-1:0] B,
    input [4:0] ALUFun,
    output reg [`DATA_WIDTH-1:0] Result
);
    always @(*) begin
        case (ALUFun)
            5'b00000: Result = A + B;                  // ADD
            5'b00001: Result = A - B;                  // SUB
            5'b00010: Result = {31'b0, $signed(A) < $signed(B)}; // SLT
            5'b00011: Result = {31'b0, A < B};         // SLTU
            5'b00100: Result = A ^ B;                  // XOR
            5'b00101: Result = A | B;                  // OR
            5'b00110: Result = A & B;                  // AND
            5'b00111: Result = A << (B & 32'h1f);      // SLL
            5'b01000: Result = A >> (B & 32'h1f);      // SRL
            5'b01001: Result = $signed(A) >>> (B & 32'h1f); // SRA
            5'b01010: Result = A;                      // Pass A (LUI)
            default:  Result = 0;
        endcase
    end
endmodule