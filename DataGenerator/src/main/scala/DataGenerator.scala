import java.util.{Properties, UUID}

import etcd.EtcdManaged
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import types.point.{Point, PointSerializer}

import scala.util.Random

object DataGenerator extends App {

  val properties = new Properties()
  properties.put("bootstrap.servers", sys.env("KAFKA_ADDR"))
  val etcdClient = new EtcdManaged(sys.env("ETCD_ADDR"))

  val stringSer = new StringSerializer
  val pointSer = new PointSerializer

  val kafkaProducer =
    new KafkaProducer[String, Point](properties, stringSer, pointSer)

  var distributions: scala.collection.mutable.ListBuffer[
    ((Double, Double), (Double, Double))
  ] = scala.collection.mutable.ListBuffer.empty
  var x = (15.0, 20.0)
  var y = (10.0, 70.0)
  distributions.addOne((x, y))
  x = (10.0, 110.0)
  y = (8.0, 110.0)
  distributions.addOne((x, y))
  x = (10.0, etcdClient.setValue("gen/cluster1x", "90.0").toDouble)
  y = (10.0, 10.0)
  distributions.addOne((x, y))

  var pointDelay = etcdClient.setValue("gen/pointDelay", "10").toInt

  etcdClient.watchWithCb("gen/pointDelay", value => {
    pointDelay = value.toInt
  })

  etcdClient.watchWithCb("gen/cluster1x", value => {
    x = (10.0, value.toDouble)
    y = (10.0, 10.0)
    distributions.remove(2)
    distributions.addOne((x, y))
  })

  while (true) {
    val index = Random.nextInt(3)
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
