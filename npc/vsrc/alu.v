`include "defs.vh"
module ALU (
    input [`DATA_WIDTH-1:0] A,
    input [`DATA_WIDTH-1:0] B,
    input [4:0] ALUFun,
    output reg [`DATA_WIDTH-1:0] Result
);
    reg [63:0] mul_temp;
    
    always @(*) begin
        mul_temp = 64'h0;
        Result = {`DATA_WIDTH{1'b0}};
        
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
            5'b01011: Result = A * B;                  // MUL (低32位)
            5'b01100: begin                            // MULH (有符号乘法高位)
                mul_temp = $signed(A) * $signed(B);
                Result = mul_temp[63:32];
            end
            5'b01101: begin                            // MULHU (无符号乘法高位)
                mul_temp = {1'b0, A} * {1'b0, B};
                Result = mul_temp[63:32];
            end
            
            // 除法指令
            5'b01110: begin                            // DIV (有符号除法)
                if (B == {`DATA_WIDTH{1'b0}}) begin
                    Result = {`DATA_WIDTH{1'b1}};
                end else begin
                    Result = $signed(A) / $signed(B);
                end
            end
            5'b01111: begin                            // DIVU (无符号除法)
                if (B == {`DATA_WIDTH{1'b0}}) begin
                    Result = {`DATA_WIDTH{1'b1}};
                end else begin
                    Result = A / B;
                end
            end
            5'b10000: begin                            // REM (有符号余数)
                if (B == {`DATA_WIDTH{1'b0}}) begin
                    Result = A;
                end else begin
                    Result = $signed(A) % $signed(B);
                end
            end
            5'b10001: begin                          
                if (B == {`DATA_WIDTH{1'b0}}) begin
                    Result = A;
                end else begin
                    Result = A % B;
                end
            end
            
            default:  Result = {`DATA_WIDTH{1'b0}};
        endcase
    end
endmodule
    