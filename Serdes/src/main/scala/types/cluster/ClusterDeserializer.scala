package types.cluster

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Deserializer
import types.cell.ClusterCell

import scala.collection.mutable

class ClusterDeserializer
    extends Deserializer[
      mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]
    ] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(
    topic: String,
    data: Array[Byte]
  ): mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]] = {
    if (data == null) {
      return null
    }

    SerializationUtils
      .deserialize[mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]](
        data
      )
  }

  override def close(): Unit = {}
}
