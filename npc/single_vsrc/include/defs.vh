`ifndef DEFS_VH
`define DEFS_VH

`define DATA_WIDTH 32
`define REG_ADDR_WIDTH 5 
`define PC_STEP 4
`define IMEM_BASE_ADDR 32'h80000000
`define RESET_PC 32'h80000000
// 握手信号相关定义
`define VALID 1'b1
`define READY 1'b1
`define INVALID 1'b0
`define NOT_READY 1'b0

`endif // DEFS_VH
