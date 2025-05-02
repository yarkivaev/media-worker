package medwork.server

import cats.effect.std.Semaphore
import cats.effect.kernel.Clock
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.*
import medwork.*
import medwork.command.*
import medwork.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName
import medwork.server.config.*
import pureconfig.ConfigSource
import medwork.server.persistence.Storage
import io.circe.Decoder
import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon
import io.minio.MinioClient
import scala.collection.mutable
import cats.Applicative
import javax.print.attribute.standard.Media
import mongo4cats.client.MongoClient

/** Pak media worker service entrypoint.
  */
object Server extends IOApp {

  def run(args: List[String]) = run(args)(Map())

  def run(args: List[String])(extraDecoders: Map[String, Decoder[_ <: MediaWorkerCommand]]): IO[ExitCode] = {

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess[IO]()

    given Clock[IO] = IO.asyncForIO

    import medwork.server.streaming.FFMpeg.{*, given}

    import medwork.server.persistence.aws.given

    given Map[String, Decoder[_ <: MediaWorkerCommand]] = MediaWorkerCommand.given_Map_String_Decoder ++ extraDecoders

    (for {
      serverConfig <- Resource.eval(ServerConfig.load(ConfigSource.default))
      mongoClient <- MongoClient.fromConnectionString[IO]("mongodb://localhost:27017")
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
      given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]
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
