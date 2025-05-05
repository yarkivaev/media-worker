package medwork.client

import cats.effect.IO
import cats.effect.Resource
import com.comcast.ip4s.Port
import fs2.Pipe
import fs2.Stream
import lepus.client.LepusClient
import lepus.client.ReturnedMessageRaw
import lepus.protocol.domains.QueueName
import lepus.protocol.domains.ShortString
import medwork.Broker
import medwork.command.MediaWorkerCommand

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
      brokerPort <- Resource.eval(
        IO.fromEither(
          Port.fromInt(queuePort).toRight(new NoSuchElementException("No value in option"))
        )
      )
      lepusClient <- LepusClient[IO](port = brokerPort)
      messageSink <- Broker.messageSink[IO, MediaWorkerCommand](
        lepusClient,
        QueueName("mediaworkercommand")
      )
    } yield Client(messageSink)
  }
}
