import java.nio.ByteBuffer
import java.util

import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer

class PointSerializer extends Serializer[Point] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def serialize(topic: String, data: Point): Array[Byte] = {
    if (data == null) {
      return null
    }

    try {
      // 2 doubles = 16 bytes
      val bytes = new Array[Byte](16)
      val buffer = ByteBuffer.wrap(bytes)

      buffer putDouble data.x
      buffer putDouble data.y

      bytes
    } catch {
      case _: Throwable => throw new SerializationException("Error serializing value");
    }
  }

  override def close(): Unit = {

  }
}
