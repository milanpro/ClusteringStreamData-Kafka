import java.awt.BorderLayout
import java.time.Duration
import java.util.Properties
import java.util.regex.Pattern

import javax.swing.{JFrame, JPanel}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.knowm.xchart.{BubbleChart, BubbleChartBuilder, XChartPanel}
import types.cell.{ClusterCell, ClusterCellDeserializer}
import types.point.{Point, PointDeserializer}

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.control.Breaks._

object StreamPlotter extends App {

  def makeChart = {
    val chart = new BubbleChartBuilder()
      .width(600)
      .height(500)
      .title("Stream Plot")
      .xAxisTitle("X")
      .yAxisTitle("Y")
      .build

    chart.addSeries(
      "Clusters",
      new Array[Double](1),
      new Array[Double](1),
      new Array[Double](1)
    )
    chart.addSeries(
      "Points",
      new Array[Double](1),
      new Array[Double](1),
      new Array[Double](1)
    )

    chart
  }

  val properties = new Properties()
  properties.put("bootstrap.servers", "localhost:9092")
  properties.put("group.id", "stream-generator")

  val clusterConsumer =
    new KafkaConsumer[String, ClusterCell](
      properties,
      new StringDeserializer,
      new ClusterCellDeserializer
    )
  val pointConsumer =
    new KafkaConsumer[String, Point](
      properties,
      new StringDeserializer,
      new PointDeserializer
    )

  val chart = makeChart
  var chartPanel: JPanel = _

  javax.swing.SwingUtilities.invokeLater(() => {
    val frame = new JFrame("Advanced Example")
    frame.setLayout(new BorderLayout)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    chartPanel = new XChartPanel[BubbleChart](chart)
    frame.add(chartPanel, BorderLayout.CENTER)

    frame.pack()
    frame.setVisible(true)
  })

  clusterConsumer.subscribe(Pattern.compile("streams-clusters-input"))
  pointConsumer.subscribe(Pattern.compile("streams-points-input"))

  val ringbuffer = mutable.Queue[Point]()
  val clustersFinal = mutable.Map[String, ClusterCell]()

  while (true) breakable {
    val pointResult = pointConsumer.poll(Duration.ofSeconds(1))
    val clusterResult = clusterConsumer.poll(Duration.ofMillis(1))

    if (pointResult.count() == 0 && clusterResult.count() == 0) break

    val points: Iterable[Point] = pointResult.asScala.map(_.value())

    points.foreach(point => {
      if (ringbuffer.length >= 500) ringbuffer.dequeue()
      ringbuffer += point
    })

    clusterResult.asScala.foreach(
      cell => {
        if (cell.value == null) {
          clustersFinal -= cell.key
        } else {
          clustersFinal.put(cell.key(), cell.value)
        }
      }
    )

    val xvalsPoints = ringbuffer.map(_.x).toArray
    val yvalsPoints = ringbuffer.map(_.y).toArray
    val zvalsPoints = ringbuffer.map(_ => 1D).toArray

    //clustersFinal.values.foreach(cell => println(cell.timelyDensity))
    val xvalsClusters = clustersFinal.values.map(_.seedPoint.x).toArray
    val yvalsClusters = clustersFinal.values.map(_.seedPoint.y).toArray
    val zvalsClusters =
      clustersFinal.values.map(_.timelyDensity * 10).toArray

    javax.swing.SwingUtilities.invokeLater(() => {
      chart.updateBubbleSeries(
        "Clusters",
        xvalsClusters,
        yvalsClusters,
        zvalsClusters
      )
      chart.updateBubbleSeries("Points", xvalsPoints, yvalsPoints, zvalsPoints)
      chartPanel.repaint()
    })
  }
}
