import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{GenericContainer, RabbitMQContainer}
import com.github.kokorin.jaffree.ffprobe.FFprobe
import medwork.client.Client
import medwork.command.StartMediaStream
import medwork.{RtmpSink, RtspSource}
import org.scalatest.flatspec
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork

import scala.concurrent.duration.DurationInt
import scala.util.Try

class RtspToRtmp extends flatspec.AnyFlatSpec with TestContainersForAll {

  override type Containers = Setup.SetupType

  override def startContainers(): Containers = Setup.setup

  "mediaSink" should "be able to put new MediaWorkerCommands" in {
    withContainers(
      Setup
        .client(_)
        .use((client, relatedContainers) =>
          for {
            _ <- client.executeCommand(
              StartMediaStream(
                RtspSource(
                  f"rtsp://${relatedContainers.rtspServer.networkAliases.head}:8554/test"
                ),
                RtmpSink(
                  f"rtmp://${relatedContainers.rtspServer.networkAliases.head}:1935/new_stream"
                )
              )
            )
            _ <- IO.sleep(10.second)
            probe <- IO.delay {
              Try(
                FFprobe
                  .atPath()
                  .setShowStreams(true)
                  .setInput(s"rtmp://localhost:${relatedContainers.rtspServer.mappedPort(1935)}/new_stream")
                  .execute()
              )
                .map(_ => true)
                .recover {
                  case th: Throwable => {
                    println(th.getClass())
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
    )
  }
}
