module tb_cpu;

    reg clk;
    reg reset;
    wire [31:0] pc;
    wire [31:0] instruction;
    wire [31:0] rdata1, rdata2;
    wire [31:0] alu_result;
    wire zero;

    ysyx_25020039_cpu uut (
        .clk(clk),
        .reset(reset)
    );

    always begin
        #5 clk = ~clk;
    end

    initial begin
        clk = 0;
        reset = 0;

        $display("Starting simulation...");

        reset = 1;
        #10;
        reset = 0;
        #1000;

        $display("Simulation complete!");
        $finish;
    end

    always @(posedge clk) begin
        $display("PC = %h, Instruction = %h", pc, instruction);
    end

endmodule

