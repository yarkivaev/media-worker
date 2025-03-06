package domain.server

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.Port
import domain.*
import domain.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend, StreamingResource}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName
import lepus.std.ChannelCodec

import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    import domain.MediaWorkerCommand.*

    given ChannelCodec[MediaWorkerCommand[IO]] = summon[ChannelCodec[MediaWorkerCommand[IO]]]

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess()

    given StreamingResource[MediaSource] = summon[StreamingResource[MediaSource]]

    given StreamingResource[MediaSink] = summon[StreamingResource[MediaSink]]

    given StreamingBackend[IO] = FFMpegStreamingBackend[IO]

    (for {
      lepusClient <- LepusClient[IO](port = Port.fromInt(5672).get)
      mediaWorker <- Resource.eval(
        MediaWorker(
          Broker.messageSource[IO, MediaWorkerCommand[IO]](
            lepusClient,
            QueueName("mediaworkercommand")
          )
        )
      )
      messageSink <- Broker.messageSink[IO, MediaWorkerCommand[IO]](lepusClient, QueueName("mediaworkercommand"))
    } yield ()).use_.as(ExitCode.Success)
  }
}
