module board_soc_uart #(
  parameter integer BAUD_DIV = 434,
  parameter integer CLOCK_HZ = 50000000
)(
  input        clk,
  input        rst,
  input [3:0]  s_axi_awid,
  input [31:0] s_axi_awaddr,
  input [7:0]  s_axi_awlen,
  input [2:0]  s_axi_awsize,
  input [1:0]  s_axi_awburst,
  input        s_axi_awlock,
  input [3:0]  s_axi_awcache,
  input [2:0]  s_axi_awprot,
  input        s_axi_awvalid,
  output       s_axi_awready,
  input [31:0] s_axi_wdata,
  input [3:0]  s_axi_wstrb,
  input        s_axi_wlast,
  input        s_axi_wvalid,
  output       s_axi_wready,
  output [3:0] s_axi_bid,
  output [1:0] s_axi_bresp,
  output       s_axi_bvalid,
  input        s_axi_bready,
  input [3:0]  s_axi_arid,
  input [31:0] s_axi_araddr,
  input [7:0]  s_axi_arlen,
  input [2:0]  s_axi_arsize,
  input [1:0]  s_axi_arburst,
  input        s_axi_arlock,
  input [3:0]  s_axi_arcache,
  input [2:0]  s_axi_arprot,
  input        s_axi_arvalid,
  output       s_axi_arready,
  output [3:0] s_axi_rid,
  output [31:0] s_axi_rdata,
  output [1:0] s_axi_rresp,
  output       s_axi_rlast,
  output       s_axi_rvalid,
  input        s_axi_rready,
  output [1:0] core_led,
  output       uart_txd
);
  localparam [31:0] UART_TX_ADDR = 32'h1000_0000;
  localparam [31:0] LED_ADDR = 32'h1000_0004;
  localparam [31:0] UART_LCR_ADDR = 32'h1000_0003;
  localparam [31:0] UART_LSR_ADDR = 32'h1000_0005;
  localparam [31:0] CYCLE_LO_ADDR = 32'h1000_0008;
  localparam [31:0] CYCLE_HI_ADDR = 32'h1000_000c;
  localparam [31:0] CLOCK_HZ_ADDR = 32'h1000_0010;

  reg write_active;
  reg write_valid;
  reg [3:0] write_id;
  reg [31:0] write_addr;
  reg [7:0] write_left;
  reg [1:0] write_resp;
  reg read_valid;
  reg [3:0] read_id;
  reg [31:0] read_data;
  reg [1:0] core_led_reg;
  reg [7:0] uart_lcr_reg;
  reg [63:0] cycle_counter;

  reg [7:0] uart_fifo [0:255];
  reg [7:0] uart_wr_ptr;
  reg [7:0] uart_rd_ptr;
  reg [8:0] uart_count;
  reg uart_busy;
  reg [3:0] uart_bit;
  reg [12:0] uart_div;
  reg [9:0] uart_shift;
  reg uart_tx_reg;

  wire uart_space = uart_count < 9'd256;
  wire write_data_ready = write_active && !write_valid &&
    ((write_addr != UART_TX_ADDR) || uart_space);
  wire write_accept = s_axi_awvalid && s_axi_awready;
  wire data_accept = s_axi_wvalid && s_axi_wready;
  wire read_accept = s_axi_arvalid && s_axi_arready;
  wire enqueue = data_accept && write_addr == UART_TX_ADDR;
  wire dequeue = !uart_busy && uart_count != 0;

  assign s_axi_awready = !write_active && !write_valid && !read_valid;
  assign s_axi_wready = write_data_ready;
  assign s_axi_bid = write_id;
  assign s_axi_bresp = write_resp;
  assign s_axi_bvalid = write_valid;
  assign s_axi_arready = !write_active && !write_valid && !read_valid;
  assign s_axi_rid = read_id;
  assign s_axi_rdata = read_data;
  assign s_axi_rresp = 2'b00;
  assign s_axi_rlast = 1'b1;
  assign s_axi_rvalid = read_valid;
  assign core_led = core_led_reg;
  assign uart_txd = uart_tx_reg;

  always @(posedge clk) begin
    if (rst) begin
      write_active <= 1'b0;
      write_valid <= 1'b0;
      write_id <= 4'b0;
      write_addr <= 32'b0;
      write_left <= 8'b0;
      write_resp <= 2'b00;
      read_valid <= 1'b0;
      read_id <= 4'b0;
      read_data <= 32'b0;
      core_led_reg <= 2'b0;
      uart_lcr_reg <= 8'b0;
      cycle_counter <= 64'b0;
    end else begin
      cycle_counter <= cycle_counter + 1'b1;

      if (write_accept) begin
        write_active <= 1'b1;
        write_id <= s_axi_awid;
        write_addr <= s_axi_awaddr;
        write_left <= s_axi_awlen;
        write_resp <= 2'b00;
      end

      if (data_accept) begin
        if (write_addr == LED_ADDR && s_axi_wstrb[0])
          core_led_reg <= s_axi_wdata[1:0];
        if (write_addr == UART_LCR_ADDR && s_axi_wstrb[0])
          uart_lcr_reg <= s_axi_wdata[7:0];
        if (write_addr != UART_TX_ADDR && write_addr != LED_ADDR &&
            write_addr != UART_LCR_ADDR)
          write_resp <= 2'b10;

        if (s_axi_wlast || write_left == 0) begin
          write_active <= 1'b0;
          write_valid <= 1'b1;
        end else begin
          write_addr <= write_addr + 4;
          write_left <= write_left - 1'b1;
        end
      end

      if (write_valid && s_axi_bready) write_valid <= 1'b0;

      if (read_accept) begin
        read_valid <= 1'b1;
        read_id <= s_axi_arid;
        case (s_axi_araddr)
          UART_LCR_ADDR: read_data <= {24'b0, uart_lcr_reg};
          UART_LSR_ADDR: read_data <= uart_space ? 32'h0000_0020 : 32'h0000_0000;
          LED_ADDR:      read_data <= {30'b0, core_led_reg};
          CYCLE_LO_ADDR: read_data <= cycle_counter[31:0];
          CYCLE_HI_ADDR: read_data <= cycle_counter[63:32];
          CLOCK_HZ_ADDR: read_data <= CLOCK_HZ;
          default:       read_data <= {30'b0, uart_busy, uart_space};
        endcase
      end
      if (read_valid && s_axi_rready) read_valid <= 1'b0;
    end
  end

  always @(posedge clk) begin
    if (rst) begin
      uart_wr_ptr <= 0;
      uart_rd_ptr <= 0;
      uart_count <= 0;
      uart_busy <= 1'b0;
      uart_bit <= 0;
      uart_div <= 0;
      uart_shift <= 10'b11_1111_1111;
      uart_tx_reg <= 1'b1;
    end else begin
      if (enqueue) begin
        uart_fifo[uart_wr_ptr] <= s_axi_wdata[7:0];
        uart_wr_ptr <= uart_wr_ptr + 1'b1;
      end
      if (dequeue) begin
        uart_rd_ptr <= uart_rd_ptr + 1'b1;
        uart_busy <= 1'b1;
        uart_bit <= 0;
        uart_div <= 0;
        uart_shift <= {1'b1, uart_fifo[uart_rd_ptr], 1'b0};
        uart_tx_reg <= 1'b0;
      end else if (uart_busy && uart_div == BAUD_DIV - 1) begin
        uart_div <= 0;
        if (uart_bit == 9) begin
          uart_busy <= 1'b0;
          uart_tx_reg <= 1'b1;
        end else begin
          uart_bit <= uart_bit + 1'b1;
          uart_shift <= {1'b0, uart_shift[9:1]};
          uart_tx_reg <= uart_shift[1];
        end
      end else if (uart_busy) begin
        uart_div <= uart_div + 1'b1;
      end else begin
        uart_tx_reg <= 1'b1;
      end

      case ({enqueue, dequeue})
        2'b10: uart_count <= uart_count + 1'b1;
        2'b01: uart_count <= uart_count - 1'b1;
        default: uart_count <= uart_count;
      endcase
    end
  end
endmodule
