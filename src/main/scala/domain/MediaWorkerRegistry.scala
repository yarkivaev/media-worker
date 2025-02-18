package domain

import cats.effect.kernel.{Async, MonadCancel, Resource}
import cats.effect.std.{Queue, Random}
import cats.implicits.*
import cats.{Applicative, Monad}

import scala.collection.mutable

/**
 * A registry that collects all workers in the hospital
 * @tparam F
 */
trait MediaWorkerRegistry[F[_]] {
  def startNewWorker(): F[MediaWorkerId]

  def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]]
}

case class MediaWorkerRegistryImpl[F[_] : Monad : Random : Async]
(
  storage: mutable.Map[MediaWorkerId, MediaWorker[F]],
  mediaWorkerSup: (id: MediaWorkerId,
                   status: MediaWorkerStatus,
                   tasksCapacity: Int,
                   queue: Queue[F, MediaStream[F]]) => Resource[F, MediaWorker[F]],
  workerCapacity: Int,
  queue: Queue[F, MediaStream[F]]
)(implicit val monadCancel: MonadCancel[F, Throwable])
  extends MediaWorkerRegistry[F] {
  override def startNewWorker(): F[MediaWorkerId] =
    for {
      id <- Random[F].nextInt
      mediaWorkerResource = mediaWorkerSup(
        id,
        MediaWorkerStatus.Active,
        workerCapacity,
        queue
      )
      _ <- Async[F].start(mediaWorkerResource.use { mediaWorker =>
        storage.put(id, mediaWorker)
        Async[F].never
      })
    } yield id

  override def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]] =
    for {
      optionalMediaWorkerFiber <- Applicative[F].pure {
        storage.get(id)
      }
      res <- optionalMediaWorkerFiber match {
        case Some(mediaWorker) => mediaWorker.stop.map(Right(_))
        case None => Applicative[F].pure {
          Left(new Exception("No such media worker"))
        }
      }
    } yield res

  def stopAllWorkers: F[Unit] = {
    storage.values.toList.traverse_ { mediaWorker => stopMediaWorker(mediaWorker.id) }
  }
}

object MediaWorkerRegistryImpl {
  def apply[F[_] : Monad : Random : Async](
                                            storage: mutable.Map[MediaWorkerId, MediaWorker[F]],
                                            mediaWorkerSup: (id: MediaWorkerId,
                                                             status: MediaWorkerStatus,
                                                             tasksCapacity: Int,
                                                             queue: Queue[F, MediaStream[F]]) => Resource[F, MediaWorker[F]],
                                            workerCapacity: Int,
                                            queue: Queue[F, MediaStream[F]]
                                          ): Resource[F, MediaWorkerRegistry[F]] = {
    Resource.make(Applicative[F].pure(new MediaWorkerRegistryImpl(
      storage,
      mediaWorkerSup,
      workerCapacity,
      queue
    ))) { registry => registry.stopAllWorkers }
  }
}