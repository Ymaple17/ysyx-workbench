module ysyx_25020039_alu (
    input wire [31:0] src1, src2,     // 操作数
    input wire [3:0] alu_ctrl,         // ALU 控制信号
    output reg [31:0] result,          // 结果输出
    output reg zero                    // 是否为零
);

    always @(*) begin
        case (alu_ctrl)
            4'b0000: result = src1 + src2;   // ADD
            4'b0001: result = src1 - src2;   // SUB
            4'b0010: result = src1 & src2;   // AND
            4'b0011: result = src1 | src2;   // OR
            4'b0100: result = src1 ^ src2;   // XOR
            4'b0101: result = (src1 < src2) ? 1 : 0;  // SLT
            4'b0110: result = src1 << src2[4:0]; // SLL
            4'b0111: result = src1 >> src2[4:0]; // SRL
            default: result = 32'b0;
        endcase

        zero = (result == 0);
    end
endmodule

