package de.hpi.msd.server.services

import com.google.gson.Gson
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import types.cell.ClusterCell
import types.point.{Point, PointDeserializer}

import scala.collection.mutable

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

  @KafkaListener(
    topics = Array("streams-cluster-input"),
    containerFactory = "kafkaListenerClusterContainerFactory"
  )
  def consumeCluster(
    @Payload cluster: ConsumerRecord[String, mutable.LinkedHashSet[
      mutable.LinkedHashSet[ClusterCell]
    ]]
  ): Unit = {
    template.convertAndSend(
      "/topic/clusters",
      new Gson().toJson(cluster)
    )
  }
}
