module Top (
    input  wire        clock,
    input  wire        reset
);
    // DPI-C function declarations
    import "DPI-C" function void dpic_ebreak(input bit [31:0] pc, input bit [31:0] code);
    import "DPI-C" function void dpic_commit(input bit [31:0] pc, input bit [31:0] inst, 
                                             input bit [31:0] npc, input byte dmAccess);
    import "DPI-C" function void paddr_read(input bit [31:0] addr, output bit [31:0] data);
    import "DPI-C" function void paddr_write(input bit [31:0] addr, input bit [31:0] data, input byte mask);

    // Internal signals
    reg  [31:0] pc;
    wire [31:0] next_pc;
    reg  [31:0] instr;

    // Instruction decoding
    wire [2:0]  funct3 = instr[14:12];
    wire [11:0] imm_i  = instr[31:20];
    wire [11:0] imm_s  = {instr[31:25], instr[11:7]};
    wire [19:0] imm_u  = instr[31:12];                   // U-type immediate (LUI, AUIPC)
    wire [20:1] imm_j  = {instr[31], instr[19:12], instr[20], instr[30:21]}; // J-type immediate (JAL)
    wire [31:0] imm_i_sext = {{20{imm_i[11]}}, imm_i};
    wire [31:0] imm_s_sext = {{20{imm_s[11]}}, imm_s};
    wire [31:0] imm_u_sext = {imm_u, 12'b0};             // Shifted left by 12
    wire [31:0] imm_j_sext = {{11{imm_j[20]}}, imm_j, 1'b0}; // Sign-extended JAL offset

    // Register file signals
    wire [4:0]  rs1 = instr[19:15];
    wire [4:0]  rs2 = instr[24:20];
    wire [4:0]  rd  = instr[11:7];
    wire [31:0] rs1_data, rs2_data, rd_data;
    wire        reg_we;

    // ALU signals
    wire [31:0] alu_a, alu_b, alu_out;

    // Memory signals
    wire [31:0] mem_addr, mem_wdata, mem_rdata;
    wire [7:0]  mem_wmask;
    wire        mem_read, mem_write;
    wire        dm_access = mem_read || mem_write;

    // Control signals
    wire        is_ebreak, is_auipc, is_lui, is_jal, is_jalr;

    // Module instantiations
    RegFile regfile (
        .clock(clock),
        .we(reg_we),
        .rs1_addr(rs1),
        .rs2_addr(rs2),
        .rd_addr(rd),
        .rd_data(rd_data),
        .rs1_data(rs1_data),
        .rs2_data(rs2_data)
    );

    ALU alu (
        .a(alu_a),
        .b(alu_b),
        .result(alu_out)
    );

    ControlUnit ctrl (
        .instr(instr),
        .mem_read(mem_read),
        .mem_write(mem_write),
        .reg_write(reg_we),
        .is_ebreak(is_ebreak),
        .is_auipc(is_auipc),
        .is_lui(is_lui),
        .is_jal(is_jal),
        .is_jalr(is_jalr)
    );

    MemoryInterface mem_if (
        .clock(clock),
        .mem_read(mem_read),
        .mem_write(mem_write),
        .addr(mem_addr),
        .wdata(mem_wdata),
        .wmask(mem_wmask),
        .rdata(mem_rdata)
    );

    // ALU input selection
    assign alu_a = (is_auipc) ? pc : rs1_data;
    assign alu_b = (is_auipc) ? imm_u_sext : 
                   (instr[6:0] == 7'b0010011) ? imm_i_sext : rs2_data;

    // Memory access
    assign mem_addr = (mem_read || mem_write) ? (rs1_data + (mem_read ? imm_i_sext : imm_s_sext)) : pc;
    assign mem_wdata = rs2_data;
    assign mem_wmask = (funct3 == 3'b010) ? 8'h0f :        // Word
                       (funct3 == 3'b001) ? 8'h03 :        // Half-word
                       (funct3 == 3'b000) ? 8'h01 : 8'h00; // Byte

    // Write back to register
    assign rd_data = (is_lui) ? imm_u_sext :
                     (is_jal || is_jalr) ? (pc + 4) :
                     (mem_read) ? mem_rdata : alu_out;

    // Next PC calculation
    assign next_pc = (is_jal) ? (pc + imm_j_sext) :
                     (is_jalr) ? (rs1_data + imm_i_sext) & 32'hFFFFFFFE : // Clear LSB
                     (pc + 4);

    // Main execution logic
    always @(posedge clock or posedge reset) begin
        if (reset) begin
            pc <= 32'h80000000; // Reset vector
            instr <= 32'b0;     // 非阻塞赋值
        end else begin
            // Fetch instruction
            reg [31:0] temp_instr; // 临时变量，用于接收 paddr_read 的输出
            paddr_read(pc, temp_instr); // 使用临时变量接收指令
            instr <= temp_instr;   // 非阻塞赋值给 instr

            // Commit every instruction
            dpic_commit(pc, instr, next_pc, dm_access);

            // Execute
            if (is_ebreak) begin
                dpic_ebreak(pc, 32'h0); // Call DPI-C ebreak with code=0
            end

            // Update PC
            pc <= next_pc;
        end
    end

endmodule
