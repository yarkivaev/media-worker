package domain.server

import cats.Applicative
import cats.effect.{Concurrent, IO, Resource}
import domain.server.streaming.StreamingBackend
import domain.BrokerMessage
import domain.command.MediaWorkerCommand
import fs2.*

object MediaWorker {
  def apply[F[_] : Concurrent : ActiveMediaStreams]
  (messageSource: Stream[F, BrokerMessage[F, MediaWorkerCommand]]): F[Unit] = {
    (for {
      _ <- Stream.eval(Applicative[F].pure(println("hello")))
      message <- messageSource
      _ <- Stream.eval(Applicative[F].pure(println(message.message)))
//      _ <- Stream.eval(message.ack)
//      commandEffect = Stream.eval(message.message.act)
    } yield Stream()).parJoinUnbounded.compile.drain
  }
}
