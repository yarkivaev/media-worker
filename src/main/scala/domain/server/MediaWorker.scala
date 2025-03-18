package domain.server

import cats.effect.kernel.Async
import domain.BrokerMessage
import domain.command.MediaWorkerCommand
import domain.server.streaming.StreamingBackend
import fs2.*

object MediaWorker {
  def apply[F[_]: Async: StreamingBackend: ActiveMediaStreams](
    messageSource: Stream[F, BrokerMessage[F, MediaWorkerCommand]]
  ): F[Unit] = {
    (for {
      message <- messageSource
      //      _ <- Stream.eval(message.ack)
      commandEffect = Stream.eval(message.message.act)
    } yield commandEffect).parJoinUnbounded.compile.drain
  }
}
