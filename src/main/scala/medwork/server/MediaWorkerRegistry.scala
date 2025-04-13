package medwork.server

import cats.syntax.all.*

type MediaWorkerId = Int

enum MediaWorkerStatus:
  case Active, Stopped

/** A registry that collects all workers in the hospital. Useful what many pak media workers works together.
  * @tparam F
  */
trait MediaWorkerRegistry[F[_]] {
  def startNewWorker(): F[MediaWorkerId]

  def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]]
}

//type MediaWorkerSupplier[F[_]] = (MediaWorkerId,
//  MediaWorkerStatus,
//  Int,
//  Queue[F, MediaStream[F]]) => Resource[F, MediaWorker[F]]
//
//class MediaWorkerRegistryImpl[F[_] : Monad : Random : Async]
//(
//  storage: mutable.Map[MediaWorkerId, MediaWorker[F]],
//  mediaWorkerSup: MediaWorkerSupplier[F],
//  workerCapacity: Int,
//  queue: Queue[F, MediaStream[F]]
//)(implicit val monadCancel: MonadCancel[F, Throwable])
//  extends MediaWorkerRegistry[F] {
//  val workersFibers: mutable.Buffer[Fiber[F, Throwable, Any]] = mutable.Buffer.empty[Fiber[F, Throwable, Any]]
//
//  override def startNewWorker(): F[MediaWorkerId] =
//    for {
//      id <- Random[F].nextInt
//      mediaWorkerResource = mediaWorkerSup(
//        id,
//        MediaWorkerStatus.Active,
//        workerCapacity,
//        queue
//      )
//      fiber <- Async[F].start(mediaWorkerResource.use({ mediaWorker =>
//        storage.put(id, mediaWorker)
//        Async[F].never
//      }))
//      _ <- Sync[F].delay {
//        workersFibers.append(fiber)
//      }
//    } yield id
//
//  override def stopMediaWorker(id: MediaWorkerId): F[Either[Throwable, Unit]] =
//    for {
//      optionalMediaWorker <- Sync[F].delay {
//        storage.get(id)
//      }
//      res <- optionalMediaWorker match {
//        case Some(mediaWorker) => {
//          mediaWorker.stop.map(Right(_))
//        }
//        case None => Applicative[F].pure {
//          Left(new Exception("No such media worker"))
//        }
//      }
//    } yield res
//
//  def stopAllWorkers(): F[Unit] =
//    storage.values.toList.traverse_ { mediaWorker => stopMediaWorker(mediaWorker.id) }
//}
//
//object MediaWorkerRegistryImpl {
//  def apply[F[_] : Monad : Random : Async](
//                                            storage: mutable.Map[MediaWorkerId, MediaWorker[F]],
//                                            mediaWorkerSup: MediaWorkerSupplier[F],
//                                            workerCapacity: Int,
//                                            queue: Queue[F, MediaStream[F]]
//                                          )(implicit monadCancel: MonadCancel[F, Throwable]): Resource[F, MediaWorkerRegistry[F]] = {
//    Resource.make(Applicative[F].pure(new MediaWorkerRegistryImpl(
//      storage,
//      mediaWorkerSup,
//      workerCapacity,
//      queue
//    )))(registry => monadCancel.uncancelable { poll =>
//      registry.stopAllWorkers() *>
//        registry.workersFibers.toList.traverse_ {
//          _.cancel
//        }
//    })
//  }
//}
