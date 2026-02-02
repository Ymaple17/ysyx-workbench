`include "include/define.vh"
module IFU(
    input        clock,
    input        reset,

    input        idu_ready,
    output reg   ifu_valid,

    input        exu_flush,
    input [31:0] exu_flush_pc,

    output reg [31:0] ifu_pc,
    output reg [31:0] ifu_inst,
    output reg [31:0] next_pc,

    // AXI Interface
    output reg         ifu_arvalid,
    output reg [31:0]  ifu_araddr,
    output reg [3:0]   ifu_arid,
    output reg [7:0]   ifu_arlen,
    output reg [2:0]   ifu_arsize,
    output reg [1:0]   ifu_arburst,
    output reg        ifu_rready,

    // Cache Interface
    input              icache_valid,
    input      [31:0]  icache_inst,
    input              icache_arvalid,
    input      [31:0]  icache_araddr,
    input      [3:0]   icache_arid,
    input      [7:0]   icache_arlen,
    input      [2:0]   icache_arsize,
    input      [1:0]   icache_arburst,
    input              icache_rready
);

    localparam IDLE       = 2'b00;
    localparam ICACHE     = 2'b01;
    reg [1:0] state;


    reg first_flush;
    reg first_inst;
    wire        is_jal    = (icache_inst[6:0] == `INST_JAL);
    wire [31:0] jal_target = (first_flush ? ifu_pc : next_pc) + {{12{icache_inst[31]}}, icache_inst[19:12], icache_inst[20], icache_inst[30:21], 1'b0};

    always @(*) begin
      ifu_arvalid  = icache_arvalid;
      ifu_araddr   = icache_araddr;
      ifu_arid     = icache_arid;
      ifu_arlen    = icache_arlen;
      ifu_arsize   = icache_arsize;
      ifu_arburst  = icache_arburst;
      ifu_rready   = icache_rready;
    end

    always @(posedge clock) begin
        if(reset) begin
            ifu_pc <= 32'h3000_0000;
            next_pc <= 32'h3000_0000;
            ifu_inst <= 32'b0;
            state <= IDLE;
            first_flush <= 1'b0;
            first_inst <= 1'b1;
            ifu_valid <= 1'b0;
        end else begin
            if(exu_flush) begin
                state <= ICACHE;
                next_pc <= exu_flush_pc;
                first_flush <= 1'b1;
                ifu_valid <= 1'b0;
            end else begin
                case(state) 
                    IDLE: begin
                        if((ifu_valid && idu_ready)||first_flush||first_inst) begin
                            state <= ICACHE;
                            ifu_valid <= 1'b0;
                            first_flush <= 1'b0;
                            first_inst <= 1'b0;
                        end
                    end
                    ICACHE: begin
                        if(icache_valid) begin
                            ifu_inst <= icache_inst;
                            ifu_pc <= first_flush ? ifu_pc : next_pc;
                            ifu_valid <= first_flush ? 1'b0 : 1'b1;
                            next_pc <= first_flush ? next_pc : (is_jal ? jal_target : next_pc + 4);
                            state <= IDLE;
                        end
                    end
                    default: begin
                        state <= IDLE;
                    end
                endcase
            end
        end
    end

    // Performance Monitor
    import "DPI-C" function void npc_pm_event(input int event_id, input longint data);
    
    always @(posedge clock) begin
        if (!reset) begin
            if (state == ICACHE && icache_valid) npc_pm_event(0, 1); // EVENT_IFU_FETCH
            if (state == ICACHE && !icache_valid) npc_pm_event(1, 1); // EVENT_IFU_STALL_ICACHE
            if (ifu_valid && !idu_ready) npc_pm_event(2, 1); // EVENT_IFU_STALL_IDU
        end
    end

endmodule

