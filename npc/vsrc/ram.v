module ram(
    input Rdclk,
    input Wrclk,
    input [31:0] Addr,
    input [2:0] MemOp,
    input [31:0] data_in,
    input Wr_en,
    output reg [31:0] data_out
);

    reg [31:0] data_memory [65535:0];

    always@(posedge Rdclk) begin
        case(MemOp)
            3'b000: begin // 1 byte with S-EXTEND
                data_out <= {{24{data_memory[Addr][7]}}, data_memory[Addr][7:0]};
            end
            3'b001: begin // 2 byte with S-EXTEND
                data_out <= {{16{data_memory[Addr][31]}}, data_memory[Addr][15:0]};
            end
            3'b010: begin // 4 byte
                data_out <= data_memory[Addr];
            end
            3'b100: begin // 1 byte with US-EXTEND
                data_out <= {24'b0, data_memory[Addr][7:0]};
            end
            3'b101: begin // 2 byte with US-EXTEND
                data_out <= {16'b0, data_memory[Addr][15:0]};
            end
            default: data_out <= data_memory[Addr];
        endcase
    end

    always@(posedge Wrclk) begin
        if(Wr_en) begin
            case(MemOp)
                3'b000: data_memory[Addr] <= {{24{data_in[31]}}, data_in[7:0]};
                3'b001: data_memory[Addr] <= {{16{data_in[31]}}, data_in[15:0]};
                3'b010: data_memory[Addr] <= data_in;
                3'b100: data_memory[Addr] <= {24'b0,data_in[7:0]};
                3'b101: data_memory[Addr] <= {16'b0,data_in[15:0]};
                default: data_memory[Addr] <= data_in;
            endcase
        end
    end

endmodule

