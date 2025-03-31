module idu(
  input [6:0]idu_opcode,
  input [6:0]idu_func7,
  input [2:0]idu_func3,
  //input [4:0]idu_rs1,
  input [4:0]idu_rs2,
  output pc_sel,
  output imm_sel,
	output regwrite,


	output jal_en,
	output jalr_en,
  output ebreak,

  output [3:0]lgc_op,
  output [2:0]md_op,
  output [2:0]br_op,
  output lgc_en,
  output md_en,
  output br_en,


  output lb,
  output lh,
  output lw,
  //output ld,
  output lbu,
  output lhu,
  //output lwu,


  output sb,
  output sh,
  output sw,
  //output sd,

  output mem_wen,
  output mem_ren,


	output w_pc,
	output w_alu,
  output w_load
);


	wire R_type;
	wire I_type;
	wire U_type;
  wire S_type;
  //wire B_type;


	wire   auipc_en = (idu_opcode == 7'b0010111);
	wire   lui_en   = (idu_opcode == 7'b0110111);
  wire   load_en  = (idu_opcode == 7'b0000011);
  wire   store_en = (idu_opcode == 7'b0100011);
	assign jal_en   = (idu_opcode == 7'b1101111);
	assign jalr_en  = (idu_opcode == 7'b1100111);
  assign br_en    = (idu_opcode == 7'b1100011);
 
  
  /*运算控制信号*/
  wire immop_en = (idu_opcode == 7'b0010011) & (idu_func3[1:0] != 2'b01); //I_type,addi、slti、sltiu、xori、ori、andi
  wire immsf_en = (idu_opcode == 7'b0010011) & (idu_func3[1:0] == 2'b01); //I_type,slli、srli、srai
  wire rop_en   = (idu_opcode == 7'b0110011) & (idu_func7[0]   == 1'b0);  //R_type,普通算术和逻辑运算
  wire mdrsop_en  = (idu_opcode == 7'b0110011) & (idu_func7[0]   == 1'b1);  //R_ype,乘除和取余运算
  

  assign lgc_en = immop_en | immsf_en | rop_en | auipc_en | lui_en | jalr_en | jal_en | load_en | store_en ;
  assign  md_en = mdrsop_en;

  assign ebreak   = (idu_opcode == 7'b1110011) & (idu_func7 == 7'b0) & (idu_rs2 == 5'b00001);

	assign I_type = jalr_en | load_en | immop_en | immsf_en;
	assign R_type = rop_en | mdrsop_en ;
	assign U_type = lui_en | auipc_en ;
  assign S_type = store_en ;
  //assign B_type   = br_en;


  /*操作数选择*/
	assign pc_sel   = auipc_en | jal_en;      //select pc
  assign imm_sel  = I_type | U_type | jal_en | S_type ; //select imm

	assign regwrite = jal_en | jalr_en | I_type | R_type | U_type;



  assign lgc_op   = ({4{auipc_en}} & (4'b0000)) |
                  ({4{  lui_en}} & (4'b1111)) |
                  ({4{  rop_en}} & {idu_func7[5], idu_func3}) |
                  ({4{immop_en}} & {        1'b0, idu_func3}) |
                  ({4{immsf_en}} & {idu_func7[5], idu_func3});  //func7[5] is inst[30]
  assign md_op = idu_func3;
  assign br_op = idu_func3;


  assign lb  = load_en & (idu_func3 == 3'b000);
  assign lh  = load_en & (idu_func3 == 3'b001);
  assign lw  = load_en & (idu_func3 == 3'b010);
  //assign ld  = load_en & (idu_func3 == 3'b011);
  assign lbu = load_en & (idu_func3 == 3'b100);
  assign lhu = load_en & (idu_func3 == 3'b101);
  //assign lwu = load_en & (idu_func3 == 3'b110);

  assign sb = store_en & (idu_func3 == 3'b000);
  assign sh = store_en & (idu_func3 == 3'b001);
  assign sw = store_en & (idu_func3 == 3'b010);
  //assign sd = store_en & (idu_func3 == 3'b011);

  assign mem_ren = lb | lh | lw | lbu | lhu;
  assign mem_wen = sb | sh | sw ;

	//写回控制信号
	assign w_pc   = jal_en | jalr_en;
	assign w_alu  = auipc_en | immop_en | lui_en | rop_en | immsf_en;
  assign w_load = load_en;
endmodule
