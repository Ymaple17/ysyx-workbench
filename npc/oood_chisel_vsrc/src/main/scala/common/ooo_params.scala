package common

import chisel3._
import chisel3.util._

object OoOParams {
  val CORE_WIDTH = 4
  val WIDE_RS_SIZE = 16
  val WIDE_CDB_NUM = 4
  val ROB_SIZE  = 32
  val N_PHYS    = 32 + ROB_SIZE
  val PHYS_W    = log2Ceil(N_PHYS)
  val ROB_PTR_W = log2Ceil(ROB_SIZE)
  val RS_SIZE   = 8
  val BRQ_SIZE  = 4
  val FQ_SIZE   = 16
  val FETCH_WIDTH = CORE_WIDTH
  val FETCH_BUFFER_SIZE = 4
  val FTQ_SIZE = 16
  val FTQ_PTR_W = log2Ceil(FTQ_SIZE)
  val FTQ_GEN_W = 8
  val WIDE_FETCH_ENABLE = true
  val ISSUE_WIDTH = CORE_WIDTH
  val DISPATCH_WIDTH = CORE_WIDTH
  val COMMIT_WIDTH = CORE_WIDTH
  val CDB_NUM = CORE_WIDTH
  val CP_DEPTH = 4
  val SQ_SIZE  = ROB_SIZE
  // Keep enough committed stores in flight to hide long AXI write bursts.
  val STORE_BUFFER_SIZE = 128
  val STORE_BUFFER_GATHER_CYCLES = 24
  val ICACHE_SET = 128
  val DCACHE_SET = 512
  val DCACHE_BLOCK_SIZE = 32
  val LSU_MLP_ENABLE = true
  val LQ_SIZE = 8
  val LQ_SPECULATE_UNKNOWN_STORES = true
  val FAST_DIV_ENABLE = true
}
