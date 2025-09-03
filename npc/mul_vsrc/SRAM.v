`include "include/defs.vh"

module SRAM (
  input                         clk,
  input                         rst,
  
  // Request interface
  input                         valid_i,   // Request valid
  output reg                    ready_o,   // Ready to accept request
  input      [`DATA_WIDTH-1:0]   addr_i,    // Address
  
  // Response interface  
  output reg                    valid_o,   // Response valid
  output reg [`DATA_WIDTH-1:0]   data_o     // Read data
);

  // Import DPI-C function
  import "DPI-C" context function int paddr_read(input int addr);

  // Internal state
  reg busy;
  reg [`DATA_WIDTH-1:0] addr_reg;

  // Ready when not busy
  always @(*) begin
    ready_o = !busy;
  end

  // Control logic
  always @(posedge clk) begin
    if (rst) begin
      busy <= 1'b0;
      valid_o <= 1'b0;
      addr_reg <= {`DATA_WIDTH{1'b0}};
      data_o <= {`DATA_WIDTH{1'b0}};
    end else begin
      // Accept new request
      if (valid_i && ready_o) begin
        busy <= 1'b1;
        addr_reg <= addr_i;
        valid_o <= 1'b0;
      end
      // Process request and return data
      else if (busy) begin
        busy <= 1'b0;
        valid_o <= 1'b1;
        data_o <= paddr_read(addr_reg);
      end
      // Clear valid after one cycle
      else if (valid_o) begin
        valid_o <= 1'b0;
      end
    end
  end

endmodule