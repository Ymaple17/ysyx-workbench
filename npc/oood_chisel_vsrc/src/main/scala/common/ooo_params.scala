package common

import chisel3._
import chisel3.util._

object OoOParams {
  val ROB_SIZE  = 16
  val N_PHYS    = 32 + ROB_SIZE
  val PHYS_W    = log2Ceil(N_PHYS)
  val ROB_PTR_W = log2Ceil(ROB_SIZE)
  val RS_SIZE   = 8
  val FQ_SIZE   = 8
  val ISSUE_WIDTH = 2
  val DISPATCH_WIDTH = ISSUE_WIDTH
  val COMMIT_WIDTH = 1
  val CDB_NUM = 2
  val CP_DEPTH = 4
  val SQ_SIZE  = ROB_SIZE
  val STORE_BUFFER_SIZE = 4
  val DCACHE_SET = 64
  val DCACHE_BLOCK_SIZE = 8
}
