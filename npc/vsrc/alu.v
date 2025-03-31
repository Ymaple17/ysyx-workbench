module alu(
  input [31:0]opdata1,
  input [31:0]opdata2,

  /*alu控制信号*/
  input lgc_en,
  input md_en,
  input br_en,
  input [3:0]lgc_op,
  input [2:0]md_op,
  input [2:0]br_op,


  output brpc_en,
  output [31:0]result
);

  wire signed [31:0]sopdata1 = opdata1;
  wire signed [31:0]sopdata2 = opdata2;

  wire unsigned [31:0]usopdata1 = sopdata1;
  wire unsigned [31:0]usopdata2 = sopdata2;

  wire [31:0]lgc_result;

  assign lgc_result = (lgc_op == 4'b0000) ? (sopdata1  +  sopdata2) : //add
                      (lgc_op == 4'b1000) ? (sopdata1  -  sopdata2) : //sub
                      (lgc_op == 4'b1111) ? sopdata2              : //lui
                      (lgc_op == 4'b0100) ? (sopdata1  ^  sopdata2) : //xor
                      (lgc_op == 4'b0110) ? (sopdata1  |  sopdata2) : //or
                      (lgc_op == 4'b0111) ? (sopdata1  &  sopdata2) : //and
                      (lgc_op == 4'b0001) ? (sopdata1 <<  sopdata2[5:0]) : //sll
                      (lgc_op == 4'b0101) ? (sopdata1 >>  sopdata2[5:0]) : //srl
                     // (lgc_op == 4'b1101) ? ({{{32{sopdata1[31]}}, sopdata1} >> sopdata2[5:0]}[31:0]) : //sra
                      (lgc_op == 4'b1101) ? ({({32{sopdata1[31]}} << 6'd32 - {1'b0, sopdata2[4:0]}) | sopdata1 >> sopdata2[4:0]}) : //sra
                      (lgc_op == 4'b0010) ? ((sopdata1 <  sopdata2) ? 32'b1 : 32'b0) : //slt
                      (lgc_op == 4'b0011) ? ((usopdata1 < usopdata2) ? 32'b1 : 32'b0) : //sltu
                      32'b0;
  wire [63:0] ss_mul_result =  sopdata1 * sopdata2;
  wire [63:0] uu_mul_result = usopdata1 * usopdata2;
  wire [63:0] su_mul_result =  sopdata1 * usopdata2;


  wire [31:0] md_result;
  assign md_result = (md_op == 3'b000) ? (ss_mul_result[31:0]) : //mul
                     (md_op == 3'b001) ? ({ss_mul_result >> 32}[31:0]) : //mulh
                     (md_op == 3'b010) ? ({su_mul_result >> 32}[31:0]) : //mulhsu
                     (md_op == 3'b011) ? ({uu_mul_result >> 32}[31:0]) : //mulhu
                     (md_op == 3'b100) ? (sopdata1 / sopdata2) : //div
                     (md_op == 3'b101) ? (usopdata1 / usopdata2) : //divu
                     (md_op == 3'b110) ? (sopdata1 % sopdata2) : //rem
                     (md_op == 3'b111) ? (usopdata1 % usopdata2) : //remu
                     32'b0;


  /*alu_result*/
  assign result = lgc_en ? lgc_result : 
                   md_en ?  md_result :
                   32'b0;



  wire br_result;
  assign br_result = (br_op == 3'b000) ? (sopdata1 == sopdata2) : //beq
                     (br_op == 3'b001) ? (sopdata1 != sopdata2) : //bne
                     (br_op == 3'b100) ? (sopdata1  < sopdata2) : //blt
                     (br_op == 3'b101) ? (sopdata1 >= sopdata2) : //bge
                     (br_op == 3'b110) ? (usopdata1 < usopdata2) :  //bltu
                     (br_op == 3'b111) ? (usopdata1 >= usopdata2) :  //bgeu
                   1'b0;


  assign brpc_en = br_result & br_en;
endmodule
