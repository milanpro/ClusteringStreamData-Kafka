package types.cell

import java.util

import com.google.gson.Gson
import org.apache.kafka.common.serialization.Deserializer

class ClusterCellDeserializer extends Deserializer[ClusterCell] {
  val gson = new Gson

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): ClusterCell = {
    if (data == null) {
      return null
    }

    gson.fromJson(new String(data, "utf-8"), ClusterCell.getClass)
  }

  override def close(): Unit = {}
}
