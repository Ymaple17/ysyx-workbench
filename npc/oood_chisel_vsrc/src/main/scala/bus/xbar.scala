package bus

import chisel3._
import chisel3.util._
import core._

class Xbar_IO(coreConfig: CoreConfig) extends Bundle {
  val imem = new AXI4Slave
  val dmem = new AXI4Slave
  val soc = new AXI4Master
  val clint = new AXI4Master
  val uart = if(coreConfig.npc) Some(new AXI4Master) else None
}

class Xbar(coreConfig: CoreConfig) extends Module{
    val io = IO(new Xbar_IO(coreConfig))

    val clint_read = Wire(Bool())
    val clint_write = Wire(Bool())
    val uart_read = Wire(Bool())
    val uart_write = Wire(Bool())

    if(coreConfig.npc) {
        clint_read := io.dmem.arvalid && io.dmem.araddr >= "ha0000048".U && io.dmem.araddr <= "ha000004f".U
        clint_write := io.dmem.awvalid && io.dmem.awaddr >= "ha0000048".U && io.dmem.awaddr <= "ha000004f".U
        
        uart_read := io.dmem.arvalid && io.dmem.araddr >= "ha00003f8".U && io.dmem.araddr <= "ha00003ff".U
        uart_write := io.dmem.awvalid && io.dmem.awaddr >= "ha00003f8".U && io.dmem.awaddr <= "ha00003ff".U
    } else {
        clint_read := io.dmem.araddr >= "h0200_0000".U && io.dmem.araddr <= "h0200_ffff".U
        clint_write := io.dmem.awaddr >= "h0200_0000".U && io.dmem.awaddr <= "h0200_ffff".U
        
        uart_read := false.B
        uart_write := false.B
    }

    val s_SELECT :: s_IMEM :: s_DMEM :: s_CLINT :: s_UART :: Nil = Enum(5)
    val state = RegInit(s_SELECT)
    val next_state = WireDefault(s_SELECT)
    // 进入 DMEM 时记下是读还是写，退出必须匹配；避免残留 rvalid 打断写事务导致从设备卡死
    val dmem_is_write = RegInit(false.B)

    val is_dmem = io.dmem.arvalid || io.dmem.awvalid
    val is_imem = io.imem.arvalid && !is_dmem && (state === s_SELECT)
    val dmemWriteFire = io.dmem.awvalid && io.dmem.awready
    val dmemReadFire = !io.dmem.awvalid && io.dmem.arvalid && io.dmem.arready
    val dmemRequestFire = dmemWriteFire || dmemReadFire
    val imemRequestFire = io.imem.arvalid && io.imem.arready

    // 5：dmem（含 commit store）优先，避免 IFU 连取时 IMEM 独占导致 awready 永不来
    next_state := MuxLookup(state, s_SELECT)(Seq(
        s_SELECT -> MuxCase(s_SELECT, Seq(
            (is_dmem && dmemRequestFire && (uart_read || uart_write)) -> s_UART,
            (is_dmem && dmemRequestFire && (clint_read || clint_write)) -> s_CLINT,
            (is_dmem && dmemRequestFire) -> s_DMEM,
            (is_imem && imemRequestFire) -> s_IMEM
        )),
        s_IMEM -> Mux(io.soc.rvalid && io.soc.rready && io.soc.rlast, s_SELECT, s_IMEM),
        s_DMEM -> Mux(
          Mux(dmem_is_write, io.soc.bvalid && io.soc.bready,
            io.soc.rvalid && io.soc.rready && io.soc.rlast),
          s_SELECT, s_DMEM),
        s_CLINT -> Mux(
          Mux(dmem_is_write, io.clint.bvalid && io.clint.bready, io.clint.rvalid && io.clint.rready),
          s_SELECT, s_CLINT),
        s_UART -> (if(coreConfig.npc) Mux(
          Mux(dmem_is_write, io.uart.get.bvalid && io.uart.get.bready, io.uart.get.rvalid && io.uart.get.rready),
          s_SELECT, s_UART) else s_SELECT)
    ))
    state := next_state
    when(state === s_SELECT && is_dmem && dmemRequestFire) {
      dmem_is_write := dmemWriteFire
    }

    io.imem.setDefaults()
    io.dmem.setDefaults()
    io.soc.setDefaults()
    io.clint.setDefaults()
    io.uart.foreach(_.setDefaults())

    switch(state){
        is(s_SELECT){
            when(is_dmem && (uart_read || uart_write)){
                io.uart.foreach {u => io.dmem <> u}
            }.elsewhen(is_dmem && (clint_read || clint_write)){
                io.dmem <> io.clint
            }.elsewhen(is_dmem){
                io.dmem <> io.soc
            }.elsewhen(is_imem){
                io.imem <> io.soc
            }
        }
        is(s_IMEM){
            io.imem <> io.soc
        }
        is(s_DMEM){
            io.dmem <> io.soc
        }
        is(s_CLINT){
            io.dmem <> io.clint
        }
        is(s_UART){
            io.uart.foreach {u => io.dmem <> u}
        }
    }
    
}
