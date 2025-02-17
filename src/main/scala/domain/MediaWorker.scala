package domain

import cats.{Applicative, Monad}
import cats.effect.Sync
import cats.effect.kernel.Resource.Pure
import cats.effect.kernel.{Async, Fiber, MonadCancel, Outcome, Resource}
import cats.effect.std.Random
import cats.syntax.*
import cats.syntax.all.toFlatMapOps
import cats.syntax.functor.*
import cats.syntax.ApplicativeSyntax
import cats.implicits._

import java.util.concurrent.BlockingQueue
import scala.collection.mutable
import scala.collection.mutable.Queue

type MediaWorkerId = Int

enum MediaWorkerStatus:
  case Active, Stopped

trait MediaWorker[F[_]] {
  val id: MediaWorkerId
  val status: MediaWorkerStatus
  val tasksCapacity: Int

  def act: F[Fiber[F, Throwable, Unit]]

  def executeMediaStream(mediaStream: MediaStream[F]): F[Unit]
}

case class MediaWorkerImpl[F[_] : Async](
                                          id: MediaWorkerId,
                                          status: MediaWorkerStatus,
                                          tasksCapacity: Int,
                                          queue: BlockingQueue[MediaStream[F]]
                                        ) extends MediaWorker[F] {
  def worker: F[Unit] = {
    def loop(): F[Unit] = {
      for {
        mediaStream <- Async[F].blocking {
          queue.take()
        }
        mediaStreamFiber <- Async[F].start(mediaStream.act)
        _ <- loop()
      } yield ()
    }

    loop()
  }

  override def act:  F[Fiber[F, Throwable, Unit]] =
    for {
      fiber <- Async[F].start(worker)
    } yield fiber

  override def executeMediaStream(mediaStream: MediaStream[F]): F[Unit] = mediaStream.act
}

trait MediaWorkerRegistry[F[_]] {
  def startNewWorker(): F[MediaWorker[F]]

  def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]]
}

case class MediaWorkerRegistryImpl[F[_]: Monad : Random]
(
  storage: mutable.Map[MediaWorkerId, Fiber[F, Throwable, Unit]],
  mediaWorkerSup: (id: MediaWorkerId,
                   status: MediaWorkerStatus,
                   tasksCapacity: Int,
                   queue: BlockingQueue[MediaStream[F]]) => MediaWorker[F],
  workerCapacity: Int,
  queue: BlockingQueue[MediaStream[F]]
)(implicit val monadCancel: MonadCancel[F, Throwable])
  extends MediaWorkerRegistry[F] {
  override def startNewWorker(): F[MediaWorker[F]] =
    for {
      id <- Random[F].nextInt
      mediaWorker: MediaWorker[F] = mediaWorkerSup(
        id,
        MediaWorkerStatus.Active,
        workerCapacity,
        queue
      )
      fiber <- mediaWorker.act
      _ <- Applicative[F].pure {
        storage.put(id, fiber)
        id
      }
    } yield mediaWorker

  override def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]] =
    for {
      optionalMediaWorkerFiber <- Applicative[F].pure  {
        storage.get(id)
      }
      res <- optionalMediaWorkerFiber match {
        case Some(resource) => Applicative[F].pure { Left(new Exception("No implemented")) }
        case None => Applicative[F].pure { Left(new Exception("No such media worker")) }
      }
    } yield res
}