package core

import chisel3._
import chisel3.util._
import scala.math._
import bus._
import core.PM
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

class ICache(val set : Int, val way : Int, val block_size : Int, val conf: CoreConfig) extends Module{
    override def desiredName = "ysyx_25020039_ICache"

    val io = IO(new ICache_IO)

    val bus_width = 4
    val m = (math.log(block_size)/math.log(2)).toInt
    val n = (math.log(set)/math.log(2)).toInt
    val w = (math.log(way)/math.log(2)).toInt
    val c = block_size / 4
    val count = RegInit(c.U(4.W))
    val tag_size = 32 - m - n

    val icache = RegInit(0.U.asTypeOf(Vec(set, new ICache_Set(tag_size, block_size, way))))
    val base_addr = Wire(UInt(32.W))
    val tagA = Wire(UInt(tag_size.W))
    val index = Wire(UInt(n.W))
    val offset = Wire(UInt(m.W))

    val rdata = RegEnable(io.in.rdata, 0.U(32.W), io.in.rvalid)

    val Cache_Set = Wire(new ICache_Set(tag_size, block_size, way))
    val Cache_Data = Wire(new ICache_Block(tag_size, block_size))
 
    val way_hit = Wire(UInt(w.W))
    val valid = Wire(Bool())
    val tagC = Wire(UInt(tag_size.W))
    val hit_data = Wire(UInt((bus_width*8).W))
    val miss_data = Wire(UInt((bus_width*8).W))
    val hit = Wire(Bool())
    
    val fence_cnt = RegInit(0.U(n.W))
    val fifo_ptr = RegInit(0.U(w.W))
    
    val in_addr = RegEnable(io.in.araddr, 0.U(32.W), io.in.arvalid && io.in.arready)
    
    val is_sdram = RegEnable(io.in.araddr >= "ha000_0000".U(32.W) && io.in.araddr <= "hbfff_ffff".U(32.W), false.B, io.in.arready & io.in.arvalid)

    val s_IFU_ADDRESS :: s_AXI_AR :: s_AXI_R :: s_IFU_DATA :: s_FENCE :: Nil = Enum(5)
    val hit_nextstate = Mux(io.in.rready, s_IFU_ADDRESS, s_IFU_DATA)
    val miss_nextstate = Mux(io.out.arready, s_AXI_R, s_AXI_AR)

    val state = RegInit(s_IFU_ADDRESS)
    val next_state = WireDefault(state)
    

    next_state := MuxLookup(state, s_IFU_ADDRESS)(Seq(
        s_IFU_ADDRESS -> MuxCase(s_IFU_ADDRESS, 
                Seq((io.in.arvalid && io.in.arready) -> Mux(hit, hit_nextstate, miss_nextstate),
                      (io.fencei.valid && io.fencei.bits.is_fencei) -> s_FENCE)),
        s_AXI_AR -> Mux(io.out.arready, s_AXI_R, s_AXI_AR),
        s_AXI_R -> Mux(io.out.rvalid, Mux(count === 0.U, hit_nextstate, Mux(is_sdram, s_AXI_R, s_AXI_AR)), s_AXI_R),
        s_IFU_DATA -> Mux(io.in.rready, s_IFU_ADDRESS, s_IFU_DATA),
        s_FENCE -> Mux(fence_cnt === (set-1).U, s_IFU_ADDRESS, s_FENCE)
    ))
    state := next_state

    // fence
    when(state === s_FENCE){
        when(fence_cnt === (set - 1).U) {
            fence_cnt := 0.U
        } .otherwise {
            fence_cnt := fence_cnt + 1.U
        }
    } .otherwise{
        fence_cnt := 0.U
    }
    io.fencei.ready := fence_cnt === (set-1).U
    when(io.fencei.valid & io.fencei.bits.is_fencei){
        icache(fence_cnt) := 0.U.asTypeOf(new ICache_Set(tag_size, block_size, way))
    }

