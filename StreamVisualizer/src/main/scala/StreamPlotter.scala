import java.awt.BorderLayout
import java.time.Duration
import java.util.Properties
import java.util.regex.Pattern

import javax.swing.{JFrame, JLabel, JPanel, SwingConstants}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.knowm.xchart.{XChartPanel, XYChart, XYChartBuilder}
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.Breaks._

object StreamPlotter extends App {

  def makeChart = {
    val chart = (new XYChartBuilder() width 600 height 500 title "Stream Plot" xAxisTitle "X" yAxisTitle "Y")
      .build

    chart.getStyler setDefaultSeriesRenderStyle XYSeriesRenderStyle.Scatter

    chart addSeries("Points", new Array[Double](1))

    chart
  }

  val properties = new Properties()
  properties put("bootstrap.servers", "localhost:9092")
  properties put("group.id", "stream-generator")

  val stringDes = new StringDeserializer
  val pointDes = new PointDeserializer

  val kafkaConsumer = new KafkaConsumer[String, Point](properties, stringDes, pointDes)

  val chart = makeChart
  var chartPanel: JPanel = _

  javax.swing.SwingUtilities.invokeLater(() => {
    val frame = new JFrame("Advanced Example")
    frame setLayout new BorderLayout
    frame setDefaultCloseOperation JFrame.EXIT_ON_CLOSE

    chartPanel = new XChartPanel[XYChart](chart)
    frame add(chartPanel, BorderLayout.CENTER)

    frame pack()
    frame setVisible true
  })

  kafkaConsumer subscribe (Pattern compile "streams-wordcount-output")

  val ringbuffer = mutable.Queue[Point]()

  while (true) breakable {
    val results = kafkaConsumer.poll(Duration.ofSeconds(10))

    if (results.count() == 0) break

    val points: Iterable[Point] = results.asScala.map(_.value())

    points foreach (point => {
      if (ringbuffer.length >= 500) ringbuffer.dequeue()
      ringbuffer += point
    })

    val xvals = ringbuffer.map(_.x).toArray
    val yvals = ringbuffer.map(_.y).toArray

    javax.swing.SwingUtilities invokeLater (() => {
      chart updateXYSeries("Points", xvals, yvals, null)
      chartPanel repaint()
    })
  }
}