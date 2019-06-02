import java.util.UUID

import org.apache.kafka.streams.processor.{Processor, ProcessorContext}
import org.apache.kafka.streams.state.KeyValueStore

import scala.collection.JavaConverters._

/*
* https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
*/

/*
 TODO: Choosing of a useful r
 */
val r = 1

val a = 0.998

val lambda = 1

val steppingtime = 1

val decay = scala.math.pow(a, lambda * steppingtime)

class PointToClusterCellProcessor extends Processor[String, Point] {

  private var context: ProcessorContext = _
  private var kvStore: KeyValueStore[String, ClusterCell] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    kvStore = context.getStateStore("ClusterCellStore").asInstanceOf[KeyValueStore[String, ClusterCell]]
  }

  override def process(key: String, value: Point): Unit = {
    val closestCell = kvStore.all.asScala
      .map(cell => (cell, pointClusterCellDist(value, cell.value)))
      .minByOption(_._2)
      .filter(_._2 < r)
    if (closestCell.isEmpty) {
      // Create new cluster cell from point
      val newClusterCell = ClusterCell(
        value,
        1,
        kvStore.all.asScala
          .filter(_.value.timelyDensity > 1)
          .map(cell => pointClusterCellDist(value, cell.value))
          .minByOption(dist => dist))
        this.context.forward(UUID.randomUUID().toString,newClusterCell)
    }
    else {
      // merge found cluster cell with point
      val oldClusterCell = closestCell.get._1
      val timelyDensity = decay * oldClusterCell.value.timelyDensity + 1
      val seedPoint = oldClusterCell.value.seedPoint
      val mergedClusterCell = ClusterCell(
        seedPoint,
        timelyDensity,
        kvStore.all.asScala
          .filter(cell => cell.key != oldClusterCell.key && cell.value.timelyDensity > timelyDensity)
          .map(cell => pointClusterCellDist(seedPoint, cell.value))
          .minByOption(dist => dist))
      this.context.forward(oldClusterCell.key,mergedClusterCell)
    }
  }

  def pointClusterCellDist(point: Point, clusterCell: ClusterCell): Double = {
    val x = point.x - clusterCell.seedPoint.x
    val y = point.y - clusterCell.seedPoint.y
    scala.math.sqrt((x * x) + (y * y))
  }

  override def close(): Unit = {

  }
}