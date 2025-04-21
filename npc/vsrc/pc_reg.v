module pc_reg(
		input clk,
		input rst,
		input jal_en,
		input jalr_en,
    input brpc_en,
    input [31:0]imm,
		input [31:0]result,
    input pc_wen,
		output [31:0]dnpc,
		output [31:0]snpc,
    output reg [31:0]tmp_pc,
		output reg [31:0]pc
);

  wire [31:0]br_pc = pc + imm;
	wire [31:0]jal_pc = result;
	wire [31:0]jalr_pc = result & {{31{1'b1}}, 1'b0};//跳转地址最低位清零

	wire snpc_en = (!jal_en) & (!jalr_en) &(!brpc_en);	

	assign snpc = pc + 4;

  assign dnpc = snpc_en ? snpc    :
                jal_en  ? jal_pc  :
                jalr_en ? jalr_pc :
                brpc_en ? br_pc   :
                pc + 4;


	always @(posedge clk)	begin
			if(rst)	begin
				pc <= 32'h80000000;	
			end
			else if (pc_wen) begin
        		pc <= dnpc;
        		tmp_pc <= pc;
			end
	end
		
endmodule
