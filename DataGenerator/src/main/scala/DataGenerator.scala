import java.util.{Properties, UUID}

import etcd.EtcdManaged
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import types.point.{Point, PointSerializer}

import scala.util.Random

object DataGenerator extends App {

  val properties = new Properties()
  properties.put("bootstrap.servers", "msd-kafka:9092")

  val etcdClient = new EtcdManaged("http://msd-etcd:2379")

  val stringSer = new StringSerializer
  val pointSer = new PointSerializer

  val kafkaProducer =
    new KafkaProducer[String, Point](properties, stringSer, pointSer)

  var distributions: List[((Double, Double), (Double, Double))] = List()

  val distriCount = Random.nextInt(3) + 2

  for (_ <- 0 to distriCount) {
    distributions = (
      (Random.nextDouble() * 25, Random.nextDouble() * 100),
      (Random.nextDouble() * 25, Random.nextDouble() * 100)
    ) :: distributions
  }

  var pointDelay = 100

  etcdClient.watchWithCb("gen/pointDelay", value => {
    pointDelay = value.toInt
  })

  while (true) {
    val index = Random.nextInt(distriCount)
    val distrib = distributions(index)
    val point = Point(
      Random.nextGaussian() * distrib._1._1 + distrib._1._2,
      Random.nextGaussian() * distrib._2._1 + distrib._2._2
    )
    val record =
      new ProducerRecord[String, Point](
        "streams-points-input",
        UUID.randomUUID.toString,
        point
      )
    Thread.sleep(pointDelay)
    kafkaProducer.send(record)
  }
}
