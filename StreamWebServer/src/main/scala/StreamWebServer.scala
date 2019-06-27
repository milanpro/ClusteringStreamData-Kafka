package MSD_Clustering_Stream_date.StreamWebServer

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

object StreamWebServer {
  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[StreamWebServer], args: _*)
}

@SpringBootApplication
class StreamWebServer