    // decode
    tagA := Mux(io.in.arvalid, io.in.araddr(31,m+n), in_addr(31,m+n))
    index := Mux(io.in.arvalid, io.in.araddr(m+n-1,m), in_addr(m+n-1,m))
    offset := Mux(io.in.arvalid, io.in.araddr(m-1,0), in_addr(m-1,0))
    base_addr := Mux(io.in.arvalid,io.in.araddr,in_addr) - offset

    Cache_Data := 0.U.asTypeOf(new ICache_Block(tag_size, block_size))
    hit_data := 0.U
    way_hit := 0.U
    hit := false.B
    valid := false.B
    tagC := 0.U
    Cache_Set := icache(index)
    for(i<-0 until way){
        val temp_cachedata = Wire(new ICache_Block(tag_size, block_size))
        val temp_valid = Wire(Bool())
        val temp_tagC = Wire(UInt(tag_size.W))
        temp_cachedata := Cache_Set.set(i)
        temp_valid := temp_cachedata.valid
        temp_tagC := temp_cachedata.tag

        when(temp_valid && (temp_tagC === tagA)){
            valid := true.B 
            tagC := temp_tagC
            hit := Mux(io.in.arvalid, true.B, false.B)
            way_hit := i.U
            hit_data := temp_cachedata.data(offset >>2 )
        }
    }

    io.in.arready := false.B
    io.in.rdata   := 0.U
    io.in.rvalid  := false.B

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

    val addr = Mux(is_sdram, base_addr, ((c.U-1.U-(count)) << 2) + base_addr)
    io.in.rresp := io.out.rresp
    io.in.rdata := Mux(hit & state =/= s_AXI_R, hit_data,Mux(count === 0.U, miss_data, 0.U))

    switch(state){
        is(s_IFU_ADDRESS){
            io.in.arready := !(io.fencei.valid && io.fencei.bits.is_fencei)
            io.in.rvalid  := Mux(hit, true.B, false.B)

            io.out.arvalid := false.B
            io.out.rready  := false.B
            io.out.araddr  := 0.U
        }
        is(s_AXI_AR){
            io.in.arready := false.B
            io.in.rvalid  := false.B

            io.out.arvalid := true.B
            io.out.rready  := false.B
            io.out.araddr  := addr
            io.out.arburst := "b01".U
            io.out.arlen   := Mux(is_sdram, (c - 1).U, 0.U)
            io.out.arsize  := "b10".U
        }
        is(s_AXI_R){
            io.in.arready := false.B
            io.in.rvalid  := Mux(count === 0.U & io.out.rvalid, true.B, false.B)

            io.out.arvalid := false.B
            io.out.rready  := true.B
            io.out.araddr  := 0.U
        }
        is(s_IFU_DATA){
            io.in.arready := false.B
            io.in.rvalid  := true.B
            io.in.rdata   := rdata

            io.out.arvalid := false.B
            io.out.rready  := false.B
            io.out.araddr  := 0.U
        }
    }

    // miss
    miss_data := 0.U
    val miss_way = Wire(UInt(w.W))
    miss_way := fifo_ptr

    Cache_Data := Cache_Set.set(miss_way)
    when(io.out.rvalid) {
        val new_block = Wire(new ICache_Block(tag_size, block_size)) 
        new_block.valid := true.B
        new_block.tag := tagA
        new_block.data := Cache_Set.set(miss_way).data
        new_block.data((c.U - (count + 1.U))(log2Ceil(c).max(1) - 1, 0)) := io.out.rdata    
        miss_data := new_block.data(offset >> 2).asUInt

        val new_Cache_Set = Wire(Cache_Set.set.cloneType)
        new_Cache_Set := Cache_Set.set
        new_Cache_Set(miss_way) := new_block
        when(io.out.rvalid & count === 0.U){
            fifo_ptr := fifo_ptr + 1.U
        }
        icache(index).set := new_Cache_Set
    }
    when(state === s_IFU_ADDRESS){
        count := (c-1).U
    }.elsewhen(count =/= 0.U && io.out.rvalid){
        count := count - 1.U
    }

    if(conf.statistics){
      PM(conf, clock, EVENT_ICACHE_MISS, 1.U, state === s_IFU_ADDRESS && io.in.arvalid && io.in.arready && !hit)
    }
}