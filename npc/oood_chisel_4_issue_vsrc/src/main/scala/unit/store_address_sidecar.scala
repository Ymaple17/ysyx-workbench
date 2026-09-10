package unit

import chisel3._
import chisel3.util._
import common.OoOParams

class StoreAddressResult extends Bundle {
  val rob_idx = UInt(OoOParams.ROB_PTR_W.W)
  val pc = UInt(32.W)
  val addr = UInt(32.W)
  val mask = UInt(4.W)
}

/** One-cycle Store address pipe independent of the Load AGUs. */
class StoreAddressSidecar extends Module {
  val io = IO(new Bundle {
    val issue = Input(Valid(new RSEntry))
    val result = Output(Valid(new StoreAddressResult))
  })

  val resultValid = RegNext(io.issue.valid, false.B)
  val resultBits = RegInit(0.U.asTypeOf(new StoreAddressResult))
  when(io.issue.valid) {
    resultBits.rob_idx := io.issue.bits.rob_idx
    resultBits.pc := io.issue.bits.pc
    resultBits.addr := io.issue.bits.src1_val + io.issue.bits.imm_ext
    resultBits.mask := io.issue.bits.lsu_mem_wmask(3, 0)
  }

  io.result.valid := resultValid
  io.result.bits := resultBits

  when(!reset.asBool && io.issue.valid) {
    assert(io.issue.bits.lsu_mem_valid && io.issue.bits.lsu_mem_write,
      "StoreAddressSidecar accepts only Store uops")
    assert(io.issue.bits.src1_ready && !io.issue.bits.src2_ready,
      "StoreAddressSidecar is reserved for address-ready/data-wait Stores")
  }
}
