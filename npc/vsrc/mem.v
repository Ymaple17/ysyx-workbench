module mem(
  input lb,
  input lh,
  input lw,
  //input ld,   //ld、lwu和sd均为rv64的指令，这里先注释掉
  input lbu,
  input lhu,
  //input lwu,

  input sb,
  input sh,
  input sw,
  //input sd,

  input [31:0]addr,   //load与store的地址均为x[rs1] + imm，从alu读入即可
  input [31:0]wdata,  //store指令只写入x[rs2]的值，直接从顶层模块传入即可

  input [31:0]rdata,  //从内存取出的数据

  output [3:0]store_len,
  output [31:0]load_data,
  output [31:0]store_data,

  output [31:0]mem_addr
);



  assign store_len = sb ? 4'd1 :
                     sh ? 4'd2 :
                     sw ? 4'd4 :
                          4'b0;

  assign store_data = ({32{sb}} & {24'b0, wdata[7:0] }) |
                      ({32{sh}} & {16'b0, wdata[15:0]}) |
                      ({32{sw}} & {      wdata       }) ;


  assign mem_addr = addr;

  /*对取出的数据符号扩展或0扩展*/
  /*符号扩展：把最高位（符号位）复制若干次，填充至32位*/
  wire [31:0]lb_data  = {{24{rdata[7] }}, rdata[7:0] };
  wire [31:0]lh_data  = {{16{rdata[15]}}, rdata[15:0]};
  wire [31:0]lw_data  =  rdata;
  wire [31:0]lbu_data = {{24{1'b0}}, rdata[7:0] };
  wire [31:0]lhu_data = {{16{1'b0}}, rdata[15:0]};


  assign load_data = ({32{ lb}} &  lb_data) |
                     ({32{ lh}} &  lh_data) |
                     ({32{ lw}} &  lw_data) |
                     ({32{lbu}} & lbu_data) |
                     ({32{lhu}} & lhu_data) ;

endmodule
