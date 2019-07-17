import java.time.Duration
import java.util.Properties

import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}
import types.cell.{ClusterCell, ClusterCellDeserializer}
import types.cluster.ClusterSerializer

object ClusterCellClusterer extends App {

  val clusterProcessorSupplier: ProcessorSupplier[String, ClusterCell] =
    () => new ClusterCellToClusteringProcessor

  val config: Properties = {
    val p = new Properties
    p.put(
      StreamsConfig.APPLICATION_ID_CONFIG,
      "cluster-cell-clusterer-application"
    )
    val bootstrapServers = if (args.length > 0) args(0) else "msd-kafka:9092"
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p
  }

  val topology = new Topology()
    .addSource(
      "clustercells",
      new StringDeserializer,
      new ClusterCellDeserializer,
      "streams-clustercells-input"
    )
    .addProcessor(
      "cluster-cell-to-cluster-processor",
      clusterProcessorSupplier,
      "clustercells"
    )
    .addSink(
      "clusters-sink",
      "streams-cluster-input",
      new StringSerializer,
      new ClusterSerializer,
      "cluster-cell-to-cluster-processor"
    )

  val streams: KafkaStreams = new KafkaStreams(topology, config)

  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
