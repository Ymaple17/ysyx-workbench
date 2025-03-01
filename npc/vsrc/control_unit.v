
module ysyx_25020039_control_unit (
    input wire [6:0] opcode,       // 操作码
    output reg reg_write,          // 是否写寄存器
    output reg alu_src,            // ALU 输入源
    //output reg mem_to_reg,         // 数据来自内存
    //output reg mem_write,          // 是否写入内存
    //output reg branch,             // 是否跳转
    output reg [3:0] alu_op        // ALU 操作类型
);

    always @(*) begin
        case (opcode)
            7'b0110011: begin  // R-type
                reg_write = 1;
                alu_src = 0;
                //mem_to_reg = 0;
                //mem_write = 0;
                //branch = 0;
                alu_op = 4'b0000;
            end
            7'b0000011: begin  // I-type
                reg_write = 1;
                alu_src = 1;
                //mem_to_reg = 1;
                //mem_write = 0;
                //branch = 0;
                alu_op = 4'b0000;
            end
            7'b0100011: begin  // S-type
                reg_write = 0;
                alu_src = 1;
                //mem_to_reg = 0;
                //mem_write = 1;
                //branch = 0;
                alu_op = 4'b0000;
            end
            7'b1100011: begin  // B-type
                reg_write = 0;
                alu_src = 0;
                //mem_to_reg = 0;
                //mem_write = 0;
                //branch = 1;
                alu_op = 4'b0001;
            end
            default: begin
                reg_write = 0;
                alu_src = 0;
                //mem_to_reg = 0;
                //mem_write = 0;
                //branch = 0;
                alu_op = 4'b0000;
            end
        endcase
    end
endmodule

