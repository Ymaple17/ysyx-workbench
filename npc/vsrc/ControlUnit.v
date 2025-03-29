module ControlUnit (
    input  wire [31:0] instr,
    output wire        mem_read,
    output wire        mem_write,
    output wire        reg_write,
    output wire        is_ebreak,
    output wire        is_auipc,
    output wire        is_lui,
    output wire        is_jal,
    output wire        is_jalr
);
    wire [6:0] opcode = instr[6:0];

    assign mem_read  = (opcode == 7'b0000011); // LW
    assign mem_write = (opcode == 7'b0100011); // SW
    assign reg_write = (opcode == 7'b0010011) || (opcode == 7'b0000011) || // ADDI, LW
                       (opcode == 7'b0010111) || (opcode == 7'b0110111) || // AUIPC, LUI
                       (opcode == 7'b1101111) || (opcode == 7'b1100111);  // JAL, JALR
    assign is_ebreak = (instr == 32'h00100073); // EBREAK
    assign is_auipc  = (opcode == 7'b0010111); // AUIPC
    assign is_lui    = (opcode == 7'b0110111); // LUI
    assign is_jal    = (opcode == 7'b1101111); // JAL
    assign is_jalr   = (opcode == 7'b1100111); // JALR
endmodule
