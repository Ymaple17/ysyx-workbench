package core

class CoreConfig(val xlen: Int,
    val useDPIC: Boolean = true,
    val ysyxsoc: Boolean = false,
    val npc: Boolean = true,
    val statistics: Boolean = false,
    val resetPc: BigInt = -1) {
  val resetVector: BigInt =
    if (resetPc >= 0) resetPc
    else if (ysyxsoc) 0x30000000L
    else if (npc) 0x80000000L
    else 0L
}

object NPC_Config{
  def apply(): CoreConfig = new CoreConfig(32, statistics = true)
}

object SoC_Config{
  def apply(): CoreConfig = new CoreConfig(32, ysyxsoc = true, npc = false, statistics = true)
}

object FPGA_Config{
  def apply(): CoreConfig = new CoreConfig(
    32,
    useDPIC = false,
    ysyxsoc = true,
    npc = false,
    statistics = false,
    resetPc = 0x80000000L)
}
