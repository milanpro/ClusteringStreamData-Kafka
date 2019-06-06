package types.point

import java.util

import com.google.gson.Gson
import org.apache.kafka.common.serialization.Serializer

class PointSerializer extends Serializer[Point] {
  var gson = new Gson
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: Point): Array[Byte] = {
    if (data == null) {
      return null
    }

    gson.toJson(data).getBytes("Utf-8")
  }

  override def close(): Unit = {}
}
