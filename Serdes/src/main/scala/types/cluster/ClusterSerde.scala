package types.cluster

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

class ClusterSerde extends Serde[Cluster] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    ()

  override def close(): Unit = ()

  override def serializer(): Serializer[Cluster] =
    new ClusterSerializer

  override def deserializer(): Deserializer[Cluster] =
    new ClusterDeserializer
}
