package domain

import cats.effect.Concurrent
import domain.streaming.StreamingBackend
import fs2.*

object MediaWorker {
  def apply[F[_] : Concurrent : StreamingBackend]
  (messageSource: Stream[F, BrokerMessage[F, MediaWorkerCommand[F]]]): F[Unit] = {
    (for {
      message <- messageSource
      _ <- Stream.eval(message.ack)
      command = Stream.eval(message.message.act)
    } yield command).parJoinUnbounded.compile.drain
  }
}
