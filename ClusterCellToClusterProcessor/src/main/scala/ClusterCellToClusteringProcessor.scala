import java.util.UUID

import etcd.EtcdManaged
import org.apache.kafka.streams.processor.{Processor, ProcessorContext}
import org.apache.kafka.streams.state.KeyValueStore
import types.cell.ClusterCell
import types.cluster.Cluster

import scala.jdk.CollectionConverters._

/*
 * https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
 */

class ClusterCellToClusteringProcessor extends Processor[String, ClusterCell] {

  var xi = 0

  var tau = 10

  private var context: ProcessorContext = _
  private var clusters: KeyValueStore[String, Cluster] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context

    clusters = context
      .getStateStore("cluster-buffer-store")
      .asInstanceOf[KeyValueStore[String, Cluster]]

    val etcdClient = new EtcdManaged("http://msd-etcd:2379")

    etcdClient.watchWithCb("cc2c/xi", value => {
      xi = value.toInt
    })

    etcdClient.watchWithCb("cc2c/tau", value => {
      tau = value.toInt
    })
  }

  override def process(key: String, value: ClusterCell): Unit = {
    val oldClusters = clusters.all.asScala

    val oldCluster =
      oldClusters.find(cluster => cluster.value.containsCell(key))

    /**
      * If the dependent distance is lower than xi, delete cell from old cluster and emit change.
      */
    if (value.dependentDistance.isDefined && value.dependentDistance.get < xi) {
      if (oldCluster.isDefined) {
        oldCluster.get.value.removeCell(key)
        context.forward(oldCluster.get.key, oldCluster.get.value)
        context.commit()
      }
      return
    }

    /**
      * If the dependent distance is empty or greater than tau, the cell is a root node of a MSDSubtree
      */
    val rootNode = value.dependentDistance.isEmpty || value.dependentDistance.get > tau

    /**
      * If the cell is alone in its cluster and a root-node, nothing changed
      */
    if (oldCluster.isDefined && oldCluster.get.value.size == 1 && rootNode) {
      return
    }

    if (rootNode) {
      if (oldCluster.isDefined) {
        val oldDepDist = oldCluster.get.value.getCell(key).get.dependentDistance
        // old and updated cell are root nodes -> nothing changed
        if (oldDepDist.isEmpty || oldDepDist.get > tau) {
          return
        }

        // cell is new root node -> remove from old cell and create new cluster cell afterwards
        oldCluster.get.value.removeCell(key)
        context.forward(oldCluster.get.key, oldCluster.get.value)
      }
      val newCluster = new Cluster()
      val newKey = UUID.randomUUID.toString
      newCluster.addCell(key, value)
      clusters.put(newKey, newCluster)
      context.forward(newKey, newCluster)
      context.commit()
    } else {

      /**
        * Find cell the updated cell depends on and add the updated cell to the same cluster
        */
      if (oldCluster.isDefined && value.dependentClusterCell.isDefined) {
        val newCluster = oldClusters
          .find(
            cluster =>
              cluster.value.containsCell(value.dependentClusterCell.get)
          )
        if (newCluster.isDefined) {
          newCluster.get.value.addCell(key, value)
          context.forward(newCluster.get.key, newCluster.get.value)
          context.commit()
        }
      }
    }
  }

  override def close(): Unit = {}
}
