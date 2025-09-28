`include "include/defs.vh"

module IFU (
    input                    clk,
    input                    rst,
    input  [2:0]             pc_sel,
    input  [`DATA_WIDTH-1:0] jump_reg_target,
    input  [`DATA_WIDTH-1:0] br_target,
    input  [`DATA_WIDTH-1:0] jmp_target,
    input                    pc_wen,
    input                    is_ecall,
    input                    is_mret,
    input  [`DATA_WIDTH-1:0] mtvec_val,
    input  [`DATA_WIDTH-1:0] mepc_val,
    output [`DATA_WIDTH-1:0] pc_o,
    output [`DATA_WIDTH-1:0] inst_o,
    output [`DATA_WIDTH-1:0] pc_plus4_o,
    output                   inst_valid_o,
    input  [`DATA_WIDTH-1:0] ifu_rdata,
    output [`DATA_WIDTH-1:0] ifu_raddr
);

    wire [`DATA_WIDTH-1:0] pc_next;
    wire [`DATA_WIDTH-1:0] pc_plus4;
    reg [`DATA_WIDTH-1:0] inst;
    reg rst_done;

    parameter IDLE = 1'b0;
    parameter WAIT = 1'b1;
    reg [`DATA_WIDTH-1:0] inst_reg;
    reg state, next_state;
    reg inst_valid_reg;

    import "DPI-C" context function int paddr_read(input int raddr);

    always @(posedge clk) begin
        if (rst) begin
            state <= WAIT;
            rst_done <= 1'b0;
        end else begin
            state <= next_state;
            rst_done <= 1'b1;
        end
    end

    always @(posedge clk) begin
        if (rst) begin
            inst_reg <= 32'h0;
            inst_valid_reg <= 1'b0;
        end else begin
            case (state)
                IDLE: begin
                    inst_valid_reg <= 1'b0;
                    next_state = WAIT;
                end
                WAIT: begin
                    inst_reg <= ifu_rdata;
                    inst_valid_reg <= rst_done;
                    next_state = IDLE;
                end
                default:next_state = IDLE;
            endcase
        end
    end

    Reg #(
        .WIDTH(`DATA_WIDTH),
        .RESET_VAL(32'h80000000)
    ) pc_reg (
        .clk(clk),
        .rst(rst),
        .din(pc_next),
        .dout(pc_o),
        .wen(pc_wen&&inst_valid_reg)
    );

    assign pc_plus4 = pc_o + 4;
    assign pc_plus4_o = pc_plus4;

    assign inst_o = inst_reg;
    assign inst_valid_o = inst_valid_reg;
    assign ifu_raddr = pc_o;

    assign pc_next = rst ? 32'h80000000 :
                    is_ecall? mtvec_val :
                    is_mret ? mepc_val :
                    (pc_sel == 3'b000) ? pc_plus4 :
                    (pc_sel == 3'b001) ? jump_reg_target :
                    (pc_sel == 3'b010) ? br_target :
                    (pc_sel == 3'b011) ? jmp_target :
                    32'h80000000;

endmodule
