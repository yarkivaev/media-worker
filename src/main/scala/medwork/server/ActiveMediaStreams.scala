package medwork.server

import cats.effect.{Async, Deferred, Spawn, Sync}
import cats.implicits.*
import medwork.MediaStream

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
  def manageMediaStream(mediaStream: MediaStream, effect: F[Unit]): F[Unit]

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
  def inMemory[F[_]: Async]: ActiveMediaStreams[F] = new ActiveMediaStreams[F] {
    private val storage: mutable.Map[MediaStream, Deferred[F, Unit]] = mutable.Map.empty[MediaStream, Deferred[F, Unit]]

    def manageMediaStream(mediaStream: MediaStream, effect: F[Unit]): F[Unit] = {
      for {
        d <- Deferred[F, Unit]
        _ <- Sync[F].delay(storage + (mediaStream -> d))
        _ <- Spawn[F].race(
          d.get,
          effect
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
