package common

import chisel3._
import chisel3.util._

object OoOParams {
  val ROB_SIZE  = 32
  val N_PHYS    = 32 + ROB_SIZE
  val PHYS_W    = log2Ceil(N_PHYS)
  val ROB_PTR_W = log2Ceil(ROB_SIZE)
  val RS_SIZE   = 8
  val FQ_SIZE   = 8
  val FETCH_WIDTH = 2
  val FETCH_BUFFER_SIZE = 2
  val FTQ_SIZE = 16
  val FTQ_PTR_W = log2Ceil(FTQ_SIZE)
  val FTQ_GEN_W = 8
  val WIDE_FETCH_ENABLE = true
  val WIDE_FETCH_MIN_SPACE = 6
  val ISSUE_WIDTH = 2
  val DISPATCH_WIDTH = ISSUE_WIDTH
  val COMMIT_WIDTH = 2
  val CDB_NUM = 2
  val CP_DEPTH = 4
  val SQ_SIZE  = ROB_SIZE
  val STORE_BUFFER_SIZE = 16
  val DCACHE_SET = 64
  val DCACHE_BLOCK_SIZE = 8
  val LSU_MLP_ENABLE = true
  val LQ_SIZE = 4
  val LQ_SPECULATE_UNKNOWN_STORES = false
}
