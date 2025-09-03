error id: file://<WORKSPACE>/chisel_vsrc/src/main/scala/npc.scala:`<none>`.
file://<WORKSPACE>/chisel_vsrc/src/main/scala/npc.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/idu.
	 -chisel3/util/idu.
	 -common/Consts.idu.
	 -common/Inst.idu.
	 -idu.
	 -scala/Predef.idu.
offset: 2374
uri: file://<WORKSPACE>/chisel_vsrc/src/main/scala/npc.scala
text:
```scala
import chisel3._
import chisel3.util._
import common.Consts._
import common.Inst._

class NPC extends Module {
  val io = IO(new Bundle {
    val imem_addr = Output(UInt(WORD_LEN.W))
    val imem_data = Input(UInt(WORD_LEN.W))
    
    val dmem_addr = Output(UInt(WORD_LEN.W))
    val dmem_wen = Output(Bool())
    val dmem_ren = Output(Bool())
    val dmem_en = Output(Bool())
    val dmem_wdata = Output(UInt(WORD_LEN.W))
    val dmem_wmask = Output(UInt(WMASK_LEN))
    val dmem_rdata = Input(UInt(WORD_LEN.W))
    
    val exit = Output(Bool())
    val debug_pc = Output(UInt(WORD_LEN.W))
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val wbu = Module(new WBU)

  val arch = "single" // 将其改为 "single" 即可切换
  val is_multi_cycle = arch == "multi"
  
  // 阶段连接
  StageConnect(ifu.io.out, idu.io.in, arch)
  StageConnect(idu.io.out, exu.io.in, arch)
  StageConnect(exu.io.out, wbu.io.in, arch)

  val regfile = Mem(32, UInt(WORD_LEN.W))
  
  idu.io.rf_rdata1 := Mux(idu.io.rf_raddr1 === 0.U, 0.U, regfile(idu.io.rf_raddr1))
  idu.io.rf_rdata2 := Mux(idu.io.rf_raddr2 === 0.U, 0.U, regfile(idu.io.rf_raddr2))
  
  when(wbu.io.rf_wen) {
    regfile(wbu.io.rf_waddr) := wbu.io.rf_wdata
  }

  val csr_regfile = Mem(4096, UInt(WORD_LEN.W))
  
  wbu.io.csr_rdata := csr_regfile(wbu.io.csr_waddr)
  when(wbu.io.csr_wen) {
    csr_regfile(wbu.io.csr_waddr) := wbu.io.csr_wdata
  }

  ifu.io.imem_data := io.imem_data
  io.imem_addr := ifu.io.imem_addr
  
  ifu.io.br_flag := wbu.io.br_flag
  ifu.io.br_target := wbu.io.br_target
  ifu.io.jalr_flag := wbu.io.jalr_flag
  ifu.io.jalr_target := wbu.io.jalr_target
  ifu.io.ecall_flag := wbu.io.ecall_flag
  ifu.io.ecall_target := wbu.io.ecall_target
  
  val s_IF :: s_ID :: s_EX :: s_WB :: Nil = Enum(4)
  val state = RegInit(s_IF)

  // 握手信号和状态机逻辑
  when(is_multi_cycle.B) {
    ifu.io.out.ready := idu.io.in.ready
    idu.io.out.ready := exu.io.in.ready
    exu.io.out.ready := wbu.io.in.ready
    
    switch(state) {
      is(s_IF) {
        when(ifu.io.out.fire) { state := s_ID }
      }
      is(s_ID) {
        when(idu.io.out.fire) { state := s_EX }
      }
      is(s_EX) {
        when(exu.io.out.fire) { state := s_WB }
      }
      is(s_WB) {
        when(wbu.io.in.fire) { state := s_IF }
      }
    }
  } .otherwise {
    // 单周期模式下的握手
    ifu.io.out.ready := true.B
    @@idu.io.in.valid := true.B
    idu.io.out.ready := true.B
    exu.io.in.valid := true.B
    exu.io.out.ready := true.B
    wbu.io.in.valid := true.B
  }
  
  io.dmem_addr := exu.io.dmem_addr
  io.dmem_wen := exu.io.dmem_wen
  io.dmem_en := exu.io.dmem_en
  io.dmem_ren := exu.io.dmem_ren
  io.dmem_wdata := exu.io.dmem_wdata
  io.dmem_wmask := exu.io.dmem_wmask
  exu.io.dmem_rdata := io.dmem_rdata

  wbu.io.dmem_rdata := io.dmem_rdata

  io.debug_pc := ifu.io.imem_addr
  
  val current_inst = Wire(UInt(WORD_LEN.W))
  current_inst := io.imem_data
  io.exit := current_inst === EBREAK
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.