package bus

import chisel3._
import chisel3.util._

class Sim_Uart extends BlackBox with HasBlackBoxInline {
    override def desiredName = "ysyx_25020039_Sim_Uart"
    val io = IO(new Bundle {
        val clk   = Input(Clock())
        val wen   = Input(Bool())
        val waddr = Input(UInt(32.W))
        val wdata = Input(UInt(8.W))
    })

    setInline("ysyx_25020039_Sim_Uart.v",
        """
        |module ysyx_25020039_Sim_Uart(
        |    input clk,
        |    input wen,
        |    input [31:0] waddr,
        |    input [7:0]  wdata
        |);
        |    always @(posedge clk) begin
        |        if (wen && waddr >= 32'ha00003f8 && waddr <= 32'ha00003ff) begin
        |            $write("%c", wdata);
        |        end
        |    end
        |endmodule
        """.stripMargin
    )
}

class UART extends Module{
    override def desiredName = "ysyx_25020039_UART"
    val io = IO(new AXI4Slave)
    io.setDefaults()

    val uart = Module(new Sim_Uart)
    uart.io.clk := clock
    uart.io.wen := false.B
    uart.io.waddr := 0.U
    uart.io.wdata := 0.U

    val OKAY = 0.U(2.W)
    val SLVERR = 2.U(2.W)

    def is_write(addr: UInt): Bool = {
        addr >= "ha00003f8".U && addr <= "ha00003ff".U
    }

    val s_R_IDLE :: s_R_DATA :: Nil = Enum(2)
    val r_state = RegInit(s_R_IDLE)
    val r_next_state = WireDefault(s_R_IDLE)

    r_next_state := MuxLookup(r_state, s_R_IDLE)(Seq(
        s_R_IDLE -> Mux(io.arvalid, s_R_DATA, s_R_IDLE),
        s_R_DATA -> Mux(io.rready, s_R_IDLE, s_R_DATA)
    ))
    r_state := r_next_state

    val arid_reg = RegInit(0.U(4.W))

    when(io.arvalid && io.arready) {
        arid_reg := io.arid
    }

    io.arready := (r_state === s_R_IDLE)
    io.rvalid := (r_state === s_R_DATA)
    io.rid := arid_reg
    io.rresp := OKAY
    io.rdata := 0.U

    val awid_reg = RegInit(0.U(4.W))
    val awaddr_reg = RegInit(0.U(32.W))
    val bresp_reg = RegInit(OKAY)

    val s_W_IDLE :: s_W_DATA :: s_W_RESP :: Nil = Enum(3)
    val w_state = RegInit(s_W_IDLE)
    val w_next_state = MuxLookup(w_state, s_W_IDLE)(Seq(
        s_W_IDLE -> Mux(io.awvalid, s_W_DATA, s_W_IDLE),
        s_W_DATA -> Mux(io.wvalid, s_W_RESP, s_W_DATA),
        s_W_RESP -> Mux(io.bready, s_W_IDLE, s_W_RESP)
    ))
    w_state := w_next_state

    
    io.awready := (w_state === s_W_IDLE)
    io.wready := (w_state === s_W_DATA)
    io.bvalid := (w_state === s_W_RESP)
    io.bid := awid_reg
    io.bresp := bresp_reg

    when(io.awvalid && io.awready) {
        awid_reg := io.awid
        awaddr_reg := io.awaddr
    }

    when(io.wvalid && io.wready) {
        when(is_write(awaddr_reg)) {
            uart.io.wen := true.B
            uart.io.waddr := awaddr_reg
            uart.io.wdata := io.wdata(7, 0)
            bresp_reg := OKAY
        }.otherwise {
            bresp_reg := SLVERR
        }
    }

}