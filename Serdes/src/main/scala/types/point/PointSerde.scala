package types.point

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

class PointSerde extends Serde[Point] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit =
    ()

  override def close(): Unit = ()

  override def serializer(): Serializer[Point] = new PointSerializer

  override def deserializer(): Deserializer[Point] = new PointDeserializer
}
