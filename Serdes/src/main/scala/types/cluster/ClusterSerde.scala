package types.cluster

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import types.cell.ClusterCell

import scala.collection.mutable

class ClusterSerde extends Serde[Clusters] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    ()

  override def close(): Unit = ()

  override def serializer(): Serializer[Clusters] =
    new ClusterSerializer

  override def deserializer(): Deserializer[Clusters] =
    new ClusterDeserializer
}
