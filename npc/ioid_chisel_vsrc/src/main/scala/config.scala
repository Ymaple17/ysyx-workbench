package core

class CoreConfig(val xlen: Int,
    val useDPIC: Boolean = true,
    val ysyxsoc: Boolean = false,
    val npc: Boolean = true,
    val statistics: Boolean = false)

object NPC_Config{
  def apply(): CoreConfig = new CoreConfig(32, statistics = true)
}

object SoC_Config{
  def apply(): CoreConfig = new CoreConfig(32, ysyxsoc = true, npc = false, statistics = true)
}
