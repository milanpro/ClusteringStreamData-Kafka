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
import types.cluster.{Cluster, Clusters}

import scala.collection.mutable

class TreeNodeCell(
  var clusterCell: Option[ClusterCell],
  var dependentCell: Option[TreeNodeCell],
  var key: String,
  var successors: mutable.Set[TreeNodeCell] = mutable.HashSet.empty
) {
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

  val etcdClient = new EtcdManaged("http://msd-etcd:2379")

  private val cellNodes: mutable.HashSet[TreeNodeCell] = mutable.HashSet.empty
  var xi: Int = etcdClient.setValue("cc2c/xi", "0").toInt
  var tau: Int = etcdClient.setValue("cc2c/tau", "25").toInt
  var depth: Int = 0
  private var context: ProcessorContext = _
  private var clusterCellBuffer: mutable.Queue[(String, ClusterCell)] =
    mutable.Queue.empty

  override def init(context: ProcessorContext): Unit = {
    this.context = context

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
      val clusterCellEventQueue = clusterCellBuffer
      clusterCellBuffer = mutable.Queue.empty
      while (clusterCellEventQueue.nonEmpty) {
        val cellEvent = clusterCellEventQueue.dequeue()
        processClusterCell(cellEvent._1, cellEvent._2)
      }
      calcAndEmitClusters()
    }): Punctuator

  private def processClusterCell(key: String, value: ClusterCell): Unit = {
    var cellNode = cellNodes.find(p => p.key == key)
    if (value != null) {
      if (cellNode.isDefined) {
        cellNode.get.clusterCell = Some(value)
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
          } else if (value.dependentClusterCell.isDefined) {
            val createdDep = new TreeNodeCell(
              Option.empty,
              Option.empty,
              value.dependentClusterCell.get
            )
            createdDep.successors.add(cellNode.get)
            cellNode.get.dependentCell = Some(createdDep)
            cellNodes.add(createdDep)
          }
        }
      } else {
        var dep =
          cellNodes.find(p => p.key == value.dependentClusterCell.orNull)
        if (value.dependentClusterCell.isDefined && dep.isEmpty) {
          dep = Some(
            new TreeNodeCell(
              Option.empty,
              Option.empty,
              value.dependentClusterCell.get
            )
          )
          cellNodes.add(dep.get)
        }
        cellNode = Some(
          new TreeNodeCell(
            Some(value),
            dep,
            key
          )
        )
        if (dep.isDefined) {
          dep.get.successors.add(cellNode.get)
        }
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

  private def calcAndEmitClusters(): Unit = {
    val root = cellNodes
      .filter(_.clusterCell.isDefined)
      .maxByOption(_.clusterCell.get.timelyDensity)
    val clusters =
      mutable.ListBuffer.empty[mutable.ListBuffer[ClusterCell]]
    if (root.isDefined) {
      recAddToCluster(
        root.get,
        null,
        clusters
      )
    }
    val id = UUID.randomUUID.toString

    val output = new Clusters(
      clusters.map(list => new Cluster(list.toArray)).toArray
    )

    context.forward(id, output)
    context.commit()
  }

  override def process(key: String, value: ClusterCell): Unit = {
    clusterCellBuffer.enqueue((key, value))
  }

  override def close(): Unit = {}

  private def recAddToCluster(
    node: TreeNodeCell,
    cluster: mutable.ListBuffer[ClusterCell],
    clusters: mutable.ListBuffer[mutable.ListBuffer[ClusterCell]]
  ): Unit = {
    var successorCluster = cluster
    val cellIsDenseEnough = node.clusterCell.isDefined && node.clusterCell.get.timelyDensity > xi

    /**
      * Create cluster if necessary and add cluster-cell to cluster
      */
    if (cellIsDenseEnough) {
      val cellInNewCluster = node.clusterCell.get.dependentDistance.isDefined && node.clusterCell.get.dependentDistance.get > tau
      if (cellInNewCluster || node.clusterCell.get.dependentDistance.isEmpty || successorCluster == null) {
        successorCluster = mutable.ListBuffer.empty[ClusterCell]
        clusters.addOne(successorCluster)
      }
      successorCluster.addOne(node.clusterCell.get)
    }

    /**
      * Go recursively through successors of cluster-cell
      */
    if (node.successors.nonEmpty && (cellIsDenseEnough || node.clusterCell.isEmpty)) {
      node.successors.foreach(succ => {
        if (!successorCluster.contains(succ.clusterCell.get)) {
          recAddToCluster(succ, successorCluster, clusters)
        }
      })
    }
  }
}
