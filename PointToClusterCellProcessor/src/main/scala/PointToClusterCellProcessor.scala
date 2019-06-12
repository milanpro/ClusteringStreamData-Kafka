import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

import org.apache.kafka.streams.processor.{
  Processor,
  ProcessorContext,
  PunctuationType,
  Punctuator
}
import org.apache.kafka.streams.state.KeyValueStore
import types.cell.ClusterCell
import types.point.Point

import scala.jdk.CollectionConverters._
import Ordering.Double.TotalOrdering

/*
 * https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
 */

/*
 * TODO: Choosing of a useful r
 */

class PointToClusterCellProcessor extends Processor[String, Point] {
  val r = 1

  val a = 0.998

  val lambda = 1

  val steppingtime = 1

  val decay = scala.math.pow(a, lambda * steppingtime)

  private var context: ProcessorContext = _
  private var clusterCells: KeyValueStore[String, ClusterCell] = _
  private var pointBuffer: KeyValueStore[String, Point] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    clusterCells = context
      .getStateStore("cluster-buffer-store")
      .asInstanceOf[KeyValueStore[String, ClusterCell]]
    pointBuffer = context
      .getStateStore("point-buffer-store")
      .asInstanceOf[KeyValueStore[String, Point]]

    val duration = Duration.of(1, ChronoUnit.SECONDS)

    context.schedule(
      duration,
      PunctuationType.WALL_CLOCK_TIME,
      (_ => {

        clusterCells.all.asScala.foreach(
          cell =>
            clusterCells.put(
              cell.key,
              ClusterCell(
                cell.value.seedPoint,
                cell.value.timelyDensity * decay,
                cell.value.dependentDistance
              )
          )
        )
        val points = pointBuffer.all.asScala
        points.foreach(point => processPoint(point.key, point.value))
        clusterCells.all.asScala.foreach(cell => {
          if (cell.value.timelyDensity < 0.8) {
            clusterCells.delete(cell.key)
            context.forward(cell.key, null)
          } else {
            val updatedCell = ClusterCell(
              cell.value.seedPoint,
              cell.value.timelyDensity,
              clusterCells.all.asScala
                .filter(
                  otherCell =>
                    otherCell.key != cell.key && otherCell.value.timelyDensity > cell.value.timelyDensity
                )
                .map(
                  otherCell =>
                    pointClusterCellDist(cell.value.seedPoint, otherCell.value)
                )
                .minByOption(dist => dist)
            )
            clusterCells.put(cell.key, updatedCell)
            context.forward(cell.key, updatedCell)
          }

        })
        context.commit()
      }): Punctuator
    )
  }

  def processPoint(key: String, value: Point): Unit = {
    val closestCell = clusterCells.all.asScala
      .map(cell => (cell, pointClusterCellDist(value, cell.value)))
      .minByOption(_._2)
      .filter(_._2 < r)
    if (closestCell.isEmpty) {
      // Create new cluster cell from point
      val newClusterCell = ClusterCell(
        value,
        1,
        None
      )

      val newuuid = UUID.randomUUID.toString
      this.clusterCells.put(newuuid, newClusterCell)
    } else {
      // merge found cluster cell with point
      val oldClusterCell = closestCell.get._1
      val timelyDensity = oldClusterCell.value.timelyDensity + 1
      val seedPoint = oldClusterCell.value.seedPoint

      val mergedClusterCell = ClusterCell(
        seedPoint,
        timelyDensity,
        None
      )

      this.clusterCells.put(oldClusterCell.key, mergedClusterCell)
    }
  }

  def pointClusterCellDist(point: Point, clusterCell: ClusterCell): Double = {
    val x = point.x - clusterCell.seedPoint.x
    val y = point.y - clusterCell.seedPoint.y
    scala.math.sqrt((x * x) + (y * y))
  }

  override def process(key: String, value: Point): Unit = {
    pointBuffer.put(key, value)
  }

  override def close(): Unit = {}
}