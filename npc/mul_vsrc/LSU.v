`include "include/defs.vh"

module LSU (
  input                        clk,            // 时钟信号
  input                        rst,            // 复位信号
  input      [`DATA_WIDTH-1:0] dmem_addr,      // 内存地址
  input      [`DATA_WIDTH-1:0] dmem_wdata_raw, // 原始写数据（rs2）
  input      [2:0]             ctl_mem_access, // 内存访问控制（操作类型）
  input                        mem_en,         // 内存访问使能
  input                        mem_wen,        // 内存写使能
  output     [`DATA_WIDTH-1:0] dmem_rdata,     // 对齐后读数据（输出到WBU）
  output reg [`DATA_WIDTH-1:0] mem_addr,       
  output reg                   wen             
);

  import "DPI-C" context function int paddr_read(input int raddr);
  import "DPI-C" context function void paddr_write(input int waddr, input int wdata, input byte wmask);

  reg  [`DATA_WIDTH-1:0] dmem_rdata_raw; 
  wire [`DATA_WIDTH-1:0] dmem_wdata;     
  wire [7:0]             wmask;         

  // 1. 读操作：组合逻辑（可保留，读操作多次执行通常无副作用）
  always @(*) begin
    if (rst || !mem_en) begin
      dmem_rdata_raw = 32'b0;
    end else begin
      dmem_rdata_raw = paddr_read(dmem_addr);
    end
  end

  // 2. 写操作：时序逻辑（仅在时钟边沿执行一次）
  always @(posedge clk) begin
    if (!rst && mem_en && mem_wen) begin  // 仅在时钟边沿且条件满足时执行
      paddr_write(dmem_addr, dmem_wdata, wmask);
    end
  end

  // 3. 数据对齐和写掩码生成（保持不变）
  alignment_network align_unit (
    .data_in(dmem_rdata_raw),
    .dmem_addr(dmem_addr),
    .control(ctl_mem_access),
    .data_out(dmem_rdata)
  );

  wmask_gen wmask_unit (
    .control(ctl_mem_access),
    .dmem_addr(dmem_addr),
    .dmem_wdata_raw(dmem_wdata_raw),
    .dmem_wdata(dmem_wdata),
    .wmask(wmask)
  );

  // 调试信号（时序输出，避免毛刺）
  always @(posedge clk) begin
    mem_addr <= dmem_addr;
    wen <= mem_en | mem_wen;
  end

endmodule
module alignment_network (
  input  [`DATA_WIDTH-1:0] data_in,  
  input  [`DATA_WIDTH-1:0] dmem_addr,     
  input  [2:0]  control,       
  output [`DATA_WIDTH-1:0] data_out       
);

  wire [1:0] shift_amount = dmem_addr[1:0];
  wire [`DATA_WIDTH-1:0] shifted_data = data_in >> (shift_amount * 8);

  wire [`DATA_WIDTH-1:0] zero_ext_byte;   
  wire [`DATA_WIDTH-1:0] zero_ext_half;
  wire [`DATA_WIDTH-1:0] sign_ext_byte;
  wire [`DATA_WIDTH-1:0] sign_ext_half;

  assign zero_ext_byte = {24'b0, shifted_data[7:0]};
  assign zero_ext_half = {16'b0, shifted_data[15:0]};
  assign sign_ext_byte = {{24{shifted_data[7]}}, shifted_data[7:0]};
  assign sign_ext_half = {{16{shifted_data[15]}}, shifted_data[15:0]};

  MuxKey #(
    .NR_KEY(5),         
    .KEY_LEN(3),        
    .DATA_LEN(`DATA_WIDTH)       
  ) mem_mux (
    .out(data_out),
    .key(control), 
    .lut({              
      3'b100, zero_ext_byte,    // LBU                       
      3'b101, zero_ext_half,    // LHU
      3'b010, data_in,          // LW
      3'b000, sign_ext_byte,    // LB
      3'b001, sign_ext_half     // LH                   
    })
  );

endmodule

module wmask_gen (
  input  [2:0]  control,   
  input  [`DATA_WIDTH-1:0] dmem_addr,     
  input  [`DATA_WIDTH-1:0] dmem_wdata_raw,  
  output [`DATA_WIDTH-1:0] dmem_wdata,   
  output [7:0] wmask       
);

  wire [1:0] shift_amount = dmem_addr[1:0];
  wire [7:0] base_mask;

  MuxKey #(3, 3, 8) mask_mux (
    .out(base_mask),
    .key(control), 
    .lut({              
      3'b010, 8'b00001111,    // SW                      
      3'b001, 8'b00000011,    // SH
      3'b000, 8'b00000001     // SB
    })
  );

  assign wmask = base_mask << (shift_amount * 1);
  assign dmem_wdata = dmem_wdata_raw << (shift_amount * 8);

endmodule

