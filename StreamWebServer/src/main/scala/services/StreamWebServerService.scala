package MSD_Clustering_Stream_date.StreamWebServer.services

import com.google.gson.Gson
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import types.cell.ClusterCell
import types.cluster.Cluster
import types.point.{Point, PointDeserializer}

@Service
class StreamWebServerService {

  @Autowired val template: SimpMessagingTemplate = null

  @KafkaListener(
    topics = Array("streams-points-input"),
    containerFactory = "kafkaListenerPointContainerFactory"
  )
  def consumePoint(@Payload point: ConsumerRecord[String, Point]): Unit = {
    template.convertAndSend("/topic/points", new Gson().toJson(point))
  }

  @KafkaListener(
    topics = Array("streams-clustercells-input"),
    containerFactory = "kafkaListenerClusterCellContainerFactory"
  )
  def consumeClusterCell(
    @Payload clusterCell: ConsumerRecord[String, ClusterCell]
  ): Unit = {
    template.convertAndSend(
      "/topic/clustercells",
      new Gson().toJson(clusterCell)
    )
  }

}
