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

/**
  * (Deprecated) Point and cluster cell plotter
  * Got replaced with the StreamWebServerFrontend
  */
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
      "Cluster Cells",
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
  properties.put("bootstrap.servers", sys.env("KAFKA_ADDR"))
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

  clusterConsumer.subscribe(Pattern.compile("streams-clustercells-input"))
  pointConsumer.subscribe(Pattern.compile("streams-points-input"))

  val ringBuffer = mutable.Queue[Point]()
  val clustersFinal = mutable.Map[String, ClusterCell]()

  while (true) breakable {
    val pointResult = pointConsumer.poll(Duration.ofSeconds(1))
    val clusterResult = clusterConsumer.poll(Duration.ofSeconds(1))

    if (pointResult.count() == 0 && clusterResult.count() == 0) break

    val points: Iterable[Point] = pointResult.asScala.map(_.value())

    points.foreach(point => {
      if (ringBuffer.length >= 500) ringBuffer.dequeue()
      ringBuffer += point
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

    val xValsPoints = ringBuffer.map(_.x).toArray
    val yValsPoints = ringBuffer.map(_.y).toArray
    val zValsPoints = ringBuffer.map(_ => 1D).toArray

    val xValsClusters = clustersFinal.values.map(_.seedPoint.x).toArray
    val yValsClusters = clustersFinal.values.map(_.seedPoint.y).toArray
    val zValsClusters =
      clustersFinal.values.map(_.timelyDensity).toArray

    javax.swing.SwingUtilities.invokeLater(() => {
      chart.updateBubbleSeries(
        "Cluster Cells",
        xValsClusters,
        yValsClusters,
        zValsClusters
      )
      chart.updateBubbleSeries("Points", xValsPoints, yValsPoints, zValsPoints)
      chartPanel.repaint()
    })
  }
}
