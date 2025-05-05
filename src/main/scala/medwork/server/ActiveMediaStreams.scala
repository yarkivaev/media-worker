package medwork.server
import cats.effect._
import cats.effect.kernel.MonadCancel
import cats.implicits._
import medwork.MediaSink
import medwork.MediaStream
import medwork.server.persistence.Storage
import medwork.server.streaming.StreamingBackend

import scala.collection.mutable

/** Keeps every active media stream in the process.
  *
  * Provides ability to start new media stream, manage it and stop.
  * @tparam F
  *   Effect
  */
trait ActiveMediaStreams[F[_]] {

  /** Manages media stream process
    * @param mediaStream
    *   media stream object
    * @param effect
    *   effect to run media stream process
    * @return
    *   Effect that manages media stream
    */
  def manageMediaStream(mediaStream: MediaStream): F[Unit]

  /** Checks if a collection have contains mediaStream.
    * @param mediaStream
    *   media stream object
    * @return
    *   Effect that returns check result
    */
  def contains(mediaStream: MediaStream): F[Boolean]

  /** Stops active mediaStream. Returns either Right(Unit), or Left(error), which means correct or error respectively.
    * @param mediaStream
    *   media stream object
    * @return
    *   Effect that stops media stream process
    */
  def stopMediaStream(mediaStream: MediaStream): F[Either[String, Unit]]
}

object ActiveMediaStreams {
  def inMemory[F[_]: Async: StreamingBackend](using
    storage: Storage[F, MediaSink],
    mc: MonadCancel[F, Throwable]
  ): ActiveMediaStreams[F] = new ActiveMediaStreams[F] {
    private val storage: mutable.Map[MediaStream, Deferred[F, Unit]] = mutable.Map.empty[MediaStream, Deferred[F, Unit]]

    def manageMediaStream(mediaStream: MediaStream): F[Unit] = {
      for {
        d <- Deferred[F, Unit]
        _ <- Sync[F].delay({ storage += (mediaStream -> d) })
        _ <- Spawn[F].race(
          d.get,
          mediaStream.act
        )
      } yield ()
    }

    def contains(mediaStream: MediaStream): F[Boolean] = Sync[F].delay(storage.contains(mediaStream))

    def stopMediaStream(mediaStream: MediaStream): F[Either[String, Unit]] = for {
      dOption <- Sync[F].delay(storage.get(mediaStream))
      _ <- dOption.map(_.complete(())).sequence
      _ <- Sync[F].delay(dOption.map(_ => storage.remove(mediaStream)))
    } yield dOption.map(_ => ()).toRight("No mediaStream found")
  }
}
