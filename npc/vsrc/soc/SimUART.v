

module SimUART(
    input clk,
    input wen,
    input [31:0] waddr,
    input [7:0]  wdata
);
    // UART Output Simulation
    always @(posedge clk) begin
        if (wen) begin
            // Matches UART check: 0xa00003f8
            if (waddr >= 32'ha00003f8 && waddr <= 32'ha00003ff) begin
                $write("%c", wdata);
            end
        end
    end
endmodule
        
