package domain

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.*
import cats.syntax.all.toFlatMapOps
import cats.syntax.functor.*
import cats.{Applicative, Monad}

import scala.collection.mutable


type MediaWorkerId = Int

enum MediaWorkerStatus:
  case Active, Stopped

trait MediaWorker[F[_]] {
  val id: MediaWorkerId
  val status: MediaWorkerStatus
  val tasksCapacity: Int

  def stop: F[Unit]
}

case class MediaWorkerImpl[F[_] : Async : Monad](
                                                  id: MediaWorkerId,
                                                  status: MediaWorkerStatus,
                                                  tasksCapacity: Int,
                                                  queue: Queue[F, MediaStream[F]],
                                                  activeStreams: mutable.Map[MediaStreamId, StreamingProcess[F]]
                                                ) extends MediaWorker[F] {
  var ifStopped: Boolean = false

  def worker: F[Unit] = {

    def loop(): F[Unit] = {
      for {
        mediaStream <- queue.take
        _ <- mediaStream.act.use {
          streamingProcess => {
            activeStreams.put(mediaStream.id, streamingProcess)
            Applicative[F].unit
          }
        }
        _ <- if ifStopped
        then
          activeStreams.values.toList.traverse_(_.stop)
        else
          loop()
      } yield ()
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
                         ): Resource[F, MediaWorker[F]] = {
    val mediaWorker = new MediaWorkerImpl(
      id, status, tasksCapacity, queue, mutable.Map()
    )
    Resource.make(
      Async[F].start(mediaWorker.worker).map(_ => mediaWorker)
    ) { mediaWorker => mediaWorker.stop }
  }
}
