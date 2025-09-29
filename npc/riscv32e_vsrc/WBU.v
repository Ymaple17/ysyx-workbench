`include "include/defs.vh"

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
    input       [31:0] alu_result,
    input       [31:0] data_out,
    output wire [31:0] rs1_val,
    output wire [31:0] rs2_val,
    output reg         wb_ready,
    output reg         wb_valid,
    output reg [31:0]  jalr_target,
    output reg         is_jalr,
    output reg [31:0]  wb_data,
    input       [11:0] csr_addr
);
    typedef enum {IDLE, STALL} state_t;
    state_t state, next_state;

    reg        RegWrite_wb;
    reg [4:0]  rd_wb;
    reg [31:0] regs [0:31];
    assign rs1_val = (rs1 != 0) ? regs[rs1] : 0;
    assign rs2_val = (rs2 != 0) ? regs[rs2] : 0;

    wire [31:0] csr_rdata;
    CSR csr(
        .clk(clk),
        .rst(reset),
        .csr_addr(csr_addr),
        .csr_rdata(csr_rdata)
    );

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            state = STALL;
            wb_ready = 1'b1;
            wb_valid = 1'b1;
            jalr_target = 32'h0;
            is_jalr = 1'b0;
            wb_data = 32'h0;
            for(integer i = 0; i < 32; i = i + 1) begin
                regs[i] <= 32'h0;
            end
        end else begin
            state = next_state;
            case (state)
                IDLE: begin
                    wb_ready = 1'b1;
                    wb_valid = 1'b0;
                    if(mem_valid) begin
                        wb_ready = 1'b0;
                        wb_valid = 1'b0;
                        jalr_target = (rs1_val + imm) & ~32'h1;
                        is_jalr = (opcode == `INST_JALR) & (func3 == 3'b000);
                        // 写回数据选择
                        case(opcode[6:2])
                            `INST_TYPE_LUI:   wb_data = imm;
                            `INST_TYPE_AUIPC: wb_data = pc + imm;
                            `INST_TYPE_JALR:  wb_data = pc + 4;//jalr
                            `INST_TYPE_L:     wb_data = data_out;//lw
                            `INST_TYPE_E:   wb_data = csr_rdata;//csrrw
                            `INST_TYPE_R,
                            `INST_TYPE_I:     wb_data = alu_result;
                            default:          wb_data = 32'b0;
                        endcase
                        //=====写回数据=====
                        rd_wb = id_rd;
                        RegWrite_wb = id_RegWrite;
                        if(RegWrite_wb && rd_wb != 0) begin
                            regs[rd_wb] <= wb_data;
                        end
                        next_state = if_ready ? STALL : IDLE;
                    end else begin
                        next_state = IDLE;
                    end
                end
                STALL: begin
                    wb_ready = 1'b0;
                    wb_valid = 1'b1;
                    next_state = if_ready ? IDLE : STALL;
                end
                default: begin
                    wb_ready = 1'b0;
                    wb_valid = 1'b0;
                    next_state = IDLE;
                end
            endcase
        end
    end

endmodule