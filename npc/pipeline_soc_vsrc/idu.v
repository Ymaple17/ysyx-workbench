`include "include/define.vh"
module IDU (
    input             clock,
    input             reset,
    input      [31:0] ifu_pc,
    input      [31:0] ifu_inst,
    input             exu_flush,

    input             ifu_valid,
    output reg        idu_ready,
    input             exu_ready,
    output reg        idu_valid,

    output reg        exit,

    output reg [31:0] idu_exu_pc,
    output reg        idu_exu_RegWrite,
    output reg [ 3:0] idu_exu_rd,
    output reg [ 3:0] idu_wbu_rs1,
    output reg [ 3:0] idu_wbu_rs2,
    output reg [ 4:0] idu_exu_zimm,
    output reg [31:0] idu_exu_imm,
    output reg [ 5:0] idu_exu_shamt,
    output reg [ 3:0] idu_exu_alu_op,
    output reg [ 4:0] idu_exu_MemLen,
    output reg        idu_exu_MemWrite,
    output reg        idu_exu_MemRead,
    output reg [ 6:0] idu_exu_opcode,
    output reg [ 2:0] idu_exu_func3,

    output reg        idu_exu_jal,
    output reg        idu_exu_jalr,
    output reg        idu_exu_fencei,
    output reg        idu_exu_csr_wen1,
    output reg        idu_exu_csr_ecall,
    output reg        idu_exu_csr_mret,
    output reg [ 1:0] idu_exu_csr_op,

    output reg [11:0] idu_exu_csr_wr_addr1,
    output reg [11:0] idu_wbu_csr_addr1,
    output reg [11:0] idu_wbu_csr_addr2
);

    wire [6:0] opcode = ifu_inst[ 6: 0];
    wire [3:0] rs1    = ifu_inst[18:15];
    wire [3:0] rs2    = ifu_inst[23:20];
    wire [3:0] rd     = ifu_inst[10: 7];
    wire [2:0] func3  = ifu_inst[14:12];
    wire [5:0] shamt  = ifu_inst[25:20];
    wire [4:0] zimm   = ifu_inst[19:15];
    wire [4:0] get_opcode = opcode[ 6: 2];

    wire [31:0] imm_I   = {{20{ifu_inst[31]}}, ifu_inst[31:20]};
    wire [31:0] imm_U   = {ifu_inst[31:12], 12'b0};
    wire [31:0] imm_S   = {{20{ifu_inst[31]}}, ifu_inst[31:25], ifu_inst[11:7]};
    wire [31:0] imm_B   = {{20{ifu_inst[31]}}, ifu_inst[7], ifu_inst[30:25], ifu_inst[11:8], 1'b0};
    wire [31:0] imm_J   = {{12{ifu_inst[31]}}, ifu_inst[19:12], ifu_inst[20], ifu_inst[30:21], 1'b0};
    wire [31:0] imm_R   = 32'b0;
    wire [31:0] imm_CSR = {27'b0, ifu_inst[19:15]};
    wire        is_fencei_inst = (ifu_inst == `FENCEI_INST);
    wire        is_ecall_inst  = (ifu_inst == `INST_ECALL);
    wire        is_mret_inst   = (ifu_inst == `INST_MRET);
    wire        is_ebreak_inst = (ifu_inst == `EBREAK_INST);

    always @(*) begin
        if (reset) begin
            idu_ready = 1'b0;
        end else begin
            idu_ready = (exu_ready || ~idu_valid) && ~exu_flush;
        end
    end

    always @(posedge clock or posedge reset) begin
        if (reset) begin
            idu_valid <= 1'b0;
        end else begin
            if ((ifu_valid && idu_ready) && (exu_ready || ~idu_valid)) begin
                idu_valid <= 1'b1;
            end else if (~(ifu_valid && idu_ready) && exu_ready) begin
                idu_valid <= 1'b0;
            end
        end
    end

    always @(posedge clock) begin
        if (reset) begin
            exit               <= 1'b0;

            idu_exu_RegWrite   <= 1'b0;
            idu_exu_MemWrite   <= 1'b0;
            idu_exu_MemRead    <= 1'b0;
            idu_exu_jal        <= 1'b0;
            idu_exu_jalr       <= 1'b0;
            idu_exu_fencei     <= 1'b0;
            idu_exu_csr_wen1   <= 1'b0;
            idu_exu_csr_ecall  <= 1'b0;
            idu_exu_csr_mret   <= 1'b0;

            idu_exu_rd         <= 4'b0;
            idu_wbu_rs1        <= 4'b0;
            idu_wbu_rs2        <= 4'b0;
            idu_exu_zimm       <= 5'b0;
            idu_exu_imm        <= 32'b0;
            idu_exu_shamt      <= 6'b0;

            idu_exu_pc         <= 32'b0;

            idu_exu_alu_op     <= `ALU_ADD;
            idu_exu_MemLen     <= `Mem_Word;
            idu_exu_opcode     <= 7'b0;
            idu_exu_func3      <= 3'b0;
            idu_exu_csr_op     <= `CSR_NONE;

            idu_exu_csr_wr_addr1 <= 12'b0;
            idu_wbu_csr_addr1    <= 12'b0;
            idu_wbu_csr_addr2    <= 12'b0;
        end else if (ifu_valid && idu_ready) begin
            exit          <= is_ebreak_inst;
            idu_exu_pc    <= ifu_pc;
            idu_exu_rd    <= rd;
            idu_wbu_rs1   <= rs1;
            idu_wbu_rs2   <= rs2;
            idu_exu_zimm  <= zimm;
            idu_exu_shamt <= shamt;
            idu_exu_opcode<= opcode;
            idu_exu_func3 <= func3;

            idu_exu_imm       <= 32'b0;
            idu_exu_RegWrite  <= 1'b0;
            idu_exu_MemWrite  <= 1'b0;
            idu_exu_MemRead   <= 1'b0;
            idu_exu_alu_op    <= `ALU_ADD;
            idu_exu_MemLen    <= `Mem_Word;
            idu_exu_csr_op    <= `CSR_NONE;
            idu_exu_csr_wen1  <= 1'b0;
            idu_exu_csr_ecall <= 1'b0;
            idu_exu_csr_mret  <= 1'b0;
            idu_exu_jal       <= 1'b0;
            idu_exu_jalr      <= 1'b0;
            idu_exu_fencei    <= is_fencei_inst;
            idu_exu_csr_wr_addr1 <= 12'b0;
            idu_wbu_csr_addr1    <= 12'b0;
            idu_wbu_csr_addr2    <= 12'b0;

            case (get_opcode)
                `INST_TYPE_LUI: begin
                    idu_exu_imm      <= imm_U;
                    idu_exu_RegWrite <= 1'b1;
                end
                `INST_TYPE_AUIPC: begin
                    idu_exu_imm      <= imm_U;
                    idu_exu_RegWrite <= 1'b1;
                    idu_exu_alu_op   <= `ALU_ADD;
                end
                `INST_TYPE_JAL: begin
                    idu_exu_imm      <= imm_J;
                    idu_exu_RegWrite <= 1'b1;
                    idu_exu_jal      <= 1'b1;
                end
                `INST_TYPE_JALR: begin
                    if (func3 == 3'b000) begin
                        idu_exu_imm      <= imm_I;
                        idu_exu_RegWrite <= 1'b1;
                        idu_exu_jalr     <= 1'b1;
                    end
                end
                `INST_TYPE_S: begin
                    idu_exu_imm      <= imm_S;
                    idu_exu_MemWrite <= 1'b1;
                    case (func3)
                        `F3_SW: idu_exu_MemLen <= `Mem_Word;
                        `F3_SH: idu_exu_MemLen <= `Mem_Half;
                        `F3_SB: idu_exu_MemLen <= `Mem_Bit;
                        default: begin end
                    endcase
                end
                `INST_TYPE_L: begin
                    idu_exu_imm      <= imm_I;
                    idu_exu_RegWrite <= 1'b1;
                    idu_exu_MemRead  <= 1'b1;
                    idu_exu_alu_op   <= `ALU_ADD;
                    case (func3)
                        `F3_LW:  idu_exu_MemLen <= `Mem_Word;
                        `F3_LH:  idu_exu_MemLen <= `Mem_Half;
                        `F3_LB:  idu_exu_MemLen <= `Mem_Bit;
                        `F3_LHU: idu_exu_MemLen <= `Mem_UHalf;
                        `F3_LBU: idu_exu_MemLen <= `Mem_UBit;
                        default: begin end
                    endcase
                end
                `INST_TYPE_R: begin
                    idu_exu_imm      <= imm_R;
                    idu_exu_RegWrite <= 1'b1;
                    case (func3)
                        3'b000:   idu_exu_alu_op <= ifu_inst[30] ? `ALU_SUB : `ALU_ADD;
                        `F3_ANDI: idu_exu_alu_op <= `ALU_AND;
                        `F3_ORI:  idu_exu_alu_op <= `ALU_OR;
                        `F3_XORI: idu_exu_alu_op <= `ALU_XOR;
                        `F3_SLTU: idu_exu_alu_op <= `ALU_SLTU;
                        `F3_SLT:  idu_exu_alu_op <= `ALU_SLT;
                        `F3_RSH:  idu_exu_alu_op <= ifu_inst[30] ? `ALU_SRA : `ALU_SRL;
                        `F3_LSH:  idu_exu_alu_op <= `ALU_SLL;
                        default: begin end
                    endcase
                end
                `INST_TYPE_I: begin
                    idu_exu_imm      <= imm_I;
                    idu_exu_RegWrite <= 1'b1;
                    case (func3)
                        `F3_ADDI: idu_exu_alu_op <= `ALU_ADD;
                        `F3_ANDI: idu_exu_alu_op <= `ALU_AND;
                        `F3_ORI:  idu_exu_alu_op <= `ALU_OR;
                        `F3_SLTU: idu_exu_alu_op <= `ALU_SLTU;
                        `F3_SLTI: idu_exu_alu_op <= `ALU_SLT;
                        `F3_XORI: idu_exu_alu_op <= `ALU_XOR;
                        `F3_RSH:  idu_exu_alu_op <= ifu_inst[30] ? `ALU_SRA : `ALU_SRL;
                        `F3_LSH:  idu_exu_alu_op <= `ALU_SLL;
                        default: begin end
                    endcase
                end
                `INST_TYPE_B: begin
                    idu_exu_imm <= imm_B;
                    case (func3)
                        `F3_BEQ, `F3_BNE:   idu_exu_alu_op <= `ALU_SUB;
                        `F3_BLT, `F3_BGE:   idu_exu_alu_op <= `ALU_SLT;
                        `F3_BLTU, `F3_BGEU: idu_exu_alu_op <= `ALU_SLTU;
                        default: begin end
                    endcase
                end
                `INST_TYPE_E: begin
                    if (opcode == `INST_CSR) begin
                        idu_exu_csr_wen1 <= 1'b1;
                        idu_wbu_csr_addr1 <= ifu_inst[31:20];
                        idu_wbu_csr_addr2 <= ifu_inst[31:20];
                        case (func3)
                            `F3_CSRRW: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRW;
                            end
                            `F3_CSRRS: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRS;
                            end
                            `F3_CSRRC: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRC;
                            end
                            `F3_CSRRWI: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRW;
                                idu_exu_imm      <= imm_CSR;
                            end
                            `F3_CSRRSI: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRS;
                                idu_exu_imm      <= imm_CSR;
                            end
                            `F3_CSRRCI: begin
                                idu_exu_RegWrite <= 1'b1;
                                idu_exu_csr_op   <= `CSR_CSRRC;
                                idu_exu_imm      <= imm_CSR;
                            end
                            `F3_ECALL: begin
                                if (is_ecall_inst) begin
                                    idu_exu_csr_ecall <= 1'b1;
                                    idu_exu_csr_wen1  <= 1'b1;
                                    idu_exu_csr_wr_addr1 <= `MCAUSE; // write cause = 11
                                    idu_wbu_csr_addr1 <= `MEPC;   // save current PC
                                    idu_wbu_csr_addr2 <= `MTVEC;  // fetch trap vector
                                end else if (is_mret_inst) begin
                                    idu_exu_csr_mret <= 1'b1;
                                    idu_exu_csr_wen1  <= 1'b0;    // mret is read-only in this path
                                    idu_wbu_csr_addr1 <= `MEPC;   // restore from mepc
                                    idu_wbu_csr_addr2 <= `MEPC;
                                end
                            end
                            default: begin end
                        endcase
                        if (!is_ecall_inst && !is_mret_inst) begin
                            idu_exu_csr_wr_addr1 <= ifu_inst[31:20];
                        end
                    end
                end
                default: begin end
            endcase
        end
    end

    // Performance Monitor
    import "DPI-C" function void npc_pm_event(input int event_id, input longint data);

    wire [4:0] pm_opcode = opcode[6:2];
    wire pm_is_load = (pm_opcode == `INST_TYPE_L);
    wire pm_is_store = (pm_opcode == `INST_TYPE_S);
    wire pm_is_branch = (pm_opcode == `INST_TYPE_B);
    wire pm_is_jump = (pm_opcode == `INST_TYPE_JAL || pm_opcode == `INST_TYPE_JALR);
    wire pm_is_csr = (pm_opcode == `INST_TYPE_CSR);
    wire pm_is_compute = (pm_opcode == `INST_TYPE_I || pm_opcode == `INST_TYPE_R || pm_opcode == `INST_TYPE_LUI || pm_opcode == `INST_TYPE_AUIPC);

    always @(posedge clock) begin
        if (!reset && ifu_valid && idu_ready) begin
            if (pm_is_load) npc_pm_event(8, 1); // EVENT_INST_TYPE_LOAD
            else if (pm_is_store) npc_pm_event(9, 1); // EVENT_INST_TYPE_STORE
            else if (pm_is_csr) npc_pm_event(10, 1); // EVENT_INST_TYPE_CSR
            else if (pm_is_branch) npc_pm_event(11, 1); // EVENT_INST_TYPE_BRANCH
            else if (pm_is_jump) npc_pm_event(12, 1); // EVENT_INST_TYPE_JUMP
            else if (pm_is_compute) npc_pm_event(7, 1); // EVENT_INST_TYPE_COMPUTE
            else npc_pm_event(13, 1); // EVENT_INST_TYPE_OTHER
        end
    end

endmodule

