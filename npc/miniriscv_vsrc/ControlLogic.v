// Control logic for RV32 instructions 
`include "include/defs.vh"

module ControlLogic (
    input  [31:0] inst,       // RV32 instruction input
    output [4:0]  alu_op,     // ALU operation type
    output [1:0]  op1_sel,    // ALU operand 1 selection
    output [1:0]  op2_sel,    // ALU operand 2 selection
    output reg [2:0]  pc_sel,     // PC selection (next PC value)
    output        rf_we,      // Register file write enable
    output        mem_en,     // Memory enable 
    output        mem_wen,    // Memory write enable 
    output [1:0]  wb_sel,     // Write-back source selection
    output        is_ebreak,  // Flag for ebreak instruction
    output [2:0]  ctl_mem_access
);

    localparam DATA_LEN  = 17;  // Length of control signals
    localparam KEY_LEN   = 17;  // Length of inst key
    localparam NR_KEY    = 9;  // Number of keys  8+1

    wire [6:0] opcode = inst[6:0];
    wire [2:0] funct3 = inst[14:12];
    wire [6:0] funct7 = inst[31:25];

    reg [KEY_LEN-1:0] inst_key;
    
    always @(*) begin
        case (opcode)
            7'b1110011: begin
                inst_key = {opcode,funct3,funct7};
            end  
            7'b1100111: begin
                case (funct3)
                    3'b000: inst_key = {opcode, funct3, 7'b0};  // opcode == 7'b1100111 && funct3 == 3'b000
                    default: inst_key = {opcode, funct3, 7'b1101111};  // 默认处理其他 funct3
                endcase
            end
            7'b0010011: begin
                case (funct3)
                    3'b101: inst_key = {opcode, funct3, funct7};  // opcode == 7'b0010011 && funct3 == 3'b101
                    default: inst_key = {opcode, funct3, 7'b0000000};  //  funct3
                endcase
            end
            7'b0010111: begin
                inst_key = {opcode, 3'b0, 7'b0};  
            end
            7'b1101111: begin
                inst_key = {opcode, 3'b0, 7'b0};  
            end
            7'b0110111: begin
                inst_key = {opcode, 3'b0, 7'b0};  
            end
            7'b0100011: begin
                inst_key = {opcode, funct3, 7'b0}; 
            end
            7'b0000011: begin
                inst_key = {opcode, funct3, 7'b0};  
            end
            7'b1100011: begin
                inst_key = {opcode, funct3, 7'b0};  
            end
            default: begin
                inst_key = {opcode, funct3, funct7};
            end
        endcase
    end

    wire [DATA_LEN-1:0] ctl_signals;
    MuxKey #(NR_KEY, KEY_LEN, DATA_LEN) funct_mux (
        .out(ctl_signals),
        .key(inst_key),
        .lut({
        // opcode_func3_func7 | {alu_op, op1_sel, op2_sel, pc_sel, rf_we, mem_val, mem_wen, wb_sel}
        // R-type instructions(10)
        17'b0110011_000_0000000, 17'b00000_00_11_000_1_0_0_10, // ADD
        // I-type instructions(14)
        17'b0010011_000_0000000, 17'b00000_00_01_000_1_0_0_10, // ADDI
        17'b0000011_010_0000000, 17'b00000_00_01_000_1_1_0_11, // LW
        17'b0000011_100_0000000, 17'b00000_00_01_000_1_1_0_11, // LBU
        // U-type instructions(2)
        17'b0110111_000_0000000, 17'b01010_01_00_000_1_0_0_10, // LUI
        // S-type instructions(3)
        17'b0100011_010_0000000, 17'b00000_00_10_000_0_1_1_00, // SW
        17'b0100011_000_0000000, 17'b00000_00_10_000_0_1_1_00, // SB
        // JALR instruction(1)
        17'b1100111_000_0000000, 17'b00000_00_01_001_1_0_0_01, // JALR
        // ebreak instruction(1)
        17'b1110011_000_0000000, 17'b00000_00_00_000_0_0_0_00  // EBREAK
        })
    );

    //wire is_ebreak_internal = (inst == 32'b00000000000100000000000001110011);
    wire is_ebreak_internal = (inst == 32'h00100073);
    assign is_ebreak = is_ebreak_internal;

    // Decode control signals
    assign alu_op   = ctl_signals[16:12];
    assign op1_sel  = ctl_signals[11:10];
    assign op2_sel  = ctl_signals[9:8];
    assign rf_we    = ctl_signals[4];
    assign mem_en   = ctl_signals[3];
    assign mem_wen  = ctl_signals[2];
    assign wb_sel   = ctl_signals[1:0];

    always @(*) begin
        pc_sel = ctl_signals[7:5];
    end

    MuxKey #(4, 10, 3) mem_acces_ctl_mux (
    .out(ctl_mem_access),
    .key({opcode, funct3}),
    .lut({
        // opcode_func3 | ctl_mem_access
        10'b0000011_010, 3'b010, // LW
        10'b0000011_100, 3'b100, // LBU
        10'b0100011_010, 3'b010, // SW
        10'b0100011_000, 3'b000 // SB
    })
    );

endmodule

    