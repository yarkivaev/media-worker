package domain.server

import domain.MediaStream
import cats.effect.{Async, Deferred, Spawn, Sync}
import cats.implicits._
import cats.syntax._

import scala.collection.mutable

trait ActiveMediaStreams[F[_]] {
  def manageMediaStream(mediaStream: MediaStream, effect: F[Unit]): F[Unit]

  def contains(mediaStream: MediaStream): F[Boolean]

  def stopMediaStream(mediaStream: MediaStream): F[Either[String, Unit]]
}

object ActiveMediaStreams {
  def inMemory[F[_] : Async]: ActiveMediaStreams[F] = new ActiveMediaStreams[F] {
    private val storage: mutable.Map[MediaStream, Deferred[F, Unit]] = mutable.Map.empty[MediaStream, Deferred[F, Unit]]

    def manageMediaStream(mediaStream: MediaStream, effect: F[Unit]): F[Unit] = {
      for {
        _ <- Sync[F].delay(println("Start race"))
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