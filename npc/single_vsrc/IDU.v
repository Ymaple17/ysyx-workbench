`include "include/defs.vh"

module IDU (
    input [`DATA_WIDTH-1:0] inst_i,
    input [`DATA_WIDTH-1:0] rs1_data_i,
    input [`DATA_WIDTH-1:0] rs2_data_i,
    input [`DATA_WIDTH-1:0] pc_i,
    output [`REG_ADDR_WIDTH-1:0] rs1_addr_o,
    output [`REG_ADDR_WIDTH-1:0] rs2_addr_o,
    output [`REG_ADDR_WIDTH-1:0] rd_addr_o,//目的寄存器
    output [`DATA_WIDTH-1:0] Op1_o,
    output [`DATA_WIDTH-1:0] Op2_o,
    output [`DATA_WIDTH-1:0] jump_reg_target_o,
    output [`DATA_WIDTH-1:0] br_target_o,
    output [`DATA_WIDTH-1:0] jmp_target_o,
    output [3:0]             alu_op_o,
    output [2:0]             pc_sel_o,
    output                   rf_we_o,//regfile
    output                   mem_en_o,//memory en
    output                   mem_wen_o,//memory write
    output [1:0]             wb_sel_o,//wb en
    output                   is_ebreak_o,
    output [2:0]             ctl_mem_access_o,
    output                   is_csr_instr_o,
    output [2:0]             csr_op_o,
    output [11:0]            csr_addr_o,
    output                   is_ecall_o,
    output                   is_mret_o
);

    wire [`REG_ADDR_WIDTH-1:0] wb_addr = inst_i[11:7];
    assign rs1_addr_o = inst_i[19:15];
    assign rs2_addr_o = inst_i[24:20];

    wire [11:0] imm_i = inst_i[31:20];
    wire [11:0] imm_s = {inst_i[31:25], inst_i[11:7]};
    wire [11:0] imm_b = {inst_i[31], inst_i[7], inst_i[30:25], inst_i[11:8]};
    wire [19:0] imm_u = inst_i[31:12];
    wire [19:0] imm_j = {inst_i[31], inst_i[19:12], inst_i[20], inst_i[30:21]};

    assign is_ecall_o = (inst_i == 32'h0000_0073);
    assign is_mret_o = (inst_i[31:25] == 7'b0011000) &&
                       (inst_i[24:20] == 5'b00010) &&
                       (inst_i[14:12] == 3'b000)&&
                       (inst_i[6:0] == 7'b1110011);

    wire [`DATA_WIDTH-1:0] imm_i_sext;
    wire [`DATA_WIDTH-1:0] imm_s_sext;
    wire [`DATA_WIDTH-1:0] imm_b_sext;
    wire [`DATA_WIDTH-1:0] imm_u_sext;
    wire [`DATA_WIDTH-1:0] imm_j_sext;

    assign imm_i_sext = {{20{imm_i[11]}}, imm_i}; 
    assign imm_s_sext = {{20{imm_s[11]}}, imm_s}; 
    assign imm_b_sext = {{19{imm_b[11]}}, imm_b, 1'b0}; 
    assign imm_u_sext = {imm_u, 12'b0}; 
    assign imm_j_sext = {{11{imm_j[19]}}, imm_j, 1'b0};

    assign jump_reg_target_o = rs1_data_i + imm_i_sext;
    assign br_target_o       = pc_i + imm_b_sext;
    assign jmp_target_o      = pc_i + imm_j_sext;

    wire br_eq, br_lt, br_ltu;
    assign br_eq  = (rs1_data_i == rs2_data_i);
    assign br_lt  = ($signed(rs1_data_i) < $signed(rs2_data_i));
    assign br_ltu = (rs1_data_i < rs2_data_i);

    assign rd_addr_o = wb_addr;
    assign csr_addr_o = inst_i[31:20];

    reg [1:0] op1_sel;
    reg [1:0] op2_sel;
    reg is_ebreak;

    MuxKey #(2,2,`DATA_WIDTH) op1_sel_mux (
        .out(Op1_o),
        .key(op1_sel),
        .lut({
            2'b00, rs1_data_i,
            2'b01, imm_u_sext
        })
    );

    MuxKey #(4,2,`DATA_WIDTH) op2_sel_mux (
        .out(Op2_o),
        .key(op2_sel),
        .lut({
            2'b00, pc_i,
            2'b01, imm_i_sext,
            2'b10, imm_s_sext,
            2'b11, rs2_data_i
        })
    );

    localparam DATA_LEN = 17;
    localparam KEY_LEN = 17;
    localparam NR_KEY = 42;

    wire [6:0] opcode = inst_i[6:0];
    wire [2:0] funct3 = inst_i[14:12];
    wire [6:0] funct7 = inst_i[31:25];

    reg [KEY_LEN-1:0] inst_key;

    always @(*) begin
        case (opcode)
            7'b1110011: begin
                inst_key = {opcode, funct3, 7'b0};
            end  
            7'b1100111: begin
                case (funct3)
                    3'b000: inst_key = {opcode, funct3, 7'b0};
                    default: inst_key = {opcode, funct3, 7'b1101111};
                endcase
            end
            7'b0010011: begin
                case (funct3)
                    3'b101: inst_key = {opcode, funct3, funct7};
                    default: inst_key = {opcode, funct3, 7'b0000000};
                endcase
            end
            7'b0010111, 7'b1101111, 7'b0110111: begin
                inst_key = {opcode, 3'b0, 7'b0};  
            end
            7'b0100011, 7'b0000011, 7'b1100011: begin
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
        //alu_op[15:12],op1_sel[11:10],op2_sel[9:8],rf_we[4],mem_en[3],mem_wen[2],wb_sel[1:0]
        // R-type instructions
        17'b0110011_000_0000000, 17'b00000_00_11_000_1_0_0_10, // ADD
        17'b0110011_000_0100000, 17'b00001_00_11_000_1_0_0_10, // SUB
        17'b0110011_001_0000000, 17'b00111_00_11_000_1_0_0_10, // SLL
        17'b0110011_010_0000000, 17'b00010_00_11_000_1_0_0_10, // SLT
        17'b0110011_011_0000000, 17'b00011_00_11_000_1_0_0_10, // SLTU
        17'b0110011_100_0000000, 17'b00100_00_11_000_1_0_0_10, // XOR
        17'b0110011_101_0000000, 17'b01000_00_11_000_1_0_0_10, // SRL
        17'b0110011_101_0100000, 17'b01001_00_11_000_1_0_0_10, // SRA
        17'b0110011_110_0000000, 17'b00101_00_11_000_1_0_0_10, // OR
        17'b0110011_111_0000000, 17'b00110_00_11_000_1_0_0_10, // AND

        // I-type instructions
        17'b0010011_000_0000000, 17'b00000_00_01_000_1_0_0_10, // ADDI
        17'b0010011_010_0000000, 17'b00010_00_01_000_1_0_0_10, // SLTI
        17'b0010011_011_0000000, 17'b00011_00_01_000_1_0_0_10, // SLTIU
        17'b0010011_100_0000000, 17'b00100_00_01_000_1_0_0_10, // XORI
        17'b0010011_110_0000000, 17'b00101_00_01_000_1_0_0_10, // ORI
        17'b0010011_111_0000000, 17'b00110_00_01_000_1_0_0_10, // ANDI
        17'b0010011_001_0000000, 17'b00111_00_01_000_1_0_0_10, // SLLI
        17'b0010011_101_0000000, 17'b01000_00_01_000_1_0_0_10, // SRLI
        17'b0010011_101_0100000, 17'b01001_00_01_000_1_0_0_10, // SRAI
        17'b0000011_010_0000000, 17'b00000_00_01_000_1_1_0_11, // LW
        17'b0000011_000_0000000, 17'b00000_00_01_000_1_1_0_11, // LB
        17'b0000011_100_0000000, 17'b00000_00_01_000_1_1_0_11, // LBU
        17'b0000011_001_0000000, 17'b00000_00_01_000_1_1_0_11, // LH
        17'b0000011_101_0000000, 17'b00000_00_01_000_1_1_0_11, // LHU

        // B-type instructions
        17'b1100011_000_0000000, 17'b00000_00_00_000_0_0_0_10, // BEQ
        17'b1100011_001_0000000, 17'b00000_00_00_000_0_0_0_10, // BNE
        17'b1100011_100_0000000, 17'b00000_00_00_000_0_0_0_10, // BLT
        17'b1100011_101_0000000, 17'b00000_00_00_000_0_0_0_10, // BGE
        17'b1100011_110_0000000, 17'b00000_00_00_000_0_0_0_10, // BLTU
        17'b1100011_111_0000000, 17'b00000_00_00_000_0_0_0_10, // BGEU

        // J-type instructions
        17'b1101111_000_0000000, 17'b00000_00_00_011_1_0_0_01, // JAL

        // U-type instructions
        17'b0010111_000_0000000, 17'b00000_01_00_000_1_0_0_10, // AUIPC
        17'b0110111_000_0000000, 17'b01010_01_00_000_1_0_0_10, // LUI

        // S-type instructions
        17'b0100011_010_0000000, 17'b00000_00_10_000_0_1_1_00, // SW
        17'b0100011_000_0000000, 17'b00000_00_10_000_0_1_1_00, // SB
        17'b0100011_001_0000000, 17'b00000_00_10_000_0_1_1_00, // SH

        // JALR instruction
        17'b1100111_000_0000000, 17'b00000_00_01_001_1_0_0_01, // JALR

        // ebreak instruction
        17'b1110011_000_0000000, 17'b00000_00_00_000_0_0_0_00,  // EBREAK

        // CSR指令
        17'b1110011_001_0000000, 17'b00000_00_11_000_1_0_0_10, // CSRRW
        17'b1110011_010_0000000, 17'b00000_00_11_000_1_0_0_10, // CSRRS
        17'b1110011_011_0000000, 17'b00000_00_11_000_1_0_0_10, // CSRRC
        17'b1110011_000_0011000, 17'b00000_00_00_000_0_0_0_00
        })
    );

    assign alu_op_o = ctl_signals[15:12];
    always @(*) begin
        op1_sel = ctl_signals[11:10];
        op2_sel = ctl_signals[9:8];
    end    
    assign rf_we_o = ctl_signals[4];
    assign mem_en_o = ctl_signals[3];
    assign mem_wen_o = ctl_signals[2];
    assign wb_sel_o = ctl_signals[1:0];
    assign is_ebreak = (inst_i == 32'h00100073); // EBREAK
    assign is_ebreak_o = is_ebreak;

    assign is_csr_instr_o = (opcode == 7'b1110011) && !is_ebreak && !is_mret_o;
    assign csr_op_o = inst_i[14:12];

    reg [2:0] pc_sel;
    always @(*) begin
        // Branch control
        if (opcode == 7'b1100011) begin
            case (funct3)
                3'b000: pc_sel = br_eq ? 3'b010 :3'b000; // BEQ
                3'b001: pc_sel = ~br_eq? 3'b010 :3'b000; // BNE
                3'b100: pc_sel = br_lt ? 3'b010 :3'b000; // BLT
                3'b101: pc_sel = ~br_lt ? 3'b010 :3'b000; // BGE
                3'b110: pc_sel = br_ltu? 3'b010 :3'b000; // BLTU
                3'b111: pc_sel = ~br_ltu? 3'b010 :3'b000; // BGEU
                default: pc_sel = 3'b000;
            endcase
        end else begin
            // non-branch control
            pc_sel = ctl_signals[7:5];
        end
    end
    assign pc_sel_o = pc_sel;

    // Memory access control
    MuxKey #(8, 10, 3) mem_acces_ctl_mux (
        .out(ctl_mem_access_o),
        .key({opcode, funct3}),
        .lut({
            10'b0000011_010, 3'b010, // LW
            10'b0000011_000, 3'b000, // LB
            10'b0000011_100, 3'b100, // LBU
            10'b0000011_001, 3'b001, // LH
            10'b0000011_101, 3'b101, // LHU
            10'b0100011_010, 3'b010, // SW
            10'b0100011_000, 3'b000, // SB
            10'b0100011_001, 3'b001  // SH
        })
    );

endmodule
    
