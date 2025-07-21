`include "defs.vh"
module top(
    input wire clk,
    input wire rst,
    output [`DATA_WIDTH-1:0] imem_pc
);
    import "DPI-C" function int pmem_read(input int raddr);
    import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);

    assign imem_pc = imem_addr;

    // Internal signals
    wire [`DATA_WIDTH-1:0] pc;
    wire [31:0] inst;
    wire [`DATA_WIDTH-1:0] reg_write_data;
    wire [4:0] alu_op;
    wire [1:0] op1_sel;
    wire [1:0] op2_sel;
    wire [2:0] pc_sel;
    wire rf_we;
    wire mem_en;
    wire mem_wen;
    wire [1:0] wb_sel;
    wire [`DATA_WIDTH-1:0] Op1;
    wire [`DATA_WIDTH-1:0] Op2;
    wire [`DATA_WIDTH-1:0] pc_plus4;
    wire [`DATA_WIDTH-1:0] jump_reg_target;
    wire [`DATA_WIDTH-1:0] br_target;
    wire [`DATA_WIDTH-1:0] jmp_target;
    wire br_eq;
    wire br_lt;
    wire br_ltu;
    wire [2:0] ctl_mem_access;
    wire [`REG_ADDR_WIDTH-1:0] rs1_addr;
    wire [`REG_ADDR_WIDTH-1:0] rs2_addr;
    wire [`REG_ADDR_WIDTH-1:0] waddr;
    wire [`DATA_WIDTH-1:0] rdata1;
    wire [`DATA_WIDTH-1:0] rdata2;

    // Instruction Memory interface
    wire [31:0] imem_addr;
    reg [`DATA_WIDTH-1:0] imem_rdata;
    always @(*) begin
        imem_rdata = pmem_read(imem_addr);
    end
    
    // Data Memory interface
    reg [`DATA_WIDTH-1:0] dmem_rdata_raw;
    wire [`DATA_WIDTH-1:0] dmem_rdata;
    wire [`DATA_WIDTH-1:0] dmem_addr;
    wire [`DATA_WIDTH-1:0] dmem_wdata;
    wire [`DATA_WIDTH-1:0] dmem_wdata_raw;
    wire [7:0] wmask;
    assign dmem_wdata_raw = rdata2;

    always @(*) begin
        if (mem_en) begin 
            dmem_rdata_raw = pmem_read(dmem_addr);
            if (mem_wen) begin 
                pmem_write(dmem_addr, dmem_wdata, wmask);
            end
        end else begin
            dmem_rdata_raw = 0;
        end
    end

    alignment_network alignment_network (
        .data_in(dmem_rdata_raw),
        .control(ctl_mem_access),
        .dmem_addr(dmem_addr),
        .data_out(dmem_rdata)
    );

    wmask_gen wmask_gen (
        .control(ctl_mem_access),
        .dmem_addr(dmem_addr),
        .wmask(wmask),
        .dmem_wdata_raw(dmem_wdata_raw),
        .dmem_wdata(dmem_wdata)
    );

    // Handle ebreak signal
    wire is_ebreak;
    import "DPI-C" function void sim_exit(input int ret);
    always @(*) begin
        if (is_ebreak) begin
           $display("EBREAK: Simulation exiting...");
           sim_exit(0);
        end
    end

    // Register File
    RegisterFile #(
        .ADDR_WIDTH(`REG_ADDR_WIDTH),
        .DATA_WIDTH(`DATA_WIDTH)
    ) u_RegisterFile (
        .clk(clk),
        .wdata(reg_write_data),
        .waddr(waddr),
        .wen(rf_we),
        .raddr1(rs1_addr),
        .raddr2(rs2_addr),
        .rdata1(rdata1),
        .rdata2(rdata2)
    );
        
    // Fetch instruction
    IFU ifu (
        .clk(clk),
        .rst(rst),
        .pc_sel(pc_sel),
        .jump_reg_target(jump_reg_target),
        .br_target(br_target),
        .jmp_target(jmp_target),
        .pc_wen(1'b1),
        .pc_o(pc),
        .inst_o(inst),
        .inst_i(imem_rdata),
        .pc_plus4_o(pc_plus4)
    );
    assign imem_addr = pc;

    // Instantiate IDU
    IDU idu (
        .inst_i(inst),
        .Op1(Op1),
        .Op2(Op2),
        .Op1Sel(op1_sel),
        .Op2Sel(op2_sel),
        .rs1_data_i(rdata1),
        .rs2_data_i(rdata2),
        .rd_addr_o(waddr),
        .rs1_addr(rs1_addr),
        .rs2_addr(rs2_addr),
        .pc_i(pc),
        .jump_reg_target_o(jump_reg_target),
        .br_target_o(br_target),
        .jmp_target_o(jmp_target),
        .br_eq(br_eq),
        .br_lt(br_lt),
        .br_ltu(br_ltu)
    );

    // Instantiate Control Logic
    ControlLogic control (
        .inst(inst),
        .br_eq(br_eq),
        .br_lt(br_lt),
        .br_ltu(br_ltu),
        .alu_op(alu_op),
        .op1_sel(op1_sel),
        .op2_sel(op2_sel),
        .pc_sel(pc_sel),
        .rf_we(rf_we),
        .mem_en(mem_en),
        .mem_wen(mem_wen),
        .wb_sel(wb_sel),
        .is_ebreak(is_ebreak),
        .ctl_mem_access(ctl_mem_access)
    );

    // Instantiate EXU
    EXU exu (
        .clk(clk),
        .rst(rst),
        .Op1(Op1),
        .Op2(Op2),
        .alu_op(alu_op),
        .wb_sel(wb_sel),
        .pc_plus4(pc_plus4),
        .dmem_rdata(dmem_rdata),
        .dmem_addr(dmem_addr),
        .reg_write_data(reg_write_data)
    );

endmodule