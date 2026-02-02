`include "include/define.vh"
module CLINT(
    input  wire         clock,
    input  wire         reset,

    output reg          arready,
    input  wire         arvalid,
    input  wire [31: 0] araddr,
    input  wire         rready,
    output reg          rvalid,
    output reg  [ 1: 0] rresp,
    output reg  [31: 0] rdata,
    output reg          rlast
);

    reg [31:0] mtime_low, mtime_high;
    wire [3:0] clint_offset;
    assign clint_offset = araddr[3:0];

    localparam IDLE = 1'b0;
    localparam DOING = 1'b1;

    reg state, next_state;

    always @(*) begin
        case(state)
            IDLE: next_state = arvalid ? DOING : IDLE;
            DOING: next_state = (rready & rlast) ? IDLE : DOING;
            default: next_state = IDLE;
        endcase
    end

    always @(posedge clock) begin
        if(reset) begin
            state <= IDLE;
            mtime_low <= 32'b0;
            mtime_high <= 32'b0;
            arready <= 1'b1;
            rvalid <= 1'b0;
        end else begin
            state <= next_state;
            if(mtime_low == 32'hFFFFFFFF) begin
                mtime_high <= mtime_high + 1;
                mtime_low <= 32'b0;
            end else begin
                mtime_low <= mtime_low + 1;
            end
            case(state)
                IDLE: begin
                    rvalid <= 1'b0;
                    rlast <= 1'b0;
                    if(arvalid & arready) begin
                        arready <= 1'b0;
                    end
                end
        
                DOING: begin
                    arready <= 1'b1;
                    rdata <= (clint_offset == 4'h0) ? mtime_low :(clint_offset == 4'h4) ? mtime_high :32'b0;
                    rvalid <= 1'b1;
                    rlast <= 1'b1;
                    rresp <= 2'b00;
                end
            endcase
        end
    end

endmodule