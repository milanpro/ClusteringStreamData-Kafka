package types.point

import java.util

import com.google.gson.Gson
import org.apache.kafka.common.serialization.Deserializer

class PointDeserializer extends Deserializer[Point] {
  var gson = new Gson
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): Point = {
    if (data == null) {
      return null
    }

    gson.fromJson(new String(data, "utf-8"), Point.getClass)
  }

  override def close(): Unit = {}
}
