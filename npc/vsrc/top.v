module top(
	input clk,
	input rst,
	input [31:0]inst,
  //input pc_wen,
	output [31:0]dnpc,
	output [31:0]snpc,
  output reg [31:0]tmp_pc,
	output reg [31:0]pc
);


	wire [31:0]imm;
	wire [6:0]opcode;
  wire [6:0]func7;
  wire [2:0]func3;
	wire [4:0]rs1;
	wire [4:0]rs2;
	wire [4:0]rd;

	wire [31:0]rd_data1;
	wire [31:0]rd_data2;
	wire [31:0]alu_data1;
  wire [31:0]alu_data2;

	wire [31:0]result;	//add result
	


  wire brpc_en; //分支pc使能信号

  wire w_pc;
	wire w_alu;
  wire w_load;


	pc_reg pc_reg1(
		.clk(clk),
		.rst(rst),
		.jal_en(jal_en),
		.jalr_en(jalr_en),
    .brpc_en(brpc_en),
    .imm(imm),
		.result(result),
    .pc_wen(1'b1),
		.dnpc(dnpc),
		.snpc(snpc),
    .tmp_pc(tmp_pc),
		.pc(pc)
	);

	decoder decoder(
		.inst(inst),
		.opcode(opcode),
    .func7(func7),
    .func3(func3),
    .rs1(rs1),
		.rs2(rs2),
		.rd(rd),
		.imm(imm)
	);

	regfile regfile1(
		.clk(clk),
		.rst(rst),
		.w_en(regwrite),
		.rs1(rs1),
		.rs2(rs2),
		.rd(rd),
		.w_pc(w_pc),
		.w_alu(w_alu),
    		.w_load(w_load),
    		.load_data(load_data),
		.alu_result(result),
		.pc_result(snpc),
		.rd_data1(rd_data1),
		.rd_data2(rd_data2)
	);

	wire pc_sel;
  	wire imm_sel;
	wire regwrite;

	wire jal_en;
	wire jalr_en;
  	wire br_en;   //分支指令使能信号
  	wire lgc_en;
  	wire md_en;
  	wire ebreak;

  	wire [3:0]lgc_op; //加、减、逻辑运算和移位运算
  	wire [2:0]md_op;  //乘、除和取余运算
  	wire [2:0]br_op;  //分支跳转
  	wire lb;
  	wire lh;
  	wire lw;
  	wire lbu;
  	wire lhu;
  	wire sb;
  	wire sh;
  	wire sw;
  	wire mem_wen;
  	wire mem_ren;
	idu idu1(
		.idu_opcode(opcode),
    		.idu_func7(func7),
    		.idu_func3(func3),
    		.idu_rs2(rs2),
		.pc_sel(pc_sel),
    		.imm_sel(imm_sel),
		.regwrite(regwrite),
		.jal_en(jal_en),
		.jalr_en(jalr_en),
    .ebreak(ebreak),
    .lgc_op(lgc_op),
    .md_op(md_op),
    .br_op(br_op),
    .lgc_en(lgc_en),
    .md_en(md_en),
    .br_en(br_en),

    .lb(lb),
    .lh(lh),
    .lw(lw),
    //.ld(ld),
    .lbu(lbu),
    .lhu(lhu),
    //.lwu(lwu),
    .sb(sb),
    .sh(sh),
    .sw(sw),
    //.sd(sd),
    .mem_wen(mem_wen),
    .mem_ren(mem_ren),
    .w_pc(w_pc),
    .w_alu(w_alu),
    .w_load(w_load)
	);

  wire [31:0]rdata;
  wire [3:0]store_len;
  wire [31:0]load_data;
  wire [31:0]store_data;
  wire [31:0]mem_addr;
  mem mem1(
    .lb(lb),
    .lh(lh),
    .lw(lw),
    //.ld(ld),
    .lbu(lbu),
    .lhu(lhu),
    //.lwu(lwu),
    
    .sb(sb),
    .sh(sh),
    .sw(sw),
    //.sd(sd),
    
    .addr(result),
    .wdata(rd_data2),
    .rdata(rdata),

    .store_len(store_len),
    .load_data(load_data),
    .store_data(store_data),

    .mem_addr(mem_addr)
  );

	mux2_1 alu_data1_mux(
		.a(rd_data1),
		.b(pc),
		.sel(pc_sel),
		.opdata(alu_data1)
	);

  mux2_1 alu_data2_mux(
    .a(rd_data2),
    .b(imm),
    .sel(imm_sel),
    .opdata(alu_data2)
  );
	alu alu1(
		.opdata1(alu_data1),
		.opdata2(alu_data2),
    		.lgc_en(lgc_en),
    		.md_en(md_en),
    		.br_en(br_en),
    		.lgc_op(lgc_op),
    		.md_op(md_op),
    		.br_op(br_op),
    		.brpc_en(brpc_en),
		.result(result)
		);

    import "DPI-C" function void npc_pmem_read(
      input int raddr,
      output int rdata,
      input bit ren
    );
    import "DPI-C" function void npc_pmem_write(
      input int waddr,
      input int wdata,
      input byte len,
      input bit wen
    );

    always @(posedge clk) begin
      npc_pmem_read(mem_addr, rdata, mem_ren);
      npc_pmem_write(mem_addr, store_data, {4'b0,store_len}, mem_wen);
    end

    import "DPI-C" function void npc_trap();
    always @(*) begin
      if(ebreak)  begin
        //$display("ebreak");
        npc_trap();
      end
    end
endmodule




	
