package medwork.server

import cats.effect.*
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import medwork.*
import medwork.command.{MediaWorkerCommand, StartMediaStream, StopMediaStream}
import medwork.server.streaming.StreamingBackendImpl
import fs2.*
import org.scalatest.*
import medwork.server.persistence.Storage

import scala.concurrent.duration.DurationInt

class MediaWorkerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "MediaWorker" should "process media streams and handle stopping correctly" in {

    val queue = Queue.unbounded[IO, MediaStream]().unsafeRunSync()

    def brokerMessage(command: MediaWorkerCommand): BrokerMessage[IO, MediaWorkerCommand] =
      new BrokerMessage[IO, MediaWorkerCommand] {
        val content: MediaWorkerCommand = command

        def ack: IO[Unit] = IO.unit

        override def nack: IO[Unit] = IO.unit
      }

    given StreamingBackendImpl[IO] = StreamingBackendImpl[IO]()

    given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]

    given Storage[IO, MediaSink] = mediaSink => IO.unit

    val fiber: Fiber[IO, Throwable, Unit] = Async[IO]
      .start(
        MediaWorker[IO](
          Stream(
            StartMediaStream(
              RtmpSource("url"),
              RtmpSink("url")
            ),
            StopMediaStream(
              RtmpSource("url"),
              RtmpSink("url")
            )
          ).map(brokerMessage)
        )
      )
      .unsafeRunSync()

    IO.sleep(3.second).unsafeRunSync()

    fiber.cancel.unsafeRunSync()
  }
}
