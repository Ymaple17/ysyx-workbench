`include "include/defs.vh"

module EXU (
    input wire clk,
    input wire rst,
    input wire [`DATA_WIDTH-1:0] Op1,
    input wire [`DATA_WIDTH-1:0] Op2,
    input wire [4:0] alu_op,
    input wire [1:0] wb_sel,
    input wire [`DATA_WIDTH-1:0] pc_plus4,
    input wire [`DATA_WIDTH-1:0] dmem_rdata,
    output wire [`DATA_WIDTH-1:0] dmem_addr,
    output wire [`DATA_WIDTH-1:0] reg_write_data
);
    assign dmem_addr = result;
    wire [`DATA_WIDTH-1:0] result;
    ALU alu_instance (
       .A(Op1),
       .B(Op2),
      .ALUFun(alu_op),
      .Result(result)
    );


    
    assign reg_write_data =(wb_sel == 2'b00 ? {`DATA_WIDTH{1'b0}} :
                           (wb_sel == 2'b01 ? pc_plus4 :
                           (wb_sel == 2'b10 ? result :
                           (wb_sel == 2'b11 ? dmem_rdata : {`DATA_WIDTH{1'b0}}))));

endmodule
