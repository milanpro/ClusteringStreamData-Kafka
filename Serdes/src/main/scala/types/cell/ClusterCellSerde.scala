package types.cell

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

class ClusterCellSerde extends Serde[ClusterCell] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    ()

  override def close(): Unit = ()

  override def serializer(): Serializer[ClusterCell] =
    new ClusterCellSerializer

  override def deserializer(): Deserializer[ClusterCell] =
    new ClusterCellDeserializer
}
