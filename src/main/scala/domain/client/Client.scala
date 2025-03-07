package domain.client

import cats.effect.IO
import domain.command.MediaWorkerCommand
import fs2.{Pipe, Stream}
import lepus.client.{Envelope, Message, ReturnedMessage, ReturnedMessageRaw}
import lepus.protocol.domains.{ExchangeName, ShortString}

trait Client[F[_]] {
  def executeCommand(command: MediaWorkerCommand): F[Unit]
}

object Client {
  def apply(messageSink: Pipe[IO, Envelope[MediaWorkerCommand], ReturnedMessageRaw]): Client[IO] =
    new Client[IO] {
      def executeCommand(command: MediaWorkerCommand): IO[Unit] = messageSink(Stream(
        Envelope(
          ExchangeName(""),
          ShortString(""),
          true,
          Message(command)
        )
      )).compile.toVector.map(println(_))
    }

  def printSink: Pipe[IO, Envelope[MediaWorkerCommand], ReturnedMessageRaw] = stream => stream.map(_ => {
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