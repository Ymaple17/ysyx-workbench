module branch(
    input zf,
    input less,
    input zero,
    input [2:0] branch,
    output reg PCAsrc,  // 0:4  1:imm
    output reg PCBsrc   // 0:pc 1:rs1
);
    
    always@(*)begin
        case(branch)
            3'b000: begin  // pc + 4
                PCAsrc = 0;
                PCBsrc = 0;
            end
            3'b001: begin  // pc + imm
                PCAsrc = 1;
                PCBsrc = 0; 
            end
            3'b010: begin // rs1 + imm
                PCAsrc = 1;
                PCBsrc = 0;
            end
            3'b100: begin // beq
                if(zf == 0) begin
                    PCAsrc = 0;
                    PCBsrc = 0;
                end
                else begin
                    PCAsrc = 1;
                    PCBsrc = 0;
                end
            end
            3'b101: begin // bne
                if(zf == 0) begin
                    PCAsrc = 1;
                    PCBsrc = 0;
                end
                else begin
                    PCAsrc = 0;
                    PCBsrc = 0;
                end
            end
            3'b110: begin // blt
                if(less == 0) begin
                    PCAsrc = 0;
                    PCBsrc = 0;
                end
                else begin
                    PCAsrc = 1;
                    PCBsrc = 0;
                end
            end
            3'b111: begin // bge
                if(less == 0) begin
                    PCAsrc = 1;
                    PCBsrc = 0;
                end
                else begin
                    PCAsrc = 0;
                    PCBsrc = 0;
                end
            end
            default:begin
                PCAsrc = 0;
                PCBsrc = 0;
            end                
        endcase
    end
    initial begin
        PCAsrc = 0;
        PCBsrc = 0;
    end

endmodule

