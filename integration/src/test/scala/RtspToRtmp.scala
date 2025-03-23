import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{GenericContainer, RabbitMQContainer}
import com.github.kokorin.jaffree.ffprobe.FFprobe
import domain.client.Client
import domain.command.RouteCameraToMiddleware
import domain.{RtmpSink, RtspSource}
import org.scalatest.flatspec
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork

import scala.concurrent.duration.DurationInt
import scala.util.Try

class RtspToRtmp extends flatspec.AnyFlatSpec with TestContainersForAll {

  override type Containers = GenericContainer and GenericContainer and RabbitMQContainer and GenericContainer

  override def startContainers(): Containers = {
    val network: Network = newNetwork()
    val rtspServer = DummyRtspServer().configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("rtsp-server")
      }
    })
    rtspServer.start()
    val rtspStream = DummyRtspStream("rtsp-server", 8554).configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("rtsp-stream")
      }
    })
    rtspStream.start()
    val rabbitMQ = RabbitMQContainer("rabbitmq:3-management").configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("broker")
      }
    })
    rabbitMQ.start()
    val mediaWorker = MediaWorker("broker", 5672).configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("media-worker")
      }
    })
    mediaWorker.start()
    println(mediaWorker.logs)
    rtspServer and rtspStream and rabbitMQ and mediaWorker
  }

  "mediaSink" should "be able to put new MediaWorkerCommands" in {
    print(getClass.getClassLoader.getResource("dummy-rtsp-server.yml"))
    withContainers { containers => {
      val mediaWorker = containers.tail
      val rabbitMq: RabbitMQContainer = containers.head.tail
      val rtspServer: GenericContainer = containers.head.head.head
      val rtspStream: GenericContainer = containers.head.head.tail
      (for {
        client <- Client(rabbitMq.amqpPort)
      } yield client)
        .use(client =>
          for {
            _ <- client.executeCommand(
              RouteCameraToMiddleware(
                RtspSource(
                  f"rtsp://rtsp-server:8554/test"
                ),
                RtmpSink(
                  f"rtmp://rtsp-server:1935/new_stream"
                )
              )
            )
            _ <- IO.sleep(3.second)
            probe <- IO.delay {
              Try(
                FFprobe.atPath()
                  .setShowStreams(true)
                  .setInput(s"rtmp://localhost:${rtspServer.mappedPort(1935)}/new_stream")
                  .execute()
              )
                .map(_ => true)
                .recover {
                  case th: Throwable => {
                    println(th)
                    false
                  }
                }
                .get
            }
            _ <- IO.delay {
              assert(probe)
            }
          } yield ()
        )
        .unsafeRunSync()
    }
    }
  }
}
