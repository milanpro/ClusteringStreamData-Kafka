package types.cluster

import types.cell.ClusterCell

class Cluster extends Serializable {
  private val cells = collection.mutable.Map[String, ClusterCell]()

  def containsCell(key: String): Boolean = {
    cells.contains(key)
  }

  def addCell(key: String, cell: ClusterCell): Unit = {
    cells.put(key, cell)
  }

  def removeCell(key: String): Boolean = {
    cells.remove(key).nonEmpty
  }

  def size: Int = {
    cells.size
  }

  def getCells: collection.mutable.Map[String, ClusterCell] = {
    cells
  }

  def getCell(key: String): Option[ClusterCell] = {
    cells.get(key)
  }
}
