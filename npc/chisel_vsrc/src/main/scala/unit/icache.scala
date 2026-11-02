package unit

import chisel3._
import chisel3.util._
import bus._
import core._
import core.PerfEvents._

class ICache_IO extends Bundle{
    val in = Flipped(new IFU_ICACHE_IO)
    val out = new AXI4Master
    val fencei = Flipped(Irrevocable(new IFU_signals))
}

class ICache_Block(val tag_size: Int, val block_size: Int) extends Bundle{
    val valid = Bool()
    val tag = UInt(tag_size.W)
    val data = Vec(block_size/4, UInt(32.W))
}

class ICache_Set(val tag_size : Int, val block_size : Int, val way : Int) extends Bundle{
    val set = Vec(way, new ICache_Block(tag_size, block_size))
}

class ICache(val set : Int,val way : Int, val block_size : Int, val conf: CoreConfig) extends Module {

    val io = IO(new ICache_IO)

    val bus_width = 4
    val m = log2Ceil(block_size)
    val n = log2Ceil(set)
    val w = log2Ceil(way).max(1)
    val c = block_size / 4
    val cntWidth = log2Ceil(c + 1).max(1)
    val count = RegInit(0.U(cntWidth.W))
    val tag_size = 32 - m - n

    val icache = RegInit(0.U.asTypeOf(Vec(set, new ICache_Set(tag_size, block_size, way))))
    val base_addr = Wire(UInt(32.W))
    val tagA = Wire(UInt(tag_size.W))
    val index = Wire(UInt(n.W))
    val offset = Wire(UInt(m.W))

    val rdata = RegEnable(io.in.rdata, 0.U(32.W), io.in.rvalid)

    val Cache_Set = Wire(new ICache_Set(tag_size, block_size, way))

    val way_hit = Wire(UInt(w.W))
    val hit_data = Wire(UInt((bus_width*8).W))
    val miss_data = Wire(UInt((bus_width*8).W))
    val hit = Wire(Bool())

    val fencei_cnt = RegInit(0.U(n.W))
    val fifo_ptr = RegInit(0.U(w.W))

    val in_addr = RegEnable(io.in.araddr, 0.U(32.W), io.in.arvalid && io.in.arready)

    val is_sdram = RegEnable(io.in.araddr >= "ha000_0000".U(32.W) && io.in.araddr <= "hbfff_ffff".U(32.W), false.B, io.in.arready & io.in.arvalid)

    val s_IFU_AR :: s_AXI_AR :: s_AXI_R :: s_IFU_DATA :: s_FENCEI :: Nil = Enum(5)

    val hit_next_state = Mux(io.in.rready, s_IFU_AR, s_IFU_DATA)
    val miss_next_state = Mux(io.out.arready, s_AXI_R, s_AXI_AR)

    val state = RegInit(s_IFU_AR)
    val next_state = WireDefault(state)


    next_state := MuxLookup(state, s_IFU_AR)(Seq(
        s_IFU_AR -> MuxCase(s_IFU_AR,
                Seq((io.in.arvalid && io.in.arready) -> Mux(hit, hit_next_state, miss_next_state),
                      (io.fencei.valid && io.fencei.bits.is_fencei) -> s_FENCEI)),
        s_AXI_AR -> Mux(io.out.arready, s_AXI_R, s_AXI_AR),
        s_AXI_R -> Mux(io.out.rvalid, Mux(count === 0.U, hit_next_state, Mux(is_sdram, s_AXI_R, s_AXI_AR)), s_AXI_R),
        s_IFU_DATA -> Mux(io.in.rready, s_IFU_AR, s_IFU_DATA),
        s_FENCEI -> Mux(fencei_cnt === (set-1).U, s_IFU_AR, s_FENCEI)
    ))
    state := next_state

    when(state === s_FENCEI){
        when(fencei_cnt === (set-1).U){
            fencei_cnt := 0.U
        }.otherwise{
            fencei_cnt := fencei_cnt + 1.U
        }
    }.otherwise{
        fencei_cnt := 0.U
    }

    io.fencei.ready := fencei_cnt === (set-1).U && state === s_FENCEI


    //decode
    tagA := Mux(io.in.arvalid, io.in.araddr(31, m + n), in_addr(31, m + n))
    index := Mux(io.in.arvalid, io.in.araddr(m + n - 1, m), in_addr(m + n - 1, m))
    offset := Mux(io.in.arvalid, io.in.araddr(m - 1, 0), in_addr(m - 1, 0))
    base_addr := Mux(io.in.arvalid, io.in.araddr, in_addr) - offset

