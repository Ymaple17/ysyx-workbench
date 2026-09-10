`timescale 1ns / 1ps

module tb_board_soc;
  reg clk_50m = 1'b0;
  reg sys_rst_n = 1'b0;
  wire [1:0] led;
  wire uart_txd;

  board_soc_top dut (
    .clk_50m(clk_50m),
    .sys_rst_n(sys_rst_n),
    .led(led),
    .uart_txd(uart_txd)
  );

  always #10 clk_50m = ~clk_50m;

  task automatic receive_byte(output [7:0] value);
    integer bit_index;
    reg [7:0] candidate;
    begin
      value = 8'b0;
      forever begin
        @(negedge uart_txd);
        repeat (217) @(posedge clk_50m);
        if (uart_txd === 1'b0) begin
          repeat (434) @(posedge clk_50m);
          for (bit_index = 0; bit_index < 8; bit_index = bit_index + 1) begin
            candidate[bit_index] = uart_txd;
            repeat (434) @(posedge clk_50m);
          end
          if (uart_txd === 1'b1) begin
            value = candidate;
            disable receive_byte;
          end
        end
      end
    end
  endtask

  reg [7:0] received;
  integer i;
  reg [47:0] message;
  integer tx_starts = 0;

  always @(negedge uart_txd) begin
    tx_starts = tx_starts + 1;
    $display("BOARD_SOC_TX_START count=%0d time=%0t", tx_starts, $time);
  end

  initial begin
    repeat (10) @(posedge clk_50m);
    sys_rst_n = 1'b1;
    $display("BOARD_SOC_SIM_START");
    message = 48'b0;
    for (i = 0; i < 6; i = i + 1) begin
      receive_byte(received);
      message[i*8 +: 8] = received;
    end
    if (message !== {8'h0a, 8'h4f, 8'h4c, 8'h4c, 8'h45, 8'h48}) begin
      $display("BOARD_SOC_UART_FAIL message=%h", message);
      $fatal(1);
    end
    $display("BOARD_SOC_UART_PASS message=%h led=%b", message, led);
    $finish;
  end

  initial begin
    repeat (50000) @(posedge clk_50m);
    $display("BOARD_SOC_UART_TIMEOUT txd=%b led=%b tx_starts=%0d", uart_txd, led, tx_starts);
    $fatal(1);
  end
endmodule
