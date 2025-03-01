module ysyx_25020039_regfile (
    input wire clk,
    input wire we,            
    input wire [4:0] read_addr1, read_addr2, write_addr,
    input wire [31:0] write_data,
    output wire [31:0] read_data1, read_data2
);
    reg [31:0] regs [31:0];


    assign read_data1 = regs[read_addr1];
    assign read_data2 = regs[read_addr2];


    always @(posedge clk) begin
        if (we) begin
            regs[write_addr] <= write_data;
        end
    end
endmodule

