package domain.server

import cats.effect.std.Semaphore
import cats.effect.kernel.Clock
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.*
import domain.*
import domain.command.MediaWorkerCommand
import domain.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName
import domain.server.config.*
import pureconfig.ConfigSource
import domain.server.persistence.Storage

import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon
import io.minio.MinioClient
import scala.collection.mutable
import cats.Applicative

/** Pak media worker service entrypoint.
  */
object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess[IO]()

    given Clock[IO] = IO.asyncForIO

    import domain.server.streaming.FFMpeg.{*, given}

    import domain.server.persistence.aws.given

    given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]

    (for {
      serverConfig <- Resource.eval(ServerConfig.load(ConfigSource.default))
      given MinioClient = MinioClient
        .builder()
        .endpoint(serverConfig.s3.endpointUrl)
        .credentials(serverConfig.s3.accessKey, serverConfig.s3.secretKey)
        .build()
      given StreamingBackend[IO] = FFMpegStreamingBackend[IO]
      bucketIntroduceSem <- Resource.eval(Semaphore[IO](1))
      bucketSems = mutable.Map.empty[String, Semaphore[IO]]
      given (String => IO[Semaphore[IO]]) = (bucketName: String) => {
        bucketIntroduceSem.permit.use(_ =>
          bucketSems
            .get(bucketName)
            .map(Applicative[IO].pure(_))
            .getOrElse({
              Semaphore[IO](1).map(sem => {
                bucketSems += (bucketName -> sem)
                sem
              })
            })
        )
      }
      lepusClient <- LepusClient[IO](
        host = Host.fromString(serverConfig.queue.host).get,
        port = Port.fromInt(serverConfig.queue.port.toInt).get
      )
      mediaWorker <- Resource.eval(
        MediaWorker(
          Broker.messageSource[IO, MediaWorkerCommand](
            lepusClient,
            QueueName("mediaworkercommand")
          )
        )
      )
    } yield ()).use_.as(ExitCode.Success)
  }
}
