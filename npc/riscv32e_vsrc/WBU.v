`include "include/defs.vh"

module WBU (
    input [`DATA_WIDTH-1:0] alu_result,
    input [`DATA_WIDTH-1:0] dmem_rdata,
    input [`DATA_WIDTH-1:0] pc_plus4,
    input [`DATA_WIDTH-1:0] csr_rdata,
    input [1:0]             wb_sel,
    input                   rf_we,
    input                   is_csr_instr,
    output reg [`DATA_WIDTH-1:0] reg_write_data,
    output                  reg_we
);

    always @(*) begin
        if(is_csr_instr) begin
            reg_write_data = csr_rdata;
        end else begin
            case(wb_sel)
                2'b01: reg_write_data = pc_plus4;
                2'b10: reg_write_data = alu_result;
                2'b11: reg_write_data = dmem_rdata;
                default: reg_write_data = 0;
        endcase
      end
    end

    assign reg_we = rf_we;

endmodule
