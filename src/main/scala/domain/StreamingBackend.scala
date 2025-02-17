package domain

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.syntax.all.*
import scala.concurrent.duration._

trait StreamingBackend[F[_]] {
  def run(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit]
}

class StreamingBackendImpl[F[_] : Async] extends StreamingBackend[F] {
  override def run(mediaSource: MediaSource, mediaSink: MediaSink): F[Unit] = {
    def loop: F[Unit] = for {
      _ <- Sync[F].delay(println(s"Streaming from $mediaSource to $mediaSink"))
      _ <- Async[F].sleep(5.second) // Delay for 1 second
      _ <- loop // Recurse to keep printing
    } yield ()

    loop // Start the loop
  }
}
