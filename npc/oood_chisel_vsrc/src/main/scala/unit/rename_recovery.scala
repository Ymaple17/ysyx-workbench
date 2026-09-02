package unit

import chisel3._
import chisel3.util._
import common.OoOParams

object RenameRecoveryMath {
  def keptCount(head: UInt, lastKept: UInt, empty: Bool): UInt = {
    val ptrW = log2Ceil(OoOParams.ROB_SIZE)
    val distance = (lastKept - head)(ptrW - 1, 0)
    Mux(empty, 0.U((ptrW + 1).W), distance +& 1.U)
  }
}
