module register(    
    input Wrclk,  // write clk    
    input RegWr,  // write enable
    input [4:0] Ra,  // read addr1
    input [4:0] Rb,  // read addr2
    input [4:0] Rw,  // write addr
    output reg [31:0] busA,  // output data
    output reg [31:0] busB,  // output data
    input [31:0] busW  // write data
);

reg [31:0] general_register [31:0];
//assign general_register[0] = 32'b0;  // x0 always 0

always@(posedge Wrclk)begin
    if(RegWr && Rw != 5'd0)begin
        general_register[Rw] <= busW;
    end
end

always@(busW)begin
    $display("write %b in reg%d",busW,Rw);
end
always@(Ra or Rb)begin
    if(Ra == 5'b0) busA <= 32'b0;
    if(Rb == 5'b0) busB <= 32'b0;
    else if(Ra != 5'b0) busA <= general_register[Ra];
    else if(Rb != 5'b0) busB <= general_register[Rb];
end
initial begin
    general_register[1] = 32'd1;
end

endmodule

