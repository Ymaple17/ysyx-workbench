`include "/home/qiu/ysyx-workbench/npc/mul_vsrc/include/defs.vh"

module MEM (
    input             clk,
    input             reset,
    input             ex_valid,//上游ex输出是否有效
    output reg        mem_ready,//mem就绪状态
    input             wb_ready,//下游wb写回是否就绪
    output reg        mem_valid,//mem模块输出是否有效
    input             MemRead,
    input             MemWrite,
    input      [31:0] addr,
    input      [31:0] data_in,
    input      [2:0]  MemLen,
    output reg [31:0] data_out,
    output reg        load_access_fault,//异常访问
    output reg        store_access_fault,//异常写入
    output reg [31:0] mem_fault_addr,//异常地址

    output reg [31:0] sram_araddr,
    output reg        sram_arvalid,
    input wire        sram_arready,
    input wire [31:0] sram_rdata,
    input wire        sram_rvalid,
    output reg        sram_rready,
    input wire [1:0]  sram_rresp,
    output reg [31:0] sram_awaddr,
    output reg        sram_awvalid,
    input wire        sram_awready,
    output reg [31:0] sram_wdata,
    output reg [3:0]  sram_wstrb,
    output reg        sram_wvalid,
    input wire        sram_wready,
    input wire [1:0]  sram_bresp,
    input wire        sram_bvalid,
    output reg        sram_bready
);
    //====状态机定义====//  
    typedef enum {IDLE, READ_ADDR, READ_DATA, 
    WRITE_ADDR, WRITE_DATA, WRITE_RESP, STALL} state_t;
    state_t state;
    
    reg [1:0] delay;
    parameter DELAY_CYCLES = 3;

    always @(posedge clk or posedge reset) begin
        if(reset) begin
            state <= IDLE;
            delay <= DELAY_CYCLES;
            mem_ready <= 1'b1;
            mem_valid <= 1'b0;
            data_out <= 32'h0;

            sram_arvalid <= 1'b0;
            sram_rready <= 1'b0;
            sram_wvalid <= 1'b0;
            sram_araddr <= 32'h0;
            sram_awaddr <= 32'h0;
            sram_awvalid <= 1'b0;
            sram_wdata <= 32'h0;
            sram_wstrb <= 4'b1111;
            sram_bready <= 1'b0;
            load_access_fault <= 1'b0;
            store_access_fault <= 1'b0;
            mem_fault_addr <= 32'h0;
        end
        else begin
            case(state)
                IDLE: begin
                    mem_ready <= 1'b1;
                    mem_valid <= 1'b0;
                    sram_arvalid <= 1'b0;
                    sram_rready <= 1'b0;
                    sram_awvalid <= 1'b0;
                    sram_bready <= 1'b0;
                    load_access_fault <= 1'b0;
                    store_access_fault <= 1'b0;
                    if(ex_valid && mem_ready) begin//mem和上游ex握手
                        if(MemRead) begin
                            sram_araddr <= addr;
                            sram_arvalid <= 1'b1;//发送sram读请求
                            state <= READ_ADDR;
                        end
                        else if(MemWrite) begin
                            sram_awaddr <= addr;
                            sram_awvalid <= 1'b1;//发送sram写地址请求
                            sram_wdata <= data_in;
                            case(MemLen)
                                `Mem_Bit:  sram_wstrb <= 4'b0001;//sb
                                `Mem_Half: sram_wstrb <= 4'b0011;//sh
                                `Mem_Word: sram_wstrb <= 4'b1111;//sw
                                default:   sram_wstrb <= 4'b1111;
                            endcase
                            state <= WRITE_ADDR;
                        end
                        else begin
                            state <= STALL;
                        end
                    end
                    else begin
                        state <= IDLE;
                    end
                end

                READ_ADDR: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    // sram_arvalid <= 1'b1;
                    if(sram_arready && sram_arvalid) begin

                        state <= READ_DATA;
                    end
                    else begin
                        state <= READ_ADDR;
                    end
                end

                READ_DATA: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    sram_rready <= 1'b1;
                    if(sram_rvalid && sram_rready) begin
                        sram_arvalid <= 1'b0;
                        case(MemLen)
                            `Mem_Bit:   data_out <= {{24{sram_rdata[7]}},sram_rdata[7:0]}; 
                            `Mem_UBit:  data_out <= {24'b0,sram_rdata[7:0]};
                            `Mem_UHalf: data_out <= {16'b0,sram_rdata[15:0]};
                            `Mem_Half:  data_out <= {{16{sram_rdata[15]}},sram_rdata[15:0]};
                            `Mem_Word:  data_out <= sram_rdata;
                            default:    data_out <= 32'h0;
                        endcase
                        if(sram_rresp != `OKAY) begin
                            load_access_fault <= 1'b1;
                            mem_fault_addr <= sram_araddr;
                        end
                        else begin
                            load_access_fault <= 1'b0;
                            mem_fault_addr <= 32'h0;
                        end
                        state <= STALL;
                    end
                    else begin
                        state <= READ_DATA;
                    end
                end

                WRITE_ADDR: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    if(sram_awready && sram_awvalid) begin
                        sram_wvalid <= 1'b1;//发送写请求
                        state <= WRITE_DATA;
                    end
                    else begin
                        state <= WRITE_ADDR;
                    end
                end

                WRITE_DATA: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    // sram_wvalid <= 1'b1;//发送写请求
                    if(sram_wready && sram_wvalid) begin
                        sram_awvalid <= 1'b0;
                        sram_bready <= 1'b1;//准备接受写响应
                        
                        state <= WRITE_RESP;
                    end
                    else begin
                        state <= WRITE_DATA;
                    end
                end
                
                WRITE_RESP: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    if(sram_bvalid && sram_bready) begin
                        if(sram_bresp != `OKAY) begin
                            store_access_fault <= 1'b1;
                            mem_fault_addr <= sram_awaddr;
                        end
                        else begin
                            store_access_fault <= 1'b0;
                            mem_fault_addr <= 32'h0;
                        end
                        state <= STALL;
                    end
                    else begin
                        state <= WRITE_RESP;
                    end
                end
                STALL: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b1;
                    sram_wvalid <= 1'b0;
                    sram_rready <= 1'b0;
                    sram_arvalid <= 1'b0;
                    sram_bready <= 1'b0;
                    sram_awvalid <= 1'b0;
                    if(wb_ready) begin
                        state <= IDLE;
                    end
                    else begin
                        state <= STALL;
                    end
                end

                default: begin
                    mem_ready <= 1'b0;
                    mem_valid <= 1'b0;
                    state <= IDLE;
                end
            endcase
        end
    end
    // 协议断言
    always @(posedge clk) begin
        assert(!(sram_arvalid && sram_arready && state != READ_ADDR)) else $error("[MEM] AR channel handshake in wrong state");
        assert(!(sram_rvalid && sram_rready && state != READ_DATA)) else $error("[MEM] R channel handshake in wrong state");
        assert(!(sram_awvalid && sram_awready && state != WRITE_ADDR)) else $error("[MEM] AW channel handshake in wrong state");
        assert(!(sram_wvalid && sram_wready && state != WRITE_DATA)) else $error("[MEM] W channel handshake in wrong state");
        assert(!(sram_bvalid && sram_bready && state != WRITE_RESP)) else $error("[MEM] B channel handshake in wrong state");
    end
endmodule