package domain.streaming

import cats.Monad
import cats.effect.Sync
import cats.effect.kernel.{Async, MonadCancel}
import cats.syntax.all.*
import domain.{MediaSink, MediaSource}

import scala.concurrent.duration.*

trait StreamingResource[A] {
  def destination(media: A): String

  def options(media: A): Map[String, String]
}

trait StreamingBackend[F[_]] {
  /**
   * Runs new streaming process in separate thread
   *
   * @param mediaSource
   * @param mediaSink
   * @return
   */
  def stream(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit]
}

class StreamingBackendImpl[F[_] : Async : Monad](implicit val monadCancel: MonadCancel[F, Throwable]) extends StreamingBackend[F] {
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
