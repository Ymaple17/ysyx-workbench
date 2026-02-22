package core

class CoreConfig (val xlen : Int){ 
  def useDPIC : Boolean = true
  def ysyxsoc : Boolean = false
  def npc     : Boolean = true
  def statistics : Boolean = false
}

object NPC_Config{
  def apply(): CoreConfig = {
    val xlen = 32
    val config = new CoreConfig(xlen)
    config
  }
}