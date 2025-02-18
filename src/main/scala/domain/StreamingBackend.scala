package domain

import cats.effect.Sync
import cats.effect.kernel.{Async, Resource}
import cats.implicits.*
import cats.syntax.all.*
import cats.{Applicative, Monad}

import scala.concurrent.duration.*

trait StreamingProcess[F[_]] {
  def stop: F[Unit]
}

trait StreamingBackend[F[_]] {
  /**
   * Runs new streaming process in separate thread
   * @param mediaSource
   * @param mediaSink
   * @return
   */
  def spawnNewProcess(mediaSource: MediaSource, mediaSink: MediaSink): Resource[F, StreamingProcess[F]]
}

class StreamingBackendImpl[F[_] : Async : Monad] extends StreamingBackend[F] {
  override def spawnNewProcess(mediaSource: MediaSource, mediaSink: MediaSink): Resource[F, StreamingProcess[F]] = {
    var ifStopped = false

    def loop: F[Unit] = for {
      _ <- Sync[F].delay(println(s"Streaming from $mediaSource to $mediaSink"))
      _ <- Async[F].sleep(5.second)
      _ <- if ifStopped then Applicative[F].unit else loop
    } yield ()
    
    Resource
      .make(
        Async[F].start(loop).map { hello =>
          new StreamingProcess[F] {
            def stop: F[Unit] = {
              ifStopped = true
              Applicative[F].unit
            }
          }
        }
      )(_.stop)
  }
}

abstract class FFMpegStreamingBackend[F[_]] extends StreamingBackend[F] {

}
