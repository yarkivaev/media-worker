package domain

import cats.Applicative
import cats.effect.*
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.github.nscala_time.time.Imports
import org.scalatest.*

import scala.collection.mutable
import scala.concurrent.duration.*

class MediaWorkerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  case class FakeMediaStream[F[_] : Async](id: MediaStreamId) extends MediaStream[F]:
    override val startDateTime: Imports.DateTime = Imports.DateTime.now()
    override val stopDateTime: Imports.DateTime = Imports.DateTime.now()
    var stopped = false
    override val source: MediaSource = RtmpSource("123")
    override val sink: MediaSink = RtmpSink("132")

    override def act: Resource[F, StreamingProcess[F]] =
      Resource.make(
        Applicative[F].pure {
          new FakeStreamingProcess[F](() => {
            stopped = true
            Applicative[F].unit
          }
          )
        }
      )(_.stop)

  class FakeStreamingProcess[F[_] : Async](stopMediaStream: () => F[Unit]) extends StreamingProcess[F] {
    def stop: F[Unit] = stopMediaStream()
  }

  "MediaWorker" should "process media streams and handle stopping correctly" in {

    val queue = Queue.unbounded[IO, MediaStream[IO]]().unsafeRunSync()

    val mediaWorkerId = 1
    val mediaWorkerStatus = MediaWorkerStatus.Active
    val tasksCapacity = 10
    val activeStreams = mutable.Map.empty[MediaStreamId, (MediaStream[IO], StreamingProcess[IO])]

    val workerResource = MediaWorkerImpl[IO](mediaWorkerId, mediaWorkerStatus, tasksCapacity, queue, activeStreams)
    val fakeStream = FakeMediaStream[IO](1)
    queue.offer(fakeStream).unsafeRunSync()

    workerResource.use {
      worker => {
        for {
          _ <- IO.sleep(1.second)
          size <- queue.size
          _ <- IO.delay {
            assert(activeStreams.size == 1)
            assert(activeStreams.contains(fakeStream.id))
            assert(size == 0)
          }
        } yield ()
      }
    }.unsafeRunSync()
    assert(activeStreams.isEmpty)
    assert(fakeStream.stopped)
    assert(queue.size.unsafeRunSync() == 1)
  }
}
