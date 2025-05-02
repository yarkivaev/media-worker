package medwork.server

import cats.effect.kernel.Async
import medwork.BrokerMessage
import medwork.command.MediaWorkerCommand
import medwork.server.streaming.StreamingBackend
import fs2.*
import medwork.MediaSink
import medwork.server.persistence.Storage

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
  )(using Storage[F, MediaSink]): F[Unit] = {
    (for {
      message <- messageSource
      commandEffect = Stream.eval(message.content.execute)
    } yield commandEffect).parJoinUnbounded.compile.drain
  }
}
