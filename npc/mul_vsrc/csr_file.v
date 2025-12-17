`include "/home/qiu/ysyx-workbench/npc/mul_vsrc/include/defs.vh"

module CSR_FILE(
    input             clk,
    input             rst,
    input             csrWrite,
    input      [11:0] addr,
    input      [31:0] wdata,
    input      [1:0]  op,
    output reg [31:0] rdata,
    input             ecall,
    input             mret,
    input      [31:0] pc,
    output     [31:0] mret_pc,
    output     [31:0] ecall_pc
);
    reg [31:0] mstatus;  
    reg [31:0] mtvec;    
    reg [31:0] mepc;     
    reg [31:0] mcause;   

    localparam MSTATUS = 12'h300;
    localparam MTVEC   = 12'h305;
    localparam MEPC    = 12'h341;
    localparam MCAUSE  = 12'h342;

    always @(*) begin
        case(addr)
            MSTATUS: rdata = mstatus;
            MTVEC:   rdata = mtvec;
            MEPC:    rdata = mepc;
            MCAUSE:  rdata = mcause;
            default: rdata = 32'h0;
        endcase
    end

    assign mret_pc  = mepc;
    assign ecall_pc = mtvec;

    always @(posedge clk or posedge rst) begin
        if (rst) begin
            mstatus <= 32'h1800;
            mtvec   <= 32'h0;
            mepc    <= 32'h0;
            mcause  <= 32'h0;
        end else if (ecall) begin
            mepc   <= pc;
            mcause <= 32'h0000_000b;
        end else if (mret) begin
            // mret does not modify CSRs directly here; target PC is exposed via mret_pc
        end else if (csrWrite) begin
            case (addr)
                MSTATUS: if (op != `CSR_NONE) mstatus <= (op == `CSR_CSRRW) ? wdata :
                                                       (op == `CSR_CSRRS) ? (mstatus | wdata) :
                                                                            (mstatus & ~wdata);
                MTVEC:   if (op != `CSR_NONE) mtvec   <= (op == `CSR_CSRRW) ? wdata :
                                                       (op == `CSR_CSRRS) ? (mtvec | wdata) :
                                                                            (mtvec & ~wdata);
                MEPC:    if (op != `CSR_NONE) mepc    <= (op == `CSR_CSRRW) ? wdata :
                                                       (op == `CSR_CSRRS) ? (mepc | wdata) :
                                                                            (mepc & ~wdata);
                MCAUSE:  if (op != `CSR_NONE) mcause  <= (op == `CSR_CSRRW) ? wdata :
                                                       (op == `CSR_CSRRS) ? (mcause | wdata) :
                                                                            (mcause & ~wdata);
                default: ;
            endcase
        end
    end
endmodule