`include "include/define.vh"
module Icache #(
    parameter ICACHE_SIZE = 32,
    parameter BLOCK_SIZE  = 16
)(
    input  wire       clk,
    input  wire       reset,
    input  wire       is_fencei,
    input  wire [31:0] addr,
    output reg  [31:0] inst,
    output reg         valid,

    // AXI read address
    output reg  [31:0] axi_araddr,
    output reg         axi_arvalid,
    input  wire        axi_arready,
    output wire [ 3:0] axi_arid,
    output wire [ 7:0] axi_arlen,
    output wire [ 2:0] axi_arsize,
    output wire [ 1:0] axi_arburst,
    input  wire        axi_rvalid,
    output reg         axi_rready,
    input  wire [31:0] axi_rdata,
    input  wire [ 1:0] axi_rresp,
    input  wire [ 3:0] axi_rid,
    input  wire        axi_rlast
);

    localparam integer NUM_BLOCKS         = ICACHE_SIZE / BLOCK_SIZE;
    localparam integer BLOCK_OFFSET_WIDTH = $clog2(BLOCK_SIZE);
    localparam integer INDEX_WIDTH        = $clog2(NUM_BLOCKS);
    localparam integer TAG_WIDTH          = 32 - INDEX_WIDTH - BLOCK_OFFSET_WIDTH;
    localparam integer BEATS_PER_BLOCK     = BLOCK_SIZE / 4;
    localparam integer AXI_BURST_LEN_INT   = (BEATS_PER_BLOCK > 0) ? (BEATS_PER_BLOCK - 1) : 0;
    localparam [7:0]   AXI_BURST_LEN       = AXI_BURST_LEN_INT[7:0];

    reg [TAG_WIDTH-1:0] tag_ram   [0:NUM_BLOCKS-1];
    reg [31:0]          data_ram  [0:NUM_BLOCKS-1][0:BEATS_PER_BLOCK-1];
    reg [NUM_BLOCKS-1:0] valid_ram;
    reg [31:0]          block_data[0:BEATS_PER_BLOCK-1];

    wire [TAG_WIDTH-1:0] req_tag   = addr[31:32 - TAG_WIDTH];
    wire [INDEX_WIDTH-1:0] req_index = addr[INDEX_WIDTH + BLOCK_OFFSET_WIDTH - 1:BLOCK_OFFSET_WIDTH];
    wire [1:0]            beat_idx  = addr[3:2];

    reg [TAG_WIDTH-1:0]   saved_tag;
    reg [INDEX_WIDTH-1:0] saved_index;
    reg [1:0]             saved_beat_idx;

    localparam [1:0] STATE_IDLE = 2'b00;
    localparam [1:0] STATE_READ = 2'b01;
    localparam [1:0] STATE_FILL = 2'b10;

    reg [1:0] state, next_state;
    reg [1:0] beat_cnt;
    reg       ar_done;

    localparam [31:0] SDRAM_BASE = 32'hA0000000;
    localparam [31:0] SDRAM_END  = 32'hBFFFFFFF;
    localparam [31:0] FLASH_BASE = 32'h30000000;
    localparam [31:0] FLASH_END  = 32'h3FFFFFFF;

    wire in_sdram = (addr >= SDRAM_BASE) && (addr <= SDRAM_END);
    wire in_flash = (addr >= FLASH_BASE) && (addr <= FLASH_END);
    wire cacheable = in_sdram || in_flash;
    
    wire hit      = valid_ram[req_index] && (tag_ram[req_index] == req_tag) && !is_fencei && cacheable;

    assign axi_arid    = 4'h0;
    assign axi_arlen   = cacheable ? AXI_BURST_LEN : 8'h0;
    assign axi_arburst = cacheable ? 2'b01 : 2'b00;
    assign axi_arsize  = 3'b010;

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            state <= STATE_IDLE;
        end else begin
            state <= next_state;
        end
    end

    always @(*) begin
        next_state = state;
        case (state)
            STATE_IDLE: if (!hit) next_state = STATE_READ;
            STATE_READ: if (axi_rvalid && axi_rready && axi_rlast) begin
                            next_state = STATE_FILL;
                        end
            STATE_FILL: next_state = STATE_IDLE;
            default:    next_state = STATE_IDLE;
        endcase
    end

    integer idx;
    integer b;

    always @(posedge clk or posedge reset) begin
        if (reset) begin
            valid_ram <= {NUM_BLOCKS{1'b0}};
            inst        <= 32'h0;
            valid       <= 1'b0;
            axi_araddr  <= 32'h0;
            axi_arvalid <= 1'b0;
            axi_rready  <= 1'b0;
            beat_cnt    <= 2'b0;
            ar_done     <= 1'b0;
        end else begin
            if (is_fencei) begin
                valid_ram <= {NUM_BLOCKS{1'b0}};
            end

            case (state)
                STATE_IDLE: begin
                    axi_rready  <= 1'b0;
                    axi_arvalid <= 1'b0;
                    beat_cnt    <= 2'b0;
                    ar_done     <= 1'b0;

                    saved_tag      <= req_tag;
                    saved_index    <= req_index;
                    saved_beat_idx <= beat_idx;

                    if (hit) begin
                        inst  <= data_ram[req_index][beat_idx];
                        valid <= 1'b1;
                    end else begin
                        valid <= 1'b0;
                    end
                end

                STATE_READ: begin
                    valid      <= 1'b0;
                    axi_rready <= 1'b1;

                    if (!axi_arvalid && !ar_done) begin
                        axi_arvalid <= 1'b1;
                        axi_araddr  <= cacheable ? {addr[31:BLOCK_OFFSET_WIDTH], {BLOCK_OFFSET_WIDTH{1'b0}}} : addr;
                    end else if (axi_arvalid && axi_arready) begin
                        axi_arvalid <= 1'b0;
                        ar_done      <= 1'b1;
                    end

                    if (axi_rvalid) begin
                        block_data[beat_cnt] <= axi_rdata;
                        beat_cnt <= cacheable ? beat_cnt + 1'b1 : 2'b0;
                    end
                end

                STATE_FILL: begin
                    axi_rready <= 1'b0;
                    valid_ram[saved_index] <= 1'b1;
                    tag_ram[saved_index]   <= saved_tag;

                    for (b = 0; b < BEATS_PER_BLOCK; b = b + 1) begin
                        data_ram[saved_index][b] <= block_data[b];
                    end

                    inst  <= cacheable ? block_data[saved_beat_idx] : block_data[0];
                    valid <= 1'b1;
                end

                default: begin
                end
            endcase
        end
    end

    // Performance Monitor
    import "DPI-C" function void npc_pm_event(input int event_id, input longint data);

    always @(posedge clk) begin
        if (!reset && state == STATE_IDLE && !hit) begin
            npc_pm_event(14, 1); // EVENT_ICACHE_MISS
        end
    end

endmodule

