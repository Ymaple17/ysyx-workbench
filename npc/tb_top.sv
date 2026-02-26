
module tb_top;
  reg clk;
  reg rst_n;

  // Clock generation
  initial begin
    clk = 1;
    forever #5 clk = ~clk;
  end

  initial begin
    rst_n = 0;
    #100 rst_n = 1;
  end

  wire ebreak;

  ysyx_25020039 dut (
    .clock(clk),
    .reset(~rst_n),
    .io_interrupt(1'b0),
    .io_ebreak(ebreak)
  );

  initial begin
    $display("[SIM] Simulation started. Waiting for ebreak...");
    
    wait(ebreak == 1);
    
    $display("[SIM] Ebreak ! Simulation completed successfully.");
    $finish;
  end
endmodule
