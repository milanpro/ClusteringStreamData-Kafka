import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Semaphore

import etcd.EtcdManaged
import org.apache.kafka.streams.processor.{
  Processor,
  ProcessorContext,
  PunctuationType,
  Punctuator
}
import types.cell.ClusterCell
import types.cluster.{Cluster, Clusters}

import scala.collection.mutable

class TreeNodeCell(
  var clusterCell: ClusterCell,
  var dependentCell: Option[TreeNodeCell],
  var key: String,
  var successors: mutable.Set[TreeNodeCell] = mutable.HashSet.empty
) {
  def dist(that: TreeNodeCell): Double = {
    math.sqrt(
      math.pow(this.clusterCell.seedPoint.x - that.clusterCell.seedPoint.x, 2) + math
        .pow(this.clusterCell.seedPoint.y - that.clusterCell.seedPoint.y, 2)
    )
  }

  override def hashCode(): Int = {
    this.key.hashCode
  }

  override def toString: String = {
    this.key
  }

  def print(): Unit = {
    print("", isTail = true)
  }

  def print(prefix: String, isTail: Boolean): Unit = {
    println(prefix + (if (isTail) "└── " else "├── ") + key)

    successors.foreach(
      _.print(prefix + (if (isTail) "    " else "│   "), isTail = false)
    )

    successors.lastOption.foreach(
      _.print(prefix + (if (isTail) "    " else "│   "), isTail = true)
    )
  }
}

class ClusterCellToClusteringProcessor extends Processor[String, ClusterCell] {

  var xi = 0

  var tau = 25

  var depth = 0

  private var context: ProcessorContext = _
  private val cellNodes: mutable.HashSet[TreeNodeCell] = mutable.HashSet.empty
  private var syncSemaphore = new Semaphore(1)

  override def init(context: ProcessorContext): Unit = {
    this.context = context

    val etcdClient = new EtcdManaged("http://msd-etcd:2379")

    etcdClient.watchWithCb("cc2c/xi", value => {
      xi = value.toInt
    })

    etcdClient.watchWithCb("cc2c/tau", value => {
      tau = value.toInt
    })

    val duration = Duration.of(5, ChronoUnit.SECONDS)

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
        mutable.ListBuffer.empty[mutable.ListBuffer[ClusterCell]]
      val rootCluster = mutable.ListBuffer.empty[ClusterCell]
      clusters += rootCluster
      if (root.isDefined) {
        recAddToCluster(
          root.get,
          rootCluster,
          clusters
        )
      }
      val id = UUID.randomUUID.toString

      val output = new Clusters(
        clusters.map(list => new Cluster(list.toArray)).toArray
      )

      context.forward(id, output)
      context.commit()
    }): Punctuator

  private def recAddToCluster(
    node: TreeNodeCell,
    cluster: mutable.ListBuffer[ClusterCell],
    clusters: mutable.ListBuffer[mutable.ListBuffer[ClusterCell]]
  ): Unit = {
    cluster += node.clusterCell
    if (node.successors.nonEmpty) {
      node.successors.foreach(succ => {
        if (node.clusterCell.timelyDensity > xi) {
          if (node.dist(succ) > tau) {
            val newCluster = mutable.ListBuffer.empty[ClusterCell]
            clusters += newCluster
            recAddToCluster(succ, newCluster, clusters)
          } else {
            if (!cluster.contains(succ.clusterCell)) {
              recAddToCluster(succ, cluster, clusters)
            }
          }
        }
      })
    }

  }

  override def process(key: String, value: ClusterCell): Unit = {
    var cellNode = cellNodes.find(p => p.key == key)
    if (value != null) {
      if (cellNode.isDefined) {
        cellNode.get.clusterCell = value
        val dep = cellNode.get.dependentCell
        if (dep.isDefined && dep.get.key != value.dependentClusterCell.orNull || dep.isEmpty && value.dependentClusterCell.isDefined) {
          if (dep.isDefined) {
            dep.get.successors.remove(cellNode.get)
            cellNode.get.dependentCell = None
          }
          val newDep =
            cellNodes.find(p => p.key == value.dependentClusterCell.orNull)
          if (newDep.isDefined) {
            newDep.get.successors.add(cellNode.get)
            cellNode.get.dependentCell = newDep
          }
        }
      } else {
        cellNode = Some(
          new TreeNodeCell(
            value,
            cellNodes.find(p => p.key == value.dependentClusterCell.orNull),
            key
          )
        )
        cellNodes.add(cellNode.get)
      }

    } else {
      if (cellNode.isDefined) {
        if (cellNode.get.dependentCell.isDefined) {
          cellNode.get.dependentCell.get.successors.remove(cellNode.get)
        }
        cellNodes.remove(cellNode.get)
      }
    }
  }

  override def close(): Unit = {}
}
