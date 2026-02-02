`include "include/define.vh"

module WBU(
  input 		  clk,
  input 		  rst,
  input			  wen,
  input           lsu_wbu_valid,
  output          wbu_lsu_ready,

  input  [31:0] wdata,
  input  [3:0] waddr,
  input  [3:0] rs1,      
  input  [3:0] rs2,      
  output [31:0] src1,     
  output [31:0] src2,     
  
  input  [11:0]           csr1_raddr,   
  input  [11:0]           csr2_raddr,   
  input                   csr1_wen,     
  input                   is_ecall,    
  input  [31:0]           csr1_wdata,
  input  [31:0]           csr2_wdata,
  input  [11:0]           csr1_waddr,
  output [31:0]           csr1_rdata,
  output [31:0]           csr2_rdata,
  input  [31:0]           lsu_wbu_pc,
  output reg [31:0]       wbu_pc
);
reg [31:0] mstatus;
reg [31:0] mtvec;
reg [31:0] mepc;
reg [31:0] mcause;

localparam [31:0] MVENDORID_VAL = 32'h79737978;
localparam [31:0] MARCHID_VAL   = 32'h25020039;
localparam [31:0] MCAUSE_DEFAULT = 32'h0000_000b;

localparam MSTATUS = 12'h300;
localparam MTVEC   = 12'h305;
localparam MEPC    = 12'h341;
localparam MCAUSE  = 12'h342;
localparam MVENDORID = 12'hf11;
localparam MARCHID   = 12'hf12;

reg [31:0] regs [31:0] ;

assign wbu_lsu_ready = 1'b1;

always @(*) begin
    wbu_pc = lsu_wbu_pc;
end

integer i;
always @(posedge clk) begin
    if(rst)begin
        for(i = 0; i < 32; i = i + 1)begin
            regs[i] <= 32'b0;
        end
    end
    else if (lsu_wbu_valid && wen && (waddr != 4'b0)) begin
        regs[waddr] <= wdata;
    end
end

assign src1 = (rs1 == 4'b0) ? 32'b0 : regs[rs1];
assign src2 = (rs2 == 4'b0) ? 32'b0 : regs[rs2];

always @(posedge clk) begin
    if (rst) begin
        mstatus <= 32'h1800;
        mtvec   <= 32'h0;
        mepc    <= 32'h0;
        mcause  <= MCAUSE_DEFAULT;
    end
    else if (lsu_wbu_valid) begin
        if (csr1_wen) begin
            case(csr1_waddr)
                MSTATUS: mstatus <= csr1_wdata;
                MTVEC:   mtvec   <= csr1_wdata;
                MEPC:    mepc    <= csr1_wdata;
                MCAUSE:  mcause  <= csr1_wdata;
                default: ;
            endcase
        end
        if (is_ecall) begin
            mepc <= csr2_wdata;
        end
    end
end

reg [31:0] csr_rdata1;
reg [31:0] csr_rdata2;

always @(*) begin
    csr_rdata1 = 32'h0;
    case (csr1_raddr)
        MSTATUS:   csr_rdata1 = mstatus;
        MTVEC:     csr_rdata1 = mtvec;
        MEPC:      csr_rdata1 = mepc;
        MCAUSE:    csr_rdata1 = mcause;
        MVENDORID: csr_rdata1 = MVENDORID_VAL;
        MARCHID:   csr_rdata1 = MARCHID_VAL;
        default:   csr_rdata1 = 32'h0;
    endcase
end

always @(*) begin
    csr_rdata2 = 32'h0;
    case (csr2_raddr)
        MSTATUS:   csr_rdata2 = mstatus;
        MTVEC:     csr_rdata2 = mtvec;
        MEPC:      csr_rdata2 = mepc;
        MCAUSE:    csr_rdata2 = mcause;
        MVENDORID: csr_rdata2 = MVENDORID_VAL;
        MARCHID:   csr_rdata2 = MARCHID_VAL;
        default:   csr_rdata2 = 32'h0;
    endcase
end

assign csr1_rdata = csr_rdata1;
assign csr2_rdata = csr_rdata2;

endmodule
