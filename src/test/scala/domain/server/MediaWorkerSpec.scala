package domain.server

import cats.effect.*
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import domain.*
import domain.command.{MediaWorkerCommand, RouteCameraToMiddleware, SupplyWebRtcServer}
import domain.server.streaming.StreamingBackendImpl
import fs2.*
import org.scalatest.*

import scala.concurrent.duration.DurationInt

class MediaWorkerSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {


  "MediaWorker" should "process media streams and handle stopping correctly" in {

    val queue = Queue.unbounded[IO, MediaStream]().unsafeRunSync()

    def brokerMessage(command: MediaWorkerCommand): BrokerMessage[IO, MediaWorkerCommand] =
      new BrokerMessage[IO, MediaWorkerCommand] {
        val message: MediaWorkerCommand = command

        def ack: IO[Unit] = IO.unit

        override def nack: IO[Unit] = IO.unit
      }

    given StreamingBackendImpl[IO] = StreamingBackendImpl[IO]()

    given ActiveMediaStreams[IO] = ActiveMediaStreams.inMemory[IO]

    val fiber: Fiber[IO, Throwable, Unit] = Async[IO].start(MediaWorker[IO](
      Stream(
        RouteCameraToMiddleware(
          RtmpSource("url"),
          RtmpSink("url")
        ),
        SupplyWebRtcServer(
          RtmpSource("url"),
          RtmpSink("url")
        )
      ).map(brokerMessage)
    )).unsafeRunSync()

    IO.sleep(3.second).unsafeRunSync()

    fiber.cancel.unsafeRunSync()
  }
}
