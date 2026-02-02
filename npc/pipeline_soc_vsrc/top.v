`include "include/define.vh"

module ysyx_25020039 (
	input         clock,
	input         reset,
	input         io_interrupt,
	// AXI Master
	input         io_master_awready,
	output        io_master_awvalid,
	output [31:0] io_master_awaddr,
	output [ 3:0] io_master_awid,
	output [ 7:0] io_master_awlen,
	output [ 2:0] io_master_awsize,
	output [ 1:0] io_master_awburst,

	input         io_master_wready,
	output        io_master_wvalid,
	output [31:0] io_master_wdata,
	output [ 3:0] io_master_wstrb,
	output        io_master_wlast,

	output        io_master_bready,
	input         io_master_bvalid,
	input  [ 1:0] io_master_bresp,
	input  [ 3:0] io_master_bid,

	input         io_master_arready,
	output        io_master_arvalid,
	output [31:0] io_master_araddr,
	output [ 3:0] io_master_arid,
	output [ 7:0] io_master_arlen,
	output [ 2:0] io_master_arsize,
	output [ 1:0] io_master_arburst,

	output        io_master_rready,
	input         io_master_rvalid,
	input  [ 1:0] io_master_rresp,
	input  [31:0] io_master_rdata,
	input         io_master_rlast,
	input  [ 3:0] io_master_rid,

	// AXI Slave (unused, tie-off ready/valid)
	output        io_slave_awready,
	input         io_slave_awvalid,
	input  [31:0] io_slave_awaddr,
	input  [ 3:0] io_slave_awid,
	input  [ 7:0] io_slave_awlen,
	input  [ 2:0] io_slave_awsize,
	input  [ 1:0] io_slave_awburst,

	output        io_slave_wready,
	input         io_slave_wvalid,
	input  [31:0] io_slave_wdata,
	input  [ 3:0] io_slave_wstrb,
	input         io_slave_wlast,

	input         io_slave_bready,
	output [ 1:0] io_slave_bresp,
	output        io_slave_bvalid,
	output [ 3:0] io_slave_bid,

	output        io_slave_arready,
	input         io_slave_arvalid,
	input  [31:0] io_slave_araddr,
	input  [ 3:0] io_slave_arid,
	input  [ 7:0] io_slave_arlen,
	input  [ 2:0] io_slave_arsize,
	input  [ 1:0] io_slave_arburst,

	input         io_slave_rready,
	output        io_slave_rvalid,
	output [ 1:0] io_slave_rresp,
	output [31:0] io_slave_rdata,
	output        io_slave_rlast,
	output [ 3:0] io_slave_rid
);

	// Unused slave port tie-offs
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

	// IF/ID/EX/LS/WB pipeline wiring
	wire        ifu_valid;
	wire        idu_ready;
	wire        idu_valid;
	wire        exu_ready;
	wire        exu_flush;
	wire [31:0] exu_flush_pc;
	wire        exu_fencei;
	wire        exit_flag;

	wire [31:0] ifu_pc;
	wire [31:0] ifu_inst;

	wire [31:0] idu_exu_pc;
	wire        idu_exu_RegWrite;
	wire [ 3:0] idu_exu_rd;
	wire [ 3:0] idu_wbu_rs1;
	wire [ 3:0] idu_wbu_rs2;
	wire [ 4:0] idu_exu_zimm;
	wire [31:0] idu_exu_imm;
	wire [ 5:0] idu_exu_shamt;
	wire [ 3:0] idu_exu_alu_op;
	wire [ 4:0] idu_exu_MemLen;
	wire        idu_exu_MemWrite;
	wire        idu_exu_MemRead;
	wire [ 6:0] idu_exu_opcode;
	wire [ 2:0] idu_exu_func3;
	wire        idu_exu_jal;
	wire        idu_exu_jalr;
	wire        idu_exu_fencei;
	wire        idu_exu_csr_wen1;
	wire        idu_exu_csr_ecall;
	wire        idu_exu_csr_mret;
	wire [ 1:0] idu_exu_csr_op;
	wire [11:0] idu_exu_csr_wr_addr1;
	wire [11:0] idu_wbu_csr_addr1;
	wire [11:0] idu_wbu_csr_addr2;

	wire        exu_lsu_valid;
	wire        lsu_exu_ready;
	wire [31:0] exu_lsu_src2;
	wire        exu_lsu_RegWrite;
	wire [ 3:0] exu_lsu_rd;
	wire        exu_lsu_MemRead;
	wire        exu_lsu_MemWrite /* verilator public_flat */;
	wire [ 4:0] exu_lsu_MemLen;
	wire        exu_lsu_csr;
	wire        exu_lsu_csr_wen1;
	wire [11:0] exu_lsu_csr_wr_addr1;
	wire [31:0] exu_lsu_csr_wr_data1;
	wire [31:0] exu_lsu_csr_wr_data2;
	wire [31:0] exu_lsu_csr_rdata;
	wire        exu_lsu_csr_ecall;
	wire        exu_lsu_csr_mret;
	wire [31:0] exu_lsu_process_result;
	wire [31:0] exu_lsu_pc;
	wire        exu_lsu_forward_las;

	wire lsu_wbu_valid /* verilator public_flat */;
	wire        lsu_wbu_RegWrite;
	wire [ 3:0] lsu_wbu_rd;
	wire [31:0] lsu_wbu_write_rd_data;
	wire [31:0] lsu_wbu_csr_wr_data1;
	wire [31:0] lsu_wbu_csr_wr_data2;
	wire [11:0] lsu_wbu_csr_wr_addr1;
	wire        lsu_wbu_csr_wen1;
	wire        lsu_wbu_csr_ecall;
	wire [31:0] lsu_wbu_pc;
	wire [31:0] wbu_pc /* verilator public_flat */;

	wire [31:0] wbu_exu_src1;
	wire [31:0] wbu_exu_src2;
	wire [31:0] wbu_exu_csr_num1;
	wire [31:0] wbu_exu_csr_num2;
	wire        wbu_lsu_ready;

	wire [ 3:0] lsu_exu_forward_rd;
	wire        lsu_exu_forward_RegWrite;
	wire        lsu_exu_forward_MemRead;

	// AXI wiring between IFU/LSU and Xbar
	wire        ifu_arvalid;
	wire [31:0] ifu_araddr;
	wire [ 3:0] ifu_arid;
	wire [ 7:0] ifu_arlen;
	wire [ 2:0] ifu_arsize;
	wire [ 1:0] ifu_arburst;
	wire        ifu_arready;
	wire        ifu_rready;
	wire        ifu_rvalid;
	wire [ 1:0] ifu_rresp;
	wire [31:0] ifu_rdata;
	wire        ifu_rlast;
	wire [ 3:0] ifu_rid;

	wire        lsu_arready;
	wire        lsu_rvalid;
	wire [ 1:0] lsu_rresp;
	wire [31:0] lsu_rdata;
	wire        lsu_rlast;
	wire [ 3:0] lsu_rid;

	wire        lsu_axi_arvalid;
	wire [31:0] lsu_axi_araddr;
	wire [ 3:0] lsu_axi_arid;
	wire [ 7:0] lsu_axi_arlen;
	wire [ 2:0] lsu_axi_arsize;
	wire [ 1:0] lsu_axi_arburst;
	wire        lsu_axi_rready;
	wire        axi_lsu_rvalid;
	wire [ 1:0] axi_lsu_rresp;
	wire [31:0] axi_lsu_rdata;
	wire        axi_lsu_rlast;
	wire [ 3:0] axi_lsu_rid;

	wire        lsu_axi_awvalid;
	wire [31:0] lsu_axi_awaddr;
	wire [ 3:0] lsu_axi_awid;
	wire [ 7:0] lsu_axi_awlen;
	wire [ 2:0] lsu_axi_awsize;
	wire [ 1:0] lsu_axi_awburst;
	wire        lsu_axi_wvalid;
	wire [31:0] lsu_axi_wdata;
	wire [ 3:0] lsu_axi_wstrb;
	wire        lsu_axi_wlast;
	wire        axi_lsu_awready;
	wire        axi_lsu_wready;
	wire [ 1:0] axi_lsu_bresp;
	wire        axi_lsu_bvalid;
	wire [ 3:0] axi_lsu_bid;
	wire        lsu_axi_bready;

	// CLINT wiring
	wire        clint_arready;
	wire        clint_arvalid;
	wire [31:0] clint_araddr;
	wire        clint_rready;
	wire        clint_rvalid;
	wire [ 1:0] clint_rresp;
	wire [31:0] clint_rdata;
	wire        clint_rlast;

	// ICache
	wire [31:0] next_pc;
	wire [31:0] icache_inst;
	wire        icache_valid;
	wire [31:0] icache_araddr;
	wire        icache_arvalid;
	wire [ 3:0] icache_arid;
	wire [ 7:0] icache_arlen;
	wire [ 2:0] icache_arsize;
	wire [ 1:0] icache_arburst;
	wire        icache_rready;


	import "DPI-C" function void sim_exit();

	always @(*) begin
		if (exit_flag) begin
			sim_exit();
		end
	end

	// IFU
	IFU ifu (
		.clock         (clock         ),
		.reset         (reset         ),
		.idu_ready     (idu_ready     ),
		.ifu_valid     (ifu_valid     ),
		.exu_flush     (exu_flush     ),
		.exu_flush_pc  (exu_flush_pc  ),
		.ifu_pc        (ifu_pc        ),
		.ifu_inst      (ifu_inst      ),
		.next_pc       (next_pc       ),
		.ifu_arvalid   (ifu_arvalid   ),
		.ifu_araddr    (ifu_araddr    ),
		.ifu_arid      (ifu_arid      ),
		.ifu_arlen     (ifu_arlen     ),
		.ifu_arsize    (ifu_arsize    ),
		.ifu_arburst   (ifu_arburst   ),
		.ifu_rready    (ifu_rready    ),
		.icache_valid  (icache_valid      ),
		.icache_inst   (icache_inst       ),
		.icache_arvalid(icache_arvalid),
		.icache_araddr (icache_araddr ),
		.icache_arid   (icache_arid   ),
		.icache_arlen  (icache_arlen  ),
		.icache_arsize (icache_arsize ),
		.icache_arburst(icache_arburst),
		.icache_rready (icache_rready )
	);

	// IDU
	IDU idu (
		.clock              (clock               ),
		.reset              (reset               ),
		.ifu_pc             (ifu_pc              ),
		.ifu_inst           (ifu_inst            ),
		.exu_flush          (exu_flush           ),
		.ifu_valid          (ifu_valid           ),
		.idu_ready          (idu_ready           ),
		.exu_ready          (exu_ready           ),
		.idu_valid          (idu_valid           ),
		.exit               (exit_flag           ),
		.idu_exu_pc         (idu_exu_pc          ),
		.idu_exu_RegWrite   (idu_exu_RegWrite    ),
		.idu_exu_rd         (idu_exu_rd          ),
		.idu_wbu_rs1        (idu_wbu_rs1         ),
		.idu_wbu_rs2        (idu_wbu_rs2         ),
		.idu_exu_zimm       (idu_exu_zimm        ),
		.idu_exu_imm        (idu_exu_imm         ),
		.idu_exu_shamt      (idu_exu_shamt       ),
		.idu_exu_alu_op     (idu_exu_alu_op      ),
		.idu_exu_MemLen     (idu_exu_MemLen      ),
		.idu_exu_MemWrite   (idu_exu_MemWrite    ),
		.idu_exu_MemRead    (idu_exu_MemRead     ),
		.idu_exu_opcode     (idu_exu_opcode      ),
		.idu_exu_func3      (idu_exu_func3       ),
		.idu_exu_jal        (idu_exu_jal         ),
		.idu_exu_jalr       (idu_exu_jalr        ),
		.idu_exu_fencei     (idu_exu_fencei      ),
		.idu_exu_csr_wen1   (idu_exu_csr_wen1    ),
		.idu_exu_csr_ecall  (idu_exu_csr_ecall   ),
		.idu_exu_csr_mret   (idu_exu_csr_mret    ),
		.idu_exu_csr_op     (idu_exu_csr_op      ),
		.idu_exu_csr_wr_addr1(idu_exu_csr_wr_addr1),
		.idu_wbu_csr_addr1  (idu_wbu_csr_addr1   ),
		.idu_wbu_csr_addr2  (idu_wbu_csr_addr2   )
	);

	// EXU
	EXU exu (
		.clk                    (clock                  ),
		.reset                  (reset                  ),
		.idu_ready              (idu_ready              ),
		.idu_valid              (idu_valid              ),
		.exu_ready              (exu_ready              ),
		.lsu_ready              (lsu_exu_ready           ),
		.exu_lsu_valid          (exu_lsu_valid          ),
		.idu_wbu_rs1            (idu_wbu_rs1            ),
		.idu_wbu_rs2            (idu_wbu_rs2            ),
		.lsu_exu_forward_rd     (lsu_exu_forward_rd     ),
		.lsu_exu_forward_RegWrite(lsu_exu_forward_RegWrite),
		.lsu_exu_forward_MemRead(lsu_exu_forward_MemRead),
		.lsu_wbu_wdata          (lsu_wbu_write_rd_data   ),
		.lsu_wbu_rd             (lsu_wbu_rd              ),
		.lsu_wbu_RegWrite       (lsu_wbu_RegWrite        ),
		.lsu_wbu_valid          (lsu_wbu_valid           ),
		.exu_lsu_forward_las    (exu_lsu_forward_las    ),
		.idu_exu_pc             (idu_exu_pc             ),
		.idu_exu_imm            (idu_exu_imm            ),
		.idu_exu_zimm           (idu_exu_zimm           ),
		.idu_exu_shamt          (idu_exu_shamt          ),
		.wbu_exu_src1           (wbu_exu_src1           ),
		.wbu_exu_src2           (wbu_exu_src2           ),
		.idu_exu_RegWrite       (idu_exu_RegWrite       ),
		.idu_exu_rd             (idu_exu_rd             ),
		.idu_exu_opcode         (idu_exu_opcode         ),
		.idu_exu_func3          (idu_exu_func3          ),
		.idu_exu_alu_op         (idu_exu_alu_op         ),
		.idu_exu_jal            (idu_exu_jal            ),
		.idu_exu_jalr           (idu_exu_jalr           ),
		.idu_exu_fencei         (idu_exu_fencei         ),
		.idu_exu_MemRead        (idu_exu_MemRead        ),
		.idu_exu_MemWrite       (idu_exu_MemWrite       ),
		.idu_exu_MemLen         (idu_exu_MemLen         ),
		.wbu_exu_csr_num1       (wbu_exu_csr_num1       ),
		.wbu_exu_csr_num2       (wbu_exu_csr_num2       ),
		.idu_exu_csr_wen1       (idu_exu_csr_wen1       ),
		.idu_exu_csr_wr_addr1   (idu_exu_csr_wr_addr1   ),
		.idu_exu_csr_ecall      (idu_exu_csr_ecall      ),
		.idu_exu_csr_mret       (idu_exu_csr_mret       ),
		.idu_exu_csr_op         (idu_exu_csr_op         ),
		.exu_flush              (exu_flush              ),
		.exu_flush_pc           (exu_flush_pc           ),
		.exu_fencei             (exu_fencei             ),
		.exu_lsu_src2           (exu_lsu_src2           ),
		.exu_lsu_RegWrite       (exu_lsu_RegWrite       ),
		.exu_lsu_rd             (exu_lsu_rd             ),
		.exu_lsu_MemRead        (exu_lsu_MemRead        ),
		.exu_lsu_MemWrite       (exu_lsu_MemWrite       ),
		.exu_lsu_MemLen         (exu_lsu_MemLen         ),
		.exu_lsu_csr            (exu_lsu_csr            ),
		.exu_lsu_csr_wen1       (exu_lsu_csr_wen1       ),
		.exu_lsu_csr_wr_addr1   (exu_lsu_csr_wr_addr1   ),
		.exu_lsu_csr_wr_data1   (exu_lsu_csr_wr_data1   ),
		.exu_lsu_csr_wr_data2   (exu_lsu_csr_wr_data2   ),
		.exu_lsu_csr_rdata      (exu_lsu_csr_rdata      ),
		.exu_lsu_csr_ecall      (exu_lsu_csr_ecall      ),
		.exu_lsu_csr_mret       (exu_lsu_csr_mret       ),
		.exu_lsu_process_result (exu_lsu_process_result ),
		.exu_lsu_pc             (exu_lsu_pc             )
	);

	// LSU
	LSU lsu (
		.clk                    (clock                  ),
		.rst                    (reset                  ),
		.exu_lsu_valid           (exu_lsu_valid          ),
		.lsu_exu_ready           (lsu_exu_ready           ),
		.wbu_lsu_ready           (wbu_lsu_ready           ),
		.lsu_wbu_valid           (lsu_wbu_valid           ),
		.exu_lsu_forward_las     (exu_lsu_forward_las    ),
		.exu_lsu_RegWrite        (exu_lsu_RegWrite       ),
		.exu_lsu_rd              (exu_lsu_rd             ),
		.exu_lsu_MemRead         (exu_lsu_MemRead        ),
		.exu_lsu_MemWrite        (exu_lsu_MemWrite       ),
		.exu_lsu_MemLen          (exu_lsu_MemLen         ),
		.addr                   (exu_lsu_process_result ),
		.data_in                (exu_lsu_src2           ),
		.exu_lsu_csr             (exu_lsu_csr            ),
		.exu_lsu_csr_wen1        (exu_lsu_csr_wen1       ),
		.exu_lsu_csr_wr_data1    (exu_lsu_csr_wr_data1   ),
		.exu_lsu_csr_wr_data2    (exu_lsu_csr_wr_data2   ),
		.exu_lsu_csr_wr_addr1    (exu_lsu_csr_wr_addr1   ),
		.exu_lsu_csr_rdata       (exu_lsu_csr_rdata      ),
		.exu_lsu_csr_ecall       (exu_lsu_csr_ecall      ),
		.exu_lsu_csr_mret        (exu_lsu_csr_mret       ),
		.exu_lsu_process_result  (exu_lsu_process_result ),
		.exu_lsu_pc              (exu_lsu_pc             ),
		.lsu_exu_forward_rd      (lsu_exu_forward_rd     ),
		.lsu_exu_forward_RegWrite(lsu_exu_forward_RegWrite),
		.lsu_exu_forward_MemRead (lsu_exu_forward_MemRead ),
		.lsu_wbu_csr_wr_data1    (lsu_wbu_csr_wr_data1    ),
		.lsu_wbu_csr_wr_data2    (lsu_wbu_csr_wr_data2    ),
		.lsu_wbu_csr_wr_addr1    (lsu_wbu_csr_wr_addr1    ),
		.lsu_wbu_csr_wen1        (lsu_wbu_csr_wen1        ),
		.lsu_wbu_csr_ecall       (lsu_wbu_csr_ecall       ),
		.lsu_wbu_RegWrite        (lsu_wbu_RegWrite        ),
		.lsu_wbu_rd              (lsu_wbu_rd              ),
		.lsu_wbu_pc              (lsu_wbu_pc              ),
		.lsu_axi_arvalid        (lsu_axi_arvalid        ),
		.axi_lsu_arready        (lsu_arready            ),
		.lsu_axi_araddr         (lsu_axi_araddr         ),
		.lsu_axi_arid           (lsu_axi_arid           ),
		.lsu_axi_arlen          (lsu_axi_arlen          ),
		.lsu_axi_arsize         (lsu_axi_arsize         ),
		.lsu_axi_arburst        (lsu_axi_arburst        ),
		.axi_lsu_rdata          (axi_lsu_rdata          ),
		.axi_lsu_rvalid         (lsu_rvalid             ),
		.lsu_axi_rready         (lsu_axi_rready         ),
		.axi_lsu_rresp          (lsu_rresp              ),
		.axi_lsu_rid            (lsu_rid                ),
		.axi_lsu_rlast          (lsu_rlast              ),
		.lsu_axi_awaddr         (lsu_axi_awaddr         ),
		.lsu_axi_awvalid        (lsu_axi_awvalid        ),
		.axi_lsu_awready        (axi_lsu_awready        ),
		.lsu_axi_awid           (lsu_axi_awid           ),
		.lsu_axi_awlen          (lsu_axi_awlen          ),
		.lsu_axi_awsize         (lsu_axi_awsize         ),
		.lsu_axi_awburst        (lsu_axi_awburst        ),
		.lsu_axi_wdata          (lsu_axi_wdata          ),
		.lsu_axi_wstrb          (lsu_axi_wstrb          ),
		.lsu_axi_wvalid         (lsu_axi_wvalid         ),
		.lsu_axi_wlast          (lsu_axi_wlast          ),
		.axi_lsu_wready         (axi_lsu_wready         ),
		.axi_lsu_bresp          (axi_lsu_bresp          ),
		.axi_lsu_bvalid         (axi_lsu_bvalid         ),
		.lsu_axi_bready         (lsu_axi_bready         ),
		.axi_lsu_bid            (axi_lsu_bid            ),
		.lsu_wbu_write_rd_data   (lsu_wbu_write_rd_data   )
	);

	// WBU
	WBU wbu (
		.clk         (clock               ),
		.rst         (reset               ),
		.wen         (lsu_wbu_RegWrite     ),
		.lsu_wbu_valid(lsu_wbu_valid        ),
		.wbu_lsu_ready(wbu_lsu_ready        ),
		.wdata       (lsu_wbu_write_rd_data),
		.waddr       (lsu_wbu_rd           ),
		.rs1         (idu_wbu_rs1         ),
		.rs2         (idu_wbu_rs2         ),
		.src1        (wbu_exu_src1        ),
		.src2        (wbu_exu_src2        ),
		.csr1_raddr  (idu_wbu_csr_addr1   ),
		.csr2_raddr  (idu_wbu_csr_addr2   ),
		.csr1_wen    (lsu_wbu_csr_wen1     ),
		.is_ecall    (lsu_wbu_csr_ecall    ),
		.csr1_wdata  (lsu_wbu_csr_wr_data1 ),
		.csr2_wdata  (lsu_wbu_csr_wr_data2 ),
		.csr1_waddr  (lsu_wbu_csr_wr_addr1 ),
		.csr1_rdata  (wbu_exu_csr_num1    ),
		.csr2_rdata  (wbu_exu_csr_num2    ),
		.lsu_wbu_pc   (lsu_wbu_pc           ),
		.wbu_pc       (wbu_pc               )
	);

	// Instruction cache
	Icache #(
		.ICACHE_SIZE(64),
		.BLOCK_SIZE(16)
	) icache (
		.clk         (clock             ),
		.reset       (reset             ),
		.is_fencei   (exu_fencei        ),
		.addr        (next_pc           ),
		.inst        (icache_inst       ),
		.valid       (icache_valid      ),
		.axi_araddr  (icache_araddr ),
		.axi_arvalid (icache_arvalid),
		.axi_arready (ifu_arready       ),
		.axi_arid    (icache_arid   ),
		.axi_arlen   (icache_arlen  ),
		.axi_arsize  (icache_arsize ),
		.axi_arburst (icache_arburst),
		.axi_rvalid  (ifu_rvalid        ),
		.axi_rready  (icache_rready ),
		.axi_rdata   (ifu_rdata         ),
		.axi_rresp   (ifu_rresp         ),
		.axi_rid     (ifu_rid           ),
		.axi_rlast   (ifu_rlast         )
	);

	// CLINT
	CLINT clint (
		.clock  (clock        ),
		.reset  (reset        ),
		.arready(clint_arready),
		.arvalid(clint_arvalid),
		.araddr (clint_araddr ),
		.rready (clint_rready ),
		.rvalid (clint_rvalid ),
		.rresp  (clint_rresp  ),
		.rdata  (clint_rdata  ),
		.rlast  (clint_rlast  )
	);

	// xbar
	Xbar xbar (
		.clk               (clock               ),
		.reset             (reset               ),
		.ifu_arready       (ifu_arready         ),
		.ifu_arvalid       (ifu_arvalid         ),
		.ifu_araddr        (ifu_araddr          ),
		.ifu_arid          (ifu_arid            ),
		.ifu_arlen         (ifu_arlen           ),
		.ifu_arsize        (ifu_arsize          ),
		.ifu_arburst       (ifu_arburst         ),
		.ifu_rready        (ifu_rready          ),
		.ifu_rvalid        (ifu_rvalid          ),
		.ifu_rresp         (ifu_rresp           ),
		.ifu_rdata         (ifu_rdata           ),
		.ifu_rlast         (ifu_rlast           ),
		.ifu_rid           (ifu_rid             ),
		.lsu_awready       (axi_lsu_awready     ),
		.lsu_awvalid       (lsu_axi_awvalid     ),
		.lsu_awaddr        (lsu_axi_awaddr      ),
		.lsu_awid          (lsu_axi_awid        ),
		.lsu_awlen         (lsu_axi_awlen       ),
		.lsu_awsize        (lsu_axi_awsize      ),
		.lsu_awburst       (lsu_axi_awburst     ),
		.lsu_wready        (axi_lsu_wready      ),
		.lsu_wvalid        (lsu_axi_wvalid      ),
		.lsu_wdata         (lsu_axi_wdata       ),
		.lsu_wstrb         (lsu_axi_wstrb       ),
		.lsu_wlast         (lsu_axi_wlast       ),
		.lsu_bready        (lsu_axi_bready      ),
		.lsu_bvalid        (axi_lsu_bvalid      ),
		.lsu_bresp         (axi_lsu_bresp       ),
		.lsu_bid           (axi_lsu_bid         ),
		.lsu_arready       (lsu_arready        ),
		.lsu_arvalid       (lsu_axi_arvalid     ),
		.lsu_araddr        (lsu_axi_araddr      ),
		.lsu_arid          (lsu_axi_arid        ),
		.lsu_arlen         (lsu_axi_arlen       ),
		.lsu_arsize        (lsu_axi_arsize      ),
		.lsu_arburst       (lsu_axi_arburst     ),
		.lsu_rready        (lsu_axi_rready      ),
		.lsu_rvalid        (lsu_rvalid          ),
		.lsu_rresp         (lsu_rresp           ),
		.lsu_rdata         (axi_lsu_rdata       ),
		.lsu_rlast         (lsu_rlast           ),
		.lsu_rid           (lsu_rid             ),
		.io_master_awready (io_master_awready   ),
		.io_master_awvalid (io_master_awvalid   ),
		.io_master_awaddr  (io_master_awaddr    ),
		.io_master_awid    (io_master_awid      ),
		.io_master_awlen   (io_master_awlen     ),
		.io_master_awsize  (io_master_awsize    ),
		.io_master_awburst (io_master_awburst   ),
		.io_master_wready  (io_master_wready    ),
		.io_master_wvalid  (io_master_wvalid    ),
		.io_master_wdata   (io_master_wdata     ),
		.io_master_wstrb   (io_master_wstrb     ),
		.io_master_wlast   (io_master_wlast     ),
		.io_master_bready  (io_master_bready    ),
		.io_master_bvalid  (io_master_bvalid    ),
		.io_master_bresp   (io_master_bresp     ),
		.io_master_bid     (io_master_bid       ),
		.io_master_arready (io_master_arready   ),
		.io_master_arvalid (io_master_arvalid   ),
		.io_master_araddr  (io_master_araddr    ),
		.io_master_arid    (io_master_arid      ),
		.io_master_arlen   (io_master_arlen     ),
		.io_master_arsize  (io_master_arsize    ),
		.io_master_arburst (io_master_arburst   ),
		.io_master_rready  (io_master_rready    ),
		.io_master_rvalid  (io_master_rvalid    ),
		.io_master_rresp   (io_master_rresp     ),
		.io_master_rdata   (io_master_rdata     ),
		.io_master_rlast   (io_master_rlast     ),
		.io_master_rid     (io_master_rid       ),
		.clint_arready     (clint_arready       ),
		.clint_arvalid     (clint_arvalid       ),
		.clint_araddr      (clint_araddr        ),
		.clint_rready      (clint_rready        ),
		.clint_rvalid      (clint_rvalid        ),
		.clint_rresp       (clint_rresp         ),
		.clint_rdata       (clint_rdata         ),
		.clint_rlast       (clint_rlast         )
	);

endmodule
