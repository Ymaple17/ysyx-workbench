module cpu(
	input clk,
	input rst,
	output reg[31:0]outdata
);
 

reg [31:0]next_pc;
reg [6:0]op;
reg [4:0]rd;
reg [2:0]func3;
reg[31:0]pc;
wire [31:0]ins;
reg [31:0] rs1;
reg [31:0] imm;
reg [31:0] rs1_data;
reg [31:0] imm_data;
initial begin
	pc = 32'h8000_0000;
	next_pc = pc + 4;
end
always @(posedge clk or posedge rst)begin
	if(rst)begin
		pc <= 32'h8000_0000;
		next_pc <= pc + 4;		
	end
	else begin
		pc <= next_pc;
		next_pc <= pc + 4;	
	end
 
end
 
ifu1 ifu(
	.pc(pc),
	.clk(clk),
	.ins(ins)
);
 

idu1 idu(
	.ins(ins),
	.op(op),
	.rd(rd),
	.clk(clk),
	.func3(func3),
	.rs1(rs1),
	.imm(imm)
);
 
//ALU
alu1 alu(
	.imm(imm_data),
	.rs1(rs1_data),
	.func3(func3),
	.clk(clk),
	.outdata(outdata)
);
 
endmodule
 
import "DPI-C" function int gpr(int idx);
module idu1(
    input [31:0] ins,  // 指令
    output [6:0] op,
    output [4:0] rd,
    input clk,
    output [2:0] func3,
    output reg[31:0] rs1,
    output reg[31:0] imm
);
reg [31:0]ins1;
initial
	ins1 = {{27{1'b0}},ins[19:15]};
always @(posedge clk) begin
    rs1 = gpr(ins1);
    imm = {{20{1'b0}},ins[31:20]};
end
endmodule
 
module ifu1(
	input [31:0]pc,
	input clk,
	output reg[31:0]ins
);
 
	//import "DPI-C" function int init_mem(int size);
 
always @(posedge clk)
 ins = 32'b000000000101_00000000000010010011;
endmodule
module alu1(
	input [31:0]imm,
	input [31:0]rs1,
	input [2:0]func3,
	input clk,
//	output reg wen,
	output reg [31:0]outdata
);
	
	always @(posedge clk)begin
		case(func3) 
			3'b000:begin
				outdata <= 32'b0 + imm;
				//wen <= 1'b1;
				end			
			default:begin
				$display("ERROR!");
		end
		endcase
	end
 
endmodule
