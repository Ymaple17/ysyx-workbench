module ysyx_25020039_imm(
   input [31:0]inst,
   input [2:0]immtype,
   output [31:0]immexit
);
   wire [31:0]immI;
   assign immI = {{20{inst[31]}},inst[31:20]};
   assign immexit = immI;
endmodule
