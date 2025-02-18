package domain

import cats.effect.kernel.{Async, MonadCancel, Resource, Sync}
import cats.effect.std.Queue
import cats.{Applicative, Monad}
import cats.implicits.*

import scala.collection.mutable


type MediaWorkerId = Int

enum MediaWorkerStatus:
  case Active, Stopped


/**
 * MediaWorker is a class that handle worker lifecycle. The main goal of the worker is to listen task queue and execute 
 * obtained tasks. When worker stops, it should gracefully stop and return to the queue all active media streams.
 *
 * @tparam F
 */
trait MediaWorker[F[_]] {
  val id: MediaWorkerId
  val status: MediaWorkerStatus
  val tasksCapacity: Int

  def stop: F[Unit]
}

type ActiveStreams[F[_]] = mutable.Map[MediaStreamId, (MediaStream[F], StreamingProcess[F])]

class MediaWorkerImpl[F[_] : Async : Monad](
                                             val id: MediaWorkerId,
                                             val status: MediaWorkerStatus,
                                             val tasksCapacity: Int,
                                             val queue: Queue[F, MediaStream[F]],
                                             val activeStreams: ActiveStreams[F]
                                           )
                                           (implicit val monadCancel: MonadCancel[F, Throwable])
  extends MediaWorker[F] {
  var ifStopped: Boolean = false

  def worker: F[Unit] = {

    def cancelWorker: F[Unit] = {
      activeStreams.values.toList.traverse_ { case (mediaStream, streamingProcess) =>
        streamingProcess.stop *> queue.offer(mediaStream)
      } *> Sync[F].delay {
        activeStreams.clear()
      }
    }

    def loop(): F[Unit] = {
      MonadCancel[F].uncancelable { poll =>
        for {
          mediaStream <- poll(monadCancel.onCancel(queue.take, cancelWorker))
          _ <- mediaStream.act.use {
            streamingProcess => {
              activeStreams.put(mediaStream.id, (mediaStream, streamingProcess))
              Applicative[F].unit
            }
          }
          _ <- if ifStopped
          then
            cancelWorker
          else
            poll(loop())
        } yield ()
      }
    }

    loop()
  }

  override def stop: F[Unit] = {
    Async[F].delay {
      ifStopped = true
    }
  }
}

object MediaWorkerImpl {
  def apply[F[_] : Async](
                           id: MediaWorkerId,
                           status: MediaWorkerStatus,
                           tasksCapacity: Int,
                           queue: Queue[F, MediaStream[F]],
                         ): Resource[F, MediaWorker[F]] =
    MediaWorkerImpl(id, status, tasksCapacity, queue, mutable.Map())

  def apply[F[_] : Async](
                           id: MediaWorkerId,
                           status: MediaWorkerStatus,
                           tasksCapacity: Int,
                           queue: Queue[F, MediaStream[F]],
                           activeStreams: ActiveStreams[F]
                         ): Resource[F, MediaWorker[F]] = {
    val mediaWorker = new MediaWorkerImpl(
      id, status, tasksCapacity, queue, activeStreams
    )
    Resource.make(
      Async[F].start(mediaWorker.worker).map(fiber =>
        new MediaWorker[F] {

          override val id: MediaWorkerId = mediaWorker.id
          override val status: MediaWorkerStatus = mediaWorker.status
          override val tasksCapacity: MediaStreamId = mediaWorker.tasksCapacity

          override def stop: F[Unit] = mediaWorker.stop *> fiber.cancel
        })
    ) { mediaWorker => mediaWorker.stop }
  }
}
