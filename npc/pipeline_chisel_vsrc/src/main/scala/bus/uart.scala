package bus

import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR

// Simulation Helper for outputting characters
class SimUART extends BlackBox with HasBlackBoxInline {
    val io = IO(new Bundle {
        val clk     = Input(Clock())
        val wen     = Input(Bool())
        val waddr   = Input(UInt(32.W))
        val wdata   = Input(UInt(8.W)) // Only lower 8 bits needed
    })

    setInline("SimUART.v",
        """
        |module SimUART(
        |    input clk,
        |    input wen,
        |    input [31:0] waddr,
        |    input [7:0]  wdata
        |);
        |    // UART Output Simulation
        |    always @(posedge clk) begin
        |        if (wen) begin
        |            // Matches UART check: 0xa00003f8
        |            if (waddr >= 32'ha00003f8 && waddr <= 32'ha00003ff) begin
        |                $write("%c", wdata);
        |            end
        |        end
        |    end
        |endmodule
        """.stripMargin
    )
}

class UartWrapper extends Module {
    override def desiredName = "ysyx_25020039_UartWrapper"

    val io = IO(new AXI4Slave)
    
    // Default Output initialization
    io.arready := false.B
    io.rvalid  := false.B
    io.rdata   := 0.U
    io.rresp   := 0.U
    io.rid     := 0.U
    io.rlast   := true.B
    
    io.awready := false.B
    io.wready  := false.B
    io.bvalid  := false.B
    io.bresp   := 0.U
    io.bid     := 0.U

    val sim = Module(new SimUART)
    sim.io.clk := clock
    sim.io.wen := false.B
    sim.io.waddr := 0.U
    sim.io.wdata := 0.U

    // Constants
    val OKAY   = 0.U(2.W)
    val SLVERR = 2.U(2.W)
    
    // State machine definition
    val s_IDLE :: s_READ_ADDR :: s_READ_DATA :: s_WRITE_ADDR :: s_WRITE_DATA :: s_WRITE_RESP :: Nil = Enum(6)
    val state = RegInit(s_IDLE)

    // Registers
    val LFSR_DELAY = Reg(UInt(32.W))
    val araddr_reg = RegInit(0.U(32.W))
    val awaddr_reg = RegInit(0.U(32.W))
    val wdata_reg  = RegInit(0.U(32.W))
    val wstrb_reg  = RegInit(0.U(4.W))
    val rdata_reg  = RegInit(0.U(32.W))
    val arid_reg   = RegInit(0.U(4.W))
    val awid_reg   = RegInit(0.U(4.W))
    val bresp_reg  = RegInit(OKAY)
    val rresp_reg  = RegInit(OKAY)

    def isUartAddr(addr: UInt): Bool = {
        (addr >= "ha00003f8".U && addr <= "ha00003ff".U)
    }

    val lfsr_val = LFSR(16)
    // val random_delay = (lfsr_val % 10.U) + 1.U
    val random_delay = 0.U

    switch(state) {
        is(s_IDLE) {
            io.arready := true.B
            io.awready := true.B
            
            when(io.arvalid) {
                araddr_reg := io.araddr
                arid_reg   := io.arid
                LFSR_DELAY := random_delay
                // Maintain ready high for handshake
                state      := s_READ_ADDR
            }.elsewhen(io.awvalid) {
                awaddr_reg := io.awaddr
                awid_reg   := io.awid
                LFSR_DELAY := random_delay
                // Maintain ready high for handshake
                state      := s_WRITE_ADDR
            }
        }
        
        is(s_READ_ADDR) {
             when(LFSR_DELAY > 0.U) {
                 LFSR_DELAY := LFSR_DELAY - 1.U
             }.otherwise {
                 // Check Address
                 when(isUartAddr(araddr_reg)) {
                     rdata_reg := 0.U
                     rresp_reg := OKAY
                 }.otherwise {
                     rdata_reg := 0.U
                     rresp_reg := SLVERR
                 }
                 state := s_READ_DATA
             }
        }
        
        is(s_READ_DATA) {
            io.rvalid := true.B
            io.rdata  := rdata_reg
            io.rresp  := rresp_reg
            io.rid    := arid_reg
            
            when(io.rready) {
                araddr_reg := 0.U
                state := s_IDLE
            }
        }
        
        is(s_WRITE_ADDR) {
            when(LFSR_DELAY > 0.U) {
                LFSR_DELAY := LFSR_DELAY - 1.U
            }.otherwise {
                state := s_WRITE_DATA
            }
        }
        
        is(s_WRITE_DATA) {
            io.wready := true.B
            
            when(io.wvalid) {
                wdata_reg := io.wdata // Capture data
                
                sim.io.wen   := true.B
                sim.io.waddr := awaddr_reg
                sim.io.wdata := io.wdata(7,0)
                
                when(isUartAddr(awaddr_reg)) {
                     bresp_reg := OKAY
                }.otherwise {
                     bresp_reg := SLVERR 
                }
                
                state := s_WRITE_RESP
            }
        }
        
        is(s_WRITE_RESP) {
            io.bvalid := true.B
            io.bresp  := bresp_reg
            io.bid    := awid_reg
            
            when(io.bready) {
                state := s_IDLE
            }
        }
    }
}
