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

import types.cell.ClusterCell

import scala.collection.mutable

case class TreeNodeCell(
  var clusterCell: ClusterCell,
  var dependentCell: Option[TreeNodeCell],
  key: String,
  var successors: mutable.Set[TreeNodeCell] = mutable.Set.empty
) extends Ordered[TreeNodeCell] {
  override def compare(that: TreeNodeCell): Int = {
    this.clusterCell.timelyDensity.compareTo(that.clusterCell.timelyDensity)
  }

  def dist(that: TreeNodeCell): Double = {
    math.sqrt(
      math.pow(this.clusterCell.seedPoint.x - that.clusterCell.seedPoint.x, 2) + math
        .pow(this.clusterCell.seedPoint.y - that.clusterCell.seedPoint.y, 2)
    )
  }
}

class ClusterCellToClusteringProcessor
    extends Processor[String, Option[ClusterCell]] {

  var xi = 0

  var tau = 10

  private var context: ProcessorContext = _
  private val cellNodes: mutable.SortedSet[TreeNodeCell] = mutable.SortedSet()

  override def init(context: ProcessorContext): Unit = {
    this.context = context

    val etcdClient = new EtcdManaged("http://msd-etcd:2379")

    etcdClient.watchWithCb("cc2c/xi", value => {
      xi = value.toInt
    })

    etcdClient.watchWithCb("cc2c/tau", value => {
      tau = value.toInt
    })

    val duration = Duration.of(1, ChronoUnit.SECONDS)

    context.schedule(
      duration,
      PunctuationType.WALL_CLOCK_TIME,
      buildClusters
    )
  }

  private def buildClusters =
    (_ => {
      val root = cellNodes.find(_.dependentCell.isEmpty)
      val clusters =
        mutable.LinkedHashSet.empty[mutable.LinkedHashSet[ClusterCell]]
      val rootCluster = mutable.LinkedHashSet.empty[ClusterCell]
      clusters += rootCluster
      if (root.isDefined) {
        recAddToCluster(
          root.get,
          rootCluster,
          clusters
        )
      }
      context.forward(UUID.randomUUID.toString, clusters)
      context.commit()
    }): Punctuator

  private def recAddToCluster(
    node: TreeNodeCell,
    cluster: mutable.Set[ClusterCell],
    clusters: mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]
  ): Unit = {
    cluster += node.clusterCell
    if (node.successors.nonEmpty) {
      node.successors.foreach(succ => {
        if (node.clusterCell.timelyDensity > xi) {
          if (node.dist(succ) > tau) {
            val newCluster = mutable.LinkedHashSet.empty[ClusterCell]
            clusters += newCluster
            recAddToCluster(succ, newCluster, clusters)
          } else {
            recAddToCluster(succ, cluster, clusters)
          }
        }
      })
    }

  }

  override def process(key: String, value: Option[ClusterCell]): Unit = {
    var cellNode = cellNodes.find(p => p.key == key)
    if (value.isDefined) {
      if (cellNode.isDefined) {
        cellNode.get.clusterCell = value.get
        val dep = cellNode.get.dependentCell
        if (dep.isDefined) {
          dep.get.successors -= cellNode.get
          cellNode.get.dependentCell = None
        }
      } else {
        cellNode = Some(TreeNodeCell(value.get, None, key))
        cellNodes += cellNode.get
      }

      val (left, right) =
        cellNodes.filter(_.key != key).partition(node => node < cellNode.get)
      val newDep = left.unsorted
        .map(node => (node.dist(cellNode.get), node))
        .minByOption(_._1)
      if (newDep.isDefined) {
        cellNode.get.dependentCell = Some(newDep.get._2)
        newDep.get._2.successors += cellNode.get
      }
      val newSucc = right.unsorted
        .map(node => (node.dist(cellNode.get), node))
        .minByOption(_._1)
      if (newSucc.isDefined) {
        if (newSucc.get._2.dependentCell.isDefined) {
          newSucc.get._2.dependentCell.get.successors -= newSucc.get._2
        }
        newSucc.get._2.dependentCell = cellNode
        cellNode.get.successors += newSucc.get._2
      }
    } else {
      if (cellNode.isDefined && cellNode.get.dependentCell.isDefined) {
        if (cellNode.get.successors.nonEmpty) {
          cellNode.get.successors
            .foreach(cellNode.get.dependentCell.get.successors += _)
        }
        cellNode.get.dependentCell.get.successors -= cellNode.get
        cellNodes -= cellNode.get
      }
    }

    val head = cellNodes.head
    if (head.dependentCell.isDefined) {
      print("called")
      head.dependentCell.get.successors -= head
      head.dependentCell = None
    }
  }

  override def close(): Unit = {}
}
