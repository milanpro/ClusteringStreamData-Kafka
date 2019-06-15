import java.util.UUID

import org.apache.kafka.streams.processor.{Processor, ProcessorContext}
import org.apache.kafka.streams.state.KeyValueStore
import types.cell.ClusterCell
import types.cluster.Cluster

import scala.jdk.CollectionConverters._

/*
 * https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
 */

class ClusterCellToClusteringProcessor extends Processor[String, ClusterCell] {

  val xi = 0

  val tau = 10

  private var context: ProcessorContext = _
  private var clusters: KeyValueStore[String, Cluster] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    clusters = context
      .getStateStore("cluster-store")
      .asInstanceOf[KeyValueStore[String, Cluster]]

  }

  override def process(key: String, value: ClusterCell): Unit = {
    if (value.dependentDistance.isEmpty && value.dependentDistance.get < xi) {
      return
    }
    val oldClusters = clusters.all.asScala;

    val oldCluster =
      oldClusters.find(cluster => cluster.value.containsCell(key))

    val rootNode = value.dependentDistance.isEmpty || value.dependentDistance.get > tau

    if (oldCluster.nonEmpty) {
      if (oldCluster.get.value.size == 1 && rootNode) {
        return
      }
      oldCluster.get.value.removeCell(key)
    }
    if (rootNode) {
      val newCluster = new Cluster()
      newCluster.addCell(key, value)
      clusters.put(UUID.randomUUID.toString, newCluster);
    } else {
      oldClusters
        .find(
          cluster => cluster.value.containsCell(value.dependentClusterCell.get)
        )
        .get
        .value
        .addCell(key, value)
    }
  }

  override def close(): Unit = {}
}
