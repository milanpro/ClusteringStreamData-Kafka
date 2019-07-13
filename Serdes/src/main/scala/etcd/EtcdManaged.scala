package etcd

import io.etcd.jetcd.watch.WatchEvent.EventType.PUT
import com.google.common.base.Charsets.UTF_8

import scala.jdk.CollectionConverters._
import io.etcd.jetcd.{ByteSequence, Client}

class EtcdManaged(etcdHost: String) {
  private var client = Client.builder.endpoints(etcdHost).build

  def watchWithCb(
    key: String,
    callback: String => Unit
  ): Unit = {
    val byte_key = ByteSequence.from(key, UTF_8)

    this.client.getWatchClient.watch(byte_key, t => {
      t.getEvents.asScala
        .filter(_.getEventType == PUT)
        .foreach(event => {
          callback(event.getKeyValue.getValue.toString(UTF_8))
        })
    })
  }
}
