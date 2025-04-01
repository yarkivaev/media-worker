import cats.effect.{IO, Sync}
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{GenericContainer, RabbitMQContainer, MinIOContainer}
import com.github.kokorin.jaffree.ffprobe.FFprobe
import domain.client.Client
import domain.command.RecordVideoSource
import domain.{RtmpSink, RtspSource}
import org.scalatest.flatspec
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork

import scala.concurrent.duration.DurationInt
import scala.util.Try
import domain.HlsSink
import io.minio.{MinioClient, GetObjectArgs}

class RtspToHlsS3 extends flatspec.AnyFlatSpec with TestContainersForAll {

  override type Containers = GenericContainer and GenericContainer and RabbitMQContainer and GenericContainer and
    MinIOContainer

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
    val s3 = MinIOContainer().configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("s3")
      }
    })
    s3.start()
    val mediaWorker = MediaWorker("broker", 5672).configure(container => {
      container.setNetwork(network)
      container.withCreateContainerCmdModifier { cmd =>
        cmd.withName("media-worker")
      }
    })
    mediaWorker.start()
    rtspServer and rtspStream and rabbitMQ and mediaWorker and s3
  }

  "mediaSink" should "be able to put new MediaWorkerCommands" in {
    print(getClass.getClassLoader.getResource("dummy-rtsp-server.yml"))
    withContainers { containers =>
      {
        val s3 = containers.tail
        val mediaWorker = containers.head.tail
        val rabbitMq: RabbitMQContainer = containers.head.head.tail
        val rtspServer: GenericContainer = containers.head.head.head.head
        val rtspStream: GenericContainer = containers.head.head.head.tail
        val minioClient = MinioClient
          .builder()
          .endpoint(f"http://localhost:${s3.mappedPort(9000)}/")
          .credentials("miniouser", "miniopassword")
          .build()
        def getFileFromS3(fileName: String) =
          Sync[IO].delay {
            val request = GetObjectArgs
              .builder()
              .bucket("first-camera")
              .`object`(fileName)
              .build()
            val response = minioClient
              .getObject(request)
          }
        (for {
          client <- Client(rabbitMq.amqpPort)
        } yield client)
          .use(client =>
            for {
              _ <- client.executeCommand(
                RecordVideoSource(
                  RtspSource(
                    f"rtsp://rtsp-server:8554/test"
                  ),
                  HlsSink(
                    f"first_camera"
                  )
                )
              )
              _ <- IO.sleep(30.second)
              _ <- getFileFromS3("output.m3u8")
              _ <- getFileFromS3("segment_000.ts")
            } yield ()
          )
          .unsafeRunSync()
      }
    }
  }
}
