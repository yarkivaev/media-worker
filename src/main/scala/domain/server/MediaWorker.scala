package domain.server

import cats.effect.Concurrent
import domain.server.streaming.StreamingBackend
import domain.{BrokerMessage, MediaWorkerCommand}
import fs2.*

object MediaWorker {
  def apply[F[_] : Concurrent : StreamingBackend]
  (messageSource: Stream[F, BrokerMessage[F, MediaWorkerCommand[F]]]): F[Unit] = {
    (for {
      message <- messageSource
      _ <- Stream.eval(message.ack)
      commandEffect = Stream.eval(message.message.act)
    } yield commandEffect).parJoinUnbounded.compile.drain
  }
}
