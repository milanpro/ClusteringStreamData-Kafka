package types.cell

import java.util

import com.google.gson.Gson
import org.apache.kafka.common.serialization.Serializer

class ClusterCellSerializer extends Serializer[ClusterCell] {
  val gson = new Gson

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: ClusterCell): Array[Byte] = {
    if (data == null) {
      return null
    }

    gson.toJson(data).getBytes("utf-8")
  }

  override def close(): Unit = {}
}
