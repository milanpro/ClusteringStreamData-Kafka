package types.cluster

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import types.cell.ClusterCell

import scala.collection.mutable

class ClusterSerde
    extends Serde[mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    ()

  override def close(): Unit = ()

  override def serializer()
    : Serializer[mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]] =
    new ClusterSerializer

  override def deserializer()
    : Deserializer[mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]] =
    new ClusterDeserializer
}
