import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

import etcd.EtcdManaged
import org.apache.kafka.streams.processor.{
  Processor,
  ProcessorContext,
  PunctuationType,
  Punctuator
}
import org.apache.kafka.streams.state.KeyValueStore
import types.cell.ClusterCell
import types.point.Point

import scala.Ordering.Double.TotalOrdering
import scala.jdk.CollectionConverters._

/*
 * https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
 */

/*
 * TODO: Choosing of a useful r
 */

class PointToClusterCellProcessor extends Processor[String, Point] {
  var r = 10

  var a = 0.898

  var lambda = 1

  var steppingtime = 1

  def decay = scala.math.pow(a, lambda * steppingtime)

  private var context: ProcessorContext = _
  private var clusterCells: KeyValueStore[String, ClusterCell] = _
  private var pointBuffer: KeyValueStore[String, Point] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context

    clusterCells = context
      .getStateStore("clustercell-buffer-store")
      .asInstanceOf[KeyValueStore[String, ClusterCell]]
    pointBuffer = context
      .getStateStore("point-buffer-store")
      .asInstanceOf[KeyValueStore[String, Point]]

    val duration = Duration.of(1, ChronoUnit.SECONDS)

    context.schedule(
      duration,
      PunctuationType.WALL_CLOCK_TIME,
      processPoints
    )

    val etcdClient = new EtcdManaged("http://msd-etcd:2379")

    etcdClient.watchWithCb("p2cc/radius", value => {
      r = value.toInt
    })

    etcdClient.watchWithCb("p2cc/decay", value => {
      a = value.toDouble
    })

    etcdClient.watchWithCb("p2cc/lambda", value => {
      lambda = value.toInt
    })
  }

  private def processPoints =
    (_ => {
      clusterCells.all.asScala.foreach(
        cell =>
          clusterCells.put(
            cell.key,
            ClusterCell(
              cell.value.seedPoint,
              cell.value.timelyDensity * decay,
              cell.value.dependentDistance,
              cell.value.dependentClusterCell
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
          val dependentCell = clusterCells.all.asScala
            .filter(
              otherCell =>
                otherCell.key != cell.key && otherCell.value.timelyDensity > cell.value.timelyDensity
            )
            .map(
              otherCell =>
                (
                  otherCell.key,
                  pointClusterCellDist(cell.value.seedPoint, otherCell.value)
              )
            )
            .minByOption(tuple => tuple._2)
          val updatedCell = ClusterCell(
            cell.value.seedPoint,
            cell.value.timelyDensity,
            dependentCell.map(_._2),
            dependentCell.map(_._1)
          )
          clusterCells.put(cell.key, updatedCell)
          context.forward(cell.key, updatedCell)
        }
      })
      context.commit()
    }): Punctuator

  private def processPoint(key: String, value: Point): Unit = {
    val closestCell = clusterCells.all.asScala
      .map(cell => (cell, pointClusterCellDist(value, cell.value)))
      .minByOption(_._2)
      .filter(_._2 < r)
    if (closestCell.isEmpty) {
      // Create new cluster cell from point
      val newClusterCell = ClusterCell(
        value,
        1,
        None,
        None
      )

      val newUuid = UUID.randomUUID.toString
      this.clusterCells.put(newUuid, newClusterCell)
    } else {
      // merge found cluster cell with point
      val oldClusterCell = closestCell.get._1
      val timelyDensity = oldClusterCell.value.timelyDensity + 1
      val seedPoint = oldClusterCell.value.seedPoint

      val mergedClusterCell = ClusterCell(
        seedPoint,
        timelyDensity,
        None,
        None
      )

      this.clusterCells.put(oldClusterCell.key, mergedClusterCell)
    }
    this.pointBuffer.delete(key)
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
