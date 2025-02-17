import cats.{Applicative, Monad}
import cats.effect.std.Random
import cats.effect.{ExitCode, IO, IOApp}
import com.github.nscala_time.time.Imports.DateTime
import domain.MediaStreamStatus.Active
import domain.{HlsSink, MediaSink, MediaSource, MediaStream, MediaStreamImpl, MediaStreamStatus, MediaStreamType, MediaWorkerImpl, MediaWorkerRegistryImpl, MediaWorkerStatus, RecordVideoSource, RtmpSink, RtmpSource, RtspSource, StreamingBackendImpl}
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.blaze.server.BlazeServerBuilder

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, LinkedBlockingDeque}
import scala.collection.mutable
import scala.concurrent.duration.*
import sys.process.*
import scala.language.postfixOps


//case class MediaWorkerImpl[F[_]: Async](
//                                         id: MediaWorkerId,
//                                         status: MediaWorkerStatus,
//                                         tasksCapacity: Int
//                                       ) extends MediaWorker[F] {
//
//  override def executeMediaStream(mediaStream: MediaStream[F]): F[Unit] = mediaStream.act
//}

object Main extends IOApp {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello, World!")
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    mainThread(args)

  def tryCommand(args: List[String]): IO[ExitCode] =
    IO(os.proc("ls", "src").spawn()).map(res => print(res.stdout)).as(ExitCode.Success)

  def mainThread(args: List[String]): IO[ExitCode] =
    for {
      random <- Random.scalaUtilRandom[IO]
      queue = LinkedBlockingDeque[MediaStream[IO]]()
      registry = MediaWorkerRegistryImpl[IO](
        mutable.Map(),
        MediaWorkerImpl[IO](_, _, _, _),
        Integer.MAX_VALUE,
        queue
      )(implicitly[Monad[IO]], random)
      streamingBackend = StreamingBackendImpl[IO]
      mediaStream1 = MediaStreamImpl[IO](
        42,
        DateTime.now(),
        DateTime.now(),
        RecordVideoSource(),
        Active,
        RtmpSource("helo"),
        HlsSink("helo"),
      )(streamingBackend)
      mediaStream2 = MediaStreamImpl[IO](
        42,
        DateTime.now(),
        DateTime.now(),
        RecordVideoSource(),
        Active,
        RtspSource("helo"),
        RtmpSink("helo"),
      )(streamingBackend)
      _ <- registry.startNewWorker()
      _ <- Applicative[IO].pure { queue.put(mediaStream1) }
      _ <- Applicative[IO].pure { queue.put(mediaStream2) }
      a <- IO.never.as(ExitCode.Success)
    } yield a //ExitCode.Success
//    BlazeServerBuilder[IO]
//      .bindHttp(8080, "0.0.0.0")
//      .withHttpApp(helloWorldService)
//      .resource
//      .use(_ => IO.never)
//      .as(ExitCode.Success)
}
/*
Pak media worker:
- Serve media streams inside the hospital (pak)
- use ffmpeg, gstreamer, etg as backend
- have access to storage that keeps all running media processes
- can be controlled via Rest API, maintain responsibility to keep consistency
 -


Entities:
- Media stream
  - id
  - FFMpeg or GStreamer
  - FFMPEG: where-from, where-from-options, where-to, where-to-options
  - startDateTime
  - stopDateTime
  - Goal
    - save recording from video source
    - stream video source to local web rtc server
    - stream video source to global web rtc server
  - status: active, stopped, finished
- Media worker (adds itself to db, maintain active status. When worker is down, status became stopped)
  - id
  - serving media streams
  - status: active, stopped
  - tasks capacity


- Also we need a queue for workers to take tasks

Worker lifecycle:
- Boot, add itself to table of active workers
- Subscribe the queue, take tasks, execute them
 */
