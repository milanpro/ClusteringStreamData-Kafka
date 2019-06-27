package types.cluster

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Deserializer

class ClusterDeserializer extends Deserializer[Cluster] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(
    topic: String,
    data: Array[Byte]
  ): Cluster = {
    if (data == null) {
      return null
    }

    SerializationUtils.deserialize[Cluster](data)
  }

  override def close(): Unit = {}
}
