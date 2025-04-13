package medwork.client

import cats.effect.{IO, Resource}
import com.comcast.ip4s.Port
import medwork.Broker
import medwork.command.MediaWorkerCommand
import fs2.{Pipe, Stream}
import lepus.client.{Envelope, LepusClient, Message, ReturnedMessage, ReturnedMessageRaw}
import lepus.protocol.domains.{ExchangeName, QueueName, ShortString}

/** Effectful client.
  * @tparam F
  *   Effect
  */
trait Client[F[_]] {
  def executeCommand(command: MediaWorkerCommand): F[Unit]
}

object Client {
  def apply(messageSink: Pipe[IO, MediaWorkerCommand, ReturnedMessageRaw]): Client[IO] =
    new Client[IO] {
      def executeCommand(command: MediaWorkerCommand): IO[Unit] = messageSink(Stream(command)).compile.drain
    }

  def apply(queuePort: Int): Resource[IO, Client[IO]] = {
    for {
      lepusClient <- LepusClient[IO](port = Port.fromInt(queuePort).get)
      messageSink <- Broker.messageSink[IO, MediaWorkerCommand](
        lepusClient,
        QueueName("mediaworkercommand")
      )
    } yield Client(messageSink)
  }

  def printSink: Pipe[IO, Envelope[MediaWorkerCommand], ReturnedMessageRaw] = stream =>
    stream.map(_ => {
      println("log")
      ReturnedMessage(
        null,
        ShortString("hello"),
        ExchangeName("hello"),
        ShortString("null"),
        null
      )
    })
}
