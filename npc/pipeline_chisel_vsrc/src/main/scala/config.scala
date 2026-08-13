package core

class CoreConfig(val xlen: Int,
    val useDPIC: Boolean = sys.env.getOrElse("USE_DPIC", "true") == "true",
    val ysyxsoc: Boolean = false,
    val npc: Boolean = true,
    val statistics: Boolean = false,
    val axiMonitor: Boolean = false)

object NPC_Config{
  def apply(): CoreConfig = new CoreConfig(32, statistics = true)
}

object SoC_Config{
  def apply(): CoreConfig = new CoreConfig(32, ysyxsoc = true, npc = false)
}
