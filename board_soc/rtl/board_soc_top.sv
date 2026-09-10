module board_soc_top #(
  parameter integer CPU_FREQ_KHZ = 84000
)(
  input        clk_50m,
  input        sys_rst_n,
  output [1:0] led,
  output       uart_txd
);
  localparam real CPU_FREQ_MHZ = CPU_FREQ_KHZ / 1000.0;
  localparam integer UART_BAUD_DIV_HW = $rtoi(
    (CPU_FREQ_MHZ * 1000000.0 / 115200.0) + 0.5);
  localparam integer CLOCK_HZ_HW = CPU_FREQ_KHZ * 1000;
`ifndef SYNTHESIS
  localparam integer UART_BAUD_DIV = 434;
  localparam integer CLOCK_HZ = 50000000;
`else
  localparam integer UART_BAUD_DIV = UART_BAUD_DIV_HW;
  localparam integer CLOCK_HZ = CLOCK_HZ_HW;
`endif

  wire cpu_clk;
  wire clock_locked;
  board_clock #(
    .CPU_FREQ_MHZ(CPU_FREQ_MHZ)
  ) clock_gen (
    .clk_in  (clk_50m),
    .reset   (~sys_rst_n),
    .clk_out (cpu_clk),
    .locked  (clock_locked)
  );

  // The CPU reset is synchronous to the generated clock. Hold it until the
  // MMCM is locked and for a short power-up interval afterward so programming
  // with the reset button already released is deterministic.
  reg [15:0] boot_reset_count = 16'h0000;
  reg boot_reset_done = 1'b0;
  wire reset = ~sys_rst_n | ~clock_locked | ~boot_reset_done;

  always @(posedge cpu_clk) begin
    if (!sys_rst_n || !clock_locked) begin
      boot_reset_count <= 16'h0000;
      boot_reset_done <= 1'b0;
    end else if (!boot_reset_done) begin
      if (boot_reset_count == 16'hffff) begin
        boot_reset_done <= 1'b1;
      end else begin
        boot_reset_count <= boot_reset_count + 1'b1;
      end
    end
  end
  wire [31:0] cpu_araddr;
  wire cpu_arvalid;
  wire [3:0] cpu_arid;
  wire [7:0] cpu_arlen;
  wire [2:0] cpu_arsize;
  wire [1:0] cpu_arburst;
  wire cpu_arready;
  wire [31:0] cpu_rdata;
  wire [1:0] cpu_rresp;
  wire cpu_rvalid;
  wire cpu_rlast;
  wire [3:0] cpu_rid;
  wire cpu_rready;
  wire [31:0] cpu_awaddr;
  wire cpu_awvalid;
  wire [3:0] cpu_awid;
  wire [7:0] cpu_awlen;
  wire [2:0] cpu_awsize;
  wire [1:0] cpu_awburst;
  wire cpu_awready;
  wire [31:0] cpu_wdata;
  wire [3:0] cpu_wstrb;
  wire cpu_wvalid;
  wire cpu_wlast;
  wire cpu_wready;
  wire [1:0] cpu_bresp;
  wire cpu_bvalid;
  wire [3:0] cpu_bid;
  wire cpu_bready;
  wire cpu_ebreak;

  ysyx_25020039 cpu (
    .clock             (cpu_clk),
    .reset             (reset),
    .io_interrupt      (1'b0),
    .io_master_araddr  (cpu_araddr),
    .io_master_arvalid (cpu_arvalid),
    .io_master_arid    (cpu_arid),
    .io_master_arlen   (cpu_arlen),
    .io_master_arsize  (cpu_arsize),
    .io_master_arburst (cpu_arburst),
    .io_master_arready (cpu_arready),
    .io_master_rdata   (cpu_rdata),
    .io_master_rresp   (cpu_rresp),
    .io_master_rvalid  (cpu_rvalid),
    .io_master_rlast   (cpu_rlast),
    .io_master_rid     (cpu_rid),
    .io_master_rready  (cpu_rready),
    .io_master_awaddr  (cpu_awaddr),
    .io_master_awvalid (cpu_awvalid),
    .io_master_awid    (cpu_awid),
    .io_master_awlen   (cpu_awlen),
    .io_master_awsize  (cpu_awsize),
    .io_master_awburst (cpu_awburst),
    .io_master_awready (cpu_awready),
    .io_master_wdata   (cpu_wdata),
    .io_master_wstrb   (cpu_wstrb),
    .io_master_wvalid  (cpu_wvalid),
    .io_master_wlast   (cpu_wlast),
    .io_master_wready  (cpu_wready),
    .io_master_bresp   (cpu_bresp),
    .io_master_bvalid  (cpu_bvalid),
    .io_master_bid     (cpu_bid),
    .io_master_bready  (cpu_bready),
    .io_slave_araddr   (32'b0),
    .io_slave_arvalid  (1'b0),
    .io_slave_arid     (4'b0),
    .io_slave_arlen    (8'b0),
    .io_slave_arsize   (3'b0),
    .io_slave_arburst  (2'b0),
    .io_slave_arready  (),
    .io_slave_rdata    (),
    .io_slave_rresp    (),
    .io_slave_rvalid   (),
    .io_slave_rlast    (),
    .io_slave_rid      (),
    .io_slave_rready   (1'b0),
    .io_slave_awaddr   (32'b0),
    .io_slave_awvalid  (1'b0),
    .io_slave_awid     (4'b0),
    .io_slave_awlen    (8'b0),
    .io_slave_awsize   (3'b0),
    .io_slave_awburst  (2'b0),
    .io_slave_awready  (),
    .io_slave_wdata    (32'b0),
    .io_slave_wstrb    (4'b0),
    .io_slave_wvalid   (1'b0),
    .io_slave_wlast    (1'b0),
    .io_slave_wready   (),
    .io_slave_bresp    (),
    .io_slave_bvalid   (),
    .io_slave_bid      (),
    .io_slave_bready   (1'b0),
    .io_ebreak         (cpu_ebreak)
  );

  wire ram_arvalid;
  wire ram_arready;
  wire [3:0] ram_rid;
  wire [31:0] ram_rdata;
  wire [1:0] ram_rresp;
  wire ram_rlast;
  wire ram_rvalid;
  wire ram_rready;
  wire ram_awvalid;
  wire ram_awready;
  wire ram_wvalid;
  wire ram_wready;
  wire [3:0] ram_bid;
  wire [1:0] ram_bresp;
  wire ram_bvalid;
  wire ram_bready;
  wire uart_arready;
  wire [3:0] uart_rid;
  wire [31:0] uart_rdata;
  wire [1:0] uart_rresp;
  wire uart_rlast;
  wire uart_rvalid;
  wire uart_rready;
  wire uart_awready;
  wire uart_wready;
  wire [3:0] uart_bid;
  wire [1:0] uart_bresp;
  wire uart_bvalid;
  wire uart_bready;
  wire [1:0] core_led;

  reg read_active;
  reg read_target_uart;
  reg write_active;
  reg write_target_uart;
  wire ar_is_uart = cpu_araddr[31:28] == 4'h1;
  wire aw_is_uart = cpu_awaddr[31:28] == 4'h1;
  wire ar_is_ram = cpu_araddr[31:28] == 4'h8;
  wire aw_is_ram = cpu_awaddr[31:28] == 4'h8;

  assign ram_arvalid = cpu_arvalid && ar_is_ram && !read_active && !write_active;
  assign cpu_arready = !read_active && !write_active &&
    (ar_is_ram ? ram_arready : ar_is_uart ? uart_arready : 1'b0);
  assign ram_awvalid = cpu_awvalid && aw_is_ram && !read_active && !write_active;
  assign cpu_awready = !read_active && !write_active &&
    (aw_is_ram ? ram_awready : aw_is_uart ? uart_awready : 1'b0);
  assign ram_wvalid = cpu_wvalid && write_active && !write_target_uart;
  assign cpu_wready = write_active ?
    (write_target_uart ? uart_wready : ram_wready) : 1'b0;
  assign ram_bready = cpu_bready && write_active && !write_target_uart;
  assign uart_bready = cpu_bready && write_active && write_target_uart;
  assign cpu_bvalid = write_active &&
    (write_target_uart ? uart_bvalid : ram_bvalid);
  assign cpu_bid = write_target_uart ? uart_bid : ram_bid;
  assign cpu_bresp = write_target_uart ? uart_bresp : ram_bresp;

  assign ram_rready = cpu_rready && read_active && !read_target_uart;
  assign uart_rready = cpu_rready && read_active && read_target_uart;
  assign cpu_rvalid = read_active &&
    (read_target_uart ? uart_rvalid : ram_rvalid);
  assign cpu_rid = read_target_uart ? uart_rid : ram_rid;
  assign cpu_rdata = read_target_uart ? uart_rdata : ram_rdata;
  assign cpu_rresp = read_target_uart ? uart_rresp : ram_rresp;
  assign cpu_rlast = read_target_uart ? uart_rlast : ram_rlast;

  wire ar_fire = cpu_arvalid && cpu_arready;
  wire aw_fire = cpu_awvalid && cpu_awready;
  wire r_fire = cpu_rvalid && cpu_rready;
  wire b_fire = cpu_bvalid && cpu_bready;

  always @(posedge cpu_clk) begin
    if (reset) begin
      read_active <= 1'b0;
      read_target_uart <= 1'b0;
      write_active <= 1'b0;
      write_target_uart <= 1'b0;
    end else begin
      if (ar_fire) begin
        read_active <= 1'b1;
        read_target_uart <= ar_is_uart;
      end
      if (r_fire && cpu_rlast) read_active <= 1'b0;
      if (aw_fire) begin
        write_active <= 1'b1;
        write_target_uart <= aw_is_uart;
      end
      if (b_fire) write_active <= 1'b0;
    end
  end

  axi_ram #(
    .DATA_WIDTH(32),
    .ADDR_WIDTH(17),
    .ID_WIDTH(4),
    .PIPELINE_OUTPUT(0),
    .MEM_INIT_FILE("board_image.hex")
  ) ram (
    .clk             (cpu_clk),
    .rst             (reset),
    .s_axi_awid      (cpu_awid),
    .s_axi_awaddr    (cpu_awaddr[16:0]),
    .s_axi_awlen     (cpu_awlen),
    .s_axi_awsize    (cpu_awsize),
    .s_axi_awburst   (cpu_awburst),
    .s_axi_awlock    (1'b0),
    .s_axi_awcache   (4'b0),
    .s_axi_awprot    (3'b0),
    .s_axi_awvalid   (ram_awvalid),
    .s_axi_awready   (ram_awready),
    .s_axi_wdata     (cpu_wdata),
    .s_axi_wstrb     (cpu_wstrb),
    .s_axi_wlast     (cpu_wlast),
    .s_axi_wvalid    (ram_wvalid),
    .s_axi_wready    (ram_wready),
    .s_axi_bid       (ram_bid),
    .s_axi_bresp     (ram_bresp),
    .s_axi_bvalid    (ram_bvalid),
    .s_axi_bready    (ram_bready),
    .s_axi_arid      (cpu_arid),
    .s_axi_araddr    (cpu_araddr[16:0]),
    .s_axi_arlen     (cpu_arlen),
    .s_axi_arsize    (cpu_arsize),
    .s_axi_arburst   (cpu_arburst),
    .s_axi_arlock    (1'b0),
    .s_axi_arcache   (4'b0),
    .s_axi_arprot    (3'b0),
    .s_axi_arvalid   (ram_arvalid),
    .s_axi_arready   (ram_arready),
    .s_axi_rid       (ram_rid),
    .s_axi_rdata     (ram_rdata),
    .s_axi_rresp     (ram_rresp),
    .s_axi_rlast     (ram_rlast),
    .s_axi_rvalid    (ram_rvalid),
    .s_axi_rready    (ram_rready)
  );

  board_soc_uart #(
    .BAUD_DIV        (UART_BAUD_DIV),
    .CLOCK_HZ        (CLOCK_HZ)
  ) uart (
    .clk             (cpu_clk),
    .rst             (reset),
    .s_axi_awid      (cpu_awid),
    .s_axi_awaddr    (cpu_awaddr),
    .s_axi_awlen     (cpu_awlen),
    .s_axi_awsize    (cpu_awsize),
    .s_axi_awburst   (cpu_awburst),
    .s_axi_awlock    (1'b0),
    .s_axi_awcache   (4'b0),
    .s_axi_awprot    (3'b0),
    .s_axi_awvalid   (cpu_awvalid && aw_is_uart && !read_active && !write_active),
    .s_axi_awready   (uart_awready),
    .s_axi_wdata     (cpu_wdata),
    .s_axi_wstrb     (cpu_wstrb),
    .s_axi_wlast     (cpu_wlast),
    .s_axi_wvalid    (cpu_wvalid && write_active && write_target_uart),
    .s_axi_wready    (uart_wready),
    .s_axi_bid       (uart_bid),
    .s_axi_bresp     (uart_bresp),
    .s_axi_bvalid    (uart_bvalid),
    .s_axi_bready    (uart_bready),
    .s_axi_arid      (cpu_arid),
    .s_axi_araddr    (cpu_araddr),
    .s_axi_arlen     (cpu_arlen),
    .s_axi_arsize    (cpu_arsize),
    .s_axi_arburst   (cpu_arburst),
    .s_axi_arlock    (1'b0),
    .s_axi_arcache   (4'b0),
    .s_axi_arprot    (3'b0),
    .s_axi_arvalid   (cpu_arvalid && ar_is_uart && !read_active && !write_active),
    .s_axi_arready   (uart_arready),
    .s_axi_rid       (uart_rid),
    .s_axi_rdata     (uart_rdata),
    .s_axi_rresp     (uart_rresp),
    .s_axi_rlast     (uart_rlast),
    .s_axi_rvalid    (uart_rvalid),
    .s_axi_rready    (uart_rready),
    .core_led        (core_led),
    .uart_txd        (uart_txd)
  );

  reg [25:0] heartbeat;
  always @(posedge cpu_clk) begin
    if (reset) heartbeat <= 0;
    else heartbeat <= heartbeat + 1'b1;
  end
  assign led[0] = heartbeat[25];
  assign led[1] = core_led[0] ^ cpu_ebreak;
endmodule
