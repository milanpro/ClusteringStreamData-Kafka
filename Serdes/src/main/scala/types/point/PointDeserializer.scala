package types.point

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Deserializer

class PointDeserializer extends Deserializer[Point] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): Point = {
    if (data == null) {
      return null
    }

    SerializationUtils.deserialize[Point](data)
  }

  override def close(): Unit = {}
}
