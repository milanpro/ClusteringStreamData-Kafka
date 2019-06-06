package types.cell

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Deserializer

class ClusterCellDeserializer extends Deserializer[ClusterCell] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): ClusterCell = {
    if (data == null) {
      return null
    }

    SerializationUtils.deserialize[ClusterCell](data)
  }

  override def close(): Unit = {}
}
