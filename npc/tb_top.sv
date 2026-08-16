
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

  integer cycles;
  always @(posedge clk) begin
    if (!rst_n) cycles <= 0;
    else cycles <= cycles + 1;
  end

  // Trace PC / inst for the first 150 cycles
  always @(posedge clk) begin
    if (rst_n && cycles < 150) begin
      $display("c%0d: next_pc=%h out_pc=%h out_inst=%h idu_in_valid=%b is_ebreak=%b ebreak=%b",
        cycles, dut._ifu_io_pc_bits_next_pc, dut._ifu_io_out_bits_pc,
        dut._ifu_io_out_bits_inst, dut.idu_io_in_valid,
        dut._idu_io_out_bits_is_ebreak, ebreak);
    end
  end

  initial begin
    $display("[SIM] Simulation started. Waiting for ebreak...");
    fork
      begin
        wait(ebreak === 1);
        $display("[SIM] Ebreak triggered at t=%0t, cycles=%0d", $time, cycles);
        $finish;
      end
      begin
        #100000000;
        $display("[SIM] TIMEOUT at t=%0t (cycles=%0d): program did not finish, ebreak=%b", $time, cycles, ebreak);
        $finish;
      end
    join
  end
endmodule
