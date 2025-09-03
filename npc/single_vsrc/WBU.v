`include "include/defs.vh"
module WBU (
  input      [`DATA_WIDTH-1:0] alu_result,     // ALU结果
  input      [`DATA_WIDTH-1:0] dmem_rdata,     // 内存读数据
  input      [`DATA_WIDTH-1:0] pc_plus4,       // PC+4
  input      [`DATA_WIDTH-1:0] csr_rdata,      // CSR读数据
  input      [1:0]             wb_sel,         // 写回选择
  input                        rf_we,          // 寄存器写使能
  input                        is_csr_instr,   // CSR指令标志
  output reg [`DATA_WIDTH-1:0] reg_write_data, // 寄存器写数据
  output                       reg_we          // 寄存器写使能
);

  always @(*) begin
    if (is_csr_instr) begin
      reg_write_data = csr_rdata;
    end else begin
      case (wb_sel)
        2'b01: reg_write_data = pc_plus4;    // JAL/JALR写回PC+4
        2'b10: reg_write_data = alu_result;  // ALU结果
        2'b11: reg_write_data = dmem_rdata;  // 内存读数据
        default: reg_write_data = 32'b0;     // 无写回
      endcase
    end
  end

  assign reg_we = rf_we;

endmodule
