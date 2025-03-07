package domain.server

import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.Port
import domain.*
import domain.command.MediaWorkerCommand
import domain.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend, StreamingResource}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName
import lepus.std.ChannelCodec

import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    given ChannelCodec[MediaWorkerCommand] = summon[ChannelCodec[MediaWorkerCommand]]

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess.fake

    given StreamingResource[MediaSource] = summon[StreamingResource[MediaSource]]

    given StreamingResource[MediaSink] = summon[StreamingResource[MediaSink]]

    given StreamingBackend[IO] = FFMpegStreamingBackend[IO]

    given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]

    (for {
      lepusClient <- LepusClient[IO](port = Port.fromInt(5672).get)
      mediaWorker <- Resource.eval(
        MediaWorker(
          Broker.messageSource[IO, MediaWorkerCommand](
            lepusClient,
            QueueName("mediaworkercommand")
          )
        )
      )
//      messageSink <- Broker.messageSink[IO, MediaWorkerCommand](lepusClient, QueueName("mediaworkercommand"))
    } yield ()).use_.as(ExitCode.Success)
  }
}
