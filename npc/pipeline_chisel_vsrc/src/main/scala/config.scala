package core

class CoreConfig(val xlen: Int) {
  def useDPIC: Boolean = sys.env.getOrElse("USE_DPIC", "true") == "true"
  def ysyxsoc: Boolean = sys.env.getOrElse("YSYXSOC", "true") == "true"
  def npc: Boolean     = sys.env.getOrElse("NPC", "false") == "true"
  def statistics: Boolean = false
}

object NPC_Config {
  def apply(): CoreConfig = {
    val xlen = 32
    val config = new CoreConfig(xlen)
    config
  }
}