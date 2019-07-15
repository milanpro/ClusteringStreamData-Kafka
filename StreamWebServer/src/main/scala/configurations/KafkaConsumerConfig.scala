package de.hpi.msd.server.configurations

import java.util

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import types.cell.{ClusterCell, ClusterCellDeserializer}
import types.cluster.{ClusterDeserializer}
import types.point.{Point, PointDeserializer}

import scala.collection.mutable

@EnableKafka
@Configuration class KafkaConsumerConfig {
  @Value("${kafka.bootstrapserver}") var bootstrapServer: String = null

  @Bean
  def consumerConfigs: util.Map[String, AnyRef] = {
    val props = new util.HashMap[String, AnyRef]
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      classOf[StringDeserializer]
    )
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "temp-groupid.group")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    props
  }

  @Bean
  def pointConsumerConfigs: util.Map[String, AnyRef] = {
    val props = consumerConfigs
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[PointDeserializer]
    )
    props
  }

  @Bean def pointConsumerFactory =
    new DefaultKafkaConsumerFactory[String, Point](pointConsumerConfigs)

  @Bean(name = Array("kafkaListenerPointContainerFactory"))
  def kafkaListenerPointContainerFactory: KafkaListenerContainerFactory[
    ConcurrentMessageListenerContainer[String, Point]
  ] = {
    val factory = new ConcurrentKafkaListenerContainerFactory[String, Point]
    factory.setConsumerFactory(pointConsumerFactory)
    factory
  }

  @Bean
  def clusterCellConsumerConfigs: util.Map[String, AnyRef] = {
    val props = consumerConfigs
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[ClusterCellDeserializer]
    )
    props
  }

  @Bean def clusterCellConsumerFactory =
    new DefaultKafkaConsumerFactory[String, ClusterCell](
      clusterCellConsumerConfigs
    )

  @Bean(name = Array("kafkaListenerClusterCellContainerFactory"))
  def kafkaListenerClusterCellContainerFactory: KafkaListenerContainerFactory[
    ConcurrentMessageListenerContainer[String, ClusterCell]
  ] = {
    val factory =
      new ConcurrentKafkaListenerContainerFactory[String, ClusterCell]
    factory.setConsumerFactory(clusterCellConsumerFactory)
    factory
  }

  @Bean
  def clusterConsumerConfigs: util.Map[String, AnyRef] = {
    val props = consumerConfigs
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[ClusterDeserializer]
    )
    props
  }

  @Bean def clusterConsumerFactory =
    new DefaultKafkaConsumerFactory[String, mutable.LinkedHashSet[
      mutable.LinkedHashSet[ClusterCell]
    ]](clusterConsumerConfigs)

  @Bean(name = Array("kafkaListenerClusterContainerFactory"))
  def kafkaListenerClusterContainerFactory: KafkaListenerContainerFactory[
    ConcurrentMessageListenerContainer[String, mutable.LinkedHashSet[
      mutable.LinkedHashSet[ClusterCell]
    ]]
  ] = {
    val factory =
      new ConcurrentKafkaListenerContainerFactory[String, mutable.LinkedHashSet[
        mutable.LinkedHashSet[ClusterCell]
      ]]
    factory.setConsumerFactory(clusterConsumerFactory)
    factory
  }

}
