package medwork.server

import cats.Applicative
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.kernel.Clock
import cats.effect.std.Semaphore
import com.comcast.ip4s._
import io.circe.Decoder
import io.minio.MinioClient
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName
import medwork._
import medwork.command._
import medwork.server.config._
import medwork.server.persistence.Storage
import medwork.server.persistence.aws
import medwork.server.streaming.FFMpegStreamingBackend
import medwork.server.streaming.RunProcess
import medwork.server.streaming.StreamingBackend
import mongo4cats.client.MongoClient
import os.Path
import pureconfig.ConfigSource

import scala.collection.mutable
import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon

/** Pak media worker service entrypoint.
  */
object Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = run(args)(Map())

  def run(args: List[String])(extraDecoders: Map[String, Decoder[_ <: MediaWorkerCommand]]): IO[ExitCode] = {

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess[IO]()

    given Clock[IO] = IO.asyncForIO

    import medwork.server.streaming.FFMpeg.{*, given}

    given Map[String, Decoder[_ <: MediaWorkerCommand]] = MediaWorkerCommand.given_Map_String_Decoder ++ extraDecoders

    (for {
      serverConfig <- Resource.eval(ServerConfig.load(ConfigSource.default))
      mongoClient <- MongoClient.fromConnectionString[IO]("mongodb://localhost:27017")
      minioClient = MinioClient
        .builder()
        .endpoint(serverConfig.s3.endpointUrl)
        .credentials(serverConfig.s3.accessKey, serverConfig.s3.secretKey)
        .build()
      given StreamingBackend[IO] = FFMpegStreamingBackend[IO]
      bucketIntroduceSem <- Resource.eval(Semaphore[IO](1))
      bucketSems = mutable.Map.empty[String, Semaphore[IO]]
      bucketSem = (bucketName: String) => {
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
      given Storage[IO, Path] = aws(
        bucketSem,
        minioClient
      )
      given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]
      brokerHost <- Resource.eval(
        IO.fromEither(
          Host.fromString(serverConfig.queue.host).toRight(new NoSuchElementException("No value in option"))
        )
      )
      brokerPort <- Resource.eval(
        IO.fromEither(
          Port.fromString(serverConfig.queue.port).toRight(new NoSuchElementException("No value in option"))
        )
      )
      lepusClient <- LepusClient[IO](
        host = brokerHost,
        port = brokerPort
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
