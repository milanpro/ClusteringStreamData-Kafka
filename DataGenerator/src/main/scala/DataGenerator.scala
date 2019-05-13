import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.util.Random

object DataGenerator extends App {

  val properties = new Properties()
  properties.put("bootstrap.servers", "localhost:9092")

  val stringSer = new StringSerializer
  val pointSer = new PointSerializer

  val kafkaProducer = new KafkaProducer[String, Point](properties, stringSer, pointSer)

  val id = 0

  while (true) {
    val point = Point(Random.nextDouble(), Random.nextDouble())
    val record = new ProducerRecord[String, Point]("streams-wordcount-output",s"$id", point)

    kafkaProducer.send(record)
  }
}