import org.apache.kafka.streams.processor.{Processor, ProcessorContext, PunctuationType}
import org.apache.kafka.streams.state.KeyValueStore

/*
* https://docs.confluent.io/current/streams/developer-guide/processor-api.html#defining-a-stream-processor
*/

class PointToClusterCellProcessor extends Processor[String, Point] {

  private var context: ProcessorContext = _
  private var kvStore: KeyValueStore[String, ClusterCell] = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
    kvStore = context.getStateStore("ClusterCellStore").asInstanceOf[[KeyValueStore[String, ClusterCell]]
  }

  override def process(key: String, value: Point): Unit = {
    //TODO: Magic
  }

  override def close(): Unit = {

  }
}