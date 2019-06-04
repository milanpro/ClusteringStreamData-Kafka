import java.nio.ByteBuffer
import java.util

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer

class PointDeserializer extends Deserializer[Point] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): Point = {
    if (data == null) {
      return null
    }

    try {
      val buffer = ByteBuffer.wrap(data)

      val x = buffer getDouble
      val y = buffer getDouble

      Point(x, y)
    } catch {
      case _: Throwable =>
        throw new SerializationException("Error deserializing value");
    }
  }

  override def close(): Unit = {}
}
