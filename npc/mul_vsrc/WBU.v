`include "/home/qiu/ysyx-workbench/npc/mul_vsrc/include/defs.vh"

module WBU (
    input              clk,
    input              reset,
    input              mem_valid,
    input              if_ready,
    input       [6:0]  opcode,
    input       [2:0]  func3,
    input       [4:0]  id_rd,
    input              id_RegWrite,
    input       [4:0]  rs1,
    input       [4:0]  rs2,
    input       [31:0] pc,
    input       [31:0] imm,
    input              alu_zero,
    input              alu_less,
    input       [31:0] alu_result,
    input       [31:0] data_out,
    input              csr_en,
    input       [1:0]  csr_op,
    input              csr_imm,
    input       [4:0]  csr_zimm,
    input              csr_ecall,
    input              csr_mret,
    input       [31:0] csr_rdata,
    input       [31:0] csr_mret_pc,
    input       [31:0] csr_ecall_pc,
    output wire [31:0] rs1_val,
    output wire [31:0] rs2_val,
    output reg         wb_ready,
    output reg         wb_valid,
    output reg [31:0]  jal_target,
    output reg [31:0]  jalr_target,
    output reg         is_jal,
    output reg         is_jalr,
    output reg         take_branch,
    output reg [31:0]  wb_data,
    output reg         csr_write,
    output reg [31:0]  csr_wdata,
    output reg [1:0]   csr_op_exec,
    output reg         csr_ecall_req,
    output reg         csr_mret_req,
    output reg         csr_trap,
    output reg [31:0]  csr_target_pc
);
    typedef enum {IDLE, STALL} state_t;
    state_t state;

    reg        RegWrite_wb;
    reg [4:0]  rd_wb;
    reg [4:0]  rd_wb_pre;
    reg [31:0] regs [0:31];
    assign rs1_val = (rs1 != 0) ? regs[rs1] : 0;
    assign rs2_val = (rs2 != 0) ? regs[rs2] : 0;

    wire [31:0] csr_src_data = csr_imm ? {{27{1'b0}}, csr_zimm} : rs1_val;
    wire        csr_has_write = csr_en && (
                                (csr_op == `CSR_CSRRW) ? 1'b1 :
                                ((csr_op == `CSR_CSRRS) || (csr_op == `CSR_CSRRC)) ? (csr_src_data != 32'b0) :
                                1'b0
                              );

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            state <= STALL;
            wb_ready = 1'b1;
            wb_valid = 1'b1;
            jal_target = 32'h0;
            jalr_target = 32'h0;
            is_jal = 1'b0;
            is_jalr = 1'b0;
            take_branch = 1'b0;
            wb_data = 32'h0;
            csr_write = 1'b0;
            csr_wdata = 32'h0;
            csr_op_exec = `CSR_NONE;
            csr_ecall_req = 1'b0;
            csr_mret_req = 1'b0;
            csr_trap = 1'b0;
            csr_target_pc = 32'h0;
            for(integer i = 0; i < 32; i = i + 1) begin
                regs[i] <= 32'h0;
            end
        end else begin
            case (state)
                IDLE: begin
                    wb_ready = 1'b1;
                    wb_valid = 1'b0;
                    csr_write = 1'b0;
                    csr_wdata = 32'h0;
                    csr_op_exec = `CSR_NONE;
                    csr_ecall_req = 1'b0;
                    csr_mret_req = 1'b0;
                    csr_trap = 1'b0;
                    csr_target_pc = 32'h0;
                    if(mem_valid) begin
                        wb_ready = 1'b0;
                        wb_valid = 1'b0;
                        jal_target = pc + imm;
                        jalr_target = (rs1_val + imm) & ~32'h1;
                        is_jal = (opcode == `INST_JAL);
                        is_jalr = (opcode == `INST_JALR) & (func3 == 3'b000);
                        take_branch = (opcode == `INST_B) && (
                            (func3 == `F3_BNE && !alu_zero) || // bne
                            (func3 == `F3_BEQ && alu_zero) ||  // beq
                            (func3 == `F3_BLT && alu_less) ||  // blt
                            (func3 == `F3_BGE && !alu_less) || // bge
                            (func3 == `F3_BLTU && alu_less) || // bltu
                            (func3 == `F3_BGEU && !alu_less)   // bgeu
                        );

                        // 写回数据选择
                        wb_data = (opcode == `INST_LUI) ? imm :// LUI
                                        (opcode == `INST_AUIPC) ? (pc + imm) :// AUIPC
                                        (opcode == `INST_JAL || opcode == `INST_JALR) ? (pc + 4) : // JAL, JALR
                                        (opcode == `INST_LW) ? data_out :// LW
                                        (opcode == `INST_R || opcode == `INST_I) ? alu_result : 32'b0;
                        if (csr_en) begin
                            wb_data = csr_rdata;
                            is_jal = 1'b0;
                            is_jalr = 1'b0;
                            take_branch = 1'b0;
                        end
                        csr_write = csr_has_write;
                        csr_wdata = csr_src_data;
                        csr_op_exec = csr_has_write ? csr_op : `CSR_NONE;
                        csr_ecall_req = csr_ecall;
                        csr_mret_req = csr_mret;
                        if (csr_ecall) begin
                            csr_trap = 1'b1;
                            csr_target_pc = csr_ecall_pc;
                        end else if (csr_mret) begin
                            csr_trap = 1'b1;
                            csr_target_pc = csr_mret_pc;
                        end
                        //=====写回数据=====
                        rd_wb = id_rd;
                        rd_wb_pre = rd_wb;
                        RegWrite_wb = id_RegWrite;
                        if(RegWrite_wb && rd_wb_pre != 0) begin
                            regs[rd_wb_pre] <= wb_data;
                        end
                        state <= if_ready ? STALL : IDLE;
                    end else begin
                        state <= IDLE;
                    end
                end
                STALL: begin
                    wb_ready = 1'b0;
                    wb_valid = 1'b1;
                    csr_write = 1'b0;
                    csr_op_exec = `CSR_NONE;
                    csr_ecall_req = 1'b0;
                    csr_mret_req = 1'b0;
                    state <= if_ready ? IDLE : STALL;
                end
                default: begin
                    wb_ready = 1'b0;
                    wb_valid = 1'b0;
                    state <= IDLE;
                end
            endcase
        end
    end

endmodule