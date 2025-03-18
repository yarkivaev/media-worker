package domain.server

import cats.effect.kernel.Async
import domain.BrokerMessage
import domain.command.MediaWorkerCommand
import domain.server.streaming.StreamingBackend
import fs2.*

object MediaWorker {

  /** Effectful stream that processes messages from broker queue.
    * @param messageSource
    *   source of messages from broker queue
    * @tparam F
    *   Effect
    * @return
    *   Effect that serve worker which process messages from broker queue
    */
  def apply[F[_]: Async: StreamingBackend: ActiveMediaStreams](
    messageSource: Stream[F, BrokerMessage[F, MediaWorkerCommand]]
  ): F[Unit] = {
    (for {
      message <- messageSource
      commandEffect = Stream.eval(message.content.act)
    } yield commandEffect).parJoinUnbounded.compile.drain
  }
}
