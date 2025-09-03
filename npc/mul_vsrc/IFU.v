`include "include/defs.vh"

module IFU (
  // Clock and reset signals
  input                                clk,
  input                                rst,
  // Program counter (PC) control signals
  input      [2:0]                     pc_sel,   
  input      [`DATA_WIDTH-1:0]         jump_reg_target,
  input      [`DATA_WIDTH-1:0]         br_target,
  input      [`DATA_WIDTH-1:0]         jmp_target,
  input                                pc_wen,
  // IFU output
  output     [`DATA_WIDTH-1:0]         pc_o,
  output reg [31:0]                    inst_o,
  output     [`DATA_WIDTH-1:0]         pc_plus4_o,
  output reg                           inst_valid,  // 添加指令有效信号
  input wire                           is_ecall,
  input wire                           is_mret,
  input wire [`DATA_WIDTH-1:0]         mtvec_val,
  input wire [`DATA_WIDTH-1:0]         mepc_val
);

  wire [`DATA_WIDTH-1:0] pc_next;
  wire [`DATA_WIDTH-1:0] pc_plus4;
  reg                    rst_done; 

  reg                    sram_valid;
  wire                   sram_ready;
  wire                   sram_data_valid;
  wire [31:0]           sram_data;
  reg                    fetching;

  always @(posedge clk) begin
    if (rst) begin
      rst_done <= 1'b0; 
    end else begin
      rst_done <= 1'b1;  
    end
  end

  // 只在有效指令且PC写使能时更新PC
  wire pc_update_en = pc_wen & inst_valid;

  Reg #(
    .WIDTH(`DATA_WIDTH),
    .RESET_VAL(32'h80000000)
  ) pc_reg (
    .clk (clk),
    .rst (rst),
    .din (pc_next),
    .dout(pc_o),
    .wen (pc_update_en)
  );

  SRAM imem_sram (
    .clk     (clk),
    .rst     (rst),
    .valid_i (sram_valid),
    .ready_o (sram_ready),
    .addr_i  (pc_o),
    .valid_o (sram_data_valid),
    .data_o  (sram_data)
  );

  // Fetch control state machine
  always @(posedge clk) begin
    if(rst) begin
      fetching <= 1'b0;
      sram_valid <= 1'b0;
      inst_valid <= 1'b0;
    end else begin
      // Start fetch if not currently fetching
      if (!fetching && rst_done && !inst_valid) begin
        sram_valid <= 1'b1;
        if (sram_valid && sram_ready) begin
          fetching <= 1'b1;
          sram_valid <= 1'b0;
        end
      end
      // When data arrives
      else if (fetching && sram_data_valid) begin
        fetching <= 1'b0;
        inst_valid <= 1'b1;  // Mark instruction as valid
      end
      // Clear valid after PC update
      else if (inst_valid && pc_update_en) begin
        inst_valid <= 1'b0;
        // Start next fetch immediately for sequential instructions
        if (pc_sel == 3'b000) begin
          sram_valid <= 1'b1;
        end
      end
    end
  end

  // Instruction output register
  always @(posedge clk) begin
    if (rst) begin
      inst_o <= 32'h00000000;
    end else if (sram_data_valid && fetching) begin
      inst_o <= sram_data;
    end
  end

  assign pc_plus4 = pc_o + `PC_STEP;
  assign pc_plus4_o = pc_plus4;

  assign pc_next = rst ? 32'h80000000 :
                   is_ecall ? mtvec_val :
                   is_mret  ? mepc_val :
                   (pc_sel == 3'b000) ? pc_plus4 :
                   (pc_sel == 3'b001) ? jump_reg_target :
                   (pc_sel == 3'b010) ? br_target :
                   (pc_sel == 3'b011) ? jmp_target : 
                   32'h80000000;

endmodule