import cats.effect.{IO, Sync}
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{GenericContainer, RabbitMQContainer, MinIOContainer}
import com.github.kokorin.jaffree.ffprobe.FFprobe
import medwork.client.Client
import medwork.command.SaveStream
import medwork.{RtmpSink, RtspSource}
import org.scalatest.flatspec
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork

import scala.concurrent.duration.DurationInt
import scala.util.Try
import medwork.HlsSink
import io.minio.{MinioClient, GetObjectArgs}

class RtspToHlsS3 extends flatspec.AnyFlatSpec with TestContainersForAll {

  override type Containers = Setup.SetupType

  override def startContainers(): Containers = Setup.setup

  "mediaSink" should "be able to put new MediaWorkerCommands" in {
    withContainers(
      Setup
        .client(_)
        .use((client, relatedContainers) => {
          val minioClient = MinioClient
            .builder()
            .endpoint(f"http://localhost:${relatedContainers.s3.mappedPort(9000)}/")
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
          for {
            _ <- client.executeCommand(
              SaveStream(
                RtspSource(
                  f"rtsp://${relatedContainers.rtspServer.networkAliases.head}:8554/test"
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
        })
        .unsafeRunSync()
    )
  }
}
