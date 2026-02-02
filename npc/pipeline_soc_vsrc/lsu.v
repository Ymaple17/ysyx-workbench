`include "include/define.vh"
module LSU(
    input         clk,
    input         rst,

    input         exu_lsu_valid,       
    output reg    lsu_exu_ready,       
    input         wbu_lsu_ready,       
    output reg    lsu_wbu_valid,       

    input         exu_lsu_forward_las,
    input         exu_lsu_RegWrite,    
    input  [ 3:0] exu_lsu_rd,          
    input         exu_lsu_MemRead,     
    input         exu_lsu_MemWrite,    
    input  [ 4:0] exu_lsu_MemLen,         
    input  [31:0] addr,               
    input  [31:0] data_in,              

    input         exu_lsu_csr,
    input         exu_lsu_csr_wen1,
    input  [31:0] exu_lsu_csr_wr_data1,
    input  [31:0] exu_lsu_csr_wr_data2,
    input  [11:0] exu_lsu_csr_wr_addr1,
    input  [31:0] exu_lsu_csr_rdata,
    input         exu_lsu_csr_ecall,
    input         exu_lsu_csr_mret,
    input  [31:0] exu_lsu_process_result,
    input  [31:0] exu_lsu_pc,

    output [ 3:0] lsu_exu_forward_rd,         
    output        lsu_exu_forward_RegWrite,   
    output        lsu_exu_forward_MemRead,    

    output reg [31:0] lsu_wbu_csr_wr_data1,
    output reg [31:0] lsu_wbu_csr_wr_data2,
    output reg [11:0] lsu_wbu_csr_wr_addr1,
    output reg        lsu_wbu_csr_wen1,
    output reg        lsu_wbu_csr_ecall,
    output reg        lsu_wbu_RegWrite,        
    output reg [ 3:0] lsu_wbu_rd,              
    output reg [31:0] lsu_wbu_write_rd_data,   
    output reg [31:0] lsu_wbu_pc,

    // AXI4-Lite
    output reg        lsu_axi_arvalid,
    input             axi_lsu_arready,      
    output wire [31:0] lsu_axi_araddr,
    output wire [ 3:0] lsu_axi_arid,
    output wire [ 7:0] lsu_axi_arlen,
    output reg  [ 2:0] lsu_axi_arsize,
    output wire [ 1:0] lsu_axi_arburst,        
    input      [31:0] axi_lsu_rdata,         
    input             axi_lsu_rvalid,       
    output reg        lsu_axi_rready,       
    input      [ 1:0] axi_lsu_rresp,
    input      [ 3:0] axi_lsu_rid,
    input             axi_lsu_rlast,         

    output wire [31:0] lsu_axi_awaddr,        
    output reg        lsu_axi_awvalid,      
    input             axi_lsu_awready, 
    output wire [ 3:0] lsu_axi_awid,
    output wire [ 7:0] lsu_axi_awlen,
    output reg  [ 2:0] lsu_axi_awsize,
    output wire [ 1:0] lsu_axi_awburst,     
    output wire [31:0] lsu_axi_wdata,         
    output wire [ 3:0] lsu_axi_wstrb,         
    output reg        lsu_axi_wvalid,       
    input             axi_lsu_wready,
    output reg        lsu_axi_wlast,       
    input      [ 1:0] axi_lsu_bresp,         
    input             axi_lsu_bvalid,       
    output wire       lsu_axi_bready,
    input      [ 3:0] axi_lsu_bid     
);


    localparam SDRAM_BASE        = 32'hA0000000;  
    localparam SDRAM_END         = 32'hBFFFFFFF;  
    localparam AXI_BURST_FIXED   = 2'b00;       
    localparam AXI_BURST_INCR    = 2'b01;   
    localparam AXI_SIZE_BYTE     = 3'h0;         
    localparam AXI_SIZE_HALF     = 3'h1;  
    localparam AXI_SIZE_WORD     = 3'h2;
    localparam AXI_ID            = 4'h0;  
    localparam BURST_LEN         = 4; 
    localparam BLOCK_SIZE        = 16;

    wire in_sdram = (saved_addr >= SDRAM_BASE) && (saved_addr <= SDRAM_END);
    // wire in_sdram = 0;

    localparam BLOCK_OFFSET_WIDTH = 4; 
    wire [1:0] word_offset = addr[3:2];

    reg [1:0] saved_word_offset;
    reg [31:0] saved_wdata;  
    reg [3:0] saved_wstrb; 
    reg [3:0] burst_cnt; 

    reg  [ 1:0] addr_off;   
    reg  [31:0] rdata;  
    reg         valid;   
    reg  [31:0] saved_addr; 

    localparam IDLE = 2'b00; 
    localparam RD   = 2'b10; 
    localparam WR   = 2'b01; 
    reg [1:0] lsu_state, lsu_next_state;

    reg        aw_done;  
    reg        w_done;   
    reg        b_done;   
    reg        ar_done;

    wire we = (exu_lsu_valid & lsu_exu_ready & exu_lsu_MemWrite);
    wire req_valid = ((exu_lsu_valid & lsu_exu_ready) & (exu_lsu_MemRead | exu_lsu_MemWrite));

    always @(posedge clk) begin
        if (lsu_state == IDLE & req_valid) begin
            addr_off <= addr[1:0];
            saved_addr <= addr; 
            saved_word_offset <= word_offset;
            saved_wdata       <= (exu_lsu_MemLen[3:0] == 4'b0001) ? ({24'b0, data_in[7:0]} << (addr[1:0] * 8)) :
                                 (exu_lsu_MemLen[3:0] == 4'b0011) ? (addr[1:0] == 2'b00 ? {16'b0, data_in[15:0]} :
                                                                    addr[1:0] == 2'b10 ? {data_in[15:0], 16'b0} :
                                                                    data_in) : data_in;
            saved_wstrb       <= exu_lsu_MemLen[3:0] << addr[1:0];
        end
    end

    always @(posedge clk) begin
        if (rst) begin
            lsu_state <= IDLE;
        end else begin
            lsu_state <= lsu_next_state;
        end
    end

    always @(*) begin
        case (lsu_state)
            IDLE: lsu_next_state = req_valid ? (we ? WR : RD) : IDLE;
            RD:   lsu_next_state = (axi_lsu_rvalid && lsu_axi_rready && axi_lsu_rlast) ? IDLE : RD;
            WR:   lsu_next_state = (aw_done && w_done && b_done) ? IDLE : WR;
            default: lsu_next_state = IDLE;
        endcase
    end
    
    always @(posedge clk) begin
        if(lsu_state == IDLE) begin
            aw_done <= 0;
            w_done  <= 0;
            b_done  <= 0;
            burst_cnt <= 0;
        end
        else begin
            if(axi_lsu_rvalid && lsu_axi_rready) begin
                burst_cnt <= burst_cnt + 1;
            end
            if (lsu_axi_awvalid && axi_lsu_awready) aw_done <= 1'b1;
            if (lsu_axi_wvalid && axi_lsu_wready && lsu_axi_wlast) w_done <= 1'b1;
            if (axi_lsu_bvalid && lsu_axi_bready) b_done <= 1'b1;
        end
    end

    reg [BLOCK_SIZE*8-1:0] block_data;
    reg [4:0]  l_MemLen;

    assign lsu_axi_arid    = AXI_ID;
    assign lsu_axi_araddr  = in_sdram ? {saved_addr[31:BLOCK_OFFSET_WIDTH], {BLOCK_OFFSET_WIDTH{1'b0}}} : saved_addr;
    assign lsu_axi_arburst = in_sdram ? AXI_BURST_INCR : AXI_BURST_FIXED;
    assign lsu_axi_arlen   = in_sdram ? BURST_LEN - 1 : 8'h0;
    assign lsu_axi_awburst = AXI_BURST_FIXED;
    assign lsu_axi_awid    = AXI_ID;
    assign lsu_axi_awlen   = 8'h0;
    assign lsu_axi_awaddr  = saved_addr;
    assign lsu_axi_wstrb   = saved_wstrb;
    assign lsu_axi_wdata   = saved_wdata;
    assign lsu_axi_bready  = 1'b1;

    always @(*) begin
        case(l_MemLen[3:0])
            4'b0001: lsu_axi_awsize = AXI_SIZE_BYTE;
            4'b0011: lsu_axi_awsize = AXI_SIZE_HALF;
            default: lsu_axi_awsize = AXI_SIZE_WORD; 
        endcase
        case(l_MemLen[3:0])
            4'b0001: lsu_axi_arsize = AXI_SIZE_BYTE;
            4'b0011: lsu_axi_arsize = AXI_SIZE_HALF;
            default: lsu_axi_arsize = AXI_SIZE_WORD; 
        endcase
    end

    always @(posedge clk) begin
        if (rst) begin
            lsu_axi_awvalid <= 1'b0;
            lsu_axi_wvalid  <= 1'b0;
            lsu_axi_arvalid <= 1'b0;
        end 
        if (lsu_state == RD) begin
            if (!ar_done && !lsu_axi_arvalid) begin
                lsu_axi_arvalid <= 1'b1;
            end else if (axi_lsu_arready) begin
                lsu_axi_arvalid <= 1'b0;  
                ar_done         <= 1'b1;
            end
            lsu_axi_rready <= 1'b1;
            if(axi_lsu_rvalid && in_sdram) begin
                block_data[burst_cnt*32 +: 32] = axi_lsu_rdata;
            end
        end 
        else begin
            lsu_axi_arvalid <= 1'b0; 
            ar_done         <= 1'b0;
        end

        if (lsu_state == WR) begin
            if (!lsu_axi_awvalid && !aw_done) begin 
                lsu_axi_awvalid <= 1'b1;
            end else if (axi_lsu_awready) begin
                lsu_axi_awvalid <= 1'b0;
            end

            if (!lsu_axi_wvalid && !w_done) begin 
                lsu_axi_wvalid <= 1'b1;
                lsu_axi_wlast  <= 1'b1; 
            end else if (axi_lsu_wready) begin
                lsu_axi_wvalid <= 1'b0;
                lsu_axi_wlast  <= 1'b0;
            end
        end
    end

    always @(posedge clk) begin
        if (rst) begin
            rdata  <= 32'h0;
            valid  <= 1'b0;
        end else begin  
            valid <= 1'b0;
            if (axi_lsu_rvalid && lsu_axi_rready && axi_lsu_rlast) begin
                rdata <= in_sdram ? block_data[saved_word_offset*32 +: 32] : axi_lsu_rdata;
                valid <= 1'b1;
            end
            if(aw_done && w_done && b_done) begin
                valid <= 1'b1;
            end
        end
    end

    reg        l_load;            
    reg        l_rd_en;           
    reg [3:0]  l_rd_addr;                   

    assign lsu_exu_forward_rd        = l_rd_addr;
    assign lsu_exu_forward_RegWrite  = l_rd_en;
    assign lsu_exu_forward_MemRead   = l_load;

    always @(posedge clk) begin
        if (rst) begin
            l_load    <= 0;
            l_rd_en   <= 0;
            l_rd_addr <= 0;
            l_MemLen  <= 0;
        end 
        else if (req_valid) begin
            l_load    <= exu_lsu_MemRead;
            l_rd_en   <= exu_lsu_RegWrite;
            l_rd_addr <= exu_lsu_rd;
            l_MemLen  <= exu_lsu_MemLen;
        end 
        else if (lsu_wbu_valid & wbu_lsu_ready) begin
            l_load    <= 0;
            l_rd_en   <= exu_lsu_RegWrite;
            l_rd_addr <= exu_lsu_rd;
            l_MemLen  <= exu_lsu_MemLen;
        end 
        else if (exu_lsu_valid & lsu_exu_ready & ~(exu_lsu_MemRead | exu_lsu_MemWrite)) begin
            l_load    <= 0;
            l_rd_en   <= exu_lsu_RegWrite;
            l_rd_addr <= exu_lsu_rd;
            l_MemLen  <= exu_lsu_MemLen;
        end 
    end

    wire [31:0] byte_data1 = (rdata >> (addr_off*8));
    wire [ 7:0] byte_data = byte_data1[7:0];
    wire [15:0] half_data = (addr_off == 2'b10) ? (rdata[31:16]) : rdata[15:0];
    wire [31:0] read_lsu_data = (l_MemLen == `Mem_Bit  ) ? {{24{byte_data[7]}}, byte_data} :
                                (l_MemLen == `Mem_UBit ) ? {24'b0, byte_data} :
                                (l_MemLen == `Mem_Half ) ? {{16{half_data[15]}}, half_data} :
                                (l_MemLen == `Mem_UHalf) ? {16'b0, half_data} :
                                rdata;
                                
    reg [31:0] rd_data;
    always @(*) begin
        if (l_load) begin
            rd_data = read_lsu_data;
        end 
        else if (exu_lsu_forward_las) begin
            rd_data = data_in;
        end 
        else if (exu_lsu_MemWrite) begin
            rd_data = 32'h0;
        end 
        else if (exu_lsu_csr & !exu_lsu_csr_ecall & !exu_lsu_csr_mret) begin
            rd_data = exu_lsu_csr_rdata;
        end 
        else begin
            rd_data = exu_lsu_process_result;
        end
    end

    always @(posedge clk) begin
        if (rst) begin
            lsu_exu_ready <= 1;
        end 
        else if (req_valid) begin
            lsu_exu_ready <= 0;
        end 
        else if (lsu_wbu_valid & wbu_lsu_ready) begin
            lsu_exu_ready <= 1;
        end 
    end

    always @(posedge clk) begin
        if (rst) begin
            lsu_wbu_valid <= 0;
        end 
        else if (exu_lsu_valid & lsu_exu_ready) begin
            lsu_wbu_valid <= ~(exu_lsu_MemRead | exu_lsu_MemWrite);
        end 
        else if (valid) begin
            lsu_wbu_valid <= 1;
        end 
        else if ((~(exu_lsu_valid && lsu_exu_ready)) && lsu_wbu_valid) begin
            lsu_wbu_valid <= 0;
        end
    end

    always @(posedge clk) begin
        if (valid) begin
            lsu_wbu_RegWrite      <= l_rd_en;
            lsu_wbu_rd            <= l_rd_addr;
            lsu_wbu_csr_wen1      <= exu_lsu_csr_wen1;
            lsu_wbu_csr_ecall     <= exu_lsu_csr_ecall;
            lsu_wbu_csr_wr_addr1  <= exu_lsu_csr_wr_addr1;
            lsu_wbu_csr_wr_data1  <= exu_lsu_csr_wr_data1;
            lsu_wbu_csr_wr_data2  <= exu_lsu_csr_wr_data2;
            lsu_wbu_write_rd_data <= rd_data;
            lsu_wbu_pc            <= exu_lsu_pc;
        end else if (exu_lsu_valid & lsu_exu_ready & ~(exu_lsu_MemRead | exu_lsu_MemWrite)) begin
            lsu_wbu_RegWrite      <= exu_lsu_RegWrite;
            lsu_wbu_rd            <= exu_lsu_rd;
            lsu_wbu_csr_wen1      <= exu_lsu_csr_wen1;
            lsu_wbu_csr_ecall     <= exu_lsu_csr_ecall;
            lsu_wbu_csr_wr_addr1  <= exu_lsu_csr_wr_addr1;
            lsu_wbu_csr_wr_data1  <= exu_lsu_csr_wr_data1;
            lsu_wbu_csr_wr_data2  <= exu_lsu_csr_wr_data2;
            lsu_wbu_write_rd_data <= rd_data;
            lsu_wbu_pc            <= exu_lsu_pc;
        end
    end


    // Performance Monitor
    import "DPI-C" function void npc_pm_event(input int event_id, input longint data);

    always @(posedge clk) begin
        if (!rst) begin
            if (exu_lsu_valid && lsu_exu_ready) begin
                if (exu_lsu_MemRead) npc_pm_event(3, 1); // EVENT_LSU_READ
                if (exu_lsu_MemWrite) npc_pm_event(4, 1); // EVENT_LSU_WRITE
            end
            if (exu_lsu_valid && (exu_lsu_MemRead || exu_lsu_MemWrite)) begin
                npc_pm_event(5, 1); // EVENT_LSU_LATENCY (accumulate cycles)
            end
        end
    end

endmodule


