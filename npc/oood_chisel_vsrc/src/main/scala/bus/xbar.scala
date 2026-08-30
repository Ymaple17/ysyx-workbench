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

    io.imem.setDefaults()
    io.dmem.setDefaults()
    io.soc.setDefaults()
    io.clint.setDefaults()
    io.uart.foreach(_.setDefaults())

    def connectRead(up: AXI4Slave, down: AXI4Master): Unit = {
      down.araddr := up.araddr
      down.arvalid := up.arvalid
      down.arid := up.arid
      down.arlen := up.arlen
      down.arsize := up.arsize
      down.arburst := up.arburst
      up.arready := down.arready
      up.rdata := down.rdata
      up.rresp := down.rresp
      up.rvalid := down.rvalid
      up.rlast := down.rlast
      up.rid := down.rid
      down.rready := up.rready
    }

    def connectWrite(up: AXI4Slave, down: AXI4Master): Unit = {
      down.awaddr := up.awaddr
      down.awvalid := up.awvalid
      down.awid := up.awid
      down.awlen := up.awlen
      down.awsize := up.awsize
      down.awburst := up.awburst
      up.awready := down.awready
      down.wdata := up.wdata
      down.wstrb := up.wstrb
      down.wvalid := up.wvalid
      down.wlast := up.wlast
      up.wready := down.wready
      up.bresp := down.bresp
      up.bvalid := down.bvalid
      up.bid := down.bid
      down.bready := up.bready
    }

    def isClint(addr: UInt): Bool = if (coreConfig.npc) {
      addr >= "ha0000048".U && addr <= "ha000004f".U
    } else {
      addr >= "h0200_0000".U && addr <= "h0200_ffff".U
    }
    def isUart(addr: UInt): Bool = if (coreConfig.npc) {
      addr >= "ha00003f8".U && addr <= "ha00003ff".U
    } else false.B

    val rSelect :: rImem :: rDmem :: rClint :: rUart :: Nil = Enum(5)
    val rState = RegInit(rSelect)
    val dmemReadClint = isClint(io.dmem.araddr)
    val dmemReadUart = isUart(io.dmem.araddr)

    switch(rState) {
      is(rSelect) {
        when(io.dmem.arvalid) {
          when(dmemReadUart) {
            io.uart.foreach { u => connectRead(io.dmem, u) }
            when(io.dmem.arvalid && io.dmem.arready) { rState := rUart }
          }.elsewhen(dmemReadClint) {
            connectRead(io.dmem, io.clint)
            when(io.dmem.arvalid && io.dmem.arready) { rState := rClint }
          }.otherwise {
            connectRead(io.dmem, io.soc)
            when(io.dmem.arvalid && io.dmem.arready) { rState := rDmem }
          }
        }.elsewhen(io.imem.arvalid) {
          connectRead(io.imem, io.soc)
          when(io.imem.arvalid && io.imem.arready) { rState := rImem }
        }
      }
      is(rImem) {
        connectRead(io.imem, io.soc)
        when(io.soc.rvalid && io.soc.rready && io.soc.rlast) { rState := rSelect }
      }
      is(rDmem) {
        connectRead(io.dmem, io.soc)
        when(io.soc.rvalid && io.soc.rready && io.soc.rlast) { rState := rSelect }
      }
      is(rClint) {
        connectRead(io.dmem, io.clint)
        when(io.clint.rvalid && io.clint.rready) { rState := rSelect }
      }
      is(rUart) {
        io.uart.foreach { u =>
          connectRead(io.dmem, u)
          when(u.rvalid && u.rready) { rState := rSelect }
        }
      }
    }

    val wSelect :: wDmem :: wClint :: wUart :: Nil = Enum(4)
    val wState = RegInit(wSelect)
    val dmemWriteClint = isClint(io.dmem.awaddr)
    val dmemWriteUart = isUart(io.dmem.awaddr)

    switch(wState) {
      is(wSelect) {
        when(io.dmem.awvalid) {
          when(dmemWriteUart) {
            io.uart.foreach { u => connectWrite(io.dmem, u) }
            when(io.dmem.awvalid && io.dmem.awready) { wState := wUart }
          }.elsewhen(dmemWriteClint) {
            connectWrite(io.dmem, io.clint)
            when(io.dmem.awvalid && io.dmem.awready) { wState := wClint }
          }.otherwise {
            connectWrite(io.dmem, io.soc)
            when(io.dmem.awvalid && io.dmem.awready) { wState := wDmem }
          }
        }
      }
      is(wDmem) {
        connectWrite(io.dmem, io.soc)
        val nextIsDmem = io.dmem.awvalid && !dmemWriteUart && !dmemWriteClint
        io.soc.awvalid := nextIsDmem
        io.dmem.awready := io.soc.awready && !dmemWriteUart && !dmemWriteClint
        when(io.soc.bvalid && io.soc.bready) {
          when(nextIsDmem && io.soc.awready) {
            wState := wDmem
          }.otherwise {
            wState := wSelect
          }
        }
      }
      is(wClint) {
        connectWrite(io.dmem, io.clint)
        when(io.clint.bvalid && io.clint.bready) { wState := wSelect }
      }
      is(wUart) {
        io.uart.foreach { u =>
          connectWrite(io.dmem, u)
          when(u.bvalid && u.bready) { wState := wSelect }
        }
      }
    }
}
