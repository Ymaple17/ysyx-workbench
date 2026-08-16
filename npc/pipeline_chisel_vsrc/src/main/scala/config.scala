package core

class CoreConfig (val xlen : Int){ 
  def useDPIC : Boolean = true
  def ysyxsoc : Boolean = true
  def npc     : Boolean = false
  def statistics : Boolean = true
}

object NPC_Config{
  def apply(): CoreConfig = {
    val xlen = 32
    val config = new CoreConfig(xlen)
    config
  }
}