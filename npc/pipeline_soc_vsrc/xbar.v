`include "include/define.vh"
module Xbar (
    input  wire         clk,
    input  wire         reset,

    // IFU Interface
    output reg          ifu_arready,
    input  wire         ifu_arvalid,
    input  wire [31: 0] ifu_araddr,
    input  wire [ 3: 0] ifu_arid,
    input  wire [ 7: 0] ifu_arlen,
    input  wire [ 2: 0] ifu_arsize,
    input  wire [ 1: 0] ifu_arburst,
    input  wire         ifu_rready,
    output reg          ifu_rvalid,
    output reg  [ 1: 0] ifu_rresp,
    output reg  [31: 0] ifu_rdata,
    output reg          ifu_rlast,
    output reg  [ 3: 0] ifu_rid,

    // LSU Interface
    output reg          lsu_awready,
    input  wire         lsu_awvalid,
    input  wire [31: 0] lsu_awaddr,
    input  wire [ 3: 0] lsu_awid,
    input  wire [ 7: 0] lsu_awlen,
    input  wire [ 2: 0] lsu_awsize,
    input  wire [ 1: 0] lsu_awburst,
    output reg          lsu_wready,
    input  wire         lsu_wvalid,
    input  wire [31: 0] lsu_wdata,
    input  wire [ 3: 0] lsu_wstrb,
    input  wire         lsu_wlast,
    input  wire         lsu_bready,
    output reg          lsu_bvalid,
    output reg  [ 1: 0] lsu_bresp,
    output reg  [ 3: 0] lsu_bid,
    output reg          lsu_arready,
    input  wire         lsu_arvalid,
    input  wire [31: 0] lsu_araddr,
    input  wire [ 3: 0] lsu_arid,
    input  wire [ 7: 0] lsu_arlen,
    input  wire [ 2: 0] lsu_arsize,
    input  wire [ 1: 0] lsu_arburst,
    input  wire         lsu_rready,
    output reg          lsu_rvalid,
    output reg  [ 1: 0] lsu_rresp,
    output reg  [31: 0] lsu_rdata,
    output reg          lsu_rlast,
    output reg  [ 3: 0] lsu_rid,

    // AXI Master Interface
    input  wire         io_master_awready,
    output reg          io_master_awvalid,
    output reg  [31: 0] io_master_awaddr,
    output reg  [ 3: 0] io_master_awid,
    output reg  [ 7: 0] io_master_awlen,
    output reg  [ 2: 0] io_master_awsize,
    output reg  [ 1: 0] io_master_awburst,
    input  wire         io_master_wready,
    output reg          io_master_wvalid,
    output reg  [31: 0] io_master_wdata,
    output reg  [ 3: 0] io_master_wstrb,
    output reg          io_master_wlast,
    output reg          io_master_bready,
    input  wire         io_master_bvalid,
    input  wire [ 1: 0] io_master_bresp,
    input  wire [ 3: 0] io_master_bid,
    input  wire         io_master_arready,
    output reg          io_master_arvalid,
    output reg  [31: 0] io_master_araddr,
    output reg  [ 3: 0] io_master_arid,
    output reg  [ 7: 0] io_master_arlen,
    output reg  [ 2: 0] io_master_arsize,
    output reg  [ 1: 0] io_master_arburst,
    output reg          io_master_rready,
    input  wire         io_master_rvalid,
    input  wire [ 1: 0] io_master_rresp,
    input  wire [31: 0] io_master_rdata,
    input  wire         io_master_rlast,
    input  wire [ 3: 0] io_master_rid,

    // CLINT Interface
    input  wire         clint_arready,
    output reg          clint_arvalid,
    output reg  [31: 0] clint_araddr,
    output reg          clint_rready,
    input  wire         clint_rvalid,
    input  wire [ 1: 0] clint_rresp,
    input  wire [31: 0] clint_rdata,
    input  wire         clint_rlast
);

    localparam CLINT_START = 32'h02000000;
    localparam CLINT_END   = 32'h0200ffff;

    localparam IDLE       = 2'b00;
    localparam IFU_STATE = 2'b01;
    localparam LSU_READ_STATE = 2'b10;
    localparam LSU_WRITE_STATE = 2'b11;

    reg [1:0] state, next_state;
    reg       is_clint;

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            state <= IDLE;
        end else begin
            state <= next_state;
        end
    end
    
    always @(*) begin
        case (state)
            IDLE: begin
                if (lsu_arvalid)      next_state = LSU_READ_STATE;
                else if (lsu_awvalid) next_state = LSU_WRITE_STATE;
                else if (ifu_arvalid) next_state = IFU_STATE;
                else                  next_state = IDLE;
            end
            IFU_STATE: begin
                if (ifu_rvalid && ifu_rready && ifu_rlast)
                    next_state = IDLE;
                else
                    next_state = IFU_STATE;
            end
            LSU_READ_STATE: begin
                if ((lsu_rvalid && lsu_rready && lsu_rlast) || (clint_rvalid && clint_rready))
                    next_state = IDLE;
                else
                    next_state = LSU_READ_STATE;
            end
            LSU_WRITE_STATE: begin
                if (io_master_bvalid && io_master_bready)
                    next_state = IDLE;
                else
                    next_state = LSU_WRITE_STATE;
            end
            default: next_state = IDLE;
        endcase
    end

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            is_clint <= 1'b0;
        end else if (state == IDLE) begin
            if (lsu_arvalid) begin
                is_clint <= (lsu_araddr >= CLINT_START) && (lsu_araddr <= CLINT_END);
            end
        end
    end

    always @(*) begin
        ifu_arready       = 1'b0;
        ifu_rvalid        = 1'b0;
        ifu_rresp         = 2'b00;
        ifu_rdata         = 32'b0;
        ifu_rlast         = 1'b0;
        ifu_rid           = 4'b0;

        lsu_arready       = 1'b0;
        lsu_awready       = 1'b0;
        lsu_wready        = 1'b0;
        lsu_bvalid        = 1'b0;
        lsu_bresp         = 2'b00;
        lsu_bid           = 4'b0;
        lsu_rvalid        = 1'b0;
        lsu_rresp         = 2'b00;
        lsu_rdata         = 32'b0;
        lsu_rlast         = 1'b0;
        lsu_rid           = 4'b0;

        io_master_arvalid = 1'b0;
        io_master_araddr  = 32'b0;
        io_master_arid    = 4'b0;
        io_master_arlen   = 8'b0;
        io_master_arsize  = 3'b0;
        io_master_arburst = 2'b0;
        io_master_rready  = 1'b0;

        io_master_awvalid = 1'b0;
        io_master_awaddr  = 32'b0;
        io_master_awid    = 4'b0;
        io_master_awlen   = 8'b0;
        io_master_awsize  = 3'b0;
        io_master_awburst = 2'b0;
        io_master_wvalid  = 1'b0;
        io_master_wdata   = 32'b0;
        io_master_wstrb   = 4'b0;
        io_master_wlast   = 1'b0;
        io_master_bready  = 1'b0;

        clint_arvalid     = 1'b0;
        clint_araddr      = 32'b0;
        clint_rready      = 1'b0;

        case (state)
            IFU_STATE: begin
                io_master_arvalid = ifu_arvalid;
                io_master_araddr  = ifu_araddr;
                io_master_arid    = ifu_arid;
                io_master_arlen   = ifu_arlen;
                io_master_arsize  = ifu_arsize;
                io_master_arburst = ifu_arburst;
                io_master_rready  = ifu_rready;

                ifu_arready       = io_master_arready;
                ifu_rdata         = io_master_rdata;
                ifu_rresp         = io_master_rresp;
                ifu_rlast         = io_master_rlast;
                ifu_rvalid        = io_master_rvalid;
                ifu_rid           = io_master_rid;
            end

            LSU_READ_STATE: begin
                if (is_clint) begin
                    clint_arvalid = lsu_arvalid;
                    clint_araddr  = lsu_araddr;
                    clint_rready  = lsu_rready;

                    lsu_arready   = clint_arready;
                    lsu_rdata     = clint_rdata;
                    lsu_rresp     = clint_rresp;
                    lsu_rlast     = clint_rlast;
                    lsu_rvalid    = clint_rvalid;
                end else begin
                    io_master_arvalid = lsu_arvalid;
                    io_master_araddr  = lsu_araddr;
                    io_master_arid    = lsu_arid;
                    io_master_arlen   = lsu_arlen;
                    io_master_arsize  = lsu_arsize;
                    io_master_arburst = lsu_arburst;
                    io_master_rready  = lsu_rready;

                    lsu_arready       = io_master_arready;
                    lsu_rdata         = io_master_rdata;
                    lsu_rresp         = io_master_rresp;
                    lsu_rlast         = io_master_rlast;
                    lsu_rvalid        = io_master_rvalid;
                    lsu_rid           = io_master_rid;
                end
            end

            LSU_WRITE_STATE: begin
                io_master_awvalid = lsu_awvalid;
                io_master_awaddr  = lsu_awaddr;
                io_master_awid    = lsu_awid;
                io_master_awlen   = lsu_awlen;
                io_master_awsize  = lsu_awsize;
                io_master_awburst = lsu_awburst;
                
                io_master_wvalid  = lsu_wvalid;
                io_master_wdata   = lsu_wdata;
                io_master_wstrb   = lsu_wstrb;
                io_master_wlast   = lsu_wlast;
                
                io_master_bready  = lsu_bready;

                lsu_awready       = io_master_awready;
                lsu_wready        = io_master_wready;
                lsu_bvalid        = io_master_bvalid;
                lsu_bresp         = io_master_bresp;
                lsu_bid           = io_master_bid;
            end
            default: begin end
        endcase
    end

endmodule
