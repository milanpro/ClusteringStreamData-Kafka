package types.point

import java.util

import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Serializer

class PointSerializer extends Serializer[Point] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: Point): Array[Byte] = {
    if (data == null) {
      return null
    }

    SerializationUtils.serialize(data)
  }

  override def close(): Unit = {}
}
