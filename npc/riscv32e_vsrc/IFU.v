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
    output [`DATA_WIDTH-1:0] pc_plus4_o
);

    wire [`DATA_WIDTH-1:0] pc_next;
    wire [`DATA_WIDTH-1:0] pc_plus4;
    reg [`DATA_WIDTH-1:0] inst;
    reg rst_done;

    import "DPI-C" context function int paddr_read(input int raddr);

    always @(*) begin
        inst = paddr_read(pc_o);
    end

    always @(posedge clk) begin
        if (rst) begin
            rst_done <= 1'b0;
        end else begin
            rst_done <= 1'b1;
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
        .wen(pc_wen&&rst_done)
    );

    assign pc_plus4 = pc_o + 4;
    assign pc_plus4_o = pc_plus4;

    assign inst_o = rst_done? inst : 32'h0;
    assign pc_next = rst ? 32'h80000000 :
                    is_ecall? mtvec_val :
                    is_mret ? mepc_val :
                    (pc_sel == 3'b000) ? pc_plus4 :
                    (pc_sel == 3'b001) ? jump_reg_target :
                    (pc_sel == 3'b010) ? br_target :
                    (pc_sel == 3'b011) ? jmp_target :
                    32'h80000000;

endmodule