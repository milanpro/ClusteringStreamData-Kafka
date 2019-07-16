package types.cluster

import java.util

import com.google.gson.Gson
import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Deserializer
import types.cell.ClusterCell

import scala.collection.mutable

class ClusterDeserializer
    extends Deserializer[
      Clusters
    ] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(
    topic: String,
    data: Array[Byte]
  ): Clusters = {
    if (data == null) {
      return null
    }
    SerializationUtils.deserialize(data)
  }

  override def close(): Unit = {}
}
