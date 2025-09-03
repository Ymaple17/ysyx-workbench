`include "include/defs.vh"

module top (
  input wire clk,
  input wire rst,
  output [`DATA_WIDTH-1:0] instr,       // 当前指令（调试用）
  output [`DATA_WIDTH-1:0] imem_pc,     // 指令内存地址（调试用）
  output wire wen,                      // 内存使能（调试用）
  output [`DATA_WIDTH-1:0] mem_addr     // 数据内存地址（调试用）
);

  // 内部信号
  // IFU相关
  wire [`DATA_WIDTH-1:0] pc;
  wire [31:0] inst;
  wire [`DATA_WIDTH-1:0] pc_plus4;
  wire [2:0] pc_sel;
  wire [`DATA_WIDTH-1:0] jump_reg_target, br_target, jmp_target;

  // IDU相关
  wire [`REG_ADDR_WIDTH-1:0] rs1_addr, rs2_addr;
  wire [`DATA_WIDTH-1:0] rdata1;
  wire [`DATA_WIDTH-1:0] rdata2;
  wire [`REG_ADDR_WIDTH-1:0] waddr;
  wire [4:0] alu_op;
  wire rf_we, mem_en, mem_wen;
  wire [1:0] wb_sel;
  wire [2:0] ctl_mem_access;
  wire br_eq, br_lt, br_ltu;
  wire is_csr_instr, is_ecall, is_ebreak, is_mret;
  wire [2:0] csr_op;
  wire [11:0] csr_addr;

  // EXU相关
  wire [`DATA_WIDTH-1:0] Op1, Op2;
  wire [`DATA_WIDTH-1:0] alu_result, dmem_addr;
  wire csr_we;
  wire [`DATA_WIDTH-1:0] csr_wdata, csr_rdata;
  // LSU相关
  wire [`DATA_WIDTH-1:0] dmem_rdata;

  // WBU相关
  wire [`DATA_WIDTH-1:0] reg_write_data;
  wire reg_we;

  //csr
  wire [`DATA_WIDTH-1:0] mtvec_val, mepc_val, mcause_val, mstatus_val;

  wire inst_valid;

  wire rf_we_valid = rf_we & inst_valid;  // 只在指令有效时写寄存器
  wire mem_en_valid = mem_en & inst_valid;  // 只在指令有效时访问内存
  wire mem_wen_valid = mem_wen & inst_valid;  // 只在指令有效时写内存
  wire csr_we_valid = csr_we & inst_valid;  // 只在指令有效时写CSR

  // 寄存器文件
  RegisterFile #(
    .ADDR_WIDTH(`REG_ADDR_WIDTH),
    .DATA_WIDTH(`DATA_WIDTH)
  ) u_RegisterFile (
    .clk(clk),
    .waddr(waddr),
    .wdata(reg_write_data),
    .wen(reg_we),
    .raddr1(rs1_addr),
    .raddr2(rs2_addr),
    .rdata1(rdata1),
    .rdata2(rdata2),
    .pc(pc),
    .is_csr_instr(is_csr_instr)
  );

  // CSR文件
  csr_file csr (
    .clk(clk),
    .rst(rst),
    .csr_addr(csr_addr),
    .csr_wdata(csr_wdata),
    .csr_we(csr_we_valid),
    .csr_rdata(csr_rdata),
    .is_ecall(is_ecall & inst_valid),
    .pc_current(pc),
    .mtvec_val(mtvec_val),
    .mepc_val(mepc_val),
    .mcause(mcause_val),
    .mstatus(mstatus_val),
    .is_mret(is_mret&& inst_valid)
  );
  

  // 1. 取指单元（包含指令读取）
  IFU ifu (
    .clk(clk),
    .rst(rst),
    .pc_sel(pc_sel),
    .jump_reg_target(jump_reg_target),
    .br_target(br_target),
    .jmp_target(jmp_target),
    .mtvec_val(mtvec_val),
    .mepc_val(mepc_val),
    .is_ecall(is_ecall),
    .is_mret(is_mret),
    .pc_o(pc),
    .inst_o(inst),
    .pc_plus4_o(pc_plus4),
    .pc_wen(1'b1),
    .inst_valid(inst_valid)
  );

  // 2. 解码单元
  IDU idu (
    .inst_i(inst),
    .rs1_data_i(rdata1),
    .rs2_data_i(rdata2),
    .pc_i(pc),
    .rs1_addr_o(rs1_addr),
    .rs2_addr_o(rs2_addr),
    .rd_addr_o(waddr),
    .alu_op_o(alu_op),
    .Op1_o(Op1),
    .Op2_o(Op2),
    .pc_sel_o(pc_sel),
    .rf_we_o(rf_we),
    .mem_en_o(mem_en),
    .mem_wen_o(mem_wen),
    .wb_sel_o(wb_sel),
    .ctl_mem_access_o(ctl_mem_access),
    .jump_reg_target_o(jump_reg_target),
    .br_target_o(br_target),
    .jmp_target_o(jmp_target),
    .is_csr_instr_o(is_csr_instr),
    .csr_op_o(csr_op),
    .csr_addr_o(csr_addr),
    .is_ecall_o(is_ecall),
    .is_ebreak_o(is_ebreak),
    .is_mret_o(is_mret)
  );

  // 3. 执行单元
  EXU exu (
    .clk(clk),
    .rst(rst),
    .Op1(Op1),
    .Op2(Op2),
    .alu_op(alu_op),
    .is_csr_instr(is_csr_instr),
    .csr_op(csr_op),
    .csr_rdata(csr_rdata),
    .alu_result(alu_result),
    .csr_we(csr_we),
    .csr_wdata(csr_wdata)
  );

  // 4. 加载存储单元（包含数据内存访问）
  LSU lsu (
    .clk(clk),
    .rst(rst),
    .dmem_addr(alu_result),
    .dmem_wdata_raw(rdata2),
    .ctl_mem_access(ctl_mem_access),
    .mem_en(mem_en_valid),
    .mem_wen(mem_wen_valid),
    .dmem_rdata(dmem_rdata),
    .mem_addr(mem_addr),
    .wen(wen)
  );

  // 5. 写回单元
  WBU wbu (
    .alu_result(alu_result),
    .dmem_rdata(dmem_rdata),
    .pc_plus4(pc_plus4),
    .csr_rdata(csr_rdata),
    .wb_sel(wb_sel),
    .rf_we(rf_we_valid),
    .is_csr_instr(is_csr_instr),
    .reg_write_data(reg_write_data),
    .reg_we(reg_we)
  );

  // 调试信号
    assign instr = inst;
    assign imem_pc = pc;

    import "DPI-C" context function void sim_exit();
    always @(*) begin
        if (is_ebreak&&inst_valid) begin
           $display("EBREAK: Simulation exiting...");
           sim_exit(); // 通知仿真环境结束
        end
    end


endmodule
