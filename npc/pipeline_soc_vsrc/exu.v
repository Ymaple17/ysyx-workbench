`include "include/define.vh"

module EXU (
    input             clk,
    input             reset,

    input             idu_ready,
    input             idu_valid,
    output reg        exu_ready,
    input             lsu_ready,
    output reg        exu_lsu_valid,

    input      [ 3:0] idu_wbu_rs1,
    input      [ 3:0] idu_wbu_rs2,
    // From LSU stage
    input      [ 3:0] lsu_exu_forward_rd,
    input             lsu_exu_forward_RegWrite,
    input             lsu_exu_forward_MemRead,
    // From WBU stage
    input      [31:0] lsu_wbu_wdata,
    input      [ 3:0] lsu_wbu_rd,
    input             lsu_wbu_RegWrite,
    input             lsu_wbu_valid,
    output reg        exu_lsu_forward_las,

    // Instruction Decode Interface
    input      [31:0] idu_exu_pc,
    input      [31:0] idu_exu_imm,
    input      [ 4:0] idu_exu_zimm,
    input      [ 5:0] idu_exu_shamt,
    input      [31:0] wbu_exu_src1,
    input      [31:0] wbu_exu_src2,
    input             idu_exu_RegWrite,
    input      [ 3:0] idu_exu_rd,
    input      [ 6:0] idu_exu_opcode,
    input      [ 2:0] idu_exu_func3,
    input      [ 3:0] idu_exu_alu_op,

    // Memory & Control Flow Interface
    input             idu_exu_jal,
    input             idu_exu_jalr,
    input             idu_exu_fencei,
    input             idu_exu_MemRead,
    input             idu_exu_MemWrite,
    input      [ 4:0] idu_exu_MemLen,

    // CSR Interface
    input      [31:0] wbu_exu_csr_num1,
    input      [31:0] wbu_exu_csr_num2,
    input             idu_exu_csr_wen1,
    input      [11:0] idu_exu_csr_wr_addr1,
    input             idu_exu_csr_ecall,
    input             idu_exu_csr_mret,
    input      [ 1:0] idu_exu_csr_op,

    // Branch/Flush Control Output
    output reg        exu_flush,
    output reg [31:0] exu_flush_pc,
    output reg        exu_fencei,

    // Output to LSU Stage
    output reg [31:0] exu_lsu_src2,
    output reg [31:0] exu_lsu_process_result,
    output reg [31:0] exu_lsu_pc,
    // Control signals
    output reg        exu_lsu_RegWrite,
    output reg [ 3:0] exu_lsu_rd,
    output reg        exu_lsu_MemRead,
    output reg        exu_lsu_MemWrite,
    output reg [ 4:0] exu_lsu_MemLen,
    // CSR signals
    output reg        exu_lsu_csr,
    output reg        exu_lsu_csr_wen1,
    output reg [11:0] exu_lsu_csr_wr_addr1,
    output reg [31:0] exu_lsu_csr_wr_data1,
    output reg [31:0] exu_lsu_csr_wr_data2,
    output reg [31:0] exu_lsu_csr_rdata,
    output reg        exu_lsu_csr_ecall,
    output reg        exu_lsu_csr_mret
);
    reg exu_flush_condition;
    
    // Forwarding control signals
    wire forward_from_lsu = exu_lsu_RegWrite & (|exu_lsu_rd) & exu_lsu_valid;
    wire forward_from_wbu = lsu_wbu_RegWrite & (|lsu_wbu_rd) & lsu_wbu_valid;
    wire load_in_lsu_stage = lsu_exu_forward_MemRead & lsu_exu_forward_RegWrite & (|lsu_exu_forward_rd);
    wire load_in_exu_stage = exu_lsu_MemRead & exu_lsu_RegWrite & (|exu_lsu_rd) & exu_lsu_valid;

    // RS1 forwarding priority: LSU > WBU > Load-use > Normal
    wire       fwd_rs1_from_lsu = forward_from_lsu & (exu_lsu_rd == idu_wbu_rs1);
    wire       fwd_rs1_from_wbu = forward_from_wbu & (lsu_wbu_rd == idu_wbu_rs1);
    wire       load_use_rs1_lsu = load_in_lsu_stage & (lsu_exu_forward_rd == idu_wbu_rs1);
    wire       load_use_rs1_exu = load_in_exu_stage & (exu_lsu_rd == idu_wbu_rs1);
    wire [31:0] src1 = fwd_rs1_from_lsu  ? exu_lsu_process_result :
                       fwd_rs1_from_wbu  ? lsu_wbu_wdata :
                       load_use_rs1_lsu  ? lsu_wbu_wdata : wbu_exu_src1;

    // RS2 forwarding priority: LSU > WBU > Load-use > Normal
    wire       fwd_rs2_from_lsu = forward_from_lsu & (exu_lsu_rd == idu_wbu_rs2);
    wire       fwd_rs2_from_wbu = forward_from_wbu & (lsu_wbu_rd == idu_wbu_rs2);
    wire       load_use_rs2_lsu = load_in_lsu_stage & (lsu_exu_forward_rd == idu_wbu_rs2);
    wire       load_use_rs2_exu = load_in_exu_stage & (exu_lsu_rd == idu_wbu_rs2);
    wire [31:0] src2 = fwd_rs2_from_lsu  ? exu_lsu_process_result :
                       fwd_rs2_from_wbu  ? lsu_wbu_wdata :
                       load_use_rs2_lsu  ? lsu_wbu_wdata : wbu_exu_src2;

    // Load-use hazard detection
    wire hazard_block = load_use_rs1_lsu | load_use_rs2_lsu | load_use_rs1_exu | load_use_rs2_exu;

    // Load-After-Store forwarding
    wire forward_las = idu_exu_MemWrite & exu_lsu_MemRead & exu_lsu_RegWrite & 
                       exu_lsu_valid & (|exu_lsu_rd) & 
                       (exu_lsu_rd != idu_wbu_rs1) & (exu_lsu_rd == idu_wbu_rs2);

    // Instruction Type Decoding
    wire [4:0] optype = idu_exu_opcode[6:2];
    
    wire is_lui    = (idu_exu_opcode == `INST_LUI);
    wire is_auipc  = (idu_exu_opcode == `INST_AUIPC);
    wire is_jal    = idu_exu_jal;
    wire is_jalr   = idu_exu_jalr;
    wire is_jump   = is_jal | is_jalr;
    wire is_branch = (optype == `INST_TYPE_B);
    wire is_rtype  = (optype == `INST_TYPE_R);
    wire is_itype  = (optype == `INST_TYPE_I);
    wire is_shift  = (idu_exu_alu_op == `ALU_SLL) | (idu_exu_alu_op == `ALU_SRL) | (idu_exu_alu_op == `ALU_SRA);

    
    // Shift amount calculation
    wire [4:0] shift_amt = (is_itype && !idu_exu_shamt[5]) ? idu_exu_shamt[4:0] :is_rtype ? src2[4:0] : 5'd0;

    // ALU operand A selection
    wire [31:0] alu_src_a = is_jump  ? idu_exu_pc  :
                            is_lui   ? idu_exu_imm :
                            is_auipc ? idu_exu_pc  : src1;

    // ALU operand B selection
    wire [31:0] alu_src_b = is_jump             ? 32'd4            :
                            is_lui              ? 32'd0            :
                            is_auipc            ? idu_exu_imm      :
                            is_shift            ? {27'd0, shift_amt} :
                            (is_rtype|is_branch) ? src2            : idu_exu_imm;

    // ALU Execution
    reg [31:0] alu_result;
    reg        alu_zero;
    reg        alu_less;

    wire        sign_a     = alu_src_a[31];
    wire        sign_b     = alu_src_b[31];
    wire [31:0] sub_result = alu_src_a - alu_src_b;

    always @(*) begin
        case (idu_exu_alu_op)
            `ALU_ADD:  alu_result = alu_src_a + alu_src_b;
            `ALU_SUB:  alu_result = sub_result;
            `ALU_AND:  alu_result = alu_src_a & alu_src_b;
            `ALU_OR:   alu_result = alu_src_a | alu_src_b;
            `ALU_XOR:  alu_result = alu_src_a ^ alu_src_b;
            `ALU_SLTU: alu_result = (alu_src_a < alu_src_b) ? 32'h1 : 32'h0;
            `ALU_SLT: begin
                if (sign_a != sign_b)
                    alu_result = sign_a ? 32'h1 : 32'h0;
                else
                    alu_result = sub_result[31] ? 32'h1 : 32'h0;
            end
            `ALU_SRA:  alu_result = $signed(alu_src_a) >>> alu_src_b[4:0];
            `ALU_SLL:  alu_result = alu_src_a << alu_src_b[4:0];
            `ALU_SRL:  alu_result = alu_src_a >> alu_src_b[4:0];
            default:   alu_result = 32'b0;
        endcase

        alu_zero = (alu_result == 32'b0);
        alu_less = alu_result[0];
    end

    // Branch Condition Evaluation
    wire branch_taken = is_branch && (
        (idu_exu_func3 == `F3_BEQ  &&  alu_zero) ||
        (idu_exu_func3 == `F3_BNE  && !alu_zero) ||
        (idu_exu_func3 == `F3_BLT  &&  alu_less) ||
        (idu_exu_func3 == `F3_BGE  && !alu_less) ||
        (idu_exu_func3 == `F3_BLTU &&  alu_less) ||
        (idu_exu_func3 == `F3_BGEU && !alu_less)
    );

    // Control Flow Target Calculation
    wire [31:0] pc_plus_4     = idu_exu_pc + 32'h4;
    wire [31:0] jalr_target   = (src1 + idu_exu_imm) & 32'hffff_fffe;
    wire [31:0] branch_target = idu_exu_pc + idu_exu_imm;
    wire [31:0] trap_entry    = wbu_exu_csr_num2;  // mtvec
    wire [31:0] trap_return   = wbu_exu_csr_num1;  // mepc

    // Flush request detection
    wire flush_request = idu_exu_fencei | idu_exu_jalr | idu_exu_csr_ecall | 
                         idu_exu_csr_mret | branch_taken;
    wire flush_allowed = ~hazard_block & exu_flush_condition & ~reset;

    // Target selection priority
    reg [31:0] flush_target;
    always @(*) begin
        if      (idu_exu_fencei)   flush_target = pc_plus_4;
        else if (idu_exu_jalr)     flush_target = jalr_target;
        else if (idu_exu_csr_ecall) flush_target = trap_entry;
        else if (idu_exu_csr_mret)  flush_target = trap_return;
        else if (branch_taken)     flush_target = branch_target;
        else                       flush_target = 32'h0;
    end

    // Flush output generation
    always @(*) begin
        exu_fencei   = idu_exu_fencei;
        exu_flush    = flush_allowed & flush_request;
        exu_flush_pc = flush_request ? flush_target : 32'h0;
    end

    // Flush condition management
    always @(posedge clk) begin
        if (reset)
            exu_flush_condition <= 1'b1;
        else if (exu_flush)
            exu_flush_condition <= 1'b0;
        else if (idu_valid)
            exu_flush_condition <= 1'b1;
    end

    // CSR Write Data Generation
    wire [31:0] zimm = {27'b0, idu_exu_zimm};
    reg  [31:0] csr_write_data;

    always @(*) begin
        csr_write_data = 32'b0;
        
        if (idu_exu_csr_ecall) begin
            csr_write_data = 32'd11;  // MCAUSE = 11 (ecall from M-mode)
        end else begin
            case ({idu_exu_csr_op, idu_exu_func3})
                {`CSR_CSRRW, `F3_CSRRW}:  csr_write_data = src1;
                {`CSR_CSRRS, `F3_CSRRS}:  csr_write_data = wbu_exu_csr_num1 | src1;
                {`CSR_CSRRC, `F3_CSRRC}:  csr_write_data = wbu_exu_csr_num1 & ~src1;
                {`CSR_CSRRW, `F3_CSRRWI}: csr_write_data = zimm;
                {`CSR_CSRRS, `F3_CSRRSI}: csr_write_data = wbu_exu_csr_num1 | zimm;
                {`CSR_CSRRC, `F3_CSRRCI}: csr_write_data = wbu_exu_csr_num1 & ~zimm;
                default:                  csr_write_data = 32'b0;
            endcase
        end
    end

    // Pipeline Control Logic
    wire lsu_can_accept = lsu_ready | ~exu_lsu_valid;
    wire exu_ready_comb = lsu_can_accept & ~hazard_block;
    wire issue_to_lsu   = idu_valid & exu_ready_comb;
    wire retire_from_exu = ~issue_to_lsu & lsu_ready;

    always @(*) begin
        exu_ready = reset ? 1'b0 : exu_ready_comb;
    end

    // EXU Valid Control
    always @(posedge clk) begin
        if (reset)
            exu_lsu_valid <= 1'b0;
        else if (issue_to_lsu)
            exu_lsu_valid <= 1'b1;
        else if (retire_from_exu)
            exu_lsu_valid <= 1'b0;
    end

    // EXU->LSU Pipeline Registers
    always @(posedge clk) begin
        if (reset) begin
            // Data path
            exu_lsu_src2           <= 32'h0;
            exu_lsu_process_result <= 32'h0;
            exu_lsu_pc             <= 32'b0;
            
            // Control signals
            exu_lsu_RegWrite       <= 1'b0;
            exu_lsu_rd             <= 4'b0;
            exu_lsu_MemRead        <= 1'b0;
            exu_lsu_MemWrite       <= 1'b0;
            exu_lsu_MemLen         <= 5'b0;
            exu_lsu_forward_las    <= 1'b0;
            
            // CSR signals
            exu_lsu_csr            <= 1'b0;
            exu_lsu_csr_wen1       <= 1'b0;
            exu_lsu_csr_wr_addr1   <= 12'b0;
            exu_lsu_csr_wr_data1   <= 32'h0;
            exu_lsu_csr_wr_data2   <= 32'h0;
            exu_lsu_csr_rdata      <= 32'h0;
            exu_lsu_csr_ecall      <= 1'b0;
            exu_lsu_csr_mret       <= 1'b0;
        end else if (issue_to_lsu) begin
            // Data path
            exu_lsu_src2           <= src2;
            exu_lsu_process_result <= alu_result;
            exu_lsu_pc             <= idu_exu_pc;
            
            // Control signals
            exu_lsu_RegWrite       <= idu_exu_RegWrite;
            exu_lsu_rd             <= idu_exu_rd;
            exu_lsu_MemRead        <= idu_exu_MemRead;
            exu_lsu_MemWrite       <= idu_exu_MemWrite;
            exu_lsu_MemLen         <= idu_exu_MemLen;
            exu_lsu_forward_las    <= forward_las;
            
            // CSR signals
            exu_lsu_csr            <= idu_exu_csr_wen1 | idu_exu_csr_ecall | idu_exu_csr_mret;
            exu_lsu_csr_wen1       <= idu_exu_csr_wen1;
            exu_lsu_csr_wr_addr1   <= idu_exu_csr_wr_addr1;
            exu_lsu_csr_wr_data1   <= csr_write_data;
            exu_lsu_csr_wr_data2   <= idu_exu_csr_ecall ? pc_plus_4 : 32'b0;
            exu_lsu_csr_rdata      <= wbu_exu_csr_num1;
            exu_lsu_csr_ecall      <= idu_exu_csr_ecall;
            exu_lsu_csr_mret       <= idu_exu_csr_mret;
        end
    end

    // Performance Monitor
    import "DPI-C" function void npc_pm_event(input int event_id, input longint data);

    always @(posedge clk) begin
        if (!reset && exu_lsu_valid && lsu_ready) begin
            npc_pm_event(6, 1); // EVENT_EXU_COMP
        end
    end

endmodule