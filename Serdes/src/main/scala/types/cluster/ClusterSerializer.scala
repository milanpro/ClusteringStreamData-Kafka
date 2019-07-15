package types.cluster

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Serializer
import types.cell.ClusterCell

import scala.collection.mutable

class ClusterSerializer
    extends Serializer[
      mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]
    ] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(
    topic: String,
    data: mutable.LinkedHashSet[mutable.LinkedHashSet[ClusterCell]]
  ): Array[Byte] = {
    if (data == null) {
      return null
    }

    SerializationUtils.serialize(data)
  }

  override def close(): Unit = {}
}
