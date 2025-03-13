module id(
    input [31:0] instr,
    output reg [31:0] imm,
    output reg [3:0] ALUctr,
    output reg ALUBsrc,
    output reg RegWr,
    output reg [2:0] branch,
    output reg MemtoReg,
    output reg [2:0] MemOp,
    output reg MemWr
);
    reg [6:0] opcode;
    reg [2:0] func3;
    reg [6:0] func7;
    reg [31:0] immI;
    reg [31:0] immS;
    reg [31:0] immB;
    reg [31:0] immJ;

    assign opcode = instr[6:0];
    assign func3  = instr[14:12];
    assign func7  = instr[31:25];
    assign immI = {{20{instr[31]}}, instr[31:20]};
    assign immS = {{20{instr[31]}}, instr[31:25], instr[11:7]};
    assign immB = {{20{instr[31]}}, instr[7], instr[30:25], instr[11:8], 1'b0};
    assign immJ = {{12{instr[31]}}, instr[19:12], instr[20], instr[30:21], 1'b0};
    import "DPI-C" function void nemu_trap();
    
    always @(*) begin
    	if(instr==32'h00100073) 
    	   begin
    	    nemu_trap();
    	   end
    end
        
    always @(*) begin
        case(opcode)
            7'b0010011: begin  // instr type I 
                case(func3)
                    3'b000: begin  // addi
                        imm     = immI;
                        ALUctr  = 4'b0000;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemOp   = 3'b000;
                        MemWr   = 1'b0;
                        MemtoReg = 1'b0;
                    end
                    3'b010: begin // slti : set less than
                        imm     = immI;
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemOp   = 3'b000;
                        MemWr   = 1'b0;
                        MemtoReg = 1'b0;
                    end
                    3'b011: begin // sltiu : set less than unsigned
                        imm     = immI;
                        ALUctr  = 4'b1010;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemOp   = 3'b000;
                        MemWr   = 1'b0;
                        MemtoReg = 1'b0;
                    end
                    3'b100: begin // xori
                        imm     = immI;
                        ALUctr  = 4'b0100;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;    
                        branch  = 3'b000;
                        MemOp   = 3'b000;
                        MemWr   = 1'b0;    
                        MemtoReg = 1'b0;              
                    end
                    3'b110: begin // ori
                        imm     = immI;
                        ALUctr  = 4'b0110;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;    
                        branch  = 3'b000;
                        MemOp   = 3'b000;
                        MemWr   = 1'b0;
                        MemtoReg = 1'b0;
                    end
                    3'b111: begin // andi
                        imm     = immI;
                        ALUctr  = 4'b0111;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                    3'b001: begin // slli : shfit left logically
                        imm     = immI;
                        ALUctr  = 4'b0001;
                        ALUBsrc = 1'b1;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                    3'b101: begin
                        case(func7)
                            7'b0000000: begin // srli : shfit left logic
                                imm     = immI;
                                ALUctr  = 4'b0101;
                                ALUBsrc = 1'b1;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                            7'b0100000: begin // srai : shift right arith
                                imm     = immI;
                                ALUctr  = 4'b1101;
                                ALUBsrc = 1'b1;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                            default: begin
                                imm     = 32'd0;
                                ALUctr  = 4'b000;
                                ALUBsrc = 1'b1;
                                RegWr   = 1'b0;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                        endcase
                    end
                endcase
            end
            7'b0110011: begin // instr type R
                case(func3)
                    3'b000: begin
                        case(func7)
                            7'b0000000: begin // add
                                ALUctr  = 4'b0000;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end 
                            7'b0100000: begin // sub
                                ALUctr  = 4'b1000;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                            default: begin
                                ALUctr  = 4'b0000;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b0;  
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end                                                                                
                        endcase
                    end
                    3'b001: begin // sll : shfit left logically
                        ALUctr  = 4'b0001;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                    3'b010: begin // slt : set less than
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                    3'b011: begin // sltu : set less than unsigned
                        ALUctr  = 4'b1010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;   
                        branch  = 3'b000;   
                        MemWr   = 1'b0;                
                    end
                    3'b100: begin // xor
                        ALUctr  = 4'b0100;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;    
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                    3'b101: begin
                        case(func7)
                            7'b0000000: begin  // srl : shfit right logically
                                ALUctr  = 4'b0101;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                            7'b0100000: begin // sra : shfit right arthimetically
                                ALUctr  = 4'b1101;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b1;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                            end
                            default: begin
                                ALUctr  = 4'b0000;
                                ALUBsrc = 1'b0;
                                RegWr   = 1'b0;
                                branch  = 3'b000;
                                MemWr   = 1'b0;
                                end                                
                        endcase
                    end
                    3'b110: begin // or
                        ALUctr  = 4'b0110;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end        
                    3'b111: begin // and
                        ALUctr  = 4'b0111;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b1;
                        branch  = 3'b000;
                        MemWr   = 1'b0;
                    end
                endcase
            end
            7'b1100011: begin // instr type B
                case(func3)
                    3'b000: begin // beq
                        imm     = immB;
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b100;
                        MemWr   = 1'b0;
                    end
                    3'b001: begin  // bne
                        imm     = immB;
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b101;
                        MemWr   = 1'b0;
                    end
                    3'b100: begin // blt
                        imm     = immB;
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b110;
                        MemWr   = 1'b0;
                    end
                    3'b101: begin // bge
                        imm     = immB;
                        ALUctr  = 4'b0010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b111;
                        MemWr   = 1'b0;
                    end
                    3'b110: begin // bltu
                        imm     = immB;
                        ALUctr  = 4'b1010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b110;
                        MemWr   = 1'b0;
                    end
                    3'b111: begin // bgeu
                        imm     = immB;
                        ALUctr  = 4'b1010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b111;
                        MemWr   = 1'b0;
                    end      
                    default:begin
                        imm     = immB;
                        ALUctr  = 4'b1010;
                        ALUBsrc = 1'b0;
                        RegWr   = 1'b0;
                        branch  = 3'b111;
                        MemWr   = 1'b0; 
                    end                               
                endcase
            end
            7'b1101111: begin // jal
                imm     = immJ;
                ALUctr  = 4'b0000;
                ALUBsrc = 1'b1;
                RegWr   = 1'b1;
                branch  = 3'b001; 
                MemWr   = 1'b0;
            end
            7'b1100111: begin // jalr
                imm     = immI;
                ALUctr  = 4'b0000;
                ALUBsrc = 1'b1;
                RegWr   = 1'b1;
                branch  = 3'b010;
                MemWr   = 1'b0;
            end

            7'b0000011: begin
                case(func3)
                    3'b000: begin // lb
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b000;
                        MemWr    = 1'b0;
                    end
                    3'b001: begin // lh
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b001;
                        MemWr    = 1'b0;
                    end
                    3'b010: begin //lw
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b010;
                        MemWr    = 1'b0;
                    end
                    3'b100: begin //lbu
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b100;
                        MemWr    = 1'b0;
                    end
                    3'b101: begin //lhu
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b101;
                        MemWr    = 1'b0;
                    end
                    default:begin
                        imm      = immI;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b1;
                        branch   = 3'b000;
                        MemtoReg = 1'b1;
                        MemOp    = 3'b101;
                        MemWr    = 1'b0;
                    end                       
                endcase
            end
             7'b0100011: begin
                case(func3)
                    3'b000: begin // sb
                        imm      = immS;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b0;
                        branch   = 3'b000;
                        MemOp    = 3'b000;
                        MemWr    = 1'b1;
                    end
                    3'b001: begin // sh
                        imm      = immS;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b0;
                        branch   = 3'b000;
                        MemOp    = 3'b001;
                        MemWr    = 1'b1;
                    end
                    3'b010: begin // sw
                        imm      = immS;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b0;
                        branch   = 3'b000;
                        MemOp    = 3'b010;
                        MemWr    = 1'b1;
                    end
                    default: begin
                        imm      = immS;
                        ALUctr   = 4'b0000;
                        ALUBsrc  = 1'b1;
                        RegWr    = 1'b0;
                        branch   = 3'b000;
                        MemOp    = 3'b010;
                        MemWr    = 1'b1;
                    end                        
                endcase
            end
            default:begin
                imm     = 32'b0;
                ALUctr  = 4'b0000;
                ALUBsrc = 1'b0;
                RegWr   = 0;
                branch  = 3'b000;
                MemWr   = 1'b0;
            end
        endcase
        // $display("instr:%b",instr);
    end

endmodule


