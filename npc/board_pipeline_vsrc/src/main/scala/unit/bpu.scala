package unit

import chisel3._
import chisel3.util._
import common.BPU_Config._
import core.CoreConfig
import unit.FixedPeriodPredictor.MetaWidth

class BPU_IO extends Bundle {
    val predict_pc = Input(UInt(32.W)) //当前取指PC
    
    val bp_valid = Output(Bool()) //是否为可预测分支指令 
    val bp_taken = Output(Bool()) //分支是否跳转
    val bp_target = Output(UInt(32.W)) //预测目标
    val bp_meta = Output(UInt(MetaWidth.W))
    val init_done = Output(Bool())

    val update_pc = Input(UInt(32.W)) //更新PC
    val update_target = Input(UInt(32.W)) //解析后的真实控制流目标
    val update_valid = Input(Bool()) //是否更新
    val update_taken = Input(Bool()) //分支是否被采取
    val update_is_branch = Input(Bool()) //被解析的指令是否为条件分支（只有分支才更新BHT）
    val update_meta = Input(UInt(MetaWidth.W))

    val update_is_call = Input(Bool()) // jal ra / jalr ra（非 ret）
    val update_is_ret = Input(Bool())  // jalr x*, 0(ra)
}

class BPU(val conf: CoreConfig) extends Module {
    val io = IO(new BPU_IO)

    // Control-flow identity and targets are learned at EXU and looked up by PC.
    // This keeps instruction RAM data, decode, and immediate addition off the
    // IFU next-PC path. A cold BTB entry simply predicts sequential execution.
    private val btbEntries = 512
    private val btbIndexBits = log2Ceil(btbEntries)
    private val btbTagBits = 32 - btbIndexBits - 2
    val btbValid = RegInit(VecInit(Seq.fill(btbEntries)(false.B)))
    val btbTag = Mem(btbEntries, UInt(btbTagBits.W))
    val btbTarget = Mem(btbEntries, UInt(32.W))
    val btbIsBranch = Mem(btbEntries, Bool())
    val btbIsRet = Mem(btbEntries, Bool())

    val btbIndex = io.predict_pc(btbIndexBits + 1, 2)
    val btbLookupTag = io.predict_pc(31, btbIndexBits + 2)
    val btbUpdateIndex = io.update_pc(btbIndexBits + 1, 2)
    val btbUpdateTag = io.update_pc(31, btbIndexBits + 2)
    // Same-index entries with different tags are aliases, not a read/write
    // collision. Bypassing an alias would redirect an unrelated instruction
    // to the branch target being trained in this cycle.
    val btbUpdateCollision = io.update_valid && io.update_pc === io.predict_pc
    val btbHit = Mux(btbUpdateCollision, true.B,
      btbValid(btbIndex) && btbTag(btbIndex) === btbLookupTag)
    val predictedIsBranch = Mux(btbUpdateCollision,
      io.update_is_branch, btbIsBranch(btbIndex))
    val predictedIsRet = Mux(btbUpdateCollision,
      io.update_is_ret, btbIsRet(btbIndex))
    val predictedTarget = Mux(btbUpdateCollision,
      io.update_target, btbTarget(btbIndex))

    when(io.update_valid) {
        btbValid(btbUpdateIndex) := true.B
        btbTag.write(btbUpdateIndex, btbUpdateTag)
        btbTarget.write(btbUpdateIndex, io.update_target)
        btbIsBranch.write(btbUpdateIndex, io.update_is_branch)
        btbIsRet.write(btbUpdateIndex, io.update_is_ret)
    }

    val direction = Module(new FixedPeriodPredictor)
    direction.io.predictPc := io.predict_pc
    direction.io.updatePc := io.update_pc
    direction.io.updateValid := io.update_valid && io.update_is_branch
    direction.io.updateTaken := io.update_taken
    direction.io.updateMeta := io.update_meta
    io.init_done := direction.io.initDone

    //RAS
    // call：jal ra 或 jalr ra, rs1(≠ra)；ret：jalr x*, 0(ra)
    // jalr ra, 0(ra) 同时满足 call/ret 时 ret 优先（不 push）
    val ras = RegInit(VecInit(Seq.fill(RAS_SIZE)(0.U(32.W))))
    val ras_ptr = RegInit(0.U(log2Ceil(RAS_SIZE).W))
    // 有效条目计数：栈空时不预测 ret（初始栈顶 0 是非法地址，取了会死锁）
    val ras_cnt = RegInit(0.U((log2Ceil(RAS_SIZE + 1)).W))
    val ras_empty = ras_cnt === 0.U

    when(io.update_valid && io.update_is_call) {
        ras(ras_ptr) := io.update_pc + 4.U
        ras_ptr := Mux(ras_ptr === (RAS_SIZE - 1).U, 0.U, ras_ptr + 1.U)
        ras_cnt := Mux(ras_cnt === RAS_SIZE.U, ras_cnt, ras_cnt + 1.U)
    }
    when(io.update_valid && io.update_is_ret) {
        ras_ptr := Mux(ras_ptr === 0.U, (RAS_SIZE - 1).U, ras_ptr - 1.U)
        ras_cnt := Mux(ras_cnt === 0.U, 0.U, ras_cnt - 1.U)
    }


    //预测逻辑
    io.bp_valid := btbHit
    // Return direction is valid only with a non-empty RAS. Other unconditional
    // BTB entries are always taken; conditional branches use local history.
    io.bp_taken := btbHit && Mux(predictedIsBranch, direction.io.predictTaken,
      Mux(predictedIsRet, !ras_empty, true.B))
    // ★ ret 的目标 = 栈顶。push 是"先写后移"（ras(ptr):=val; ptr+1），
    //   所以栈顶在 ras_ptr-1（读 ras(ras_ptr) 会读到未写的空槽 0，导致取指 0x0 死锁）
    val ras_top_idx = Mux(ras_ptr === 0.U, (RAS_SIZE - 1).U, ras_ptr - 1.U)
    val ras_top = Mux1H((0 until RAS_SIZE).map(i => (ras_top_idx === i.U) -> ras(i)))
    io.bp_target := Mux(predictedIsRet, ras_top, predictedTarget)
    io.bp_meta := direction.io.predictMeta
    
}