    hit_data := 0.U
    way_hit := 0.U
    hit := false.B
    Cache_Set := icache(index)
    
    for(i <- 0 until way){
        val temp_cachedata = Wire(new ICache_Block(tag_size, block_size))
        val temp_valid = Wire(Bool())
        val temp_tag = Wire(UInt(tag_size.W))
        temp_cachedata := Cache_Set.set(i)
        temp_valid := temp_cachedata.valid
        temp_tag := temp_cachedata.tag
        
        when(temp_valid && temp_tag === tagA){
            hit := Mux(io.in.arvalid, true.B, false.B)
            way_hit := i.U
            hit_data := temp_cachedata.data(offset >> 2)
        }
    }

    io.in.arready := false.B
    io.in.rdata := 0.U
    io.in.rvalid := false.B
    io.in.rresp := io.out.rresp

    io.out.araddr  := 0.U
    io.out.arvalid := false.B
    io.out.arid    := 0.U
    io.out.arlen   := 0.U
    io.out.arsize  := 0.U
    io.out.arburst := 0.U
    io.out.rready  := false.B
    io.out.awaddr  := 0.U
    io.out.awvalid := false.B
    io.out.awid    := 0.U
    io.out.awlen   := 0.U
    io.out.awsize  := 0.U
    io.out.awburst := 0.U
    io.out.wdata   := 0.U
    io.out.wstrb   := 0.U
    io.out.wvalid  := false.B
    io.out.wlast   := false.B
    io.out.bready  := false.B

    val addr = Mux(is_sdram, base_addr, ((c.U - 1.U - count) << 2) + base_addr)
    io.in.rdata := Mux(hit & state =/= s_AXI_R, hit_data, Mux(count === 0.U, miss_data, 0.U))

    switch(state){
        is(s_IFU_AR){
            io.in.arready := !(io.fencei.valid && io.fencei.bits.is_fencei)
            io.in.rvalid := Mux(hit, true.B, false.B)

            io.out.araddr := 0.U
            io.out.arvalid := false.B
            io.out.rready := false.B
        }
        is(s_AXI_AR){
            io.in.arready := false.B
            io.in.rvalid := false.B

            io.out.araddr := addr
            io.out.arvalid := true.B
            io.out.arburst := "b01".U
            io.out.arlen := Mux(is_sdram, (c - 1).U, 0.U)
            io.out.arsize := "b10".U
            io.out.rready := false.B

        }
        is(s_AXI_R){
            io.in.arready := false.B
            io.in.rvalid := Mux(count === 0.U & io.out.rvalid, true.B, false.B)

            io.out.araddr := 0.U
            io.out.arvalid := false.B
            io.out.rready := true.B
        }
        is(s_IFU_DATA){
            io.in.arready := false.B
            io.in.rvalid := true.B
            io.in.rdata := rdata

            io.out.araddr := 0.U
            io.out.arvalid := false.B
            io.out.rready := false.B
        }
        is(s_FENCEI){
            io.in.arready := false.B
            io.in.rvalid := false.B

            io.out.araddr := 0.U
            io.out.arvalid := false.B
            io.out.rready := false.B

            icache(fencei_cnt) := 0.U.asTypeOf(new ICache_Set(tag_size, block_size, way))
        }
    }

    //miss refill
    miss_data := 0.U
    val miss_way = Wire(UInt(w.W))
    miss_way := fifo_ptr

    val fill_idx_width = log2Ceil(way).max(1)
    when(io.out.rvalid){
        val new_block = Wire(new ICache_Block(tag_size, block_size))
        new_block.valid := true.B
        new_block.tag := tagA
        new_block.data := Cache_Set.set(miss_way).data
        new_block.data((c.U - (count + 1.U))(log2Ceil(c).max(1) - 1, 0)) := io.out.rdata
        miss_data := new_block.data(offset >> 2).asUInt

        val new_Cache_Set = Wire(Cache_Set.set.cloneType)
        new_Cache_Set := Cache_Set.set
        new_Cache_Set(miss_way) := new_block
        
        when(count === 0.U){
            fifo_ptr := fifo_ptr + 1.U
        }

        icache(index).set := new_Cache_Set
    }

    when(state === s_IFU_AR){
        count := (c - 1).U
    }.elsewhen(count =/= 0.U && io.out.rvalid){
        count := count - 1.U
    }


    if (conf.statistics) {
      PM(conf, clock, EVENT_ICACHE_MISS, 1.U, state === s_IFU_AR && io.in.arvalid && io.in.arready && !hit)
    }
}
