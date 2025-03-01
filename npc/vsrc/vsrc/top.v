module ysyx_25020039_cpu (
    input wire clk,
    input wire reset
);

 
    wire [31:0] instruction;   
    wire [31:0] pc;            
    wire [31:0] alu_result;    
    wire [31:0] read_data1, read_data2;
    wire [31:0] imm;           
    wire zero, reg_write, alu_src;
    wire [3:0] alu_op;         
    reg [4:0] rs1, rs2, rd;
    wire [6:0] opcode;

    reg [31:0] pc_reg;
    always @(posedge clk or posedge reset) begin
        if (reset)
            pc_reg <= 32'b0;
        else
            pc_reg <= pc_reg + 4;
    end
    assign pc = pc_reg;

    instruction_memory imem (
        .pc(pc),
        .instruction(instruction)
    );


    ysyx_25020039_control_unit control (
        .opcode(instruction[6:0]),
        .alu_src(alu_src),
        .alu_op(alu_op),
        .reg_write(reg_write)
    );

    ysyx_25020039_regfile rf (
        .clk(clk),
        .we(reg_write),
        .read_addr1(instruction[19:15]),
        .read_addr2(instruction[24:20]),
        .write_addr(rd),
        .write_data(alu_result),
        .read_data1(read_data1),
        .read_data2(read_data2)
    );

    // ALU
    ysyx_25020039_alu alu_inst (
        .src1(read_data1),
        .src2(alu_src ? imm : read_data2),
        .alu_ctrl(alu_op),
        .result(alu_result),
        .zero(zero)
    );


    assign imm = {{20{instruction[31]}}, instruction[31:20]};


    always @(posedge clk) begin
        if (instruction[6:0] == 7'b0010011)
            rd = instruction[11:7];
    end

endmodule

