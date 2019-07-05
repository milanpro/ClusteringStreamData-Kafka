import java.time.Duration
import java.util.Properties

import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}
import types.cell.{ClusterCellSerde, ClusterCellSerializer}
import types.point.{Point, PointDeserializer, PointSerde}

object PointClusterer extends App {

  val clusterCellProcessorSupplier: ProcessorSupplier[String, Point] =
    () => new PointToClusterCellProcessor

  val pointBufferStateStore = Stores.keyValueStoreBuilder(
    Stores.inMemoryKeyValueStore("point-buffer-store"),
    new StringSerde,
    new PointSerde
  )

  val clusterBufferStateStore = Stores.keyValueStoreBuilder(
    Stores.inMemoryKeyValueStore("clustercell-buffer-store"),
    new StringSerde,
    new ClusterCellSerde
  )

  val config: Properties = {
    val p = new Properties
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "point-clusterer-application")
    val bootstrapServers = if (args.length > 0) args(0) else "localhost:9092"
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "point.clusterer")
    p
  }

  val topology = new Topology()
    .addSource(
      "points",
      new StringDeserializer,
      new PointDeserializer,
      "streams-points-input"
    )
    .addProcessor(
      "point-to-cluster-cell-processor",
      clusterCellProcessorSupplier,
      "points"
    )
    .addSink(
      "clusters-sink",
      "streams-clustercells-input",
      new StringSerializer,
      new ClusterCellSerializer,
      "point-to-cluster-cell-processor"
    )
    .addStateStore(pointBufferStateStore, "point-to-cluster-cell-processor")
    .addStateStore(clusterBufferStateStore, "point-to-cluster-cell-processor")

  val streams: KafkaStreams = new KafkaStreams(topology, config)

  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
