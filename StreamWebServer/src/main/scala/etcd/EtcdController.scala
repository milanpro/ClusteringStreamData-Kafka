package de.hpi.msd.server.etcd

import etcd.EtcdManaged
import org.springframework.web.bind.annotation.{
  GetMapping,
  PostMapping,
  RequestBody,
  RequestParam,
  RestController
}

case class setValue(key: String, value: String) extends Serializable

@RestController
class EtcdController {
  val etcdClient = new EtcdManaged("http://msd-etcd:2379")

  @PostMapping(path = Array("/setval"))
  def setEtcdValue(@RequestBody body: setValue): Unit = {
    etcdClient.setValue(body.key, body.value)
  }

  @GetMapping(path = Array("/getval"))
  def getEtcdValue(@RequestParam key: String): String = {
    etcdClient.getValue(key)
  }
}
