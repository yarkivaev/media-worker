package medwork.server.streaming

import cats.Monad
import cats.effect.Sync
import cats.effect.kernel.Async
import cats.effect.kernel.MonadCancel
import cats.syntax.all._
import medwork.MediaSink
import medwork.MediaSource

import scala.concurrent.duration._

/** Represents some resource in hospital system that can be streamed.
  * @tparam A
  *   Resource type
  */
trait StreamingResource[F[_], A] {
  def destination(media: A): F[String]

  def options(media: A): F[Map[String, String]]
}

/** Streaming backend that runs stream from source to sink
  * @tparam F
  *   Effect
  */
trait StreamingBackend[F[_]] {

  /** Runs stream from source to sink
    * @param mediaSource
    *   stream source
    * @param mediaSink
    *   stream sink
    * @return
    *   Effect that runs streaming process
    */
  def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit]
}

class StreamingBackendImpl[F[_]: Async: Monad](implicit val monadCancel: MonadCancel[F, Throwable])
  extends StreamingBackend[F] {
  override def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit] = {
    def loop: F[Unit] =
      for {
        _ <- Sync[F].delay(println(s"Streaming from $mediaSource to $mediaSink"))
        _ <- Sync[F].sleep(1.second)
        _ <- loop
      } yield ()

    loop
  }
}
