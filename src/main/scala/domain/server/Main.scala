package domain.server

import cats.effect.kernel.Clock
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s.Port
import domain.*
import domain.command.MediaWorkerCommand
import domain.server.streaming.{FFMpegStreamingBackend, RunProcess, StreamingBackend}
import lepus.client.LepusClient
import lepus.protocol.domains.QueueName

import scala.language.postfixOps
import scala.runtime.stdLibPatches.Predef.summon

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    given Name[HlsSink] = hlsSink => hlsSink.sinkName

    given RunProcess[IO] = RunProcess[IO]()

    given Clock[IO] = IO.asyncForIO

    import domain.server.streaming.FFMpeg.{*, given}

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
    } yield ()).use_.as(ExitCode.Success)
  }
}
