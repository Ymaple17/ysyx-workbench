package unit

import chisel3._
import chisel3.util._
import common.IMM_TYPE._
import common.BPU_Config._
import unit.IMM
import core.CoreConfig

class BPU_IO extends Bundle {
    val predict_pc = Input(UInt(32.W)) //当前取指PC
    val predict_inst = Input(UInt(32.W)) //当前取指指令
    
    val bp_valid = Output(Bool()) //是否为可预测分支指令 
    val bp_taken = Output(Bool()) //分支是否跳转
    val bp_target = Output(UInt(32.W)) //预测目标
    val bp_index = Output(UInt(log2Ceil(BHT_SIZE).W)) //预测时算好的 BHT 索引（随指令流传递，更新时使用）

    val update_pc = Input(UInt(32.W)) //更新PC
    val update_valid = Input(Bool()) //是否更新
    val update_taken = Input(Bool()) //分支是否被采取
    val update_is_branch = Input(Bool()) //被解析的指令是否为条件分支（只有分支才更新BHT）
    val update_index = Input(UInt(log2Ceil(BHT_SIZE).W)) //预测时算好的索引

    val update_is_call = Input(Bool()) // jal ra / jalr ra（非 ret）
    val update_is_ret = Input(Bool())  // jalr x*, 0(ra)
}

class BPU(val conf: CoreConfig) extends Module {
    val io = IO(new BPU_IO)

    val inst = io.predict_inst
    val opcode = inst(6, 0)
    
    val is_jal = opcode === "b1101111".U(7.W) //JAL
    val is_jalr = opcode === "b1100111".U(7.W) //JALR
    val is_branch = opcode === "b1100011".U(7.W) //条件分支

    val imm = Module(new IMM)
    imm.io.inst := inst
    imm.io.imm_type := Mux(is_jal, ImmJ, ImmB)

    //2-bits饱和计数器
    val bht = RegInit(VecInit(Seq.fill(BHT_SIZE)(BHT_INIT.U(2.W)))) //2-bits饱和计数器
    val bht_valid = RegInit(VecInit(Seq.fill(BHT_SIZE)(false.B)))
    val ghr = RegInit(0.U(log2Ceil(BHT_SIZE).W)) //全局历史（与 BHT 索引同宽）
    // GShare 索引 = PC[10:2] ^ ghr
    // 关键：更新必须用"预测时的同一个索引"（update_index 随指令流传递），
    // 否则 EXU 更新时 ghr 已被中间的分支改掉，写错条目
    val bht_index = io.predict_pc(log2Ceil(BHT_SIZE) + 1, 2) ^ ghr
    val bht_value = bht(bht_index) //BHT预测值
    val bht_trained = bht_valid(bht_index)
    val bht_counter_taken = bht_value(1) //预测是否跳转 0=不跳 1=跳
    val cold_static_taken = imm.io.imm_ext(31) // backward-taken / forward-not-taken
    val bht_predict_taken =
        if (BHT_COLD_STATIC) Mux(bht_trained, bht_counter_taken, cold_static_taken)
        else bht_counter_taken

    //更新BHT：直接用预测时传递的索引，不重新计算
    when(io.update_valid && io.update_is_branch) {
        val update_value = bht(io.update_index)
        bht(io.update_index) := Mux(io.update_taken, Mux(update_value === 3.U, 3.U, update_value + 1.U), Mux(update_value === 0.U, 0.U, update_value - 1.U))
        bht_valid(io.update_index) := true.B
        // 更新全局历史寄存器
        ghr := Cat(ghr(log2Ceil(BHT_SIZE) - 2, 0), io.update_taken)
    }

    //RAS
    // call：jal ra 或 jalr ra, rs1(≠ra)；ret：jalr x*, 0(ra)
    // jalr ra, 0(ra) 同时满足 call/ret 时 ret 优先（不 push）
    val rd_is_ra  = inst(11, 7) === 1.U
    val rs1_is_ra = inst(19, 15) === 1.U
    val is_call = rd_is_ra && (is_jal || (is_jalr && !rs1_is_ra))
    val is_ret = is_jalr && rs1_is_ra

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
    io.bp_valid := is_jal || is_branch || is_ret
    // ★ ret 的方向：栈非空才预测跳转；栈空时回退"不跳"（false），
    //   绝不能回退到 bht_predict_taken——BHT 值会泄漏给 ret（同 bp_taken 门控的坑）
    io.bp_taken := io.bp_valid && Mux(is_jal, true.B, Mux(is_ret, !ras_empty, bht_predict_taken))
    // ★ ret 的目标 = 栈顶。push 是"先写后移"（ras(ptr):=val; ptr+1），
    //   所以栈顶在 ras_ptr-1（读 ras(ras_ptr) 会读到未写的空槽 0，导致取指 0x0 死锁）
    val ras_top_idx = Mux(ras_ptr === 0.U, (RAS_SIZE - 1).U, ras_ptr - 1.U)
    io.bp_target := Mux(is_ret, ras(ras_top_idx), io.predict_pc + imm.io.imm_ext)
    io.bp_index := bht_index
    
}
