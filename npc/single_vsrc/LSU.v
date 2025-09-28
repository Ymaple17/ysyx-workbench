`include "include/defs.vh"

module LSU(
    input  clk,
    input  rst,
    input [`DATA_WIDTH-1:0] dmem_addr,
    input [`DATA_WIDTH-1:0] dmem_wdata_raw,
    input [2:0]             ctl_mem_access,
    input                   mem_en,
    input                   mem_wen,
    output [`DATA_WIDTH-1:0] dmem_rdata,
    output reg [`DATA_WIDTH-1:0] mem_addr,
    output reg               wen
);

    import "DPI-C" context function int paddr_read(input int raddr);
    import "DPI-C" context function void paddr_write(input int waddr, input int wdata, input byte wmask);

    reg [`DATA_WIDTH-1:0] dmem_rdata_raw;
    wire [`DATA_WIDTH-1:0] dmem_wdata;
    wire [7:0]             wmask;
    wire [1:0]             shift_amount = dmem_addr[1:0];

    //read move
    wire [`DATA_WIDTH-1:0] shifted_data = dmem_rdata_raw >> (shift_amount * 8);

    //extend data
    wire [`DATA_WIDTH-1:0] zero_ext_byte = {24'b0, shifted_data[7:0]};
    wire [`DATA_WIDTH-1:0] zero_ext_half = {16'b0, shifted_data[15:0]};
    wire [`DATA_WIDTH-1:0] sign_ext_byte = {{24{shifted_data[7]}}, shifted_data[7:0]};
    wire [`DATA_WIDTH-1:0] sign_ext_half = {{16{shifted_data[15]}}, shifted_data[15:0]};
    
    //READ DATA
  assign dmem_rdata = 
    (ctl_mem_access == 3'b100) ? zero_ext_byte :    // LBU
    (ctl_mem_access == 3'b101) ? zero_ext_half :    // LHU
    (ctl_mem_access == 3'b010) ? dmem_rdata_raw :   // LW
    (ctl_mem_access == 3'b000) ? sign_ext_byte :    // LB
    (ctl_mem_access == 3'b001) ? sign_ext_half :    // LH
    32'b0;
  
  //WMASK 
  wire [7:0] base_mask = 
    (ctl_mem_access == 3'b010) ? 8'b00001111 :      // SW
    (ctl_mem_access == 3'b001) ? 8'b00000011 :      // SH
    (ctl_mem_access == 3'b000) ? 8'b00000001 :      // SB
    8'b0;
  
  assign wmask = base_mask << shift_amount;
  assign dmem_wdata = dmem_wdata_raw << (shift_amount * 8);

  // READ
  always @(*) begin
    if (rst || !mem_en) begin
      dmem_rdata_raw = 32'b0;
    end else begin
      dmem_rdata_raw = paddr_read(dmem_addr);
    end
  end

  //WIRTE
  always @(posedge clk) begin
    if (!rst && mem_en && mem_wen) begin
      paddr_write(dmem_addr, dmem_wdata, wmask);
    end
  end

  always @(posedge clk) begin
    mem_addr <= dmem_addr;
    wen <= mem_en | mem_wen;
  end

endmodule