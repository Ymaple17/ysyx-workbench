module RegFile (
    input  wire        clock,
    input  wire        we,
    input  wire [4:0]  rs1_addr,
    input  wire [4:0]  rs2_addr,
    input  wire [4:0]  rd_addr,
    input  wire [31:0] rd_data,
    output wire [31:0] rs1_data,
    output wire [31:0] rs2_data
);
    reg [31:0] regfile [0:31];

    assign rs1_data = (rs1_addr == 0) ? 32'b0 : regfile[rs1_addr];
    assign rs2_data = (rs2_addr == 0) ? 32'b0 : regfile[rs2_addr];

    always @(posedge clock) begin
        if (we && rd_addr != 0) begin
            regfile[rd_addr] <= rd_data;
        end
    end

    initial begin
        for (integer i = 0; i < 32; i = i + 1) begin
            regfile[i] = 32'b0;
        end
    end
endmodule
