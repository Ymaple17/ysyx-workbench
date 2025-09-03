`include "include/defs.vh"

module ALU (
    input [`DATA_WIDTH-1:0] A,
    input [`DATA_WIDTH-1:0] B,
    input [4:0] ALUFun,
    output reg [`DATA_WIDTH-1:0] Result
);

    // Internal signals declaration
    wire [`DATA_WIDTH-1:0] add_result = A + B;                  // Addition
    wire [`DATA_WIDTH-1:0] op1_result = A;                          // Pass A as result


    // Instantiate MuxKey module
    MuxKey #(2, 5, `DATA_WIDTH) alu_mux (
        .out(Result),
        .key(ALUFun),
        .lut({
            5'b00000, add_result,  // Addition
            5'b01010, op1_result   // Pass A as result
        })
    );

endmodule
