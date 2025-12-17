`include "/home/qiu/ysyx-workbench/npc/mul_soc_vsrc/include/defs.vh"

module ysyx_25020039 (
    input         clock,
    input         reset,
    input         io_interrupt,
    // output [31:0] instr,       // 指令
    // output [31:0] imem_pc,     // 指令内存地址
    // output wire wen,                      // 内存使能
    // output [31:0] mem_addr     // 数据内存地址
    //====== AXI Master ======//
    input         io_master_awready,
    output        io_master_awvalid,
    output [31:0] io_master_awaddr,
    output [3:0]  io_master_awid,
    output [7:0]  io_master_awlen,
    output [2:0]  io_master_awsize,
    output [1:0]  io_master_awburst,

    input         io_master_wready,
    output        io_master_wvalid,
    output [31:0] io_master_wdata,
    output [3:0]  io_master_wstrb,
    output        io_master_wlast,

    output        io_master_bready,
    input         io_master_bvalid,
    input  [1:0]  io_master_bresp,
    input  [3:0]  io_master_bid,

    input         io_master_arready,
    output        io_master_arvalid,
    output [31:0] io_master_araddr,
    output [3:0]  io_master_arid,
    output [7:0]  io_master_arlen,
    output [2:0]  io_master_arsize,
    output [1:0]  io_master_arburst,

    output        io_master_rready,
    input         io_master_rvalid,
    input  [1:0]  io_master_rresp,
    input  [31:0] io_master_rdata,
    input         io_master_rlast,
    input  [3:0]  io_master_rid,

    //====== AXI Slave ======//
    output        io_slave_awready,
    input         io_slave_awvalid,
    input  [31:0] io_slave_awaddr,
    input  [3:0]  io_slave_awid,
    input  [7:0]  io_slave_awlen,
    input  [2:0]  io_slave_awsize,
    input  [1:0]  io_slave_awburst,

    output        io_slave_wready,
    input         io_slave_wvalid,
    input  [31:0] io_slave_wdata,
    input  [3:0]  io_slave_wstrb,
    input         io_slave_wlast,

    input         io_slave_bready,
    output        io_slave_bvalid,
    output [1:0]  io_slave_bresp,
    output [3:0]  io_slave_bid,

    output        io_slave_arready,
    input         io_slave_arvalid,
    input  [31:0] io_slave_araddr,
    input  [3:0]  io_slave_arid,
    input  [7:0]  io_slave_arlen,
    input  [2:0]  io_slave_arsize,
    input  [1:0]  io_slave_arburst,

    input         io_slave_rready,
    output        io_slave_rvalid,
    output [1:0]  io_slave_rresp,
    output [31:0] io_slave_rdata,
    output        io_slave_rlast,
    output [3:0]  io_slave_rid
);
    //unuse signals
    assign io_slave_awready = 1'b0;
    assign io_slave_wready  = 1'b0;
    assign io_slave_bvalid  = 1'b0;
    assign io_slave_bresp   = 2'b0;
    assign io_slave_bid     = 4'b0;
    assign io_slave_arready = 1'b0;
    assign io_slave_rvalid  = 1'b0;
    assign io_slave_rresp   = 2'b0;
    assign io_slave_rdata   = 32'b0;
    assign io_slave_rlast   = 1'b0;
    assign io_slave_rid     = 4'b0;

    //===== IFU =====//
    wire [31:0]  pc;
    wire [31:0]  inst;
    wire         if_ready;
    wire         wb_valid;
    wire         if_valid;
    wire         id_ready;
    wire         if_access_fault;
    wire [31:0]  if_fault_addr;
    wire [31:0]  trap_pc;
    wire [31:0] ifu_sram_araddr;
    wire        ifu_sram_arvalid;
    wire        ifu_sram_arready;
    wire [31:0] ifu_sram_rdata;
    wire        ifu_sram_rvalid;
    wire        ifu_sram_rready;
    wire [1:0]  ifu_sram_rresp;
    wire [31:0] ifu_sram_awaddr;
    wire        ifu_sram_awvalid;
    wire        ifu_sram_awready;
    wire [31:0] ifu_sram_wdata;
    wire [3:0]  ifu_sram_wstrb;
    wire        ifu_sram_wvalid;
    wire        ifu_sram_wready;
    wire [1:0]  ifu_sram_bresp;
    wire        ifu_sram_bvalid;
    wire        ifu_sram_bready;
    //===== ID =====//
    wire [6:0]   opcode;
    wire [4:0]   rs1, rs2, rd;
    wire [31:0]  imm;
    wire [2:0]   func3;
    wire [6:0]   func7;
    wire         RegWrite;
    wire         MemWrite;
    wire         MemRead;
    wire [3:0]   alu_op;
    wire [2:0]   MemLen;
    wire         id_valid;
    wire         ex_ready;

    //===== EXU =====//
    wire [31:0]  rs1_val, rs2_val;
    wire [31:0]  alu_result;
    wire         alu_zero;
    wire         alu_less;
    wire         ex_valid;
    wire         mem_ready;

    //===== MEM =====//
    wire [31:0]  data_out;
    wire         mem_valid;
    wire         wb_ready;
    wire         load_access_fault;
    wire         store_access_fault;
    wire [31:0]  mem_fault_addr;

    wire [31:0] mem_sram_araddr;
    wire        mem_sram_arvalid;
    wire        mem_sram_arready;
    wire [31:0] mem_sram_rdata;
    wire        mem_sram_rvalid;
    wire        mem_sram_rready;
    wire [1:0]  mem_sram_rresp;
    wire [31:0] mem_sram_awaddr;
    wire        mem_sram_awvalid;
    wire        mem_sram_awready;
    wire [31:0] mem_sram_wdata;
    wire [3:0]  mem_sram_wstrb;
    wire        mem_sram_wvalid;
    wire        mem_sram_wready;
    wire [1:0]  mem_sram_bresp;
    wire        mem_sram_bvalid;
    wire        mem_sram_bready;
    //===== WB =====//
    wire [31:0]  wb_data;
    wire [31:0]  jal_target;
    wire [31:0]  jalr_target;
    wire         is_jal, is_jalr;
    wire         take_branch;
    wire [31:0]  branch_target;  
    wire [4:0]   rd_wb;
    wire         RegWrite_wb;
    wire         wb_MemRead, wb_MemWrite;
    wire [2:0]   wb_MemLen;
    wire [31:0]  wb_addr, wb_data_in;
    
    //csr
    wire         csr_en;
    wire [11:0]  csr_addr;
    wire [1:0]   csr_op;
    wire         csr_imm;
    wire [4:0]   csr_zimm;
    wire         csr_ecall;
    wire         csr_mret;
    wire         csr_trap;
    wire [31:0]  csr_target_pc;
    wire         pc_src_sel;
    //====SRAM读写接口====//
    //AR channel
    wire [31:0] sram_araddr;//读地址
    wire        sram_arvalid;//读地址有效
    wire        sram_arready;//sram读地址准备好
    //R channel
    wire [1:0]  sram_rresp;//读响应信号
    wire [31:0] sram_rdata;//读数据
    wire        sram_rvalid;//读数据有效
    wire        sram_rready;//CPU读数据准备好
    //AW channel
    wire [31:0] sram_awaddr;//写地址
    wire        sram_awready;//sram写地址准备好
    wire        sram_awvalid;//写地址有效
    //W channel
    wire [31:0] sram_wdata;//写数据
    wire [3:0]  sram_wstrb;//写掩码
    wire        sram_wvalid;//写请求有效
    wire        sram_wready;//sram写请求准备好
    //B channel
    wire [1:0]  sram_bresp;//写响应信号
    wire        sram_bvalid;//写响应有效
    wire        sram_bready;//写响应准备好

    //====== CLINT =======//
    //AR channel
    wire [31:0] clint_araddr;//读地址
    wire        clint_arvalid;//读地址有效
    wire        clint_arready;//clint读地址准备好
    //R channel
    wire [1:0]  clint_rresp;//读响应信号
    wire [31:0] clint_rdata;//读数据
    wire        clint_rvalid;//读数据有效
    wire        clint_rready;//CPU读数据准备好
    //AW channel
    wire [31:0] clint_awaddr;//写地址
    wire        clint_awready;//clint写地址准备好
    wire        clint_awvalid;//写地址有效
    //W channel
    wire [31:0] clint_wdata;//写数据
    wire [3:0]  clint_wstrb;//写掩码
    wire        clint_wvalid;//写请求有效
    wire        clint_wready;//clint写请求准备好
    //B channel
    wire [1:0]  clint_bresp;//写响应信号
    wire        clint_bvalid;//写响应有效
    wire        clint_bready;//写响应准备好


    Arbiter arbiter (
        .clk(clock),
        .reset(reset),

        // IFU
        .ifu_araddr(ifu_sram_araddr),
        .ifu_arvalid(ifu_sram_arvalid),
        .ifu_arready(ifu_sram_arready),
        .ifu_rdata(ifu_sram_rdata),
        .ifu_rresp(ifu_sram_rresp),
        .ifu_rvalid(ifu_sram_rvalid),
        .ifu_rready(ifu_sram_rready),

        // MEM
        .mem_araddr(mem_sram_araddr),
        .mem_arvalid(mem_sram_arvalid),
        .mem_arready(mem_sram_arready),
        .mem_rdata(mem_sram_rdata),
        .mem_rresp(mem_sram_rresp),
        .mem_rvalid(mem_sram_rvalid),
        .mem_rready(mem_sram_rready),
        .mem_awaddr(mem_sram_awaddr),
        .mem_awvalid(mem_sram_awvalid),
        .mem_awready(mem_sram_awready),
        .mem_wdata(mem_sram_wdata),
        .mem_wstrb(mem_sram_wstrb),
        .mem_wvalid(mem_sram_wvalid),
        .mem_wready(mem_sram_wready),
        .mem_bresp(mem_sram_bresp),
        .mem_bvalid(mem_sram_bvalid),
        .mem_bready(mem_sram_bready),

        .io_master_araddr(io_master_araddr),
        .io_master_arvalid(io_master_arvalid),
        .io_master_arready(io_master_arready),
        .io_master_rdata(io_master_rdata),
        .io_master_rresp(io_master_rresp),
        .io_master_rvalid(io_master_rvalid),
        .io_master_rready(io_master_rready),
        .io_master_awaddr(io_master_awaddr),
        .io_master_awvalid(io_master_awvalid),
        .io_master_awready(io_master_awready),
        .io_master_wdata(io_master_wdata),
        .io_master_wstrb(io_master_wstrb),
        .io_master_wvalid(io_master_wvalid),
        .io_master_wready(io_master_wready),
        .io_master_bresp(io_master_bresp),
        .io_master_bvalid(io_master_bvalid),
        .io_master_bready(io_master_bready),    

        .clint_araddr(clint_araddr),
        .clint_arvalid(clint_arvalid),
        .clint_arready(clint_arready),
        .clint_rdata(clint_rdata),
        .clint_rresp(clint_rresp),
        .clint_rvalid(clint_rvalid),
        .clint_rready(clint_rready),
        .clint_awaddr(clint_awaddr),
        .clint_awvalid(clint_awvalid),
        .clint_awready(clint_awready),
        .clint_wdata(clint_wdata),
        .clint_wstrb(clint_wstrb),
        .clint_wvalid(clint_wvalid),
        .clint_wready(clint_wready),
        .clint_bresp(clint_bresp),
        .clint_bvalid(clint_bvalid),
        .clint_bready(clint_bready)
    );

    // CLINT
    CLINT clint (
        .clk(clock),
        .reset(reset),
        .awvalid(clint_awvalid),
        .awready(clint_awready),
        .awaddr(clint_awaddr),
        .wdata(clint_wdata),
        .wstrb(clint_wstrb),
        .wvalid(clint_wvalid),
        .wready(clint_wready),
        .bresp(clint_bresp),
        .bvalid(clint_bvalid),
        .bready(clint_bready),
        .arvalid(clint_arvalid),
        .araddr(clint_araddr),
        .arready(clint_arready),
        .rready(clint_rready),
        .rvalid(clint_rvalid),
        .rresp(clint_rresp),
        .rdata(clint_rdata)
    );

    // 取指模块
    IFU ifu (
        .clk(clock),
        .reset(reset),
        .branch_target(branch_target),
        .pc_src(pc_src_sel),
        .pc(pc),
        .instr(inst),
        .if_ready(if_ready),
        .wb_valid(wb_valid),
        .if_valid(if_valid),
        .id_ready(id_ready),
        .if_access_fault(if_access_fault),
        .if_fault_addr(if_fault_addr),
        .sram_araddr(ifu_sram_araddr),
        .sram_arvalid(ifu_sram_arvalid),
        .sram_arready(ifu_sram_arready),
        .sram_rdata(ifu_sram_rdata),
        .sram_rvalid(ifu_sram_rvalid),
        .sram_rready(ifu_sram_rready),
        .sram_rresp(ifu_sram_rresp),
        .sram_awaddr(ifu_sram_awaddr),
        .sram_awvalid(ifu_sram_awvalid),
        .sram_awready(ifu_sram_awready),
        .sram_wdata(ifu_sram_wdata),
        .sram_wstrb(ifu_sram_wstrb),
        .sram_wvalid(ifu_sram_wvalid),
        .sram_wready(ifu_sram_wready),
        .sram_bresp(ifu_sram_bresp),
        .sram_bvalid(ifu_sram_bvalid),
        .sram_bready(ifu_sram_bready)
    );

    // 译码模块
    IDU idu (
        .clk(clock),
        .reset(reset),
        .instr(inst),
        .if_valid(if_valid),
        .id_ready(id_ready),
        .id_valid(id_valid),
        .ex_ready(ex_ready),
        .opcode(opcode),
        .rs1(rs1),
        .rs2(rs2),
        .rd(rd),
        .imm(imm),
        .func3(func3),
        .func7(func7),
        .RegWrite(RegWrite),
        .MemWrite(MemWrite),
        .MemRead(MemRead),
        .alu_op(alu_op),
        .MemLen(MemLen),
        .csr_en(csr_en),
        .csr_addr(csr_addr),
        .csr_op(csr_op),
        .csr_imm(csr_imm),
        .csr_zimm(csr_zimm),
        .csr_ecall(csr_ecall),
        .csr_mret(csr_mret)
    );
    
    EXU exu(
        .clk(clock), 
        .reset(reset),
        .id_valid(id_valid),
        .ex_ready(ex_ready),
        .opcode(opcode), 
        .rs1_val(rs1_val),
        .rs2_val(rs2_val),
        .imm(imm),
        .alu_op(alu_op),
        .mem_ready(mem_ready),
        .ex_valid(ex_valid),
        .alu_result(alu_result),
        .alu_zero(alu_zero),   
        .alu_less(alu_less)
    );
    // 内存模块
    MEM mem(
        .clk(clock),
        .reset(reset),
        .ex_valid(ex_valid),
        .mem_ready(mem_ready),
        .wb_ready(wb_ready),
        .mem_valid(mem_valid),
        .MemRead(MemRead),
        .MemWrite(MemWrite),
        .MemLen(MemLen),
        .addr(rs1_val + imm),
        .data_in(rs2_val),
        .data_out(data_out),
        .load_access_fault(load_access_fault),
        .store_access_fault(store_access_fault),
        .mem_fault_addr(mem_fault_addr),
        .sram_araddr(mem_sram_araddr),
        .sram_arvalid(mem_sram_arvalid),
        .sram_arready(mem_sram_arready),
        .sram_rdata(mem_sram_rdata),
        .sram_rvalid(mem_sram_rvalid),
        .sram_rready(mem_sram_rready),
        .sram_rresp(mem_sram_rresp),
        .sram_awaddr(mem_sram_awaddr),
        .sram_awvalid(mem_sram_awvalid),
        .sram_awready(mem_sram_awready),
        .sram_wdata(mem_sram_wdata),
        .sram_wstrb(mem_sram_wstrb),
        .sram_wvalid(mem_sram_wvalid),
        .sram_wready(mem_sram_wready),
        .sram_bresp(mem_sram_bresp),
        .sram_bvalid(mem_sram_bvalid),
        .sram_bready(mem_sram_bready)
    );

    // 写回模块
    WBU wbu (
        .clk(clock), 
        .reset(reset),
        .mem_valid(mem_valid),
        .wb_ready(wb_ready),
        .if_ready(if_ready),
        .wb_valid(wb_valid),
        .opcode(opcode),
        .func3(func3),
        .id_rd(rd),
        .id_RegWrite(RegWrite),
        .rs1(rs1),
        .rs2(rs2),
        .rs1_val(rs1_val),
        .rs2_val(rs2_val),
        .pc(pc),
        .imm(imm),
        .alu_less(alu_less),
        .alu_zero(alu_zero),
        .alu_result(alu_result),
        .data_out(data_out),
        .csr_en(csr_en),
        .csr_addr(csr_addr),
        .csr_op(csr_op),
        .csr_imm(csr_imm),
        .csr_zimm(csr_zimm),
        .csr_ecall(csr_ecall),
        .csr_mret(csr_mret),
        .is_jal(is_jal),
        .is_jalr(is_jalr),
        .take_branch(take_branch),
        .jal_target(jal_target),
        .jalr_target(jalr_target),
        .wb_data(wb_data),
        .csr_trap(csr_trap),
        .csr_target_pc(csr_target_pc)
    );
    assign branch_target = csr_trap ? csr_target_pc : (is_jalr ? jalr_target : jal_target);
    assign pc_src_sel = csr_trap | is_jal | is_jalr | take_branch;

    always @(*) begin
        if(if_access_fault) begin
            $display("\033[31m[IFU]:IF access fault at address: %h\033[0m", if_fault_addr);
        end
        if(load_access_fault) begin
            $display("\033[31m[MEM]:Load access fault at address: %h\033[0m", mem_fault_addr);
        end
        if(store_access_fault) begin
            $display("\033[31m[MEM]:Store access fault at address: %h\033[0m", mem_fault_addr);
        end
    end 
endmodule
