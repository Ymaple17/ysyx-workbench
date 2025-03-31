module decoder(
	input [31:0]inst,
	output [6:0]opcode,
  output [6:0]func7,
  output [2:0]func3,
	output [4:0]rs1,
	output [4:0]rs2,
	output [4:0]rd,
	output [31:0]imm
);
	
	wire I_type;
	wire U_type;
	wire B_type;
	wire J_type;
	wire S_type;

	wire [31:0]I_imm;
	wire [31:0]U_imm;
	wire [31:0]B_imm;
	wire [31:0]J_imm;
	wire [31:0]S_imm;

	assign opcode = inst[6:0];
  assign func7  = inst[31:25];
  assign func3  = inst[14:12];
	assign rs1 = inst[19:15];
	assign rs2 = inst[24:20];
	assign rd = inst[11:7];

	assign I_type = (inst[6:0] == 7'b1100111/*jalr*/) | (inst[6:0] == 7'b0000011/*load*/) | (inst[6:0] == 7'b0010011) | (inst[6:0] == 7'b0011011);
	assign U_type = (inst[6:0] == 7'b0110111/*lui*/) | (inst[6:0] == 7'b0010111/*auipc*/);
	assign B_type = (inst[6:0] == 7'b1100011);
	assign J_type = (inst[6:0] == 7'b1101111/*jal*/);
	assign S_type = (inst[6:0] == 7'b0100011/*store*/);

	assign I_imm = {{20{inst[31]}}, inst[31:20]};
	assign U_imm = {{inst[31:12]}, {12{1'b0}}};
	assign B_imm = {{20{inst[31]}}, inst[7], inst[30:25], inst[11:8], 1'b0};
	assign J_imm = {{12{inst[31]}}, inst[19:12], inst[20], inst[30:21], 1'b0};
	assign S_imm = {{20{inst[31]}}, inst[31:25], inst[11:7]};

	assign imm = I_type ? I_imm :	
							 U_type ? U_imm :
							 B_type ? B_imm :
							 J_type ? J_imm :
							 S_type ? S_imm : 32'b0;

endmodule
							 
